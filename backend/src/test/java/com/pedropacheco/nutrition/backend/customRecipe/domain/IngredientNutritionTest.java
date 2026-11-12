package com.pedropacheco.nutrition.backend.customRecipe.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static com.pedropacheco.nutrition.backend.customRecipe.domain.CustomRecipeTest.*;
import static org.junit.jupiter.api.Assertions.*;

class IngredientNutritionTest {
    @ParameterizedTest
    @CsvSource({
        "PER_100_GRAMS,100,g,150,GRAM,300",
        "PER_100_GRAMS,50,g,1.5,SERVING,150",
        "PER_100_GRAMS,0.1,kg,2,SERVING,400",
        "PER_100_MILLILITERS,250,ml,0.5,SERVING,250",
        "PER_100_MILLILITERS,1,l,25,MILLILITER,50",
        "PER_SERVING,50,g,75,GRAM,300",
        "PER_SERVING,0.25,l,125,MILLILITER,100",
        "PER_SERVING,1,serving,0.5,SERVING,100"
    })
    void scalesSupportedBasesAndConversions(IngredientSnapshot.Basis basis, String serving, String servingUnit,
                                            String amount, IngredientQuantity.Unit unit, String expectedCalories) {
        var row = ingredient(food("Food", basis, serving, servingUnit), amount, unit);
        equalsNumber(expectedCalories, row.calculateContribution().caloriesKcal());
    }
    @Test
    void rejectsCrossDimensionAndUnknownServingConversionsButAllowsExplicitBasisAmount() {
        var liquid = food("Liquid", IngredientSnapshot.Basis.PER_100_MILLILITERS, "250", "ml");
        assertThrows(IllegalArgumentException.class, () -> ingredient(liquid, "100", IngredientQuantity.Unit.GRAM));
        var unknown = food("Bowl", IngredientSnapshot.Basis.PER_100_GRAMS, "1", "bowl");
        assertThrows(IllegalArgumentException.class, () -> ingredient(unknown, "1", IngredientQuantity.Unit.SERVING));
        assertDoesNotThrow(() -> ingredient(unknown, "100", IngredientQuantity.Unit.GRAM));
        var portion = food("Serving", IngredientSnapshot.Basis.PER_SERVING, "1", "serving");
        assertThrows(IllegalArgumentException.class, () -> ingredient(portion, "10", IngredientQuantity.Unit.GRAM));
    }
}
