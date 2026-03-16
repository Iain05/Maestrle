package org.composerguesser.backend.service;

import org.composerguesser.backend.dto.AuthResponseDto;
import org.composerguesser.backend.dto.LoginRequestDto;
import org.composerguesser.backend.dto.RegisterRequestDto;
import org.composerguesser.backend.model.User;
import org.composerguesser.backend.repository.UserRepository;
import org.composerguesser.backend.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Handles user registration and authentication.
 * Passwords are hashed with BCrypt and never stored in plaintext.
 * On success, both methods return a signed JWT alongside the user's public profile data.
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Registers a new user account.
     *
     * @param request contains the desired username, email, and plaintext password
     * @return JWT token and user profile on success
     * @throws IllegalArgumentException if the email is already in use
     */
    public AuthResponseDto register(RegisterRequestDto request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already in use");
        }
        User user = new User();
        user.setDisplayUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setTotalPoints(0);
        user.setCreatedAt(LocalDateTime.now());
        userRepository.save(user);
        String token = jwtUtil.generateToken(user.getEmail());
        return new AuthResponseDto(token, user.getDisplayUsername(), user.getEmail(), user.getTotalPoints());
    }

    /**
     * Authenticates an existing user by email or username.
     * The {@code email} field in the request is treated as an identifier and checked
     * against both email and username columns.
     *
     * @param request contains the email/username identifier and plaintext password
     * @return JWT token and user profile on success
     * @throws IllegalArgumentException if no matching user is found or the password is incorrect
     */
    public AuthResponseDto login(LoginRequestDto request) {
        User user = userRepository.findByEmail(request.getEmail())
                .or(() -> userRepository.findByUsername(request.getEmail()))
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }
        String token = jwtUtil.generateToken(user.getEmail());
        return new AuthResponseDto(token, user.getDisplayUsername(), user.getEmail(), user.getTotalPoints());
    }
}
