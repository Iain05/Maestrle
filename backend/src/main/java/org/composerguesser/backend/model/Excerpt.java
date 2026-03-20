package org.composerguesser.backend.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_excerpt")
public class Excerpt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "excerpt_id")
    private Long excerptId;

    @Column(name = "composer_id", nullable = false)
    private Long composerId;

    @Column(name = "uploaded_by_user_id", nullable = false)
    private Long uploadedByUserId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String filename;

    @Column(name = "composition_year")
    private Integer compositionYear;

    @Column(name = "work_number")
    private Integer workNumber;

    @Column(name = "work_id")
    private Long workId;

    @Column
    private String description;

    @Column(name = "date_uploaded", nullable = false)
    private LocalDateTime dateUploaded;

    @Column(name = "times_used", nullable = false)
    private Integer timesUsed;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "excerpt_status_type")
    private ExcerptStatus status = ExcerptStatus.DRAFT;

    public Long getExcerptId() { return excerptId; }
    public void setExcerptId(Long excerptId) { this.excerptId = excerptId; }

    public Long getComposerId() { return composerId; }
    public void setComposerId(Long composerId) { this.composerId = composerId; }

    public Long getUploadedByUserId() { return uploadedByUserId; }
    public void setUploadedByUserId(Long uploadedByUserId) { this.uploadedByUserId = uploadedByUserId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public Integer getCompositionYear() { return compositionYear; }
    public void setCompositionYear(Integer compositionYear) { this.compositionYear = compositionYear; }

    public Integer getWorkNumber() { return workNumber; }
    public void setWorkNumber(Integer workNumber) { this.workNumber = workNumber; }

    public Long getWorkId() { return workId; }
    public void setWorkId(Long workId) { this.workId = workId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getDateUploaded() { return dateUploaded; }
    public void setDateUploaded(LocalDateTime dateUploaded) { this.dateUploaded = dateUploaded; }

    public Integer getTimesUsed() { return timesUsed; }
    public void setTimesUsed(Integer timesUsed) { this.timesUsed = timesUsed; }
    public ExcerptStatus getStatus() { return status; }
    public void setStatus(ExcerptStatus status) { this.status = status; }
}
