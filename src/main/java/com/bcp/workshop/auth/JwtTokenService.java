package com.bcp.workshop.auth;

import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Instant;

@ApplicationScoped
public class JwtTokenService {
    public static final long TOKEN_TTL_SECONDS = 3600;

    @ConfigProperty(name = "auth.jwt.issuer")
    String issuer;

    @ConfigProperty(name = "auth.jwt.audience")
    String audience;

    public String create(String username, String role) {
        long expiresAt = Instant.now().plusSeconds(TOKEN_TTL_SECONDS).getEpochSecond();
        return Jwt.issuer(issuer)
            .audience(audience)
                .subject(username)
                .claim("role", role)
                .expiresAt(expiresAt)
                .sign();
    }
}
