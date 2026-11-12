package com.pedropacheco.nutrition.backend.customRecipe.controller;

import com.pedropacheco.nutrition.backend.cli.TerminalRunner;
import com.pedropacheco.nutrition.backend.customRecipe.domain.*;
import com.pedropacheco.nutrition.backend.customRecipe.repository.CustomRecipeRepository;
import com.pedropacheco.nutrition.backend.dailyLog.dto.AddRecipeMealItemRequestDTO;
import com.pedropacheco.nutrition.backend.dailyLog.repository.DailyLogRepository;
import com.pedropacheco.nutrition.backend.dailyLog.service.DailyLogApplicationService;
import com.pedropacheco.nutrition.backend.foodCatalog.service.FoodCatalogProvider;
import com.pedropacheco.nutrition.backend.userAuth.domain.UserId;
import com.pedropacheco.nutrition.backend.userNutritionalProfile.domain.*;
import com.pedropacheco.nutrition.backend.userNutritionalProfile.repository.UserProfileRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:us006logging;DB_CLOSE_DELAY=-1",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.open-in-view=false"
})
@AutoConfigureMockMvc
class RecipeBrowsingAndLoggingIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired CustomRecipeRepository recipes;
    @Autowired DailyLogRepository logs;
    @Autowired UserProfileRepository profiles;
    @Autowired DailyLogApplicationService logging;
    @Autowired EntityManager entityManager;
    @Autowired PlatformTransactionManager transactionManager;
    @MockitoBean TerminalRunner terminalRunner;
    @MockitoBean FoodCatalogProvider provider;
    @MockitoBean Clock clock;

    @BeforeEach
    void setUp() {
        logs.deleteAll();
        recipes.deleteAll();
        profiles.deleteAll();
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        when(clock.instant()).thenReturn(Instant.parse("2026-11-10T04:00:00Z"));
    }
    private void profile() {
        profiles.save(new UserNutritionalProfile(new UserId("owner"),
                PhysicalData.validateAndCreate(30, 70, 175, "MALE", null), ActivityLevel.of("SEDENTARY"),
                new NutritionalGoal(2200, 2200, new MacroBreakdown(150, 220, 80))));
    }
    private CustomRecipe recipe(String owner, String name, String calories, String servings) {
        var snapshot = new IngredientSnapshot("Ingredient", null, null, new BigDecimal(calories),
                BigDecimal.TEN, new BigDecimal("25"), new BigDecimal("6.5"),
                IngredientSnapshot.Basis.PER_SERVING, BigDecimal.ONE, "serving");
        return recipes.saveAndFlush(new CustomRecipe(new UserId(owner), new RecipeName(name), new BigDecimal(servings),
                List.of(new RecipeIngredient(snapshot, new IngredientQuantity(new BigDecimal("3"), IngredientQuantity.Unit.SERVING)))));
    }
    private String body(String id, String quantity) {
        return "{\"recipeId\":\"" + id + "\",\"mealType\":\"Lunch\",\"quantity\":" + quantity + "}";
    }
    private void number(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }

    @Test
    void listsOnlyOwnersRecipesWithPaginationAndReturnsSavedDetailsWithoutCatalogCalls() throws Exception {
        recipe("owner", "B Bowl", "200", "3");
        var a = recipe("owner", "A Bowl", "200", "3");
        recipe("other", "Private", "900", "1");
        mvc.perform(get("/api/recipes").param("size", "1").with(jwt().jwt(t -> t.subject("owner"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].name").value("A Bowl"))
                .andExpect(jsonPath("$.totalElements").value(2)).andExpect(jsonPath("$.totalPages").value(2));
        mvc.perform(get("/api/recipes").param("page", "1").param("size", "1").with(jwt().jwt(t -> t.subject("owner"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].name").value("B Bowl"));
        mvc.perform(get("/api/recipes/" + a.getId().getValue()).with(jwt().jwt(t -> t.subject("owner"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.ingredients[0].foodName").value("Ingredient"))
                .andExpect(jsonPath("$.totalNutrition.caloriesKcal").value(600))
                .andExpect(jsonPath("$.perServingNutrition.caloriesKcal").value(200));
        mvc.perform(get("/api/recipes").with(jwt().jwt(t -> t.subject("empty"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
        verifyNoInteractions(provider);
    }

    @Test
    void foreignAndMissingRecipesHaveTheSameNotFoundResponseForViewingAndLogging() throws Exception {
        var foreign = recipe("other", "Private", "200", "3");
        for (String id : List.of(foreign.getId().getValue(), "missing")) {
            mvc.perform(get("/api/recipes/" + id).with(jwt().jwt(t -> t.subject("owner"))))
                    .andExpect(status().isNotFound()).andExpect(content().string("Recipe not found."));
            mvc.perform(post("/api/daily-log/today/recipe-items").with(jwt().jwt(t -> t.subject("owner")))
                            .contentType(MediaType.APPLICATION_JSON).content(body(id, "1")))
                    .andExpect(status().isNotFound()).andExpect(content().string("Recipe not found."));
        }
        assertEquals(0, logs.count());
        verifyNoInteractions(provider);
    }

    @Test
    void requiresAuthenticationForAllNewEndpoints() throws Exception {
        mvc.perform(get("/api/recipes")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/recipes/id")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/daily-log/today/recipe-items").contentType(MediaType.APPLICATION_JSON).content(body("id", "1")))
                .andExpect(status().isUnauthorized());
        assertEquals(0, logs.count());
    }

    @Test
    void logsFractionalServingsAndReusesTodayWithOneItemPerAdditionAndStoredGoals() throws Exception {
        profile();
        var id = recipe("owner", "Bowl", "200", "3").getId().getValue();
        mvc.perform(post("/api/daily-log/today/recipe-items").with(jwt().jwt(t -> t.subject("owner")))
                        .contentType(MediaType.APPLICATION_JSON).content(body(id, "1.5")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nutritionSummary.consumed.caloriesKcal").value(300))
                .andExpect(jsonPath("$.nutritionSummary.consumed.proteinGrams").value(15))
                .andExpect(jsonPath("$.nutritionSummary.consumed.carbohydrateGrams").value(37.5))
                .andExpect(jsonPath("$.nutritionSummary.consumed.fatGrams").value(9.75))
                .andExpect(jsonPath("$.nutritionSummary.targets.caloriesKcal").value(2200))
                .andExpect(jsonPath("$.nutritionSummary.remaining.caloriesKcal").value(1900));
        // Existing logs reuse their target snapshot even if no current profile can be loaded.
        profiles.deleteAll();
        logging.addRecipeMealItem("owner", new AddRecipeMealItemRequestDTO(id, "Lunch", new BigDecimal("0.5")));
        assertEquals(1, logs.count());
        var today = logging.getToday("owner");
        number("400", today.nutritionSummary().consumed().caloriesKcal());
        number("2200", today.nutritionSummary().targets().caloriesKcal());
        var lunch = today.meals().stream().filter(m -> m.type().equals("Lunch")).findFirst().orElseThrow();
        assertEquals(2, lunch.items().size());
        assertEquals("Bowl", lunch.items().get(0).foodName());
        assertEquals("SERVING", lunch.items().get(0).quantityUnit());
        verifyNoInteractions(provider);
    }

    @Test
    void recipeChangesAndDeletionDoNotRewriteTodayOrHistory() throws Exception {
        profile();
        var recipe = recipe("owner", "Original bowl", "200", "3");
        logging.addRecipeMealItem("owner", new AddRecipeMealItemRequestDTO(recipe.getId().getValue(), "Lunch", BigDecimal.ONE));
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            entityManager.createNativeQuery("update custom_recipe set recipe_name = 'Changed' where recipe_id = :id")
                    .setParameter("id", recipe.getId().getValue()).executeUpdate();
            entityManager.createNativeQuery("update recipe_macros set calories_kcal = 999 where id = :id")
                    .setParameter("id", recipe.getMacros().getId()).executeUpdate();
        });
        var today = logging.getToday("owner");
        number("200", today.nutritionSummary().consumed().caloriesKcal());
        recipes.deleteAll();
        today = logging.getToday("owner");
        number("200", today.nutritionSummary().consumed().caloriesKcal());
        assertEquals("Original bowl", today.meals().stream().flatMap(m -> m.items().stream()).findFirst().orElseThrow().foodName());
        when(clock.instant()).thenReturn(Instant.parse("2026-11-11T04:00:00Z"));
        mvc.perform(get("/api/daily-log/history").param("month", "11").param("year", "2026")
                        .with(jwt().jwt(t -> t.subject("owner"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.logs[0].nutritionSummary.consumed.caloriesKcal").value(200));
        verifyNoInteractions(provider);
    }

    @Test
    void fractionalPerServingValuesSurviveSnapshotPersistenceWithoutRoundingToTwoDecimals() {
        profile();
        var id = recipe("owner", "Thirds", "100", "9").getId().getValue();
        var added = logging.addRecipeMealItem("owner", new AddRecipeMealItemRequestDTO(id, "Lunch", BigDecimal.ONE));
        number("33.3333333333", added.nutritionSummary().consumed().caloriesKcal());
        number("33.3333333333", logging.getToday("owner").nutritionSummary().consumed().caloriesKcal());
        verifyNoInteractions(provider);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "null", "0.00001", "100000000"})
    void invalidRecipeQuantitiesDoNotCreateALog(String quantity) throws Exception {
        var id = recipe("owner", "Bowl", "200", "3").getId().getValue();
        mvc.perform(post("/api/daily-log/today/recipe-items").with(jwt().jwt(t -> t.subject("owner")))
                        .contentType(MediaType.APPLICATION_JSON).content(body(id, quantity)))
                .andExpect(status().isBadRequest());
        assertEquals(0, logs.count());
    }

    @Test
    void invalidMealAndMissingProfileCannotCreateALog() throws Exception {
        var id = recipe("owner", "Bowl", "200", "3").getId().getValue();
        mvc.perform(post("/api/daily-log/today/recipe-items").with(jwt().jwt(t -> t.subject("owner")))
                        .contentType(MediaType.APPLICATION_JSON).content(body(id, "1").replace("Lunch", "Brunch")))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/daily-log/today/recipe-items").with(jwt().jwt(t -> t.subject("owner")))
                        .contentType(MediaType.APPLICATION_JSON).content(body(id, "1")))
                .andExpect(status().isConflict());
        assertEquals(0, logs.count());
    }

    @ParameterizedTest
    @ValueSource(strings = {"page=-1&size=20", "page=0&size=0", "page=0&size=101", "page=2147483647&size=100"})
    void validatesPagination(String query) throws Exception {
        mvc.perform(get("/api/recipes?" + query).with(jwt())).andExpect(status().isBadRequest());
    }

    @Test
    void persistenceFailureRollsBackNewLogAndDoesNotChangeAnExistingLog() throws Exception {
        profile();
        var huge = recipe("owner", "Huge", "10000000000000000000", "0.0000000001").getId().getValue();
        mvc.perform(post("/api/daily-log/today/recipe-items").with(jwt().jwt(t -> t.subject("owner")))
                        .contentType(MediaType.APPLICATION_JSON).content(body(huge, "1")))
                .andExpect(status().isConflict());
        assertEquals(0, logs.count());
        var normal = recipe("owner", "Normal", "200", "3").getId().getValue();
        logging.addRecipeMealItem("owner", new AddRecipeMealItemRequestDTO(normal, "Lunch", BigDecimal.ONE));
        mvc.perform(post("/api/daily-log/today/recipe-items").with(jwt().jwt(t -> t.subject("owner")))
                        .contentType(MediaType.APPLICATION_JSON).content(body(huge, "1")))
                .andExpect(status().isConflict());
        assertEquals(1, logs.count());
        number("200", logging.getToday("owner").nutritionSummary().consumed().caloriesKcal());
    }
}
