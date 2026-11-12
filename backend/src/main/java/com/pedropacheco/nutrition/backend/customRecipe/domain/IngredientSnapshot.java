package com.pedropacheco.nutrition.backend.customRecipe.domain;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.math.BigDecimal;
import java.util.Objects;

@Embeddable
public class IngredientSnapshot {
    public enum Basis { PER_100_GRAMS, PER_100_MILLILITERS, PER_SERVING }
    @Column(nullable = false)
    private String foodName;
    private String brand;
    private String barcode;

    @Column(nullable = false, precision = 30, scale = 10)
    private BigDecimal caloriesKcal;

    @Column(nullable = false, precision = 30, scale = 10)
    private BigDecimal proteinGrams;

    @Column(nullable = false, precision = 30, scale = 10)
    private BigDecimal carbohydrateGrams;

    @Column(nullable = false, precision = 30, scale = 10)
    private BigDecimal fatGrams;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Basis nutrientBasis;

    @Column(nullable = false, precision = 30, scale = 10)
    private BigDecimal servingSizeQuantity;

    @Column(nullable = false)
    private String servingSizeUnit;

    protected IngredientSnapshot() {}

    public IngredientSnapshot(String foodName, String brand, String barcode,
                        BigDecimal caloriesKcal, BigDecimal proteinGrams,
                        BigDecimal carbohydrateGrams, BigDecimal fatGrams,
                        Basis nutrientBasis,
                        BigDecimal servingSizeQuantity, String servingSizeUnit) {

        if (foodName == null || foodName.isBlank()) throw new IllegalArgumentException("Food name cannot be blank.");

        this.foodName = foodName.trim();
        this.brand = blankToNull(brand);
        this.barcode = blankToNull(barcode);
        this.caloriesKcal = RecipeNumbers.stored(caloriesKcal, "Calories");
        this.proteinGrams = RecipeNumbers.stored(proteinGrams, "Protein");
        this.carbohydrateGrams = RecipeNumbers.stored(carbohydrateGrams, "Carbohydrates");
        this.fatGrams = RecipeNumbers.stored(fatGrams, "Fat");
        this.nutrientBasis = Objects.requireNonNull(nutrientBasis, "Nutrient basis cannot be null.");

        if (servingSizeQuantity == null || servingSizeQuantity.signum() <= 0
                || servingSizeUnit == null || servingSizeUnit.isBlank()) {
            throw new IllegalArgumentException("A positive catalog serving size and unit are required.");
        }
        this.servingSizeQuantity = RecipeNumbers.positive(servingSizeQuantity, "Serving size");
        this.servingSizeUnit = servingSizeUnit.trim().toLowerCase(java.util.Locale.ROOT);
    }

    public RecipeMacros.Values calculateContribution(IngredientQuantity quantity) {
        return new IngredientNutritionCalculator().calculateContribution(this, Objects.requireNonNull(quantity));
    }

    private static String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    public String getFoodName() { return foodName; }
    public String getBrand() { return brand; }
    public String getBarcode() { return barcode; }
    public BigDecimal getCaloriesKcal() { return caloriesKcal; }
    public BigDecimal getProteinGrams() { return proteinGrams; }
    public BigDecimal getCarbohydrateGrams() { return carbohydrateGrams; }
    public BigDecimal getFatGrams() { return fatGrams; }
    public Basis getNutrientBasis() { return nutrientBasis; }
    public BigDecimal getServingSizeQuantity() { return servingSizeQuantity; }
    public String getServingSizeUnit() { return servingSizeUnit; }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof IngredientSnapshot that)) return false;
        return Objects.equals(foodName, that.foodName) && Objects.equals(brand, that.brand)
                && Objects.equals(barcode, that.barcode)
                && caloriesKcal.compareTo(that.caloriesKcal) == 0
                && proteinGrams.compareTo(that.proteinGrams) == 0
                && carbohydrateGrams.compareTo(that.carbohydrateGrams) == 0
                && fatGrams.compareTo(that.fatGrams) == 0 && nutrientBasis == that.nutrientBasis
                && servingSizeQuantity.compareTo(that.servingSizeQuantity) == 0
                && Objects.equals(servingSizeUnit, that.servingSizeUnit);
    }
    @Override public int hashCode() {
        return Objects.hash(foodName, brand, barcode, caloriesKcal.stripTrailingZeros(),
                proteinGrams.stripTrailingZeros(), carbohydrateGrams.stripTrailingZeros(),
                fatGrams.stripTrailingZeros(), nutrientBasis, servingSizeQuantity.stripTrailingZeros(), servingSizeUnit);
    }
}
