package com.pedropacheco.nutrition.backend.dailyLog.service;

import com.pedropacheco.nutrition.backend.dailyLog.Exception.LoggedItemNotFoundException;
import com.pedropacheco.nutrition.backend.dailyLog.dto.ChangeMealItemQuantityRequestDTO;
import com.pedropacheco.nutrition.backend.dailyLog.dto.MoveMealItemRequestDTO;
import com.pedropacheco.nutrition.backend.customRecipe.service.CustomRecipeService;
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
import java.time.YearMonth;
import org.springframework.beans.factory.annotation.Value;
import com.pedropacheco.nutrition.backend.dailyLog.dto.CopyMealRequestDTO;
import com.pedropacheco.nutrition.backend.dailyLog.Exception.LoggedMealNotFoundException;

@Service
public class DailyLogApplicationService {
    private final DailyLogRepository dailyLogRepository;
    private final UserProfileRepository profileRepository;
    private final DailyLogMapper mapper;
    private final NutrientScalingService nutrientScalingService;
    private final Clock clock;
    private final int maxLookbackYears;
    private final FoodCatalogQueryService foodCatalogQueryService;
    private final DailyLogFactory dailyLogFactory;
    private final MealItemFactory mealItemFactory;
    private final CustomRecipeService recipes;
    private final com.pedropacheco.nutrition.backend.dailyLog.dto.RecipeFoodSnapshotMapper recipeSnapshotMapper;

