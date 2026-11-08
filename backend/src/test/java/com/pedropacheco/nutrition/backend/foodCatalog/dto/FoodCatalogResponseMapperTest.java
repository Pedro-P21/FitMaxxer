package com.pedropacheco.nutrition.backend.foodCatalog.dto;

import com.pedropacheco.nutrition.backend.foodCatalog.domain.FoodItem;
import com.pedropacheco.nutrition.backend.foodCatalog.factory.FoodItemFactory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FoodCatalogResponseMapperTest {
    private final FoodCatalogResponseMapper mapper = new FoodCatalogResponseMapper();
    private final FoodItemFactory factory = new FoodItemFactory();

    @Test
    void mapsBrandAndOtherCatalogFieldsToResponseDto() {
        FoodItem item = factory.createFoodItem("off:12345678", "Oat Drink", "Acme Foods", "12345678",
                BigDecimal.valueOf(45), BigDecimal.valueOf(1.2), BigDecimal.valueOf(6.5),
                BigDecimal.valueOf(1.5),
                com.pedropacheco.nutrition.backend.foodCatalog.domain.NutrientComposition.Basis.PER_100_GRAMS,
                BigDecimal.valueOf(250), "g");

        FoodItemDTO dto = mapper.toResponseDTO(List.of(item)).items().getFirst();

        assertEquals("Acme Foods", dto.brand());
        assertEquals("Oat Drink", dto.name());
        assertEquals("12345678", dto.barcode());
    }

    @Test
    void returnsInformationalMessageForEmptyResults() {
        FoodCatalogResponseDTO response = mapper.toResponseDTO(List.of());

        assertTrue(response.items().isEmpty());
        assertEquals("No food items matched your search.", response.message());
    }
}
