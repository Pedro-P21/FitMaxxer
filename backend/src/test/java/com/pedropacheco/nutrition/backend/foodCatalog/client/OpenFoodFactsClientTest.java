package com.pedropacheco.nutrition.backend.foodCatalog.client;

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
                .andRespond(withSuccess("{\"status\":1,\"product\":{\"code\":\"12345678\","
                        + "\"product_name\":\"Oat Drink\",\"brands\":\"Acme Foods\"}}",
                        MediaType.APPLICATION_JSON));

        var product = client.findByBarcode("12345678");

        assertNotNull(product);
        assertEquals("Acme Foods", product.getBrands());
        server.verify();
    }
}
