package com.pedropacheco.nutrition.backend.foodCatalog.service;

import com.pedropacheco.nutrition.backend.foodCatalog.domain.FoodItem;
import com.pedropacheco.nutrition.backend.foodCatalog.dto.FoodCatalogResponseDTO;
import com.pedropacheco.nutrition.backend.foodCatalog.dto.FoodCatalogResponseMapper;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

@Service
public class FoodCatalogAggregatorService {
    private final List<FoodCatalogProvider> providers;
    private final FoodCatalogResponseMapper responseMapper;

    public FoodCatalogAggregatorService(List<FoodCatalogProvider> providers,
                                        FoodCatalogResponseMapper responseMapper) {
        this.providers = List.copyOf(providers);
        this.responseMapper = responseMapper;
    }

    @Cacheable(value = "foodSearchCache", key = "#query.trim().toLowerCase()")
    public FoodCatalogResponseDTO search(String query) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Search query cannot be blank.");
        }
        LinkedHashMap<String, FoodItem> results = new LinkedHashMap<>();
        RuntimeException lastFailure = null;
        int successfulProviders = 0;

        for (FoodCatalogProvider provider : providers) {
            try {
                for (FoodItem item : provider.search(query.trim())) {
                    results.putIfAbsent(item.getId().getValue(), item);
                }
                successfulProviders++;

            } catch (RuntimeException exception) {
                lastFailure = exception;
            }
        }
        if (successfulProviders == 0 && lastFailure != null) {
            throw new FoodCatalogException("Food catalog is temporarily unavailable.", lastFailure);
        }

        return responseMapper.toResponseDTO(List.copyOf(results.values()));
    }
}
