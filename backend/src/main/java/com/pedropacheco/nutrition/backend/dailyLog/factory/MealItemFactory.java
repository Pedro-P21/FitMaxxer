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
    public void changeItemQuantity(MealItem item, BigDecimal newAmount) {
        // Enforce the persisted decimal(12,4) bounds before replacing the value object.
        if (newAmount == null || newAmount.signum() <= 0)
            throw new IllegalArgumentException("Quantity must be positive.");
        newAmount = newAmount.stripTrailingZeros();
        if (newAmount.scale() > 4 || newAmount.precision() - newAmount.scale() > 8)
            throw new IllegalArgumentException("Quantity supports up to 8 integer and 4 decimal digits.");
        PortionQuantity previous = item.getPortionQuantity();
        PortionQuantity replacement = previous.getUnit() == PortionQuantity.Unit.NAMED
                ? PortionQuantity.named(previous.getLabel(), newAmount,
                        previous.getEquivalentAmount(), previous.getEquivalentUnit())
                : new PortionQuantity(newAmount, previous.getUnit());
        item.replacePortionQuantity(replacement);
    }

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
