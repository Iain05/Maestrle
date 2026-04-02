package org.composerguesser.backend.repository;

import org.composerguesser.backend.model.ExcerptDay;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ExcerptDayRepository extends JpaRepository<ExcerptDay, LocalDate> {
    List<ExcerptDay> findByDateBeforeOrderByDateDesc(LocalDate date);
}
