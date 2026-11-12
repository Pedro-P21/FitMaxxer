package com.pedropacheco.nutrition.backend.cli;

import com.pedropacheco.nutrition.backend.customRecipe.domain.IngredientQuantity;
import com.pedropacheco.nutrition.backend.customRecipe.service.CustomRecipeService;
import com.pedropacheco.nutrition.backend.foodCatalog.dto.FoodItemDTO;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Scanner;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CustomRecipeUITest {
    private final FoodCatalogSearchUI catalog = mock(FoodCatalogSearchUI.class);
    private final CustomRecipeService service =
            mock(CustomRecipeService.class);
    private final CustomRecipeUI ui = new CustomRecipeUI(catalog, service);
    private FoodItemDTO food() {
        return new FoodItemDTO("openfoodfacts:12345678", "Oats", null, "12345678", new BigDecimal("100"), "g",
                new BigDecimal("200"), BigDecimal.TEN, new BigDecimal("30"), new BigDecimal("5"), "PER_100_GRAMS");
    }
    @Test
    void rejectsIncompatibleQuantityAndContinuesWithTheSameDraft() {
        when(catalog.chooseFood(any())).thenReturn(food());
        var input = new Scanner("Bowl\n2\n1\n3\n100\n1\n2\n150\n2\n");
        var draft = ui.buildDraft(input).orElseThrow();
        assertEquals(1, draft.getIngredients().size());
        assertEquals(IngredientQuantity.Unit.GRAM, draft.getSelections().get(0).quantityUnit());
        assertEquals(0, new BigDecimal("150").compareTo(draft.getSelections().get(0).quantity()));
    }
    @Test
    void cannotCompleteAnEmptyDraftAndCancellationDiscardsIt() {
        assertTrue(ui.buildDraft(new Scanner("Bowl\n1\n2\n0\n")).isEmpty());
        verifyNoInteractions(catalog);
    }
    @Test
    void separateInvocationsCannotLeakAnotherUsersDraft() {
        when(catalog.chooseFood(any())).thenReturn(food());
        var first = ui.buildDraft(new Scanner("First\n1\n1\n1\n1\n2\n")).orElseThrow();
        assertTrue(ui.buildDraft(new Scanner("Second\n2\n2\n0\n")).isEmpty());
        assertEquals("First", first.getName().getValue());
        assertEquals(1, first.getIngredients().size());
    }
    @Test
    void invalidServingCountDoesNotSearchAndCancelledFoodDoesNotAddARow() {
        assertTrue(ui.buildDraft(new Scanner("Bowl\n-1\n")).isEmpty());
        verifyNoInteractions(catalog);
        when(catalog.chooseFood(any())).thenReturn(null);
        assertTrue(ui.buildDraft(new Scanner("Bowl\n1\n1\n0\n")).isEmpty());
    }
    @Test
    void exposesImmutableSelectionsWithoutAcceptingClientNutrientTotals() {
        var draft = new RecipeDraft("Bowl", BigDecimal.ONE);
        draft.addIngredient(food(), new IngredientQuantity(BigDecimal.ONE, IngredientQuantity.Unit.SERVING));
        assertThrows(UnsupportedOperationException.class, () -> draft.getIngredients().clear());
        assertThrows(UnsupportedOperationException.class, () -> draft.getSelections().clear());
        assertEquals(1, draft.getSelections().size());
    }

    private com.pedropacheco.nutrition.backend.customRecipe.dto.CustomRecipeResponseDTO savedRecipe() {
        var nutrients = new com.pedropacheco.nutrition.backend.customRecipe.dto.RecipeNutrientsDTO(
                new BigDecimal("200"), BigDecimal.TEN, new BigDecimal("30"), new BigDecimal("5"));
        return new com.pedropacheco.nutrition.backend.customRecipe.dto.CustomRecipeResponseDTO(
                "saved-recipe", "Bowl", BigDecimal.ONE, java.util.List.of(), nutrients, nutrients);
    }

    @Test
    void confirmsSavingWithTheSignedInOwnerAndSelectedReferences() {
        when(catalog.chooseFood(any())).thenReturn(food());
        when(service.createRecipe(eq("owner"), any())).thenReturn(savedRecipe());

        ui.show("owner", new Scanner("Bowl\n1\n1\n1\n1\n2\ny\n"));

        var request = org.mockito.ArgumentCaptor.forClass(
                com.pedropacheco.nutrition.backend.customRecipe.dto.CreateRecipeRequestDTO.class);
        verify(service).createRecipe(eq("owner"), request.capture());
        assertEquals("Bowl", request.getValue().name());
        assertEquals("openfoodfacts:12345678", request.getValue().ingredients().get(0).foodId());
        assertEquals(1, request.getValue().ingredients().size());
    }

    @Test
    void decliningConfirmationDoesNotSave() {
        when(catalog.chooseFood(any())).thenReturn(food());
        ui.show("owner", new Scanner("Bowl\n1\n1\n1\n1\n2\nn\n"));
        verifyNoInteractions(service);
    }

    @Test
    void failedSaveRetainsTheDraftForRetryWithoutRepeatingFoodSearch() {
        when(catalog.chooseFood(any())).thenReturn(food());
        when(service.createRecipe(eq("owner"), any()))
                .thenThrow(new IllegalStateException("temporary persistence failure"))
                .thenReturn(savedRecipe());
        ui.show("owner", new Scanner("Bowl\n1\n1\n1\n1\n2\ny\ny\n"));
        verify(service, times(2)).createRecipe(eq("owner"), any());
        verify(catalog, times(1)).chooseFood(any());
    }
}
