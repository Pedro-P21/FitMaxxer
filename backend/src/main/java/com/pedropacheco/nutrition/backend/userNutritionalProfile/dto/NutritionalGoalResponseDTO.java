package com.pedropacheco.nutrition.backend.userNutritionalProfile.dto;

public class NutritionalGoalResponseDTO {
    public final String message;
    public final int maintenanceCalories;
    public final int calorieTarget;
    public final int proteinGrams;
    public final int carbsGrams;
    public final int fatsGrams;

    public NutritionalGoalResponseDTO(String message, int maintenanceCalories, int calorieTarget, int proteinGrams, int carbsGrams, int fatsGrams) {
        this.message = message;
        this.maintenanceCalories = maintenanceCalories;
        this.calorieTarget = calorieTarget;
        this.proteinGrams = proteinGrams;
        this.carbsGrams = carbsGrams;
        this.fatsGrams = fatsGrams;
    }

    public String getMessage() { return message; }
    public int getMaintenanceCalories() { return maintenanceCalories; }
    public int getCalorieTarget() { return calorieTarget; }
    public int getProteinGrams() { return proteinGrams; }
    public int getCarbsGrams() { return carbsGrams; }
    public int getFatsGrams() { return fatsGrams; }
}