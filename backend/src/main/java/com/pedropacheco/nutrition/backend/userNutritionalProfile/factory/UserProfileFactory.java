package com.pedropacheco.nutrition.backend.userNutritionalProfile.factory;

import com.pedropacheco.nutrition.backend.userAuth.domain.UserId;
import com.pedropacheco.nutrition.backend.userNutritionalProfile.domain.*;
import com.pedropacheco.nutrition.backend.userNutritionalProfile.dto.NutritionalGoalRequestDTO;
import com.pedropacheco.nutrition.backend.userNutritionalProfile.dto.ProfileChangesDTO;
import org.springframework.stereotype.Component;

@Component
public class UserProfileFactory {

    public PhysicalData createPhysicalData(NutritionalGoalRequestDTO dto) {
        double weightKg;
        double heightCm;

        if ("IMPERIAL".equalsIgnoreCase(dto.getUnitSystem())) {
            weightKg = dto.getWeight() / 2.2;      // pounds to kg
            heightCm = dto.getHeight() * 2.54;     // feet to cm
        } else {
            weightKg = dto.getWeight();
            heightCm = dto.getHeight();
        }

        return PhysicalData.validateAndCreate(
                dto.getAge(),
                weightKg,
                heightCm,
                dto.getGender(),
                dto.getBodyFatPercentage()
        );
    }

    public ActivityLevel createActivityLevel(NutritionalGoalRequestDTO dto) {
        return ActivityLevel.of(dto.activityLevel);
    }

    public ActivityLevel createActivityLevel(String activityLevel) {
        return ActivityLevel.of(activityLevel);
    }

    public NutritionalGoal createNutritionalGoal(int maintenanceCalories, int calorieTarget) {
        MacroBreakdown macroBreakdown = MacroBreakdown.fromPercentages(calorieTarget,
                35.0,
                40.0,
                25.0);
        return new NutritionalGoal(maintenanceCalories, calorieTarget, macroBreakdown);
    }

    public MacroBreakdown createMacroBreakdown(int calorieTarget) {
        return MacroBreakdown.fromPercentages(calorieTarget, 35.0, 40.0, 25.0);
    }

    /**
     * Builds a user supplied macro breakdown only when its calories align with the target.
     * Protein and carbohydrates provide 4 kcal per gram; fat provides 9 kcal per gram.
     */

    public MacroBreakdown reConfigureMacros(
            ProfileChangesDTO.MacroBreakdownDTO changes,
            int existingCalorieTarget
    ) {
        if (changes == null || changes.proteinGrams == null || changes.carbsGrams == null
                || changes.fatsGrams == null || changes.proteinGrams < 0
                || changes.carbsGrams < 0 || changes.fatsGrams < 0) {
            throw new IllegalArgumentException("Proposed macros are not correctly divided for the calorie target.");
        }

        MacroBreakdown proposed = new MacroBreakdown(
                changes.proteinGrams,
                changes.carbsGrams,
                changes.fatsGrams);

        int caloriesFromMacros = proposed.getProteinGrams() * 4
                + proposed.getCarbsGrams() * 4
                + proposed.getFatsGrams() * 9;

        if (Math.abs(caloriesFromMacros - existingCalorieTarget) > 15) {
            throw new IllegalArgumentException("Proposed macros are not correctly divided for the calorie target.");
        }
        return proposed;
    }

    public UserNutritionalProfile createUserNutritionalProfile(
            UserId userId,
            PhysicalData physicalData,
            ActivityLevel activityLevel,
            NutritionalGoal nutritionalGoal
    ) {
        return new UserNutritionalProfile(userId, physicalData, activityLevel, nutritionalGoal);
    }


    public PhysicalData updatePhysicalData(int age, double weight, double height, String gender,
                                           Double bodyFatPercentage, String unitSystem) {
        double weightKg;
        double heightCm;

        if ("IMPERIAL".equalsIgnoreCase(unitSystem)) {
            weightKg = weight / 2.2;
            heightCm = height * 2.54;
        } else {
            weightKg = weight;
            heightCm = height;
        }

        return PhysicalData.validateAndCreate(
                age,
                weightKg,
                heightCm,
                gender,
                bodyFatPercentage
        );
    }
}
