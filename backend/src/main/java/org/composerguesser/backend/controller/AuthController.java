package org.composerguesser.backend.controller;

import org.composerguesser.backend.dto.AuthResponseDto;
import org.composerguesser.backend.dto.LoginRequestDto;
import org.composerguesser.backend.dto.RegisterRequestDto;
import org.composerguesser.backend.model.User;
import org.composerguesser.backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for user authentication.
 * All endpoints are publicly accessible (no JWT required to call register or login).
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Creates a new user account.
     * Returns 400 if the email is already registered.
     *
     * @param request username, email, and password
     * @return JWT token and user profile, or 400 on validation failure
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequestDto request) {
        try {
            return ResponseEntity.ok(userService.register(request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Authenticates a user by email or username and password.
     * Returns 401 if credentials are invalid.
     *
     * @param request email/username identifier and password
     * @return JWT token and user profile, or 401 on bad credentials
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDto request) {
        try {
            return ResponseEntity.ok(userService.login(request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Returns the current user's profile based on the JWT in the Authorization header.
     * The token field in the response is null — clients should reuse their existing token.
     *
     * @param user injected by Spring Security from the JWT; null if unauthenticated
     * @return user profile, or 401 if not authenticated
     */
    @GetMapping("/me")
    public ResponseEntity<AuthResponseDto> me(@AuthenticationPrincipal User user) {
        if (user == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(new AuthResponseDto(null, user.getDisplayUsername(), user.getEmail(), user.getTotalPoints()));
    }
}
