package com.pedropacheco.nutrition.backend.userNutritionalProfile.service;

import com.pedropacheco.nutrition.backend.dailyLog.domain.DailyGoalSnapshot;
import com.pedropacheco.nutrition.backend.dailyLog.domain.DailyLog;
import com.pedropacheco.nutrition.backend.dailyLog.repository.DailyLogRepository;
import com.pedropacheco.nutrition.backend.userAuth.domain.UserId;
import com.pedropacheco.nutrition.backend.userNutritionalProfile.domain.*;
import com.pedropacheco.nutrition.backend.userNutritionalProfile.dto.*;
import com.pedropacheco.nutrition.backend.userNutritionalProfile.factory.UserProfileFactory;
import com.pedropacheco.nutrition.backend.userNutritionalProfile.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EditUserNutritionalProfileServiceTest {
    private UserProfileRepository repository;
    private NutritionalGoalCalculatorService calculator;
    private DailyLogRepository dailyLogRepository;
    private EditUserNutritionalProfileService service;
    private UserNutritionalProfile existing;

    @BeforeEach
    void setUp() {
        repository = mock(UserProfileRepository.class);
        calculator = mock(NutritionalGoalCalculatorService.class);
        dailyLogRepository = mock(DailyLogRepository.class);
        UserProfileFactory factory = new UserProfileFactory();
        service = new EditUserNutritionalProfileService(repository, factory, calculator, new UserProfileMapper(),
                dailyLogRepository, Clock.fixed(Instant.parse("2026-11-11T12:00:00Z"), ZoneOffset.UTC));
        existing = profile();
        when(repository.findByUserId(any(UserId.class))).thenReturn(Optional.of(existing));
        when(repository.save(any(UserNutritionalProfile.class))).thenAnswer(call -> call.getArgument(0));
        when(calculator.calculateGoal(any(), any(), any())).thenReturn(
                new NutritionalGoal(2000, 2000, new MacroBreakdown(175, 200, 56)));
    }

    @Test
    void refreshesTodaysDailyLogTargetSnapshotAfterGoalEdit() {
        DailyLog today = new DailyLog(new UserId("user-123"), LocalDate.of(2026, 11, 11), 1L,
                new DailyGoalSnapshot(1800, 100, 150, 89));
        when(dailyLogRepository.findByUserIdAndDate("user-123", LocalDate.of(2026, 11, 11)))
                .thenReturn(Optional.of(today));
        ProfileChangesDTO changes = changes();
        changes.calorieTarget = 2200;

        service.editUserProfile("user-123", changes);

        assertEquals(2200, today.getGoalSnapshot().getCalorieTarget());
        assertEquals(192, today.getGoalSnapshot().getProteinTargetGrams());
        assertEquals(220, today.getGoalSnapshot().getCarbohydrateTargetGrams());
        assertEquals(61, today.getGoalSnapshot().getFatTargetGrams());
        verify(dailyLogRepository).save(today);
    }

    @Test
    void doesNotCreateOrSaveDailyLogWhenNoLogExistsToday() {
        ProfileChangesDTO changes = changes();
        changes.calorieTarget = 2200;

        service.editUserProfile("user-123", changes);

        verify(dailyLogRepository).findByUserIdAndDate("user-123", LocalDate.of(2026, 11, 11));
        verify(dailyLogRepository, never()).save(any(DailyLog.class));
    }

    @Test
    void changesOnlyCalorieTargetAndRebuildsMacrosWithoutRecalculatingProfile() {
        ProfileChangesDTO changes = changes(); changes.calorieTarget = 2200;
        NutritionalGoalResponseDTO response = service.editUserProfile("user-123", changes);

        assertEquals(2200, response.calorieTarget);
        assertEquals(192, response.proteinGrams);
        assertEquals(220, response.carbsGrams);
        assertEquals(61, response.fatsGrams);
        assertEquals(1800, response.maintenanceCalories);
        verify(calculator, never()).calculateGoal(any(), any(), any());
        verify(repository).save(same(existing));
        assertEquals(70, existing.getPhysicalData().getWeightKg());
    }

    @Test
    void appliesValidCustomMacrosToExistingTarget() {
        ProfileChangesDTO changes = changes(); changes.macroBreakdown = macros(100, 150, 89); // 1801 kcal
        NutritionalGoalResponseDTO response = service.editUserProfile("user-123", changes);

        assertEquals(1800, response.calorieTarget);
        assertEquals(100, response.proteinGrams);
        assertEquals(150, response.carbsGrams);
        assertEquals(89, response.fatsGrams);
        verify(calculator, never()).calculateGoal(any(), any(), any());
        verify(repository).save(same(existing));
    }

    @Test
    void recalculatesForActivityChangeAndCreatesProfile() {
        ProfileChangesDTO changes = changes(); changes.activityLevel = "VERY_ACTIVE";
        service.editUserProfile("user-123", changes);

        verify(calculator).calculateGoal(any(PhysicalData.class), any(ActivityLevel.class), eq("MAINTAIN_WEIGHT"));
        verify(repository).save(argThat(saved -> saved != existing
                && saved.getActivityLevel().getLevel().equals("VERY_ACTIVE")
                && saved.getPhysicalData().getWeightKg() == 70));
    }

    @Test
    void recalculatesPhysicalInputAndUsesExplicitCalorieAndMacroOverrides() {
        ProfileChangesDTO changes = changes();
        changes.weightKg = 75.0; changes.goalType = "LOSE_WEIGHT"; changes.calorieTarget = 2000;
        changes.macroBreakdown = macros(150, 200, 67); // 2003 kcal, within tolerance
        service.editUserProfile("user-123", changes);

        verify(calculator).calculateGoal(any(PhysicalData.class), any(ActivityLevel.class), eq("LOSE_WEIGHT"));
        verify(repository).save(argThat(saved -> saved != existing
                && saved.getPhysicalData().getWeightKg() == 75
                && saved.getNutritionalGoal().getCalorieTarget() == 2000
                && saved.getNutritionalGoal().getMacroBreakdown().getProteinGrams() == 150));
    }

    @Test
    void preservesMetricValuesWhenChangingOnlyAgeThroughImperialInput() {
        ProfileChangesDTO changes = changes(); changes.age = 31; changes.unitSystem = "IMPERIAL";
        service.editUserProfile("user-123", changes);

        verify(repository).save(argThat(saved -> Math.abs(saved.getPhysicalData().getWeightKg() - 70) < 0.001
                && Math.abs(saved.getPhysicalData().getHeightCm() - 175) < 0.001
                && saved.getPhysicalData().getAge() == 31));
    }

    @Test
    void acceptsCalorieAndMacroChangesTogetherWhenTheyAlign() {
        ProfileChangesDTO changes = changes(); changes.calorieTarget = 2000;
        changes.macroBreakdown = macros(150, 200, 68); // 2012 kcal
        service.editUserProfile("user-123", changes);
        verify(repository).save(argThat(saved -> saved.getNutritionalGoal().getCalorieTarget() == 2000
                && saved.getNutritionalGoal().getMacroBreakdown().getFatsGrams() == 68));
    }

    @Test
    void rejectsNullEmptyUnknownProfileAndInvalidRequestsWithoutSaving() {
        assertThrows(IllegalArgumentException.class, () -> service.editUserProfile("user-123", null));
        assertThrows(IllegalArgumentException.class, () -> service.editUserProfile("user-123", changes()));
        verify(repository, never()).save(any());

        when(repository.findByUserId(any(UserId.class))).thenReturn(Optional.empty());
        ProfileChangesDTO valid = changes(); valid.calorieTarget = 2100;
        assertThrows(IllegalArgumentException.class, () -> service.editUserProfile("missing", valid));
        verify(repository, never()).save(any());
    }

    @Test
    void rejectsInvalidActivityPhysicalDataGoalAndMacroInputsWithoutSaving() {
        ProfileChangesDTO activity = changes(); activity.activityLevel = "NOPE";
        ProfileChangesDTO physical = changes(); physical.age = 0;
        ProfileChangesDTO goal = changes(); goal.age = 30; goal.goalType = "NOPE";
        ProfileChangesDTO badMacros = changes(); badMacros.macroBreakdown = macros(100, 100, 10);
        ProfileChangesDTO incompleteMacros = changes(); incompleteMacros.macroBreakdown = new ProfileChangesDTO.MacroBreakdownDTO();
        when(calculator.calculateGoal(any(), any(), eq("NOPE")))
                .thenThrow(new IllegalArgumentException("Invalid goal type: NOPE"));

        assertThrows(IllegalArgumentException.class, () -> service.editUserProfile("user-123", activity));
        assertThrows(IllegalArgumentException.class, () -> service.editUserProfile("user-123", physical));
        assertThrows(IllegalArgumentException.class, () -> service.editUserProfile("user-123", goal));
        assertThrows(IllegalArgumentException.class, () -> service.editUserProfile("user-123", badMacros));
        assertThrows(IllegalArgumentException.class, () -> service.editUserProfile("user-123", incompleteMacros));
        verify(repository, never()).save(any());
    }

    private static UserNutritionalProfile profile() {
        return new UserNutritionalProfile(new UserId("user-123"),
                PhysicalData.validateAndCreate(30, 70, 175, "MALE", null), ActivityLevel.of("SEDENTARY"),
                new NutritionalGoal(1800, 1800, new MacroBreakdown(100, 150, 89)));
    }
    private static ProfileChangesDTO changes() { return new ProfileChangesDTO(); }
    private static ProfileChangesDTO.MacroBreakdownDTO macros(int protein, int carbs, int fats) {
        var result = new ProfileChangesDTO.MacroBreakdownDTO();
        result.proteinGrams = protein; result.carbsGrams = carbs; result.fatsGrams = fats; return result;
    }
}
