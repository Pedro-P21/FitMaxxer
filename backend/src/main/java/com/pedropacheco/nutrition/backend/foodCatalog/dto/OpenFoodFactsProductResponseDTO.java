package com.pedropacheco.nutrition.backend.foodCatalog.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Provider-specific envelope returned by the Open Food Facts product lookup. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenFoodFactsProductResponseDTO {
    private int status;
    private OpenFoodFactsProductDTO product;

    public int getStatus() { return status; }
    public OpenFoodFactsProductDTO getProduct() { return product; }
    public void setStatus(int status) { this.status = status; }
    public void setProduct(OpenFoodFactsProductDTO product) { this.product = product; }
}
