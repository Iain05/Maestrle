package org.composerguesser.backend.controller;

import org.composerguesser.backend.dto.ArchiveChallengeDto;
import org.composerguesser.backend.dto.DailyChallengeDto;
import org.composerguesser.backend.model.ExcerptDay;
import org.composerguesser.backend.model.User;
import org.composerguesser.backend.repository.ArchiveStatusProjection;
import org.composerguesser.backend.repository.ExcerptDayRepository;
import org.composerguesser.backend.repository.UserGuessRepository;
import org.composerguesser.backend.repository.UserRepository;
import org.composerguesser.backend.service.ExcerptSubmitService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/excerpt")
public class ExcerptController {

    private static final ZoneId PACIFIC = ZoneId.of("America/Vancouver");

    private final ExcerptDayRepository excerptDayRepository;
    private final ExcerptSubmitService excerptSubmitService;
    private final UserRepository userRepository;
    private final UserGuessRepository userGuessRepository;
    private final String audioBaseUrl;

    public ExcerptController(ExcerptDayRepository excerptDayRepository,
                             ExcerptSubmitService excerptSubmitService,
                             UserRepository userRepository,
                             UserGuessRepository userGuessRepository,
                             @Value("${audio.base-url}") String audioBaseUrl) {
        this.excerptDayRepository = excerptDayRepository;
        this.excerptSubmitService = excerptSubmitService;
        this.userRepository = userRepository;
        this.userGuessRepository = userGuessRepository;
        this.audioBaseUrl = audioBaseUrl;
    }

    /**
     * Returns today's daily challenge excerpt, resolved using the America/Vancouver timezone.
     * The audio URL is constructed from the configured {@code audio.base-url} and the excerpt's filename.
     *
     * @return 200 with {@link DailyChallengeDto} containing excerptId and audioUrl,
     *         or 404 if no challenge has been scheduled for today
     */
    @GetMapping("/daily-challenge")
    public ResponseEntity<DailyChallengeDto> getDailyChallenge(@AuthenticationPrincipal User user) {
        LocalDate today = LocalDate.now(PACIFIC);
        return excerptDayRepository.findById(today)
                .map(day -> {
                    boolean submittedByCurrentUser = user != null &&
                            user.getUserId().equals(day.getExcerpt().getUploadedByUserId());
                    String uploaderUsername = userRepository.findById(day.getExcerpt().getUploadedByUserId())
                            .map(User::getDisplayUsername)
                            .orElse("Unknown");
                    return ResponseEntity.ok(new DailyChallengeDto(
                            day.getExcerpt().getExcerptId(),
                            audioBaseUrl + "/" + day.getExcerpt().getFilename(),
                            day.getChallengeNumber(),
                            today.toString(),
                            submittedByCurrentUser,
                            uploaderUsername
                    ));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Returns the daily challenge for a specific past date.
     * The date must be in the past (strictly before today in the America/Vancouver timezone).
     *
     * @param date ISO date string (YYYY-MM-DD)
     * @return 200 with {@link DailyChallengeDto}, 400 if the date is today/future or malformed,
     *         or 404 if no challenge was scheduled for that date
     */
    @GetMapping("/challenge/{date}")
    public ResponseEntity<DailyChallengeDto> getChallengeByDate(@PathVariable String date,
                                                                 @AuthenticationPrincipal User user) {
        LocalDate targetDate;
        try {
            targetDate = LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().build();
        }

        if (!targetDate.isBefore(LocalDate.now(PACIFIC))) {
            return ResponseEntity.badRequest().build();
        }

        return excerptDayRepository.findById(targetDate)
                .map(day -> {
                    boolean submittedByCurrentUser = user != null &&
                            user.getUserId().equals(day.getExcerpt().getUploadedByUserId());
                    String uploaderUsername = userRepository.findById(day.getExcerpt().getUploadedByUserId())
                            .map(User::getDisplayUsername)
                            .orElse("Unknown");
                    return ResponseEntity.ok(new DailyChallengeDto(
                            day.getExcerpt().getExcerptId(),
                            audioBaseUrl + "/" + day.getExcerpt().getFilename(),
                            day.getChallengeNumber(),
                            targetDate.toString(),
                            submittedByCurrentUser,
                            uploaderUsername
                    ));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Returns all past daily challenges (before today, most recent first), each annotated with
     * the authenticated user's play status ({@code guessCount}, {@code correct}).
     * For unauthenticated requests, {@code guessCount} and {@code correct} are {@code null} for every entry.
     */
    @GetMapping("/archive")
    public ResponseEntity<List<ArchiveChallengeDto>> getArchive(@AuthenticationPrincipal User user) {
        LocalDate today = LocalDate.now(PACIFIC);
        List<ExcerptDay> days = excerptDayRepository.findByDateBeforeOrderByDateDesc(today);

        Map<String, ArchiveStatusProjection> statuses = user == null ? Map.of() :
                userGuessRepository.findArchiveStatuses(user.getUserId())
                        .stream()
                        .collect(Collectors.toMap(ArchiveStatusProjection::getDate, p -> p));

        List<ArchiveChallengeDto> result = days.stream()
                .map(day -> {
                    String date = day.getDate().toString();
                    ArchiveStatusProjection status = statuses.get(date);
                    return new ArchiveChallengeDto(
                            date,
                            day.getChallengeNumber(),
                            status != null ? status.getGuessCount() : 0,
                            status != null && status.getCorrect()
                    );
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    /**
     * Returns how many point-eligible excerpt submissions the authenticated user has remaining today (0–5).
     * Submissions are point-eligible up to a cap of 5 per calendar day (America/Vancouver).
     *
     * @return 200 with {@code { "remaining": N }}, or 401 if not logged in
     */
    @GetMapping("/submission-points-remaining")
    public ResponseEntity<?> getSubmissionPointsRemaining(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "You must be logged in"));
        }
        int remaining = excerptSubmitService.getSubmissionPointsRemaining(user);
        return ResponseEntity.ok(Map.of("remaining", remaining));
    }

    /**
     * Accepts a trimmed WAV file and metadata from an authenticated user, creates a draft
     * excerpt record, and writes the audio to the configured storage volume.
     *
     * <p>The audio file is saved before the database record is inserted. If the insert fails,
     * the file is deleted immediately so no orphaned files can accumulate. Retries are always
     * safe because a fresh UUID filename is generated on each request.</p>
     *
     * @param audio       trimmed WAV blob from the frontend
     * @param composerId  ID of the composer
     * @param workId      optional ID of the work within that composer's catalogue
     * @param title       display name for the excerpt
     * @param description optional free-text description
     * @param user        injected from JWT; null if not authenticated
     * @return 201 on success, 400 on bad input, 401 if not logged in, 500 on server error
     */
    @PostMapping("/submit")
    public ResponseEntity<?> submitExcerpt(
            @RequestParam("audio") MultipartFile audio,
            @RequestParam("composerId") Long composerId,
            @RequestParam(value = "workId", required = false) Long workId,
            @RequestParam("title") String title,
            @RequestParam(value = "compositionYear", required = false) Integer compositionYear,
            @RequestParam(value = "description", required = false) String description,
            @AuthenticationPrincipal User user) {

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "You must be logged in to submit an excerpt"));
        }

        try {
            excerptSubmitService.submit(audio, composerId, workId, title, compositionYear, description, user);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
