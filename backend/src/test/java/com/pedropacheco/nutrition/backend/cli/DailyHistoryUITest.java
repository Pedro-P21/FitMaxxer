package com.pedropacheco.nutrition.backend.cli;

import com.pedropacheco.nutrition.backend.dailyLog.domain.*;
import com.pedropacheco.nutrition.backend.dailyLog.dto.*;
import com.pedropacheco.nutrition.backend.dailyLog.service.*;
import com.pedropacheco.nutrition.backend.foodCatalog.domain.NutrientComposition;
import com.pedropacheco.nutrition.backend.userAuth.domain.UserId;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;
import static org.mockito.Mockito.*;

class DailyHistoryUITest {
    private final LogHistoryService history = mock(LogHistoryService.class);
    private final DailyLogApplicationService logs = mock(DailyLogApplicationService.class);
    private final DailyHistoryUI ui = new DailyHistoryUI(history, logs);
    private final LocalDate date = LocalDate.of(2026, 11, 14);

    private DailyLogResponseDTO view(boolean withItems) {
        var log = new DailyLog(new UserId("owner"), date, 1L, new DailyGoalSnapshot(2000, 120, 220, 70));
        if (withItems) log.addMealItem(new MealType("Breakfast"), new MealItem(new FoodSnapshot("Food", null, null,
                BigDecimal.TEN, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE,
                NutrientComposition.Basis.PER_SERVING, BigDecimal.ONE, "serving"), PortionQuantity.servings(BigDecimal.ONE)));
        var scaling = new NutrientScalingService();
        return new DailyLogMapper(scaling).toResponseDTO(log, log.calculateNutritionSummary(scaling));
    }

    @Test
    void copiesOnlySelectedNonemptyMealAndOffersRepeatedCopiesToDifferentDestinations() {
        var source = view(true);
        when(history.getPastHistory("owner", 11, 2026)).thenReturn(new LogHistoryResponseDTO(2026, 11, List.of(source)));
        when(logs.copyMeal(eq("owner"), any())).thenReturn(source);
        ui.show("owner", new Scanner("11\n2026\n1\n4\n1\n2\n0\n"));
        verify(logs).copyMeal("owner", new CopyMealRequestDTO(date, "Breakfast", "Snacks"));
        verify(logs).copyMeal("owner", new CopyMealRequestDTO(date, "Breakfast", "Lunch"));
        verifyNoMoreInteractions(logs);
    }

    @Test
    void cancelsDestinationAndRejectsUnlistedSourcesWithoutCopying() {
        when(history.getPastHistory("owner", 11, 2026)).thenReturn(new LogHistoryResponseDTO(2026, 11, List.of(view(true))));
        ui.show("owner", new Scanner("11\n2026\n2\n1\n0\n0\n"));
        verifyNoInteractions(logs);
    }

    @Test
    void historyContainingOnlyEmptyMealsOffersNoCopyAction() {
        when(history.getPastHistory("owner", 11, 2026)).thenReturn(new LogHistoryResponseDTO(2026, 11, List.of(view(false))));
        ui.show("owner", new Scanner("11\n2026\n"));
        verifyNoInteractions(logs);
    }
}
