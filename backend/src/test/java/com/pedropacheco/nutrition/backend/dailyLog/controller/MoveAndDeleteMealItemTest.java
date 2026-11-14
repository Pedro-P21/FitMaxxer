package com.pedropacheco.nutrition.backend.dailyLog.controller;

import com.pedropacheco.nutrition.backend.cli.TerminalRunner;
import com.pedropacheco.nutrition.backend.dailyLog.domain.*;
import com.pedropacheco.nutrition.backend.dailyLog.dto.*;
import com.pedropacheco.nutrition.backend.dailyLog.repository.DailyLogRepository;
import com.pedropacheco.nutrition.backend.dailyLog.service.DailyLogApplicationService;
import com.pedropacheco.nutrition.backend.foodCatalog.domain.NutrientComposition;
import com.pedropacheco.nutrition.backend.foodCatalog.service.FoodCatalogQueryService;
import com.pedropacheco.nutrition.backend.foodCatalog.dto.FoodCatalogDetailsDTO;
import com.pedropacheco.nutrition.backend.userAuth.domain.UserId;
import com.pedropacheco.nutrition.backend.userNutritionalProfile.domain.*;
import com.pedropacheco.nutrition.backend.userNutritionalProfile.repository.UserProfileRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.http.MediaType;
import java.math.BigDecimal;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:meal-membership;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.open-in-view=false"})
@AutoConfigureMockMvc
class MoveAndDeleteMealItemTest {
    @Autowired MockMvc mvc;
    @Autowired DailyLogRepository logs;
    @Autowired UserProfileRepository profiles;
    @Autowired DailyLogApplicationService service;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager transactions;
    @MockitoBean TerminalRunner terminalRunner;
    @MockitoBean Clock clock;
    @MockitoBean FoodCatalogQueryService foods;
    private final LocalDate today = LocalDate.of(2026, 11, 14);

    @BeforeEach
    void setUp() {
        when(clock.instant()).thenReturn(Instant.parse("2026-11-13T16:00:00Z"));
        when(clock.getZone()).thenReturn(ZoneId.of("Asia/Seoul"));
        logs.deleteAll();
        profiles.deleteAll();
        profiles.save(new UserNutritionalProfile(new UserId("owner"),
                PhysicalData.validateAndCreate(30, 70, 175, "MALE", null), ActivityLevel.of("SEDENTARY"),
                new NutritionalGoal(2200, 2200, new MacroBreakdown(150, 220, 80))));
    }

    private MealItem item(String name) {
        return new MealItem(new FoodSnapshot(name, null, null, new BigDecimal("100"), BigDecimal.TEN,
                new BigDecimal("15"), new BigDecimal("2"), NutrientComposition.Basis.PER_SERVING,
                BigDecimal.ONE, "serving"), PortionQuantity.servings(BigDecimal.ONE));
    }

    private Long createLog(String owner, LocalDate date, int dinnerItems) {
        var log = new DailyLog(new UserId(owner), date, 1L, new DailyGoalSnapshot(2000, 120, 220, 70));
        log.addMealItem(new MealType("Lunch"), item("Logged recipe snapshot"));
        for (int index = 0; index < dinnerItems; index++) log.addMealItem(new MealType("Dinner"), item("Food"));
        return logs.saveAndFlush(log).getMeals().getFirst().getItems().getFirst().getId();
    }

    private long count(String table) { return jdbc.queryForObject("select count(*) from " + table, Long.class); }

