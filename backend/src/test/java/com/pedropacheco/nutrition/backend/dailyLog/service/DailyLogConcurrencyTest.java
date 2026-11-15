package com.pedropacheco.nutrition.backend.dailyLog.service;

import com.pedropacheco.nutrition.backend.cli.TerminalRunner;
import com.pedropacheco.nutrition.backend.customRecipe.dto.*;
import com.pedropacheco.nutrition.backend.customRecipe.service.CustomRecipeService;
import com.pedropacheco.nutrition.backend.dailyLog.Exception.DailyLogWriteConflictException;
import com.pedropacheco.nutrition.backend.dailyLog.domain.*;
import com.pedropacheco.nutrition.backend.dailyLog.dto.*;
import com.pedropacheco.nutrition.backend.dailyLog.repository.DailyLogRepository;
import com.pedropacheco.nutrition.backend.foodCatalog.domain.NutrientComposition;
import com.pedropacheco.nutrition.backend.foodCatalog.dto.FoodCatalogDetailsDTO;
import com.pedropacheco.nutrition.backend.foodCatalog.service.FoodCatalogQueryService;
import com.pedropacheco.nutrition.backend.userAuth.domain.UserId;
import com.pedropacheco.nutrition.backend.userNutritionalProfile.domain.*;
import com.pedropacheco.nutrition.backend.userNutritionalProfile.dto.ProfileChangesDTO;
import com.pedropacheco.nutrition.backend.userNutritionalProfile.repository.UserProfileRepository;
import com.pedropacheco.nutrition.backend.userNutritionalProfile.service.EditUserNutritionalProfileService;
import jakarta.persistence.EntityManager;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:daily-log-concurrency;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=2000",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.open-in-view=false"})
@AutoConfigureMockMvc
class DailyLogConcurrencyTest {
    @Autowired DailyLogApplicationService service;
    @Autowired EditUserNutritionalProfileService profileService;
    @Autowired UserProfileRepository profiles;
    @Autowired PlatformTransactionManager transactions;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager em;
    @Autowired MockMvc mvc;
    @MockitoSpyBean DailyLogRepository logs;
    @MockitoBean TerminalRunner terminalRunner;
    @MockitoBean Clock clock;
    @MockitoBean FoodCatalogQueryService catalog;
    @MockitoBean CustomRecipeService recipes;
    private final LocalDate today = LocalDate.of(2026, 11, 15);
    private ExecutorService pool;

