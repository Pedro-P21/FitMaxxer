package com.pedropacheco.nutrition.backend.dailyLog.domain;

import java.math.BigDecimal;
import java.util.Objects;

/** Calculated daily intake, targets, and remaining amounts. It is not persisted. */
public record DailyNutritionSummary(NutrientTotals consumed, NutrientTotals targets, NutrientTotals remaining) {
    public DailyNutritionSummary {
        Objects.requireNonNull(consumed);
        Objects.requireNonNull(targets);
        Objects.requireNonNull(remaining);
    }

    public static DailyNutritionSummary from(NutrientTotals consumed, DailyGoalSnapshot goalSnapshot) {
        Objects.requireNonNull(consumed);
        Objects.requireNonNull(goalSnapshot);
        NutrientTotals targets = new NutrientTotals(BigDecimal.valueOf(goalSnapshot.getCalorieTarget()),
                BigDecimal.valueOf(goalSnapshot.getProteinTargetGrams()),
                BigDecimal.valueOf(goalSnapshot.getCarbohydrateTargetGrams()),
                BigDecimal.valueOf(goalSnapshot.getFatTargetGrams()));
        return new DailyNutritionSummary(consumed, targets, new NutrientTotals(
                targets.caloriesKcal().subtract(consumed.caloriesKcal()),
                targets.proteinGrams().subtract(consumed.proteinGrams()),
                targets.carbohydrateGrams().subtract(consumed.carbohydrateGrams()),
                targets.fatGrams().subtract(consumed.fatGrams())));
    }

    public static DailyNutritionSummary empty(DailyGoalSnapshot goalSnapshot) {
        return from(NutrientTotals.ZERO, goalSnapshot);
    }
}
