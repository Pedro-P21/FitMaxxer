package com.pedropacheco.nutrition.backend.cli;

import com.pedropacheco.nutrition.backend.foodCatalog.dto.FoodCatalogResponseDTO;
import com.pedropacheco.nutrition.backend.foodCatalog.dto.FoodItemDTO;
import com.pedropacheco.nutrition.backend.foodCatalog.service.FoodCatalogAggregatorService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Scanner;

/** Console screen for trying US003 keyword and barcode searches. */
@Component
public class FoodCatalogSearchUI {
    private final FoodCatalogAggregatorService foodCatalogService;

    public FoodCatalogSearchUI(FoodCatalogAggregatorService foodCatalogService) {
        this.foodCatalogService = foodCatalogService;
    }

    public void show(Scanner scanner) {
        System.out.println("\n=== FitMaxxer - Food Catalog Search (US003) ===");
        System.out.println("Searches use Open Food Facts. Search limit: 10 queries per minute.");

        boolean searching = true;
        while (searching) {
            System.out.print("Enter a food name or barcode (0 to return): ");
            String query = scanner.nextLine().trim();
            if ("0".equals(query)) return;
            if (query.isBlank()) {
                System.out.println("[INFO] Enter a food name or barcode.");
                continue;
            }

            try {
                FoodCatalogResponseDTO response = foodCatalogService.search(query);
                List<FoodItemDTO> items = response.items();
                if (items.isEmpty()) {
                    System.out.println("[INFO] " + response.message());
                    continue;
                }

                printResults(items);
                FoodItemDTO selected = selectItem(items, scanner);
                if (selected != null) printDetails(selected);
            } catch (RuntimeException exception) {
                System.out.println("[ERROR] Food search failed: " + exception.getMessage());
            }

            System.out.print("Search again? (y/n): ");
            searching = scanner.nextLine().trim().equalsIgnoreCase("y");
        }
    }

    private void printResults(List<FoodItemDTO> items) {
        System.out.println("\nMatching foods:");
        for (int index = 0; index < items.size(); index++) {
            FoodItemDTO item = items.get(index);
            String brand = item.brand() == null || item.brand().isBlank() ? "" : " — " + item.brand();
            System.out.printf("%d. %s%s | barcode: %s%n", index + 1, item.name(), brand,
                    item.barcode() == null ? "not available" : item.barcode());
        }
    }

    private FoodItemDTO selectItem(List<FoodItemDTO> items, Scanner scanner) {
        while (true) {
            System.out.print("Select a result number for nutrition details (0 to skip): ");
            String input = scanner.nextLine().trim();
            if ("0".equals(input)) return null;
            try {
                int selectedIndex = Integer.parseInt(input) - 1;
                if (selectedIndex >= 0 && selectedIndex < items.size()) return items.get(selectedIndex);
            } catch (NumberFormatException ignored) {
                // Ask again for a numeric result selection.
            }
            System.out.println("[INFO] Enter a number from 1 to " + items.size() + ", or 0 to skip.");
        }
    }

    private void printDetails(FoodItemDTO item) {
        System.out.println("\n--- Food details ---");
        System.out.println("Name: " + item.name());
        System.out.println("Brand: " + valueOrUnavailable(item.brand()));
        System.out.println("Barcode: " + valueOrUnavailable(item.barcode()));
        System.out.println("Serving: " + item.servingQuantity().stripTrailingZeros().toPlainString()
                + " " + item.servingUnit());
        System.out.println("Nutrients (" + item.nutrientBasis() + "):");
        System.out.println(" - Calories: " + item.caloriesKcal() + " kcal");
        System.out.println(" - Protein: " + item.proteinGrams() + " g");
        System.out.println(" - Carbohydrates: " + item.carbohydrateGrams() + " g");
        System.out.println(" - Fat: " + item.fatGrams() + " g");
    }

    private String valueOrUnavailable(String value) {
        return value == null || value.isBlank() ? "not available" : value;
    }
}
