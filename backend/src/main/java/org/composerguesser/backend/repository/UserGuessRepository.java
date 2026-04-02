package org.composerguesser.backend.repository;

import org.composerguesser.backend.model.UserGuess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface UserGuessRepository extends JpaRepository<UserGuess, Long> {
    int countByUserIdAndDate(Long userId, LocalDate date);
    boolean existsByUserIdAndDateAndComposerId(Long userId, LocalDate date, Long composerId);
    List<UserGuess> findByUserIdAndDateOrderByGuessNumber(Long userId, LocalDate date);

    /**
     * Returns one row per calendar date on which the user submitted guesses.
     * Joins to {@code tbl_excerpt} to determine correctness without a separate lookup.
     */
    @Query(value = """
            SELECT ug.date::text         AS date,
                   MAX(ug.guess_number)  AS guess_count,
                   BOOL_OR(ug.composer_id = e.composer_id) AS correct
            FROM   tbl_user_guess ug
            JOIN   tbl_excerpt    e ON e.excerpt_id = ug.excerpt_id
            WHERE  ug.user_id = :userId
            GROUP  BY ug.date
            """, nativeQuery = true)
    List<ArchiveStatusProjection> findArchiveStatuses(@Param("userId") Long userId);
}
