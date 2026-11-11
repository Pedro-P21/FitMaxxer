package com.pedropacheco.nutrition.backend.userNutritionalProfile.service;

import com.pedropacheco.nutrition.backend.dailyLog.domain.DailyGoalSnapshot;
import com.pedropacheco.nutrition.backend.dailyLog.repository.DailyLogRepository;
import com.pedropacheco.nutrition.backend.userAuth.domain.UserId;
import com.pedropacheco.nutrition.backend.userNutritionalProfile.domain.*;
import com.pedropacheco.nutrition.backend.userNutritionalProfile.dto.NutritionalGoalResponseDTO;
import com.pedropacheco.nutrition.backend.userNutritionalProfile.dto.ProfileChangesDTO;
import com.pedropacheco.nutrition.backend.userNutritionalProfile.dto.UserProfileMapper;
import com.pedropacheco.nutrition.backend.userNutritionalProfile.factory.UserProfileFactory;
import com.pedropacheco.nutrition.backend.userNutritionalProfile.repository.UserProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;

@Service
public class EditUserNutritionalProfileService {

    private static final String SUCCESS_MESSAGE = "Nutritional profile updated successfully.";
    private static final String DEFAULT_GOAL_TYPE = "MAINTAIN_WEIGHT";

    private final UserProfileRepository profileRepository;
    private final UserProfileFactory profileFactory;
    private final NutritionalGoalCalculatorService goalCalculator;
    private final UserProfileMapper profileMapper;
    private final DailyLogRepository dailyLogRepository;
    private final Clock clock;

    public EditUserNutritionalProfileService(UserProfileRepository profileRepository,
                                             UserProfileFactory profileFactory,
                                             NutritionalGoalCalculatorService goalCalculator,
                                             UserProfileMapper profileMapper,
                                             DailyLogRepository dailyLogRepository,
                                             Clock clock) {
        this.profileRepository = profileRepository;
        this.profileFactory = profileFactory;
        this.goalCalculator = goalCalculator;
        this.profileMapper = profileMapper;
        this.dailyLogRepository = dailyLogRepository;
        this.clock = clock;
    }

    @Transactional
    public NutritionalGoalResponseDTO editUserProfile(String userId, ProfileChangesDTO changes) {
        validateChanges(changes);

        UserId profileOwnerId = new UserId(userId);
        UserNutritionalProfile currentProfile = findProfile(profileOwnerId, userId);


        boolean physicalChanged = hasPhysicalDataChanges(changes);
        boolean activityChanged = changes.activityLevel != null;
        boolean goalChanged = hasNutritionalGoalChanges(changes);
        validateAtLeastOneChange(physicalChanged, activityChanged, goalChanged);


        PhysicalData physicalData = updatePhysicalData(currentProfile.getPhysicalData(), changes, physicalChanged);

        ActivityLevel activityLevel = updateActivityLevel(currentProfile.getActivityLevel(), changes, activityChanged);

        NutritionalGoal goal = updateNutritionalGoal(currentProfile, changes, physicalData,
                activityLevel, physicalChanged || activityChanged);

        UserNutritionalProfile updatedProfile = updateProfile(currentProfile, profileOwnerId,
                physicalData, activityLevel, goal, physicalChanged || activityChanged);

        UserNutritionalProfile savedProfile = profileRepository.save(updatedProfile);
        dailyLogRepository.findByUserIdAndDate(profileOwnerId.getValue(), LocalDate.now(clock))
                .ifPresent(dailyLog -> {
                    dailyLog.updateGoalSnapshot(DailyGoalSnapshot.from(savedProfile.getNutritionalGoal()));
                    dailyLogRepository.save(dailyLog);
                });
        return profileMapper.toDTO(savedProfile, SUCCESS_MESSAGE);
    }

    private void validateChanges(ProfileChangesDTO changes) {
        if (changes == null) {
            throw new IllegalArgumentException("Profile changes must be provided.");
        }
    }

