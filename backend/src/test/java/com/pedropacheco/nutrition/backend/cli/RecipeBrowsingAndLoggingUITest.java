package com.pedropacheco.nutrition.backend.cli;

import com.pedropacheco.nutrition.backend.customRecipe.dto.*;
import com.pedropacheco.nutrition.backend.customRecipe.service.CustomRecipeService;
import com.pedropacheco.nutrition.backend.dailyLog.dto.*;
import com.pedropacheco.nutrition.backend.dailyLog.service.DailyLogApplicationService;
import com.pedropacheco.nutrition.backend.foodCatalog.dto.*;
import com.pedropacheco.nutrition.backend.foodCatalog.service.FoodCatalogSearchService;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RecipeBrowsingAndLoggingUITest {
    private CustomRecipeResponseDTO recipe(String id) {
        var nutrients = new RecipeNutrientsDTO(new BigDecimal("200"), BigDecimal.TEN,
                new BigDecimal("25"), new BigDecimal("6.5"));
        return new CustomRecipeResponseDTO(id, "Bowl", new BigDecimal("3"), List.of(), nutrients, nutrients);
    }
    private FoodItemDTO food() {
        return new FoodItemDTO("openfoodfacts:12345678", "Oats", null, null, BigDecimal.ONE, "serving",
                new BigDecimal("100"), BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ONE, "PER_SERVING");
    }
    private DailyLogResponseDTO today() {
        var zero = new NutrientValuesDTO(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        return new DailyLogResponseDTO(LocalDate.of(2026, 11, 10), List.of(), new DailyNutritionSummaryDTO(zero, zero, zero));
    }

    @Test
    void us003PersonalChoiceBrowsesForSignedInUserWithoutOpenCatalogSearch() {
        var catalog = mock(FoodCatalogSearchService.class);
        var personal = mock(PersonalRecipesUI.class);
        new FoodCatalogSearchUI(catalog, personal).show("owner", new Scanner("2\n0\n"));
        verify(personal).chooseRecipe(eq("owner"), any());
        verifyNoInteractions(catalog);
    }
    @Test
    void us003OpenCatalogChoiceKeepsKeywordSearchWorking() {
        var catalog = mock(FoodCatalogSearchService.class);
        var personal = mock(PersonalRecipesUI.class);
        when(catalog.search("oats")).thenReturn(new FoodCatalogResponseDTO(List.of(food()), null));
        new FoodCatalogSearchUI(catalog, personal).show("owner", new Scanner("1\noats\n1\nn\n0\n"));
        verify(catalog).search("oats");
        verifyNoInteractions(personal);
    }
    @Test
    void us004PersonalChoiceLogsFractionalRecipeServingsForSignedInUser() {
        var logging = mock(DailyLogApplicationService.class);
        var catalog = mock(FoodCatalogSearchService.class);
        var personal = mock(PersonalRecipesUI.class);
        when(logging.getToday("owner")).thenReturn(today());
        when(personal.chooseRecipe(eq("owner"), any())).thenReturn(recipe("recipe-id"));
        when(logging.addRecipeMealItem(eq("owner"), any())).thenReturn(today());
        new DailyLogUI(logging, catalog, personal).show("owner", new Scanner("1\n2\n2\n1.5\n0\n"));
        verify(logging).addRecipeMealItem("owner", new AddRecipeMealItemRequestDTO("recipe-id", "Lunch", new BigDecimal("1.5")));
        verify(logging, never()).addMealItem(any(), any());
        verifyNoInteractions(catalog);
    }
    @Test
    void us004OpenCatalogChoiceKeepsFoodLoggingWorking() {
        var logging = mock(DailyLogApplicationService.class);
        var catalog = mock(FoodCatalogSearchService.class);
        var personal = mock(PersonalRecipesUI.class);
        when(logging.getToday("owner")).thenReturn(today());
        when(catalog.search("oats")).thenReturn(new FoodCatalogResponseDTO(List.of(food()), null));
        when(logging.addMealItem(eq("owner"), any())).thenReturn(today());
        new DailyLogUI(logging, catalog, personal).show("owner", new Scanner("1\n1\noats\n1\n1\n2\n0\n"));
        verify(logging).addMealItem(eq("owner"), any(AddMealItemRequestDTO.class));
        verify(logging, never()).addRecipeMealItem(any(), any());
        verifyNoInteractions(personal);
    }
    @Test
    void us004EmptyOrCancelledRecipeSelectionDoesNotLog() {
        var logging = mock(DailyLogApplicationService.class);
        var catalog = mock(FoodCatalogSearchService.class);
        var personal = mock(PersonalRecipesUI.class);
        when(logging.getToday("owner")).thenReturn(today());
        new DailyLogUI(logging, catalog, personal).show("owner", new Scanner("1\n2\n0\n"));
        verify(logging, never()).addRecipeMealItem(any(), any());
        verifyNoInteractions(catalog);
    }
    @Test
    void personalRecipePickerPagesAndLoadsOwnedDetails() {
        var service = mock(CustomRecipeService.class);
        var detail = recipe("second");
        var summary = new RecipeSummaryDTO(detail.recipeId(), detail.name(), detail.numberOfServings(),
                detail.totalNutrition(), detail.perServingNutrition());
        when(service.listRecipes("owner", 0, 20)).thenReturn(new RecipePageResponseDTO(List.of(summary), 0, 20, 21, 2));
        when(service.listRecipes("owner", 1, 20)).thenReturn(new RecipePageResponseDTO(List.of(summary), 1, 20, 21, 2));
        when(service.getRecipe("owner", "second")).thenReturn(detail);
        assertEquals(detail, new PersonalRecipesUI(service).chooseRecipe("owner", new Scanner("n\n1\n")));
        verify(service).listRecipes("owner", 1, 20);
        verify(service).getRecipe("owner", "second");
    }
    @Test
    void emptyPersonalListDoesNotRequestDetailsOrInput() {
        var service = mock(CustomRecipeService.class);
        when(service.listRecipes("owner", 0, 20)).thenReturn(new RecipePageResponseDTO(List.of(), 0, 20, 0, 0));
        assertNull(new PersonalRecipesUI(service).chooseRecipe("owner", new Scanner("")));
        verify(service, never()).getRecipe(any(), any());
    }
}
