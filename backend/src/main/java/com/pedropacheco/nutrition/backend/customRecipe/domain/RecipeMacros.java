package com.pedropacheco.nutrition.backend.customRecipe.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

/** Whole-recipe totals; owned and persisted exclusively through CustomRecipe. */
@Entity
@Table(name = "recipe_macros")
public class RecipeMacros {
    @Id
    private String id;
    @Column(nullable = false, precision = 30, scale = 10)
    private BigDecimal caloriesKcal;
    @Column(nullable = false, precision = 30, scale = 10)
    private BigDecimal proteinGrams;
    @Column(nullable = false, precision = 30, scale = 10)
    private BigDecimal carbohydrateGrams;
    @Column(nullable = false, precision = 30, scale = 10)
    private BigDecimal fatGrams;

    protected RecipeMacros() {}

    RecipeMacros(Values totals) {
        id = UUID.randomUUID().toString();
        caloriesKcal = rounded(totals.caloriesKcal());
        proteinGrams = rounded(totals.proteinGrams());
        carbohydrateGrams = rounded(totals.carbohydrateGrams());
        fatGrams = rounded(totals.fatGrams());
    }

    private static BigDecimal rounded(BigDecimal amount) {
        return RecipeNumbers.stored(amount.setScale(10, RoundingMode.HALF_UP), "Recipe total");
    }

    public String getId() { return id; }
    public BigDecimal getCaloriesKcal() { return caloriesKcal; }
    public BigDecimal getProteinGrams() { return proteinGrams; }
    public BigDecimal getCarbohydrateGrams() { return carbohydrateGrams; }
    public BigDecimal getFatGrams() { return fatGrams; }
    public Values getValues() { return new Values(caloriesKcal, proteinGrams, carbohydrateGrams, fatGrams); }

    /** Transient calculation result, not another persisted entity or shared aggregate object. */
    public record Values(BigDecimal caloriesKcal, BigDecimal proteinGrams,
                         BigDecimal carbohydrateGrams, BigDecimal fatGrams) {
        public static final Values ZERO = new Values(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

        public Values {
            caloriesKcal = RecipeNumbers.nonNegative(caloriesKcal, "Calories");
            proteinGrams = RecipeNumbers.nonNegative(proteinGrams, "Protein");
            carbohydrateGrams = RecipeNumbers.nonNegative(carbohydrateGrams, "Carbohydrates");
            fatGrams = RecipeNumbers.nonNegative(fatGrams, "Fat");
        }

        public Values plus(Values other) {
            return new Values(caloriesKcal.add(other.caloriesKcal), proteinGrams.add(other.proteinGrams),
                    carbohydrateGrams.add(other.carbohydrateGrams), fatGrams.add(other.fatGrams));
        }

        public Values perServing(BigDecimal servings) {
            servings = RecipeNumbers.positive(servings, "Number of servings");
            return new Values(caloriesKcal.divide(servings, 10, RoundingMode.HALF_UP),
                    proteinGrams.divide(servings, 10, RoundingMode.HALF_UP),
                    carbohydrateGrams.divide(servings, 10, RoundingMode.HALF_UP),
                    fatGrams.divide(servings, 10, RoundingMode.HALF_UP));
        }
    }
}
