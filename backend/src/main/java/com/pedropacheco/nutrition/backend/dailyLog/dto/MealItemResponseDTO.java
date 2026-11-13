package com.pedropacheco.nutrition.backend.dailyLog.dto;

import java.math.BigDecimal;
import com.pedropacheco.nutrition.backend.foodCatalog.domain.NutrientComposition;

public record MealItemResponseDTO(Long itemId, String foodName, String brand, String barcode,
                                  BigDecimal quantity, String quantityUnit, String portionLabel,
                                  BigDecimal catalogServingQuantity,
                                  String catalogServingUnit, BigDecimal caloriesKcal,
                                  BigDecimal proteinGrams, BigDecimal carbohydrateGrams,
                                  BigDecimal fatGrams, NutrientComposition.Basis nutrientBasis) {}
