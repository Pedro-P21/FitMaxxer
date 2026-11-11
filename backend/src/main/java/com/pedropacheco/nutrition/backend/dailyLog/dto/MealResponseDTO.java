package com.pedropacheco.nutrition.backend.dailyLog.dto;

import java.util.List;

public record MealResponseDTO(String type, List<MealItemResponseDTO> items, NutrientValuesDTO consumed) {}
