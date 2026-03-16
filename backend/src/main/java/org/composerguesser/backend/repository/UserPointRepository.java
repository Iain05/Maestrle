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
     * Returns all users who scored on the given date, ranked by points descending.
     */
    @Query(value = "SELECT u.username, up.points FROM tbl_user_point up " +
                   "JOIN tbl_user u ON up.user_id = u.user_id " +
                   "WHERE up.excerpt_day_date = :date ORDER BY up.points DESC",
           countQuery = "SELECT COUNT(*) FROM tbl_user_point WHERE excerpt_day_date = :date",
           nativeQuery = true)
    Page<LeaderboardProjection> findDailyLeaderboard(@Param("date") LocalDate date, Pageable pageable);
}
