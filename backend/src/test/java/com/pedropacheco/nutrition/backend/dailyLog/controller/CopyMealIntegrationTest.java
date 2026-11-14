package com.pedropacheco.nutrition.backend.dailyLog.controller;

import com.pedropacheco.nutrition.backend.cli.TerminalRunner;
import com.pedropacheco.nutrition.backend.customRecipe.service.CustomRecipeService;
import com.pedropacheco.nutrition.backend.dailyLog.domain.*;
import com.pedropacheco.nutrition.backend.dailyLog.dto.*;
import com.pedropacheco.nutrition.backend.dailyLog.repository.DailyLogRepository;
import com.pedropacheco.nutrition.backend.dailyLog.service.*;
import com.pedropacheco.nutrition.backend.foodCatalog.domain.NutrientComposition.Basis;
import com.pedropacheco.nutrition.backend.foodCatalog.service.FoodCatalogQueryService;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:meal-copy;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.open-in-view=false",
        "daily-log.history.max-lookback-years=2"})
@AutoConfigureMockMvc
class CopyMealIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired DailyLogRepository logs;
    @Autowired UserProfileRepository profiles;
    @Autowired DailyLogApplicationService service;
    @Autowired LogHistoryService history;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager transactions;
    @MockitoSpyBean DailyLogMapper mapper;
    @MockitoBean TerminalRunner terminalRunner;
    @MockitoBean Clock clock;
    @MockitoBean FoodCatalogQueryService catalog;
    @MockitoBean CustomRecipeService recipes;
    private final LocalDate today = LocalDate.of(2026, 11, 15);
    private final LocalDate yesterday = today.minusDays(1);

    @BeforeEach
    void setUp() {
        when(clock.instant()).thenReturn(Instant.parse("2026-11-14T16:00:00Z"));
        when(clock.getZone()).thenReturn(ZoneId.of("Asia/Seoul"));
        logs.deleteAll();
        profiles.deleteAll();
        profiles.save(new UserNutritionalProfile(new UserId("owner"),
                PhysicalData.validateAndCreate(30, 70, 175, "MALE", null), ActivityLevel.of("SEDENTARY"),
                new NutritionalGoal(2200, 2200, new MacroBreakdown(150, 220, 80))));
    }

    private MealItem item(String name, Basis basis, PortionQuantity quantity) {
        return new MealItem(new FoodSnapshot(name, "Brand", "12345678", new BigDecimal("100.1234567890"),
                BigDecimal.TEN, new BigDecimal("15"), new BigDecimal("2"), basis,
                basis == Basis.PER_SERVING ? BigDecimal.ONE : new BigDecimal("100"),
                basis == Basis.PER_SERVING ? "serving" : basis == Basis.PER_100_GRAMS ? "g" : "ml"), quantity);
    }

    private void source(String owner, LocalDate date) {
        var log = new DailyLog(new UserId(owner), date, 1L, new DailyGoalSnapshot(1400, 90, 150, 50));
        log.addMealItem(new MealType("Breakfast"), item("Food", Basis.PER_100_GRAMS,
                PortionQuantity.grams(new BigDecimal("150"))));
        log.addMealItem(new MealType("Breakfast"), item("Soup", Basis.PER_100_MILLILITERS,
                PortionQuantity.named("bowl", new BigDecimal("1.5"), new BigDecimal("200"), PortionQuantity.Unit.MILLILITER)));
        log.addMealItem(new MealType("Breakfast"), item("Recorded recipe", Basis.PER_SERVING,
                PortionQuantity.servings(new BigDecimal("0.5"))));
        log.addMealItem(new MealType("Dinner"), item("Not selected", Basis.PER_SERVING, PortionQuantity.servings(BigDecimal.ONE)));
        logs.saveAndFlush(log);
    }

    private void existingToday() {
        var log = new DailyLog(new UserId("owner"), today, 1L, new DailyGoalSnapshot(1800, 100, 200, 60));
        log.addMealItem(new MealType("Snacks"), item("Already logged", Basis.PER_SERVING, PortionQuantity.servings(BigDecimal.ONE)));
        logs.saveAndFlush(log);
    }

    private CopyMealRequestDTO request(LocalDate date, String destination) {
        return new CopyMealRequestDTO(date, "Breakfast", destination);
    }

    private String body(LocalDate date, String source, String destination) {
        return "{\"sourceDate\":\"" + date + "\",\"sourceMealType\":\"" + source
                + "\",\"destinationMealType\":\"" + destination + "\"}";
    }

    private DailyLogResponseDTO past() { return history.getPastHistory("owner", 11, 2026).logs().getFirst(); }
    private long count(String table) { return jdbc.queryForObject("select count(*) from " + table, Long.class); }
    private void number(BigDecimal expected, BigDecimal actual) { assertEquals(0, expected.compareTo(actual)); }

    @Test
    void appendsEntireMealAndAllowsRepeatedCopiesWithNewIdsAndRecordedValues() throws Exception {
        source("owner", yesterday);
        var original = past();
        var sourceMeal = original.meals().getFirst();
        existingToday();
        var before = service.getToday("owner");
        profiles.deleteAll(); // Existing log must retain its own targets without profile lookup.
        mvc.perform(post("/api/daily-log/today/meal-copies").with(jwt().jwt(t -> t.subject("owner")))
                        .contentType(MediaType.APPLICATION_JSON).content(body(yesterday, "Breakfast", "Snacks")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value(today.toString()))
                .andExpect(jsonPath("$.meals[3].items.length()").value(4))
                .andExpect(jsonPath("$.nutritionSummary.targets.caloriesKcal").value(1800));
        var copied = service.getToday("owner");
        var entries = copied.meals().get(3).items();
        assertEquals(before.meals().get(3).items().getFirst(), entries.getFirst());
        for (int index = 0; index < 3; index++) {
            var from = sourceMeal.items().get(index);
            var to = entries.get(index + 1);
            assertNotEquals(from.itemId(), to.itemId());
            assertEquals(from.foodName(), to.foodName());
            assertEquals(from.brand(), to.brand());
            assertEquals(from.barcode(), to.barcode());
            assertEquals(from.nutrientBasis(), to.nutrientBasis());
            assertEquals(from.quantityUnit(), to.quantityUnit());
            number(from.quantity(), to.quantity());
            number(from.caloriesKcal(), to.caloriesKcal());
        }
        var expected = sourceMeal.consumed();
        var prior = before.nutritionSummary().consumed();
        var actual = copied.nutritionSummary().consumed();
        number(prior.caloriesKcal().add(expected.caloriesKcal()), actual.caloriesKcal());
        number(prior.proteinGrams().add(expected.proteinGrams()), actual.proteinGrams());
        number(prior.carbohydrateGrams().add(expected.carbohydrateGrams()), actual.carbohydrateGrams());
        number(prior.fatGrams().add(expected.fatGrams()), actual.fatGrams());
        number(new BigDecimal("1800").subtract(actual.caloriesKcal()), copied.nutritionSummary().remaining().caloriesKcal());
        service.copyMeal("owner", request(yesterday, "Snacks"));
        var twice = service.getToday("owner").meals().get(3).items();
        assertEquals(7, twice.size());
        assertEquals(7, twice.stream().map(MealItemResponseDTO::itemId).distinct().count());
        assertEquals(1, jdbc.queryForObject("select count(*) from daily_log_meal where daily_log_id = "
                + "(select daily_log_id from daily_log where log_date = ?) and meal_type = 'SNACKS'", Long.class, today));
        assertEquals(original, past());
        verifyNoInteractions(catalog, recipes);
    }

    @Test
    void createsFirstLogAndMissingDestinationUsingCurrentTargetsAndIndependentValues() {
        source("owner", yesterday);
        var original = past();
        var response = service.copyMeal("owner", request(yesterday, "Lunch"));
        assertEquals(3, response.meals().get(1).items().size());
        assertEquals(2200, response.nutritionSummary().targets().caloriesKcal().intValue());
        assertEquals(2, logs.count());
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            em.flush();
            em.clear();
            var from = logs.findByUserIdAndDate("owner", yesterday).orElseThrow().getMealByType("Breakfast").getItems();
            var to = logs.findByUserIdAndDate("owner", today).orElseThrow().getMealByType("Lunch").getItems();
            for (int index = 0; index < from.size(); index++) {
                assertNotEquals(from.get(index).getId(), to.get(index).getId());
                assertEquals(from.get(index).getFoodSnapshot(), to.get(index).getFoodSnapshot());
                assertEquals(from.get(index).getPortionQuantity(), to.get(index).getPortionQuantity());
                assertNotSame(from.get(index).getFoodSnapshot(), to.get(index).getFoodSnapshot());
                assertNotSame(from.get(index).getPortionQuantity(), to.get(index).getPortionQuantity());
            }
        });
        Long copyId = response.meals().get(1).items().getFirst().itemId();
        service.changeItemQuantity("owner", copyId, new ChangeMealItemQuantityRequestDTO(new BigDecimal("75")));
        service.moveItem("owner", copyId, new MoveMealItemRequestDTO("Dinner"));
        service.deleteItem("owner", copyId);
        assertEquals(original, past());
        verifyNoInteractions(catalog, recipes);
    }

    @Test
    void sourceDatesMatchTheConfiguredHistoryWindowAndServerTimezone() throws Exception {
        LocalDate boundary = LocalDate.of(2024, 11, 1);
        source("owner", boundary);
        assertEquals(1, history.getPastHistory("owner", 11, 2024).logs().size());
        service.copyMeal("owner", request(boundary, "Breakfast"));
        for (LocalDate invalid : List.of(today, today.plusDays(1), LocalDate.of(2024, 10, 31))) {
            mvc.perform(post("/api/daily-log/today/meal-copies").with(jwt().jwt(t -> t.subject("owner")))
                            .contentType(MediaType.APPLICATION_JSON).content(body(invalid, "Breakfast", "Lunch")))
                    .andExpect(status().isBadRequest());
        }
        assertThrows(IllegalArgumentException.class, () -> history.getPastHistory("owner", 10, 2024));
        assertEquals(3, service.getToday("owner").meals().getFirst().items().size());
    }

    @Test
    void rejectsForeignMissingAndEmptySourcesWithoutCreatingToday() throws Exception {
        source("other", yesterday);
        for (LocalDate date : List.of(yesterday, yesterday.minusDays(1)))
            mvc.perform(post("/api/daily-log/today/meal-copies").with(jwt().jwt(t -> t.subject("owner")))
                            .contentType(MediaType.APPLICATION_JSON).content(body(date, "Breakfast", "Lunch")))
                    .andExpect(status().isNotFound());
        source("owner", yesterday);
        mvc.perform(post("/api/daily-log/today/meal-copies").with(jwt().jwt(t -> t.subject("owner")))
                        .contentType(MediaType.APPLICATION_JSON).content(body(yesterday, "Lunch", "Snacks")))
                .andExpect(status().isNotFound());
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            var source = logs.findByUserIdAndDate("owner", yesterday).orElseThrow();
            for (var item : List.copyOf(source.getMealByType("Breakfast").getItems()))
                logs.deleteRemovedItem(source.removeMealItem(item.getId()));
        });
        mvc.perform(post("/api/daily-log/today/meal-copies").with(jwt().jwt(t -> t.subject("owner")))
                        .contentType(MediaType.APPLICATION_JSON).content(body(yesterday, "Breakfast", "Lunch")))
                .andExpect(status().isBadRequest());
        assertTrue(logs.findByUserIdAndDate("owner", today).isEmpty());
    }

    @Test
    void rejectsInvalidPayloadsAndUnauthenticatedRequests() throws Exception {
        for (String payload : List.of("{}", "{\"sourceDate\":\"bad-date\"}",
                body(yesterday, "Brunch", "Lunch"), body(yesterday, "Breakfast", "Brunch")))
            mvc.perform(post("/api/daily-log/today/meal-copies").with(jwt().jwt(t -> t.subject("owner")))
                            .contentType(MediaType.APPLICATION_JSON).content(payload))
                    .andExpect(status().isBadRequest());
        mvc.perform(post("/api/daily-log/today/meal-copies").contentType(MediaType.APPLICATION_JSON)
                        .content(body(yesterday, "Breakfast", "Lunch"))).andExpect(status().isUnauthorized());
        assertEquals(0, logs.count());
    }

    @Test
    void missingProfileDoesNotLeaveAnEmptyNewLog() throws Exception {
        source("owner", yesterday);
        profiles.deleteAll();
        mvc.perform(post("/api/daily-log/today/meal-copies").with(jwt().jwt(t -> t.subject("owner")))
                        .contentType(MediaType.APPLICATION_JSON).content(body(yesterday, "Breakfast", "Lunch")))
                .andExpect(status().isConflict());
        assertEquals(1, logs.count());
        assertEquals(4, count("daily_log_meal_item"));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void failureAfterSaveRollsBackCopiedItemsAndAnyNewLog(boolean existing) {
        source("owner", yesterday);
        var original = past();
        if (existing) existingToday();
        long logCount = logs.count(), mealCount = count("daily_log_meal"), itemCount = count("daily_log_meal_item");
        doAnswer(invocation -> {
            em.flush(); // Exercise real SQL writes before the failure.
            throw new IllegalStateException("Simulated failure after saving the copy");
        }).when(mapper).toResponseDTO(any(), any());
        assertThrows(IllegalStateException.class, () -> service.copyMeal("owner", request(yesterday, "Snacks")));
        reset(mapper);
        assertEquals(logCount, logs.count());
        assertEquals(mealCount, count("daily_log_meal"));
        assertEquals(itemCount, count("daily_log_meal_item"));
        assertEquals(original, past());
        if (existing) assertEquals(1, service.getToday("owner").meals().get(3).items().size());
        else assertTrue(logs.findByUserIdAndDate("owner", today).isEmpty());
    }
}
