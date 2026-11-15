package com.pedropacheco.nutrition.backend.userNutritionalProfile.controller;

import com.pedropacheco.nutrition.backend.cli.TerminalRunner;
import com.pedropacheco.nutrition.backend.userAuth.domain.UserId;
import com.pedropacheco.nutrition.backend.userNutritionalProfile.domain.*;
import com.pedropacheco.nutrition.backend.userNutritionalProfile.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.jpa.open-in-view=false")
@AutoConfigureMockMvc
class ViewUserNutritionalProfileTest {
    @Autowired MockMvc mvc;
    @Autowired UserProfileRepository profiles;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean TerminalRunner terminalRunner;
    private Long ownerProfileId;

    @BeforeEach
    void setUp() {
        profiles.deleteAll();
        var owner = new UserNutritionalProfile(new UserId("owner"),
                PhysicalData.validateAndCreate(30, 70.5, 175.5, "MALE", null), ActivityLevel.of("SEDENTARY"),
                new NutritionalGoal(2000, 2000, new MacroBreakdown(120, 220, 70)));
        ownerProfileId = profiles.saveAndFlush(owner).getId();
        profiles.saveAndFlush(new UserNutritionalProfile(new UserId("other"),
                PhysicalData.validateAndCreate(40, 80, 180, "FEMALE", 25.0), ActivityLevel.of("VERY_ACTIVE"),
                new NutritionalGoal(2200, 2200, new MacroBreakdown(150, 220, 80))));
    }

    @Test
    void returnsAllSavedFieldsForAuthenticatedOwnerAndIgnoresClientSuppliedOwner() throws Exception {
        mvc.perform(get("/api/profiles").param("userId", "other").with(jwt().jwt(token -> token.subject("owner"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileId").value(ownerProfileId))
                .andExpect(jsonPath("$.userId").value("owner"))
                .andExpect(jsonPath("$.age").value(30))
                .andExpect(jsonPath("$.weightKg").value(70.5))
                .andExpect(jsonPath("$.heightCm").value(175.5))
                .andExpect(jsonPath("$.gender").value("MALE"))
                .andExpect(jsonPath("$.bodyFatPercentage").isEmpty())
                .andExpect(jsonPath("$.activityLevel").value("SEDENTARY"))
                .andExpect(jsonPath("$.activityFactor").value(1.2))
                .andExpect(jsonPath("$.maintenanceCalories").value(2000))
                .andExpect(jsonPath("$.calorieTarget").value(2000))
                .andExpect(jsonPath("$.proteinGrams").value(120))
                .andExpect(jsonPath("$.carbsGrams").value(220))
                .andExpect(jsonPath("$.fatsGrams").value(70));
        assertEquals(2, profiles.count());
        assertEquals(70.5, profiles.findByUserId(new UserId("owner")).orElseThrow().getPhysicalData().getWeightKg());
        assertEquals(0, jdbc.queryForObject("select count(*) from daily_log", Long.class));
    }

    @Test
    void reportsMissingProfileWithoutReturningAnotherUsersDataOrCreatingAProfile() throws Exception {
        mvc.perform(get("/api/profiles").with(jwt().jwt(token -> token.subject("missing"))))
                .andExpect(status().isNotFound())
                .andExpect(content().string("No nutritional profile found. Create your profile first."));
        assertEquals(2, profiles.count());
    }

    @Test
    void requiresAuthentication() throws Exception {
        mvc.perform(get("/api/profiles")).andExpect(status().isUnauthorized());
    }
}
