package com.pedropacheco.nutrition.backend.customRecipe.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;

@Embeddable
public class RecipeName {
    @Column(name = "recipe_name", nullable = false)
    private String value;

    protected RecipeName() {}

    public RecipeName(String value) {

        if (value == null || value.isBlank() || value.trim().length() > 255)
            throw new IllegalArgumentException("Recipe name must contain between 1 and 255 characters.");
        this.value = value.trim();

    }
    public String getValue() { return value; }

    @Override public boolean equals(Object other) {
        return this == other || other instanceof RecipeName that && value.equals(that.value);
    }

    @Override public int hashCode() { return Objects.hash(value); }
}
