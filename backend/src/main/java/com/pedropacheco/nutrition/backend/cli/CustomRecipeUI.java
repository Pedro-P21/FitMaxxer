package com.pedropacheco.nutrition.backend.cli;

import com.pedropacheco.nutrition.backend.customRecipe.domain.IngredientQuantity;
import org.springframework.stereotype.Component;
import com.pedropacheco.nutrition.backend.customRecipe.dto.CreateRecipeRequestDTO;
import com.pedropacheco.nutrition.backend.customRecipe.dto.RecipeNutrientsDTO;
import com.pedropacheco.nutrition.backend.customRecipe.service.CustomRecipeService;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.Scanner;

/** US006 parts 1 and 2: build a local draft and confirm saving for the signed-in user. */
@Component
public class CustomRecipeUI {
    private final FoodCatalogSearchUI catalogUI;

    private final CustomRecipeService service;

    public CustomRecipeUI(FoodCatalogSearchUI catalogUI, CustomRecipeService service) {
        this.catalogUI = catalogUI;
        this.service = service;
    }

    public void show(String userId, Scanner scanner) {
        var result = buildDraft(scanner);
        if (result.isEmpty()) return;
        var draft = result.get();
        while (true) {
            System.out.print("Save this recipe? (y/n): ");
            String choice = scanner.nextLine().trim();
            if (choice.equalsIgnoreCase("n")) return;
            if (!choice.equalsIgnoreCase("y")) continue;
            try {
                var saved = service.createRecipe(userId, new CreateRecipeRequestDTO(draft.getName().getValue(),
                        draft.getNumberOfServings(), draft.getSelections()));
                System.out.println("[SUCCESS] Recipe saved: " + saved.name());
                printNutrition("Whole recipe", saved.totalNutrition());
                printNutrition("Per serving", saved.perServingNutrition());
                return;
            } catch (RuntimeException exception) {
                System.out.println("[ERROR] Could not save recipe: " + exception.getMessage());
                System.out.println("Draft retained. You can retry saving or return to the menu.");
            }
        }
    }

    private void printNutrition(String label, RecipeNutrientsDTO nutrients) {
        System.out.println(label + ": " + CliNumberFormatter.whole(nutrients.caloriesKcal()) + " kcal, "
                + CliNumberFormatter.whole(nutrients.proteinGrams()) + " g protein, "
                + CliNumberFormatter.whole(nutrients.carbohydrateGrams()) + " g carbohydrates, "
                + CliNumberFormatter.whole(nutrients.fatGrams()) + " g fat");
    }

    public Optional<RecipeDraft> buildDraft(Scanner scanner) {
        System.out.println("\n=== Custom Recipe Builder===");
        System.out.print("Recipe name (0 to cancel): ");
        String name = scanner.nextLine().trim();
        if ("0".equals(name)) return Optional.empty();
        System.out.print("Number of servings (0 to cancel): ");
        String servings = scanner.nextLine().trim();
        if ("0".equals(servings)) return Optional.empty();
        RecipeDraft draft;
        try {
            draft = new RecipeDraft(name, new BigDecimal(servings));
        } catch (IllegalArgumentException exception) {
            System.out.println("[ERROR] " + exception.getMessage());
            return Optional.empty();
        }
        while (true) {
            System.out.println("\n1. Add ingredient  2. Review completed draft  0. Discard and return");
            String choice = scanner.nextLine().trim();
            if ("0".equals(choice)) return Optional.empty();
            if ("2".equals(choice)) {
                if (draft.getIngredients().isEmpty()) {
                    System.out.println("[INFO] Add at least one ingredient.");
                    continue;
                }
                printDraft(draft);
                System.out.println("[INFO] Draft ready for confirmation.");
                return Optional.of(draft);
            }
            if (!"1".equals(choice)) {
                System.out.println("[INFO] Select 1, 2, or 0.");
                continue;
            }
            try {
                var food = catalogUI.chooseFood(scanner);
                if (food == null) continue;
                System.out.print("Quantity unit: 1. Servings  2. Grams  3. Milliliters  0. Cancel: ");
                String unitInput = scanner.nextLine().trim();
                if ("0".equals(unitInput)) continue;
                var unit = switch (unitInput) {
                    case "1" -> IngredientQuantity.Unit.SERVING;
                    case "2" -> IngredientQuantity.Unit.GRAM;
                    case "3" -> IngredientQuantity.Unit.MILLILITER;
                    default -> throw new IllegalArgumentException("Select a listed ingredient unit.");
                };
                System.out.print("Positive quantity (0 to cancel): ");
                String amount = scanner.nextLine().trim();
                if ("0".equals(amount)) continue;
                draft.addIngredient(food, new IngredientQuantity(new BigDecimal(amount), unit));
                printDraft(draft);
            } catch (RuntimeException exception) {
                System.out.println("[ERROR] Could not add ingredient: " + exception.getMessage());
            }
        }
    }
    private void printDraft(RecipeDraft draft) {
        System.out.println("\n" + draft.getName().getValue() + " — " + CliNumberFormatter.whole(draft.getNumberOfServings()) + " servings (unsaved)");
        for (var entry : draft.getIngredients()) {
            var selection = entry.selection();
            System.out.println(" - " + entry.foodName() + ": " + CliNumberFormatter.whole(selection.quantity()) + " " + selection.quantityUnit());
        }
    }
}
