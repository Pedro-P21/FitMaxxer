package com.pedropacheco.nutrition.backend.foodCatalog.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Provider-specific product fields; deliberately kept out of the domain package. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenFoodFactsProductDTO {

    private String code;

    @JsonProperty("product_name")
    private String productName;

    private String brands;

    @JsonProperty("serving_size")
    private String servingSize;

    @JsonProperty("serving_quantity")
    private String servingQuantity;

    @JsonProperty("serving_quantity_unit")
    private String servingQuantityUnit;

    @JsonProperty("nutrition_data_per")
    private String nutritionDataPer;

    private OpenFoodFactsNutrimentsDTO nutriments;

    public String getCode() { return code; }
    public String getProductName() { return productName; }
    public String getBrands() { return brands; }
    public String getServingSize() { return servingSize; }
    public String getServingQuantity() { return servingQuantity; }
    public String getServingQuantityUnit() { return servingQuantityUnit; }
    public String getNutritionDataPer() { return nutritionDataPer; }
    public OpenFoodFactsNutrimentsDTO getNutriments() { return nutriments; }

    public void setCode(String code) { this.code = code; }
    public void setProductName(String productName) { this.productName = productName; }
    public void setBrands(String brands) { this.brands = brands; }
    public void setBrand(String brand) { this.brands = brand; }
    public void setServingSize(String servingSize) { this.servingSize = servingSize; }
    public void setServingQuantity(String servingQuantity) { this.servingQuantity = servingQuantity; }
    public void setServingQuantityUnit(String servingQuantityUnit) {
        this.servingQuantityUnit = servingQuantityUnit;
    }
    public void setNutritionDataPer(String nutritionDataPer) { this.nutritionDataPer = nutritionDataPer; }
    public void setNutriments(OpenFoodFactsNutrimentsDTO nutriments) { this.nutriments = nutriments; }
}
