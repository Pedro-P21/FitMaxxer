package com.pedropacheco.nutrition.backend.cli;

import com.pedropacheco.nutrition.backend.userNutritionalProfile.dto.NutritionalGoalResponseDTO;
import com.pedropacheco.nutrition.backend.userNutritionalProfile.dto.ProfileChangesDTO;
import com.pedropacheco.nutrition.backend.userNutritionalProfile.service.EditUserNutritionalProfileService;
import org.springframework.stereotype.Component;

import java.util.Scanner;

/** Console harness for exercising profile edits directly through the application service. */
@Component
public class UpdateUserNutritionalProfileUI {
    private final EditUserNutritionalProfileService service;

    public UpdateUserNutritionalProfileUI(EditUserNutritionalProfileService service) {
        this.service = service;
    }

    public void show(String userId, Scanner scanner) {
        System.out.println("\n=== FitMaxxer - Edit Nutritional Profile (US002) ===");
        System.out.println("Select the profile sections you want to change. Choose 0 when finished.");

        ProfileChangesDTO changes = new ProfileChangesDTO();
        boolean selecting = true;
        boolean physicalDataSelected = false;
        boolean activityLevelSelected = false;
        boolean nutritionalGoalSelected = false;
        while (selecting) {
            printSections(physicalDataSelected, activityLevelSelected, nutritionalGoalSelected);
            switch (ask(scanner, "Selection: ")) {
                case "1" -> {
                    if (physicalDataSelected) {
                        System.out.println("PhysicalData has already been selected.");
                    } else {
                        editPhysicalData(scanner, changes);
                        physicalDataSelected = hasPhysicalDataChanges(changes);
                    }
                }
                case "2" -> {
                    if (activityLevelSelected) {
                        System.out.println("ActivityLevel has already been selected.");
                    } else {
                        editActivityLevel(scanner, changes);
                        activityLevelSelected = changes.activityLevel != null;
                    }
                }
                case "3" -> {
                    if (nutritionalGoalSelected) {
                        System.out.println("NutritionalGoal has already been selected.");
                    } else {
                        editNutritionalGoal(scanner, changes);
                        nutritionalGoalSelected = changes.calorieTarget != null || changes.macroBreakdown != null;
                    }
                }
                case "0" -> selecting = false;
                default -> System.out.println("Please select 0, 1, 2, or 3.");
            }
        }

        if (!hasChanges(changes)) {
            System.out.println("[INFO] No profile changes were selected.");
            return;
        }

        if (hasProfileInputChanges(changes)) {
            changes.goalType = ask(scanner,
                    "Goal Type (AGGRESSIVE_CUTTING, MODERATE_CUTTING, LIGHT_CUTTING, MAINTAIN_WEIGHT, LIGHT_BULK, MODERATE_BULK, AGGRESSIVE_BULK; Enter for maintain): ");
            if (changes.goalType.isBlank()) changes.goalType = "MAINTAIN_WEIGHT";
        }

        if (!confirm(changes, scanner)) {
            System.out.println("[INFO] Profile edit cancelled.");
            return;
        }

        try {
            NutritionalGoalResponseDTO result = service.editUserProfile(userId, changes);
            printResult(result);
        } catch (IllegalArgumentException e) {
            System.out.println("\n[ERROR] Profile edit failed: " + e.getMessage() + "\n");
        }
    }

    private void printSections(boolean physicalSelected, boolean activitySelected, boolean goalSelected) {
        System.out.println("1. PhysicalData" + (physicalSelected ? " (selected)" : ""));
        System.out.println("2. ActivityLevel" + (activitySelected ? " (selected)" : ""));
        System.out.println("3. NutritionalGoal" + (goalSelected ? " (selected)" : ""));
        System.out.println("0. Finish selection");
    }

    private void editPhysicalData(Scanner scanner, ProfileChangesDTO changes) {
        System.out.println("\nPhysicalData fields (enter y to change a field):");
        if (askYesNo(scanner, "Change age? (y/n): ")) changes.age = readInt(scanner, "Age: ");

        boolean changeWeight = askYesNo(scanner, "Change weight? (y/n): ");
        boolean changeHeight = askYesNo(scanner, "Change height? (y/n): ");
        if (changeWeight || changeHeight) {
            changes.unitSystem = ask(scanner, "Units for changed measurements (METRIC / IMPERIAL): ");
            if (changeWeight) {
                changes.weightKg = readDouble(scanner,
                        "Weight (" + (isImperial(changes.unitSystem) ? "lbs" : "kg") + "): ");
            }
            if (changeHeight) {
                changes.heightCm = readDouble(scanner,
                        "Height (" + (isImperial(changes.unitSystem) ? "in" : "cm") + "): ");
            }
        }

        if (askYesNo(scanner, "Change gender? (y/n): ")) {
            changes.gender = ask(scanner, "Gender (MALE / FEMALE): ");
        }
        if (askYesNo(scanner, "Change body fat percentage? (y/n): ")) {
            changes.bodyFatPercentage = readDouble(scanner, "Body Fat %: ");
        }
    }

    private void editActivityLevel(Scanner scanner, ProfileChangesDTO changes) {
        System.out.println("\nActivity level options: SEDENTARY, LIGHTLY_ACTIVE, MODERATELY_ACTIVE, VERY_ACTIVE, EXTRA_ACTIVE");
        changes.activityLevel = ask(scanner, "New activity level: ");
    }

