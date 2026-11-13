package com.pedropacheco.nutrition.backend.dailyLog.dto;

import java.math.BigDecimal;

public record ChangeMealItemQuantityRequestDTO(BigDecimal quantity) {
    public ChangeMealItemQuantityRequestDTO {
        if (quantity == null || quantity.signum() <= 0)
            throw new IllegalArgumentException("Quantity must be positive.");
        quantity = quantity.stripTrailingZeros();
        if (quantity.scale() > 4 || quantity.precision() - quantity.scale() > 8)
            throw new IllegalArgumentException("Quantity supports up to 8 integer and 4 decimal digits.");
    }
}
