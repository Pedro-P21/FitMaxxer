package com.pedropacheco.nutrition.backend.foodCatalog.dto;

import java.util.List;

/** Unified response used regardless of the upstream catalog source. */
public record FoodCatalogResponseDTO(List<FoodItemDTO> items, String message) {
    public FoodCatalogResponseDTO {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
