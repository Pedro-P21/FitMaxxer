package com.pedropacheco.nutrition.backend.foodCatalog.controller;

import com.pedropacheco.nutrition.backend.foodCatalog.dto.FoodCatalogResponseDTO;
import com.pedropacheco.nutrition.backend.foodCatalog.service.FoodCatalogSearchService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/foods")
public class FoodCatalogController {
    private final FoodCatalogSearchService foodCatalogService;

    public FoodCatalogController(FoodCatalogSearchService foodCatalogService) {
        this.foodCatalogService = foodCatalogService;
    }

    @GetMapping
    public ResponseEntity<FoodCatalogResponseDTO> searchFood(@RequestParam String query) {
        return ResponseEntity.ok(foodCatalogService.search(query));
    }
}
