package com.pedropacheco.nutrition.backend.foodCatalog.service;

import com.pedropacheco.nutrition.backend.foodCatalog.cache.FoodDetailsCache;
import com.pedropacheco.nutrition.backend.foodCatalog.dto.FoodCatalogResponseDTO;
import org.springframework.stereotype.Service;

/** Populates food details even when the aggregator returns a cached search response. */
@Service
public class FoodCatalogSearchService {
    private final FoodCatalogAggregatorService aggregator;
    private final FoodDetailsCache detailsCache;

    public FoodCatalogSearchService(FoodCatalogAggregatorService aggregator, FoodDetailsCache detailsCache) {
        this.aggregator = aggregator;
        this.detailsCache = detailsCache;
    }

    public FoodCatalogResponseDTO search(String query) {
        FoodCatalogResponseDTO response = aggregator.search(query);
        response.items().forEach(detailsCache::put);
        return response;
    }
}
