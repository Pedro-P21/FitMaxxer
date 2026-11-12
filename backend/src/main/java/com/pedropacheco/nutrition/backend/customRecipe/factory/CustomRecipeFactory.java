package com.pedropacheco.nutrition.backend.customRecipe.factory;

import com.pedropacheco.nutrition.backend.customRecipe.domain.*;
import com.pedropacheco.nutrition.backend.foodCatalog.dto.FoodCatalogDetailsDTO;
import com.pedropacheco.nutrition.backend.userAuth.domain.UserId;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Component
public class CustomRecipeFactory {
    public record ResolvedIngredient(FoodCatalogDetailsDTO food, IngredientQuantity quantity) {
        public ResolvedIngredient {
            Objects.requireNonNull(food, "Resolved food is required.");
            Objects.requireNonNull(quantity, "Ingredient quantity is required.");
        }
    }

    public CustomRecipe createRecipe(UserId owner, String name, BigDecimal numberOfServings,
                                     List<ResolvedIngredient> resolvedIngredients) {

        var ingredients = resolvedIngredients.stream().map(resolved -> {

            var food = resolved.food();
            var snapshot = new IngredientSnapshot(food.name(), food.brand(), food.barcode(), food.caloriesKcal(),
                    food.proteinGrams(), food.carbohydrateGrams(), food.fatGrams(),
                    IngredientSnapshot.Basis.valueOf(food.nutrientBasis()), food.servingQuantity(), food.servingUnit());
            return new RecipeIngredient(snapshot, resolved.quantity());

        }).toList();

        return new CustomRecipe(owner, new RecipeName(name), numberOfServings, ingredients);
    }
}
