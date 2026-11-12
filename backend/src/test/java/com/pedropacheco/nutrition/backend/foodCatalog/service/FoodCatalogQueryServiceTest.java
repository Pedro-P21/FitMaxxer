package com.pedropacheco.nutrition.backend.foodCatalog.service;

import com.pedropacheco.nutrition.backend.foodCatalog.cache.FoodDetailsCache;
import com.pedropacheco.nutrition.backend.foodCatalog.dto.FoodCatalogResponseDTO;
import com.pedropacheco.nutrition.backend.foodCatalog.dto.FoodItemDTO;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FoodCatalogQueryServiceTest {
    private final FoodCatalogSearchService aggregator = mock(FoodCatalogSearchService.class);
    private final FoodDetailsCache cache = new FoodDetailsCache(new org.springframework.cache.concurrent.ConcurrentMapCacheManager("foodDetailsCache"));
    private final FoodCatalogQueryService service = new FoodCatalogQueryService(aggregator, cache);

    @Test
    void resolvesTheExactSelectedCatalogFoodAndReturnsTrustedDetails() {
        FoodItemDTO food = new FoodItemDTO("openfoodfacts:12345678", "Banana", "Fruit", "12345678",
                BigDecimal.ONE, "serving", new BigDecimal("100"), new BigDecimal("1"),
                new BigDecimal("20"), new BigDecimal("1"), "PER_SERVING");
        when(aggregator.search("12345678")).thenReturn(new FoodCatalogResponseDTO(List.of(food), null));

        var result = service.getFoodDetails("openfoodfacts:12345678");

        assertEquals("Banana", result.name());
        assertEquals(new BigDecimal("100"), result.caloriesKcal());
        verify(aggregator).search("12345678");
    }

    @Test
    void rejectsUnknownCatalogKeys() {
        when(aggregator.search("12345678")).thenReturn(new FoodCatalogResponseDTO(List.of(), "No match"));

        assertThrows(FoodNotFoundException.class, () -> service.getFoodDetails("openfoodfacts:12345678"));
    }
}
