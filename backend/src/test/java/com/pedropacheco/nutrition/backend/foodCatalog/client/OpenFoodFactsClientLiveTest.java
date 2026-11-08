package com.pedropacheco.nutrition.backend.foodCatalog.client;

import com.pedropacheco.nutrition.backend.foodCatalog.adapter.OpenFoodFactsAdapter;
import com.pedropacheco.nutrition.backend.foodCatalog.dto.FoodItemMapper;
import com.pedropacheco.nutrition.backend.foodCatalog.factory.FoodItemFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** Optional live smoke test: run with OPENFOODFACTS_LIVE=true. */
@EnabledIfEnvironmentVariable(named = "OPENFOODFACTS_LIVE", matches = "true")
class OpenFoodFactsClientLiveTest {
    @Test
    void findsBarcodeWithNutritionViaFallback() {
        String baseUrl = System.getProperty("openfoodfacts.base-url", "https://world.openfoodfacts.net");
        String username = System.getProperty("openfoodfacts.staging-username", "off");
        String password = System.getProperty("openfoodfacts.staging-password", "off");
        var restClient = RestClient.builder().baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.USER_AGENT, "FitMaxxerLiveTest/1.0 (test@example.com)")
                .build();
        var client = new OpenFoodFactsClient(restClient, true, username, password);

        var items = new OpenFoodFactsAdapter(client,
                new FoodItemMapper(new FoodItemFactory())).search("5601312507994");

        assertEquals(1, items.size());
        assertNotNull(items.getFirst().getBarcode());
        assertEquals("5601312507994", items.getFirst().getBarcode().getValue());
        assertEquals(35, items.getFirst().getNutrientComposition().getCaloriesKcal().intValue());
    }
}
