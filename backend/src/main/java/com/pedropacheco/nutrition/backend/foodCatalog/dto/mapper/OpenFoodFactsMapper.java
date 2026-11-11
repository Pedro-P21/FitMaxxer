package com.pedropacheco.nutrition.backend.foodCatalog.dto.mapper;

import com.pedropacheco.nutrition.backend.foodCatalog.domain.FoodItem;
import com.pedropacheco.nutrition.backend.foodCatalog.domain.NutrientComposition;
import com.pedropacheco.nutrition.backend.foodCatalog.factory.FoodItemFactory;
import com.pedropacheco.nutrition.backend.foodCatalog.dto.OpenFoodFactsNutrimentsDTO;
import com.pedropacheco.nutrition.backend.foodCatalog.dto.OpenFoodFactsProductDTO;
import com.pedropacheco.nutrition.backend.foodCatalog.dto.OpenFoodFactsSearchResponseDTO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Converts Open Food Facts DTOs into validated catalog domain entities. */
@Component
public class OpenFoodFactsMapper {

    private static final Pattern SERVING_SIZE_PATTERN =
            Pattern.compile("^\\s*([0-9]+(?:[.,][0-9]+)?)\\s*([\\p{L}]+)\\s*$");

    private final FoodItemFactory foodItemFactory;

    public OpenFoodFactsMapper(FoodItemFactory foodItemFactory) {
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

        // Open Food Facts exposes normalized per-serving values even when the
        // package label itself reports nutrients per 100 g/ml. Prefer them so
        // callers can scale a serving count without reconstructing the serving.
        boolean hasNormalizedServingValues = hasValues(source.getEnergyKcalPerServing(), source.getProteinsPerServing(),
                source.getCarbohydratesPerServing(), source.getFatPerServing());

        boolean perServing = hasNormalizedServingValues
                || "serving".equalsIgnoreCase(product.getNutritionDataPer());

        String calories = perServing ? source.getEnergyKcalPerServing() : source.getEnergyKcalPer100g();
        String protein = perServing ? source.getProteinsPerServing() : source.getProteinsPer100g();
        String carbs = perServing ? source.getCarbohydratesPerServing() : source.getCarbohydratesPer100g();
        String fat = perServing ? source.getFatPerServing() : source.getFatPer100g();

        NutrientComposition.Basis basis = perServing
                ? NutrientComposition.Basis.PER_SERVING
                : normalizedPerHundredBasis(product);
        return new NutrientValues(decimal(calories), decimal(protein), decimal(carbs), decimal(fat), basis);
    }

    private boolean hasValues(String calories, String protein, String carbohydrates, String fat) {
        return usable(calories) && usable(protein) && usable(carbohydrates) && usable(fat);
    }

    private boolean usable(String value) {
        if (value == null || value.isBlank()) return false;
        try {
            return new BigDecimal(value.trim()).signum() >= 0;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private NutrientComposition.Basis normalizedPerHundredBasis(OpenFoodFactsProductDTO product) {
        String declaredBasis = product.getNutritionDataPer();
        if (declaredBasis != null && declaredBasis.replace(" ", "").equalsIgnoreCase("100ml")) {
            return NutrientComposition.Basis.PER_100_MILLILITERS;
        }
        // OFF's normalized *_100g fields are per 100 ml for liquids. Its
        // serving unit is the source-provided indication available here.
        if (isMilliliterUnit(product.getServingQuantityUnit())) {
            return NutrientComposition.Basis.PER_100_MILLILITERS;
        }
        return NutrientComposition.Basis.PER_100_GRAMS;
    }

    private boolean isMilliliterUnit(String unit) {
        if (unit == null) return false;
        return switch (unit.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "ml", "milliliter", "milliliters", "millilitre", "millilitres" -> true;
            default -> false;
        };
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
