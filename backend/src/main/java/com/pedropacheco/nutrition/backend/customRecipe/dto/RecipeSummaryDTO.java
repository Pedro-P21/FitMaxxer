package com.pedropacheco.nutrition.backend.customRecipe.dto;

import java.math.BigDecimal;

public record RecipeSummaryDTO(String recipeId, String name, BigDecimal numberOfServings,
        RecipeNutrientsDTO totalNutrition, RecipeNutrientsDTO perServingNutrition) {}
