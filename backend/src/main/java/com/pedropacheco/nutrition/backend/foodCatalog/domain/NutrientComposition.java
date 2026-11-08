package com.pedropacheco.nutrition.backend.foodCatalog.domain;

import java.math.BigDecimal;
import java.util.Objects;

/** Nutrients for a clearly identified measurement basis. */
public final class NutrientComposition {

    public enum Basis {
        PER_100_GRAMS,
        PER_100_MILLILITERS,
        PER_SERVING
    }

    private final BigDecimal caloriesKcal;
    private final BigDecimal proteinGrams;
    private final BigDecimal carbohydrateGrams;
    private final BigDecimal fatGrams;
    private final Basis basis;

    public NutrientComposition(BigDecimal caloriesKcal, BigDecimal proteinGrams,
                               BigDecimal carbohydrateGrams, BigDecimal fatGrams, Basis basis) {
        this.caloriesKcal = nonNegative("Calories", caloriesKcal);
        this.proteinGrams = nonNegative("Protein", proteinGrams);
        this.carbohydrateGrams = nonNegative("Carbohydrates", carbohydrateGrams);
        this.fatGrams = nonNegative("Fat", fatGrams);
        if (basis == null) {
            throw new IllegalArgumentException("Nutrient measurement basis cannot be null.");
        }
        this.basis = basis;
    }

    public NutrientComposition(String caloriesKcal, String proteinGrams,
                               String carbohydrateGrams, String fatGrams, Basis basis) {
        this(parse("Calories", caloriesKcal), parse("Protein", proteinGrams),
                parse("Carbohydrates", carbohydrateGrams), parse("Fat", fatGrams), basis);
    }

    private static BigDecimal nonNegative(String label, BigDecimal value) {
        if (value == null || value.signum() < 0) {
            throw new IllegalArgumentException(label + " must be zero or greater.");
        }
        return value.stripTrailingZeros();
    }

    private static BigDecimal parse(String label, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " cannot be null or blank.");
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(label + " must be numeric.", exception);
        }
    }

    public BigDecimal getCaloriesKcal() { return caloriesKcal; }
    public BigDecimal getProteinGrams() { return proteinGrams; }
    public BigDecimal getCarbohydrateGrams() { return carbohydrateGrams; }
    public BigDecimal getFatGrams() { return fatGrams; }
    public Basis getBasis() { return basis; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof NutrientComposition that)) return false;
        return caloriesKcal.compareTo(that.caloriesKcal) == 0
                && proteinGrams.compareTo(that.proteinGrams) == 0
                && carbohydrateGrams.compareTo(that.carbohydrateGrams) == 0
                && fatGrams.compareTo(that.fatGrams) == 0
                && basis == that.basis;
    }

    @Override
    public int hashCode() {
        return Objects.hash(caloriesKcal, proteinGrams, carbohydrateGrams, fatGrams, basis);
    }
}
