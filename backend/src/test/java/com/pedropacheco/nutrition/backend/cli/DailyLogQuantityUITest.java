package com.pedropacheco.nutrition.backend.cli;

import com.pedropacheco.nutrition.backend.dailyLog.domain.*;
import com.pedropacheco.nutrition.backend.dailyLog.dto.*;
import com.pedropacheco.nutrition.backend.dailyLog.service.*;
import com.pedropacheco.nutrition.backend.foodCatalog.domain.NutrientComposition;
import com.pedropacheco.nutrition.backend.foodCatalog.service.FoodCatalogSearchService;
import com.pedropacheco.nutrition.backend.userAuth.domain.UserId;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Scanner;
import static org.mockito.Mockito.*;

class DailyLogQuantityUITest {
    @Test
    void selectsAnEntryAndSendsItsNewQuantityToTheService() throws Exception {
        var service = mock(DailyLogApplicationService.class);
        var log = new DailyLog(new UserId("owner"), LocalDate.of(2026, 11, 13), 1L,
                new DailyGoalSnapshot(2000, 120, 220, 70));
        var item = new MealItem(new FoodSnapshot("Food", null, null, BigDecimal.TEN, BigDecimal.ONE,
                BigDecimal.ONE, BigDecimal.ONE, NutrientComposition.Basis.PER_SERVING, BigDecimal.ONE, "serving"),
                PortionQuantity.servings(BigDecimal.ONE));
        var id = MealItem.class.getDeclaredField("id");
        id.setAccessible(true);
        id.set(item, 42L);
        log.addMealItem(new MealType("Lunch"), item);
        var scaling = new NutrientScalingService();
        var response = new DailyLogMapper(scaling).toResponseDTO(log, log.calculateNutritionSummary(scaling));
        when(service.getToday("owner")).thenReturn(response);
        when(service.changeItemQuantity(eq("owner"), eq(42L), any())).thenReturn(response);
        var ui = new DailyLogUI(service, mock(FoodCatalogSearchService.class), mock(PersonalRecipesUI.class));
        ui.show("owner", new Scanner("2\n1\n2.5\n0\n"));
        verify(service).changeItemQuantity("owner", 42L, new ChangeMealItemQuantityRequestDTO(new BigDecimal("2.5")));
        verify(service, never()).addMealItem(any(), any());
    }
}
