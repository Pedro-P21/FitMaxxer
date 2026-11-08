package com.pedropacheco.nutrition.backend.foodCatalog.domain;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Objects;

public final class ServingSize {

    private final BigDecimal quantity;
    private final String unit;

    public ServingSize(BigDecimal quantity, String unit) {
        if (quantity == null || quantity.signum() <= 0) {
            throw new IllegalArgumentException("Serving size quantity must be greater than zero.");
        }
        if (unit == null || unit.isBlank()) {
            throw new IllegalArgumentException("Serving size unit cannot be null or blank.");
        }
        this.quantity = quantity.stripTrailingZeros();
        this.unit = unit.trim().toLowerCase(Locale.ROOT);
    }

    public ServingSize(String quantity, String unit) {
        this(parseQuantity(quantity), unit);
    }

    private static BigDecimal parseQuantity(String quantity) {
        if (quantity == null || quantity.isBlank()) {
            throw new IllegalArgumentException("Serving size quantity cannot be null or blank.");
        }
        try {
            return new BigDecimal(quantity.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Serving size quantity must be numeric.", exception);
        }
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public String getUnit() {
        return unit;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ServingSize that)) return false;
        return quantity.compareTo(that.quantity) == 0 && unit.equals(that.unit);
    }

    @Override
    public int hashCode() {
        return Objects.hash(quantity.stripTrailingZeros(), unit);
    }
}
