package com.pedropacheco.nutrition.backend.dailyLog.service;

import com.pedropacheco.nutrition.backend.dailyLog.domain.DailyGoalSnapshot;
import com.pedropacheco.nutrition.backend.dailyLog.domain.DailyLog;
import com.pedropacheco.nutrition.backend.dailyLog.domain.DailyNutritionSummary;
import com.pedropacheco.nutrition.backend.dailyLog.domain.MealType;
import com.pedropacheco.nutrition.backend.dailyLog.domain.MealItem;
import com.pedropacheco.nutrition.backend.dailyLog.dto.DailyLogMapper;
import com.pedropacheco.nutrition.backend.dailyLog.dto.DailyLogResponseDTO;
import com.pedropacheco.nutrition.backend.dailyLog.dto.AddMealItemRequestDTO;
import com.pedropacheco.nutrition.backend.dailyLog.factory.DailyLogFactory;
import com.pedropacheco.nutrition.backend.dailyLog.factory.MealItemFactory;
import com.pedropacheco.nutrition.backend.dailyLog.repository.DailyLogRepository;
import com.pedropacheco.nutrition.backend.foodCatalog.service.FoodCatalogQueryService;
import com.pedropacheco.nutrition.backend.userAuth.domain.UserId;
import com.pedropacheco.nutrition.backend.userNutritionalProfile.repository.UserProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.LocalDate;

@Service
public class DailyLogApplicationService {
    private final DailyLogRepository dailyLogRepository;
    private final UserProfileRepository profileRepository;
    private final DailyLogMapper mapper;
    private final NutrientScalingService nutrientScalingService;
    private final Clock clock;
    private final FoodCatalogQueryService foodCatalogQueryService;
    private final DailyLogFactory dailyLogFactory;
    private final MealItemFactory mealItemFactory;

    public DailyLogApplicationService(DailyLogRepository dailyLogRepository,
                                      UserProfileRepository profileRepository,
                                      DailyLogMapper mapper,
                                      NutrientScalingService nutrientScalingService,
                                      Clock clock,
                                      FoodCatalogQueryService foodCatalogQueryService,
                                      DailyLogFactory dailyLogFactory,
                                      MealItemFactory mealItemFactory) {
        this.dailyLogRepository = dailyLogRepository;
        this.profileRepository = profileRepository;
        this.mapper = mapper;
        this.nutrientScalingService = nutrientScalingService;
        this.clock = clock;
        this.foodCatalogQueryService = foodCatalogQueryService;
        this.dailyLogFactory = dailyLogFactory;
        this.mealItemFactory = mealItemFactory;
    }

    @Transactional(readOnly = true)
    public DailyLogResponseDTO getToday(String authenticatedUserId) {

        UserId userId = new UserId(authenticatedUserId);
        LocalDate today = LocalDate.now(clock);
        var existingLog = dailyLogRepository.findByUserIdAndDate(userId.getValue(), today);

        if (existingLog.isPresent()) {
            DailyLog log = existingLog.get();
            DailyNutritionSummary summary = log.calculateNutritionSummary(nutrientScalingService);
            return mapper.toResponseDTO(log, summary);

        }

        var profile = profileRepository.findByUserId(userId)
                .orElseThrow(NutritionalProfileRequiredException::new);
        DailyGoalSnapshot goalSnapshot = DailyGoalSnapshot.from(profile.getNutritionalGoal());
        return mapper.toEmptyResponseDTO(today, DailyNutritionSummary.empty(goalSnapshot));
    }

    @Transactional
    public DailyLogResponseDTO addMealItem(String authenticatedUserId, AddMealItemRequestDTO request) {

        UserId userId = new UserId(authenticatedUserId);
        LocalDate today = LocalDate.now(clock);
        var foodDetails = foodCatalogQueryService.getFoodDetails(request.foodId());
        DailyLog log = dailyLogRepository.findByUserIdAndDate(userId.getValue(), today).orElseGet(() -> {
            var profile = profileRepository.findByUserId(userId)
                    .orElseThrow(NutritionalProfileRequiredException::new);
            return dailyLogFactory.create(userId, today, profile.getId(),
                    DailyGoalSnapshot.from(profile.getNutritionalGoal()));
        });

        MealItem item = mealItemFactory.create(foodDetails, request.quantity(), request.quantityUnit());
        MealType mealType = new MealType(request.mealType());
        // Validate that the catalog's serving and nutrient basis can be scaled before mutating the aggregate.
        nutrientScalingService.calculateContribution(item.getFoodSnapshot(), item.getPortionQuantity());
        log.addMealItem(mealType, item);

        DailyNutritionSummary summary = log.calculateNutritionSummary(nutrientScalingService);
        DailyLog savedLog = dailyLogRepository.save(log);
        return mapper.toResponseDTO(savedLog, summary);
    }
}
