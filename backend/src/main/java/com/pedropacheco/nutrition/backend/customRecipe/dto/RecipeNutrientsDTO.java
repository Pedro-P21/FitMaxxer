package com.pedropacheco.nutrition.backend.customRecipe.dto;

import java.math.BigDecimal;

public record RecipeNutrientsDTO(BigDecimal caloriesKcal, BigDecimal proteinGrams,
                                 BigDecimal carbohydrateGrams, BigDecimal fatGrams) {}
