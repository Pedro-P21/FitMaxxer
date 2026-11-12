package com.pedropacheco.nutrition.backend.customRecipe.domain;

import com.pedropacheco.nutrition.backend.userAuth.domain.UserId;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.*;

@Entity
@Table(name = "custom_recipe", indexes = @Index(name = "ix_custom_recipe_owner", columnList = "user_id"))
public class CustomRecipe {
    @EmbeddedId
    private RecipeId id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private String userId;

    @Embedded
    private RecipeName name;

    @Column(name = "number_of_servings", nullable = false, precision = 30, scale = 10)
    private BigDecimal numberOfServings;

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderColumn(name = "ingredient_position")
    private List<RecipeIngredient> ingredients = new ArrayList<>();

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, optional = false)
    @JoinColumn(name = "recipe_macros_id", nullable = false, unique = true)
    private RecipeMacros macros;

    protected CustomRecipe() {}

    public CustomRecipe(UserId owner, RecipeName name, BigDecimal numberOfServings,
                        List<RecipeIngredient> ingredients) {

        this.userId = Objects.requireNonNull(owner, "Owner is required.").getValue();
        this.name = Objects.requireNonNull(name, "Recipe name is required.");
        this.numberOfServings = RecipeNumbers.positive(numberOfServings, "Number of servings");

        if (ingredients == null || ingredients.isEmpty())
            throw new IllegalArgumentException("A recipe needs at least one ingredient.");

        List<RecipeIngredient> copy = List.copyOf(ingredients);
        Set<String> ids = new HashSet<>();
        RecipeMacros.Values totals = RecipeMacros.Values.ZERO;

        for (RecipeIngredient ingredient : copy) {

            if (ingredient.isAttached() || !ids.add(ingredient.getId()))

                throw new IllegalArgumentException("Ingredient rows must be distinct and belong to this recipe only.");

            totals = totals.plus(ingredient.calculateContribution());
        }
        this.macros = new RecipeMacros(totals);
        this.id = RecipeId.generate();
        copy.forEach(ingredient -> ingredient.attachTo(this));
        this.ingredients.addAll(copy);
    }

    public RecipeId getId() { return id; }
    public UserId getUserId() { return new UserId(userId); }
    public RecipeName getName() { return name; }
    public BigDecimal getNumberOfServings() { return numberOfServings; }
    public List<RecipeIngredient> getIngredients() { return Collections.unmodifiableList(ingredients); }
    public RecipeMacros getMacros() { return macros; }
    public RecipeMacros.Values getPerServingNutrition() { return macros.getValues().perServing(numberOfServings); }
}
