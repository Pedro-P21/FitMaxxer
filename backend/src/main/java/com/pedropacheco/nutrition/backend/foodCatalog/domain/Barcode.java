package com.pedropacheco.nutrition.backend.foodCatalog.domain;

import java.util.Objects;

/** Numeric GTIN/UPC/EAN barcode, retained as text to preserve leading zeroes. */
public final class Barcode {

    private final String value;

    public Barcode(String value) {
        if (value == null || !value.matches("\\d{8,14}")) {
            throw new IllegalArgumentException("Barcode must contain between 8 and 14 digits.");
        }
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Barcode barcode)) return false;
        return value.equals(barcode.value);
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
