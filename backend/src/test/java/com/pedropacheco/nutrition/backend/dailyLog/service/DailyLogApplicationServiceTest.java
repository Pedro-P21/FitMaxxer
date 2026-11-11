package com.pedropacheco.nutrition.backend.dailyLog.service;

import com.pedropacheco.nutrition.backend.dailyLog.domain.DailyLog;
import com.pedropacheco.nutrition.backend.dailyLog.dto.AddMealItemRequestDTO;
import com.pedropacheco.nutrition.backend.dailyLog.dto.DailyLogMapper;
import com.pedropacheco.nutrition.backend.dailyLog.factory.DailyLogFactory;
import com.pedropacheco.nutrition.backend.dailyLog.factory.MealItemFactory;
import com.pedropacheco.nutrition.backend.dailyLog.repository.DailyLogRepository;
import com.pedropacheco.nutrition.backend.foodCatalog.dto.FoodCatalogDetailsDTO;
import com.pedropacheco.nutrition.backend.foodCatalog.service.FoodCatalogQueryService;
import com.pedropacheco.nutrition.backend.userAuth.domain.UserId;
import com.pedropacheco.nutrition.backend.userNutritionalProfile.domain.*;
import com.pedropacheco.nutrition.backend.userNutritionalProfile.repository.UserProfileRepository;
import org.junit.jupiter.api.Test;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.math.BigDecimal;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DailyLogApplicationServiceTest {
    private final DailyLogRepository dailyLogRepository = mock(DailyLogRepository.class);
    private final UserProfileRepository profileRepository = mock(UserProfileRepository.class);
    private final FoodCatalogQueryService foodCatalogQueryService = mock(FoodCatalogQueryService.class);
    private final DailyLogApplicationService service = new DailyLogApplicationService(dailyLogRepository,
            profileRepository, new DailyLogMapper(new NutrientScalingService()), new NutrientScalingService(),
            Clock.fixed(Instant.parse("2026-11-10T04:00:00Z"), ZoneOffset.UTC), foodCatalogQueryService,
            new DailyLogFactory(), new MealItemFactory());

    @Test
    void returnsEmptyMealsAndZeroConsumedValuesUsingCurrentProfileTargets() {
        UserNutritionalProfile profile = profile();
        when(dailyLogRepository.findByUserIdAndDate("user-1", LocalDate.of(2026, 11, 10))).thenReturn(Optional.empty());
        when(profileRepository.findByUserId(new UserId("user-1"))).thenReturn(Optional.of(profile));

        var response = service.getToday("user-1");

        assertEquals(LocalDate.of(2026, 11, 10), response.date());
        assertEquals(4, response.meals().size());
        assertEquals("Breakfast", response.meals().getFirst().type());
        assertEquals(0, response.meals().getFirst().items().size());
        assertEquals(0, response.nutritionSummary().consumed().caloriesKcal().signum());
        assertEquals(0, response.meals().getFirst().consumed().caloriesKcal().signum());
        assertEquals(2200, response.nutritionSummary().targets().caloriesKcal().intValue());
        assertEquals(2200, response.nutritionSummary().remaining().caloriesKcal().intValue());
        verify(dailyLogRepository, never()).save(any());
    }

    @Test
    void requiresProfileBeforeReturningAnEmptyDailyLog() {
        when(dailyLogRepository.findByUserIdAndDate("user-1", LocalDate.of(2026, 11, 10))).thenReturn(Optional.empty());
        when(profileRepository.findByUserId(new UserId("user-1"))).thenReturn(Optional.empty());

        assertThrows(NutritionalProfileRequiredException.class, () -> service.getToday("user-1"));
        verify(dailyLogRepository, never()).save(any());
    }

    @Test
    void createsAndPersistsFirstDailyLogWhenFirstFoodIsAdded() {
        UserNutritionalProfile profile = mock(UserNutritionalProfile.class);
        when(profile.getId()).thenReturn(12L);
        when(profile.getNutritionalGoal()).thenReturn(profile().getNutritionalGoal());
        when(dailyLogRepository.findByUserIdAndDate("user-1", LocalDate.of(2026, 11, 10))).thenReturn(Optional.empty());
        when(profileRepository.findByUserId(new UserId("user-1"))).thenReturn(Optional.of(profile));
        when(foodCatalogQueryService.getFoodDetails("openfoodfacts:12345678")).thenReturn(banana());
        when(dailyLogRepository.save(any(DailyLog.class))).thenAnswer(call -> call.getArgument(0));

        var response = service.addMealItem("user-1", new AddMealItemRequestDTO(
                "openfoodfacts:12345678", "Breakfast", new BigDecimal("1.5")));

        assertEquals(150, response.nutritionSummary().consumed().caloriesKcal().intValue());
        assertEquals("Banana", response.meals().getFirst().items().getFirst().foodName());
        assertEquals(new BigDecimal("1.5"), response.meals().getFirst().items().getFirst().quantity());
        assertEquals("SERVING", response.meals().getFirst().items().getFirst().quantityUnit());
        verify(profileRepository).findByUserId(new UserId("user-1"));
        verify(dailyLogRepository).save(any(DailyLog.class));
    }

    @Test
    void addingToExistingLogReusesItsGoalSnapshotWithoutLoadingProfile() {
        DailyLog log = new DailyLog(new UserId("user-1"), LocalDate.of(2026, 11, 10), 12L,
                new com.pedropacheco.nutrition.backend.dailyLog.domain.DailyGoalSnapshot(2000, 120, 220, 70));
        when(dailyLogRepository.findByUserIdAndDate("user-1", LocalDate.of(2026, 11, 10))).thenReturn(Optional.of(log));
        when(foodCatalogQueryService.getFoodDetails("openfoodfacts:12345678")).thenReturn(banana());
        when(dailyLogRepository.save(any(DailyLog.class))).thenAnswer(call -> call.getArgument(0));

        var response = service.addMealItem("user-1", new AddMealItemRequestDTO(
                "openfoodfacts:12345678", "Lunch", BigDecimal.ONE));

        assertEquals(2000, response.nutritionSummary().targets().caloriesKcal().intValue());
        assertEquals(100, response.nutritionSummary().consumed().caloriesKcal().intValue());
        verifyNoInteractions(profileRepository);
    }

    @Test
    void logsAWeightAmountWhenTheCatalogServingUsesAnIncompatibleVolumeUnit() {
        UserNutritionalProfile profile = mock(UserNutritionalProfile.class);
        when(profile.getId()).thenReturn(12L);
        when(profile.getNutritionalGoal()).thenReturn(profile().getNutritionalGoal());
        when(dailyLogRepository.findByUserIdAndDate("user-1", LocalDate.of(2026, 11, 10))).thenReturn(Optional.empty());
        when(profileRepository.findByUserId(new UserId("user-1"))).thenReturn(Optional.of(profile));
        when(foodCatalogQueryService.getFoodDetails("food-1"))
                .thenReturn(new FoodCatalogDetailsDTO("food-1", "Milk", null, null,
                        new BigDecimal("250"), "ml", new BigDecimal("35"), new BigDecimal("3.5"),
                        new BigDecimal("5"), new BigDecimal("0.1"), "PER_100_GRAMS"));
        when(dailyLogRepository.save(any(DailyLog.class))).thenAnswer(call -> call.getArgument(0));

        var response = service.addMealItem("user-1", new AddMealItemRequestDTO(
                "food-1", "Breakfast", new BigDecimal("300"), com.pedropacheco.nutrition.backend.dailyLog.domain.PortionQuantity.Unit.GRAM));

        assertEquals(105, response.nutritionSummary().consumed().caloriesKcal().intValue());
        assertEquals("GRAM", response.meals().getFirst().items().getFirst().quantityUnit());
    }

    @Test
    void doesNotSaveWhenNutrientBasisAndServingUnitAreIncompatible() {
        UserNutritionalProfile profile = mock(UserNutritionalProfile.class);
        when(profile.getId()).thenReturn(12L);
        when(profile.getNutritionalGoal()).thenReturn(profile().getNutritionalGoal());
        when(dailyLogRepository.findByUserIdAndDate("user-1", LocalDate.of(2026, 11, 10))).thenReturn(Optional.empty());
        when(profileRepository.findByUserId(new UserId("user-1"))).thenReturn(Optional.of(profile));
        when(foodCatalogQueryService.getFoodDetails("openfoodfacts:12345678"))
                .thenReturn(new FoodCatalogDetailsDTO("openfoodfacts:12345678", "Banana", null, null,
                        new BigDecimal("100"), "piece", new BigDecimal("100"), new BigDecimal("1"),
                        new BigDecimal("20"), new BigDecimal("1"), "PER_100_GRAMS"));

        assertThrows(IllegalArgumentException.class, () -> service.addMealItem("user-1",
                new AddMealItemRequestDTO("openfoodfacts:12345678", "Breakfast", BigDecimal.ONE)));
        verify(dailyLogRepository, never()).save(any());
    }

    private UserNutritionalProfile profile() {
        MacroBreakdown macros = new MacroBreakdown(150, 220, 80);
        NutritionalGoal goal = new NutritionalGoal(2200, 2200, macros);
        return new UserNutritionalProfile(new UserId("user-1"),
                PhysicalData.validateAndCreate(30, 70, 175, "MALE", null), ActivityLevel.of("SEDENTARY"), goal);
    }

    private FoodCatalogDetailsDTO banana() {
        return new FoodCatalogDetailsDTO("openfoodfacts:12345678", "Banana", "Fruit", "12345678",
                BigDecimal.ONE, "serving", new BigDecimal("100"), new BigDecimal("1"),
                new BigDecimal("20"), new BigDecimal("1"), "PER_SERVING");
    }
}
