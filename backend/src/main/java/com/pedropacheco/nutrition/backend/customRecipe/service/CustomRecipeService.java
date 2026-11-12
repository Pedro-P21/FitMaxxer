package com.pedropacheco.nutrition.backend.customRecipe.service;

import com.pedropacheco.nutrition.backend.customRecipe.domain.IngredientQuantity;
import com.pedropacheco.nutrition.backend.customRecipe.domain.RecipeId;
import com.pedropacheco.nutrition.backend.customRecipe.dto.*;
import com.pedropacheco.nutrition.backend.customRecipe.factory.CustomRecipeFactory;
import com.pedropacheco.nutrition.backend.customRecipe.repository.CustomRecipeRepository;
import com.pedropacheco.nutrition.backend.foodCatalog.service.FoodCatalogQueryService;
import com.pedropacheco.nutrition.backend.userAuth.domain.UserId;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class CustomRecipeService {
    private final FoodCatalogQueryService catalogQuery;
    private final CustomRecipeFactory factory;
    private final CustomRecipeRepository repository;
    private final CustomRecipeMapper mapper;
    private final TransactionTemplate transaction;

    public CustomRecipeService(FoodCatalogQueryService catalogQuery, CustomRecipeFactory factory,
                               CustomRecipeRepository repository, CustomRecipeMapper mapper, PlatformTransactionManager transactionManager) {
        this.catalogQuery = catalogQuery;
        this.factory = factory;
        this.repository = repository;
        this.mapper = mapper;
        this.transaction = new TransactionTemplate(transactionManager);
    }
    public CustomRecipeResponseDTO createRecipe(String authenticatedUserId, CreateRecipeRequestDTO request) {

        var owner = new UserId(authenticatedUserId);
        var resolved = request.ingredients().stream().map(selection ->
                new CustomRecipeFactory.ResolvedIngredient(catalogQuery.getFoodDetails(selection.foodId()),
                        new IngredientQuantity(selection.quantity(), selection.quantityUnit()))).toList();
        var recipe = factory.createRecipe(owner, request.name(), request.numberOfServings(), resolved);

        return transaction.execute(status -> mapper.toResponseDTO(repository.saveAndFlush(recipe)));
    }

    @Transactional(readOnly = true)
    public RecipePageResponseDTO listRecipes(String authenticatedUserId, int page, int size) {

        var owner = new UserId(authenticatedUserId);
        if (page < 0 || size < 1 || size > 100 || (long) page * size > Integer.MAX_VALUE)
            throw new IllegalArgumentException("Page must be non-negative and size must be between 1 and 100.");
        var pageable = PageRequest.of(page, size,
                Sort.by("name.value", "id.value"));
        return mapper.toPageResponseDTO(repository.findByUserId(owner.getValue(), pageable));

    }

    @Transactional(readOnly = true)
    public CustomRecipeResponseDTO getRecipe(String authenticatedUserId, String recipeId) {

        var owner = new UserId(authenticatedUserId);
        var id = new RecipeId(recipeId);
        return mapper.toResponseDTO(repository.findByIdAndUserId(id, owner.getValue())
                .orElseThrow(RecipeNotFoundException::new));

    }
}
