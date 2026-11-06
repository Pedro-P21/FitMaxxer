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

    private final UpdateUserNutritionalProfileUI updateUserNutritionalProfileUI;

    public FitMaxxerMainMenu(UpdateUserNutritionalProfileUI updateUserNutritionalProfileUI) {
        this.updateUserNutritionalProfileUI = updateUserNutritionalProfileUI;
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
            System.out.println(EXIT_OPTION + ". Logout & Exit");
            System.out.print("Choice: " );

            String choice = scanner.nextLine();

            switch (choice) {
                case "1":
                    System.out.println("[INFO] Profile feature is under development.");
                    break;
                case "2":
                    System.out.println("[INFO] Meal logging feature is under development.");
                    break;
                case "3":
                    System.out.println("[INFO] Nutrition plan feature is under development.");
                    break;
                case "4":
                    updateUserNutritionalProfileUI.show(userId, scanner);
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
