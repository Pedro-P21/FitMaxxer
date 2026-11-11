package com.pedropacheco.nutrition.backend.dailyLog.service;

import com.pedropacheco.nutrition.backend.dailyLog.domain.DailyGoalSnapshot;
import com.pedropacheco.nutrition.backend.dailyLog.domain.DailyLog;
import com.pedropacheco.nutrition.backend.dailyLog.domain.MealItem;
import com.pedropacheco.nutrition.backend.dailyLog.domain.MealType;
import com.pedropacheco.nutrition.backend.dailyLog.domain.PortionQuantity;
import com.pedropacheco.nutrition.backend.dailyLog.domain.FoodSnapshot;
import com.pedropacheco.nutrition.backend.dailyLog.dto.DailyLogMapper;
import com.pedropacheco.nutrition.backend.dailyLog.dto.LogHistoryResponseDTO;
import com.pedropacheco.nutrition.backend.dailyLog.dto.PastHistoryMapper;
import com.pedropacheco.nutrition.backend.dailyLog.repository.DailyLogRepository;
import com.pedropacheco.nutrition.backend.foodCatalog.domain.NutrientComposition;
import com.pedropacheco.nutrition.backend.userAuth.domain.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LogHistoryServiceTest {
    private final DailyLogRepository repository = mock(DailyLogRepository.class);
    private final NutrientScalingService scalingService = new NutrientScalingService();
    private final LocalDate today = LocalDate.of(2026, 11, 15);
    private LogHistoryService service;

    @BeforeEach
    void setUp() {
        var mapper = new PastHistoryMapper(new DailyLogMapper(scalingService), scalingService);
        service = new LogHistoryService(repository, mapper,
                Clock.fixed(Instant.parse("2026-11-15T12:00:00Z"), ZoneOffset.UTC), 10);
    }

    @Test
    void returnsSelectedMonthLogsWithSummariesAndAuthenticatedOwnerScope() {
        DailyLog newest = log(today.minusDays(1), "Dinner");
        DailyLog older = log(today.minusDays(5), "Lunch");
        when(repository.findByUserIdAndDateRange("user-1", LocalDate.of(2026, 11, 1), today.minusDays(1)))
                .thenReturn(List.of(newest, older));

        LogHistoryResponseDTO result = service.getPastHistory("user-1", 11, 2026);

        assertEquals(2026, result.year());
        assertEquals(11, result.month());
        assertEquals(List.of(today.minusDays(1), today.minusDays(5)),
                result.logs().stream().map(log -> log.date()).toList());
        assertEquals(new BigDecimal("100"), result.logs().getFirst()
                .nutritionSummary().consumed().caloriesKcal());
        verify(repository).findByUserIdAndDateRange("user-1", LocalDate.of(2026, 11, 1), today.minusDays(1));
    }

    @Test
    void queriesFullDateRangeForPastMonth() {
        when(repository.findByUserIdAndDateRange("user-1", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)))
                .thenReturn(List.of());

        LogHistoryResponseDTO result = service.getPastHistory("user-1", 10, 2026);

        assertTrue(result.logs().isEmpty());
        verify(repository).findByUserIdAndDateRange("user-1", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));
    }

    @Test
    void returnsEmptyForCurrentMonthOnItsFirstDayWithoutQueryingFutureDays() {
        var firstDayClock = Clock.fixed(Instant.parse("2026-11-01T12:00:00Z"), ZoneOffset.UTC);
        service = new LogHistoryService(repository,
                new PastHistoryMapper(new DailyLogMapper(scalingService), scalingService), firstDayClock, 10);

        LogHistoryResponseDTO result = service.getPastHistory("user-1", 11, 2026);

        assertTrue(result.logs().isEmpty());
        verifyNoInteractions(repository);
    }

    @Test
    void rejectsInvalidFutureAndTooOldMonthsWithoutQueryingRepository() {
        assertThrows(IllegalArgumentException.class, () -> service.getPastHistory("user-1", 13, 2026));
        assertThrows(IllegalArgumentException.class, () -> service.getPastHistory("user-1", 12, 2026));
        assertThrows(IllegalArgumentException.class, () -> service.getPastHistory("user-1", 10, 2016));
        verifyNoInteractions(repository);
    }

    private DailyLog log(LocalDate date, String mealType) {
        DailyLog log = new DailyLog(new UserId("user-1"), date, 1L,
                new DailyGoalSnapshot(2000, 120, 220, 70));
        FoodSnapshot food = new FoodSnapshot("Banana", null, null,
                new BigDecimal("100"), BigDecimal.ONE, new BigDecimal("20"), BigDecimal.ONE,
                NutrientComposition.Basis.PER_SERVING, BigDecimal.ONE, "serving");
        log.addMealItem(new MealType(mealType), new MealItem(food, PortionQuantity.servings(BigDecimal.ONE)));
        return log;
    }
}
