package com.pedropacheco.nutrition.backend.foodCatalog.cache;

import com.pedropacheco.nutrition.backend.foodCatalog.dto.FoodItemDTO;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import java.util.Objects;
import java.util.Optional;

/** Process-local catalog definitions, keyed by complete provider-qualified food ID. */
@Component
public class FoodDetailsCache {
    private final Cache cache;

    public FoodDetailsCache(CacheManager cacheManager) {
        this.cache = Objects.requireNonNull(cacheManager.getCache("foodDetailsCache"),
                "foodDetailsCache must be configured.");
    }

    public Optional<FoodItemDTO> get(String foodId) {
        return Optional.ofNullable(cache.get(foodId, FoodItemDTO.class));
    }

    public void put(FoodItemDTO food) {
        cache.put(food.foodId(), food);
    }
}
