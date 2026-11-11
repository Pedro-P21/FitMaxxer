package com.pedropacheco.nutrition.backend.dailyLog.dto;

import java.time.LocalDate;
import java.util.List;

public record DailyLogResponseDTO(LocalDate date, List<MealResponseDTO> meals,
                                  DailyNutritionSummaryDTO nutritionSummary) {}
