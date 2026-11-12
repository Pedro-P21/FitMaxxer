package com.pedropacheco.nutrition.backend.customRecipe.controller;

import com.pedropacheco.nutrition.backend.customRecipe.dto.CreateRecipeRequestDTO;
import com.pedropacheco.nutrition.backend.customRecipe.dto.CustomRecipeResponseDTO;
import com.pedropacheco.nutrition.backend.customRecipe.dto.RecipePageResponseDTO;
import com.pedropacheco.nutrition.backend.customRecipe.service.CustomRecipeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.net.URI;

@RestController
@RequestMapping("/api/recipes")
public class CustomRecipeController {
    private final CustomRecipeService service;
    public CustomRecipeController(CustomRecipeService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<CustomRecipeResponseDTO> createRecipe(@AuthenticationPrincipal Jwt authenticatedUser,
                                                               @RequestBody CreateRecipeRequestDTO request) {
        var response = service.createRecipe(authenticatedUser.getSubject(), request);
        return ResponseEntity.created(URI.create("/api/recipes/" + response.recipeId())).body(response);
    }

    @GetMapping
    public ResponseEntity<RecipePageResponseDTO> listRecipes(
            @AuthenticationPrincipal Jwt authenticatedUser,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.listRecipes(authenticatedUser.getSubject(), page, size));
    }

    @GetMapping("/{recipeId}")
    public ResponseEntity<CustomRecipeResponseDTO> getRecipe(@AuthenticationPrincipal Jwt authenticatedUser,
                                                            @PathVariable String recipeId) {
        return ResponseEntity.ok(service.getRecipe(authenticatedUser.getSubject(), recipeId));
    }
}
