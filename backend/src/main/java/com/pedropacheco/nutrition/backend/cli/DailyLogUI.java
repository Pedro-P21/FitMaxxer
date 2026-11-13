package com.pedropacheco.nutrition.backend.cli;

import com.pedropacheco.nutrition.backend.dailyLog.dto.AddMealItemRequestDTO;
import com.pedropacheco.nutrition.backend.dailyLog.dto.DailyLogResponseDTO;
import com.pedropacheco.nutrition.backend.dailyLog.domain.PortionQuantity;
import com.pedropacheco.nutrition.backend.foodCatalog.dto.FoodItemDTO;
import com.pedropacheco.nutrition.backend.foodCatalog.service.FoodCatalogSearchService;
import com.pedropacheco.nutrition.backend.dailyLog.service.DailyLogApplicationService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Scanner;

/** Console workflow for viewing and updating today's logged intake without HTTP. */
@Component
public class DailyLogUI {
    private final DailyLogApplicationService dailyLogService;
    private final FoodCatalogSearchService foodCatalogService;

    private final PersonalRecipesUI personalRecipesUI;

    public DailyLogUI(DailyLogApplicationService dailyLogService,
                      FoodCatalogSearchService foodCatalogService, PersonalRecipesUI personalRecipesUI) {
        this.dailyLogService = dailyLogService;
        this.foodCatalogService = foodCatalogService;
        this.personalRecipesUI = personalRecipesUI;
    }

    public void show(String userId, Scanner scanner) {
        while (true) {
            DailyLogResponseDTO response;
            try {
                response = dailyLogService.getToday(userId);
                printDailyLog(response);
            } catch (RuntimeException exception) {
                System.out.println("[ERROR] Could not load today's food log: " + exception.getMessage());
                return;
            }

            System.out.println("\n1. Add food");
            System.out.println("2. Change a logged quantity");
            System.out.println("0. Return to main menu");
            System.out.print("Choice: ");
            String choice = scanner.nextLine().trim();
            if ("0".equals(choice)) return;
            if ("2".equals(choice)) {
                changeLoggedQuantity(userId, scanner, response);
                continue;
            }
            if (!"1".equals(choice)) {
                System.out.println("[INFO] Select 1 to add food, 2 to change a quantity, or 0 to return.");
                continue;
            }

            System.out.println("Source: 1. Open food catalog  2. Personal recipes  0. Cancel");
            String source = scanner.nextLine().trim();
            if ("0".equals(source)) continue;
            if ("2".equals(source)) {
                logRecipe(userId, scanner);
                continue;
            }
            if (!"1".equals(source)) {
                System.out.println("[INFO] Select 1, 2, or 0.");
                continue;
            }
            FoodItemDTO selectedFood = chooseFood(scanner);
            if (selectedFood == null) continue;
            String mealType = chooseMealType(scanner);
            if (mealType == null) continue;
            PortionInput portion = readQuantity(scanner, selectedFood);
            if (portion == null) continue;

            try {
                response = dailyLogService.addMealItem(userId,
                        new AddMealItemRequestDTO(selectedFood.foodId(), mealType, portion.amount(), portion.unit()));
                System.out.println("[SUCCESS] Food added to " + mealType + ".");
                printDailyLog(response);
            } catch (RuntimeException exception) {
                System.out.println("[ERROR] Could not add food: " + exception.getMessage());
            }
        }
    }

