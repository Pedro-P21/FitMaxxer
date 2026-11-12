package com.pedropacheco.nutrition.backend.customRecipe.controller;

import com.pedropacheco.nutrition.backend.cli.TerminalRunner;
import com.pedropacheco.nutrition.backend.customRecipe.domain.RecipeId;
import com.pedropacheco.nutrition.backend.customRecipe.dto.CustomRecipeMapper;
import com.pedropacheco.nutrition.backend.customRecipe.repository.CustomRecipeRepository;
import com.pedropacheco.nutrition.backend.foodCatalog.domain.FoodItem;
import com.pedropacheco.nutrition.backend.foodCatalog.domain.NutrientComposition;
import com.pedropacheco.nutrition.backend.foodCatalog.factory.FoodItemFactory;
import com.pedropacheco.nutrition.backend.foodCatalog.service.FoodCatalogProvider;
import com.pedropacheco.nutrition.backend.foodCatalog.service.FoodCatalogSearchService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:us006creation;DB_CLOSE_DELAY=-1",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class CustomRecipeCreationIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired CustomRecipeRepository repository;
    @Autowired CustomRecipeMapper mapper;
    @Autowired FoodCatalogSearchService search;
    @Autowired CacheManager cacheManager;
    @Autowired EntityManager entityManager;
    @Autowired PlatformTransactionManager transactionManager;
    @MockitoBean FoodCatalogProvider provider;
    @MockitoBean TerminalRunner terminalRunner;

    private static final String REQUEST = """
            {"name":"Homemade bowl","numberOfServings":3,"ingredients":[
              {"foodId":"openfoodfacts:12345678","quantity":150,"quantityUnit":"GRAM"},
              {"foodId":"openfoodfacts:87654321","quantity":3,"quantityUnit":"SERVING"}
            ]}
            """;

    @BeforeEach
    void resetState() {
        repository.deleteAll();
        cacheManager.getCache("foodSearchCache").clear();
        cacheManager.getCache("foodDetailsCache").clear();
        reset(provider);
    }

    private FoodItem firstFood(String brand) {
        return new FoodItemFactory().createFoodItem("openfoodfacts:12345678", "Food A", brand, "12345678",
                new BigDecimal("200"), BigDecimal.TEN, new BigDecimal("30"), new BigDecimal("5"),
                NutrientComposition.Basis.PER_100_GRAMS, new BigDecimal("100"), "g");
    }
    private FoodItem secondFood() {
        return new FoodItemFactory().createFoodItem("openfoodfacts:87654321", "Food B", null, "87654321",
                new BigDecimal("100"), new BigDecimal("5"), BigDecimal.TEN, new BigDecimal("4"),
                NutrientComposition.Basis.PER_SERVING, BigDecimal.ONE, "serving");
    }
    private void seedSearch() {
        when(provider.search("bowl")).thenReturn(List.of(firstFood(null), secondFood()));
        search.search("bowl");
    }
    private void assertNoPartialAggregate() {
        assertEquals(0, repository.count());
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            assertEquals(0L, entityManager.createQuery("select count(i) from RecipeIngredient i", Long.class).getSingleResult());
            assertEquals(0L, entityManager.createQuery("select count(m) from RecipeMacros m", Long.class).getSingleResult());
        });
    }

    @Test
    void savesTrustedSnapshotsAndAllNutritionWithoutASecondProviderQuery() throws Exception {
        seedSearch();
        cacheManager.getCache("foodSearchCache").clear();
        var result = mvc.perform(post("/api/recipes").with(jwt().jwt(token -> token.subject("owner")))
                        .contentType(MediaType.APPLICATION_JSON).content(REQUEST))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("/api/recipes/")))
                .andExpect(jsonPath("$.name").value("Homemade bowl"))
                .andExpect(jsonPath("$.ingredients.length()").value(2))
                .andExpect(jsonPath("$.ingredients[0].foodName").value("Food A"))
                .andExpect(jsonPath("$.ingredients[0].quantity").value(150))
                .andExpect(jsonPath("$.totalNutrition.caloriesKcal").value(600))
                .andExpect(jsonPath("$.totalNutrition.proteinGrams").value(30))
                .andExpect(jsonPath("$.totalNutrition.carbohydrateGrams").value(75))
                .andExpect(jsonPath("$.totalNutrition.fatGrams").value(19.5))
                .andExpect(jsonPath("$.perServingNutrition.caloriesKcal").value(200))
                .andExpect(jsonPath("$.perServingNutrition.proteinGrams").value(10))
                .andExpect(jsonPath("$.perServingNutrition.carbohydrateGrams").value(25))
                .andExpect(jsonPath("$.perServingNutrition.fatGrams").value(6.5))
                .andReturn();
        String id = result.getResponse().getHeader("Location").substring("/api/recipes/".length());
        cacheManager.getCache("foodDetailsCache").clear();
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            entityManager.clear();
            var saved = repository.findById(new RecipeId(id)).orElseThrow();
            assertEquals("owner", saved.getUserId().getValue());
            assertEquals(2, saved.getIngredients().size());
            assertEquals("Food A", saved.getIngredients().get(0).getSnapshot().getFoodName());
            assertEquals("Food B", saved.getIngredients().get(1).getSnapshot().getFoodName());
            assertEquals(0, new BigDecimal("600").compareTo(mapper.toResponseDTO(saved).totalNutrition().caloriesKcal()));
        });
        verify(provider).search("bowl");
        verifyNoMoreInteractions(provider);
    }

    @Test
    void clientCannotOverrideOwnerOrNutrition() throws Exception {
        seedSearch();
        String tampered = REQUEST.replace("\"name\":", "\"userId\":\"someone-else\",\"totalNutrition\":{\"caloriesKcal\":1},\"name\":")
                .replace("\"quantity\":150", "\"caloriesKcal\":1,\"quantity\":150");
        mvc.perform(post("/api/recipes").with(jwt().jwt(token -> token.subject("owner")))
                        .contentType(MediaType.APPLICATION_JSON).content(tampered))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalNutrition.caloriesKcal").value(600));
        assertEquals("owner", repository.findAll().get(0).getUserId().getValue());
    }

    @Test
    void missingDetailsFallBackBeforeStartingTheWriteTransaction() throws Exception {
        when(provider.search("12345678")).thenAnswer(call -> {
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
            return List.of(firstFood(null));
        });
        String request = """
                {"name":"Oats","numberOfServings":2.5,"ingredients":[
                  {"foodId":"openfoodfacts:12345678","quantity":100,"quantityUnit":"GRAM"},
                  {"foodId":"openfoodfacts:12345678","quantity":50,"quantityUnit":"GRAM"}]}
                """;
        mvc.perform(post("/api/recipes").with(jwt().jwt(token -> token.subject("owner")))
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalNutrition.caloriesKcal").value(300))
                .andExpect(jsonPath("$.perServingNutrition.caloriesKcal").value(120));
        verify(provider).search("12345678");
        verifyNoMoreInteractions(provider);
    }

    @Test
    void requiresAuthentication() throws Exception {
        mvc.perform(post("/api/recipes").contentType(MediaType.APPLICATION_JSON).content(REQUEST))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(provider);
        assertNoPartialAggregate();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{}",
        "{\"name\":\"Bowl\",\"numberOfServings\":0,\"ingredients\":[]}",
        "{\"name\":\"Bowl\",\"numberOfServings\":1,\"ingredients\":[]}",
        "{\"name\":\"Bowl\",\"numberOfServings\":1,\"ingredients\":[null]}",
        "{\"name\":\"Bowl\",\"numberOfServings\":1,\"ingredients\":[{\"foodId\":\"x\",\"quantity\":0,\"quantityUnit\":\"GRAM\"}]}",
        "{\"name\":\"Bowl\",\"numberOfServings\":1,\"ingredients\":[{\"foodId\":\"x\",\"quantity\":1,\"quantityUnit\":\"NAMED\"}]}",
        "{\"name\":\"Bowl\",\"numberOfServings\":1,\"ingredients\":[{\"foodId\":\"x\",\"quantity\":1}]}"
    })
    void rejectsInvalidRequestsBeforeCatalogLookup(String body) throws Exception {
        mvc.perform(post("/api/recipes").with(jwt()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(provider);
        assertNoPartialAggregate();
    }

    @Test
    void unknownIngredientCannotLeaveAPartialRecipe() throws Exception {
        seedSearch();
        when(provider.search("99999999")).thenReturn(List.of());
        mvc.perform(post("/api/recipes").with(jwt()).contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST.replace("87654321", "99999999")))
                .andExpect(status().isNotFound());
        assertNoPartialAggregate();
    }

    @Test
    void unavailableCatalogCannotLeaveAPartialRecipe() throws Exception {
        when(provider.search("12345678")).thenThrow(new IllegalStateException("offline"));
        mvc.perform(post("/api/recipes").with(jwt()).contentType(MediaType.APPLICATION_JSON).content(REQUEST))
                .andExpect(status().isServiceUnavailable());
        assertNoPartialAggregate();
    }

    @Test
    void incompatibleUnitCannotLeaveAPartialRecipe() throws Exception {
        seedSearch();
        mvc.perform(post("/api/recipes").with(jwt()).contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST.replace("\"GRAM\"", "\"MILLILITER\"")))
                .andExpect(status().isBadRequest());
        assertNoPartialAggregate();
    }

    @Test
    void databaseFailureRollsBackRootIngredientsAndMacros() throws Exception {
        when(provider.search("bowl")).thenReturn(List.of(firstFood("X".repeat(300)), secondFood()));
        search.search("bowl");
        mvc.perform(post("/api/recipes").with(jwt()).contentType(MediaType.APPLICATION_JSON).content(REQUEST))
                .andExpect(status().isConflict());
        assertNoPartialAggregate();
    }
}
