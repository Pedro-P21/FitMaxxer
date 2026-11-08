package com.pedropacheco.nutrition.backend.foodCatalog.service;

import com.pedropacheco.nutrition.backend.foodCatalog.dto.FoodCatalogResponseDTO;
import com.pedropacheco.nutrition.backend.cli.TerminalRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class FoodCatalogCacheIntegrationTest {
    @Autowired
    private FoodCatalogAggregatorService service;

    @Autowired
    private CacheManager cacheManager;

    @MockitoBean
    private FoodCatalogProvider provider;

    @MockitoBean
    private TerminalRunner terminalRunner;

    @BeforeEach
    void clearFoodSearchCache() {
        cacheManager.getCache("foodSearchCache").clear();
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
}
