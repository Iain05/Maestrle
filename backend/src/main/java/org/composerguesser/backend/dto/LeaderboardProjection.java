package org.composerguesser.backend.dto;

/**
 * Spring Data projection used by both leaderboard queries.
 * Column aliases in native queries must match the getter names (username, points).
 */
public interface LeaderboardProjection {
    String getUsername();
    int getPoints();
    int getStreak();
    int getTotalPoints();
}
