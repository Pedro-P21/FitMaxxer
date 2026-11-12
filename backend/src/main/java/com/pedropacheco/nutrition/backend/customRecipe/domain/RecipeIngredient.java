package com.pedropacheco.nutrition.backend.customRecipe.domain;

import jakarta.persistence.*;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "recipe_ingredient")
public class RecipeIngredient {
    @Id
    private String id;

    @Embedded
    private IngredientSnapshot snapshot;

    @Embedded
    private IngredientQuantity quantity;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipe_id", nullable = false)
    private CustomRecipe recipe;

    protected RecipeIngredient() {}

    public RecipeIngredient(IngredientSnapshot snapshot, IngredientQuantity quantity) {
        this.snapshot = Objects.requireNonNull(snapshot, "Ingredient snapshot is required.");
        this.quantity = Objects.requireNonNull(quantity, "Ingredient quantity is required.");
        calculateContribution();
        id = UUID.randomUUID().toString();
    }
    boolean isAttached() { return recipe != null; }

    void attachTo(CustomRecipe recipe) {
        if (isAttached()) throw new IllegalStateException("An ingredient can belong to only one recipe.");
        this.recipe = Objects.requireNonNull(recipe);
    }

    public RecipeMacros.Values calculateContribution() {
        return snapshot.calculateContribution(quantity);
    }
    public String getId() { return id; }
    public IngredientSnapshot getSnapshot() { return snapshot; }
    public IngredientQuantity getQuantity() { return quantity; }
}
