package com.pedropacheco.nutrition.backend.userNutritionalProfile.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NutritionalGoalTest {

    private final MacroBreakdown macros = new MacroBreakdown(100, 100, 100); // 1,700 kcal

    @Test
    void createsGoalAtMinimumCaloriesAndMaximumAllowedMacroDifference() {
        NutritionalGoal goal = new NutritionalGoal(500, 1685, macros);

        assertEquals(500, goal.getMaintenanceCalories());
        assertEquals(1685, goal.getCalorieTarget());
        assertSame(macros, goal.getMacroBreakdown());
    }

    @Test
    void rejectsMaintenanceCaloriesBelowMinimum() {
        assertThrows(IllegalArgumentException.class, () -> new NutritionalGoal(499, 1700, macros));
    }

    @Test
    void rejectsTargetsBelowMinimumOrMacrosOutsideTolerance() {
        assertThrows(IllegalArgumentException.class, () -> new NutritionalGoal(1700, 999, macros));
        assertThrows(IllegalArgumentException.class, () -> new NutritionalGoal(1700, 1716, macros));
    }

    @Test
    void rejectsNullMacros() {
        assertThrows(IllegalArgumentException.class, () -> new NutritionalGoal(1700, 1700, null));
    }
}
