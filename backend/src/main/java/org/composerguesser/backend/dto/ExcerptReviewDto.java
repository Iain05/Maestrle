package org.composerguesser.backend.dto;

import org.composerguesser.backend.model.ExcerptStatus;

public class ExcerptReviewDto {

    private final Long excerptId;
    private final Long composerId;
    private final String composerName;
    private final Long workId;
    private final Long uploadedByUserId;
    private final String uploaderUsername;
    private final String name;
    private final String audioUrl;
    private final Integer compositionYear;
    private final String description;
    private final String dateUploaded;
    private final Integer timesUsed;
    private final ExcerptStatus status;

    public ExcerptReviewDto(Long excerptId, Long composerId, String composerName, Long workId,
                            Long uploadedByUserId, String uploaderUsername, String name,
                            String audioUrl, Integer compositionYear, String description,
                            String dateUploaded, Integer timesUsed, ExcerptStatus status) {
        this.excerptId = excerptId;
        this.composerId = composerId;
        this.composerName = composerName;
        this.workId = workId;
        this.uploadedByUserId = uploadedByUserId;
        this.uploaderUsername = uploaderUsername;
        this.name = name;
        this.audioUrl = audioUrl;
        this.compositionYear = compositionYear;
        this.description = description;
        this.dateUploaded = dateUploaded;
        this.timesUsed = timesUsed;
        this.status = status;
    }

    public Long getExcerptId() { return excerptId; }
    public Long getComposerId() { return composerId; }
    public String getComposerName() { return composerName; }
    public Long getWorkId() { return workId; }
    public Long getUploadedByUserId() { return uploadedByUserId; }
    public String getUploaderUsername() { return uploaderUsername; }
    public String getName() { return name; }
    public String getAudioUrl() { return audioUrl; }
    public Integer getCompositionYear() { return compositionYear; }
    public String getDescription() { return description; }
    public String getDateUploaded() { return dateUploaded; }
    public Integer getTimesUsed() { return timesUsed; }
    public ExcerptStatus getStatus() { return status; }
}
