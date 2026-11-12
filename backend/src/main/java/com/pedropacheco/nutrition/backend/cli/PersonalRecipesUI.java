package com.pedropacheco.nutrition.backend.cli;

import com.pedropacheco.nutrition.backend.customRecipe.dto.CustomRecipeResponseDTO;
import com.pedropacheco.nutrition.backend.customRecipe.dto.RecipeNutrientsDTO;
import com.pedropacheco.nutrition.backend.customRecipe.service.CustomRecipeService;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.Scanner;

/** Shared owner-scoped browsing and selection for US003 and US004. */
@Component
public class PersonalRecipesUI {
    private final CustomRecipeService recipes;
    public PersonalRecipesUI(CustomRecipeService recipes) { this.recipes = recipes; }

    public CustomRecipeResponseDTO chooseRecipe(String userId, Scanner scanner) {
        int page = 0;
        while (true) {
            var response = recipes.listRecipes(userId, page, 20);
            if (response.totalElements() == 0) {
                System.out.println("[INFO] No saved recipes.");
                return null;
            }
            System.out.println("\nPersonal recipes — page " + (page + 1) + " of " + response.totalPages());
            for (int i = 0; i < response.items().size(); i++) {
                var recipe = response.items().get(i);
                System.out.println((i + 1) + ". " + recipe.name() + " | " + number(recipe.numberOfServings())
                        + " servings | " + number(recipe.perServingNutrition().caloriesKcal()) + " kcal per serving");
            }
            System.out.print("Select recipe; n = next page, p = previous page, 0 = return: ");
            String choice = scanner.nextLine().trim();
            if ("0".equals(choice)) return null;
            if ("n".equalsIgnoreCase(choice)) {
                if (page + 1 < response.totalPages()) page++;
                continue;
            }
            if ("p".equalsIgnoreCase(choice)) { if (page > 0) page--; continue; }
            int index;
            try { index = Integer.parseInt(choice) - 1; }
            catch (NumberFormatException exception) { continue; }
            if (index < 0 || index >= response.items().size()) continue;
            var recipe = recipes.getRecipe(userId, response.items().get(index).recipeId());
            printDetails(recipe);
            return recipe;
        }
    }
    private void printDetails(CustomRecipeResponseDTO recipe) {
        System.out.println("\n" + recipe.name() + " — " + number(recipe.numberOfServings()) + " servings");
        recipe.ingredients().forEach(row -> System.out.println(" - " + row.foodName() + ": "
                + number(row.quantity()) + " " + row.quantityUnit()));
        printNutrition("Whole recipe", recipe.totalNutrition());
        printNutrition("Per serving", recipe.perServingNutrition());
    }
    private void printNutrition(String label, RecipeNutrientsDTO nutrients) {
        System.out.println(label + ": " + number(nutrients.caloriesKcal()) + " kcal | "
                + number(nutrients.proteinGrams()) + " g protein | " + number(nutrients.carbohydrateGrams())
                + " g carbohydrates | " + number(nutrients.fatGrams()) + " g fat");
    }
    private String number(BigDecimal amount) { return CliNumberFormatter.whole(amount); }
}
