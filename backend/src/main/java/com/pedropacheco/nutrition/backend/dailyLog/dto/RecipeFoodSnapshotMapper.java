package com.pedropacheco.nutrition.backend.dailyLog.dto;

import com.pedropacheco.nutrition.backend.customRecipe.dto.CustomRecipeResponseDTO;
import com.pedropacheco.nutrition.backend.dailyLog.domain.FoodSnapshot;
import com.pedropacheco.nutrition.backend.foodCatalog.domain.NutrientComposition;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class RecipeFoodSnapshotMapper {
    public FoodSnapshot toFoodSnapshot(CustomRecipeResponseDTO recipe) {
        var nutrients = recipe.perServingNutrition();
        return new FoodSnapshot(recipe.name(), null, null, nutrients.caloriesKcal(), nutrients.proteinGrams(),
                nutrients.carbohydrateGrams(), nutrients.fatGrams(), NutrientComposition.Basis.PER_SERVING,
                BigDecimal.ONE, "serving");
    }
}
