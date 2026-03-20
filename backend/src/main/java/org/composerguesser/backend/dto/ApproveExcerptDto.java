package org.composerguesser.backend.dto;

public class ApproveExcerptDto {

    private Long composerId;
    private Long workId;
    private String name;
    private Integer compositionYear;
    private String description;

    public Long getComposerId() { return composerId; }
    public void setComposerId(Long composerId) { this.composerId = composerId; }

    public Long getWorkId() { return workId; }
    public void setWorkId(Long workId) { this.workId = workId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getCompositionYear() { return compositionYear; }
    public void setCompositionYear(Integer compositionYear) { this.compositionYear = compositionYear; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
