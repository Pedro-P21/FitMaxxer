package com.pedropacheco.nutrition.backend.dailyLog.dto;

import com.pedropacheco.nutrition.backend.dailyLog.domain.MealType;
import java.math.BigDecimal;

/** Recipes are logged in recipe servings; no mass/volume conversion is implied. */
public record AddRecipeMealItemRequestDTO(String recipeId, String mealType, BigDecimal quantity) {
    public AddRecipeMealItemRequestDTO {
        if (recipeId == null || recipeId.isBlank()) throw new IllegalArgumentException("Recipe ID is required.");
        recipeId = recipeId.trim();
        new MealType(mealType);
        if (quantity == null || quantity.signum() <= 0) throw new IllegalArgumentException("Recipe servings must be positive.");
        quantity = quantity.stripTrailingZeros();
        // Matches DailyLog's existing decimal(12,4) portion storage.
        if (quantity.scale() > 4 || quantity.precision() - quantity.scale() > 8)
            throw new IllegalArgumentException("Recipe servings support up to 8 integer and 4 decimal digits.");
    }
}
