package com.pedropacheco.nutrition.backend.customRecipe.service;

public class RecipeNotFoundException extends RuntimeException {
    public RecipeNotFoundException() { super("Recipe not found."); }
}
