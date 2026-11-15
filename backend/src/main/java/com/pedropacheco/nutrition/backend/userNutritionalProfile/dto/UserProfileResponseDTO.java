package com.pedropacheco.nutrition.backend.userNutritionalProfile.dto;

/** The complete saved nutritional profile, with measurements in metric units. */
public record UserProfileResponseDTO(Long profileId, String userId, int age,
        double weightKg, double heightCm, String gender, Double bodyFatPercentage,
        String activityLevel, double activityFactor, int maintenanceCalories,
        int calorieTarget, int proteinGrams, int carbsGrams, int fatsGrams) {}
