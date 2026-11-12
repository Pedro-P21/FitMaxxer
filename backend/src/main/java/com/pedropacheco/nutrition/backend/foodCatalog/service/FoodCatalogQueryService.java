package com.pedropacheco.nutrition.backend.foodCatalog.service;

import com.pedropacheco.nutrition.backend.foodCatalog.cache.FoodDetailsCache;
import com.pedropacheco.nutrition.backend.foodCatalog.dto.FoodCatalogDetailsDTO;
import com.pedropacheco.nutrition.backend.foodCatalog.dto.FoodItemDTO;
import org.springframework.stereotype.Service;

@Service
public class FoodCatalogQueryService {
    private static final String OPEN_FOOD_FACTS_PREFIX = "openfoodfacts:";
    private final FoodCatalogSearchService catalogSearch;
    private final FoodDetailsCache detailsCache;

    public FoodCatalogQueryService(FoodCatalogSearchService catalogSearch, FoodDetailsCache detailsCache) {
        this.catalogSearch = catalogSearch;
        this.detailsCache = detailsCache;
    }

    public FoodCatalogDetailsDTO getFoodDetails(String catalogLookupKey) {
        if (catalogLookupKey == null || catalogLookupKey.isBlank()) {
            throw new IllegalArgumentException("Food catalog ID cannot be blank.");
        }
        String key = catalogLookupKey.trim();
        String searchTerm = key.startsWith(OPEN_FOOD_FACTS_PREFIX)
                ? key.substring(OPEN_FOOD_FACTS_PREFIX.length()) : key;

        FoodItemDTO item = detailsCache.get(key).orElseGet(() -> catalogSearch.search(searchTerm).items().stream()
                .filter(candidate -> key.equals(candidate.foodId()))
                .findFirst()
                .orElseThrow(() -> new FoodNotFoundException(key)));

        return new FoodCatalogDetailsDTO(item.foodId(), item.name(), item.brand(), item.barcode(),
                item.servingQuantity(), item.servingUnit(), item.caloriesKcal(), item.proteinGrams(),
                item.carbohydrateGrams(), item.fatGrams(), item.nutrientBasis());
    }
}
