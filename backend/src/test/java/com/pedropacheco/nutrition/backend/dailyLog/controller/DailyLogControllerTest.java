package com.pedropacheco.nutrition.backend.dailyLog.controller;

import com.pedropacheco.nutrition.backend.cli.TerminalRunner;
import com.pedropacheco.nutrition.backend.dailyLog.dto.DailyLogResponseDTO;
import com.pedropacheco.nutrition.backend.dailyLog.dto.DailyNutritionSummaryDTO;
import com.pedropacheco.nutrition.backend.dailyLog.dto.NutrientValuesDTO;
import com.pedropacheco.nutrition.backend.dailyLog.dto.LogHistoryResponseDTO;
import com.pedropacheco.nutrition.backend.dailyLog.dto.AddMealItemRequestDTO;
import com.pedropacheco.nutrition.backend.dailyLog.service.DailyLogApplicationService;
import com.pedropacheco.nutrition.backend.dailyLog.service.LogHistoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DailyLogControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private DailyLogApplicationService dailyLogApplicationService;
    @MockitoBean private LogHistoryService logHistoryService;
    @MockitoBean private TerminalRunner terminalRunner;

    @Test
    void returnsTodayLogForAuthenticatedUser() throws Exception {
        var zero = new NutrientValuesDTO(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        DailyLogResponseDTO response = new DailyLogResponseDTO(LocalDate.of(2026, 11, 10), List.of(),
                new DailyNutritionSummaryDTO(zero, zero, zero));
        when(dailyLogApplicationService.getToday("user-1")).thenReturn(response);

        mockMvc.perform(get("/api/daily-log/today").with(jwt().jwt(token -> token.subject("user-1")))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2026-11-10"))
                .andExpect(jsonPath("$.nutritionSummary.consumed.caloriesKcal").value(0));

        verify(dailyLogApplicationService).getToday("user-1");
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/daily-log/today")).andExpect(status().isUnauthorized());
        verifyNoInteractions(dailyLogApplicationService);
    }

    @Test
    void returnsMonthlyHistoryForAuthenticatedUser() throws Exception {
        when(logHistoryService.getPastHistory("user-1", 10, 2026))
                .thenReturn(new LogHistoryResponseDTO(2026, 10, List.of()));

        mockMvc.perform(get("/api/daily-log/history").param("month", "10").param("year", "2026")
                        .with(jwt().jwt(token -> token.subject("user-1")))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(2026))
                .andExpect(jsonPath("$.month").value(10))
                .andExpect(jsonPath("$.logs").isEmpty());

        verify(logHistoryService).getPastHistory("user-1", 10, 2026);
    }

    @Test
    void requiresAuthenticationForMonthlyHistory() throws Exception {
        mockMvc.perform(get("/api/daily-log/history").param("month", "10").param("year", "2026"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(logHistoryService);
    }

    @Test
    void addsFoodForAuthenticatedUser() throws Exception {
        var zero = new NutrientValuesDTO(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        DailyLogResponseDTO response = new DailyLogResponseDTO(LocalDate.of(2026, 11, 10), List.of(),
                new DailyNutritionSummaryDTO(zero, zero, zero));
        when(dailyLogApplicationService.addMealItem(eq("user-1"), any(AddMealItemRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/api/daily-log/today/items").with(jwt().jwt(token -> token.subject("user-1")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"foodId\":\"openfoodfacts:12345678\",\"mealType\":\"Breakfast\",\"servingQuantity\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2026-11-10"));

        verify(dailyLogApplicationService).addMealItem(eq("user-1"), any(AddMealItemRequestDTO.class));
    }

    @Test
    void rejectsAddRequestWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/api/daily-log/today/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"foodId\":\"openfoodfacts:12345678\",\"mealType\":\"Breakfast\",\"servingQuantity\":1}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(dailyLogApplicationService);
    }

    @Test
    void acceptsAQuantityExplicitlyMeasuredInGrams() throws Exception {
        var zero = new NutrientValuesDTO(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        when(dailyLogApplicationService.addMealItem(eq("user-1"), any(AddMealItemRequestDTO.class)))
                .thenReturn(new DailyLogResponseDTO(LocalDate.of(2026, 11, 10), List.of(),
                        new DailyNutritionSummaryDTO(zero, zero, zero)));

        mockMvc.perform(post("/api/daily-log/today/items").with(jwt().jwt(token -> token.subject("user-1")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"foodId\":\"food-1\",\"mealType\":\"Breakfast\",\"quantity\":300,\"quantityUnit\":\"GRAM\"}"))
                .andExpect(status().isOk());

        var request = org.mockito.ArgumentCaptor.forClass(AddMealItemRequestDTO.class);
        verify(dailyLogApplicationService).addMealItem(eq("user-1"), request.capture());
        assertEquals(new BigDecimal("300"), request.getValue().quantity());
        assertEquals(com.pedropacheco.nutrition.backend.dailyLog.domain.PortionQuantity.Unit.GRAM,
                request.getValue().quantityUnit());
    }
}
