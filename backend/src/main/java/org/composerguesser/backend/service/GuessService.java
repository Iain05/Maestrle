package org.composerguesser.backend.service;

import org.composerguesser.backend.dto.GuessRequestDto;
import org.composerguesser.backend.dto.GuessResultDto;
import org.composerguesser.backend.model.*;
import org.composerguesser.backend.repository.ComposerRepository;
import org.composerguesser.backend.repository.ExcerptDayRepository;
import org.composerguesser.backend.repository.ExcerptRepository;
import org.composerguesser.backend.repository.UserGuessRepository;
import org.composerguesser.backend.repository.UserPointRepository;
import org.composerguesser.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Core game logic for processing composer guesses.
 * <p>
 * For authenticated users, every guess is persisted to {@code tbl_user_guess}.
 * On a correct guess against today's challenge, points are awarded via
 * {@link #tryAwardPoints} — but this step is intentionally separate so that
 * future replay modes (e.g. past daily challenges) can reuse guess evaluation
 * without granting points.
 * </p>
 * Points scale: {@code 11 - guessNumber}, giving 10 points on the first guess
 * down to 6 on the fifth.
 */
@Service
public class GuessService {

    private static final Logger log = LoggerFactory.getLogger(GuessService.class);

    private final ExcerptRepository excerptRepository;
    private final ExcerptDayRepository excerptDayRepository;
    private final ComposerRepository composerRepository;
    private final UserRepository userRepository;
    private final UserPointRepository userPointRepository;
    private final UserGuessRepository userGuessRepository;
    private final Clock clock;

    public GuessService(ExcerptRepository excerptRepository, ExcerptDayRepository excerptDayRepository,
                        ComposerRepository composerRepository, UserRepository userRepository,
                        UserPointRepository userPointRepository, UserGuessRepository userGuessRepository,
                        Clock clock) {
        this.excerptRepository = excerptRepository;
        this.excerptDayRepository = excerptDayRepository;
        this.composerRepository = composerRepository;
        this.userRepository = userRepository;
        this.userPointRepository = userPointRepository;
        this.userGuessRepository = userGuessRepository;
        this.clock = clock;
    }

    /**
     * Processes a single composer guess against today's daily challenge.
     *
     * @param request contains the {@code excerptId} and {@code composerId} being guessed
     * @param user    the authenticated user, or {@code null} if not logged in
     * @return hint feedback along with {@code pointsEarned} (0 if unauthenticated or incorrect)
     * @throws IllegalArgumentException if the excerpt doesn't match today's challenge, or IDs are invalid
     */
    @Transactional
    public GuessResultDto processGuess(GuessRequestDto request, User user) {
        LocalDate today = LocalDate.now(clock);

        Long dailyExcerptId = excerptDayRepository.findById(today)
                .map(day -> day.getExcerpt().getExcerptId())
                .orElseThrow(() -> new IllegalArgumentException("No daily challenge found for today"));

        if (!request.getExcerptId().equals(dailyExcerptId)) {
            throw new IllegalArgumentException("Excerpt does not match today's daily challenge");
        }

        Excerpt excerpt = excerptRepository.findById(request.getExcerptId())
                .orElseThrow(() -> new IllegalArgumentException("Excerpt not found"));
        Composer target = composerRepository.findById(excerpt.getComposerId())
                .orElseThrow(() -> new IllegalArgumentException("Target composer not found"));
        Composer guessed = composerRepository.findById(request.getComposerId())
                .orElseThrow(() -> new IllegalArgumentException("Guessed composer not found"));

        boolean correct = guessed.getComposerId().equals(target.getComposerId());

        log.info("Guess submitted: user={} guessed=\"{}\" correct={}",
                user != null ? user.getDisplayUsername() : "anonymous",
                guessed.getCompleteName(), correct);

        int pointsEarned = 0;
        int newStreak = user != null ? user.getCurrentStreak() : 0;

        if (user != null) {
            if (userGuessRepository.existsByUserIdAndDateAndComposerId(user.getUserId(), today, guessed.getComposerId())) {
                throw new IllegalArgumentException("You have already guessed that composer");
            }
            int guessNumber = recordGuess(user, excerpt, guessed, today);

            if (correct) {
                pointsEarned = tryAwardPoints(user, excerpt, guessNumber, today);
                newStreak = user.getCurrentStreak();
            }
        }

        return new GuessResultDto(
                correct,
                guessed.getLastName(),
                guessed.getBirthYear(),
                guessed.getEra().name(),
                guessed.getNationality(),
                correct ? "CORRECT" : "WRONG",
                getYearHint(guessed.getBirthYear(), target.getBirthYear()),
                getEraHint(guessed.getEra(), target.getEra()),
                guessed.getNationality().equals(target.getNationality()) ? "CORRECT" : "WRONG",
                excerpt.getName(),
                target.getCompleteName(),
                excerpt.getCompositionYear(),
                excerpt.getDescription(),
                pointsEarned,
                newStreak
        );
    }

    /**
     * Returns all of the authenticated user's guesses for today's daily challenge, ordered by guess number.
     * Used on page load to restore a session in progress.
     * Returns an empty list if the user is null or no daily challenge exists for today.
     */
    public List<GuessResultDto> getGuessHistory(User user) {
        if (user == null) return List.of();

        LocalDate today = LocalDate.now(clock);
        return excerptDayRepository.findById(today)
                .map(day -> {
                    Excerpt excerpt = day.getExcerpt();
                    Composer target = composerRepository.findById(excerpt.getComposerId()).orElse(null);
                    if (target == null) return List.<GuessResultDto>of();

                    Integer rawPoints = userPointRepository.findDailyPointsByUserIdAndDate(user.getUserId(), today);
                    int todayPoints = rawPoints != null ? rawPoints : 0;

                    return userGuessRepository
                            .findByUserIdAndDateOrderByGuessNumber(user.getUserId(), today)
                            .stream()
                            .map(ug -> buildHistoryEntry(ug, excerpt, target, todayPoints))
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
                })
                .orElse(List.of());
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /** Persists a {@link UserGuess} and returns the 1-based guess number for today. */
    private int recordGuess(User user, Excerpt excerpt, Composer guessed, LocalDate date) {
        int guessNumber = userGuessRepository.countByUserIdAndDate(user.getUserId(), date) + 1;
        userGuessRepository.save(new UserGuess(user.getUserId(), excerpt.getExcerptId(), guessed.getComposerId(), guessNumber, date));
        return guessNumber;
    }

    /**
     * Awards points for a correct guess if the user is eligible.
     * Ineligible when the user uploaded this excerpt or has already earned points today.
     * On a successful award, also updates the user's streak and persists the user.
     *
     * @return points awarded, or 0 if ineligible
     */
    private int tryAwardPoints(User user, Excerpt excerpt, int guessNumber, LocalDate date) {
        boolean ownSubmission = excerpt.getUploadedByUserId().equals(user.getUserId());
        if (ownSubmission || userPointRepository.existsByUserIdAndExcerptDayDate(user.getUserId(), date)) {
            return 0;
        }
        int points = 11 - guessNumber;
        userPointRepository.save(new UserPoint(user.getUserId(), date, points, LocalDateTime.now(clock)));
        user.setTotalPoints(user.getTotalPoints() + points);
        updateStreak(user, date);
        userRepository.save(user);
        return points;
    }

    /**
     * Increments the user's streak if they played (or uploaded) yesterday's challenge,
     * otherwise resets it to 1.
     * <p>
     * A user who uploaded yesterday's excerpt couldn't earn a point for it, so the
     * absence of a point entry must not be treated as a missed day.
     * </p>
     */
    private void updateStreak(User user, LocalDate date) {
        boolean hadYesterday = userPointRepository.existsByUserIdAndExcerptDayDate(user.getUserId(), date.minusDays(1));
        if (!hadYesterday) {
            hadYesterday = excerptDayRepository.findById(date.minusDays(1))
                    .map(day -> day.getExcerpt().getUploadedByUserId().equals(user.getUserId()))
                    .orElse(false);
        }
        user.setCurrentStreak(hadYesterday ? user.getCurrentStreak() + 1 : 1);
    }

    private String getYearHint(int guessedYear, int targetYear) {
        int diff = Math.abs(guessedYear - targetYear);
        if (diff == 0)       return "CORRECT";
        if (diff <= 15)      return guessedYear < targetYear ? "CLOSE_LOW" : "CLOSE_HIGH";
        return               guessedYear < targetYear ? "TOO_LOW" : "TOO_HIGH";
    }

    private String getEraHint(Era guessed, Era target) {
        if (guessed == target) return "CORRECT";
        return Math.abs(guessed.ordinal() - target.ordinal()) == 1 ? "CLOSE" : "WRONG";
    }

    private GuessResultDto buildHistoryEntry(UserGuess ug, Excerpt excerpt, Composer target, int todayPoints) {
        Composer guessed = composerRepository.findById(ug.getComposerId()).orElse(null);
        if (guessed == null) return null;
        boolean correct = guessed.getComposerId().equals(target.getComposerId());
        return new GuessResultDto(
                correct,
                guessed.getLastName(),
                guessed.getBirthYear(),
                guessed.getEra().name(),
                guessed.getNationality(),
                correct ? "CORRECT" : "WRONG",
                getYearHint(guessed.getBirthYear(), target.getBirthYear()),
                getEraHint(guessed.getEra(), target.getEra()),
                guessed.getNationality().equals(target.getNationality()) ? "CORRECT" : "WRONG",
                excerpt.getName(),
                target.getCompleteName(),
                excerpt.getCompositionYear(),
                excerpt.getDescription(),
                correct ? todayPoints : 0,
                0
        );
    }
}
