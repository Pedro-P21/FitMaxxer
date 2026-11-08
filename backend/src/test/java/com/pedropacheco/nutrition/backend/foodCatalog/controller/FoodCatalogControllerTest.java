package com.pedropacheco.nutrition.backend.foodCatalog.controller;

import com.pedropacheco.nutrition.backend.foodCatalog.dto.FoodCatalogResponseDTO;
import com.pedropacheco.nutrition.backend.foodCatalog.dto.FoodItemDTO;
import com.pedropacheco.nutrition.backend.foodCatalog.service.FoodCatalogAggregatorService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FoodCatalogControllerTest {
    private final FoodCatalogAggregatorService service = mock(FoodCatalogAggregatorService.class);
    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new FoodCatalogController(service)).build();

    @Test
    void returnsFoodResultsIncludingBrand() throws Exception {
        FoodItemDTO item = new FoodItemDTO("off:12345678", "Oat Drink", "Acme Foods", "12345678",
                BigDecimal.valueOf(250), "g", BigDecimal.valueOf(45), BigDecimal.ONE,
                BigDecimal.valueOf(6), BigDecimal.valueOf(2), "PER_100_GRAMS");
        when(service.search("oat drink")).thenReturn(new FoodCatalogResponseDTO(List.of(item), null));

        mockMvc.perform(get("/api/foods").param("query", "oat drink"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].name").value("Oat Drink"))
                .andExpect(jsonPath("$.items[0].brand").value("Acme Foods"))
                .andExpect(jsonPath("$.items[0].barcode").value("12345678"));

        verify(service).search("oat drink");
    }

    @Test
    void returnsBadRequestWhenQueryIsMissing() throws Exception {
        mockMvc.perform(get("/api/foods")).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
