package com.pedropacheco.nutrition.backend.userNutritionalProfile.controller;

import com.pedropacheco.nutrition.backend.userNutritionalProfile.dto.NutritionalGoalRequestDTO;
import com.pedropacheco.nutrition.backend.userNutritionalProfile.dto.NutritionalGoalResponseDTO;
import com.pedropacheco.nutrition.backend.userNutritionalProfile.service.PersonalDataService;
import com.pedropacheco.nutrition.backend.cli.TerminalRunner;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PersonalDataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtEncoder jwtEncoder;

    @MockitoBean
    private PersonalDataService personalDataService;

    @MockitoBean
    private TerminalRunner terminalRunner;

    @Test
    void shouldReturnGoalSummaryWhenPersonalDataIsLogged() throws Exception {
        NutritionalGoalRequestDTO request = new NutritionalGoalRequestDTO(
                30, 70, 175, "MALE", "SEDENTARY", "MAINTAIN_WEIGHT", null, "METRIC");
        NutritionalGoalResponseDTO response = new NutritionalGoalResponseDTO(
                "Personal data logged successfully.", 1979, 1979, 173, 197, 54);
        when(personalDataService.logPersonalData(eq("user-123"), any(NutritionalGoalRequestDTO.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/profiles/personal-data")
                        .header("Authorization", "Bearer " + tokenFor("user-123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Personal data logged successfully."))
                .andExpect(jsonPath("$.maintenanceCalories").value(1979))
                .andExpect(jsonPath("$.calorieTarget").value(1979))
                .andExpect(jsonPath("$.proteinGrams").value(173))
                .andExpect(jsonPath("$.carbsGrams").value(197))
                .andExpect(jsonPath("$.fatsGrams").value(54));

        verify(personalDataService).logPersonalData(eq("user-123"), any(NutritionalGoalRequestDTO.class));
    }

    @Test
    void shouldReturnBadRequestForMalformedJsonWithoutCallingService() throws Exception {
        mockMvc.perform(post("/api/profiles/personal-data")
                        .header("Authorization", "Bearer " + tokenFor("user-123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(personalDataService);
    }

    @Test
    void shouldReturnBadRequestWhenServiceRejectsInvalidProfileData() throws Exception {
        when(personalDataService.logPersonalData(eq("user-123"), any(NutritionalGoalRequestDTO.class)))
                .thenThrow(new IllegalArgumentException("Age must be between 1 and 120 years."));
        NutritionalGoalRequestDTO request = new NutritionalGoalRequestDTO(
                0, 70, 175, "MALE", "SEDENTARY", "MAINTAIN_WEIGHT", null, "METRIC");

        mockMvc.perform(post("/api/profiles/personal-data")
                        .header("Authorization", "Bearer " + tokenFor("user-123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectProfileRequestWithoutBearerToken() throws Exception {
        mockMvc.perform(post("/api/profiles/personal-data")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new NutritionalGoalRequestDTO(
                                30, 70, 175, "MALE", "SEDENTARY", "MAINTAIN_WEIGHT", null, "METRIC"))))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(personalDataService);
    }

    @Test
    void shouldRejectTokenWithInvalidSignature() throws Exception {
        String token = tokenFor("user-123");
        String[] tokenParts = token.split("\\.");
        tokenParts[2] = (tokenParts[2].startsWith("a") ? "b" : "a") + tokenParts[2].substring(1);
        String tamperedToken = String.join(".", tokenParts);

        mockMvc.perform(post("/api/profiles/personal-data")
                        .header("Authorization", "Bearer " + tamperedToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new NutritionalGoalRequestDTO(
                                30, 70, 175, "MALE", "SEDENTARY", "MAINTAIN_WEIGHT", null, "METRIC"))))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(personalDataService);
    }

    @Test
    void shouldRejectExpiredToken() throws Exception {
        Instant now = Instant.now();
        String expiredToken = tokenFor("user-123", now.minusSeconds(600), now.minusSeconds(1200));

        mockMvc.perform(post("/api/profiles/personal-data")
                        .header("Authorization", "Bearer " + expiredToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new NutritionalGoalRequestDTO(
                                30, 70, 175, "MALE", "SEDENTARY", "MAINTAIN_WEIGHT", null, "METRIC"))))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(personalDataService);
    }

    @Test
    void shouldRejectTokenWithInvalidIssuerAudienceTokenUseOrSubject() throws Exception {
        List<String> invalidTokens = List.of(
                tokenFor("user-123", "another-issuer", "fitmaxxer-api", "access"),
                tokenFor("user-123", "fitmaxxer-api", "another-audience", "access"),
                tokenFor("user-123", "fitmaxxer-api", "fitmaxxer-api", "refresh"),
                tokenFor(" ", "fitmaxxer-api", "fitmaxxer-api", "access")
        );

        for (String token : invalidTokens) {
            mockMvc.perform(post("/api/profiles/personal-data")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new NutritionalGoalRequestDTO(
                                    30, 70, 175, "MALE", "SEDENTARY", "MAINTAIN_WEIGHT", null, "METRIC"))))
                    .andExpect(status().isUnauthorized());
        }

        verifyNoInteractions(personalDataService);
    }

    private String tokenFor(String userId) {
        Instant now = Instant.now();
        return tokenFor(userId, now.plusSeconds(600), now);
    }

    private String tokenFor(String userId, Instant expiresAt, Instant issuedAt) {
        return tokenFor(userId, "fitmaxxer-api", "fitmaxxer-api", "access", expiresAt, issuedAt);
    }

    private String tokenFor(String subject, String issuer, String audience, String tokenUse) {
        Instant now = Instant.now();
        return tokenFor(subject, issuer, audience, tokenUse, now.plusSeconds(600), now);
    }

    private String tokenFor(String subject, String issuer, String audience, String tokenUse,
                            Instant expiresAt, Instant issuedAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(subject)
                .audience(List.of(audience))
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("token_use", tokenUse)
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }
}
