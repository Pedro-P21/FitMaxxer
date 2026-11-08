package com.pedropacheco.nutrition.backend.foodCatalog.dto;

import java.math.BigDecimal;

/** Client-facing normalized summary of one catalog item. */
public record FoodItemDTO(
        String foodId,
        String name,
        String brand,
        String barcode,
        BigDecimal servingQuantity,
        String servingUnit,
        BigDecimal caloriesKcal,
        BigDecimal proteinGrams,
        BigDecimal carbohydrateGrams,
        BigDecimal fatGrams,
        String nutrientBasis
) {}
