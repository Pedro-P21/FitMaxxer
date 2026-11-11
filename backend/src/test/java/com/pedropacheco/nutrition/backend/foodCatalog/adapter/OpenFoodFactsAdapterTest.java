package com.pedropacheco.nutrition.backend.foodCatalog.adapter;

import com.pedropacheco.nutrition.backend.foodCatalog.client.OpenFoodFactsClient;
import com.pedropacheco.nutrition.backend.foodCatalog.domain.FoodItem;
import com.pedropacheco.nutrition.backend.foodCatalog.dto.mapper.OpenFoodFactsMapper;
import com.pedropacheco.nutrition.backend.foodCatalog.dto.OpenFoodFactsProductDTO;
import com.pedropacheco.nutrition.backend.foodCatalog.dto.OpenFoodFactsSearchResponseDTO;
import com.pedropacheco.nutrition.backend.foodCatalog.factory.FoodItemFactory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class OpenFoodFactsAdapterTest {
    private final OpenFoodFactsClient client = mock(OpenFoodFactsClient.class);
    private final OpenFoodFactsAdapter adapter = new OpenFoodFactsAdapter(client,
            new OpenFoodFactsMapper(new FoodItemFactory()));

    @Test
    void routesBarcodeQueryToProductLookup() {
        OpenFoodFactsProductDTO product = new OpenFoodFactsProductDTO(null, null, null,
                null, null, null, null, null);
        when(client.findByBarcode("12345678")).thenReturn(product);

        adapter.search("12345678");

        verify(client).findByBarcode("12345678");
        verify(client, never()).search(anyString());
    }

    @Test
    void routesKeywordQueryToSearchEndpoint() {
        when(client.search("oat drink")).thenReturn(new OpenFoodFactsSearchResponseDTO(0, 1, 0, List.of()));

        List<FoodItem> items = adapter.search("oat drink");

        assertEquals(List.of(), items);
        verify(client).search("oat drink");
        verify(client, never()).findByBarcode(anyString());
    }
}