    private UserNutritionalProfile findProfile(UserId userId, String userIdValue) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Nutritional profile not found for user " + userIdValue + "."));
    }

    private boolean hasPhysicalDataChanges(ProfileChangesDTO changes) {
        return changes.age != null || changes.weightKg != null || changes.heightCm != null
                || changes.gender != null || changes.bodyFatPercentage != null;
    }

    private boolean hasNutritionalGoalChanges(ProfileChangesDTO changes) {
        return changes.calorieTarget != null || changes.macroBreakdown != null;
    }

    private void validateAtLeastOneChange(boolean physicalChanged, boolean activityChanged, boolean goalChanged) {
        if (!physicalChanged && !activityChanged && !goalChanged) {
            throw new IllegalArgumentException("At least one profile item must be provided for editing.");
        }
    }

    private PhysicalData updatePhysicalData(PhysicalData oldData, ProfileChangesDTO changes,
                                            boolean physicalChanged) {
        if (!physicalChanged) {
            return oldData;
        }

        int age = changes.age != null ? changes.age : oldData.getAge();
        double weight = changes.weightKg != null ? changes.weightKg : oldData.getWeightKg();
        double height = changes.heightCm != null ? changes.heightCm : oldData.getHeightCm();
        if ("IMPERIAL".equalsIgnoreCase(changes.unitSystem)) {
            // Convert unchanged metric values to the chosen input system before the factory normalizes them.
            if (changes.weightKg == null) weight *= 2.2;
            if (changes.heightCm == null) height /= 2.54;
        }

        String gender = changes.gender != null ? changes.gender : oldData.getGender();
        Double bodyFat = changes.bodyFatPercentage != null
                ? changes.bodyFatPercentage
                : oldData.getBodyFatPercentage();

        return profileFactory.updatePhysicalData(age, weight, height, gender, bodyFat, changes.unitSystem);
    }

    private ActivityLevel updateActivityLevel(ActivityLevel oldActivityLevel, ProfileChangesDTO changes,
                                              boolean activityChanged) {
        return activityChanged ? profileFactory.createActivityLevel(changes.activityLevel) : oldActivityLevel;
    }

    private NutritionalGoal updateNutritionalGoal(UserNutritionalProfile profile, ProfileChangesDTO changes,
                                                  PhysicalData physicalData, ActivityLevel activityLevel,
                                                  boolean profileInputsChanged) {
        NutritionalGoal baseGoal = profileInputsChanged
                ? recalculateGoal(physicalData, activityLevel, changes.goalType)
                : profile.getNutritionalGoal();

        int calorieTarget = changes.calorieTarget != null
                ? changes.calorieTarget
                : baseGoal.getCalorieTarget();


        MacroBreakdown macroBreakdown = updateMacroBreakdown(baseGoal.getMacroBreakdown(), changes.macroBreakdown,
                changes.calorieTarget, baseGoal.getCalorieTarget());

        return new NutritionalGoal(baseGoal.getMaintenanceCalories(), calorieTarget, macroBreakdown);
    }

    private NutritionalGoal recalculateGoal(PhysicalData physicalData, ActivityLevel activityLevel, String goalType) {
        String selectedGoalType = goalType != null ? goalType : DEFAULT_GOAL_TYPE;
        return goalCalculator.calculateGoal(physicalData, activityLevel, selectedGoalType);
    }

    private MacroBreakdown updateMacroBreakdown(MacroBreakdown oldBreakdown,
                                                ProfileChangesDTO.MacroBreakdownDTO changes,
                                                Integer calorieTarget, int existingCalorieTarget) {
        if (changes != null) {
            int targetToValidateAgainst = calorieTarget != null ? calorieTarget : existingCalorieTarget;
            return profileFactory.reConfigureMacros(changes, targetToValidateAgainst);
        }
        if (calorieTarget != null && calorieTarget != existingCalorieTarget) {
            return profileFactory.createMacroBreakdown(calorieTarget);
        }
        return oldBreakdown;
    }

    private UserNutritionalProfile updateProfile(UserNutritionalProfile currentProfile, UserId userId,
                                                 PhysicalData physicalData, ActivityLevel activityLevel,
                                                 NutritionalGoal goal, boolean profileInputsChanged) {
        if (profileInputsChanged) {
            return profileFactory.createUserNutritionalProfile(userId, physicalData, activityLevel, goal);
        }

        currentProfile.replaceNutritionalGoal(goal);
        return currentProfile;
    }
}
