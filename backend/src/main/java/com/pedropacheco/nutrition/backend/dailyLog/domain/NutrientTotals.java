package com.pedropacheco.nutrition.backend.dailyLog.domain;

import java.math.BigDecimal;
import java.util.Objects;

public record NutrientTotals(BigDecimal caloriesKcal, BigDecimal proteinGrams,
                             BigDecimal carbohydrateGrams, BigDecimal fatGrams) {
    public static final NutrientTotals ZERO = new NutrientTotals(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

    public NutrientTotals {
        Objects.requireNonNull(caloriesKcal);
        Objects.requireNonNull(proteinGrams);
        Objects.requireNonNull(carbohydrateGrams);
        Objects.requireNonNull(fatGrams);
    }

    public NutrientTotals plus(NutrientTotals other) {
        return new NutrientTotals(caloriesKcal.add(other.caloriesKcal), proteinGrams.add(other.proteinGrams),
                carbohydrateGrams.add(other.carbohydrateGrams), fatGrams.add(other.fatGrams));
    }
}
