package com.pedropacheco.nutrition.backend.configuration;

import com.pedropacheco.nutrition.backend.userAuth.service.JwtTokenService;
import com.pedropacheco.nutrition.backend.userAuth.service.UserAuthService;
import com.pedropacheco.nutrition.backend.userNutritionalProfile.service.UserProfileNotFoundException;
import com.pedropacheco.nutrition.backend.userNutritionalProfile.service.ViewUserNutritionalProfileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.time.Instant;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class ApiProblemResponseTest {
    @Autowired MockMvc mvc;
    @Autowired JwtEncoder encoder;
    @Autowired AccessDeniedHandler apiAccessDeniedHandler;
    @MockitoBean ViewUserNutritionalProfileService profiles;
    @MockitoBean UserAuthService auth;

    @Test
    void activityLoggingIncludesSecurityRejectionsAndSuccessfulRequests(CapturedOutput output) throws Exception {
        mvc.perform(get("/api/profiles")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/health")).andExpect(status().isOk());
        org.assertj.core.api.Assertions.assertThat(output.getAll())
                .contains("REJECTED View nutritional profile status=401", "COMPLETED Check API health status=200");
        verifyNoInteractions(profiles);
    }

    @Test
    void missingAndInvalidTokensReturnProblemDetailsWithBearerChallenges() throws Exception {
        mvc.perform(get("/api/profiles"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", containsString("Bearer")))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").value("Unauthorized"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").isString())
                .andExpect(jsonPath("$.instance").value("/api/profiles"));
        mvc.perform(get("/api/profiles").header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", containsString("invalid_token")))
                .andExpect(jsonPath("$.status").value(401));
        verifyNoInteractions(profiles);
    }

    @Test
    void expiredSignedTokenReturnsTheSameStructuredUnauthorizedResponse() throws Exception {
        Instant now = Instant.now();
        var claims = JwtClaimsSet.builder().issuer(JwtTokenService.ISSUER).subject("owner")
                .audience(List.of(JwtTokenService.AUDIENCE)).claim("token_use", "access")
                .issuedAt(now.minusSeconds(3600)).expiresAt(now.minusSeconds(300)).build();
        String token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        mvc.perform(get("/api/profiles").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(header().string("WWW-Authenticate", containsString("invalid_token")))
                .andExpect(jsonPath("$.title").value("Unauthorized"))
                .andExpect(jsonPath("$.instance").value("/api/profiles"));
        verifyNoInteractions(profiles);
    }

    @Test
    void applicationNotFoundAndInvalidCredentialsUseProblemDetails() throws Exception {
        when(profiles.getProfile("owner")).thenThrow(new UserProfileNotFoundException());
        mvc.perform(get("/api/profiles").with(jwt().jwt(token -> token.subject("owner"))))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.detail").value("No nutritional profile found. Create your profile first."))
                .andExpect(jsonPath("$.instance").value("/api/profiles"));
        when(auth.authenticate("owner", "wrong")).thenThrow(new BadCredentialsException("internal credentials detail"));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"owner\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid credentials."));
    }

    @Test
    void malformedJsonAndInvalidQueryParametersHaveConsistentErrors() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.instance").value("/api/auth/login"));
        mvc.perform(get("/api/daily-log/history").param("month", "invalid").param("year", "2026").with(jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("A request parameter has an invalid value."))
                .andExpect(jsonPath("$.instance").value("/api/daily-log/history"));
    }

    @Test
    void securityAccessDeniedHandlerWritesProblemDetailsAndPreservesBearerHeaders() throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/profiles");
        var response = new MockHttpServletResponse();
        var token = Jwt.withTokenValue("test-token").header("alg", "HS256").subject("owner").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(token));
        try {
            apiAccessDeniedHandler.handle(request, response, new AccessDeniedException("internal denial reason"));
            assertEquals(403, response.getStatus());
            assertTrue(response.getHeader("WWW-Authenticate").contains("Bearer"));
            assertEquals(MediaType.APPLICATION_PROBLEM_JSON_VALUE, response.getContentType());
            String json = response.getContentAsString();
            assertTrue(json.contains("\"status\":403"));
            assertTrue(json.contains("\"instance\":\"/api/profiles\""));
            assertFalse(json.contains("internal denial reason"));
        } finally { SecurityContextHolder.clearContext(); }
    }

    @Test
    void unexpectedFailuresReturnGeneric500WithoutLeakingDiagnosticDetails() throws Exception {
        when(profiles.getProfile("owner")).thenThrow(new IllegalStateException("sensitive database connection details"));
        mvc.perform(get("/api/profiles").with(jwt().jwt(token -> token.subject("owner"))))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred. Please try again later."))
                .andExpect(jsonPath("$.instance").value("/api/profiles"));
    }

    @Test
    void authenticatedUnknownRoutesUseTheSameErrorFormat() throws Exception {
        mvc.perform(get("/api/does-not-exist").with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.instance").value("/api/does-not-exist"));
    }
}
