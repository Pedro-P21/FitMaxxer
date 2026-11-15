package com.pedropacheco.nutrition.backend.dailyLog.service;

import com.pedropacheco.nutrition.backend.dailyLog.Exception.DailyLogWriteConflictException;
import com.pedropacheco.nutrition.backend.dailyLog.Exception.LoggedItemNotFoundException;
import com.pedropacheco.nutrition.backend.dailyLog.Exception.LoggedMealNotFoundException;
import com.pedropacheco.nutrition.backend.customRecipe.service.CustomRecipeService;
import com.pedropacheco.nutrition.backend.dailyLog.domain.*;
import com.pedropacheco.nutrition.backend.dailyLog.dto.*;
import com.pedropacheco.nutrition.backend.dailyLog.factory.DailyLogFactory;
import com.pedropacheco.nutrition.backend.dailyLog.factory.MealItemFactory;
import com.pedropacheco.nutrition.backend.dailyLog.repository.DailyLogRepository;
import com.pedropacheco.nutrition.backend.foodCatalog.service.FoodCatalogQueryService;
import com.pedropacheco.nutrition.backend.userAuth.domain.UserId;
import com.pedropacheco.nutrition.backend.userNutritionalProfile.repository.UserProfileRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Locale;
import java.util.function.Supplier;

@Service
public class DailyLogApplicationService {
    private static final int MAX_CREATION_ATTEMPTS = 3;
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
    private final RecipeFoodSnapshotMapper recipeSnapshotMapper;
    private final TransactionTemplate writeTransaction;

    public DailyLogApplicationService(DailyLogRepository dailyLogRepository,
                                      UserProfileRepository profileRepository,
                                      DailyLogMapper mapper,
                                      NutrientScalingService nutrientScalingService,
                                      Clock clock,
                                      FoodCatalogQueryService foodCatalogQueryService,
                                      DailyLogFactory dailyLogFactory,
                                      MealItemFactory mealItemFactory,
                                      CustomRecipeService recipes,
                                      RecipeFoodSnapshotMapper recipeSnapshotMapper,
                                      @Value("${daily-log.history.max-lookback-years:10}") int maxLookbackYears,
                                      PlatformTransactionManager transactionManager) {
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
        this.writeTransaction = new TransactionTemplate(transactionManager);
    }

    @Transactional(readOnly = true)
    public DailyLogResponseDTO getToday(String authenticatedUserId) {
        UserId userId = new UserId(authenticatedUserId);
        LocalDate today = LocalDate.now(clock);
        var existingLog = dailyLogRepository.findByUserIdAndDate(userId.getValue(), today);
        if (existingLog.isPresent()) {
            DailyLog log = existingLog.get();
            return mapper.toResponseDTO(log, log.calculateNutritionSummary(nutrientScalingService));
        }
        return emptyView(userId, today);
    }

    public DailyLogResponseDTO copyMeal(String authenticatedUserId, CopyMealRequestDTO request) {
        UserId userId = new UserId(authenticatedUserId);
        LocalDate today = LocalDate.now(clock);
        if (!request.sourceDate().isBefore(today))
            throw new IllegalArgumentException("Only meals from previous days can be copied.");
        LogHistoryWindow.validateMonth(YearMonth.from(request.sourceDate()), YearMonth.from(today), maxLookbackYears);
        return executeWrite(true, () -> {
            DailyLog source = dailyLogRepository.findByUserIdAndDate(userId.getValue(), request.sourceDate())
                    .orElseThrow(LoggedMealNotFoundException::new);
            var sourceItems = source.getMealByType(request.sourceMealType()).getItems();
            if (sourceItems.isEmpty()) throw new IllegalArgumentException("Empty meals cannot be copied.");
            var copiedItems = sourceItems.stream().map(mealItemFactory::copy).toList();
            DailyLog destination = loadOrCreateLog(userId, today);
            destination.addMealItems(request.destinationMealType(), copiedItems);
            return saveAndMap(destination);
        });
    }

    public DailyLogResponseDTO changeItemQuantity(String authenticatedUserId, Long itemId,
            ChangeMealItemQuantityRequestDTO request) {
        UserId userId = new UserId(authenticatedUserId);
        LocalDate today = LocalDate.now(clock);
        return executeWrite(false, () -> {
            DailyLog log = requireTodayLog(userId, today);
            mealItemFactory.changeItemQuantity(log.getItem(itemId), request.quantity());
            return saveAndMap(log);
        });
    }

