package com.pedropacheco.nutrition.backend.cli;

import org.springframework.stereotype.Component;

import java.util.Scanner;

@Component
public class FitMaxxerMainMenu {

    private static final int EXIT_OPTION = 0;
    private static final int VIEW_PROFILE_OPTION = 1;
    private static final int LOG_MEAL_OPTION = 2;
    private static final int VIEW_NUTRITION_PLAN_OPTION = 3;

    public void show(String token) {
        Scanner scanner = new Scanner(System.in);
        boolean sessionActive = true;

        System.out.println("----------------------------------------");
        System.out.println(" FitMaxxer Main Menu");
        System.out.println("----------------------------------------");
        System.out.println("Session Token: " + token);

        while (sessionActive) {
            System.out.println("\nSelect an option:");
            System.out.println(VIEW_PROFILE_OPTION + ". View User Profile");
            System.out.println(LOG_MEAL_OPTION + ". Log Meal");
            System.out.println(VIEW_NUTRITION_PLAN_OPTION + ". View Nutrition Plan");
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