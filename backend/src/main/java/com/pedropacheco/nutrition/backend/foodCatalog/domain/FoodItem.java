package com.pedropacheco.nutrition.backend.foodCatalog.domain;

import java.util.Objects;

/** Aggregate root for a normalized catalog food. */
public final class FoodItem {

    private final FoodId id;
    private final FoodName name;
    private final Brand brand;
    private final Barcode barcode;
    private final NutrientComposition nutrientComposition;
    private final ServingSize servingSize;

    public FoodItem(FoodId id, FoodName name, Brand brand, Barcode barcode,
                    NutrientComposition nutrientComposition, ServingSize servingSize) {
        this.id = Objects.requireNonNull(id, "Food ID cannot be null.");
        this.name = Objects.requireNonNull(name, "Food name cannot be null.");
        this.brand = brand;
        this.nutrientComposition = Objects.requireNonNull(
                nutrientComposition, "Nutrient composition cannot be null.");
        this.servingSize = Objects.requireNonNull(servingSize, "Serving size cannot be null.");
        this.barcode = barcode;
    }

    public FoodId getId() { return id; }
    public FoodName getName() { return name; }
    public Brand getBrand() { return brand; }
    public Barcode getBarcode() { return barcode; }
    public NutrientComposition getNutrientComposition() { return nutrientComposition; }
    public ServingSize getServingSize() { return servingSize; }

    /** Food items have entity identity, so equality is based on FoodId. */
    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof FoodItem foodItem)) return false;
        return id.equals(foodItem.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
