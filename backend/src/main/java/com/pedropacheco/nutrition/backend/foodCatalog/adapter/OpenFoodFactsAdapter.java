package com.pedropacheco.nutrition.backend.foodCatalog.adapter;

import com.pedropacheco.nutrition.backend.foodCatalog.client.OpenFoodFactsClient;
import com.pedropacheco.nutrition.backend.foodCatalog.domain.FoodItem;
import com.pedropacheco.nutrition.backend.foodCatalog.dto.FoodItemMapper;
import com.pedropacheco.nutrition.backend.foodCatalog.dto.OpenFoodFactsProductDTO;
import com.pedropacheco.nutrition.backend.foodCatalog.service.FoodCatalogProvider;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

@Component
public class OpenFoodFactsAdapter implements FoodCatalogProvider {
    private static final Pattern BARCODE = Pattern.compile("\\d{8,14}");
    private final OpenFoodFactsClient client;
    private final FoodItemMapper mapper;

    public OpenFoodFactsAdapter(OpenFoodFactsClient client, FoodItemMapper mapper) {
        this.client = client;
        this.mapper = mapper;
    }

    @Override
    public List<FoodItem> search(String query) {
        if (BARCODE.matcher(query).matches()) {

            OpenFoodFactsProductDTO product = client.findByBarcode(query);

            return product == null ? List.of() : mapper.toDomainItem(product).stream().toList();
        }
        return mapper.toDomainItems(client.search(query));
    }
}
