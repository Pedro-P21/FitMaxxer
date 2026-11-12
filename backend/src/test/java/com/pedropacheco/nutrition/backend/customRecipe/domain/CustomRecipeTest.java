package com.pedropacheco.nutrition.backend.customRecipe.domain;

import com.pedropacheco.nutrition.backend.userAuth.domain.UserId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class CustomRecipeTest {
    static BigDecimal n(String value) { return new BigDecimal(value); }
    static IngredientSnapshot food(String name, IngredientSnapshot.Basis basis, String serving, String unit) {
        return new IngredientSnapshot(name, null, null, n("200"), n("10"), n("30"), n("5"),
                basis, n(serving), unit);
    }
    static RecipeIngredient ingredient(IngredientSnapshot food, String amount, IngredientQuantity.Unit unit) {
        return new RecipeIngredient(food, new IngredientQuantity(n(amount), unit));
    }
    static CustomRecipe recipe(BigDecimal servings, List<RecipeIngredient> ingredients) {
        return new CustomRecipe(new UserId("owner"), new RecipeName("Homemade bowl"), servings, ingredients);
    }
    static void equalsNumber(String expected, BigDecimal actual) {
        assertEquals(0, n(expected).compareTo(actual), () -> "Expected " + expected + " but got " + actual);
    }

    @Test
    void calculatesWholeRecipeAndPerServingCaloriesAndAllMacros() {
        var a = food("Food A", IngredientSnapshot.Basis.PER_100_GRAMS, "100", "g");
        var b = new IngredientSnapshot("Food B", null, null, n("100"), n("5"), n("10"), n("4"),
                IngredientSnapshot.Basis.PER_SERVING, BigDecimal.ONE, "serving");
        var recipe = recipe(n("3"), List.of(ingredient(a, "150", IngredientQuantity.Unit.GRAM),
                ingredient(b, "3", IngredientQuantity.Unit.SERVING)));
        var totals = recipe.getMacros();
        equalsNumber("600", totals.getCaloriesKcal());
        equalsNumber("30", totals.getProteinGrams());
        equalsNumber("75", totals.getCarbohydrateGrams());
        equalsNumber("19.5", totals.getFatGrams());
        var serving = recipe.getPerServingNutrition();
        equalsNumber("200", serving.caloriesKcal());
        equalsNumber("10", serving.proteinGrams());
        equalsNumber("25", serving.carbohydrateGrams());
        equalsNumber("6.5", serving.fatGrams());
        assertNotNull(totals.getId());
    }

    @Test
    void keepsDuplicateFoodRowsDistinctAndDefensivelyCopiesTheCollection() {
        var snapshot = food("Oats", IngredientSnapshot.Basis.PER_100_GRAMS, "100", "g");
        var first = ingredient(snapshot, "50", IngredientQuantity.Unit.GRAM);
        var second = ingredient(snapshot, "50", IngredientQuantity.Unit.GRAM);
        var source = new ArrayList<>(List.of(first, second));
        var recipe = recipe(n("2"), source);
        source.clear();
        assertEquals(2, recipe.getIngredients().size());
        assertNotEquals(first.getId(), second.getId());
        assertThrows(UnsupportedOperationException.class, () -> recipe.getIngredients().clear());
        equalsNumber("200", recipe.getMacros().getCaloriesKcal());
        assertThrows(IllegalArgumentException.class, () -> recipe(n("1"), List.of(first)));
    }

    @Test
    void rejectsReusingTheSameRowWithoutAttachingItOnFailedConstruction() {
        var row = ingredient(food("Oats", IngredientSnapshot.Basis.PER_SERVING, "1", "serving"),
                "1", IngredientQuantity.Unit.SERVING);
        assertThrows(IllegalArgumentException.class, () -> recipe(n("1"), List.of(row, row)));
        assertDoesNotThrow(() -> recipe(n("1"), List.of(row)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "0.00000000001", "100000000000000000000"})
    void rejectsInvalidServingCountsAndQuantities(String value) {
        var row = ingredient(food("Food", IngredientSnapshot.Basis.PER_SERVING, "1", "serving"),
                "1", IngredientQuantity.Unit.SERVING);
        assertThrows(IllegalArgumentException.class, () -> recipe(n(value), List.of(row)));
        assertThrows(IllegalArgumentException.class, () -> new IngredientQuantity(n(value), IngredientQuantity.Unit.GRAM));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void rejectsMissingRecipeNamesAndIdentifiers(String value) {
        assertThrows(IllegalArgumentException.class, () -> new RecipeName(value));
        assertThrows(IllegalArgumentException.class, () -> new RecipeId(value));
    }

    @Test
    void rejectsEmptyRecipesNullUnitsAndMissingOrNegativeNutrients() {
        assertThrows(IllegalArgumentException.class, () -> recipe(BigDecimal.ONE, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new IngredientQuantity(BigDecimal.ONE, null));
        assertThrows(IllegalArgumentException.class, () -> new IngredientQuantity(null, IngredientQuantity.Unit.GRAM));
        for (BigDecimal calories : new BigDecimal[]{null, n("-1")}) {
            assertThrows(IllegalArgumentException.class, () -> new IngredientSnapshot("Food", null, null,
                    calories, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    IngredientSnapshot.Basis.PER_SERVING, BigDecimal.ONE, "serving"));
        }
    }

    @Test
    void supportsZeroNutritionFractionalServingsAndPredictableDivisionRounding() {
        var water = new IngredientSnapshot("Water", null, null, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, IngredientSnapshot.Basis.PER_100_MILLILITERS, n("250"), "ml");
        var recipe = recipe(n("0.5"), List.of(ingredient(water, "0.5", IngredientQuantity.Unit.SERVING)));
        equalsNumber("0", recipe.getPerServingNutrition().caloriesKcal());
        var thirds = recipe(n("3"), List.of(ingredient(food("Food", IngredientSnapshot.Basis.PER_SERVING,
                "1", "serving"), "1", IngredientQuantity.Unit.SERVING)));
        equalsNumber("66.6666666667", thirds.getPerServingNutrition().caloriesKcal());
    }

    @Test
    void valueObjectsUseValueEqualityRegardlessOfDecimalScale() {
        assertEquals(new IngredientQuantity(n("1.00"), IngredientQuantity.Unit.GRAM),
                new IngredientQuantity(n("1"), IngredientQuantity.Unit.GRAM));
        assertEquals(new RecipeName(" Bowl "), new RecipeName("Bowl"));
        var first = food("Food", IngredientSnapshot.Basis.PER_100_GRAMS, "100.0", "g");
        var second = food("Food", IngredientSnapshot.Basis.PER_100_GRAMS, "100", "g");
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void sumsBeforeRoundingStoredTotals() {
        var tiny = new IngredientSnapshot("Tiny", null, null, n("0.3333333333"), n("0"), n("0"), n("0"),
                IngredientSnapshot.Basis.PER_100_GRAMS, n("100"), "g");
        var recipe = recipe(n("1"), List.of(ingredient(tiny, "1", IngredientQuantity.Unit.GRAM),
                ingredient(tiny, "1", IngredientQuantity.Unit.GRAM),
                ingredient(tiny, "1", IngredientQuantity.Unit.GRAM)));
        equalsNumber("0.0100000000", recipe.getMacros().getCaloriesKcal());
    }
}
