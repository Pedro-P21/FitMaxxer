package com.pedropacheco.nutrition.backend.userAuth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class JwtTokenService {

    public static final String ISSUER = "fitmaxxer-api";
    public static final String AUDIENCE = "fitmaxxer-api";

    private final JwtEncoder jwtEncoder;
    private final Duration accessTokenLifetime;

    public JwtTokenService(
            JwtEncoder jwtEncoder,
            @Value("${app.jwt.access-token-lifetime:PT15M}") Duration accessTokenLifetime
    ) {
        if (accessTokenLifetime.isZero() || accessTokenLifetime.isNegative()) {
            throw new IllegalArgumentException("JWT access token lifetime must be positive.");
        }
        this.jwtEncoder = jwtEncoder;
        this.accessTokenLifetime = accessTokenLifetime;
    }

    public String issueAccessToken(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("User ID cannot be blank when issuing an access token.");
        }

        Instant issuedAt = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(userId)
                .audience(List.of(AUDIENCE))
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(accessTokenLifetime))
                .claim("token_use", "access")
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