    private void editNutritionalGoal(Scanner scanner, ProfileChangesDTO changes) {
        System.out.println("\nNutritionalGoal fields:");
        if (askYesNo(scanner, "Change calorie target? (y/n): ")) {
            changes.calorieTarget = readInt(scanner, "Calorie target (kcal): ");
        }
        if (askYesNo(scanner, "Change macro breakdown? (y/n): ")) {
            changes.macroBreakdown = collectMacros(scanner);
        }
    }

    private ProfileChangesDTO.MacroBreakdownDTO collectMacros(Scanner scanner) {
        ProfileChangesDTO.MacroBreakdownDTO macros = new ProfileChangesDTO.MacroBreakdownDTO();
        System.out.println("Enter macro amounts in grams. Their calories must match the target.");
        macros.proteinGrams = readInt(scanner, "Protein (g): ");
        macros.carbsGrams = readInt(scanner, "Carbohydrates (g): ");
        macros.fatsGrams = readInt(scanner, "Fat (g): ");
        return macros;
    }

    private boolean hasChanges(ProfileChangesDTO changes) {
        return hasPhysicalDataChanges(changes)
                || changes.activityLevel != null || changes.calorieTarget != null
                || changes.macroBreakdown != null;
    }

    private boolean hasPhysicalDataChanges(ProfileChangesDTO changes) {
        return changes.age != null || changes.weightKg != null || changes.heightCm != null
                || changes.gender != null || changes.bodyFatPercentage != null;
    }

    private boolean hasProfileInputChanges(ProfileChangesDTO changes) {
        return changes.age != null || changes.weightKg != null || changes.heightCm != null
                || changes.gender != null || changes.bodyFatPercentage != null
                || changes.activityLevel != null;
    }

    private boolean confirm(ProfileChangesDTO changes, Scanner scanner) {
        System.out.println("\nChanges to apply:");
        if (changes.age != null || changes.weightKg != null || changes.heightCm != null
                || changes.gender != null || changes.bodyFatPercentage != null) {
            System.out.println(" - PhysicalData:");
            if (changes.age != null) System.out.println("   age: " + changes.age);
            if (changes.weightKg != null) System.out.println("   weight: " + CliNumberFormatter.whole(changes.weightKg) + " " + weightUnit(changes.unitSystem));
            if (changes.heightCm != null) System.out.println("   height: " + CliNumberFormatter.whole(changes.heightCm) + " " + heightUnit(changes.unitSystem));
            if (changes.gender != null) System.out.println("   gender: " + changes.gender);
            if (changes.bodyFatPercentage != null) System.out.println("   body fat: " + CliNumberFormatter.whole(changes.bodyFatPercentage) + "%");
        }
        if (changes.activityLevel != null) System.out.println(" - ActivityLevel: " + changes.activityLevel);
        if (changes.calorieTarget != null) System.out.println(" - Calorie target: " + changes.calorieTarget + " kcal");
        if (changes.macroBreakdown != null) {
            System.out.printf(" - MacroBreakdown: protein %dg, carbs %dg, fat %dg%n",
                    changes.macroBreakdown.proteinGrams,
                    changes.macroBreakdown.carbsGrams,
                    changes.macroBreakdown.fatsGrams);
        }
        if (changes.goalType != null) System.out.println(" - Goal type for recalculation: " + changes.goalType);
        return askYesNo(scanner, "Confirm and save these changes? (y/n): ");
    }

    private void printResult(NutritionalGoalResponseDTO result) {
        System.out.println("\n--------------------------------------------------");
        System.out.println("SUCCESS: " + result.getMessage());
        System.out.println("--------------------------------------------------");
        System.out.println("Maintenance Calories: " + result.getMaintenanceCalories() + " kcal");
        System.out.println("Calorie Target:        " + result.getCalorieTarget() + " kcal");
        System.out.println("Macronutrients:");
        System.out.println(" - Protein: " + result.getProteinGrams() + "g");
        System.out.println(" - Carbs:   " + result.getCarbsGrams() + "g");
        System.out.println(" - Fats:    " + result.getFatsGrams() + "g");
        System.out.println("--------------------------------------------------\n");
    }

    private boolean isImperial(String unitSystem) {
        return "IMPERIAL".equalsIgnoreCase(unitSystem);
    }

    private String weightUnit(String unitSystem) {
        return isImperial(unitSystem) ? "lbs" : "kg";
    }

    private String heightUnit(String unitSystem) {
        return isImperial(unitSystem) ? "in" : "cm";
    }

    private String ask(Scanner scanner, String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    private boolean askYesNo(Scanner scanner, String prompt) {
        while (true) {
            String answer = ask(scanner, prompt);
            if (answer.equalsIgnoreCase("y") || answer.equalsIgnoreCase("yes")) return true;
            if (answer.equalsIgnoreCase("n") || answer.equalsIgnoreCase("no")) return false;
            System.out.println("Please enter y or n.");
        }
    }

    private int readInt(Scanner scanner, String prompt) {
        while (true) {
            String value = ask(scanner, prompt);
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a whole number.");
            }
        }
    }

    private double readDouble(Scanner scanner, String prompt) {
        while (true) {
            String value = ask(scanner, prompt);
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a number.");
            }
        }
    }
}
