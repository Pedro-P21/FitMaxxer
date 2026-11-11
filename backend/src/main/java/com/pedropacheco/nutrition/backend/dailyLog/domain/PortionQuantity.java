package com.pedropacheco.nutrition.backend.dailyLog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * A logged amount independent of any food-catalog provider. Named portions can
 * retain an equivalent in servings, grams, or milliliters for future options.
 */
@Embeddable
public class PortionQuantity {
    public enum Unit { SERVING, GRAM, MILLILITER, NAMED }

    // Keep the existing database column so stored serving-only logs remain readable.
    @Column(name = "serving_quantity", nullable = false, precision = 12, scale = 4)
    private BigDecimal amount;

    // Nullable for rows written before unit-aware portions were introduced.
    @Enumerated(EnumType.STRING)
    @Column(name = "quantity_unit")
    private Unit unit;

    @Column(name = "portion_label")
    private String label;

    @Column(name = "portion_equivalent_amount", precision = 12, scale = 4)
    private BigDecimal equivalentAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "portion_equivalent_unit")
    private Unit equivalentUnit;

    protected PortionQuantity() {}

    /** Compatibility constructor: a numeric quantity with no unit means catalog servings. */
    public PortionQuantity(BigDecimal servings) { this(servings, Unit.SERVING); }
    public PortionQuantity(String servings) { this(new BigDecimal(servings), Unit.SERVING); }

    public PortionQuantity(BigDecimal amount, Unit unit) {
        this(amount, unit, null, null, null);
    }

    private PortionQuantity(BigDecimal amount, Unit unit, String label,
                            BigDecimal equivalentAmount, Unit equivalentUnit) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Portion amount must be greater than zero.");
        }
        if (unit == null) throw new IllegalArgumentException("Portion unit cannot be null.");
        if (unit == Unit.NAMED) {
            if (label == null || label.isBlank()) throw new IllegalArgumentException("A named portion needs a label.");
            if (equivalentAmount == null || equivalentAmount.signum() <= 0) {
                throw new IllegalArgumentException("A named portion needs a positive measured equivalent.");
            }
            if (equivalentUnit != Unit.SERVING && equivalentUnit != Unit.GRAM && equivalentUnit != Unit.MILLILITER) {
                throw new IllegalArgumentException("A named portion equivalent must be servings, grams, or milliliters.");
            }
        } else if (label != null || equivalentAmount != null || equivalentUnit != null) {
            throw new IllegalArgumentException("Only a named portion can have a label and measured equivalent.");
        }
        this.amount = amount.stripTrailingZeros();
        this.unit = unit;
        this.label = label == null ? null : label.trim();
        this.equivalentAmount = equivalentAmount == null ? null : equivalentAmount.stripTrailingZeros();
        this.equivalentUnit = equivalentUnit;
    }

    public static PortionQuantity servings(BigDecimal amount) { return new PortionQuantity(amount, Unit.SERVING); }
    public static PortionQuantity grams(BigDecimal amount) { return new PortionQuantity(amount, Unit.GRAM); }
    public static PortionQuantity milliliters(BigDecimal amount) { return new PortionQuantity(amount, Unit.MILLILITER); }
    public static PortionQuantity named(String label, BigDecimal count, BigDecimal equivalentAmount, Unit equivalentUnit) {
        return new PortionQuantity(count, Unit.NAMED, label, equivalentAmount, equivalentUnit);
    }

    public BigDecimal getAmount() { return amount; }
    /** Old accessor retained for callers while serving-only flows are migrated. */
    public BigDecimal getServings() { return amount; }
    public Unit getUnit() { return unit == null ? Unit.SERVING : unit; }
    public String getLabel() { return label; }
    public BigDecimal getEquivalentAmount() { return equivalentAmount; }
    public Unit getEquivalentUnit() { return equivalentUnit; }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof PortionQuantity that)) return false;
        return amount.compareTo(that.amount) == 0 && getUnit() == that.getUnit()
                && Objects.equals(label, that.label)
                && decimalEquals(equivalentAmount, that.equivalentAmount)
                && equivalentUnit == that.equivalentUnit;
    }
    @Override public int hashCode() {
        return Objects.hash(amount.stripTrailingZeros(), getUnit(), label,
                equivalentAmount == null ? null : equivalentAmount.stripTrailingZeros(), equivalentUnit);
    }
    private boolean decimalEquals(BigDecimal left, BigDecimal right) {
        return left == null ? right == null : right != null && left.compareTo(right) == 0;
    }
}
