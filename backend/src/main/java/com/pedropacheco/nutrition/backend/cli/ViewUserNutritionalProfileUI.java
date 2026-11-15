package com.pedropacheco.nutrition.backend.cli;

import com.pedropacheco.nutrition.backend.userNutritionalProfile.service.UserProfileNotFoundException;
import com.pedropacheco.nutrition.backend.userNutritionalProfile.service.ViewUserNutritionalProfileService;
import org.springframework.stereotype.Component;

@Component
public class ViewUserNutritionalProfileUI {
    private final ViewUserNutritionalProfileService service;

    public ViewUserNutritionalProfileUI(ViewUserNutritionalProfileService service) {
        this.service = service;
    }

    public void show(String userId) {
        try {
            var profile = service.getProfile(userId);
            System.out.println("\n=== Your Nutritional Profile ===");
            System.out.println("User ID: " + profile.userId());
            System.out.println("Age: " + profile.age());
            System.out.println("Gender: " + profile.gender());
            System.out.println("Weight: " + profile.weightKg() + " kg");
            System.out.println("Height: " + profile.heightCm() + " cm");
            System.out.println("Body fat: " + (profile.bodyFatPercentage() == null
                    ? "Not recorded" : profile.bodyFatPercentage() + "%"));
            System.out.println("Activity level: " + profile.activityLevel().replace('_', ' '));
            System.out.println("Maintenance calories: " + profile.maintenanceCalories() + " kcal");
            System.out.println("Calorie target: " + profile.calorieTarget() + " kcal");
            System.out.println("Protein target: " + profile.proteinGrams() + " g");
            System.out.println("Carbohydrate target: " + profile.carbsGrams() + " g");
            System.out.println("Fat target: " + profile.fatsGrams() + " g");
        } catch (UserProfileNotFoundException exception) {
            System.out.println("[INFO] " + exception.getMessage());
        } catch (RuntimeException exception) {
            System.out.println("[ERROR] Could not load your nutritional profile: " + exception.getMessage());
        }
    }
}
