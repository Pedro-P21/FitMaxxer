package com.pedropacheco.nutrition.backend.customRecipe.dto;

import java.math.BigDecimal;
import java.util.List;

public record CustomRecipeResponseDTO(String recipeId, String name, BigDecimal numberOfServings,
        List<RecipeIngredientResponseDTO> ingredients, RecipeNutrientsDTO totalNutrition,
        RecipeNutrientsDTO perServingNutrition) {
    public CustomRecipeResponseDTO { ingredients = List.copyOf(ingredients); }
}
