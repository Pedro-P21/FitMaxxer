package com.pedropacheco.nutrition.backend.foodCatalog.client;

import com.pedropacheco.nutrition.backend.foodCatalog.dto.OpenFoodFactsProductDTO;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.http.HttpMethod.GET;
import static org.hamcrest.Matchers.containsString;

class OpenFoodFactsClientTest {
    private static final String BASE_URL = "https://off.test";
    private static final String USER_AGENT = "FitMaxxerTest/1.0 (test@example.com)";

    @Test
    void sendsKeywordSearchWithUserAgentAndStagingBasicAuth() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL)
                .defaultHeader(HttpHeaders.USER_AGENT, USER_AGENT);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenFoodFactsClient client = new OpenFoodFactsClient(builder.build(), true, "off", "off");
        server.expect(requestTo(containsString("/cgi/search.pl?search_terms=oat%20milk")))
                .andExpect(method(GET))
                .andExpect(header(HttpHeaders.USER_AGENT, USER_AGENT))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Basic b2ZmOm9mZg=="))
                .andRespond(withSuccess("{\"count\":0,\"products\":[]}", MediaType.APPLICATION_JSON));

        assertNotNull(client.search("oat milk"));
        server.verify();
    }

    @Test
    void looksUpBarcodeAndDeserializesBrandWithoutStagingAuthWhenDisabled() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL)
                .defaultHeader(HttpHeaders.USER_AGENT, USER_AGENT);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenFoodFactsClient client = new OpenFoodFactsClient(builder.build(), false, "", "");
        server.expect(requestTo(containsString("/api/v3.6/product/12345678.json")))
                .andExpect(method(GET))
                .andExpect(header(HttpHeaders.USER_AGENT, USER_AGENT))
                .andRespond(withSuccess("{\"code\":\"12345678\",\"errors\":[],"
                        + "\"product\":{\"code\":\"12345678\",\"product_name\":\"Oat Drink\","
                        + "\"brands\":\"Acme Foods\",\"serving_quantity\":1000,"
                        + "\"serving_quantity_unit\":\"ml\",\"nutriments\":{"
                        + "\"energy-kcal_100g\":40,\"proteins_100g\":1,"
                        + "\"carbohydrates_100g\":5,\"fat_100g\":2}},"
                        + "\"result\":{\"id\":\"product_found\"},"
                        + "\"status\":\"success\",\"warnings\":[]}",
                        MediaType.APPLICATION_JSON));

        var product = client.findByBarcode("12345678");

        assertNotNull(product);
        assertEquals("Acme Foods", product.getBrands());
        assertEquals("1000", product.getServingQuantity());
        server.verify();
    }

    @Test
    void fallsBackToV2WhenV36ProductHasNoNutriments() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL)
                .defaultHeader(HttpHeaders.USER_AGENT, USER_AGENT);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenFoodFactsClient client = new OpenFoodFactsClient(builder.build(), false, "", "");
        server.expect(requestTo(containsString("/api/v3.6/product/5601312507994.json")))
                .andRespond(withSuccess("{\"status\":\"success\",\"product\":{"
                        + "\"code\":\"5601312507994\",\"product_name\":\"Leite Magro\","
                        + "\"nutriments\":{}}}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(containsString("/api/v2/product/5601312507994.json")))
                .andRespond(withSuccess("{\"status\":1,\"product\":{"
                        + "\"code\":\"5601312507994\",\"product_name\":\"Leite Magro\","
                        + "\"nutrition_data_per\":\"100ml\",\"nutriments\":{"
                        + "\"energy-kcal_100g\":35,\"proteins_100g\":3.5,"
                        + "\"carbohydrates_100g\":5,\"fat_100g\":0.1}}}",
                        MediaType.APPLICATION_JSON));

        var product = client.findByBarcode("5601312507994");

        assertNotNull(product);
        assertEquals("35", product.getNutriments().getEnergyKcalPer100g());
        server.verify();
    }

    @Test
    void acceptsNormalizedPerServingNutrientsEvenWhenLabelBasisIsPerHundred() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL)
                .defaultHeader(HttpHeaders.USER_AGENT, USER_AGENT);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenFoodFactsClient client = new OpenFoodFactsClient(builder.build(), false, "", "");
        server.expect(requestTo(containsString("/api/v3.6/product/12345678.json")))
                .andRespond(withSuccess("{\"status\":\"success\",\"product\":{"
                        + "\"code\":\"12345678\",\"product_name\":\"Milk\",\"serving_size\":\"250 ml\","
                        + "\"nutrition_data_per\":\"100g\",\"nutriments\":{"
                        + "\"energy-kcal_serving\":87.5,\"proteins_serving\":8.75,"
                        + "\"carbohydrates_serving\":12.5,\"fat_serving\":0.25}}}",
                        MediaType.APPLICATION_JSON));

        OpenFoodFactsProductDTO product = client.findByBarcode("12345678");

        assertNotNull(product);
        assertEquals("87.5", product.getNutriments().getEnergyKcalPerServing());
        server.verify();
    }
}
