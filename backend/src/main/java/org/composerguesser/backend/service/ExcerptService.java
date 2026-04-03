package org.composerguesser.backend.service;

import org.composerguesser.backend.dto.ArchiveChallengeDto;
import org.composerguesser.backend.dto.DailyChallengeDto;
import org.composerguesser.backend.model.ExcerptDay;
import org.composerguesser.backend.model.User;
import org.composerguesser.backend.repository.ArchiveStatusProjection;
import org.composerguesser.backend.repository.ExcerptDayRepository;
import org.composerguesser.backend.repository.UserGuessRepository;
import org.composerguesser.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ExcerptService {

    private static final ZoneId PACIFIC = ZoneId.of("America/Vancouver");

    private final ExcerptDayRepository excerptDayRepository;
    private final UserRepository userRepository;
    private final UserGuessRepository userGuessRepository;
    private final String audioBaseUrl;

    public ExcerptService(ExcerptDayRepository excerptDayRepository,
                          UserRepository userRepository,
                          UserGuessRepository userGuessRepository,
                          @Value("${audio.base-url}") String audioBaseUrl) {
        this.excerptDayRepository = excerptDayRepository;
        this.userRepository = userRepository;
        this.userGuessRepository = userGuessRepository;
        this.audioBaseUrl = audioBaseUrl;
    }

    public Optional<DailyChallengeDto> getDailyChallenge(User user) {
        LocalDate today = LocalDate.now(PACIFIC);
        return excerptDayRepository.findById(today)
                .map(day -> buildDailyChallengeDto(day, today.toString(), user));
    }

    public Optional<DailyChallengeDto> getChallengeByDate(LocalDate date, User user) {
        return excerptDayRepository.findById(date)
                .map(day -> buildDailyChallengeDto(day, date.toString(), user));
    }

    public List<ArchiveChallengeDto> getArchive(User user) {
        LocalDate today = LocalDate.now(PACIFIC);
        List<ExcerptDay> days = excerptDayRepository.findByDateBeforeOrderByDateDesc(today);

        Map<String, ArchiveStatusProjection> statuses = user == null ? Map.of() :
                userGuessRepository.findArchiveStatuses(user.getUserId())
                        .stream()
                        .collect(Collectors.toMap(ArchiveStatusProjection::getDate, p -> p));

        return days.stream()
                .map(day -> {
                    String date = day.getDate().toString();
                    ArchiveStatusProjection status = statuses.get(date);
                    return new ArchiveChallengeDto(
                            date,
                            day.getChallengeNumber(),
                            status != null ? status.getGuessCount() : 0,
                            status != null && status.getCorrect(),
                            user != null && day.getExcerpt().getUploadedByUserId().equals(user.getUserId())
                    );
                })
                .collect(Collectors.toList());
    }

    private DailyChallengeDto buildDailyChallengeDto(ExcerptDay day, String date, User user) {
        boolean submittedByCurrentUser = user != null &&
                user.getUserId().equals(day.getExcerpt().getUploadedByUserId());
        String uploaderUsername = userRepository.findById(day.getExcerpt().getUploadedByUserId())
                .map(User::getDisplayUsername)
                .orElse("Unknown");
        return new DailyChallengeDto(
                day.getExcerpt().getExcerptId(),
                audioBaseUrl + "/" + day.getExcerpt().getFilename(),
                day.getChallengeNumber(),
                date,
                submittedByCurrentUser,
                uploaderUsername
        );
    }
}
