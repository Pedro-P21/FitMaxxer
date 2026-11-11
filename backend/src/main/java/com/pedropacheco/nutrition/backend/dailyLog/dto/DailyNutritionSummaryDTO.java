package com.pedropacheco.nutrition.backend.dailyLog.dto;

public record DailyNutritionSummaryDTO(NutrientValuesDTO consumed, NutrientValuesDTO targets,
                                       NutrientValuesDTO remaining) {}
