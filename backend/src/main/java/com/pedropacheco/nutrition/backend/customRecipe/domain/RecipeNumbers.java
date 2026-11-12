package com.pedropacheco.nutrition.backend.customRecipe.domain;

import java.math.BigDecimal;

/** Input precision matches recipe decimal columns; calculations retain extra intermediate precision. */
final class RecipeNumbers {

    private RecipeNumbers() {}

    static BigDecimal nonNegative(BigDecimal value, String label) {
        if (value == null || value.signum() < 0) throw new IllegalArgumentException(label + " must be non-negative.");
        return value.stripTrailingZeros();
    }

    static BigDecimal stored(BigDecimal value, String label) {
        value = nonNegative(value, label);
        if (value.scale() > 10 || value.precision() - value.scale() > 20) {
            throw new IllegalArgumentException(label + " exceeds the supported decimal precision.");
        }
        return value;
    }

    static BigDecimal positive(BigDecimal value, String label) {
        value = stored(value, label);
        if (value.signum() == 0) throw new IllegalArgumentException(label + " must be positive.");
        return value;
    }
}
