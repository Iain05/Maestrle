package org.composerguesser.backend.controller;

import org.composerguesser.backend.dto.LeaderboardProjection;
import org.composerguesser.backend.repository.UserPointRepository;
import org.composerguesser.backend.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;

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
}
