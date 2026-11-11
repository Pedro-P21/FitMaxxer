package com.pedropacheco.nutrition.backend.cli;

import com.pedropacheco.nutrition.backend.dailyLog.dto.DailyLogResponseDTO;
import com.pedropacheco.nutrition.backend.dailyLog.dto.LogHistoryResponseDTO;
import com.pedropacheco.nutrition.backend.dailyLog.dto.NutrientValuesDTO;
import com.pedropacheco.nutrition.backend.dailyLog.service.LogHistoryService;
import org.springframework.stereotype.Component;

import java.util.Scanner;

/** Console workflow for selecting and reviewing one month of past daily logs. */
@Component
public class DailyHistoryUI {
    private final LogHistoryService logHistoryService;

    public DailyHistoryUI(LogHistoryService logHistoryService) {
        this.logHistoryService = logHistoryService;
    }

    public void show(String userId, Scanner scanner) {
        System.out.print("Month (1-12, 0 to return): ");
        String monthInput = scanner.nextLine().trim();
        if ("0".equals(monthInput)) return;
        System.out.print("Year (for example, 2026): ");
        String yearInput = scanner.nextLine().trim();

        try {
            int month = Integer.parseInt(monthInput);
            int year = Integer.parseInt(yearInput);
            LogHistoryResponseDTO history = logHistoryService.getPastHistory(userId, month, year);
            if (history.logs().isEmpty()) {
                System.out.printf("[INFO] No daily logs are available for %04d-%02d.%n", year, month);
                return;
            }
            history.logs().forEach(this::printDailyLog);
        } catch (RuntimeException exception) {
            System.out.println("[ERROR] Could not load daily history: " + exception.getMessage());
        }
    }

    private void printDailyLog(DailyLogResponseDTO response) {
        System.out.println("\n=== Daily food log: " + response.date() + " ===");
        response.meals().forEach(meal -> {
            System.out.println("\n" + meal.type() + " (" + format(meal.consumed().caloriesKcal()) + " kcal)");
            if (meal.items().isEmpty()) {
                System.out.println("  No foods logged.");
            } else {
                meal.items().forEach(item -> System.out.printf("  - %s: %s %s%n", item.foodName(),
                        format(item.quantity()), item.quantityUnit().toLowerCase(java.util.Locale.ROOT)));
            }
        });

        var summary = response.nutritionSummary();
        System.out.println("\nDaily nutrition (consumed / target / remaining):");
        printNutrient("Calories", "kcal", summary.consumed(), summary.targets(), summary.remaining(), 0);
        printNutrient("Protein", "g", summary.consumed(), summary.targets(), summary.remaining(), 1);
        printNutrient("Carbohydrates", "g", summary.consumed(), summary.targets(), summary.remaining(), 2);
        printNutrient("Fat", "g", summary.consumed(), summary.targets(), summary.remaining(), 3);
    }

    private void printNutrient(String name, String unit, NutrientValuesDTO consumed,
                               NutrientValuesDTO target, NutrientValuesDTO remaining, int index) {
        var consumedValue = value(consumed, index);
        var targetValue = value(target, index);
        var remainingValue = value(remaining, index);
        System.out.printf("  %s: %s / %s / %s %s%n", name, format(consumedValue),
                format(targetValue), format(remainingValue), unit);
    }

    private java.math.BigDecimal value(NutrientValuesDTO values, int index) {
        return switch (index) {
            case 0 -> values.caloriesKcal();
            case 1 -> values.proteinGrams();
            case 2 -> values.carbohydrateGrams();
            case 3 -> values.fatGrams();
            default -> throw new IllegalArgumentException("Unknown nutrient index.");
        };
    }

    private String format(java.math.BigDecimal value) { return CliNumberFormatter.whole(value); }
}
