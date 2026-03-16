package org.composerguesser.backend.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * Utility for creating and validating HMAC-SHA256 signed JWTs.
 * The secret is read from {@code jwt.secret} (Base64-encoded, min 32 bytes).
 * Token expiry is read from {@code jwt.expiration} (milliseconds).
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    /**
     * Generates a signed JWT with the user's email as the subject.
     *
     * @param email the user's email address, used as the token subject
     * @return compact, URL-safe JWT string
     */
    public String generateToken(String email) {
        return Jwts.builder()
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getKey())
                .compact();
    }

    /**
     * Extracts the email (subject) from a signed JWT.
     *
     * @param token the JWT string
     * @return the email encoded as the token subject
     * @throws JwtException if the token is invalid or expired
     */
    public String extractEmail(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    /**
     * Returns true if the token has a valid signature and has not expired.
     *
     * @param token the JWT string to validate
     * @return true if valid, false otherwise
     */
    public boolean isValid(String token) {
        try {
            extractEmail(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }
}
