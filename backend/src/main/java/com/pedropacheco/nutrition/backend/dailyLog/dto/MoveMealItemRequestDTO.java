package com.pedropacheco.nutrition.backend.dailyLog.dto;

public record MoveMealItemRequestDTO(String destinationMealType) {
    public MoveMealItemRequestDTO {
        if (destinationMealType == null || destinationMealType.isBlank())
            throw new IllegalArgumentException("Destination meal type is required.");
    }
}
