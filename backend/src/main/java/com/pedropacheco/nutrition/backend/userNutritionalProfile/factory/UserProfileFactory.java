package com.pedropacheco.nutrition.backend.userNutritionalProfile.factory;

import com.pedropacheco.nutrition.backend.userAuth.domain.UserId;
import com.pedropacheco.nutrition.backend.userNutritionalProfile.domain.*;
import com.pedropacheco.nutrition.backend.userNutritionalProfile.dto.NutritionalGoalRequestDTO;
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

    public NutritionalGoal createNutritionalGoal(int maintenanceCalories, int calorieTarget) {
        MacroBreakdown macroBreakdown = MacroBreakdown.fromPercentages(calorieTarget,
                35.0,
                40.0,
                25.0);
        return new NutritionalGoal(maintenanceCalories, calorieTarget, macroBreakdown);
    }

    public UserNutritionalProfile createUserNutritionalProfile(
            UserId userId,
            PhysicalData physicalData,
            ActivityLevel activityLevel,
            NutritionalGoal nutritionalGoal
    ) {
        return new UserNutritionalProfile(userId, physicalData, activityLevel, nutritionalGoal);
    }
}