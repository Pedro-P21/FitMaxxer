package com.pedropacheco.nutrition.backend.foodCatalog.service;

import com.pedropacheco.nutrition.backend.foodCatalog.domain.FoodItem;
import com.pedropacheco.nutrition.backend.foodCatalog.dto.FoodCatalogResponseDTO;
import com.pedropacheco.nutrition.backend.foodCatalog.dto.FoodCatalogResponseMapper;
import com.pedropacheco.nutrition.backend.foodCatalog.factory.FoodItemFactory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FoodCatalogAggregatorServiceTest {
    private final FoodCatalogResponseMapper responseMapper = mock(FoodCatalogResponseMapper.class);
    private final FoodItem item = new FoodItemFactory().createFoodItem("off:12345678", "Oats", "Acme",
            "12345678", BigDecimal.TEN, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE,
            com.pedropacheco.nutrition.backend.foodCatalog.domain.NutrientComposition.Basis.PER_100_GRAMS,
            BigDecimal.valueOf(100), "g");

    @Test
    void combinesAndDeduplicatesProviderResults() {
        FoodCatalogProvider first = query -> List.of(item);
        FoodCatalogProvider second = query -> List.of(item);
        FoodCatalogResponseDTO expected = new FoodCatalogResponseDTO(List.of(), null);
        when(responseMapper.toResponseDTO(List.of(item))).thenReturn(expected);
        FoodCatalogAggregatorService service = new FoodCatalogAggregatorService(
                List.of(first, second), responseMapper);

        assertSame(expected, service.search(" oats "));
        verify(responseMapper).toResponseDTO(List.of(item));
    }

    @Test
    void returnsSuccessfulProviderResultsWhenAnotherProviderFails() {
        FoodCatalogProvider failed = query -> { throw new RuntimeException("offline"); };
        FoodCatalogProvider success = query -> List.of(item);
        FoodCatalogResponseDTO expected = new FoodCatalogResponseDTO(List.of(), null);
        when(responseMapper.toResponseDTO(List.of(item))).thenReturn(expected);
        FoodCatalogAggregatorService service = new FoodCatalogAggregatorService(
                List.of(failed, success), responseMapper);

        assertSame(expected, service.search("oats"));
    }

    @Test
    void failsWhenEveryProviderFailsAndRejectsBlankQuery() {
        FoodCatalogProvider failed = query -> { throw new RuntimeException("offline"); };
        FoodCatalogAggregatorService service = new FoodCatalogAggregatorService(List.of(failed), responseMapper);

        assertThrows(FoodCatalogException.class, () -> service.search("oats"));
        assertThrows(IllegalArgumentException.class, () -> service.search("  "));
        verifyNoInteractions(responseMapper);
    }
}
