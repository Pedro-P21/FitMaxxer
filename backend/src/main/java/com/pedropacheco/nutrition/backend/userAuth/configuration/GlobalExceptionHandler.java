package com.pedropacheco.nutrition.backend.userAuth.configuration;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.pedropacheco.nutrition.backend.foodCatalog.service.FoodCatalogException;
import com.pedropacheco.nutrition.backend.dailyLog.service.NutritionalProfileRequiredException;
import com.pedropacheco.nutrition.backend.foodCatalog.service.FoodNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.util.PlaceholderResolutionException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(com.pedropacheco.nutrition.backend.customRecipe.service.RecipeNotFoundException.class)
    public ResponseEntity<String> handleRecipeNotFound(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(FoodNotFoundException.class)
    public ResponseEntity<String> handleFoodNotFound(FoodNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(NutritionalProfileRequiredException.class)
    public ResponseEntity<String> handleNutritionalProfileRequired(NutritionalProfileRequiredException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }

    @ExceptionHandler(FoodCatalogException.class)
    public ResponseEntity<String> handleFoodCatalogFailure(FoodCatalogException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("Food catalog is temporarily unavailable. Please try again later.");
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<String> handleBadCredentialsException(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleInvalidRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<String> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body("The request conflicts with existing data.");
    }

    @ExceptionHandler(PlaceholderResolutionException.class)
    public String handlePlaceholderResolutionException(PlaceholderResolutionException ex) {
        return "JWT variable not set";
    }
}
