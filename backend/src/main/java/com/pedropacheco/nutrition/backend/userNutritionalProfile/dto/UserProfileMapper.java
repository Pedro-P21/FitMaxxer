package com.pedropacheco.nutrition.backend.userNutritionalProfile.dto;

import com.pedropacheco.nutrition.backend.userNutritionalProfile.domain.UserNutritionalProfile;
import org.springframework.stereotype.Component;

@Component
public class UserProfileMapper {

    public NutritionalGoalResponseDTO toDTO(UserNutritionalProfile profile, String confirmationMessage) {
        var goal = profile.getNutritionalGoal();
        var macros = goal.getMacroBreakdown();

        return new NutritionalGoalResponseDTO(
                confirmationMessage,
                goal.getMaintenanceCalories(),
                goal.getCalorieTarget(),
                macros.getProteinGrams(),
                macros.getCarbsGrams(),
                macros.getFatsGrams()
        );
    }
}