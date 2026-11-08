package com.pedropacheco.nutrition.backend.foodCatalog.factory;

import com.pedropacheco.nutrition.backend.foodCatalog.domain.Barcode;
import com.pedropacheco.nutrition.backend.foodCatalog.domain.Brand;
import com.pedropacheco.nutrition.backend.foodCatalog.domain.FoodId;
import com.pedropacheco.nutrition.backend.foodCatalog.domain.FoodItem;
import com.pedropacheco.nutrition.backend.foodCatalog.domain.FoodName;
import com.pedropacheco.nutrition.backend.foodCatalog.domain.NutrientComposition;
import com.pedropacheco.nutrition.backend.foodCatalog.domain.ServingSize;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class FoodItemFactory {

    public FoodItem createFoodItem(String id, String name, String brand, String barcode,
                                   BigDecimal caloriesKcal, BigDecimal proteinGrams,
                                   BigDecimal carbohydrateGrams, BigDecimal fatGrams,
                                   NutrientComposition.Basis basis,
                                   BigDecimal servingQuantity, String servingUnit) {
        Barcode barcodeValue = barcode == null || barcode.isBlank() ? null : new Barcode(barcode);
        Brand brandValue = brand == null || brand.isBlank() ? null : new Brand(brand);
        return new FoodItem(
                new FoodId(id),
                new FoodName(name),
                brandValue,
                barcodeValue,
                new NutrientComposition(caloriesKcal, proteinGrams, carbohydrateGrams, fatGrams, basis),
                new ServingSize(servingQuantity, servingUnit)
        );
    }
}
