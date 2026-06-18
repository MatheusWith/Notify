package com.notify.identity.infrastructure.security;

import com.notify.identity.domain.model.User;
import com.notify.identity.infrastructure.config.JwtProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.getSecret());
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        Instant expiration = now.plusMillis(jwtProperties.getAccessTokenExpiration());

        return Jwts.builder().subject(user.getId().value().toString()).claim("email", user.getEmail().value())
                .claim("name", user.getName())
                .claim("roles", user.getRoles().stream().map(Enum::name).collect(Collectors.toList()))
                .issuedAt(Date.from(now)).expiration(Date.from(expiration)).signWith(getSigningKey()).compact();
    }

    public String generateRefreshToken(User user) {
        Instant now = Instant.now();
        Instant expiration = now.plusMillis(jwtProperties.getRefreshTokenExpiration());

        return Jwts.builder().subject(user.getId().value().toString()).claim("type", "refresh")
                .claim("tokenVersion", user.getTokenVersion()).issuedAt(Date.from(now))
                .expiration(Date.from(expiration)).signWith(getSigningKey()).compact();
    }

    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return Long.parseLong(claims.getSubject());
    }

    public List<String> getRolesFromToken(String token) {
        Claims claims = parseToken(token);
        @SuppressWarnings("unchecked")
        List<String> roles = claims.get("roles", List.class);
        return roles;
    }

    public Long getTokenVersionFromToken(String token) {
        Claims claims = parseToken(token);
        Integer version = claims.get("tokenVersion", Integer.class);
        return version != null ? version.longValue() : 0L;
    }

    public String getEmailFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("email", String.class);
    }

    public String getNameFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("name", String.class);
    }

    private Claims parseToken(String token) {
        return Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();
    }
}
