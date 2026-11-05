package com.pedropacheco.nutrition.backend.userNutritionalProfile.service;

import com.pedropacheco.nutrition.backend.userAuth.domain.UserId;
import com.pedropacheco.nutrition.backend.userAuth.repository.UserRepository;
import com.pedropacheco.nutrition.backend.userNutritionalProfile.domain.*;
import com.pedropacheco.nutrition.backend.userNutritionalProfile.dto.NutritionalGoalRequestDTO;
import com.pedropacheco.nutrition.backend.userNutritionalProfile.dto.NutritionalGoalResponseDTO;
import com.pedropacheco.nutrition.backend.userNutritionalProfile.factory.UserProfileFactory;
import com.pedropacheco.nutrition.backend.userNutritionalProfile.dto.UserProfileMapper;
import com.pedropacheco.nutrition.backend.userNutritionalProfile.repository.UserProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class PersonalDataService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserProfileFactory userProfileFactory;
    private final NutritionalGoalCalculatorService nutritionalGoalCalculatorService;
    private final UserProfileMapper userProfileMapper;

    public PersonalDataService(
            UserRepository userRepository,
            UserProfileRepository userProfileRepository,
            UserProfileFactory userProfileFactory,
            NutritionalGoalCalculatorService nutritionalGoalCalculatorService,
            UserProfileMapper userProfileMapper
    ) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.userProfileFactory = userProfileFactory;
        this.nutritionalGoalCalculatorService = nutritionalGoalCalculatorService;
        this.userProfileMapper = userProfileMapper;
    }

    @Transactional
    public NutritionalGoalResponseDTO logPersonalData(String userId, NutritionalGoalRequestDTO dto) {

        UserId businessUserId = new UserId(userId);
        boolean userExists = userRepository.existsByUserId(businessUserId);
        if (!userExists) {
            throw new IllegalArgumentException("User with ID " + userId + " does not exist.");
        }


        PhysicalData physicalData = userProfileFactory.createPhysicalData(dto);
        ActivityLevel activityLevel = userProfileFactory.createActivityLevel(dto);

        NutritionalGoal nutritionalGoal = nutritionalGoalCalculatorService.calculateGoal(
                physicalData,
                activityLevel,
                dto.getGoalType()
        );


        Optional<UserNutritionalProfile> existingProfile = userProfileRepository.findByUserId(businessUserId);
        UserNutritionalProfile profile;
        if (existingProfile.isPresent()) {
            profile = existingProfile.get();
            profile.updateData(physicalData, activityLevel, nutritionalGoal);
        } else {
            profile = userProfileFactory.createUserNutritionalProfile(
                    businessUserId,
                    physicalData,
                    activityLevel,
                    nutritionalGoal
            );
        }

        UserNutritionalProfile savedProfile = userProfileRepository.save(profile);

        return userProfileMapper.toDTO(savedProfile, "Personal data logged and nutritional goals calculated successfully.");
    }
}