    @Test
    void movesIntoExistingDinnerPreservingItsFourItemsAndItemIdentity() throws Exception {
        Long id = createLog("owner", today, 4);
        Long originalMeal = jdbc.queryForObject("select meal_id from daily_log_meal_item where id = ?", Long.class, id);
        mvc.perform(patch("/api/daily-log/today/items/{id}/meal", id).with(jwt().jwt(t -> t.subject("owner")))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"destinationMealType\":\"Dinner\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meals[1].items").isEmpty())
                .andExpect(jsonPath("$.meals[2].items.length()").value(5))
                .andExpect(jsonPath("$.nutritionSummary.consumed.caloriesKcal").value(500));
        assertNotEquals(originalMeal, jdbc.queryForObject("select meal_id from daily_log_meal_item where id = ?", Long.class, id));
        assertEquals(5, count("daily_log_meal_item"));
        assertEquals(2, count("daily_log_meal")); // Empty Lunch remains.
        var reloaded = service.getToday("owner");
        var moved = reloaded.meals().get(2).items().stream().filter(i -> i.itemId().equals(id)).findFirst().orElseThrow();
        assertEquals("Logged recipe snapshot", moved.foodName());
        assertEquals(1, moved.quantity().intValue());
        assertEquals(2000, reloaded.nutritionSummary().targets().caloriesKcal().intValue());
        // Moving again to the same destination is a no-op.
        service.moveItem("owner", id, new MoveMealItemRequestDTO("dinner"));
        assertEquals(5, count("daily_log_meal_item"));
    }

    @Test
    void createsMissingDestinationAndReusesSnacksForSubsequentMovesAndAdditions() {
        Long first = createLog("owner", today, 1);
        service.moveItem("owner", first, new MoveMealItemRequestDTO("Snacks"));
        Long second = service.getToday("owner").meals().get(2).items().getFirst().itemId();
        service.moveItem("owner", second, new MoveMealItemRequestDTO("Snacks"));
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            var log = logs.findByUserIdAndDate("owner", today).orElseThrow();
            log.addMealItem(new MealType("Snacks"), item("Another snack"));
            log.addMealItem(new MealType("Snacks"), item("Another snack"));
            em.flush();
            em.clear();
            var loaded = logs.findByUserIdAndDate("owner", today).orElseThrow();
            assertEquals(1, loaded.getMeals().stream().filter(m -> m.getMealType().getValue().equals("SNACKS")).count());
            assertEquals(4, loaded.getMeals().stream().filter(m -> m.getMealType().getValue().equals("SNACKS"))
                    .findFirst().orElseThrow().getItems().size());
        });
        assertEquals(4, service.getToday("owner").meals().get(3).items().size());
    }

    @Test
    void explicitlyDeletesOneItemThenDeletesFinalLogAndRecreatesItWithCurrentTargets() throws Exception {
        Long id = createLog("owner", today, 1);
        var originalLogId = jdbc.queryForObject("select daily_log_id from daily_log", String.class);
        mvc.perform(delete("/api/daily-log/today/items/{id}", id).with(jwt().jwt(t -> t.subject("owner"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nutritionSummary.consumed.caloriesKcal").value(100));
        assertEquals(1, count("daily_log_meal_item"));
        assertEquals(2, count("daily_log_meal"));
        Long last = service.getToday("owner").meals().get(2).items().getFirst().itemId();
        mvc.perform(delete("/api/daily-log/today/items/{id}", last).with(jwt().jwt(t -> t.subject("owner"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nutritionSummary.consumed.caloriesKcal").value(0))
                .andExpect(jsonPath("$.nutritionSummary.targets.caloriesKcal").value(2200));
        assertEquals(0, count("daily_log_meal_item"));
        assertEquals(0, count("daily_log_meal"));
        assertEquals(0, count("daily_log"));
        when(foods.getFoodDetails("food")).thenReturn(new FoodCatalogDetailsDTO("food", "Food", null, null,
                BigDecimal.ONE, "serving", new BigDecimal("100"), BigDecimal.TEN,
                new BigDecimal("15"), new BigDecimal("2"), "PER_SERVING"));
        var recreated = service.addMealItem("owner", new AddMealItemRequestDTO("food", "Lunch", BigDecimal.ONE));
        assertEquals(2200, recreated.nutritionSummary().targets().caloriesKcal().intValue());
        assertNotEquals(originalLogId, jdbc.queryForObject("select daily_log_id from daily_log", String.class));
    }

    @Test
    void rollsBackMovesAndExplicitDeletionsIncludingFinalLogDeletion() {
        Long id = createLog("owner", today, 1);
        assertThrows(IllegalStateException.class, () -> new TransactionTemplate(transactions).executeWithoutResult(status -> {
            service.moveItem("owner", id, new MoveMealItemRequestDTO("Snacks"));
            em.flush();
            throw new IllegalStateException("rollback");
        }));
        assertEquals(id, service.getToday("owner").meals().get(1).items().getFirst().itemId());
        assertEquals(2, count("daily_log_meal"));
        assertThrows(IllegalStateException.class, () -> new TransactionTemplate(transactions).executeWithoutResult(status -> {
            service.deleteItem("owner", id);
            Long last = service.getToday("owner").meals().get(2).items().getFirst().itemId();
            service.deleteItem("owner", last);
            em.flush();
            throw new IllegalStateException("rollback");
        }));
        assertEquals(1, count("daily_log"));
        assertEquals(2, count("daily_log_meal_item"));
    }

    @Test
    void rejectsForeignHistoricalMissingAndInvalidRequestsWithoutChangingRecords() throws Exception {
        Long own = createLog("owner", today, 0);
        Long foreign = createLog("other", today, 0);
        Long past = createLog("owner", today.minusDays(1), 0);
        for (Long id : new Long[]{foreign, past, Long.MAX_VALUE}) {
            mvc.perform(patch("/api/daily-log/today/items/{id}/meal", id).with(jwt().jwt(t -> t.subject("owner")))
                            .contentType(MediaType.APPLICATION_JSON).content("{\"destinationMealType\":\"Dinner\"}"))
                    .andExpect(status().isNotFound());
            mvc.perform(delete("/api/daily-log/today/items/{id}", id).with(jwt().jwt(t -> t.subject("owner"))))
                    .andExpect(status().isNotFound());
        }
        for (String body : new String[]{"{}", "{\"destinationMealType\":\"Brunch\"}"})
            mvc.perform(patch("/api/daily-log/today/items/{id}/meal", own).with(jwt().jwt(t -> t.subject("owner")))
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        mvc.perform(delete("/api/daily-log/today/items/{id}", own)).andExpect(status().isUnauthorized());
        mvc.perform(patch("/api/daily-log/today/items/{id}/meal", own).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"destinationMealType\":\"Dinner\"}")).andExpect(status().isUnauthorized());
        assertEquals(3, count("daily_log_meal_item"));
    }

    @Test
    void consolidatesLegacySnackRowsWithoutChangingEntriesOrNutrition() {
        Long first = createLog("owner", today, 1);
        service.moveItem("owner", first, new MoveMealItemRequestDTO("Snacks"));
        Long second = service.getToday("owner").meals().get(2).items().getFirst().itemId();
        service.moveItem("owner", second, new MoveMealItemRequestDTO("Snacks"));
        jdbc.update("insert into daily_log_meal(daily_log_id, meal_type) select daily_log_id, 'SNACKS' from daily_log");
        Long duplicate = jdbc.queryForObject("select max(id) from daily_log_meal", Long.class);
        jdbc.update("update daily_log_meal_item set meal_id = ? where id = ?", duplicate, second);
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            jdbc.update("""
                    UPDATE daily_log_meal_item AS item
                    SET meal_id = (
                        SELECT MIN(destination.id)
                        FROM daily_log_meal AS source
                        JOIN daily_log_meal AS destination
                          ON destination.daily_log_id = source.daily_log_id
                         AND destination.meal_type = source.meal_type
                        WHERE source.id = item.meal_id
                    )
                    """);
            jdbc.update("""
                    DELETE FROM daily_log_meal
                    WHERE id NOT IN (
                        SELECT retained.keep_id FROM (
                            SELECT MIN(id) AS keep_id
                            FROM daily_log_meal
                            GROUP BY daily_log_id, meal_type
                        ) AS retained
                    )
                    """);
        });
        assertEquals(1, jdbc.queryForObject("select count(*) from daily_log_meal where meal_type = 'SNACKS'", Long.class));
        assertEquals(2, count("daily_log_meal_item"));
        var snackItems = service.getToday("owner").meals().get(3).items();
        assertEquals(java.util.Set.of(first, second), snackItems.stream().map(MealItemResponseDTO::itemId)
                .collect(java.util.stream.Collectors.toSet()));
        assertEquals(200, service.getToday("owner").nutritionSummary().consumed().caloriesKcal().intValue());
    }

    @Test
    void deletingWholeAggregateCascadesToItsItemsWithoutOrphanRemoval() {
        createLog("owner", today, 4);
        logs.deleteAll();
        assertEquals(0, count("daily_log_meal_item"));
        assertEquals(0, count("daily_log_meal"));
    }
}
