package com.pedropacheco.nutrition.backend.customRecipe.dto;

import com.pedropacheco.nutrition.backend.customRecipe.domain.*;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
public class CustomRecipeMapper {
    public CustomRecipeResponseDTO toResponseDTO(CustomRecipe recipe) {

        var ingredients = recipe.getIngredients().stream().map(row -> {
            var snapshot = row.getSnapshot();

            return new RecipeIngredientResponseDTO(row.getId(), snapshot.getFoodName(), snapshot.getBrand(),
                    snapshot.getBarcode(), row.getQuantity().getAmount(), row.getQuantity().getUnit(),
                    snapshot.getNutrientBasis(), snapshot.getServingSizeQuantity(), snapshot.getServingSizeUnit(),
                    new RecipeNutrientsDTO(snapshot.getCaloriesKcal(), snapshot.getProteinGrams(),
                            snapshot.getCarbohydrateGrams(), snapshot.getFatGrams()));

        }).toList();

        return new CustomRecipeResponseDTO(recipe.getId().getValue(), recipe.getName().getValue(),
                recipe.getNumberOfServings(), ingredients, nutrients(recipe.getMacros().getValues()),
                nutrients(recipe.getPerServingNutrition()));
    }
    public RecipePageResponseDTO toPageResponseDTO(Page<CustomRecipe> recipes) {

        var items = recipes.getContent().stream().map(recipe -> new RecipeSummaryDTO(recipe.getId().getValue(),
                recipe.getName().getValue(), recipe.getNumberOfServings(), nutrients(recipe.getMacros().getValues()),
                nutrients(recipe.getPerServingNutrition()))).toList();

        return new RecipePageResponseDTO(items, recipes.getNumber(), recipes.getSize(),
                recipes.getTotalElements(), recipes.getTotalPages());
    }

    private RecipeNutrientsDTO nutrients(RecipeMacros.Values values) {
        return new RecipeNutrientsDTO(values.caloriesKcal(), values.proteinGrams(),
                values.carbohydrateGrams(), values.fatGrams());
    }
}
