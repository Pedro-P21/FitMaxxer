package com.pedropacheco.nutrition.backend.customRecipe.dto;

import java.util.List;

public record RecipePageResponseDTO(List<RecipeSummaryDTO> items, int page, int size,
                                    long totalElements, int totalPages) {
    public RecipePageResponseDTO { items = List.copyOf(items); }
}
