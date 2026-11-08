package com.pedropacheco.nutrition.backend.foodCatalog.domain;

import java.util.Objects;

public final class FoodName {

    private final String value;

    public FoodName(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Food name cannot be null or blank.");
        }
        this.value = value.trim();
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof FoodName foodName)) return false;
        return value.equals(foodName.value);
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
