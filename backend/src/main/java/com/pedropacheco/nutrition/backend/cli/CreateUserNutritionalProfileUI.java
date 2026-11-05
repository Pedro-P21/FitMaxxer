package com.pedropacheco.nutrition.backend.cli;

import com.pedropacheco.nutrition.backend.userNutritionalProfile.dto.NutritionalGoalRequestDTO;
import com.pedropacheco.nutrition.backend.userNutritionalProfile.dto.NutritionalGoalResponseDTO;
import com.pedropacheco.nutrition.backend.userNutritionalProfile.service.PersonalDataService;
import org.springframework.stereotype.Component;

import java.util.Scanner;

@Component
public class CreateUserNutritionalProfileUI {

    private final PersonalDataService personalDataService;

    public CreateUserNutritionalProfileUI(PersonalDataService personalDataService) {
        this.personalDataService = personalDataService;
    }

    public void show(String userId) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("\n=== FitMaxxer - Create Nutritional Profile (US001) ===");

        try {
            System.out.print("Unit System (METRIC / IMPERIAL): ");
            String unitSystem = scanner.nextLine();

            System.out.print("Age: ");
            int age = Integer.parseInt(scanner.nextLine());

            System.out.print("Weight (" + ("IMPERIAL".equalsIgnoreCase(unitSystem) ? "lbs" : "kg") + "): ");
            double weight = Double.parseDouble(scanner.nextLine());

            System.out.print("Height (" + ("IMPERIAL".equalsIgnoreCase(unitSystem) ? "in" : "cm") + "): ");
            double height = Double.parseDouble(scanner.nextLine());

            System.out.print("Gender (MALE / FEMALE): ");
            String gender = scanner.nextLine();

            System.out.print("Activity Level (SEDENTARY, LIGHTLY_ACTIVE, MODERATELY_ACTIVE, VERY_ACTIVE, EXTRA_ACTIVE): ");
            String activityLevel = scanner.nextLine();

            System.out.print("Goal Type (AGGRESSIVE_CUTTING, MODERATE_CUTTING, LIGHT_CUTTING, MAINTAIN_WEIGHT, LIGHT_BULK, MODERATE_BULK, AGGRESSIVE_BULK): ");
            String goalType = scanner.nextLine();

            System.out.print("Body Fat % (Optional - press Enter to skip): ");
            String bfInput = scanner.nextLine();
            Double bodyFat = bfInput.isEmpty() ? null : Double.parseDouble(bfInput);

            NutritionalGoalRequestDTO requestDTO = new NutritionalGoalRequestDTO(
                    age, weight, height, gender, activityLevel, goalType, bodyFat, unitSystem
            );

            NutritionalGoalResponseDTO response = personalDataService.logPersonalData(userId, requestDTO);

            System.out.println("\n--------------------------------------------------");
            System.out.println("SUCCESS: " + response.getMessage());
            System.out.println("--------------------------------------------------");
            System.out.println("Maintenance Calories (TDEE): " + response.getMaintenanceCalories() + " kcal");
            System.out.println("Calorie Target:              " + response.getCalorieTarget() + " kcal");
            System.out.println("Macronutrients:");
            System.out.println(" - Protein: " + response.getProteinGrams() + "g");
            System.out.println(" - Carbs:   " + response.getCarbsGrams() + "g");
            System.out.println(" - Fats:    " + response.getFatsGrams() + "g");
            System.out.println("--------------------------------------------------\n");

        } catch (Exception e) {
            System.err.println("\n[ERROR] Failed to log personal data: " + e.getMessage() + "\n");
        }
    }
}