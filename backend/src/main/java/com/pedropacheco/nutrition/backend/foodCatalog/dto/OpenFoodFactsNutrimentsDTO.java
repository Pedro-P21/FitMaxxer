package com.pedropacheco.nutrition.backend.foodCatalog.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Selected Open Food Facts normalized nutrient keys for per-100g/ml and per-serving values. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenFoodFactsNutrimentsDTO {
    private final String energyKcalPer100g;
    private final String proteinsPer100g;
    private final String carbohydratesPer100g;
    private final String fatPer100g;
    private final String energyKcalPerServing;
    private final String proteinsPerServing;
    private final String carbohydratesPerServing;
    private final String fatPerServing;

    @JsonCreator
    public OpenFoodFactsNutrimentsDTO(
            @JsonProperty("energy-kcal_100g") String energyKcalPer100g,
            @JsonProperty("proteins_100g") String proteinsPer100g,
            @JsonProperty("carbohydrates_100g") String carbohydratesPer100g,
            @JsonProperty("fat_100g") String fatPer100g,
            @JsonProperty("energy-kcal_serving") String energyKcalPerServing,
            @JsonProperty("proteins_serving") String proteinsPerServing,
            @JsonProperty("carbohydrates_serving") String carbohydratesPerServing,
            @JsonProperty("fat_serving") String fatPerServing) {
        this.energyKcalPer100g = energyKcalPer100g;
        this.proteinsPer100g = proteinsPer100g;
        this.carbohydratesPer100g = carbohydratesPer100g;
        this.fatPer100g = fatPer100g;
        this.energyKcalPerServing = energyKcalPerServing;
        this.proteinsPerServing = proteinsPerServing;
        this.carbohydratesPerServing = carbohydratesPerServing;
        this.fatPerServing = fatPerServing;
    }

    public String getEnergyKcalPer100g() { return energyKcalPer100g; }
    public String getProteinsPer100g() { return proteinsPer100g; }
    public String getCarbohydratesPer100g() { return carbohydratesPer100g; }
    public String getFatPer100g() { return fatPer100g; }
    public String getEnergyKcalPerServing() { return energyKcalPerServing; }
    public String getProteinsPerServing() { return proteinsPerServing; }
    public String getCarbohydratesPerServing() { return carbohydratesPerServing; }
    public String getFatPerServing() { return fatPerServing; }
}
