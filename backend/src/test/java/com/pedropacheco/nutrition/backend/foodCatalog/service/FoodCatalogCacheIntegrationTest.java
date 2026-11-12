package com.pedropacheco.nutrition.backend.foodCatalog.service;

import com.pedropacheco.nutrition.backend.foodCatalog.dto.FoodCatalogResponseDTO;
import com.pedropacheco.nutrition.backend.cli.TerminalRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.pedropacheco.nutrition.backend.foodCatalog.domain.FoodItem;
import com.pedropacheco.nutrition.backend.foodCatalog.factory.FoodItemFactory;
import com.pedropacheco.nutrition.backend.foodCatalog.domain.NutrientComposition;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class FoodCatalogCacheIntegrationTest {
    @Autowired
    private FoodCatalogAggregatorService service;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private FoodCatalogSearchService searchService;

    @Autowired
    private FoodCatalogQueryService queryService;

    @MockitoBean
    private FoodCatalogProvider provider;

    @MockitoBean
    private TerminalRunner terminalRunner;

    @BeforeEach
    void clearFoodSearchCache() {
        cacheManager.getCache("foodSearchCache").clear();
        cacheManager.getCache("foodDetailsCache").clear();
        reset(provider);
    }

    @Test
    void repeatedEquivalentQueriesUseCachedResponse() {
        when(provider.search("Oats")).thenReturn(List.of());

        FoodCatalogResponseDTO first = service.search("Oats");
        FoodCatalogResponseDTO second = service.search("  oats ");

        assertSame(first, second);
        assertEquals("No food items matched your search.", second.message());
        verify(provider, times(1)).search("Oats");
    }

    @Test
    void failedProviderCallIsNotCachedAndCanBeRetried() {
        when(provider.search("oats"))
                .thenThrow(new IllegalStateException("temporary upstream failure"))
                .thenReturn(List.of());

        assertThrows(FoodCatalogException.class, () -> service.search("oats"));
        FoodCatalogResponseDTO retry = service.search("oats");

        assertEquals("No food items matched your search.", retry.message());
        verify(provider, times(2)).search("oats");
    }

    private FoodItem oats() {
        return new FoodItemFactory().createFoodItem(
                "openfoodfacts:12345678", "Oats", "Acme", "12345678",
                BigDecimal.TEN, BigDecimal.ONE, BigDecimal.ONE,
                BigDecimal.ONE,
                NutrientComposition.Basis.PER_100_GRAMS,
                BigDecimal.valueOf(100), "g");
    }

    @Test
    void keywordSearchSeedsDetailsWithoutAnotherProviderLookup() {
        when(provider.search("oats")).thenReturn(List.of(oats()));

        searchService.search("oats");
        var details = queryService.getFoodDetails("openfoodfacts:12345678");

        assertEquals("Oats", details.name());
        assertEquals(0, BigDecimal.TEN.compareTo(details.caloriesKcal()));
        verify(provider).search("oats");
        verifyNoMoreInteractions(provider);
    }

    @Test
    void cachedSearchRepopulatesEvictedDetails() {
        when(provider.search("oats")).thenReturn(List.of(oats()));
        searchService.search("oats");
        cacheManager.getCache("foodDetailsCache").clear();

        searchService.search(" OATS ");
        assertEquals("Oats", queryService.getFoodDetails("openfoodfacts:12345678").name());

        verify(provider).search("oats");
        verifyNoMoreInteractions(provider);
    }

    @Test
    void missingDetailsFallBackToCatalogAndCacheSuccessfulResult() {
        when(provider.search("12345678")).thenReturn(List.of(oats()));

        assertEquals("Oats", queryService.getFoodDetails("openfoodfacts:12345678").name());
        cacheManager.getCache("foodSearchCache").clear();
        assertEquals("Oats", queryService.getFoodDetails("openfoodfacts:12345678").name());

        verify(provider).search("12345678");
        verifyNoMoreInteractions(provider);
    }

    @Test
    void unavailableDetailsCanBeRetriedAfterProviderRecovers() {
        when(provider.search("12345678"))
                .thenThrow(new IllegalStateException("offline"))
                .thenReturn(List.of(oats()));

        assertThrows(FoodCatalogException.class, () -> queryService.getFoodDetails("openfoodfacts:12345678"));
        assertEquals("Oats", queryService.getFoodDetails("openfoodfacts:12345678").name());
        verify(provider, times(2)).search("12345678");
    }

    @Test
    void expiredDetailsFallBackToCatalog() {
        when(provider.search("oats")).thenReturn(List.of(oats()));
        when(provider.search("12345678")).thenReturn(List.of(oats()));
        searchService.search("oats");
        var nativeCache = (com.github.benmanes.caffeine.cache.Cache<?, ?>)
                cacheManager.getCache("foodDetailsCache").getNativeCache();
        var expiration = nativeCache.policy().expireAfterWrite().orElseThrow();
        var originalDuration = expiration.getExpiresAfter();
        try {
            expiration.setExpiresAfter(java.time.Duration.ZERO);
            nativeCache.cleanUp();
            assertNull(cacheManager.getCache("foodDetailsCache").get("openfoodfacts:12345678"));
        } finally {
            expiration.setExpiresAfter(originalDuration);
        }

        assertEquals("Oats", queryService.getFoodDetails("openfoodfacts:12345678").name());
        verify(provider).search("oats");
        verify(provider).search("12345678");
        verifyNoMoreInteractions(provider);
    }
}
