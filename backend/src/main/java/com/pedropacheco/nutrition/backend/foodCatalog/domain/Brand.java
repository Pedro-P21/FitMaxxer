package com.pedropacheco.nutrition.backend.foodCatalog.domain;

import java.util.Objects;

/** Optional product brand supplied by the catalog source. */
public final class Brand {
    private final String value;

    public Brand(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Brand cannot be null or blank.");
        }
        this.value = value.trim();
    }

    public String getValue() { return value; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Brand brand)) return false;
        return value.equals(brand.value);
    }

    @Override
    public int hashCode() { return Objects.hash(value); }

    @Override
    public String toString() { return value; }
}