    private void changeLoggedQuantity(String userId, Scanner scanner, DailyLogResponseDTO log) {
        var items = log.meals().stream().flatMap(meal -> meal.items().stream()).toList();
        if (items.isEmpty()) {
            System.out.println("[INFO] No items logged today.");
            return;
        }
        for (int index = 0; index < items.size(); index++) {
            var item = items.get(index);
            System.out.printf("%d. %s: %s %s%n", index + 1, item.foodName(),
                    item.quantity().stripTrailingZeros().toPlainString(), item.quantityUnit());
        }
        try {
            System.out.print("Select item number (0 to cancel): ");
            String selection = scanner.nextLine().trim();
            if ("0".equals(selection)) return;
            int index = Integer.parseInt(selection) - 1;
            if (index < 0 || index >= items.size()) {
                System.out.println("[INFO] Select a listed item number.");
                return;
            }
            var item = items.get(index);
            System.out.printf("New quantity in %s (0 to cancel): ", item.quantityUnit());
            String amount = scanner.nextLine().trim();
            if ("0".equals(amount)) return;
            var request = new com.pedropacheco.nutrition.backend.dailyLog.dto.ChangeMealItemQuantityRequestDTO(
                    new BigDecimal(amount));
            var updated = dailyLogService.changeItemQuantity(userId, item.itemId(), request);
            System.out.println("[SUCCESS] Logged quantity updated.");
            printDailyLog(updated);
        } catch (RuntimeException exception) {
            System.out.println("[ERROR] Could not change quantity: " + exception.getMessage());
        }
    }

    private void logRecipe(String userId, Scanner scanner) {
        try {
            var recipe = personalRecipesUI.chooseRecipe(userId, scanner);
            if (recipe == null) return;
            String mealType = chooseMealType(scanner);
            if (mealType == null) return;
            System.out.print("Number of recipe servings (fractions allowed; 0 to cancel): ");
            String amount = scanner.nextLine().trim();
            if ("0".equals(amount)) return;
            var request = new com.pedropacheco.nutrition.backend.dailyLog.dto.AddRecipeMealItemRequestDTO(
                    recipe.recipeId(), mealType, new BigDecimal(amount));
            var response = dailyLogService.addRecipeMealItem(userId, request);
            System.out.println("[SUCCESS] Recipe added to " + mealType + ".");
            printDailyLog(response);
        } catch (RuntimeException exception) {
            System.out.println("[ERROR] Could not log recipe: " + exception.getMessage());
        }
    }

    private FoodItemDTO chooseFood(Scanner scanner) {
        System.out.print("Search food by name or barcode (0 to cancel): ");
        String query = scanner.nextLine().trim();
        if ("0".equals(query)) return null;
        if (query.isBlank()) {
            System.out.println("[INFO] Enter a food name or barcode.");
            return null;
        }

        List<FoodItemDTO> items;
        try {
            items = foodCatalogService.search(query).items();
        } catch (RuntimeException exception) {
            System.out.println("[ERROR] Food search failed: " + exception.getMessage());
            return null;
        }
        if (items.isEmpty()) {
            System.out.println("[INFO] No matching foods found.");
            return null;
        }

        System.out.println("\nMatching foods:");
        for (int index = 0; index < items.size(); index++) {
            FoodItemDTO food = items.get(index);
            System.out.printf("%d. %s%s | %s %s | %s kcal (%s)%n", index + 1, food.name(),
                    food.brand() == null || food.brand().isBlank() ? "" : " — " + food.brand(),
                    format(food.servingQuantity()), food.servingUnit(),
                    format(food.caloriesKcal()), food.nutrientBasis());
        }

        while (true) {
            System.out.print("Select food number (0 to cancel): ");
            String input = scanner.nextLine().trim();
            if ("0".equals(input)) return null;
            try {
                int selected = Integer.parseInt(input) - 1;
                if (selected >= 0 && selected < items.size()) {
                    FoodItemDTO food = items.get(selected);
                    printSelectedFood(food);
                    return food;
                }
            } catch (NumberFormatException ignored) {
                // Keep prompting until a valid result number or cancellation is entered.
            }
            System.out.println("[INFO] Enter a number from 1 to " + items.size() + ", or 0 to cancel.");
        }
    }

