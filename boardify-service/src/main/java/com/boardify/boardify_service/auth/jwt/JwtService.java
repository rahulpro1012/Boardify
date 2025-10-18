package com.boardify.boardify_service.auth.jwt;

import com.boardify.boardify_service.auth.service.JwtBlacklistService;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.security.Key;
import java.util.Date;


@Service
public class JwtService {

    private final Key key;
    private final long expMs;
    private final JwtBlacklistService blacklistService;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.exp-ms}") long expMs,
            JwtBlacklistService blacklistService) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.expMs = expMs;
        this.blacklistService = blacklistService;
    }

    public boolean isTokenBlacklisted(String token) {
        return blacklistService.isTokenBlacklisted(token);
    }

    public void blacklistToken(String token, long expirationTime) {
        try {
            logger.info("Blacklisting token, expires at: {}", new Date(expirationTime));
            blacklistService.blacklistToken(token, expirationTime);
        } catch (Exception e) {
            logger.error("Failed to blacklist token: {}", e.getMessage());
            // Even if blacklisting fails, we still want to proceed
            // The token will naturally expire based on its expiration time
        }
    }

    public long getExpiration(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getExpiration()
                .getTime();
    }

    public String generateToken(String subject) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expMs))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);

    public String validateAndGetSubject(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            
            // Explicitly check if token is expired
            if (claims.getExpiration().before(new Date())) {
                throw new ExpiredJwtException(null, claims, "Token has expired");
            }
            
            return claims.getSubject();
        } catch (ExpiredJwtException ex) {
            logger.error("Token expired: {}", ex.getMessage());
            throw ex; // Re-throw to be caught by the filter
        } catch (JwtException | IllegalArgumentException e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
            throw new MalformedJwtException("Invalid JWT token", e);
        }
    }
}