package org.composerguesser.backend.controller;

import org.composerguesser.backend.dto.GuessRequestDto;
import org.composerguesser.backend.dto.GuessResultDto;
import org.composerguesser.backend.model.User;
import org.composerguesser.backend.service.GuessService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/guess")
public class GuessController {

    private final GuessService guessService;

    public GuessController(GuessService guessService) {
        this.guessService = guessService;
    }

    /**
     * Returns the authenticated user's guess history for today's daily challenge.
     * Used on page load to restore an in-progress game session.
     * Returns an empty array for unauthenticated requests.
     */
    @GetMapping
    public ResponseEntity<List<GuessResultDto>> getGuessHistory(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(guessService.getGuessHistory(user));
    }

    /**
     * Processes a composer guess for today's daily challenge.
     * Returns hint feedback comparing the guessed composer against the correct answer.
     * If the authenticated user guesses correctly, points are awarded and their streak is updated.
     *
     * @param request body containing {@code excerptId} and {@code composerId}
     * @param user    the authenticated user, or {@code null} for anonymous play
     * @return 200 with {@link GuessResultDto} containing hints and (on win) points earned,
     *         or 400 if the guess is invalid (already guessed, game over, unknown composer, etc.)
     */
    @PostMapping
    public ResponseEntity<?> submitGuess(@RequestBody GuessRequestDto request,
                                         @AuthenticationPrincipal User user) {
        try {
            return ResponseEntity.ok(guessService.processGuess(request, user));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
