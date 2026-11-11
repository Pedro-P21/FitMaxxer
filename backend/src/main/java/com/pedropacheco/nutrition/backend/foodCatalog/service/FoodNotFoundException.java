package com.pedropacheco.nutrition.backend.foodCatalog.service;

public class FoodNotFoundException extends RuntimeException {
    public FoodNotFoundException(String foodId) {
        super("No loggable catalog food was found for ID: " + foodId);
    }
}
