package org.composerguesser.backend.dto;

/**
 * Play status for a single archive challenge date.
 * Included in the map returned by {@code GET /api/guess/archive/statuses}.
 */
public record ArchiveStatusDto(int guessCount, boolean correct) {}
