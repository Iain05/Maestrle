package org.composerguesser.backend.controller;

import org.composerguesser.backend.dto.LeaderboardProjection;
import org.composerguesser.backend.dto.MyRankDto;
import org.composerguesser.backend.model.User;
import org.composerguesser.backend.repository.UserPointRepository;
import org.composerguesser.backend.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;

@RestController
@RequestMapping("/leaderboard")
public class LeaderboardController {

    private static final int MAX_PAGE_SIZE = 50;
    private static final ZoneId VANCOUVER = ZoneId.of("America/Vancouver");

    private final UserPointRepository userPointRepository;
    private final UserRepository userRepository;

    public LeaderboardController(UserPointRepository userPointRepository, UserRepository userRepository) {
        this.userPointRepository = userPointRepository;
        this.userRepository = userRepository;
    }

    /**
     * Returns the leaderboard for today's daily challenge, ordered by points descending.
     *
     * @param page zero-based page index
     * @param size number of entries per page (capped at 50)
     */
    @GetMapping("/daily")
    public Page<LeaderboardProjection> getDaily(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        LocalDate today = LocalDate.now(VANCOUVER);
        return userPointRepository.findDailyLeaderboard(today, PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE)));
    }

    @GetMapping("/weekly")
    public Page<LeaderboardProjection> getWeekly(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        LocalDate today = LocalDate.now(VANCOUVER);
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return userPointRepository.findWeeklyLeaderboard(weekStart, today, PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE)));
    }

    /**
     * Returns the all-time leaderboard ordered by total points descending.
     *
     * @param page zero-based page index
     * @param size number of entries per page (capped at 50)
     */
    @GetMapping("/all-time")
    public Page<LeaderboardProjection> getAllTime(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return userRepository.findAllTimeLeaderboard(PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE)));
    }

    /**
     * Returns the authenticated user's all-time and daily rank and points.
     * dailyRank and dailyPoints are null if the user hasn't played today.
     */
    @GetMapping("/my-rank")
    public ResponseEntity<MyRankDto> getMyRank(@AuthenticationPrincipal User user) {
        if (user == null) return ResponseEntity.status(401).build();
        LocalDate today = LocalDate.now(VANCOUVER);
        int allTimeRank = userRepository.findAllTimeRankByUserId(user.getUserId());
        Integer dailyPoints = userPointRepository.findDailyPointsByUserIdAndDate(user.getUserId(), today);
        Integer dailyRank = dailyPoints != null
                ? userPointRepository.findDailyRankByUserIdAndDate(user.getUserId(), today)
                : null;
        return ResponseEntity.ok(new MyRankDto(user.getDisplayUsername(), allTimeRank, user.getTotalPoints(), dailyRank, dailyPoints, user.getCurrentStreak()));
    }
}
