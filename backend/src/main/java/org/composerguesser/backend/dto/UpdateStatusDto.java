package org.composerguesser.backend.dto;

import org.composerguesser.backend.model.ExcerptStatus;

public class UpdateStatusDto {
    private ExcerptStatus status;

    public ExcerptStatus getStatus() { return status; }
    public void setStatus(ExcerptStatus status) { this.status = status; }
}
