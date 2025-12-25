package com.smartvillage.authservice.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.Set;

@Component
public class JwtUtil {

    @Value("${jwt.secret:your-256-bit-secret-key-change-this-in-production-environment-must-be-at-least-32-chars}")
    private String jwtSecret;

    @Value("${jwt.expiration-ms:604800000}")  // 7 days
    private long jwtExpirationMs;

    @Value("${jwt.refresh-expiration-ms:604800000}")  // 7 days
    private long refreshTokenExpirationMs;

    private Key getSigningKey() {
        // Ensure the secret is at least 32 bytes for HS256 (256 bits)
        String secret = jwtSecret;
        if (secret == null || secret.trim().isEmpty()) {
            secret = "your-256-bit-secret-key-change-this-in-production-environment-must-be-at-least-32-chars";
        }
        
        byte[] keyBytes;
        if (secret.length() < 32) {
            // Pad the secret to at least 32 bytes if too short
            secret = secret + "0".repeat(Math.max(0, 32 - secret.length()));
        }
        
        // Use the secret bytes directly without Base64 encoding
        keyBytes = secret.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(String subject, Set<String> permissions) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + jwtExpirationMs);
        return Jwts.builder()
                .setSubject(subject)
                .claim("permissions", permissions)
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRefreshToken(String subject) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + refreshTokenExpirationMs);
        return Jwts.builder()
                .setSubject(subject)
                .claim("type", "refresh")
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String getSubjectFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public boolean validateToken(String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                return false;
            }
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            System.err.println("Token expired: " + e.getMessage());
            return false;
        } catch (io.jsonwebtoken.UnsupportedJwtException e) {
            System.err.println("Unsupported JWT: " + e.getMessage());
            return false;
        } catch (io.jsonwebtoken.MalformedJwtException e) {
            System.err.println("Malformed JWT: " + e.getMessage());
            return false;
        } catch (io.jsonwebtoken.SignatureException e) {
            System.err.println("Invalid signature: " + e.getMessage());
            return false;
        } catch (IllegalArgumentException e) {
            System.err.println("JWT claims string is empty: " + e.getMessage());
            return false;
        } catch (Exception e) {
            System.err.println("Token validation failed: " + e.getMessage());
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public Set<String> getPermissionsFromToken(String token) {
        return (Set<String>) Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("permissions", Set.class);
    }

    public long getExpirationTimeInSeconds() {
        return jwtExpirationMs / 1000;
    }
}
