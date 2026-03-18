package org.composerguesser.backend.repository;

import org.composerguesser.backend.model.UserGuess;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface UserGuessRepository extends JpaRepository<UserGuess, Long> {
    int countByUserIdAndDate(Long userId, LocalDate date);
    boolean existsByUserIdAndDateAndComposerId(Long userId, LocalDate date, Long composerId);
    List<UserGuess> findByUserIdAndDateOrderByGuessNumber(Long userId, LocalDate date);
}
