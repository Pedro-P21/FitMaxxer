package com.pedropacheco.nutrition.backend.foodCatalog.dto;

import com.pedropacheco.nutrition.backend.foodCatalog.domain.FoodItem;
import com.pedropacheco.nutrition.backend.foodCatalog.factory.FoodItemFactory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FoodItemMapperTest {
    private final FoodItemMapper mapper = new FoodItemMapper(new FoodItemFactory());

    @Test
    void mapsBrandAndNutritionToDomainItem() {
        OpenFoodFactsProductDTO product = product("Acme Foods", "250", "g");

        FoodItem item = mapper.toDomainItem(product).orElseThrow();

        assertEquals("Acme Foods", item.getBrand().getValue());
        assertEquals("Oat Drink", item.getName().getValue());
        assertEquals("12345678", item.getBarcode().getValue());
        assertEquals("250", item.getServingSize().getQuantity().toPlainString());
        assertEquals(45, item.getNutrientComposition().getCaloriesKcal().intValue());
    }

    @Test
    void allowsMissingBrandAndSkipsInvalidProducts() {
        OpenFoodFactsProductDTO noBrand = product(null, "250", "g");
        OpenFoodFactsProductDTO invalid = product("Acme Foods", null, "g");
        OpenFoodFactsSearchResponseDTO response = new OpenFoodFactsSearchResponseDTO();
        response.setProducts(List.of(invalid));

        assertNull(mapper.toDomainItem(noBrand).orElseThrow().getBrand());
        assertTrue(mapper.toDomainItems(response).isEmpty());
    }

    private OpenFoodFactsProductDTO product(String brands, String servingQuantity, String unit) {
        OpenFoodFactsProductDTO product = new OpenFoodFactsProductDTO();
        product.setCode("12345678");
        product.setProductName("Oat Drink");
        product.setBrands(brands);
        product.setServingQuantity(servingQuantity);
        product.setServingQuantityUnit(unit);
        OpenFoodFactsNutrimentsDTO nutriments = new OpenFoodFactsNutrimentsDTO();
        nutriments.setEnergyKcalPer100g("45");
        nutriments.setProteinsPer100g("1.2");
        nutriments.setCarbohydratesPer100g("6.5");
        nutriments.setFatPer100g("1.5");
        product.setNutriments(nutriments);
        return product;
    }
}
