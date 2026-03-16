package org.composerguesser.backend.repository;

import org.composerguesser.backend.model.UserGuess;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserGuessRepository extends JpaRepository<UserGuess, Long> {
    int countByUserIdAndExcerptId(Long userId, Long excerptId);
    boolean existsByUserIdAndExcerptIdAndComposerId(Long userId, Long excerptId, Long composerId);
    List<UserGuess> findByUserIdAndExcerptIdOrderByGuessNumber(Long userId, Long excerptId);
}
