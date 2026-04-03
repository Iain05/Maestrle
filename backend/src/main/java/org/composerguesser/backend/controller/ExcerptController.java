package org.composerguesser.backend.controller;

import org.composerguesser.backend.dto.ArchiveChallengeDto;
import org.composerguesser.backend.dto.DailyChallengeDto;
import org.composerguesser.backend.model.User;
import org.composerguesser.backend.service.ExcerptService;
import org.composerguesser.backend.service.ExcerptSubmitService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/excerpt")
public class ExcerptController {

    private static final ZoneId PACIFIC = ZoneId.of("America/Vancouver");

    private final ExcerptService excerptService;
    private final ExcerptSubmitService excerptSubmitService;

    public ExcerptController(ExcerptService excerptService, ExcerptSubmitService excerptSubmitService) {
        this.excerptService = excerptService;
        this.excerptSubmitService = excerptSubmitService;
    }

    @GetMapping("/daily-challenge")
    public ResponseEntity<DailyChallengeDto> getDailyChallenge(@AuthenticationPrincipal User user) {
        return excerptService.getDailyChallenge(user)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/challenge/{date}")
    public ResponseEntity<DailyChallengeDto> getChallengeByDate(@PathVariable String date,
                                                                 @AuthenticationPrincipal User user) {
        LocalDate targetDate;
        try {
            targetDate = LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().build();
        }

        if (!targetDate.isBefore(LocalDate.now(PACIFIC))) {
            return ResponseEntity.badRequest().build();
        }

        return excerptService.getChallengeByDate(targetDate, user)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/archive")
    public ResponseEntity<List<ArchiveChallengeDto>> getArchive(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(excerptService.getArchive(user));
    }

    @GetMapping("/submission-points-remaining")
    public ResponseEntity<?> getSubmissionPointsRemaining(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "You must be logged in"));
        }
        return ResponseEntity.ok(Map.of("remaining", excerptSubmitService.getSubmissionPointsRemaining(user)));
    }

    @PostMapping("/submit")
    public ResponseEntity<?> submitExcerpt(
            @RequestParam("audio") MultipartFile audio,
            @RequestParam("composerId") Long composerId,
            @RequestParam(value = "workId", required = false) Long workId,
            @RequestParam("title") String title,
            @RequestParam(value = "compositionYear", required = false) Integer compositionYear,
            @RequestParam(value = "description", required = false) String description,
            @AuthenticationPrincipal User user) {

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "You must be logged in to submit an excerpt"));
        }

        try {
            excerptSubmitService.submit(audio, composerId, workId, title, compositionYear, description, user);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
