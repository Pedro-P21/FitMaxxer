package com.pedropacheco.nutrition.backend.foodCatalog.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Provider-specific envelope returned by the Open Food Facts product lookup. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenFoodFactsProductResponseDTO {
    // v3.6 returns "success"; older API responses used the numeric value 1.
    private final Object status;
    private final OpenFoodFactsProductDTO product;

    @JsonCreator
    public OpenFoodFactsProductResponseDTO(
            @JsonProperty("status") Object status,
            @JsonProperty("product") OpenFoodFactsProductDTO product) {
        this.status = status;
        this.product = product;
    }

    public Object getStatus() { return status; }
    public OpenFoodFactsProductDTO getProduct() { return product; }
}
