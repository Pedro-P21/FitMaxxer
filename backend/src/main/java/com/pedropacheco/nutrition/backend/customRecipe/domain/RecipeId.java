package com.pedropacheco.nutrition.backend.customRecipe.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class RecipeId {
    @Column(name = "recipe_id", nullable = false)
    private String value;

    protected RecipeId() {}

    public RecipeId(String value) {
        if (value == null || value.isBlank() || value.trim().length() > 255)
            throw new IllegalArgumentException("Recipe ID must contain between 1 and 255 characters.");
        this.value = value.trim();
    }

    public static RecipeId generate() { return new RecipeId(UUID.randomUUID().toString()); }

    public String getValue() { return value; }

    @Override public boolean equals(Object other) {
        return this == other || other instanceof RecipeId that && value.equals(that.value);
    }

    @Override public int hashCode() { return Objects.hash(value); }
}
