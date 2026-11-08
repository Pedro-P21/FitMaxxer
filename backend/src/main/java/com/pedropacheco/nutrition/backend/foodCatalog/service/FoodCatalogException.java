package com.pedropacheco.nutrition.backend.foodCatalog.service;

/** Indicates that every configured food catalog provider failed to answer. */
public class FoodCatalogException extends RuntimeException {
    public FoodCatalogException(String message, Throwable cause) {
        super(message, cause);
    }
}
