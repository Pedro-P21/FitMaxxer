package com.pedropacheco.nutrition.backend.foodCatalog.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/** Provider-specific response shape for the Open Food Facts search endpoint. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenFoodFactsSearchResponseDTO {

    private int count;
    private int page;

    @JsonProperty("page_size")
    private int pageSize;

    private List<OpenFoodFactsProductDTO> products = new ArrayList<>();

    public int getCount() { return count; }
    public int getPage() { return page; }
    public int getPageSize() { return pageSize; }
    public List<OpenFoodFactsProductDTO> getProducts() { return products; }

    public void setCount(int count) { this.count = count; }
    public void setPage(int page) { this.page = page; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }
    public void setProducts(List<OpenFoodFactsProductDTO> products) {
        this.products = products == null ? new ArrayList<>() : products;
    }
}
