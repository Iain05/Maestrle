package org.composerguesser.backend.repository;

import org.composerguesser.backend.dto.LeaderboardProjection;
import org.composerguesser.backend.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);

    /**
     * Returns all users ranked by total points descending.
     */
    @Query(value = "SELECT username, total_points as points FROM tbl_user ORDER BY total_points DESC",
           countQuery = "SELECT COUNT(*) FROM tbl_user",
           nativeQuery = true)
    Page<LeaderboardProjection> findAllTimeLeaderboard(Pageable pageable);
}
