package org.composerguesser.backend.repository;

import org.composerguesser.backend.dto.LeaderboardProjection;
import org.composerguesser.backend.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);

    /**
     * Returns all users ranked by total points descending, with their current streak.
     */
    @Query(value = "SELECT username, total_points AS points, current_streak AS streak FROM tbl_user ORDER BY total_points DESC",
           countQuery = "SELECT COUNT(*) FROM tbl_user",
           nativeQuery = true)
    Page<LeaderboardProjection> findAllTimeLeaderboard(Pageable pageable);

    /**
     * Returns 1-based all-time rank for the given user (number of users with strictly more points + 1).
     */
    @Query(value = "SELECT COUNT(*) + 1 FROM tbl_user WHERE total_points > (SELECT total_points FROM tbl_user WHERE user_id = :userId)",
           nativeQuery = true)
    int findAllTimeRankByUserId(@Param("userId") Long userId);

    /**
     * Resets current_streak to 0 for every user who has a streak but no point entry for yesterday,
     * excluding the submitter of yesterday's excerpt (their streak is handled separately).
     * Pass -1 as excludeUserId if there is no submitter to exclude.
     * Returns the number of rows updated.
     */
    @Modifying
    @Query(value = """
            UPDATE tbl_user SET current_streak = 0
            WHERE current_streak > 0
            AND user_id NOT IN (
                SELECT user_id FROM tbl_user_point WHERE excerpt_day_date = :yesterday
            )
            AND user_id != :excludeUserId
            """, nativeQuery = true)
    int resetExpiredStreaks(@Param("yesterday") LocalDate yesterday, @Param("excludeUserId") Long excludeUserId);

    /**
     * Increments current_streak by 1 for the given user if they have a point entry for yesterday
     * (streak continues), otherwise sets it to 1 (new streak starts).
     * Used to credit the submitter of today's daily challenge at midnight.
     */
    @Modifying
    @Query(value = """
            UPDATE tbl_user SET current_streak =
                CASE WHEN EXISTS (
                    SELECT 1 FROM tbl_user_point WHERE user_id = :userId AND excerpt_day_date = :yesterday
                ) THEN current_streak + 1 ELSE 1 END
            WHERE user_id = :userId
            """, nativeQuery = true)
    void incrementStreakForSubmitter(@Param("userId") Long userId, @Param("yesterday") LocalDate yesterday);
}
