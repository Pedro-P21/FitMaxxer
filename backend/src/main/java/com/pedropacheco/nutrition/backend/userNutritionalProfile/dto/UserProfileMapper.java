package com.pedropacheco.nutrition.backend.userNutritionalProfile.dto;

import com.pedropacheco.nutrition.backend.userNutritionalProfile.domain.UserNutritionalProfile;
import org.springframework.stereotype.Component;

@Component
public class UserProfileMapper {

    public UserProfileResponseDTO toProfileResponseDTO(UserNutritionalProfile profile) {
        var physical = profile.getPhysicalData();
        var activity = profile.getActivityLevel();
        var goal = profile.getNutritionalGoal();
        var macros = goal.getMacroBreakdown();
        return new UserProfileResponseDTO(profile.getId(), profile.getUserId().getValue(), physical.getAge(),
                physical.getWeightKg(), physical.getHeightCm(), physical.getGender(), physical.getBodyFatPercentage(),
                activity.getLevel(), activity.getFactor(), goal.getMaintenanceCalories(), goal.getCalorieTarget(),
                macros.getProteinGrams(), macros.getCarbsGrams(), macros.getFatsGrams());
    }


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