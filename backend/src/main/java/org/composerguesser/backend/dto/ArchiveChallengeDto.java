package org.composerguesser.backend.dto;

/**
 * One entry in the archive challenge list returned by {@code GET /api/excerpt/archive}.
 * {@code guessCount} and {@code correct} are {@code null} when the user has not played
 * this challenge (or when the request is unauthenticated).
 */
public record ArchiveChallengeDto(
        String date,
        int challengeNumber,
        int guessCount,
        boolean correct
) {}
