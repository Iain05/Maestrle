package org.composerguesser.backend.repository;

/**
 * Projection returned by {@link UserGuessRepository#findArchiveStatuses}.
 * Each row represents one calendar date on which the user submitted at least one guess.
 */
public interface ArchiveStatusProjection {
    /** ISO date string (YYYY-MM-DD) for the challenge date. */
    String getDate();

    /** Total number of guesses submitted by the user for this date (1–5). */
    int getGuessCount();

    /** {@code true} if any of the user's guesses was correct for this date. */
    boolean getCorrect();
}
