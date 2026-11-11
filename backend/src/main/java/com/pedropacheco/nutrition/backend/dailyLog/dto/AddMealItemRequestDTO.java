package com.pedropacheco.nutrition.backend.dailyLog.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.pedropacheco.nutrition.backend.dailyLog.domain.PortionQuantity;
import java.math.BigDecimal;

public record AddMealItemRequestDTO(String foodId, String mealType,
                                    @JsonProperty("quantity") @JsonAlias("servingQuantity") BigDecimal quantity,
                                    PortionQuantity.Unit quantityUnit) {
    public AddMealItemRequestDTO {
        if (foodId == null || foodId.isBlank()) throw new IllegalArgumentException("Food ID cannot be blank.");
        if (mealType == null || mealType.isBlank()) throw new IllegalArgumentException("Meal type cannot be blank.");
        if (quantity == null || quantity.signum() <= 0) {
            throw new IllegalArgumentException("Portion quantity must be greater than zero.");
        }
        if (quantityUnit == PortionQuantity.Unit.NAMED) {
            throw new IllegalArgumentException("Named portions require a measured equivalent and are not available yet.");
        }
        if (quantityUnit == null) quantityUnit = PortionQuantity.Unit.SERVING;
        foodId = foodId.trim();
        mealType = mealType.trim();
    }

    /** Keeps the original three-argument Java API as serving-count input. */
    public AddMealItemRequestDTO(String foodId, String mealType, BigDecimal servingQuantity) {
        this(foodId, mealType, servingQuantity, PortionQuantity.Unit.SERVING);
    }
}
