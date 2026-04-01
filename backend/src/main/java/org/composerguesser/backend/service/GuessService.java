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
 * If the guess is correct, points are awarded to {@code tbl_user_point} and the
 * user's {@code total_points} is updated — but only once per day per user.
 * </p>
 * <p>
 * Points scale linearly: {@code 11 - guessNumber}, giving 10 points for a
 * first-guess win down to 6 points for a fifth-guess win.
 * </p>
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
     * <p>
     * Validates that the submitted {@code excerptId} matches today's active daily challenge
     * (resolved in {@code America/Vancouver} timezone) before evaluating the guess.
     * </p>
     * <p>
     * For authenticated users:
     * <ul>
     *   <li>The guess is recorded in {@code tbl_user_guess} with an incremented guess number.</li>
     *   <li>On a correct guess, points ({@code 11 - guessNumber}) are awarded once per day.</li>
     * </ul>
     * Unauthenticated users receive identical hint feedback but no persistence occurs.
     * </p>
     *
     * @param request contains the {@code excerptId} and {@code composerId} being guessed
     * @param user    the authenticated user, or {@code null} if not logged in
     * @return hint feedback for the guess along with {@code pointsEarned} (0 if unauthenticated or incorrect)
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
                guessed.getCompleteName(),
                correct);

        String composerHint = correct ? "CORRECT" : "WRONG";

        String yearHint;
        int yearDiff = Math.abs(guessed.getBirthYear() - target.getBirthYear());
        if (guessed.getBirthYear().equals(target.getBirthYear())) {
            yearHint = "CORRECT";
        } else if (yearDiff <= 15) {
            yearHint = guessed.getBirthYear() < target.getBirthYear() ? "CLOSE_LOW" : "CLOSE_HIGH";
        } else if (guessed.getBirthYear() < target.getBirthYear()) {
            yearHint = "TOO_LOW";
        } else {
            yearHint = "TOO_HIGH";
        }

        String eraHint = getEraHint(guessed.getEra(), target.getEra());
        String nationalityHint = guessed.getNationality().equals(target.getNationality()) ? "CORRECT" : "WRONG";

        int pointsEarned = 0;
        int newStreak = user != null ? user.getCurrentStreak() : 0;
        if (user != null) {
            if (userGuessRepository.existsByUserIdAndDateAndComposerId(user.getUserId(), today, guessed.getComposerId())) {
                throw new IllegalArgumentException("You have already guessed that composer");
            }
            int guessNumber = userGuessRepository.countByUserIdAndDate(user.getUserId(), today) + 1;
            userGuessRepository.save(new UserGuess(user.getUserId(), excerpt.getExcerptId(), guessed.getComposerId(), guessNumber, today));

            if (correct) {
                boolean ownSubmission = excerpt.getUploadedByUserId().equals(user.getUserId());
                if (!ownSubmission && !userPointRepository.existsByUserIdAndExcerptDayDate(user.getUserId(), today)) {
                    int points = 11 - guessNumber;
                    userPointRepository.save(new UserPoint(user.getUserId(), today, points, LocalDateTime.now(clock)));
                    user.setTotalPoints(user.getTotalPoints() + points);
                    boolean hadYesterday = userPointRepository.existsByUserIdAndExcerptDayDate(user.getUserId(), today.minusDays(1));
                    if (!hadYesterday) {
                        // User may have been yesterday's submitter — they can't earn points on their own
                        // excerpt, so the absence of a point entry doesn't mean they broke their streak.
                        hadYesterday = excerptDayRepository.findById(today.minusDays(1))
                                .map(day -> day.getExcerpt().getUploadedByUserId().equals(user.getUserId()))
                                .orElse(false);
                    }
                    user.setCurrentStreak(hadYesterday ? user.getCurrentStreak() + 1 : 1);
                    userRepository.save(user);
                    pointsEarned = points;
                    newStreak = user.getCurrentStreak();
                }
            }
        }

        return new GuessResultDto(
                correct,
                guessed.getLastName(),
                guessed.getBirthYear(),
                guessed.getEra().name(),
                guessed.getNationality(),
                composerHint,
                yearHint,
                eraHint,
                nationalityHint,
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
     *
     * @param user the authenticated user, or {@code null} if not logged in
     * @return ordered list of guess results with hint data; {@code pointsEarned} is 0 for all entries
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
                            .map(ug -> {
                                Composer guessed = composerRepository.findById(ug.getComposerId()).orElse(null);
                                if (guessed == null) return null;
                                boolean correct = guessed.getComposerId().equals(target.getComposerId());
                                String yearHint;
                                int yearDiff = Math.abs(guessed.getBirthYear() - target.getBirthYear());
                                if (guessed.getBirthYear().equals(target.getBirthYear())) yearHint = "CORRECT";
                                else if (yearDiff <= 15) yearHint = guessed.getBirthYear() < target.getBirthYear() ? "CLOSE_LOW" : "CLOSE_HIGH";
                                else if (guessed.getBirthYear() < target.getBirthYear()) yearHint = "TOO_LOW";
                                else yearHint = "TOO_HIGH";
                                return new GuessResultDto(
                                        correct,
                                        guessed.getLastName(),
                                        guessed.getBirthYear(),
                                        guessed.getEra().name(),
                                        guessed.getNationality(),
                                        correct ? "CORRECT" : "WRONG",
                                        yearHint,
                                        getEraHint(guessed.getEra(), target.getEra()),
                                        guessed.getNationality().equals(target.getNationality()) ? "CORRECT" : "WRONG",
                                        excerpt.getName(),
                                        target.getCompleteName(),
                                        excerpt.getCompositionYear(),
                                        excerpt.getDescription(),
                                        correct ? todayPoints : 0,
                                        0
                                );
                            })
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
                })
                .orElse(List.of());
    }

    /**
     * Computes the era hint by comparing ordinal positions of the two eras.
     *
     * @param guessed the era of the guessed composer
     * @param target  the era of the correct composer
     * @return {@code "correct"} if equal, {@code "close"} if adjacent, {@code "wrong"} otherwise
     */
    private String getEraHint(Era guessed, Era target) {
        if (guessed == target) return "CORRECT";
        return Math.abs(guessed.ordinal() - target.ordinal()) == 1 ? "CLOSE" : "WRONG";
    }
}
