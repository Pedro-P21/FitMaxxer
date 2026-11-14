package com.pedropacheco.nutrition.backend.dailyLog.domain;

import jakarta.persistence.Embeddable;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Embeddable
public class MealType {
    private static final Set<String> SUPPORTED = Set.of("BREAKFAST", "LUNCH", "DINNER", "SNACKS");
    private String value;

    protected MealType() {}
    public MealType(String value) {
        this.value = normalizeName(value);
    }

    public static String normalizeName(String value) {
        if (value == null || !SUPPORTED.contains(value.trim().toUpperCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Meal type must be Breakfast, Lunch, Dinner, or Snacks.");
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }
    public String getValue() { return value; }
    public String getDisplayName() { return value.charAt(0) + value.substring(1).toLowerCase(Locale.ROOT); }
    @Override public boolean equals(Object other) {
        return this == other || other instanceof MealType that && value.equals(that.value);
    }
    @Override public int hashCode() { return Objects.hash(value); }
}
