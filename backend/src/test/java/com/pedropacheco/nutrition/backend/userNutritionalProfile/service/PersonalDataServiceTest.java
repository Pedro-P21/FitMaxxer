package com.pedropacheco.nutrition.backend.userNutritionalProfile.service;

import com.pedropacheco.nutrition.backend.userAuth.domain.UserId;
import com.pedropacheco.nutrition.backend.userAuth.repository.UserRepository;
import com.pedropacheco.nutrition.backend.userNutritionalProfile.domain.UserNutritionalProfile;
import com.pedropacheco.nutrition.backend.userNutritionalProfile.dto.NutritionalGoalRequestDTO;
import com.pedropacheco.nutrition.backend.userNutritionalProfile.dto.NutritionalGoalResponseDTO;
import com.pedropacheco.nutrition.backend.userNutritionalProfile.dto.UserProfileMapper;
import com.pedropacheco.nutrition.backend.userNutritionalProfile.factory.UserProfileFactory;
import com.pedropacheco.nutrition.backend.userNutritionalProfile.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PersonalDataServiceTest {

    private UserRepository userRepository;
    private UserProfileRepository userProfileRepository;
    private PersonalDataService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userProfileRepository = mock(UserProfileRepository.class);
        UserProfileFactory factory = new UserProfileFactory();
        service = new PersonalDataService(
                userRepository,
                userProfileRepository,
                factory,
                new NutritionalGoalCalculatorService(factory),
                new UserProfileMapper()
        );
        when(userRepository.existsByUserId(any(UserId.class))).thenReturn(true);
        when(userProfileRepository.findByUserId(any(UserId.class))).thenReturn(Optional.empty());
        when(userProfileRepository.save(any(UserNutritionalProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void logsValidMetricDataCalculatesGoalAndPersistsProfile() {
        NutritionalGoalRequestDTO request = new NutritionalGoalRequestDTO(
                30, 70, 175, "male", "SEDENTARY", "MAINTAIN_WEIGHT", null, "METRIC");

        NutritionalGoalResponseDTO response = service.logPersonalData("user-123", request);

        assertEquals("Personal data logged and nutritional goals calculated successfully.", response.getMessage());
        assertEquals(1979, response.getMaintenanceCalories());
        assertEquals(1979, response.getCalorieTarget());
        assertTrue(response.getProteinGrams() > 0);
        assertTrue(response.getCarbsGrams() > 0);
        assertTrue(response.getFatsGrams() > 0);
        verify(userRepository).existsByUserId(any(UserId.class));
        verify(userProfileRepository).save(argThat(profile ->
                profile.getUserId().getValue().equals("user-123")
                        && profile.getPhysicalData().getWeightKg() == 70
                        && profile.getActivityLevel().getLevel().equals("SEDENTARY")
                        && profile.getNutritionalGoal().getCalorieTarget() == 1979));
    }

    @Test
    void rejectsInvalidPhysicalDataWithoutSavingOrCalculating() {
        NutritionalGoalRequestDTO invalid = new NutritionalGoalRequestDTO(
                0, 70, 175, "MALE", "SEDENTARY", "MAINTAIN_WEIGHT", null, "METRIC");

        assertThrows(IllegalArgumentException.class, () -> service.logPersonalData("user-123", invalid));

        verify(userProfileRepository, never()).save(any());
    }

    @Test
    void rejectsUnsupportedGoalTypeWithoutSavingProfile() {
        NutritionalGoalRequestDTO inconsistent = new NutritionalGoalRequestDTO(
                30, 70, 175, "MALE", "SEDENTARY", "LOSE_WEIGHT_FAST", null, "METRIC");

        assertThrows(IllegalArgumentException.class, () -> service.logPersonalData("user-123", inconsistent));

        verify(userProfileRepository, never()).save(any());
    }

    @Test
    void refusesToLogDataForUnknownUser() {
        when(userRepository.existsByUserId(any(UserId.class))).thenReturn(false);
        NutritionalGoalRequestDTO valid = new NutritionalGoalRequestDTO(
                30, 70, 175, "MALE", "SEDENTARY", "MAINTAIN_WEIGHT", null, "METRIC");

        assertThrows(IllegalArgumentException.class, () -> service.logPersonalData("unknown", valid));

        verifyNoInteractions(userProfileRepository);
    }

    @Test
    void normalizesImperialInputsBeforePersisting() {
        NutritionalGoalRequestDTO imperial = new NutritionalGoalRequestDTO(
                30, 154, 69, "FEMALE", "LIGHTLY_ACTIVE", "MAINTAIN_WEIGHT", null, "IMPERIAL");

        service.logPersonalData("user-123", imperial);

        verify(userProfileRepository).save(argThat(profile ->
                Math.abs(profile.getPhysicalData().getWeightKg() - (154.0 / 2.2)) < 0.0001
                        && Math.abs(profile.getPhysicalData().getHeightCm() - (69 * 2.54)) < 0.0001));
    }

    @Test
    void updatesExistingProfileInsteadOfCreatingAnotherForSameUser() {
        UserId id = new UserId("user-123");
        UserNutritionalProfile existing = new UserNutritionalProfile(
                id,
                com.pedropacheco.nutrition.backend.userNutritionalProfile.domain.PhysicalData
                        .validateAndCreate(25, 65, 170, "FEMALE", null),
                com.pedropacheco.nutrition.backend.userNutritionalProfile.domain.ActivityLevel.of("SEDENTARY"),
                new com.pedropacheco.nutrition.backend.userNutritionalProfile.domain.NutritionalGoal(
                        1700, 1700, new com.pedropacheco.nutrition.backend.userNutritionalProfile.domain.MacroBreakdown(100, 100, 100)));
        when(userProfileRepository.findByUserId(any(UserId.class))).thenReturn(Optional.of(existing));
        NutritionalGoalRequestDTO updated = new NutritionalGoalRequestDTO(
                31, 72, 180, "MALE", "VERY_ACTIVE", "MAINTAIN_WEIGHT", null, "METRIC");

        service.logPersonalData("user-123", updated);

        verify(userProfileRepository).save(same(existing));
        assertEquals("user-123", existing.getUserId().getValue());
        assertEquals(31, existing.getPhysicalData().getAge());
        assertEquals(72, existing.getPhysicalData().getWeightKg());
        assertEquals("VERY_ACTIVE", existing.getActivityLevel().getLevel());
        verify(userProfileRepository).findByUserId(any(UserId.class));
    }
}
