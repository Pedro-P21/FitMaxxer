package com.pedropacheco.nutrition.backend.customRecipe.dto;

import com.pedropacheco.nutrition.backend.customRecipe.domain.IngredientQuantity;
import java.math.BigDecimal;

/** Draft submission data: an ingredient reference and quantity, never authoritative catalog nutrition. */
public record RecipeIngredientSelection(String foodId, BigDecimal quantity, IngredientQuantity.Unit quantityUnit) {

    public RecipeIngredientSelection {
        if (foodId == null || foodId.isBlank()) throw new IllegalArgumentException("Food ID is required.");
        foodId = foodId.trim();
        var validated = new IngredientQuantity(quantity, quantityUnit);
        quantity = validated.getAmount();
    }
}
