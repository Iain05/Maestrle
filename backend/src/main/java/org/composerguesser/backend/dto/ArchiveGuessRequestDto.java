package org.composerguesser.backend.dto;

public class ArchiveGuessRequestDto {

    private Long excerptId;
    private Long composerId;
    private String date;

    public Long getExcerptId() { return excerptId; }
    public void setExcerptId(Long excerptId) { this.excerptId = excerptId; }

    public Long getComposerId() { return composerId; }
    public void setComposerId(Long composerId) { this.composerId = composerId; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
}