    public DailyLogResponseDTO moveItem(String authenticatedUserId, Long itemId, MoveMealItemRequestDTO request) {
        UserId userId = new UserId(authenticatedUserId);
        LocalDate today = LocalDate.now(clock);
        return executeWrite(false, () -> {
            DailyLog log = requireTodayLog(userId, today);
            log.moveMealItem(itemId, request.destinationMealType());
            return saveAndMap(log);
        });
    }

    public DailyLogResponseDTO deleteItem(String authenticatedUserId, Long itemId) {
        UserId userId = new UserId(authenticatedUserId);
        LocalDate today = LocalDate.now(clock);
        return executeWrite(false, () -> {
            DailyLog log = requireTodayLog(userId, today);
            dailyLogRepository.deleteRemovedItem(log.removeMealItem(itemId));
            if (log.isEmpty()) {
                dailyLogRepository.delete(log);
                return emptyView(userId, today);
            }
            return saveAndMap(log);
        });
    }

    public DailyLogResponseDTO addMealItem(String authenticatedUserId, AddMealItemRequestDTO request) {
        UserId userId = new UserId(authenticatedUserId);
        var foodDetails = foodCatalogQueryService.getFoodDetails(request.foodId());
        LocalDate today = LocalDate.now(clock);
        return executeWrite(true, () -> addResolvedItem(userId, today, request.mealType(),
                mealItemFactory.create(foodDetails, request.quantity(), request.quantityUnit())));
    }

    public DailyLogResponseDTO addRecipeMealItem(String authenticatedUserId, AddRecipeMealItemRequestDTO request) {
        UserId userId = new UserId(authenticatedUserId);
        var recipe = recipes.getRecipe(authenticatedUserId, request.recipeId());
        LocalDate today = LocalDate.now(clock);
        return executeWrite(true, () -> addResolvedItem(userId, today, request.mealType(),
                mealItemFactory.create(recipeSnapshotMapper.toFoodSnapshot(recipe), request.quantity(), PortionQuantity.Unit.SERVING)));
    }

    private DailyLog requireTodayLog(UserId userId, LocalDate today) {
        return dailyLogRepository.findByUserIdAndDateForUpdate(userId.getValue(), today)
                .orElseThrow(LoggedItemNotFoundException::new);
    }

    private DailyLogResponseDTO addResolvedItem(UserId userId, LocalDate today, String requestedMealType, MealItem item) {
        nutrientScalingService.calculateContribution(item.getFoodSnapshot(), item.getPortionQuantity());
        DailyLog log = loadOrCreateLog(userId, today);
        log.addMealItem(new MealType(requestedMealType), item);
        return saveAndMap(log);
    }

    private DailyLog loadOrCreateLog(UserId userId, LocalDate today) {
        return dailyLogRepository.findByUserIdAndDateForUpdate(userId.getValue(), today).orElseGet(() -> {
            var profile = profileRepository.findByUserId(userId).orElseThrow(NutritionalProfileRequiredException::new);
            return dailyLogFactory.create(userId, today, profile.getId(), DailyGoalSnapshot.from(profile.getNutritionalGoal()));
        });
    }

    private DailyLogResponseDTO saveAndMap(DailyLog log) {
        var summary = log.calculateNutritionSummary(nutrientScalingService);
        return mapper.toResponseDTO(dailyLogRepository.save(log), summary);
    }

    private DailyLogResponseDTO emptyView(UserId userId, LocalDate today) {
        var profile = profileRepository.findByUserId(userId).orElseThrow(NutritionalProfileRequiredException::new);
        return mapper.toEmptyResponseDTO(today, DailyNutritionSummary.empty(DailyGoalSnapshot.from(profile.getNutritionalGoal())));
    }

    private DailyLogResponseDTO executeWrite(boolean canCreateLog, Supplier<DailyLogResponseDTO> operation) {
        boolean callerTransaction = TransactionSynchronizationManager.isActualTransactionActive();
        for (int attempt = 1; ; attempt++) {
            try {
                return writeTransaction.execute(status -> operation.get());
            } catch (DataIntegrityViolationException exception) {
                if (!canCreateLog || !isDailyLogCreationConflict(exception)) throw exception;
                if (callerTransaction || attempt >= MAX_CREATION_ATTEMPTS)
                    throw new DailyLogWriteConflictException(exception);

            } catch (PessimisticLockingFailureException | QueryTimeoutException exception) {
                throw new DailyLogWriteConflictException(exception);
            }
        }
    }

    private boolean isDailyLogCreationConflict(DataIntegrityViolationException exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation
                    && "23505".equals(violation.getSQLState()) && violation.getConstraintName() != null
                    && violation.getConstraintName().toLowerCase(Locale.ROOT).contains("uk_daily_log_user_date"))
                return true;
        }
        return false;
    }
}
