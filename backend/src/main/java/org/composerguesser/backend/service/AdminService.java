package org.composerguesser.backend.service;

import org.composerguesser.backend.dto.ApproveExcerptDto;
import org.composerguesser.backend.dto.DailyChallengesDto;
import org.composerguesser.backend.dto.ExcerptReviewDto;
import org.composerguesser.backend.model.Composer;
import org.composerguesser.backend.model.Excerpt;
import org.composerguesser.backend.model.ExcerptDay;
import org.composerguesser.backend.model.ExcerptStatus;
import org.composerguesser.backend.model.User;
import org.composerguesser.backend.repository.ComposerRepository;
import org.composerguesser.backend.repository.ExcerptDayRepository;
import org.composerguesser.backend.repository.ExcerptRepository;
import org.composerguesser.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * Business logic for the admin excerpt management workflow.
 * Admins can list excerpts by status, update their metadata, and publish or reject them.
 */
@Service
public class AdminService {

    private final ExcerptRepository excerptRepository;
    private final ExcerptDayRepository excerptDayRepository;
    private final ComposerRepository composerRepository;
    private final UserRepository userRepository;

    @Value("${audio.base-url}")
    private String audioBaseUrl;

    public AdminService(ExcerptRepository excerptRepository,
                        ExcerptDayRepository excerptDayRepository,
                        ComposerRepository composerRepository,
                        UserRepository userRepository) {
        this.excerptRepository = excerptRepository;
        this.excerptDayRepository = excerptDayRepository;
        this.composerRepository = composerRepository;
        this.userRepository = userRepository;
    }

    /**
     * Returns a page of excerpts matching any of the given statuses, optionally filtered
     * to a single composer, ordered oldest-first.
     *
     * @param statuses   one or more statuses to include
     * @param composerId optional composer filter (null = all composers)
     * @param page       zero-based page index
     * @param size       items per page
     */
    public Page<ExcerptReviewDto> getExcerpts(List<ExcerptStatus> statuses, Long composerId, String sort, int page, int size) {
        Sort springSort = switch (sort) {
            case "timesUsed_desc"    -> Sort.by("timesUsed").descending();
            case "dateUploaded_asc"  -> Sort.by("dateUploaded").ascending();
            case "dateUploaded_desc" -> Sort.by("dateUploaded").descending();
            default                  -> Sort.by("timesUsed").ascending();
        };
        PageRequest pageable = PageRequest.of(page, size, springSort);
        Page<Excerpt> results = composerId != null
                ? excerptRepository.findAllByStatusInAndComposerId(statuses, composerId, pageable)
                : excerptRepository.findAllByStatusIn(statuses, pageable);
        return results.map(this::toDto);
    }

    /**
     * Flips an excerpt's status without changing any other fields.
     *
     * @param excerptId the excerpt to update
     * @param newStatus the target status
     * @throws IllegalArgumentException if the excerpt does not exist
     */
    public void updateStatus(Long excerptId, ExcerptStatus newStatus) {
        Excerpt excerpt = excerptRepository.findById(excerptId)
                .orElseThrow(() -> new IllegalArgumentException("Excerpt not found: " + excerptId));
        excerpt.setStatus(newStatus);
        excerptRepository.save(excerpt);
    }

    /**
     * Updates the mutable fields of an excerpt and sets its status to {@link ExcerptStatus#ACTIVE},
     * making it eligible for the daily challenge.
     * <p>
     * If the excerpt is marked as point-eligible and points have not yet been awarded,
     * 2 points are added to the submitter's {@code total_points}.
     * </p>
     *
     * @param excerptId the excerpt to approve
     * @param dto       updated metadata from the admin
     * @throws IllegalArgumentException if the excerpt or the new composer does not exist
     */
    @Transactional
    public void approveExcerpt(Long excerptId, ApproveExcerptDto dto) {
        Excerpt excerpt = excerptRepository.findById(excerptId)
                .orElseThrow(() -> new IllegalArgumentException("Excerpt not found: " + excerptId));

        if (dto.getComposerId() == null || !composerRepository.existsById(dto.getComposerId())) {
            throw new IllegalArgumentException("Composer not found: " + dto.getComposerId());
        }
        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new IllegalArgumentException("Excerpt name must not be blank");
        }

        excerpt.setComposerId(dto.getComposerId());
        excerpt.setWorkId(dto.getWorkId());
        excerpt.setName(dto.getName().trim());
        excerpt.setCompositionYear(dto.getCompositionYear());
        excerpt.setDescription(dto.getDescription());
        excerpt.setStatus(ExcerptStatus.ACTIVE);

        if (excerpt.isPointsEligible() && !excerpt.isPointsAwarded()) {
            userRepository.findById(excerpt.getUploadedByUserId()).ifPresent(submitter -> {
                submitter.setTotalPoints(submitter.getTotalPoints() + 2);
                userRepository.save(submitter);
            });
            excerpt.setPointsAwarded(true);
        }

        excerptRepository.save(excerpt);
    }

    @Transactional
    public void scheduleTomorrow(Long excerptId) {
        Excerpt excerpt = excerptRepository.findById(excerptId)
                .orElseThrow(() -> new IllegalArgumentException("Excerpt not found: " + excerptId));
        if (excerpt.getStatus() != ExcerptStatus.ACTIVE) {
            throw new IllegalArgumentException("Only active excerpts can be scheduled");
        }
        LocalDate tomorrow = LocalDate.now(ZoneId.of("America/Vancouver")).plusDays(1);
        ExcerptDay day = excerptDayRepository.findById(tomorrow).orElse(new ExcerptDay());
        day.setDate(tomorrow);
        day.setExcerpt(excerpt);
        excerptDayRepository.save(day);
    }

    public DailyChallengesDto getDailyChallenges() {
        LocalDate today = LocalDate.now(ZoneId.of("America/Vancouver"));
        LocalDate tomorrow = today.plusDays(1);
        DailyChallengesDto dto = new DailyChallengesDto();
        excerptDayRepository.findById(today).ifPresent(ed -> dto.setToday(toEntry(ed)));
        excerptDayRepository.findById(tomorrow).ifPresent(ed -> dto.setTomorrow(toEntry(ed)));
        return dto;
    }

    private DailyChallengesDto.Entry toEntry(ExcerptDay ed) {
        Excerpt excerpt = ed.getExcerpt();
        String composerName = composerRepository.findById(excerpt.getComposerId())
                .map(Composer::getCompleteName)
                .orElse("Unknown composer");
        return new DailyChallengesDto.Entry(excerpt.getExcerptId(), excerpt.getName(), composerName, ed.getChallengeNumber());
    }

    private ExcerptReviewDto toDto(Excerpt excerpt) {
        String composerName = composerRepository.findById(excerpt.getComposerId())
                .map(Composer::getCompleteName)
                .orElse("Unknown composer");

        String uploaderUsername = userRepository.findById(excerpt.getUploadedByUserId())
                .map(User::getDisplayUsername)
                .orElse("Unknown user");

        String audioUrl = audioBaseUrl + "/" + excerpt.getFilename();
        String dateUploaded = excerpt.getDateUploaded() != null ? excerpt.getDateUploaded().toString() : null;

        return new ExcerptReviewDto(
                excerpt.getExcerptId(),
                excerpt.getComposerId(),
                composerName,
                excerpt.getWorkId(),
                excerpt.getUploadedByUserId(),
                uploaderUsername,
                excerpt.getName(),
                audioUrl,
                excerpt.getCompositionYear(),
                excerpt.getDescription(),
                dateUploaded,
                excerpt.getTimesUsed(),
                excerpt.getStatus()
        );
    }
}
