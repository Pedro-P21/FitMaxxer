package com.pedropacheco.nutrition.backend.userNutritionalProfile.controller;

import com.pedropacheco.nutrition.backend.cli.TerminalRunner;
import com.pedropacheco.nutrition.backend.userNutritionalProfile.dto.NutritionalGoalResponseDTO;
import com.pedropacheco.nutrition.backend.userNutritionalProfile.dto.ProfileChangesDTO;
import com.pedropacheco.nutrition.backend.userNutritionalProfile.service.EditUserNutritionalProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class EditUserNutritionalProfileControllerTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JwtEncoder jwtEncoder;
    @MockitoBean EditUserNutritionalProfileService service;
    @MockitoBean TerminalRunner terminalRunner;

    @Test
    void delegatesAuthenticatedSubjectAndReturnsUpdatedGoal() throws Exception {
        when(service.editUserProfile(eq("user-123"), any(ProfileChangesDTO.class)))
                .thenReturn(new NutritionalGoalResponseDTO("Nutritional profile updated successfully.",
                        2000, 2100, 180, 220, 60));
        ProfileChangesDTO changes = new ProfileChangesDTO(); changes.calorieTarget = 2100;

        mockMvc.perform(put("/api/profiles").header("Authorization", "Bearer " + tokenFor("user-123"))
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(changes)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Nutritional profile updated successfully."))
                .andExpect(jsonPath("$.maintenanceCalories").value(2000))
                .andExpect(jsonPath("$.calorieTarget").value(2100))
                .andExpect(jsonPath("$.proteinGrams").value(180))
                .andExpect(jsonPath("$.carbsGrams").value(220))
                .andExpect(jsonPath("$.fatsGrams").value(60));

        verify(service).editUserProfile(eq("user-123"), any(ProfileChangesDTO.class));
    }

    @Test
    void mapsServiceValidationFailureToBadRequest() throws Exception {
        when(service.editUserProfile(eq("user-123"), any(ProfileChangesDTO.class)))
                .thenThrow(new IllegalArgumentException("At least one profile item must be provided for editing."));
        mockMvc.perform(put("/api/profiles").header("Authorization", "Bearer " + tokenFor("user-123"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"calorieTarget\":-1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsMalformedJsonWithoutCallingService() throws Exception {
        mockMvc.perform(put("/api/profiles").header("Authorization", "Bearer " + tokenFor("user-123"))
                        .contentType(MediaType.APPLICATION_JSON).content("{broken"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void requiresAuthenticatedUser() throws Exception {
        ProfileChangesDTO changes = new ProfileChangesDTO(); changes.calorieTarget = 2100;
        mockMvc.perform(put("/api/profiles").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changes)))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    private String tokenFor(String subject) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer("fitmaxxer-api").subject(subject)
                .audience(List.of("fitmaxxer-api")).issuedAt(now).expiresAt(now.plusSeconds(600))
                .claim("token_use", "access").build();
        return jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }
}
