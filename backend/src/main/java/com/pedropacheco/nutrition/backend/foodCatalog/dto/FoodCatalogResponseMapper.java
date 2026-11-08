package com.pedropacheco.nutrition.backend.foodCatalog.dto;

import com.pedropacheco.nutrition.backend.foodCatalog.domain.FoodItem;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FoodCatalogResponseMapper {

    public FoodCatalogResponseDTO toResponseDTO(List<FoodItem> foodItems) {
        List<FoodItemDTO> items = foodItems == null ? List.of()
                : foodItems.stream().map(this::toFoodItemDTO).toList();
        String message = items.isEmpty() ? "No food items matched your search." : null;
        return new FoodCatalogResponseDTO(items, message);
    }

    private FoodItemDTO toFoodItemDTO(FoodItem item) {
        var nutrients = item.getNutrientComposition();
        var serving = item.getServingSize();
        return new FoodItemDTO(
                item.getId().getValue(),
                item.getName().getValue(),
                item.getBrand() == null ? null : item.getBrand().getValue(),
                item.getBarcode() == null ? null : item.getBarcode().getValue(),
                serving.getQuantity(),
                serving.getUnit(),
                nutrients.getCaloriesKcal(),
                nutrients.getProteinGrams(),
                nutrients.getCarbohydrateGrams(),
                nutrients.getFatGrams(),
                nutrients.getBasis().name()
        );
    }
}
