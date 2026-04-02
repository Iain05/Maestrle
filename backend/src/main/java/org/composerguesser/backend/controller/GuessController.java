package org.composerguesser.backend.controller;

import org.composerguesser.backend.dto.ArchiveGuessRequestDto;
import org.composerguesser.backend.dto.ArchiveStatusDto;
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
     * Returns the authenticated user's guess history for a daily challenge.
     * Defaults to today if no {@code date} query parameter is supplied.
     * Returns an empty array for unauthenticated requests.
     *
     * @param date optional ISO date (YYYY-MM-DD); omit for today's challenge
     */
    @GetMapping
    public ResponseEntity<List<GuessResultDto>> getGuessHistory(@AuthenticationPrincipal User user,
                                                                 @RequestParam(required = false) String date) {
        return ResponseEntity.ok(guessService.getGuessHistory(user, date));
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

    /**
     * Returns the authenticated user's play status for every archive date they have played.
     * The response is a map from ISO date string (YYYY-MM-DD) to {@link ArchiveStatusDto}.
     * Dates with no guesses are omitted — a missing key means "not played".
     * Returns an empty object for unauthenticated users.
     */
    @GetMapping("/archive/statuses")
    public ResponseEntity<Map<String, ArchiveStatusDto>> getArchiveStatuses(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(guessService.getArchiveStatuses(user));
    }

    /**
     * Processes a composer guess for a past daily challenge.
     * No points are awarded and streaks are not updated for archive guesses.
     *
     * @param request body containing {@code excerptId}, {@code composerId}, and {@code date}
     * @param user    the authenticated user, or {@code null} for anonymous play
     * @return 200 with {@link GuessResultDto}, or 400 if the guess or date is invalid
     */
    @PostMapping("/archive")
    public ResponseEntity<?> submitArchiveGuess(@RequestBody ArchiveGuessRequestDto request,
                                                 @AuthenticationPrincipal User user) {
        try {
            return ResponseEntity.ok(guessService.processArchiveGuess(request, user));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
