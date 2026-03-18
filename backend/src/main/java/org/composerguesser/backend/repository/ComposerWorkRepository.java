package org.composerguesser.backend.repository;

import org.composerguesser.backend.model.ComposerWork;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComposerWorkRepository extends JpaRepository<ComposerWork, Long> {

    List<ComposerWork> findByComposerId(Long composerId);
}
