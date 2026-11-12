package com.pedropacheco.nutrition.backend.customRecipe.domain;

import com.pedropacheco.nutrition.backend.cli.TerminalRunner;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import static com.pedropacheco.nutrition.backend.customRecipe.domain.CustomRecipeTest.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:us006domain;DB_CLOSE_DELAY=-1",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional
class CustomRecipePersistenceTest {
    @Autowired EntityManager entityManager;
    @MockitoBean TerminalRunner terminalRunner;

    @Test
    void persistsAndReloadsCompleteAggregateWithOrderedSnapshotsAndOwnedMacros() {
        var snapshot = food("Oats", IngredientSnapshot.Basis.PER_100_GRAMS, "100", "g");
        var recipe = recipe(n("2.5"), List.of(ingredient(snapshot, "75.5", IngredientQuantity.Unit.GRAM),
                ingredient(snapshot, "24.5", IngredientQuantity.Unit.GRAM)));
        var ingredientIds = recipe.getIngredients().stream().map(RecipeIngredient::getId).toList();
        String macrosId = recipe.getMacros().getId();
        entityManager.persist(recipe);
        entityManager.flush();
        entityManager.clear();

        var loaded = entityManager.find(CustomRecipe.class, recipe.getId());
        assertEquals("owner", loaded.getUserId().getValue());
        assertEquals("Homemade bowl", loaded.getName().getValue());
        equalsNumber("2.5", loaded.getNumberOfServings());
        assertEquals(ingredientIds, loaded.getIngredients().stream().map(RecipeIngredient::getId).toList());
        assertEquals(snapshot, loaded.getIngredients().get(0).getSnapshot());
        equalsNumber("75.5", loaded.getIngredients().get(0).getQuantity().getAmount());
        assertEquals(macrosId, loaded.getMacros().getId());
        equalsNumber("200", loaded.getMacros().getCaloriesKcal());
        equalsNumber("10", loaded.getMacros().getProteinGrams());
        equalsNumber("30", loaded.getMacros().getCarbohydrateGrams());
        equalsNumber("5", loaded.getMacros().getFatGrams());
        equalsNumber("80", loaded.getPerServingNutrition().caloriesKcal());

        entityManager.remove(loaded);
        entityManager.flush();
        entityManager.clear();
        assertNull(entityManager.find(RecipeMacros.class, macrosId));
        ingredientIds.forEach(id -> assertNull(entityManager.find(RecipeIngredient.class, id)));
    }
}
