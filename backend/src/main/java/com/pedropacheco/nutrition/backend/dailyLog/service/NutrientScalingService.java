package com.pedropacheco.nutrition.backend.dailyLog.service;

import com.pedropacheco.nutrition.backend.dailyLog.domain.FoodSnapshot;
import com.pedropacheco.nutrition.backend.dailyLog.domain.NutrientTotals;
import com.pedropacheco.nutrition.backend.dailyLog.domain.PortionQuantity;
import com.pedropacheco.nutrition.backend.foodCatalog.domain.NutrientComposition;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Map;

/** Converts supported portion amounts into the basis used by a food's nutrients. */
@Component
public class NutrientScalingService {
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private static final int DIVISION_SCALE = 10;
    private static final Map<String, BigDecimal> GRAM_FACTORS = Map.of(
            "g", BigDecimal.ONE, "gram", BigDecimal.ONE, "grams", BigDecimal.ONE,
            "kg", BigDecimal.valueOf(1000), "mg", new BigDecimal("0.001"));
    private static final Map<String, BigDecimal> MILLILITER_FACTORS = Map.of(
            "ml", BigDecimal.ONE, "milliliter", BigDecimal.ONE, "milliliters", BigDecimal.ONE,
            "millilitre", BigDecimal.ONE, "millilitres", BigDecimal.ONE,
            "l", BigDecimal.valueOf(1000), "liter", BigDecimal.valueOf(1000), "liters", BigDecimal.valueOf(1000),
            "cl", BigDecimal.valueOf(10), "dl", BigDecimal.valueOf(100));

    public NutrientTotals calculateContribution(FoodSnapshot food, PortionQuantity portion) {
        NutrientComposition.Basis basis = food.getNutrientBasis();
        BigDecimal multiplier;
        if (basis == NutrientComposition.Basis.PER_SERVING) {
            multiplier = servingCount(food, portion);
        } else {
            PortionQuantity.Unit requiredUnit = basis == NutrientComposition.Basis.PER_100_GRAMS
                    ? PortionQuantity.Unit.GRAM : PortionQuantity.Unit.MILLILITER;
            BigDecimal measuredAmount = amountInRequiredUnit(food, portion, requiredUnit);
            multiplier = measuredAmount.divide(ONE_HUNDRED, DIVISION_SCALE, RoundingMode.HALF_UP);
        }
        return new NutrientTotals(food.getCaloriesKcal().multiply(multiplier),
                food.getProteinGrams().multiply(multiplier),
                food.getCarbohydrateGrams().multiply(multiplier),
                food.getFatGrams().multiply(multiplier));
    }

    private BigDecimal servingCount(FoodSnapshot food, PortionQuantity portion) {
        if (portion.getUnit() == PortionQuantity.Unit.SERVING) return portion.getAmount();
        if (portion.getUnit() == PortionQuantity.Unit.NAMED
                && portion.getEquivalentUnit() == PortionQuantity.Unit.SERVING) {
            return portion.getAmount().multiply(portion.getEquivalentAmount());
        }
        MeasuredAmount amount = measuredAmount(food, portion);
        MeasuredAmount serving = normalize(food.getServingSizeQuantity(), food.getServingSizeUnit());
        if (amount.unit() != serving.unit()) {
            throw incompatible(portion, food, "catalog serving");
        }
        return amount.amount().divide(serving.amount(), DIVISION_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal amountInRequiredUnit(FoodSnapshot food, PortionQuantity portion,
                                            PortionQuantity.Unit requiredUnit) {
        MeasuredAmount amount = measuredAmount(food, portion);
        if (amount.unit() != requiredUnit) throw incompatible(portion, food, basisName(requiredUnit));
        return amount.amount();
    }

    private MeasuredAmount measuredAmount(FoodSnapshot food, PortionQuantity portion) {
        return switch (portion.getUnit()) {
            case SERVING -> normalize(food.getServingSizeQuantity().multiply(portion.getAmount()), food.getServingSizeUnit());
            case GRAM -> new MeasuredAmount(portion.getAmount(), PortionQuantity.Unit.GRAM);
            case MILLILITER -> new MeasuredAmount(portion.getAmount(), PortionQuantity.Unit.MILLILITER);
            case NAMED -> {
                if (portion.getEquivalentUnit() == PortionQuantity.Unit.SERVING) {
                    throw new IllegalArgumentException("This named portion is defined in servings, not grams or milliliters.");
                }
                yield normalize(portion.getAmount().multiply(portion.getEquivalentAmount()),
                        portion.getEquivalentUnit() == PortionQuantity.Unit.GRAM ? "g" : "ml");
            }
        };
    }

    private MeasuredAmount normalize(BigDecimal amount, String unit) {
        if (unit == null || unit.isBlank()) throw new IllegalArgumentException("Food portion unit is missing.");
        String normalized = unit.trim().toLowerCase(Locale.ROOT);
        BigDecimal gramFactor = GRAM_FACTORS.get(normalized);
        if (gramFactor != null) return new MeasuredAmount(amount.multiply(gramFactor), PortionQuantity.Unit.GRAM);
        BigDecimal milliliterFactor = MILLILITER_FACTORS.get(normalized);
        if (milliliterFactor != null) return new MeasuredAmount(amount.multiply(milliliterFactor), PortionQuantity.Unit.MILLILITER);
        throw new IllegalArgumentException("Unsupported food quantity unit '" + unit + "'. Use grams, milliliters, or a catalog serving.");
    }

    private IllegalArgumentException incompatible(PortionQuantity portion, FoodSnapshot food, String expected) {
        String entered = portion.getUnit() == PortionQuantity.Unit.NAMED
                ? "named portion '" + portion.getLabel() + "'"
                : portion.getAmount().stripTrailingZeros().toPlainString() + " " + portion.getUnit().name().toLowerCase(Locale.ROOT);
        return new IllegalArgumentException("Cannot calculate nutrients for " + entered + ": this food needs "
                + expected + " but its catalog serving is " + food.getServingSizeQuantity().stripTrailingZeros().toPlainString()
                + " " + food.getServingSizeUnit() + " and its nutrient basis is " + food.getNutrientBasis() + ".");
    }

    private String basisName(PortionQuantity.Unit unit) {
        return unit == PortionQuantity.Unit.GRAM ? "an amount in grams" : "an amount in milliliters";
    }

    private record MeasuredAmount(BigDecimal amount, PortionQuantity.Unit unit) {}
}
