package com.pedropacheco.nutrition.backend.foodCatalog.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Provider-specific envelope returned by the Open Food Facts product lookup. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenFoodFactsProductResponseDTO {
    // v3.6 returns "success"; older API responses used the numeric value 1.
    private Object status;
    private OpenFoodFactsProductDTO product;

    public Object getStatus() { return status; }
    public OpenFoodFactsProductDTO getProduct() { return product; }
    public void setStatus(Object status) { this.status = status; }
    public void setProduct(OpenFoodFactsProductDTO product) { this.product = product; }
}
