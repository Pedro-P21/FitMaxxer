package com.pedropacheco.nutrition.backend.userNutritionalProfile.dto;

public class NutritionalGoalRequestDTO {
    public final int age;
    public final double weight;
    public final double height;
    public final String gender;
    public final String activityLevel;
    public final String goalType;
    public final Double bodyFatPercentage;
    public final String unitSystem; // "METRIC" or "IMPERIAL"


    public NutritionalGoalRequestDTO(int age, double weight, double height, String gender,
                                     String activityLevel, String goalType, Double bodyFatPercentage, String unitSystem) {
        this.age = age;
        this.weight = weight;
        this.height = height;
        this.gender = gender;
        this.activityLevel = activityLevel;
        this.goalType = goalType;
        this.bodyFatPercentage = bodyFatPercentage;
        this.unitSystem = unitSystem != null ? unitSystem : "METRIC";
    }

    public int getAge() {
        return age;
    }

    public double getWeight() {
        return weight;
    }

    public double getHeight() {
        return height;
    }

    public String getGender() {
        return gender;
    }

    public String getActivityLevel() {
        return activityLevel;
    }

    public String getGoalType() {
        return goalType;
    }

    public Double getBodyFatPercentage() {
        return bodyFatPercentage;
    }

    public String getUnitSystem() {
        return unitSystem;
    }
}