    private String chooseMealType(Scanner scanner) {
        while (true) {
            System.out.println("Meal: 1. Breakfast  2. Lunch  3. Dinner  4. Snacks  0. Cancel");
            System.out.print("Choice: ");
            switch (scanner.nextLine().trim()) {
                case "1": return "Breakfast";
                case "2": return "Lunch";
                case "3": return "Dinner";
                case "4": return "Snacks";
                case "0": return null;
                default: System.out.println("[INFO] Select a meal from 1 to 4, or 0 to cancel.");
            }
        }
    }

    private void printSelectedFood(FoodItemDTO food) {
        System.out.println("\n--- Selected food ---");
        System.out.println("Name: " + food.name());
        if (food.brand() != null && !food.brand().isBlank()) System.out.println("Brand: " + food.brand());
        if (food.barcode() != null && !food.barcode().isBlank()) System.out.println("Barcode: " + food.barcode());
        System.out.println("Catalog serving: " + format(food.servingQuantity()) + " " + food.servingUnit());
        System.out.println("Nutrient basis: " + describeBasis(food.nutrientBasis()));
        System.out.println("Nutrition per " + describeBasis(food.nutrientBasis()) + ":");
        System.out.println("  Calories: " + format(food.caloriesKcal()) + " kcal");
        System.out.println("  Protein: " + format(food.proteinGrams()) + " g");
        System.out.println("  Carbohydrates: " + format(food.carbohydrateGrams()) + " g");
        System.out.println("  Fat: " + format(food.fatGrams()) + " g");
        if (food.nutrientBasis().equals("PER_100_GRAMS") && isMilliliterUnit(food.servingUnit())
                || food.nutrientBasis().equals("PER_100_MILLILITERS") && isGramUnit(food.servingUnit())) {
            System.out.println("[INFO] This serving unit does not match the nutrient basis, so its nutrition cannot be scaled reliably.");
        }
    }

    private String describeBasis(String basis) {
        return switch (basis) {
            case "PER_100_GRAMS" -> "100 g";
            case "PER_100_MILLILITERS" -> "100 ml";
            case "PER_SERVING" -> "one serving";
            default -> basis;
        };
    }

    private boolean isMilliliterUnit(String unit) {
        return List.of("ml", "milliliter", "milliliters", "millilitre", "millilitres",
                        "l", "liter", "liters", "litre", "litres", "cl", "dl")
                .contains(unit.trim().toLowerCase(java.util.Locale.ROOT));
    }

    private boolean isGramUnit(String unit) {
        return List.of("g", "gram", "grams", "kg", "mg", "milligram", "milligrams")
                .contains(unit.trim().toLowerCase(java.util.Locale.ROOT));
    }

    private PortionInput readQuantity(Scanner scanner, FoodItemDTO food) {
        List<PortionQuantity.Unit> options = quantityOptionsFor(food);
        PortionQuantity.Unit unit;
        if (options.size() > 1) {
            System.out.println("How do you want to enter the portion?");
            for (int index = 0; index < options.size(); index++) {
                PortionQuantity.Unit option = options.get(index);
                System.out.printf("%d. %s%n", index + 1, option == PortionQuantity.Unit.SERVING
                        ? "Catalog serving multiplier" : option == PortionQuantity.Unit.GRAM
                        ? "Amount in grams" : "Amount in milliliters");
            }
            System.out.println("0. Cancel");
            System.out.print("Choice: ");
            String choice = scanner.nextLine().trim();
            if ("0".equals(choice)) return null;
            try {
                int selected = Integer.parseInt(choice) - 1;
                if (selected < 0 || selected >= options.size()) throw new NumberFormatException();
                unit = options.get(selected);
            } catch (NumberFormatException exception) {
                System.out.println("[INFO] Choose one of the listed portion input options.");
                return null;
            }
        } else {
            unit = options.get(0);
        }

        if (unit == PortionQuantity.Unit.SERVING) {
            BigDecimal halfServing = food.servingQuantity().divide(BigDecimal.valueOf(2), 4, java.math.RoundingMode.HALF_UP);
            System.out.printf("Number of catalog servings (1 = %s %s, half serving = %s %s; 0 to cancel): ",
                    format(food.servingQuantity()), food.servingUnit(), format(halfServing), food.servingUnit());
        } else {
            System.out.printf("Nutrients are %s; the catalog serving is %s %s. Enter an amount in %s (0 to cancel): ",
                    describeBasis(food.nutrientBasis()), format(food.servingQuantity()), food.servingUnit(),
                    unit == PortionQuantity.Unit.GRAM ? "grams" : "milliliters");
        }
        String input = scanner.nextLine().trim();
        if ("0".equals(input)) return null;
        try {
            BigDecimal quantity = new BigDecimal(input);
            if (quantity.signum() > 0) return new PortionInput(quantity, unit);
        } catch (NumberFormatException ignored) {
            // Report invalid input below.
        }
        System.out.println("[INFO] Enter a positive amount in the selected unit.");
        return null;
    }

