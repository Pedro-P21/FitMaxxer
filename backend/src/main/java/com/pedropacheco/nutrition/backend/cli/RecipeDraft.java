package com.pedropacheco.nutrition.backend.cli;

import com.pedropacheco.nutrition.backend.customRecipe.domain.*;
import com.pedropacheco.nutrition.backend.customRecipe.dto.RecipeIngredientSelection;
import com.pedropacheco.nutrition.backend.foodCatalog.dto.FoodItemDTO;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Local draft only: no recipe identity, persistence, or shared state between user sessions. */
public final class RecipeDraft {
    private final RecipeName name;
    private final BigDecimal numberOfServings;
    private final List<Entry> ingredients = new ArrayList<>();

    public RecipeDraft(String name, BigDecimal numberOfServings) {
        this.name = new RecipeName(name);
        if (numberOfServings == null || numberOfServings.signum() <= 0)
            throw new IllegalArgumentException("Number of servings must be positive.");
        BigDecimal normalized = numberOfServings.stripTrailingZeros();
        if (normalized.scale() > 10 || normalized.precision() - normalized.scale() > 20)
            throw new IllegalArgumentException("Number of servings exceeds the supported decimal precision.");
        this.numberOfServings = normalized;
    }
    public void addIngredient(FoodItemDTO food, IngredientQuantity quantity) {
        Objects.requireNonNull(food, "Food is required.");

        var snapshot = new IngredientSnapshot(food.name(), food.brand(), food.barcode(),
                food.caloriesKcal(), food.proteinGrams(), food.carbohydrateGrams(), food.fatGrams(),
                IngredientSnapshot.Basis.valueOf(food.nutrientBasis()), food.servingQuantity(), food.servingUnit());

        snapshot.calculateContribution(quantity);

        var selection = new RecipeIngredientSelection(food.foodId(), quantity.getAmount(), quantity.getUnit());

        ingredients.add(new Entry(food.name(), selection));
    }
    public RecipeName getName() { return name; }

    public BigDecimal getNumberOfServings() { return numberOfServings; }

    public List<Entry> getIngredients() { return List.copyOf(ingredients); }

    public List<RecipeIngredientSelection> getSelections() {
        return ingredients.stream().map(Entry::selection).toList();
    }
    public record Entry(String foodName, RecipeIngredientSelection selection) {}
}
