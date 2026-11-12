package com.pedropacheco.nutrition.backend.customRecipe.dto;

import com.pedropacheco.nutrition.backend.customRecipe.domain.RecipeName;
import java.math.BigDecimal;
import java.util.List;

public record CreateRecipeRequestDTO(String name, BigDecimal numberOfServings,
                                     List<RecipeIngredientSelection> ingredients) {
    public CreateRecipeRequestDTO {
        name = new RecipeName(name).getValue();
        if (numberOfServings == null || numberOfServings.signum() <= 0)
            throw new IllegalArgumentException("Number of servings must be positive.");
        numberOfServings = numberOfServings.stripTrailingZeros();
        if (numberOfServings.scale() > 10 || numberOfServings.precision() - numberOfServings.scale() > 20)
            throw new IllegalArgumentException("Number of servings exceeds the supported decimal precision.");
        if (ingredients == null || ingredients.isEmpty() || ingredients.stream().anyMatch(java.util.Objects::isNull))
            throw new IllegalArgumentException("A recipe needs at least one valid ingredient.");
        ingredients = List.copyOf(ingredients);
    }
}
