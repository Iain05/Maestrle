package org.composerguesser.backend.dto;

public class ComposerWorkSummaryDto {

    private Long workId;
    private String title;

    public ComposerWorkSummaryDto(Long workId, String title) {
        this.workId = workId;
        this.title = title;
    }

    public Long getWorkId() { return workId; }
    public String getTitle() { return title; }
}
