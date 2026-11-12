package com.pedropacheco.nutrition.backend.customRecipe.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.Objects;

@Embeddable
public class IngredientQuantity {

    public enum Unit { SERVING, GRAM, MILLILITER }

    @Column(name = "quantity_amount", nullable = false, precision = 30, scale = 10)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "quantity_unit", nullable = false)
    private Unit unit;

    protected IngredientQuantity() {}

    public IngredientQuantity(BigDecimal amount, Unit unit) {
        this.amount = RecipeNumbers.positive(amount, "Ingredient quantity");
        if (unit == null) throw new IllegalArgumentException("Ingredient unit is required.");
        this.unit = unit;
    }

    public BigDecimal getAmount() { return amount; }

    public Unit getUnit() { return unit; }

    @Override public boolean equals(Object other) {
        return this == other || other instanceof IngredientQuantity that
                && amount.compareTo(that.amount) == 0 && unit == that.unit;
    }
    @Override public int hashCode() { return Objects.hash(amount.stripTrailingZeros(), unit); }
}
