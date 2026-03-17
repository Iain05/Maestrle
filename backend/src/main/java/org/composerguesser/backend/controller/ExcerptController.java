package org.composerguesser.backend.controller;

import org.composerguesser.backend.dto.DailyChallengeDto;
import org.composerguesser.backend.model.ExcerptDay;
import org.composerguesser.backend.repository.ExcerptDayRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;

@RestController
@RequestMapping("/excerpt")
public class ExcerptController {

    private static final ZoneId PACIFIC = ZoneId.of("America/Vancouver");

    private final ExcerptDayRepository excerptDayRepository;
    private final String audioBaseUrl;

    public ExcerptController(ExcerptDayRepository excerptDayRepository,
                             @Value("${audio.base-url}") String audioBaseUrl) {
        this.excerptDayRepository = excerptDayRepository;
        this.audioBaseUrl = audioBaseUrl;
    }

    /**
     * Returns today's daily challenge excerpt, resolved using the America/Vancouver timezone.
     * The audio URL is constructed from the configured {@code audio.base-url} and the excerpt's filename.
     *
     * @return 200 with {@link DailyChallengeDto} containing excerptId and audioUrl,
     *         or 404 if no challenge has been scheduled for today
     */
    @GetMapping("/daily-challenge")
    public ResponseEntity<DailyChallengeDto> getDailyChallenge() {
        LocalDate today = LocalDate.now(PACIFIC);
        return excerptDayRepository.findById(today)
                .map(ExcerptDay::getExcerpt)
                .map(excerpt -> ResponseEntity.ok(new DailyChallengeDto(
                        excerpt.getExcerptId(),
                        audioBaseUrl + "/" + excerpt.getFilename()
                )))
                .orElse(ResponseEntity.notFound().build());
    }
}
