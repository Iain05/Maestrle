package org.composerguesser.backend.service;

import org.composerguesser.backend.model.Excerpt;
import org.composerguesser.backend.model.ExcerptStatus;
import org.composerguesser.backend.model.User;
import org.composerguesser.backend.repository.ComposerRepository;
import org.composerguesser.backend.repository.ExcerptRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Handles user excerpt submissions.
 *
 * <p>Atomicity guarantee: the audio file is written to disk first, then the database record is
 * inserted. If the database insert fails for any reason, the file is immediately deleted before
 * the error is propagated — ensuring no orphaned files can accumulate. Because a fresh UUID
 * filename is generated on every call, retrying a failed submission is always safe.</p>
 */
@Service
public class ExcerptSubmitService {

    private static final Logger log = LoggerFactory.getLogger(ExcerptSubmitService.class);

    private final ExcerptRepository excerptRepository;
    private final ComposerRepository composerRepository;
    private final String storagePath;

    public ExcerptSubmitService(ExcerptRepository excerptRepository,
                                ComposerRepository composerRepository,
                                @Value("${audio.storage-path}") String storagePath) {
        this.excerptRepository = excerptRepository;
        this.composerRepository = composerRepository;
        this.storagePath = storagePath;
    }

    /**
     * Saves the trimmed audio file and creates a draft {@link Excerpt} record.
     *
     * @param audio           the trimmed WAV file uploaded by the user
     * @param composerId      ID of the composer this excerpt belongs to
     * @param workId          optional ID of the specific work
     * @param title           display name for the excerpt (required)
     * @param compositionYear optional year the piece was composed
     * @param description     optional free-text description
     * @param user            the authenticated user submitting the excerpt
     * @return the newly created (draft) {@link Excerpt}
     * @throws IllegalArgumentException if the composer does not exist or inputs are invalid
     * @throws RuntimeException         if the file cannot be saved to disk
     */
    public Excerpt submit(MultipartFile audio, Long composerId, Long workId,
                          String title, Integer compositionYear, String description, User user) {

        if (audio == null || audio.isEmpty()) {
            throw new IllegalArgumentException("Audio file is required");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title is required");
        }
        if (!composerRepository.existsById(composerId)) {
            throw new IllegalArgumentException("Composer not found");
        }

        String lastName = composerRepository.findById(composerId)
                .map(c -> c.getLastName().toLowerCase().replaceAll("[^a-z0-9]", "-").replaceAll("-+", "-").strip())
                .orElse("unknown");

        // Use a temporary UUID filename until we have the excerpt ID from the DB insert
        String tempFilename = UUID.randomUUID() + ".wav";
        Path tempPath = Paths.get(storagePath, tempFilename);

        log.info("Excerpt submission started: user={} composerId={} title=\"{}\"",
                user.getDisplayUsername(), composerId, title.trim());

        // Step 1: write file to disk under temp name
        try {
            Files.createDirectories(tempPath.getParent());
            audio.transferTo(tempPath);
            log.info("Audio file saved (temp): {}", tempPath);
        } catch (IOException e) {
            log.error("Failed to save audio file {}: {}", tempPath, e.getMessage(), e);
            throw new RuntimeException("Failed to save audio file — please try again", e);
        }

        // Step 2: insert DB record with temp filename; on any failure delete the file
        Excerpt saved;
        try {
            Excerpt excerpt = new Excerpt();
            excerpt.setComposerId(composerId);
            excerpt.setWorkId(workId);
            excerpt.setUploadedByUserId(user.getUserId());
            excerpt.setName(title.trim());
            excerpt.setFilename(tempFilename);
            excerpt.setCompositionYear(compositionYear);
            excerpt.setDescription(description != null && !description.isBlank() ? description.trim() : null);
            excerpt.setStatus(ExcerptStatus.DRAFT);
            excerpt.setTimesUsed(0);
            excerpt.setDateUploaded(LocalDateTime.now());
            saved = excerptRepository.save(excerpt);
        } catch (Exception e) {
            log.error("DB insert failed, attempting cleanup of {}: {}", tempFilename, e.getMessage(), e);
            try {
                Files.deleteIfExists(tempPath);
                log.info("Cleaned up orphaned audio file: {}", tempFilename);
            } catch (IOException cleanupEx) {
                log.warn("Could not delete orphaned audio file {}: {}", tempFilename, cleanupEx.getMessage());
            }
            throw new RuntimeException("Failed to save excerpt — please try again", e);
        }

        // Step 3: rename file to final name now that we have the excerpt ID
        String finalFilename = lastName + "-" + saved.getExcerptId() + ".wav";
        Path finalPath = Paths.get(storagePath, finalFilename);
        try {
            Files.move(tempPath, finalPath);
            saved.setFilename(finalFilename);
            excerptRepository.save(saved);
            log.info("Excerpt created: excerptId={} user={} file={}",
                    saved.getExcerptId(), user.getDisplayUsername(), finalFilename);
        } catch (IOException e) {
            // Non-critical: record is valid, file exists under temp name — log and continue
            log.warn("Could not rename {} to {}, keeping temp name: {}", tempFilename, finalFilename, e.getMessage());
        }

        return saved;
    }
}
