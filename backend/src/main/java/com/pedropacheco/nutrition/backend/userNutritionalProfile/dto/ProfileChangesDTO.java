package com.pedropacheco.nutrition.backend.userNutritionalProfile.dto;

/** Nullable fields represent profile items the user did not select for editing. */
public class ProfileChangesDTO {
    public Integer age;
    public Double weightKg;
    public Double heightCm;
    public String gender;
    public Double bodyFatPercentage;
    public String activityLevel;
    public Integer calorieTarget;
    public MacroBreakdownDTO macroBreakdown;
    public String goalType;
    public String unitSystem;

    public ProfileChangesDTO() {
        this.unitSystem = "METRIC";
    }

    public ProfileChangesDTO(Double weightKg, Integer age, String gender,
                             Double heightCm, Double bodyFatPercentage,
                             Integer calorieTarget, MacroBreakdownDTO macroBreakdown,
                             String goalType, String unitSystem, String activityLevel) {
        this.weightKg = weightKg;
        this.age = age;
        this.gender = gender;
        this.heightCm = heightCm;
        this.bodyFatPercentage = bodyFatPercentage;
        this.calorieTarget = calorieTarget;
        this.macroBreakdown = macroBreakdown;
        this.goalType = goalType;
        this.unitSystem = unitSystem != null ? unitSystem : "METRIC";
        this.activityLevel = activityLevel;
    }

    public static class MacroBreakdownDTO {
        public Integer proteinGrams;
        public Integer carbsGrams;
        public Integer fatsGrams;
    }
}
