package com.pedropacheco.nutrition.backend.dailyLog.dto;

import com.pedropacheco.nutrition.backend.dailyLog.domain.*;
import com.pedropacheco.nutrition.backend.dailyLog.service.NutrientScalingService;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.util.List;

@Component
public class DailyLogMapper {
    private static final List<String> DISPLAY_MEAL_TYPES = List.of("BREAKFAST", "LUNCH", "DINNER", "SNACKS");
    private final NutrientScalingService nutrientScalingService;

    public DailyLogMapper(NutrientScalingService nutrientScalingService) {
        this.nutrientScalingService = nutrientScalingService;
    }

    public DailyLogResponseDTO toResponseDTO(DailyLog dailyLog, DailyNutritionSummary summary) {
        return toResponse(dailyLog.getDate().getValue(), dailyLog.getMeals(), summary);
    }

    public DailyLogResponseDTO toEmptyResponseDTO(LocalDate date, DailyNutritionSummary summary) {
        return new DailyLogResponseDTO(date, DISPLAY_MEAL_TYPES.stream()
                .map(type -> new MealResponseDTO(display(type), List.of(), toValues(NutrientTotals.ZERO))).toList(),
                toSummaryDTO(summary));
    }

    private DailyLogResponseDTO toResponse(LocalDate date, List<Meal> sourceMeals, DailyNutritionSummary summary) {
        List<MealResponseDTO> meals = DISPLAY_MEAL_TYPES.stream()
                .map(type -> {
                    List<Meal> matchingMeals = sourceMeals.stream()
                            .filter(meal -> meal.getMealType().getValue().equals(type)).toList();
                    List<MealItemResponseDTO> items = matchingMeals.stream().flatMap(meal -> meal.getItems().stream())
                            .map(this::toItemDTO).toList();
                    NutrientTotals totals = matchingMeals.stream().map(meal -> meal.calculateNutrientTotals(nutrientScalingService))
                            .reduce(NutrientTotals.ZERO, NutrientTotals::plus);
                    return new MealResponseDTO(display(type), items, toValues(totals));
                })
                .toList();
        return new DailyLogResponseDTO(date, meals, toSummaryDTO(summary));
    }

    private MealItemResponseDTO toItemDTO(MealItem item) {
        FoodSnapshot food = item.getFoodSnapshot();
        return new MealItemResponseDTO(item.getId(), food.getFoodName(), food.getBrand(), food.getBarcode(),
                item.getPortionQuantity().getAmount(), item.getPortionQuantity().getUnit().name(),
                item.getPortionQuantity().getLabel(), food.getServingSizeQuantity(), food.getServingSizeUnit(),
                food.getCaloriesKcal(), food.getProteinGrams(), food.getCarbohydrateGrams(), food.getFatGrams(),
                food.getNutrientBasis());
    }

    private DailyNutritionSummaryDTO toSummaryDTO(DailyNutritionSummary summary) {
        return new DailyNutritionSummaryDTO(toValues(summary.consumed()), toValues(summary.targets()), toValues(summary.remaining()));
    }

    private NutrientValuesDTO toValues(NutrientTotals values) {
        return new NutrientValuesDTO(values.caloriesKcal(), values.proteinGrams(),
                values.carbohydrateGrams(), values.fatGrams());
    }

    private String display(String value) { return value.charAt(0) + value.substring(1).toLowerCase(java.util.Locale.ROOT); }
}
