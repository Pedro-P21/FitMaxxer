package com.pedropacheco.nutrition.backend.foodCatalog.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** Provider-specific response shape for the Open Food Facts search endpoint. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenFoodFactsSearchResponseDTO {
    private final int count;
    private final int page;
    private final int pageSize;
    private final List<OpenFoodFactsProductDTO> products;

    @JsonCreator
    public OpenFoodFactsSearchResponseDTO(
            @JsonProperty("count") Integer count,
            @JsonProperty("page") Integer page,
            @JsonProperty("page_size") Integer pageSize,
            @JsonProperty("products") List<OpenFoodFactsProductDTO> products) {
        this.count = count == null ? 0 : count;
        this.page = page == null ? 0 : page;
        this.pageSize = pageSize == null ? 0 : pageSize;
        this.products = products == null ? List.of() : List.copyOf(products);
    }

    public int getCount() { return count; }
    public int getPage() { return page; }
    public int getPageSize() { return pageSize; }
    public List<OpenFoodFactsProductDTO> getProducts() { return products; }
}
