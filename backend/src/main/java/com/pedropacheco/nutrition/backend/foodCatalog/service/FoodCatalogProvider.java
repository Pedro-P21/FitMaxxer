package com.pedropacheco.nutrition.backend.foodCatalog.service;

import com.pedropacheco.nutrition.backend.foodCatalog.domain.FoodItem;
import java.util.List;

/** Adapter contract for a food catalog source. */
public interface FoodCatalogProvider {
    List<FoodItem> search(String query);
}
