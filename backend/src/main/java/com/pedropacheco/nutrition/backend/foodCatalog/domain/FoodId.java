package com.pedropacheco.nutrition.backend.foodCatalog.domain;

import java.util.Objects;

/** Stable identity for a catalog item, usually supplied by its catalog source. */
public final class FoodId {

    private final String value;

    public FoodId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Food ID cannot be null or blank.");
        }
        this.value = value.trim();
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof FoodId foodId)) return false;
        return value.equals(foodId.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
