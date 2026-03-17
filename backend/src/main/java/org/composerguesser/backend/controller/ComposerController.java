package org.composerguesser.backend.controller;

import org.composerguesser.backend.dto.ComposerSummaryDto;
import org.composerguesser.backend.model.Composer;
import org.composerguesser.backend.repository.ComposerRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/composers")
public class ComposerController {

    private final ComposerRepository composerRepository;

    public ComposerController(ComposerRepository composerRepository) {
        this.composerRepository = composerRepository;
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
}
