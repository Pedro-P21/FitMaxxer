package com.pedropacheco.nutrition.backend.userNutritionalProfile.service;

import com.pedropacheco.nutrition.backend.userNutritionalProfile.domain.ActivityLevel;
import com.pedropacheco.nutrition.backend.userNutritionalProfile.domain.NutritionalGoal;
import com.pedropacheco.nutrition.backend.userNutritionalProfile.domain.PhysicalData;
import com.pedropacheco.nutrition.backend.userNutritionalProfile.factory.UserProfileFactory;
import org.springframework.stereotype.Service;

@Service
public class NutritionalGoalCalculatorService {

    private final UserProfileFactory factory;

    public NutritionalGoalCalculatorService(UserProfileFactory factory) {
        this.factory = factory;
    }

    /**
     * Calculates the nutritional goal based on the given physical data, activity level, and goal type.
     * */
    public NutritionalGoal calculateGoal(PhysicalData physicalData, ActivityLevel activityLevel, String goalType) {

        // 1. BMR calculation
        double bmr;
        if ("MALE".equalsIgnoreCase(physicalData.getGender())) {
            bmr = (10 * physicalData.getWeightKg()) + (6.25 * physicalData.getHeightCm()) - (5 * physicalData.getAge()) + 5;
        } else {
            bmr = (10 * physicalData.getWeightKg()) + (6.25 * physicalData.getHeightCm()) - (5 * physicalData.getAge()) - 161;
        }

        // 2. TDEE
        int maintenanceCalories = (int) Math.round(bmr * activityLevel.getFactor());

        // 3. Apply goal-type offset
        int calorieOffset = getOffsetForGoalType(goalType);
        int calorieTarget = maintenanceCalories + calorieOffset;



        return factory.createNutritionalGoal(maintenanceCalories,calorieTarget);
    }

    /**
     * Translates the goal type string into an integer offset.
     */
    private int getOffsetForGoalType(String goalType) {
        if (goalType == null) {
            throw new IllegalArgumentException("Goal type cannot be null.");
        }
        return switch (goalType.toUpperCase()) {
            case "AGGRESSIVE_CUTTING" -> -500;
            case "MODERATE_CUTTING"   -> -300;
            case "LIGHT_CUTTING"      -> -200;
            case "MAINTAIN_WEIGHT"    -> 0;
            case "LIGHT_BULK"         -> 200;
            case "MODERATE_BULK"      -> 300;
            case "AGGRESSIVE_BULK"    -> 500;
            default -> throw new IllegalArgumentException("Invalid goal type: " + goalType);
        };
    }
}