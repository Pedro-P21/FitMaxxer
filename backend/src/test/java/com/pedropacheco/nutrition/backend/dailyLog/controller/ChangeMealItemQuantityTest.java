package com.pedropacheco.nutrition.backend.dailyLog.controller;

import com.pedropacheco.nutrition.backend.cli.TerminalRunner;
import com.pedropacheco.nutrition.backend.dailyLog.domain.*;
import com.pedropacheco.nutrition.backend.dailyLog.repository.DailyLogRepository;
import com.pedropacheco.nutrition.backend.dailyLog.service.DailyLogApplicationService;
import com.pedropacheco.nutrition.backend.foodCatalog.domain.NutrientComposition;
import com.pedropacheco.nutrition.backend.userAuth.domain.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import java.math.BigDecimal;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:quantity-change;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class ChangeMealItemQuantityTest {
    @Autowired MockMvc mvc;
    @Autowired DailyLogRepository repository;
    @Autowired DailyLogApplicationService service;
    @MockitoBean TerminalRunner terminalRunner;
    @MockitoBean Clock clock;
    private final LocalDate today = LocalDate.of(2026, 11, 13);

    @BeforeEach
    void setUp() {
        when(clock.instant()).thenReturn(Instant.parse("2026-11-12T16:00:00Z"));
        when(clock.getZone()).thenReturn(ZoneId.of("Asia/Seoul"));
        repository.deleteAll();
    }

    private Long createItem(String owner, LocalDate date, String name) {
        var log = new DailyLog(new UserId(owner), date, 1L, new DailyGoalSnapshot(2000, 120, 220, 70));
        var snapshot = new FoodSnapshot(name, null, null, new BigDecimal("100"), new BigDecimal("10"),
                new BigDecimal("15"), new BigDecimal("2"), NutrientComposition.Basis.PER_SERVING, BigDecimal.ONE, "serving");
        log.addMealItem(new MealType("Breakfast"), new MealItem(snapshot, PortionQuantity.servings(BigDecimal.ONE)));
        log.addMealItem(new MealType("Breakfast"), new MealItem(snapshot, PortionQuantity.servings(BigDecimal.ONE)));
        return repository.saveAndFlush(log).getMeals().getFirst().getItems().getFirst().getId();
    }

    @Test
    void updatesOnlySelectedDuplicateAndPersistsFoodAndRecipeSnapshotQuantities() throws Exception {
        for (String name : new String[]{"Banana", "Saved recipe snapshot"}) {
            repository.deleteAll();
            Long id = createItem("owner", today, name);
            mvc.perform(patch("/api/daily-log/today/items/{id}/quantity", id)
                            .with(jwt().jwt(t -> t.subject("owner"))).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"quantity\":2.5}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.meals[0].items[0].itemId").value(id))
                    .andExpect(jsonPath("$.meals[0].items[0].quantity").value(2.5))
                    .andExpect(jsonPath("$.meals[0].items[1].quantity").value(1))
                    .andExpect(jsonPath("$.meals[0].consumed.caloriesKcal").value(350))
                    .andExpect(jsonPath("$.nutritionSummary.consumed.proteinGrams").value(35))
                    .andExpect(jsonPath("$.nutritionSummary.consumed.carbohydrateGrams").value(52.5))
                    .andExpect(jsonPath("$.nutritionSummary.consumed.fatGrams").value(7))
                    .andExpect(jsonPath("$.nutritionSummary.remaining.caloriesKcal").value(1650));
            var reloaded = service.getToday("owner");
            assertEquals(0, new BigDecimal("2.5").compareTo(reloaded.meals().getFirst().items().getFirst().quantity()));
            assertEquals(name, reloaded.meals().getFirst().items().getFirst().foodName());
            assertEquals("SERVING", reloaded.meals().getFirst().items().getFirst().quantityUnit());
            assertEquals(2000, reloaded.nutritionSummary().targets().caloriesKcal().intValue());
        }
    }

    @Test
    void rejectsForeignHistoricalAndMissingItems() throws Exception {
        Long foreign = createItem("other", today, "Foreign");
        Long past = createItem("owner", today.minusDays(1), "Historical");
        Long own = createItem("owner", today, "Current");
        for (Long id : new Long[]{foreign, past, Long.MAX_VALUE}) {
            mvc.perform(patch("/api/daily-log/today/items/{id}/quantity", id)
                            .with(jwt().jwt(t -> t.subject("owner"))).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"quantity\":2}"))
                    .andExpect(status().isNotFound());
        }
        mvc.perform(patch("/api/daily-log/today/items/{id}/quantity", own)
                        .with(jwt().jwt(t -> t.subject("no-log"))).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":2}"))
                .andExpect(status().isNotFound());
        assertEquals(1, service.getToday("owner").meals().getFirst().items().getFirst().quantity().intValue());
        assertEquals(1, service.getToday("other").meals().getFirst().items().getFirst().quantity().intValue());
        assertEquals(3, repository.count());
    }

    @Test
    void rejectsInvalidAmountsWithoutPersistingChanges() throws Exception {
        Long id = createItem("owner", today, "Food");
        for (String body : new String[]{"{}", "{\"quantity\":null}", "{\"quantity\":0}",
                "{\"quantity\":-1}", "{\"quantity\":0.00001}", "{\"quantity\":100000000}"}) {
            mvc.perform(patch("/api/daily-log/today/items/{id}/quantity", id)
                            .with(jwt().jwt(t -> t.subject("owner"))).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        assertEquals(1, service.getToday("owner").meals().getFirst().items().getFirst().quantity().intValue());
    }

    @Test
    void requiresAuthentication() throws Exception {
        mvc.perform(patch("/api/daily-log/today/items/1/quantity")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":2}"))
                .andExpect(status().isUnauthorized());
    }
}