    public DailyLogApplicationService(DailyLogRepository dailyLogRepository,
                                      UserProfileRepository profileRepository,
                                      DailyLogMapper mapper,
                                      NutrientScalingService nutrientScalingService,
                                      Clock clock,
                                      FoodCatalogQueryService foodCatalogQueryService,
                                      DailyLogFactory dailyLogFactory,
                                      MealItemFactory mealItemFactory,
                                      CustomRecipeService recipes,
                                      com.pedropacheco.nutrition.backend.dailyLog.dto.RecipeFoodSnapshotMapper recipeSnapshotMapper,
                                      @Value("${daily-log.history.max-lookback-years:10}") int maxLookbackYears) {
        if (maxLookbackYears < 1) throw new IllegalArgumentException("History lookback must be at least one year.");
        this.maxLookbackYears = maxLookbackYears;
        this.dailyLogRepository = dailyLogRepository;
        this.profileRepository = profileRepository;
        this.mapper = mapper;
        this.nutrientScalingService = nutrientScalingService;
        this.clock = clock;
        this.foodCatalogQueryService = foodCatalogQueryService;
        this.dailyLogFactory = dailyLogFactory;
        this.mealItemFactory = mealItemFactory;
        this.recipes = recipes;
        this.recipeSnapshotMapper = recipeSnapshotMapper;
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
    public DailyLogResponseDTO copyMeal(String authenticatedUserId, CopyMealRequestDTO request) {
        UserId userId = new UserId(authenticatedUserId);
        LocalDate today = LocalDate.now(clock);
        if (!request.sourceDate().isBefore(today))
            throw new IllegalArgumentException("Only meals from previous days can be copied.");
        LogHistoryWindow.validateMonth(YearMonth.from(request.sourceDate()), YearMonth.from(today), maxLookbackYears);
        DailyLog source = dailyLogRepository.findByUserIdAndDate(userId.getValue(), request.sourceDate())
                .orElseThrow(LoggedMealNotFoundException::new);
        var sourceItems = source.getMealByType(request.sourceMealType()).getItems();
        if (sourceItems.isEmpty()) throw new IllegalArgumentException("Empty meals cannot be copied.");
        var copiedItems = sourceItems.stream().map(mealItemFactory::copy).toList();
        DailyLog destination = loadOrCreateLog(userId, today);
        destination.addMealItems(request.destinationMealType(), copiedItems);
        var summary = destination.calculateNutritionSummary(nutrientScalingService);
        return mapper.toResponseDTO(dailyLogRepository.save(destination), summary);
    }

    @Transactional
    public DailyLogResponseDTO changeItemQuantity(String authenticatedUserId, Long itemId,
            ChangeMealItemQuantityRequestDTO request) {
        UserId userId = new UserId(authenticatedUserId);
        LocalDate today = LocalDate.now(clock);
        DailyLog log = dailyLogRepository.findByUserIdAndDate(userId.getValue(), today)
                .orElseThrow(LoggedItemNotFoundException::new);
        MealItem item = log.getItem(itemId);
        mealItemFactory.changeItemQuantity(item, request.quantity());
        DailyNutritionSummary summary = log.calculateNutritionSummary(nutrientScalingService);
        return mapper.toResponseDTO(dailyLogRepository.save(log), summary);
    }

    @Transactional
    public DailyLogResponseDTO moveItem(String authenticatedUserId, Long itemId, MoveMealItemRequestDTO request) {
        DailyLog log = requireTodayLog(authenticatedUserId);
        log.moveMealItem(itemId, request.destinationMealType());
        var summary = log.calculateNutritionSummary(nutrientScalingService);
        return mapper.toResponseDTO(dailyLogRepository.save(log), summary);
    }

    @Transactional
    public DailyLogResponseDTO deleteItem(String authenticatedUserId, Long itemId) {
        DailyLog log = requireTodayLog(authenticatedUserId);
        MealItem removed = log.removeMealItem(itemId);
        dailyLogRepository.deleteRemovedItem(removed);
        if (log.isEmpty()) {
            dailyLogRepository.delete(log);
            return getToday(authenticatedUserId);
        }
        var summary = log.calculateNutritionSummary(nutrientScalingService);
        return mapper.toResponseDTO(dailyLogRepository.save(log), summary);
    }

    private DailyLog requireTodayLog(String authenticatedUserId) {
        UserId userId = new UserId(authenticatedUserId);
        return dailyLogRepository.findByUserIdAndDate(userId.getValue(), LocalDate.now(clock))
                .orElseThrow(LoggedItemNotFoundException::new);
    }

    @Transactional
    public DailyLogResponseDTO addMealItem(String authenticatedUserId, AddMealItemRequestDTO request) {

        var foodDetails = foodCatalogQueryService.getFoodDetails(request.foodId());
        MealItem item = mealItemFactory.create(foodDetails, request.quantity(), request.quantityUnit());
        return addResolvedItem(authenticatedUserId, request.mealType(), item);
    }

    @Transactional
    public DailyLogResponseDTO addRecipeMealItem(String authenticatedUserId,
            com.pedropacheco.nutrition.backend.dailyLog.dto.AddRecipeMealItemRequestDTO request) {
        var recipe = recipes.getRecipe(authenticatedUserId, request.recipeId());
        var item = mealItemFactory.create(recipeSnapshotMapper.toFoodSnapshot(recipe), request.quantity(),
                com.pedropacheco.nutrition.backend.dailyLog.domain.PortionQuantity.Unit.SERVING);
        return addResolvedItem(authenticatedUserId, request.mealType(), item);
    }

    private DailyLogResponseDTO addResolvedItem(String authenticatedUserId, String requestedMealType, MealItem item) {
        UserId userId = new UserId(authenticatedUserId);
        LocalDate today = LocalDate.now(clock);
        DailyLog log = loadOrCreateLog(userId, today);

        MealType mealType = new MealType(requestedMealType);
        // Validate that the catalog's serving and nutrient basis can be scaled before mutating the aggregate.
        nutrientScalingService.calculateContribution(item.getFoodSnapshot(), item.getPortionQuantity());
        log.addMealItem(mealType, item);

        DailyNutritionSummary summary = log.calculateNutritionSummary(nutrientScalingService);
        DailyLog savedLog = dailyLogRepository.save(log);
        return mapper.toResponseDTO(savedLog, summary);
    }

    private DailyLog loadOrCreateLog(UserId userId, LocalDate today) {
        return dailyLogRepository.findByUserIdAndDate(userId.getValue(), today).orElseGet(() -> {
            var profile = profileRepository.findByUserId(userId)
                    .orElseThrow(NutritionalProfileRequiredException::new);
            return dailyLogFactory.create(userId, today, profile.getId(),
                    DailyGoalSnapshot.from(profile.getNutritionalGoal()));
        });
    }
}
