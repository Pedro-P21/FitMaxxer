package com.pedropacheco.nutrition.backend.customRecipe.repository;

import com.pedropacheco.nutrition.backend.customRecipe.domain.CustomRecipe;
import com.pedropacheco.nutrition.backend.customRecipe.domain.RecipeId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomRecipeRepository extends JpaRepository<CustomRecipe, RecipeId> {
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = "macros")
    org.springframework.data.domain.Page<CustomRecipe> findByUserId(String userId, org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"macros", "ingredients"})
    java.util.Optional<CustomRecipe> findByIdAndUserId(RecipeId id, String userId);
}
