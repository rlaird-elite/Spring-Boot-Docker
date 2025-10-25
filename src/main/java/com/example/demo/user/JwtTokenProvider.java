package com.example.demo.user;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final SecretKey jwtSecretKey;
    private final long jwtExpirationInMs;

    public JwtTokenProvider(@Value("${app.jwt.secret}") String jwtSecret,
                            @Value("${app.jwt.expiration-in-ms}") long jwtExpirationInMs) {
        // Create a SecretKey object from the string
        this.jwtSecretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        this.jwtExpirationInMs = jwtExpirationInMs;
    }

    /**
     * Generates a JWT token for a given authenticated user.
     * @param authentication The Spring Security Authentication object.
     * @return A signed JWT token as a string.
     */
    public String generateToken(Authentication authentication) {
        String username = authentication.getName();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        // --- UPDATED for modern jjwt API ---
        return Jwts.builder()
                .setSubject(username) // .subject() is now .setSubject()
                .setIssuedAt(now)     // .issuedAt() is now .setIssuedAt()
                .setExpiration(expiryDate) // .expiration() is now .setExpiration()
                .signWith(jwtSecretKey) // .signWith() is still correct
                .compact();
    }

    /**
     * Validates if a JWT token is correct and not expired.
     * @param token The JWT token string.
     * @return true if the token is valid, false otherwise.
     */
    public boolean validateToken(String token) {
        try {
            // --- UPDATED for modern jjwt API ---
            // Jwts.parser() is deprecated, use parserBuilder()
            // .setSigningKey() replaces .verifyWith()
            Jwts.parserBuilder()
                    .setSigningKey(jwtSecretKey)
                    .build()
                    .parseClaimsJws(token); // parseSignedClaims() is now parseClaimsJws()
            return true;
        } catch (Exception ex) {
            // This will catch expired tokens, malformed tokens, etc.
            return false;
        }
    }

    /**
     * Extracts the username (subject) from a validated JWT token.
     * @param token The JWT token string.
     * @return The username.
     */
    public String getUsernameFromToken(String token) {
        // --- UPDATED for modern jjwt API ---
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(jwtSecretKey) // .setSigningKey() replaces .verifyWith()
                .build()
                .parseClaimsJws(token) // parseSignedClaims() is now parseClaimsJws()
                .getBody(); // .getPayload() is now .getBody()

        return claims.getSubject();
    }
}