    private void printDailyLog(DailyLogResponseDTO response) {
        System.out.println("\n=== Today's food log: " + response.date() + " ===");
        response.meals().forEach(meal -> {
            System.out.println("\n" + meal.type() + " (" + format(meal.consumed().caloriesKcal()) + " kcal)");
            if (meal.items().isEmpty()) {
                System.out.println("  No foods logged.");
            } else {
                meal.items().forEach(item -> System.out.printf("  - %s: %s %s%n", item.foodName(),
                        format(item.quantity()),
                        item.quantityUnit().toLowerCase(java.util.Locale.ROOT)));
            }
        });

        var summary = response.nutritionSummary();
        System.out.println("\nDaily nutrition (consumed / target / remaining):");
        printNutrient("Calories", "kcal", summary.consumed().caloriesKcal(), summary.targets().caloriesKcal(), summary.remaining().caloriesKcal());
        printNutrient("Protein", "g", summary.consumed().proteinGrams(), summary.targets().proteinGrams(), summary.remaining().proteinGrams());
        printNutrient("Carbohydrates", "g", summary.consumed().carbohydrateGrams(), summary.targets().carbohydrateGrams(), summary.remaining().carbohydrateGrams());
        printNutrient("Fat", "g", summary.consumed().fatGrams(), summary.targets().fatGrams(), summary.remaining().fatGrams());
    }

    private void printNutrient(String name, String unit, BigDecimal consumed, BigDecimal target, BigDecimal remaining) {
        System.out.printf("  %s: %s / %s / %s %s%n", name, format(consumed), format(target), format(remaining), unit);
    }

    private String format(BigDecimal value) { return CliNumberFormatter.whole(value); }

    private List<PortionQuantity.Unit> quantityOptionsFor(FoodItemDTO food) {
        PortionQuantity.Unit measuredUnit = switch (food.nutrientBasis()) {
            case "PER_100_GRAMS" -> PortionQuantity.Unit.GRAM;
            case "PER_100_MILLILITERS" -> PortionQuantity.Unit.MILLILITER;
            case "PER_SERVING" -> isGramUnit(food.servingUnit()) ? PortionQuantity.Unit.GRAM
                    : isMilliliterUnit(food.servingUnit()) ? PortionQuantity.Unit.MILLILITER : null;
            default -> throw new IllegalArgumentException("Unsupported nutrient basis: " + food.nutrientBasis());
        };
        boolean servingCompatible = food.nutrientBasis().equals("PER_SERVING")
                || food.nutrientBasis().equals("PER_100_GRAMS") && isGramUnit(food.servingUnit())
                || food.nutrientBasis().equals("PER_100_MILLILITERS") && isMilliliterUnit(food.servingUnit());
        if (servingCompatible && measuredUnit != null) return List.of(PortionQuantity.Unit.SERVING, measuredUnit);
        if (servingCompatible) return List.of(PortionQuantity.Unit.SERVING);
        if (measuredUnit != null) return List.of(measuredUnit);
        throw new IllegalArgumentException("No compatible portion unit is available for this food.");
    }

    private record PortionInput(BigDecimal amount, PortionQuantity.Unit unit) {}
}
