package com.pedropacheco.nutrition.backend.cli;

import com.pedropacheco.nutrition.backend.dailyLog.dto.DailyLogResponseDTO;
import com.pedropacheco.nutrition.backend.dailyLog.dto.LogHistoryResponseDTO;
import com.pedropacheco.nutrition.backend.dailyLog.dto.NutrientValuesDTO;
import com.pedropacheco.nutrition.backend.dailyLog.service.LogHistoryService;
import com.pedropacheco.nutrition.backend.dailyLog.service.DailyLogApplicationService;
import com.pedropacheco.nutrition.backend.dailyLog.dto.CopyMealRequestDTO;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;

import java.util.Scanner;

/** Console workflow for selecting and reviewing one month of past daily logs. */
@Component
public class DailyHistoryUI {
    private final LogHistoryService logHistoryService;

    private final DailyLogApplicationService dailyLogService;

    public DailyHistoryUI(LogHistoryService logHistoryService, DailyLogApplicationService dailyLogService) {
        this.logHistoryService = logHistoryService;
        this.dailyLogService = dailyLogService;
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
            copyFromHistory(userId, scanner, history);
        } catch (RuntimeException exception) {
            System.out.println("[ERROR] Could not load daily history: " + exception.getMessage());
        }
    }

    private record HistoricalMeal(LocalDate date, String type) {}

    private void copyFromHistory(String userId, Scanner scanner, LogHistoryResponseDTO history) {
        List<HistoricalMeal> choices = history.logs().stream().flatMap(log -> log.meals().stream()
                .filter(meal -> !meal.items().isEmpty())
                .map(meal -> new HistoricalMeal(log.date(), meal.type()))).toList();
        if (choices.isEmpty()) return;
        List<String> destinations = List.of("Breakfast", "Lunch", "Dinner", "Snacks");
        while (true) {
            System.out.println("\nCopy a historical meal into today:");
            for (int index = 0; index < choices.size(); index++) {
                var meal = choices.get(index);
                System.out.printf("%d. %s — %s%n", index + 1, meal.date(), meal.type());
            }
            System.out.print("Select meal number (0 to return): ");
            String input = scanner.nextLine().trim();
            if ("0".equals(input)) return;
            try {
                int sourceIndex = Integer.parseInt(input) - 1;
                if (sourceIndex < 0 || sourceIndex >= choices.size()) {
                    System.out.println("[INFO] Select a listed meal number.");
                    continue;
                }
                var source = choices.get(sourceIndex);
                System.out.println("Destination: 1. Breakfast  2. Lunch  3. Dinner  4. Snacks  0. Cancel");
                String destinationInput = scanner.nextLine().trim();
                if ("0".equals(destinationInput)) continue;
                int destinationIndex = Integer.parseInt(destinationInput) - 1;
                if (destinationIndex < 0 || destinationIndex >= destinations.size()) {
                    System.out.println("[INFO] Select a listed destination.");
                    continue;
                }
                var request = new CopyMealRequestDTO(source.date(), source.type(), destinations.get(destinationIndex));
                var updated = dailyLogService.copyMeal(userId, request);
                System.out.println("[SUCCESS] Meal copied into today's " + destinations.get(destinationIndex) + ".");
                printDailyLog(updated);
            } catch (RuntimeException exception) {
                System.out.println("[ERROR] Could not copy meal: " + exception.getMessage());
            }
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