    @BeforeEach
    void setUp() {
        when(clock.instant()).thenReturn(Instant.parse("2026-11-15T04:00:00Z"));
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        logs.deleteAll();
        profiles.deleteAll();
        profile("owner"); profile("other");
        when(catalog.getFoodDetails("food")).thenAnswer(invocation -> {
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive(), "Catalog lookup must precede the write transaction");
            return new FoodCatalogDetailsDTO("food", "Food", null, null, BigDecimal.ONE, "serving",
                    new BigDecimal("100"), BigDecimal.TEN, new BigDecimal("15"), new BigDecimal("2"), "PER_SERVING");
        });
        when(recipes.getRecipe("owner", "recipe")).thenAnswer(invocation -> {
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
            var nutrients = new RecipeNutrientsDTO(new BigDecimal("100"), BigDecimal.TEN, new BigDecimal("15"), new BigDecimal("2"));
            return new CustomRecipeResponseDTO("recipe", "Saved recipe", BigDecimal.ONE, List.of(), nutrients, nutrients);
        });
        pool = Executors.newFixedThreadPool(4);
        clearInvocations(logs);
    }

    @AfterEach
    void shutDown() throws InterruptedException {
        pool.shutdownNow();
        assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
    }

    private void profile(String owner) {
        profiles.save(new UserNutritionalProfile(new UserId(owner),
                PhysicalData.validateAndCreate(30, 70, 175, "MALE", null), ActivityLevel.of("SEDENTARY"),
                new NutritionalGoal(2000, 2000, new MacroBreakdown(120, 220, 70))));
    }

    private MealItem item() {
        return new MealItem(new FoodSnapshot("Food", null, null, new BigDecimal("100"), BigDecimal.TEN,
                new BigDecimal("15"), new BigDecimal("2"), NutrientComposition.Basis.PER_SERVING,
                BigDecimal.ONE, "serving"), PortionQuantity.servings(BigDecimal.ONE));
    }

    private void log(String owner, LocalDate date) {
        var log = new DailyLog(new UserId(owner), date, 1L, new DailyGoalSnapshot(2000, 120, 220, 70));
        log.addMealItem(new MealType("Dinner"), item());
        logs.saveAndFlush(log);
    }

    private DailyLogResponseDTO add(String owner) {
        return service.addMealItem(owner, new AddMealItemRequestDTO("food", "Snacks", BigDecimal.ONE));
    }

    private static void await(CountDownLatch latch) {
        try { assertTrue(latch.await(10, TimeUnit.SECONDS), "Timed out waiting for concurrent operation"); }
        catch (InterruptedException exception) { Thread.currentThread().interrupt(); throw new IllegalStateException(exception); }
    }

    private Future<?> holdOwnerLock(CountDownLatch held, CountDownLatch release) {
        return pool.submit(() -> new TransactionTemplate(transactions).executeWithoutResult(status -> {
            logs.findByUserIdAndDateForUpdate("owner", today).orElseThrow();
            held.countDown();
            await(release);
        }));
    }

    @ParameterizedTest
    @ValueSource(strings = {"catalog", "recipe", "copy"})
    void concurrentFirstWritesRetryWithOneLogAndBothItems(String mode) throws Exception {
        if (mode.equals("copy")) log("owner", today.minusDays(1));
        var bothMissing = new CyclicBarrier(2);
        var missingReads = new AtomicInteger();
        doAnswer(invocation -> {
            Object result = mockingDetails(logs).getMockCreationSettings().getDefaultAnswer().answer(invocation);
            if (((Optional<?>) result).isEmpty() && missingReads.getAndIncrement() < 2)
                bothMissing.await(10, TimeUnit.SECONDS);
            return result;
        }).when(logs).findByUserIdAndDateForUpdate("owner", today);
        Callable<DailyLogResponseDTO> write = switch (mode) {
            case "catalog" -> () -> add("owner");
            case "recipe" -> () -> service.addRecipeMealItem("owner", new AddRecipeMealItemRequestDTO("recipe", "Snacks", BigDecimal.ONE));
            default -> () -> service.copyMeal("owner", new CopyMealRequestDTO(today.minusDays(1), "Dinner", "Snacks"));
        };
        var first = pool.submit(write);
        var second = pool.submit(write);
        first.get(15, TimeUnit.SECONDS); second.get(15, TimeUnit.SECONDS);
        var result = service.getToday("owner");
        assertEquals(2, result.meals().get(3).items().size());
        assertEquals(2, result.meals().get(3).items().stream().map(MealItemResponseDTO::itemId).distinct().count());
        assertEquals(200, result.nutritionSummary().consumed().caloriesKcal().intValue());
        assertEquals(1, jdbc.queryForObject("select count(*) from daily_log where user_id = 'owner' and log_date = ?", Long.class, today));
        verify(logs, times(3)).findByUserIdAndDateForUpdate("owner", today);
        if (mode.equals("catalog")) verify(catalog, times(2)).getFoodDetails("food");
        if (mode.equals("recipe")) verify(recipes, times(2)).getRecipe("owner", "recipe");
    }

    @Test
    void concurrentAddsToExistingLogPreserveBothEntriesAndOneMealGrouping() throws Exception {
        log("owner", today);
        var start = new CyclicBarrier(2);
        Callable<DailyLogResponseDTO> write = () -> { start.await(10, TimeUnit.SECONDS); return add("owner"); };
        var first = pool.submit(write); var second = pool.submit(write);
        first.get(10, TimeUnit.SECONDS); second.get(10, TimeUnit.SECONDS);
        assertEquals(2, service.getToday("owner").meals().get(3).items().size());
        assertEquals(300, service.getToday("owner").nutritionSummary().consumed().caloriesKcal().intValue());
        assertEquals(1, jdbc.queryForObject("select count(*) from daily_log_meal where meal_type = 'SNACKS'", Long.class));
    }

    @Test
    void sameLogWriterWaitsButAnotherUserAndNormalReadsProceed() throws Exception {
        log("owner", today); log("other", today);
        var held = new CountDownLatch(1); var release = new CountDownLatch(1);
        var holder = holdOwnerLock(held, release);
        try {
            await(held);
            var requested = new CountDownLatch(1);
            doAnswer(invocation -> { requested.countDown(); return mockingDetails(logs).getMockCreationSettings().getDefaultAnswer().answer(invocation); })
                    .when(logs).findByUserIdAndDateForUpdate("owner", today);
            var writer = pool.submit(() -> add("owner"));
            await(requested);
            assertThrows(TimeoutException.class, () -> writer.get(150, TimeUnit.MILLISECONDS));
            assertEquals(1, pool.submit(() -> add("other")).get(1, TimeUnit.SECONDS).meals().get(3).items().size());
            assertEquals(100, pool.submit(() -> service.getToday("owner")).get(1, TimeUnit.SECONDS)
                    .nutritionSummary().consumed().caloriesKcal().intValue());
            release.countDown();
            writer.get(5, TimeUnit.SECONDS);
            holder.get(5, TimeUnit.SECONDS);
        } finally { release.countDown(); }
    }

    @ParameterizedTest
    @ValueSource(strings = {"quantity", "move", "delete", "copy"})
    void everyItemMutationUsesTheAggregateLock(String operation) throws Exception {
        log("owner", today);
        if (operation.equals("copy")) log("owner", today.minusDays(1));
        Long itemId = service.getToday("owner").meals().get(2).items().getFirst().itemId();
        var held = new CountDownLatch(1); var release = new CountDownLatch(1);
        var holder = holdOwnerLock(held, release);
        try {
            await(held);
            var requested = new CountDownLatch(1);
            doAnswer(invocation -> {
                requested.countDown();
                return mockingDetails(logs).getMockCreationSettings().getDefaultAnswer().answer(invocation);
            }).when(logs).findByUserIdAndDateForUpdate("owner", today);
            var writer = pool.submit(() -> switch (operation) {
                case "quantity" -> service.changeItemQuantity("owner", itemId, new ChangeMealItemQuantityRequestDTO(new BigDecimal("2")));
                case "move" -> service.moveItem("owner", itemId, new MoveMealItemRequestDTO("Lunch"));
                case "delete" -> service.deleteItem("owner", itemId);
                default -> service.copyMeal("owner", new CopyMealRequestDTO(today.minusDays(1), "Dinner", "Snacks"));
            });
            await(requested);
            assertThrows(TimeoutException.class, () -> writer.get(150, TimeUnit.MILLISECONDS));
            release.countDown(); writer.get(5, TimeUnit.SECONDS); holder.get(5, TimeUnit.SECONDS);
        } finally { release.countDown(); }
    }

    @Test
    void targetRefreshWaitsForTheSameDailyLogLock() throws Exception {
        log("owner", today);
        var held = new CountDownLatch(1); var release = new CountDownLatch(1);
        var holder = holdOwnerLock(held, release);
        try {
            await(held);
            var requested = new CountDownLatch(1);
            doAnswer(invocation -> { requested.countDown(); return mockingDetails(logs).getMockCreationSettings().getDefaultAnswer().answer(invocation); })
                    .when(logs).findByUserIdAndDateForUpdate("owner", today);
            var changes = new ProfileChangesDTO(); changes.calorieTarget = 2200;
            var edit = pool.submit(() -> profileService.editUserProfile("owner", changes));
            await(requested);
            assertThrows(TimeoutException.class, () -> edit.get(150, TimeUnit.MILLISECONDS));
            release.countDown(); edit.get(5, TimeUnit.SECONDS); holder.get(5, TimeUnit.SECONDS);
            assertEquals(2200, service.getToday("owner").nutritionSummary().targets().caloriesKcal().intValue());
        } finally { release.countDown(); }
    }

    @Test
    void lockTimeoutReturnsConflictAndDoesNotSaveTheAddition() throws Exception {
        log("owner", today);
        var held = new CountDownLatch(1); var release = new CountDownLatch(1);
        var holder = holdOwnerLock(held, release);
        try {
            await(held);
            mvc.perform(post("/api/daily-log/today/items").with(jwt().jwt(token -> token.subject("owner")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"foodId\":\"food\",\"mealType\":\"Snacks\",\"quantity\":1,\"quantityUnit\":\"SERVING\"}"))
                    .andExpect(status().isConflict());
        } finally { release.countDown(); holder.get(5, TimeUnit.SECONDS); }
        assertEquals(100, service.getToday("owner").nutritionSummary().consumed().caloriesKcal().intValue());
    }

    @Test
    void creationRetriesAreBoundedAndUseNewEntitiesAndValueObjects() {
        List<MealItem> attemptedItems = new ArrayList<>();
        doAnswer(invocation -> {
            DailyLog log = invocation.getArgument(0);
            attemptedItems.add(log.getMeals().getFirst().getItems().getFirst());
            throw new DataIntegrityViolationException("creation conflict", new ConstraintViolationException(
                    "duplicate", new SQLException("duplicate", "23505"), "uk_daily_log_user_date"));
        }).when(logs).save(any(DailyLog.class));
        assertThrows(DailyLogWriteConflictException.class, () -> add("owner"));
        assertEquals(3, attemptedItems.size());
        assertNotSame(attemptedItems.get(0), attemptedItems.get(1));
        assertNotSame(attemptedItems.get(0).getFoodSnapshot(), attemptedItems.get(1).getFoodSnapshot());
        assertNotSame(attemptedItems.get(0).getPortionQuantity(), attemptedItems.get(1).getPortionQuantity());
        assertEquals(0, logs.count());
        verify(catalog).getFoodDetails("food");
    }

    @Test
    void creationConflictInsideCallerTransactionPropagatesWithoutAnInnerRetry() {
        log("owner", today.minusDays(1));
        clearInvocations(logs);
        doThrow(new DataIntegrityViolationException("creation conflict", new ConstraintViolationException(
                "duplicate", new SQLException("duplicate", "23505"), "uk_daily_log_user_date")))
                .when(logs).save(any(DailyLog.class));
        assertThrows(DailyLogWriteConflictException.class, () -> new TransactionTemplate(transactions).executeWithoutResult(status ->
                service.copyMeal("owner", new CopyMealRequestDTO(today.minusDays(1), "Dinner", "Snacks"))));
        verify(logs).save(any(DailyLog.class));
        assertEquals(1, logs.count());
        assertTrue(logs.findByUserIdAndDate("owner", today).isEmpty());
    }

    @Test
    void unrelatedIntegrityFailureIsNotRetriedAndCallerTransactionIsNotCommittedIndependently() {
        doThrow(new DataIntegrityViolationException("unrelated constraint")).when(logs).save(any(DailyLog.class));
        assertThrows(DataIntegrityViolationException.class, () -> add("owner"));
        verify(logs).save(any(DailyLog.class));
        reset(logs);
        // Calling from an existing transaction preserves the caller's rollback boundary.
        assertThrows(IllegalStateException.class, () -> new TransactionTemplate(transactions).executeWithoutResult(status -> {
            // Use copy to avoid external lookup preparation inside the caller's transaction.
            var source = new DailyLog(new UserId("owner"), today.minusDays(1), 1L, new DailyGoalSnapshot(2000, 120, 220, 70));
            source.addMealItem(new MealType("Dinner"), item()); logs.saveAndFlush(source);
            service.copyMeal("owner", new CopyMealRequestDTO(today.minusDays(1), "Dinner", "Snacks"));
            em.flush();
            throw new IllegalStateException("caller rollback");
        }));
        assertEquals(0, logs.count());
    }
}
