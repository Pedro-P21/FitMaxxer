package com.pedropacheco.nutrition.backend.dailyLog.domain;

import com.pedropacheco.nutrition.backend.foodCatalog.domain.NutrientComposition;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.math.BigDecimal;
import java.util.Objects;

@Embeddable
public class FoodSnapshot {
    private String foodName;
    private String brand;
    private String barcode;
    private BigDecimal caloriesKcal;
    private BigDecimal proteinGrams;
    private BigDecimal carbohydrateGrams;
    private BigDecimal fatGrams;
    @Enumerated(EnumType.STRING)
    private NutrientComposition.Basis nutrientBasis;
    private BigDecimal servingSizeQuantity;
    private String servingSizeUnit;

    protected FoodSnapshot() {}

    public FoodSnapshot(String foodName, String brand, String barcode,
                        BigDecimal caloriesKcal, BigDecimal proteinGrams,
                        BigDecimal carbohydrateGrams, BigDecimal fatGrams,
                        NutrientComposition.Basis nutrientBasis,
                        BigDecimal servingSizeQuantity, String servingSizeUnit) {
        if (foodName == null || foodName.isBlank()) throw new IllegalArgumentException("Food name cannot be blank.");
        this.foodName = foodName.trim();
        this.brand = blankToNull(brand);
        this.barcode = blankToNull(barcode);
        this.caloriesKcal = nonNegative(caloriesKcal, "Calories");
        this.proteinGrams = nonNegative(proteinGrams, "Protein");
        this.carbohydrateGrams = nonNegative(carbohydrateGrams, "Carbohydrates");
        this.fatGrams = nonNegative(fatGrams, "Fat");
        this.nutrientBasis = Objects.requireNonNull(nutrientBasis, "Nutrient basis cannot be null.");
        if (servingSizeQuantity == null || servingSizeQuantity.signum() <= 0
                || servingSizeUnit == null || servingSizeUnit.isBlank()) {
            throw new IllegalArgumentException("A positive catalog serving size and unit are required.");
        }
        this.servingSizeQuantity = servingSizeQuantity.stripTrailingZeros();
        this.servingSizeUnit = servingSizeUnit.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private static BigDecimal nonNegative(BigDecimal value, String label) {
        if (value == null || value.signum() < 0) throw new IllegalArgumentException(label + " must be non-negative.");
        return value.stripTrailingZeros();
    }
    private static String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    public String getFoodName() { return foodName; }
    public String getBrand() { return brand; }
    public String getBarcode() { return barcode; }
    public BigDecimal getCaloriesKcal() { return caloriesKcal; }
    public BigDecimal getProteinGrams() { return proteinGrams; }
    public BigDecimal getCarbohydrateGrams() { return carbohydrateGrams; }
    public BigDecimal getFatGrams() { return fatGrams; }
    public NutrientComposition.Basis getNutrientBasis() { return nutrientBasis; }
    public BigDecimal getServingSizeQuantity() { return servingSizeQuantity; }
    public String getServingSizeUnit() { return servingSizeUnit; }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof FoodSnapshot that)) return false;
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
