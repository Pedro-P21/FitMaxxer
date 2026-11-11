package com.pedropacheco.nutrition.backend.foodCatalog.dto;

import java.math.BigDecimal;

/** Trusted temporary catalog data used to create a daily-log food snapshot. */
public record FoodCatalogDetailsDTO(String foodId, String name, String brand, String barcode,
                                    BigDecimal servingQuantity, String servingUnit,
                                    BigDecimal caloriesKcal, BigDecimal proteinGrams,
                                    BigDecimal carbohydrateGrams, BigDecimal fatGrams,
                                    String nutrientBasis) {}
