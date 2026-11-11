package com.pedropacheco.nutrition.backend.foodCatalog.dto.mapper;

import com.pedropacheco.nutrition.backend.foodCatalog.domain.FoodItem;
import com.pedropacheco.nutrition.backend.foodCatalog.domain.NutrientComposition;
import com.pedropacheco.nutrition.backend.foodCatalog.dto.OpenFoodFactsNutrimentsDTO;
import com.pedropacheco.nutrition.backend.foodCatalog.dto.OpenFoodFactsProductDTO;
import com.pedropacheco.nutrition.backend.foodCatalog.dto.OpenFoodFactsSearchResponseDTO;
import com.pedropacheco.nutrition.backend.foodCatalog.factory.FoodItemFactory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OpenFoodFactsMapperTest {
    private final OpenFoodFactsMapper mapper = new OpenFoodFactsMapper(new FoodItemFactory());

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
        OpenFoodFactsSearchResponseDTO response = new OpenFoodFactsSearchResponseDTO(1, 1, 1, List.of(invalid));

        assertNull(mapper.toDomainItem(noBrand).orElseThrow().getBrand());
        assertTrue(mapper.toDomainItems(response).isEmpty());
    }

    @Test
    void prefersNormalizedPerServingValuesEvenWhenTheLabelBasisIsPerHundred() {
        OpenFoodFactsProductDTO product = product("Continente", "250", "ml", "100g",
                new OpenFoodFactsNutrimentsDTO("45", "1.2", "6.5", "1.5",
                        "87.5", "8.75", "12.5", "0.25"));

        FoodItem item = mapper.toDomainItem(product).orElseThrow();

        assertEquals(NutrientComposition.Basis.PER_SERVING, item.getNutrientComposition().getBasis());
        assertEquals(0, item.getNutrientComposition().getCaloriesKcal().compareTo(new java.math.BigDecimal("87.5")));
    }

    @Test
    void mapsOpenFoodFactsNormalizedLiquidValuesToPerHundredMilliliters() {
        OpenFoodFactsProductDTO product = product("Continente", "250", "ml", "100g",
                new OpenFoodFactsNutrimentsDTO("45", "1.2", "6.5", "1.5", null, null, null, null));

        FoodItem item = mapper.toDomainItem(product).orElseThrow();

        assertEquals(NutrientComposition.Basis.PER_100_MILLILITERS, item.getNutrientComposition().getBasis());
    }

    private OpenFoodFactsProductDTO product(String brands, String servingQuantity, String unit) {
        return product(brands, servingQuantity, unit, null,
                new OpenFoodFactsNutrimentsDTO("45", "1.2", "6.5", "1.5", null, null, null, null));
    }

    private OpenFoodFactsProductDTO product(String brands, String servingQuantity, String unit,
                                             String nutritionDataPer, OpenFoodFactsNutrimentsDTO nutriments) {
        return new OpenFoodFactsProductDTO("12345678", "Oat Drink", brands, null,
                servingQuantity, unit, nutritionDataPer, nutriments);
    }
}
