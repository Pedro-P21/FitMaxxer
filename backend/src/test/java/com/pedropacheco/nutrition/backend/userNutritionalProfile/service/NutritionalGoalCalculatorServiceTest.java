package com.pedropacheco.nutrition.backend.userNutritionalProfile.service;

import com.pedropacheco.nutrition.backend.userNutritionalProfile.domain.ActivityLevel;
import com.pedropacheco.nutrition.backend.userNutritionalProfile.domain.NutritionalGoal;
import com.pedropacheco.nutrition.backend.userNutritionalProfile.domain.PhysicalData;
import com.pedropacheco.nutrition.backend.userNutritionalProfile.factory.UserProfileFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NutritionalGoalCalculatorServiceTest {

    private final NutritionalGoalCalculatorService service =
            new NutritionalGoalCalculatorService(new UserProfileFactory());

    @Test
    void calculatesMaleBmrAndMaintenanceCaloriesForSedentaryUser() {
        PhysicalData data = PhysicalData.validateAndCreate(30, 70, 175, "MALE", null);

        NutritionalGoal goal = service.calculateGoal(data, ActivityLevel.of("SEDENTARY"), "MAINTAIN_WEIGHT");

        assertEquals(1979, goal.getMaintenanceCalories());
        assertEquals(1979, goal.getCalorieTarget());
    }

    @Test
    void appliesCuttingAndBulkingOffsetsToFemaleCalories() {
        PhysicalData data = PhysicalData.validateAndCreate(30, 60, 165, "FEMALE", null);
        NutritionalGoal cutting = service.calculateGoal(data, ActivityLevel.of("LIGHTLY_ACTIVE"), "moderate_cutting");
        NutritionalGoal bulking = service.calculateGoal(data, ActivityLevel.of("LIGHTLY_ACTIVE"), "LIGHT_BULK");

        assertEquals(1815, cutting.getMaintenanceCalories());
        assertEquals(1515, cutting.getCalorieTarget());
        assertEquals(2015, bulking.getCalorieTarget());
    }

    @Test
    void rejectsNullOrUnsupportedGoalType() {
        PhysicalData data = PhysicalData.validateAndCreate(30, 70, 175, "MALE", null);
        ActivityLevel activity = ActivityLevel.of("SEDENTARY");

        assertThrows(IllegalArgumentException.class, () -> service.calculateGoal(data, activity, null));
        assertThrows(IllegalArgumentException.class, () -> service.calculateGoal(data, activity, "MAINTENANCEISH"));
    }

    @Test
    void rejectsCalculatedTargetsBelowSafeDomainMinimum() {
        PhysicalData extreme = PhysicalData.validateAndCreate(120, 10.01, 100.01, "FEMALE", null);

        assertThrows(IllegalArgumentException.class,
                () -> service.calculateGoal(extreme, ActivityLevel.of("SEDENTARY"), "AGGRESSIVE_CUTTING"));
    }
}
