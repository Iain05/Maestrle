package org.composerguesser.backend.repository;

import org.composerguesser.backend.dto.LeaderboardProjection;
import org.composerguesser.backend.model.UserPoint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface UserPointRepository extends JpaRepository<UserPoint, Long> {
    boolean existsByUserIdAndExcerptDayDate(Long userId, LocalDate date);

    /**
     * Returns all users who scored on the given date, ranked by points descending, with their current streak.
     */
    @Query(value = "SELECT u.username, up.points, u.current_streak AS streak, u.total_points AS totalPoints " +
                   "FROM tbl_user_point up JOIN tbl_user u ON up.user_id = u.user_id " +
                   "WHERE up.excerpt_day_date = :date ORDER BY up.points DESC",
           countQuery = "SELECT COUNT(*) FROM tbl_user_point WHERE excerpt_day_date = :date",
           nativeQuery = true)
    Page<LeaderboardProjection> findDailyLeaderboard(@Param("date") LocalDate date, Pageable pageable);

    /**
     * Returns 1-based daily rank for the given user on the given date, or null if they haven't played.
     */
    @Query(value = "SELECT COUNT(*) + 1 FROM tbl_user_point " +
                   "WHERE excerpt_day_date = :date AND points > " +
                   "(SELECT points FROM tbl_user_point WHERE user_id = :userId AND excerpt_day_date = :date)",
           nativeQuery = true)
    Integer findDailyRankByUserIdAndDate(@Param("userId") Long userId, @Param("date") LocalDate date);

    /**
     * Returns the points the user earned on the given date, or null if they haven't played.
     */
    @Query(value = "SELECT points FROM tbl_user_point WHERE user_id = :userId AND excerpt_day_date = :date",
           nativeQuery = true)
    Integer findDailyPointsByUserIdAndDate(@Param("userId") Long userId, @Param("date") LocalDate date);

    /**
     * Returns users ranked by total points earned in the week starting at weekStart through today, inclusive.
     */
    @Query(value = "SELECT u.username, CAST(SUM(up.points) AS INTEGER) AS points, u.current_streak AS streak, u.total_points AS totalPoints " +
                   "FROM tbl_user_point up JOIN tbl_user u ON up.user_id = u.user_id " +
                   "WHERE up.excerpt_day_date >= :weekStart AND up.excerpt_day_date <= :today " +
                   "GROUP BY u.username, u.current_streak, u.total_points " +
                   "ORDER BY SUM(up.points) DESC",
           countQuery = "SELECT COUNT(DISTINCT up.user_id) FROM tbl_user_point up " +
                        "WHERE up.excerpt_day_date >= :weekStart AND up.excerpt_day_date <= :today",
           nativeQuery = true)
    Page<LeaderboardProjection> findWeeklyLeaderboard(@Param("weekStart") LocalDate weekStart, @Param("today") LocalDate today, Pageable pageable);
}
