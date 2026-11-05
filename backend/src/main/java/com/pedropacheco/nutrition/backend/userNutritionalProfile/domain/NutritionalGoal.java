package com.pedropacheco.nutrition.backend.userNutritionalProfile.domain;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import java.util.Objects;

@Embeddable
public class NutritionalGoal {

    private int maintenanceCalories;
    private int calorieTarget;

    @Embedded
    private MacroBreakdown macroBreakdown;

    protected NutritionalGoal() {}

    public NutritionalGoal(int maintenanceCalories, int calorieTarget, MacroBreakdown macroBreakdown) {
        if (maintenanceCalories < 500) {
            throw new IllegalArgumentException("Maintenance calories cannot be unrealistically low.");
        }
        if (calorieTarget < 1000) {
            throw new IllegalArgumentException("Calorie target is critically low and unsafe.");
        }
        if (macroBreakdown == null) {
            throw new IllegalArgumentException("Macro breakdown must not be null.");
        }

      //Check if the calculated calories from macros matches the target calories
        int calculatedCaloriesFromMacros = (macroBreakdown.getProteinGrams() * 4) +
                (macroBreakdown.getCarbsGrams() * 4) +
                (macroBreakdown.getFatsGrams() * 9);

        // Allow a small margin of error for calorie target
        if (Math.abs(calculatedCaloriesFromMacros - calorieTarget) > 15) {
            throw new IllegalArgumentException(
                    String.format("Macro breakdown calories (%d kcal) do not match the calorie target (%d kcal).",
                            calculatedCaloriesFromMacros, calorieTarget)
            );
        }

        this.maintenanceCalories = maintenanceCalories;
        this.calorieTarget = calorieTarget;
        this.macroBreakdown = macroBreakdown;
    }

    public int getMaintenanceCalories() { return maintenanceCalories; }
    public int getCalorieTarget() { return calorieTarget; }
    public MacroBreakdown getMacroBreakdown() { return macroBreakdown; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NutritionalGoal that = (NutritionalGoal) o;
        return maintenanceCalories == that.maintenanceCalories &&
                calorieTarget == that.calorieTarget &&
                Objects.equals(macroBreakdown, that.macroBreakdown);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maintenanceCalories, calorieTarget, macroBreakdown);
    }
}