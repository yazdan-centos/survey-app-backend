package org.mapnaom.surveyappbackend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.stream.Collectors;

@Service
public class JwtService {
    private final SecretKey key;
    private final long expiration;

    public JwtService(@Value("${app.security.jwt.secret}") String secret,
                      @Value("${app.security.jwt.expiration:3600000}") long expiration) {
        byte[] secretBytes = secret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if (secretBytes.length < 32) throw new IllegalArgumentException("app.security.jwt.secret must be at least 32 bytes");
        this.key = Keys.hmacShaKeyFor(secretBytes);
        this.expiration = expiration;
    }

    public String issue(Authentication authentication) {
        Date now = new Date();
        return Jwts.builder().subject(authentication.getName()).issuedAt(now)
                .claim("authorities", authentication.getAuthorities().stream().map(a -> a.getAuthority()).collect(Collectors.toList()))
                .expiration(new Date(now.getTime() + expiration)).signWith(key).compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
