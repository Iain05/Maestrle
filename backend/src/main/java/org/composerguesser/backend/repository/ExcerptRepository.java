package org.composerguesser.backend.repository;

import org.composerguesser.backend.model.Excerpt;
import org.composerguesser.backend.model.ExcerptStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.Optional;

public interface ExcerptRepository extends JpaRepository<Excerpt, Long> {

    Page<Excerpt> findAllByStatusIn(Collection<ExcerptStatus> statuses, Pageable pageable);

    Page<Excerpt> findAllByStatusInAndComposerId(Collection<ExcerptStatus> statuses, Long composerId, Pageable pageable);

    /**
     * Returns a random excerpt from those with the lowest times_used value.
     * Only ACTIVE excerpts are eligible for the daily challenge.
     * Ties within the minimum group are broken randomly.
     */
    @Query(value = """
            SELECT * FROM tbl_excerpt
            WHERE status = 'ACTIVE'
              AND times_used = (SELECT MIN(times_used) FROM tbl_excerpt WHERE status = 'ACTIVE')
            ORDER BY RANDOM()
            LIMIT 1
            """, nativeQuery = true)
    Optional<Excerpt> findLeastUsedRandom();
}
