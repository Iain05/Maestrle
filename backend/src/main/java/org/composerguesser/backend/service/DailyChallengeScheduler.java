package org.composerguesser.backend.service;

import org.composerguesser.backend.model.Excerpt;
import org.composerguesser.backend.model.ExcerptDay;
import org.composerguesser.backend.repository.ExcerptDayRepository;
import org.composerguesser.backend.repository.ExcerptRepository;
import org.composerguesser.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;

@Component
public class DailyChallengeScheduler {

    private static final Logger log = LoggerFactory.getLogger(DailyChallengeScheduler.class);
    private static final ZoneId VANCOUVER = ZoneId.of("America/Vancouver");

    private final ExcerptDayRepository excerptDayRepository;
    private final ExcerptRepository excerptRepository;
    private final UserRepository userRepository;

    public DailyChallengeScheduler(ExcerptDayRepository excerptDayRepository,
                                   ExcerptRepository excerptRepository,
                                   UserRepository userRepository) {
        this.excerptDayRepository = excerptDayRepository;
        this.excerptRepository = excerptRepository;
        this.userRepository = userRepository;
    }

    /**
     * Scheduled nightly at 23:59 (America/Vancouver). Ensures tomorrow's daily challenge
     * is assigned before the day rolls over. No-ops if tomorrow already has an entry.
     */
    @Scheduled(cron = "0 59 23 * * *", zone = "America/Vancouver")
    @Transactional
    public void scheduleTomorrowsChallenge() {
        ensureChallengeExists(LocalDate.now(VANCOUVER).plusDays(1));
    }

    /**
     * Scheduled nightly at 00:01 (America/Vancouver). Resets {@code current_streak} to 0
     * for any user who has a streak but did not earn points yesterday.
     */
    @Scheduled(cron = "0 1 0 * * *", zone = "America/Vancouver")
    @Transactional
    public void expireStreaks() {
        resetExpiredStreaks();
    }

    /**
     * Runs once on application startup to catch up on any maintenance missed while the backend
     * was down. Ensures both today and tomorrow have a daily challenge assigned, then runs
     * streak expiry in case the midnight job was skipped.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void onStartup() {
        LocalDate today = LocalDate.now(VANCOUVER);
        ensureChallengeExists(today);
        ensureChallengeExists(today.plusDays(1));
        resetExpiredStreaks();
    }

    /**
     * Assigns a daily challenge for {@code date} if one does not already exist.
     * Selects the excerpt with the lowest {@code times_used} count (random tiebreak)
     * and increments its counter to maintain round-robin fairness across the pool.
     */
    private void ensureChallengeExists(LocalDate date) {
        if (excerptDayRepository.existsById(date)) {
            log.info("Daily challenge for {} already exists — skipping.", date);
            return;
        }

        Excerpt excerpt = excerptRepository.findLeastUsedRandom().orElse(null);
        if (excerpt == null) {
            log.error("No excerpts in pool — cannot schedule challenge for {}.", date);
            return;
        }

        excerpt.setTimesUsed(excerpt.getTimesUsed() + 1);
        excerptRepository.save(excerpt);

        ExcerptDay day = new ExcerptDay();
        day.setDate(date);
        day.setExcerpt(excerpt);
        excerptDayRepository.save(day);

        log.info("Scheduled daily challenge for {}: excerpt {} (times_used now {}).",
                date, excerpt.getExcerptId(), excerpt.getTimesUsed());
    }

    /**
     * Resets {@code current_streak} to 0 for every user who has a streak but no
     * {@code tbl_user_point} entry for yesterday. Logs how many users were affected.
     */
    private void resetExpiredStreaks() {
        LocalDate yesterday = LocalDate.now(VANCOUVER).minusDays(1);
        int count = userRepository.resetExpiredStreaks(yesterday);
        if (count > 0) {
            log.info("Expired streaks for {} user(s) who missed {}.", count, yesterday);
        } else {
            log.info("No streaks expired for {}.", yesterday);
        }
    }
}
