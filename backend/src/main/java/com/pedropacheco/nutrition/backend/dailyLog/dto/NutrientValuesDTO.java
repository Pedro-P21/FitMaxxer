package com.pedropacheco.nutrition.backend.dailyLog.dto;

import java.math.BigDecimal;

public record NutrientValuesDTO(BigDecimal caloriesKcal, BigDecimal proteinGrams,
                                BigDecimal carbohydrateGrams, BigDecimal fatGrams) {}
