package com.pedropacheco.nutrition.backend.userNutritionalProfile.domain;

import jakarta.persistence.Embeddable;
import java.util.Objects;

@Embeddable
public class MacroBreakdown {

    private int proteinGrams;
    private int carbsGrams;
    private int fatsGrams;

    protected MacroBreakdown() {}

    public MacroBreakdown(int proteinGrams, int carbsGrams, int fatsGrams) {
        if (proteinGrams < 0 || carbsGrams < 0 || fatsGrams < 0) {
            throw new IllegalArgumentException("Macronutrients cannot be negative.");
        }
        this.proteinGrams = proteinGrams;
        this.carbsGrams = carbsGrams;
        this.fatsGrams = fatsGrams;
    }

    /**
     * Calculates the macronutrients based on the given percentages.
     */
    public static MacroBreakdown fromPercentages(
            int calorieTarget,
            double proteinPercentage,
            double carbsPercentage,
            double fatsPercentage
    ) {
        double totalPercentage = proteinPercentage + carbsPercentage + fatsPercentage;
        if (Math.abs(totalPercentage - 100.0) > 0.01) {
            throw new IllegalArgumentException("Macronutrient percentages must add up to 100%. Current sum: " + totalPercentage + "%");
        }

        int proteinCalories = (int) Math.round(calorieTarget * (proteinPercentage / 100.0));
        int carbsCalories = (int) Math.round(calorieTarget * (carbsPercentage / 100.0));
        int fatsCalories = (int) Math.round(calorieTarget * (fatsPercentage / 100.0));

        return new MacroBreakdown(
                proteinCalories / 4,
                carbsCalories / 4,
                fatsCalories / 9
        );
    }

    public int getProteinGrams() { return proteinGrams; }
    public int getCarbsGrams() { return carbsGrams; }
    public int getFatsGrams() { return fatsGrams; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MacroBreakdown that = (MacroBreakdown) o;
        return proteinGrams == that.proteinGrams &&
                carbsGrams == that.carbsGrams &&
                fatsGrams == that.fatsGrams;
    }

    @Override
    public int hashCode() {
        return Objects.hash(proteinGrams, carbsGrams, fatsGrams);
    }
}