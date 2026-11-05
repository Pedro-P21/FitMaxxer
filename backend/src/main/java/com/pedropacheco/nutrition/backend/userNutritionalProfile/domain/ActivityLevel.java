package com.pedropacheco.nutrition.backend.userNutritionalProfile.domain;

import jakarta.persistence.Embeddable;
import java.util.Objects;

@Embeddable
public class ActivityLevel {

    private String level; // Sedentary, LightlyActive, ModeratelyActive, VeryActive, ExtraActive
    private double factor;

    protected ActivityLevel() {}

    private ActivityLevel(String level, double factor) {
        this.level = level;
        this.factor = factor;
    }

    public static ActivityLevel of(String levelStr) {
        if (levelStr == null) {
            throw new IllegalArgumentException("Activity level cannot be null.");
        }
        return switch (levelStr.toUpperCase()) {
            case "SEDENTARY" -> new ActivityLevel("SEDENTARY", 1.2);
            case "LIGHTLY_ACTIVE" -> new ActivityLevel("LIGHTLY_ACTIVE", 1.375);
            case "MODERATELY_ACTIVE" -> new ActivityLevel("MODERATELY_ACTIVE", 1.55);
            case "VERY_ACTIVE" -> new ActivityLevel("VERY_ACTIVE", 1.725);
            case "EXTRA_ACTIVE" -> new ActivityLevel("EXTRA_ACTIVE", 1.9);
            default -> throw new IllegalArgumentException("Invalid activity level: " + levelStr);
        };
    }

    public String getLevel() { return level; }
    public double getFactor() { return factor; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ActivityLevel that = (ActivityLevel) o;
        return Double.compare(that.factor, factor) == 0 && Objects.equals(level, that.level);
    }

    @Override
    public int hashCode() {
        return Objects.hash(level, factor);
    }
}