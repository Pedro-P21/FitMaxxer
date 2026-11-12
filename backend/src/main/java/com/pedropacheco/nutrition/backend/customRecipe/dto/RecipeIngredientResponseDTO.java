package com.pedropacheco.nutrition.backend.customRecipe.dto;

import com.pedropacheco.nutrition.backend.customRecipe.domain.IngredientQuantity;
import com.pedropacheco.nutrition.backend.customRecipe.domain.IngredientSnapshot;
import java.math.BigDecimal;

public record RecipeIngredientResponseDTO(String ingredientId, String foodName, String brand, String barcode,
        BigDecimal quantity, IngredientQuantity.Unit quantityUnit,
        IngredientSnapshot.Basis nutrientBasis, BigDecimal servingQuantity, String servingUnit,
        RecipeNutrientsDTO nutrients) {}
