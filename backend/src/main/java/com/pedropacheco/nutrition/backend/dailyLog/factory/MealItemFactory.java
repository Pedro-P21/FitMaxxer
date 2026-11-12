package com.pedropacheco.nutrition.backend.dailyLog.factory;

import com.pedropacheco.nutrition.backend.dailyLog.domain.FoodSnapshot;
import com.pedropacheco.nutrition.backend.dailyLog.domain.MealItem;
import com.pedropacheco.nutrition.backend.dailyLog.domain.PortionQuantity;
import com.pedropacheco.nutrition.backend.foodCatalog.domain.NutrientComposition;
import com.pedropacheco.nutrition.backend.foodCatalog.dto.FoodCatalogDetailsDTO;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class MealItemFactory {
    public MealItem create(FoodCatalogDetailsDTO food, BigDecimal quantity, PortionQuantity.Unit quantityUnit) {
        NutrientComposition.Basis basis;
        try {
            basis = NutrientComposition.Basis.valueOf(food.nutrientBasis());
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Food has an unsupported nutrient measurement basis.", exception);
        }
        FoodSnapshot snapshot = new FoodSnapshot(food.name(), food.brand(), food.barcode(),
                food.caloriesKcal(), food.proteinGrams(), food.carbohydrateGrams(), food.fatGrams(),
                basis, food.servingQuantity(), food.servingUnit());
        return create(snapshot, quantity, quantityUnit);
    }

    public MealItem create(FoodSnapshot snapshot, BigDecimal quantity, PortionQuantity.Unit unit) {
        return new MealItem(snapshot, new PortionQuantity(quantity, unit));
    }
}
