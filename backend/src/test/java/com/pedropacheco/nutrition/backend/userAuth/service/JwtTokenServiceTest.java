package com.pedropacheco.nutrition.backend.userAuth.service;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenServiceTest {

    private static final String SECRET = "test-secret-that-is-at-least-32-bytes-long";

    @Test
    void shouldIssueSignedAccessTokenWithExpectedClaimsAndLifetime() {
        SecretKey key = new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(key));
        JwtTokenService service = new JwtTokenService(encoder, Duration.ofMinutes(15));
        JwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256)
                .build();
        Instant beforeIssue = Instant.now();

        Jwt token = decoder.decode(service.issueAccessToken("user-123"));

        assertEquals("user-123", token.getSubject());
        assertEquals(JwtTokenService.ISSUER, token.getClaimAsString("iss"));
        assertTrue(token.getAudience().contains(JwtTokenService.AUDIENCE));
        assertEquals("access", token.getClaimAsString("token_use"));
        assertNotNull(token.getIssuedAt());
        assertNotNull(token.getExpiresAt());
        assertTrue(token.getIssuedAt().getEpochSecond() >= beforeIssue.getEpochSecond());
        assertEquals(Duration.ofMinutes(15), Duration.between(token.getIssuedAt(), token.getExpiresAt()));
    }

    @Test
    void shouldRejectBlankUserId() {
        SecretKey key = new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(key));
        JwtTokenService service = new JwtTokenService(encoder, Duration.ofMinutes(15));

        assertThrows(IllegalArgumentException.class, () -> service.issueAccessToken(" "));
    }

    @Test
    void shouldRejectNonPositiveTokenLifetime() {
        SecretKey key = new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(key));

        assertThrows(IllegalArgumentException.class, () -> new JwtTokenService(encoder, Duration.ZERO));
        assertThrows(IllegalArgumentException.class, () -> new JwtTokenService(encoder, Duration.ofSeconds(-1)));
    }
}
