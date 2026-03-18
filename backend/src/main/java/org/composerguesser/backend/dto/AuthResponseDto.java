package org.composerguesser.backend.dto;

import org.composerguesser.backend.model.Role;

public class AuthResponseDto {
    private final String token;
    private final String username;
    private final String email;
    private final int totalPoints;
    private final Role role;

    public AuthResponseDto(String token, String username, String email, int totalPoints, Role role) {
        this.token = token;
        this.username = username;
        this.email = email;
        this.totalPoints = totalPoints;
        this.role = role;
    }

    public String getToken() { return token; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public int getTotalPoints() { return totalPoints; }
    public Role getRole() { return role; }
}
