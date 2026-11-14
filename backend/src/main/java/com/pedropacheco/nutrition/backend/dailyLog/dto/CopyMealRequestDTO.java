package com.pedropacheco.nutrition.backend.dailyLog.dto;

import com.pedropacheco.nutrition.backend.dailyLog.domain.MealType;
import java.time.LocalDate;

public record CopyMealRequestDTO(LocalDate sourceDate, String sourceMealType, String destinationMealType) {
    public CopyMealRequestDTO {
        if (sourceDate == null) throw new IllegalArgumentException("Source date is required.");
        sourceMealType = MealType.normalizeName(sourceMealType);
        destinationMealType = MealType.normalizeName(destinationMealType);
    }
}
