package com.pedropacheco.nutrition.backend.cli;

import org.springframework.stereotype.Component;

import java.util.Scanner;

@Component
public class FitMaxxerMainMenu {

    private static final int EXIT_OPTION = 0;
    private static final int VIEW_PROFILE_OPTION = 1;
    private static final int LOG_MEAL_OPTION = 2;
    private static final int VIEW_NUTRITION_PLAN_OPTION = 3;
    private static final int EDIT_NUTRITIONAL_PROFILE_OPTION = 4;
    private static final int SEARCH_FOOD_OPTION = 5;
    private static final int DAILY_HISTORY_OPTION = 6;
    private static final int RECIPE_DRAFT_OPTION = 7;

    private final UpdateUserNutritionalProfileUI updateUserNutritionalProfileUI;
    private final FoodCatalogSearchUI foodCatalogSearchUI;
    private final DailyLogUI dailyLogUI;
    private final DailyHistoryUI dailyHistoryUI;
    private final CustomRecipeUI customRecipeUI;

    public FitMaxxerMainMenu(UpdateUserNutritionalProfileUI updateUserNutritionalProfileUI,
                             FoodCatalogSearchUI foodCatalogSearchUI,
                             DailyLogUI dailyLogUI,
                             DailyHistoryUI dailyHistoryUI,
                             CustomRecipeUI customRecipeUI) {
        this.updateUserNutritionalProfileUI = updateUserNutritionalProfileUI;
        this.foodCatalogSearchUI = foodCatalogSearchUI;
        this.dailyLogUI = dailyLogUI;
        this.dailyHistoryUI = dailyHistoryUI;
        this.customRecipeUI = customRecipeUI;
    }

    public void show(String userId, Scanner scanner) {
        boolean sessionActive = true;

        System.out.println("----------------------------------------");
        System.out.println(" FitMaxxer Main Menu");
        System.out.println("----------------------------------------");
        System.out.println("Signed in as: " + userId);

        while (sessionActive) {
            System.out.println("\nSelect an option:");
            System.out.println(VIEW_PROFILE_OPTION + ". View User Profile");
            System.out.println(LOG_MEAL_OPTION + ". Log Meal");
            System.out.println(VIEW_NUTRITION_PLAN_OPTION + ". View Nutrition Plan");
            System.out.println(EDIT_NUTRITIONAL_PROFILE_OPTION + ". Edit Nutritional Profile");
            System.out.println(SEARCH_FOOD_OPTION + ". Search Food Catalog");
            System.out.println(DAILY_HISTORY_OPTION + ". Daily History");
            System.out.println(RECIPE_DRAFT_OPTION + ". Build Custom Recipe");
            System.out.println(EXIT_OPTION + ". Logout & Exit");
            System.out.print("Choice: " );

            String choice = scanner.nextLine();

            switch (choice) {
                case "1":
                    System.out.println("[INFO] Profile feature is under development.");
                    break;
                case "2":
                    dailyLogUI.show(userId, scanner);
                    break;
                case "3":
                    System.out.println("[INFO] Nutrition plan feature is under development.");
                    break;
                case "4":
                    updateUserNutritionalProfileUI.show(userId, scanner);
                    break;
                case "5":
                    foodCatalogSearchUI.show(userId, scanner);
                    break;
                case "6":
                    dailyHistoryUI.show(userId, scanner);
                    break;
                case "7":
                    customRecipeUI.show(userId, scanner);
                    break;
                case "0":
                    System.out.println("[INFO] Logging out. Returning to authentication menu...");
                    sessionActive = false;
                    break;
                default:
                    System.out.println("[ERROR] Invalid option. Please select a valid number.");
            }
        }
    }
}
