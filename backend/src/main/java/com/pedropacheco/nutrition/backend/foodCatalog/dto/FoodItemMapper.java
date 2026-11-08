package com.pedropacheco.nutrition.backend.foodCatalog.dto;

import com.pedropacheco.nutrition.backend.foodCatalog.domain.FoodItem;
import com.pedropacheco.nutrition.backend.foodCatalog.domain.NutrientComposition;
import com.pedropacheco.nutrition.backend.foodCatalog.factory.FoodItemFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Converts Open Food Facts provider DTOs into validated catalog domain entities. */
@Component
public class FoodItemMapper {

    private static final Pattern SERVING_SIZE_PATTERN =
            Pattern.compile("^\\s*([0-9]+(?:[.,][0-9]+)?)\\s*([\\p{L}]+)\\s*$");

    private final FoodItemFactory foodItemFactory;

    public FoodItemMapper(FoodItemFactory foodItemFactory) {
        this.foodItemFactory = foodItemFactory;
    }

    public List<FoodItem> toDomainItems(OpenFoodFactsSearchResponseDTO response) {
        if (response == null || response.getProducts() == null) {
            return List.of();
        }
        List<FoodItem> items = new ArrayList<>();
        for (OpenFoodFactsProductDTO product : response.getProducts()) {
            toDomainItem(product).ifPresent(items::add);
        }
        return List.copyOf(items);
    }

    /** Invalid or materially incomplete provider products are omitted from search results. */
    public Optional<FoodItem> toDomainItem(OpenFoodFactsProductDTO product) {
        if (product == null || product.getNutriments() == null) {
            return Optional.empty();
        }
        try {
            var serving = servingSize(product);
            var nutrients = nutrientValues(product);
            String code = required(product.getCode());
            String name = required(product.getProductName());
            return Optional.of(foodItemFactory.createFoodItem(
                    "openfoodfacts:" + code,
                    name,
                    product.getBrands(),
                    product.getCode(),
                    nutrients.calories(), nutrients.protein(), nutrients.carbohydrates(), nutrients.fat(),
                    nutrients.basis(), serving.quantity(), serving.unit()
            ));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private NutrientValues nutrientValues(OpenFoodFactsProductDTO product) {
        OpenFoodFactsNutrimentsDTO source = product.getNutriments();

        boolean perServing = "serving".equalsIgnoreCase(product.getNutritionDataPer());

        String calories = perServing ? source.getEnergyKcalPerServing() : source.getEnergyKcalPer100g();
        String protein = perServing ? source.getProteinsPerServing() : source.getProteinsPer100g();
        String carbs = perServing ? source.getCarbohydratesPerServing() : source.getCarbohydratesPer100g();
        String fat = perServing ? source.getFatPerServing() : source.getFatPer100g();

        NutrientComposition.Basis basis = perServing
                ? NutrientComposition.Basis.PER_SERVING
                : NutrientComposition.Basis.PER_100_GRAMS;
        return new NutrientValues(decimal(calories), decimal(protein), decimal(carbs), decimal(fat), basis);
    }

    private ServingValues servingSize(OpenFoodFactsProductDTO product) {
        String quantity = product.getServingQuantity();
        String unit = product.getServingQuantityUnit();
        if (quantity != null && !quantity.isBlank() && unit != null && !unit.isBlank()) {
            return new ServingValues(decimal(quantity), unit);
        }

        String label = product.getServingSize();
        if (label != null) {
            Matcher matcher = SERVING_SIZE_PATTERN.matcher(label);
            if (matcher.matches()) {
                return new ServingValues(decimal(matcher.group(1).replace(',', '.')), matcher.group(2));
            }
        }
        throw new IllegalArgumentException("Serving size is missing or not parseable.");
    }

    private BigDecimal decimal(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Required numeric field is missing.");
        }
        try {
            return new BigDecimal(value.trim().replace(',', '.'));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Provider numeric field is malformed.", exception);
        }
    }

    private String required(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Required provider field is missing.");
        }
        return value.trim();
    }

    private record ServingValues(BigDecimal quantity, String unit) {}
    private record NutrientValues(BigDecimal calories, BigDecimal protein, BigDecimal carbohydrates,
                                  BigDecimal fat, NutrientComposition.Basis basis) {}
}
