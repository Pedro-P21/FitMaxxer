package com.pedropacheco.nutrition.backend.foodCatalog.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Selected Open Food Facts nutrient keys, normalized to per-100g values. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenFoodFactsNutrimentsDTO {

    @JsonProperty("energy-kcal_100g")
    private String energyKcalPer100g;

    @JsonProperty("proteins_100g")
    private String proteinsPer100g;

    @JsonProperty("carbohydrates_100g")
    private String carbohydratesPer100g;

    @JsonProperty("fat_100g")
    private String fatPer100g;

    @JsonProperty("energy-kcal_serving")
    private String energyKcalPerServing;

    @JsonProperty("proteins_serving")
    private String proteinsPerServing;

    @JsonProperty("carbohydrates_serving")
    private String carbohydratesPerServing;

    @JsonProperty("fat_serving")
    private String fatPerServing;

    public String getEnergyKcalPer100g() { return energyKcalPer100g; }
    public String getProteinsPer100g() { return proteinsPer100g; }
    public String getCarbohydratesPer100g() { return carbohydratesPer100g; }
    public String getFatPer100g() { return fatPer100g; }
    public String getEnergyKcalPerServing() { return energyKcalPerServing; }
    public String getProteinsPerServing() { return proteinsPerServing; }
    public String getCarbohydratesPerServing() { return carbohydratesPerServing; }
    public String getFatPerServing() { return fatPerServing; }

    public void setEnergyKcalPer100g(String value) { energyKcalPer100g = value; }
    public void setProteinsPer100g(String value) { proteinsPer100g = value; }
    public void setCarbohydratesPer100g(String value) { carbohydratesPer100g = value; }
    public void setFatPer100g(String value) { fatPer100g = value; }
    public void setEnergyKcalPerServing(String value) { energyKcalPerServing = value; }
    public void setProteinsPerServing(String value) { proteinsPerServing = value; }
    public void setCarbohydratesPerServing(String value) { carbohydratesPerServing = value; }
    public void setFatPerServing(String value) { fatPerServing = value; }
}
