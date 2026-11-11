package com.pedropacheco.nutrition.backend.foodCatalog.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenFoodFactsProductDTO {
    private final String code;
    private final String productName;
    private final String brands;
    private final String servingSize;
    private final String servingQuantity;
    private final String servingQuantityUnit;
    private final String nutritionDataPer;
    private final OpenFoodFactsNutrimentsDTO nutriments;

    @JsonCreator
    public OpenFoodFactsProductDTO(
            @JsonProperty("code") String code,
            @JsonProperty("product_name") String productName,
            @JsonProperty("brands") String brands,
            @JsonProperty("serving_size") String servingSize,
            @JsonProperty("serving_quantity") String servingQuantity,
            @JsonProperty("serving_quantity_unit") String servingQuantityUnit,
            @JsonProperty("nutrition_data_per") String nutritionDataPer,
            @JsonProperty("nutriments") OpenFoodFactsNutrimentsDTO nutriments) {
        this.code = code;
        this.productName = productName;
        this.brands = brands;
        this.servingSize = servingSize;
        this.servingQuantity = servingQuantity;
        this.servingQuantityUnit = servingQuantityUnit;
        this.nutritionDataPer = nutritionDataPer;
        this.nutriments = nutriments;
    }

    public String getCode() { return code; }
    public String getProductName() { return productName; }
    public String getBrands() { return brands; }
    public String getServingSize() { return servingSize; }
    public String getServingQuantity() { return servingQuantity; }
    public String getServingQuantityUnit() { return servingQuantityUnit; }
    public String getNutritionDataPer() { return nutritionDataPer; }
    public OpenFoodFactsNutrimentsDTO getNutriments() { return nutriments; }
}
