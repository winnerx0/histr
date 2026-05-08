package com.histr.api.utils;

import com.histr.api.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtils {

    public enum TokenType { ACCESS, REFRESH }

    @Value("${jwt.access-token-secret}")
    private String accessTokenSecret;

    @Value("${jwt.refresh-token-secret}")
    private String refreshTokenSecret;

    private SecretKey signingKey(TokenType type) {
        String secret = type == TokenType.ACCESS ? accessTokenSecret : refreshTokenSecret;
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(User user, String type) {
        return generateToken(user, "access".equalsIgnoreCase(type) ? TokenType.ACCESS : TokenType.REFRESH);
    }

    public String generateToken(User user, TokenType type) {
        return type == TokenType.ACCESS ? buildAccessToken(user) : buildRefreshToken(user);
    }

    private String buildAccessToken(User user) {
        return Jwts.builder()
                .subject(user.getUsername())
                .claims(Map.of("role", user.getRole()))
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plus(Duration.ofMinutes(15))))
                .signWith(signingKey(TokenType.ACCESS), Jwts.SIG.HS256)
                .compact();
    }

    private String buildRefreshToken(User user) {
        return Jwts.builder()
                .subject(user.getUsername())
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plus(Duration.ofDays(30))))
                .signWith(signingKey(TokenType.REFRESH), Jwts.SIG.HS256)
                .compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        return isTokenValid(token, userDetails, TokenType.ACCESS);
    }

    public boolean isTokenValid(String token, UserDetails userDetails, TokenType type) {
        try {
            return extractUsername(token, type).equals(userDetails.getUsername())
                    && !isTokenExpired(token, type);
        } catch (Exception e) {
            return false;
        }
    }

    public String extractUsername(String token) {
        return extractUsername(token, TokenType.ACCESS);
    }

    public String extractUsername(String token, TokenType type) {
        return extractClaim(token, type, Claims::getSubject);
    }

    private boolean isTokenExpired(String token, TokenType type) {
        return extractClaim(token, type, Claims::getExpiration).toInstant().isBefore(Instant.now());
    }

    private <T> T extractClaim(String token, TokenType type, Function<Claims, T> resolver) {
        return resolver.apply(extractAllClaims(token, type));
    }

    private Claims extractAllClaims(String token, TokenType type) {
        return Jwts.parser()
                .verifyWith(signingKey(type))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
