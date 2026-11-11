package com.pedropacheco.nutrition.backend.dailyLog.domain;

import com.pedropacheco.nutrition.backend.userNutritionalProfile.domain.NutritionalGoal;
import jakarta.persistence.Embeddable;
import java.util.Objects;

@Embeddable
public class DailyGoalSnapshot {
    private int calorieTarget;
    private int proteinTargetGrams;
    private int carbohydrateTargetGrams;
    private int fatTargetGrams;

    protected DailyGoalSnapshot() {}

    public DailyGoalSnapshot(int calorieTarget, int proteinTargetGrams,
                             int carbohydrateTargetGrams, int fatTargetGrams) {
        if (calorieTarget <= 0 || proteinTargetGrams < 0 || carbohydrateTargetGrams < 0 || fatTargetGrams < 0) {
            throw new IllegalArgumentException("Daily targets must be positive calories and non-negative macros.");
        }
        this.calorieTarget = calorieTarget;
        this.proteinTargetGrams = proteinTargetGrams;
        this.carbohydrateTargetGrams = carbohydrateTargetGrams;
        this.fatTargetGrams = fatTargetGrams;
    }

    public static DailyGoalSnapshot from(NutritionalGoal goal) {
        Objects.requireNonNull(goal, "Nutritional goal cannot be null.");
        var macros = goal.getMacroBreakdown();
        return new DailyGoalSnapshot(goal.getCalorieTarget(), macros.getProteinGrams(),
                macros.getCarbsGrams(), macros.getFatsGrams());
    }

    public int getCalorieTarget() { return calorieTarget; }
    public int getProteinTargetGrams() { return proteinTargetGrams; }
    public int getCarbohydrateTargetGrams() { return carbohydrateTargetGrams; }
    public int getFatTargetGrams() { return fatTargetGrams; }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof DailyGoalSnapshot that)) return false;
        return calorieTarget == that.calorieTarget && proteinTargetGrams == that.proteinTargetGrams
                && carbohydrateTargetGrams == that.carbohydrateTargetGrams && fatTargetGrams == that.fatTargetGrams;
    }
    @Override public int hashCode() {
        return Objects.hash(calorieTarget, proteinTargetGrams, carbohydrateTargetGrams, fatTargetGrams);
    }
}
