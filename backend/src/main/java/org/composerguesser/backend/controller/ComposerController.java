package org.composerguesser.backend.controller;

import org.composerguesser.backend.dto.ComposerSummaryDto;
import org.composerguesser.backend.dto.ComposerWorkSummaryDto;
import org.composerguesser.backend.model.Composer;
import org.composerguesser.backend.repository.ComposerRepository;
import org.composerguesser.backend.repository.ComposerWorkRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/composers")
public class ComposerController {

    private final ComposerRepository composerRepository;
    private final ComposerWorkRepository composerWorkRepository;

    public ComposerController(ComposerRepository composerRepository, ComposerWorkRepository composerWorkRepository) {
        this.composerRepository = composerRepository;
        this.composerWorkRepository = composerWorkRepository;
    }

    /**
     * Returns a summary list of all composers (id + full name), sorted by database insertion order.
     * Used to populate the guess search dropdown on the frontend.
     *
     * @return 200 with list of {@link ComposerSummaryDto}
     */
    @GetMapping
    public List<ComposerSummaryDto> getAllComposers() {
        return composerRepository.findAll().stream()
                .map(c -> new ComposerSummaryDto(c.getComposerId(), c.getCompleteName()))
                .toList();
    }

    /**
     * Returns the full details of a single composer by ID.
     *
     * @param id the composer's primary key
     * @return 200 with the {@link Composer} entity, or 404 if not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<Composer> getComposer(@PathVariable Long id) {
        return composerRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Returns all works for a given composer as a summary list (id + title), sorted by title.
     * Used to populate the work dropdown on the excerpt submission form.
     *
     * @param id the composer's primary key
     * @return 200 with list of {@link ComposerWorkSummaryDto}, or 404 if the composer does not exist
     */
    @GetMapping("/{id}/works")
    public ResponseEntity<List<ComposerWorkSummaryDto>> getComposerWorks(@PathVariable Long id) {
        if (!composerRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        List<ComposerWorkSummaryDto> works = composerWorkRepository.findByComposerId(id).stream()
                .map(w -> new ComposerWorkSummaryDto(w.getWorkId(), w.getTitle()))
                .sorted((a, b) -> a.getTitle().compareToIgnoreCase(b.getTitle()))
                .toList();
        return ResponseEntity.ok(works);
    }
}
