package com.pedropacheco.nutrition.backend.foodCatalog.client;

import com.pedropacheco.nutrition.backend.foodCatalog.dto.OpenFoodFactsProductDTO;
import com.pedropacheco.nutrition.backend.foodCatalog.dto.OpenFoodFactsProductResponseDTO;
import com.pedropacheco.nutrition.backend.foodCatalog.dto.OpenFoodFactsSearchResponseDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

/** HTTP boundary for Open Food Facts. Full-text search is served by the legacy CGI endpoint. */
@Component
public class OpenFoodFactsClient {
    private static final String FIELDS = "code,product_name,brands,serving_size,serving_quantity,"
            + "serving_quantity_unit,nutrition_data_per,nutriments";
    private final RestClient restClient;
    private final boolean staging;
    private final String stagingUsername;
    private final String stagingPassword;

    @Autowired
    public OpenFoodFactsClient(
            @Value("${openfoodfacts.base-url}") String baseUrl,
            @Value("${openfoodfacts.staging}") boolean staging,
            @Value("${openfoodfacts.staging-username}") String stagingUsername,
            @Value("${openfoodfacts.staging-password}") String stagingPassword,
            @Value("${openfoodfacts.user-agent}") String userAgent) {
        this(RestClient.builder().baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.USER_AGENT, userAgent).build(), staging,
                stagingUsername, stagingPassword);
    }

    OpenFoodFactsClient(RestClient restClient, boolean staging,
                        String stagingUsername, String stagingPassword) {
        this.restClient = restClient;
        this.staging = staging;
        this.stagingUsername = stagingUsername;
        this.stagingPassword = stagingPassword;
    }

    public OpenFoodFactsSearchResponseDTO search(String query) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/cgi/search.pl")
                        .queryParam("search_terms", query)
                        .queryParam("search_simple", 1)
                        .queryParam("action", "process")
                        .queryParam("json", 1)
                        .queryParam("page_size", 24)
                        .queryParam("fields", FIELDS)
                        .build())
                .headers(this::applyStagingAuth)
                .retrieve().body(OpenFoodFactsSearchResponseDTO.class);
    }

    public OpenFoodFactsProductDTO findByBarcode(String barcode) {
        OpenFoodFactsProductResponseDTO response = restClient.get()
                .uri("/api/v3.6/product/{barcode}.json?fields={fields}", barcode, FIELDS)
                .headers(this::applyStagingAuth)
                .retrieve().body(OpenFoodFactsProductResponseDTO.class);
        if (response == null || !isSuccessfulStatus(response.getStatus())) {
            return null;
        }
        OpenFoodFactsProductDTO product = response.getProduct();
        if (hasRequiredNutrients(product)) {
            return product;
        }

        // v3.6 currently returns an empty nutriments object for some products
        // even though the same records have nutrition in v2 and keyword search.
        OpenFoodFactsProductResponseDTO legacyResponse = restClient.get()
                .uri("/api/v2/product/{barcode}.json?fields={fields}", barcode, FIELDS)
                .headers(this::applyStagingAuth)
                .retrieve().body(OpenFoodFactsProductResponseDTO.class);
        if (legacyResponse == null || !isSuccessfulStatus(legacyResponse.getStatus())) {
            return product;
        }
        OpenFoodFactsProductDTO legacyProduct = legacyResponse.getProduct();
        return hasRequiredNutrients(legacyProduct) ? legacyProduct : product;
    }

    private boolean hasRequiredNutrients(OpenFoodFactsProductDTO product) {
        if (product == null || product.getNutriments() == null) {
            return false;
        }
        var nutrients = product.getNutriments();
        if (hasCompleteValues(nutrients.getEnergyKcalPerServing(), nutrients.getProteinsPerServing(),
                nutrients.getCarbohydratesPerServing(), nutrients.getFatPerServing())) {
            return true;
        }
        if ("serving".equalsIgnoreCase(product.getNutritionDataPer())) return false;
        return hasCompleteValues(nutrients.getEnergyKcalPer100g(), nutrients.getProteinsPer100g(),
                nutrients.getCarbohydratesPer100g(), nutrients.getFatPer100g());
    }

    private boolean hasCompleteValues(String calories, String protein, String carbohydrates, String fat) {
        return calories != null && protein != null && carbohydrates != null && fat != null;
    }

    private boolean isSuccessfulStatus(Object status) {
        return status != null && ("success".equalsIgnoreCase(status.toString())
                || "1".equals(status.toString()));
    }

    private void applyStagingAuth(HttpHeaders headers) {
        if (staging) {
            String token = Base64.getEncoder().encodeToString(
                    (stagingUsername + ":" + stagingPassword).getBytes(StandardCharsets.UTF_8));
            headers.set(HttpHeaders.AUTHORIZATION, "Basic " + token);
        }
    }
}
