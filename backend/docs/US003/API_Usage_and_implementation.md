# US003 Food Catalog Implementation

This document describes the food catalog implementation currently in the backend. It follows US003's search flow and normalizes Open Food Facts data into FitMaxxer types before returning it to the caller.

## Request and response

The endpoint is `GET /api/foods?query={keyword-or-barcode}`. The request is currently represented by the Spring `@RequestParam` on `FoodCatalogController.searchFood`; there is no separate request DTO class. The endpoint requires authentication under the existing security configuration.

Example:

```http
GET /api/foods?query=oat%20milk
Authorization: Bearer <access-token>
```

The response body is a `FoodCatalogResponseDTO` containing an `items` list and an optional informational `message`. Each item is a `FoodItemDTO` containing the catalog ID, name, optional brand, barcode, serving size, calories, protein, carbohydrates, fat, and the nutrient measurement basis.

## End-to-end flow

1. **HTTP request to controller.** `FoodCatalogController` receives the `query` parameter and delegates it to `FoodCatalogAggregatorService`.
2. **Validation and cache lookup.** The service rejects blank queries. Spring Cache uses the trimmed, lower-case query as the key in `foodSearchCache`. A cache hit returns the previously normalized response without making an upstream request.
3. **Provider dispatch.** On a cache miss, the service calls each registered `FoodCatalogProvider`. `OpenFoodFactsAdapter` is the current provider; the interface allows additional providers to be added later.
4. **Choose Open Food Facts endpoint.** The adapter treats an 8–14 digit query as a barcode and calls the v3.6 product lookup endpoint. Other queries use Open Food Facts' full-text CGI search endpoint. Results are limited to 24 and request only the fields needed by the mapper.
5. **HTTP and staging behavior.** `OpenFoodFactsClient` sends the configured User-Agent. Staging is the default and adds HTTP Basic credentials `off:off`; requests use the staging host. These credentials are Open Food Facts staging access credentials, not a global contributor account. A global account is relevant to authenticated write operations; this feature only reads products.
6. **Provider DTO to domain.** Jackson deserializes the provider payload into `OpenFoodFactsSearchResponseDTO` / `OpenFoodFactsProductDTO` (or the product response envelope for barcode lookup). `FoodItemMapper` selects usable fields, parses nutrient values and serving quantity, and calls `FoodItemFactory` to create validated `FoodItem`, `FoodId`, `FoodName`, optional `Brand` and `Barcode`, `NutrientComposition`, and `ServingSize` objects. Products with missing or invalid required values are skipped.
7. **Aggregate and deduplicate.** The aggregator combines provider results and deduplicates by `FoodId`, preserving the first occurrence. If a provider fails while another succeeds, available results are returned. If every provider fails, `FoodCatalogException` is translated to HTTP 503. Failed calls are not cached; successful empty results are cached and have an explanatory message.
8. **Domain to response DTO.** `FoodCatalogResponseMapper` converts domain values into the unified `FoodItemDTO` shape and wraps the items in `FoodCatalogResponseDTO`. Spring serializes that DTO as JSON, and the controller returns HTTP 200.

```text
GET /api/foods?query=...
  -> FoodCatalogController (@RequestParam query)
  -> FoodCatalogAggregatorService (validate, cache, aggregate, deduplicate)
  -> FoodCatalogProvider
  -> OpenFoodFactsAdapter (barcode or keyword route)
  -> OpenFoodFactsClient (HTTP request and provider response DTO)
  -> FoodItemMapper -> FoodItemFactory -> List<FoodItem>
  -> FoodCatalogResponseMapper
  -> FoodCatalogResponseDTO { items: [FoodItemDTO...], message? }
```

## Configuration

The defaults are in `src/main/resources/application.properties`. Environment variables can override them:

| Property | Environment variable | Default |
|---|---|---|
| `openfoodfacts.base-url` | `OPENFOODFACTS_BASE_URL` | `https://world.openfoodfacts.net` |
| `openfoodfacts.staging` | `OPENFOODFACTS_STAGING` | `true` |
| `openfoodfacts.staging-username` | `OPENFOODFACTS_STAGING_USERNAME` | `off` |
| `openfoodfacts.staging-password` | `OPENFOODFACTS_STAGING_PASSWORD` | `off` |
| `openfoodfacts.user-agent` | `OPENFOODFACTS_USER_AGENT` | `FitMaxxer/1.0 (contact@fitmaxxer.com)` |


## Main classes

- `foodCatalog/controller/FoodCatalogController`: HTTP endpoint.
- `foodCatalog/service/FoodCatalogAggregatorService` and `FoodCatalogProvider`: provider orchestration and extension point.
- `foodCatalog/adapter/OpenFoodFactsAdapter`: source-specific routing and normalization boundary.
- `foodCatalog/client/OpenFoodFactsClient`: HTTP calls, User-Agent, and staging authentication.
- `foodCatalog/dto/OpenFoodFacts*DTO`: external response shapes.
- `foodCatalog/domain/Brand`: optional brand value object retained on `FoodItem` and returned in `FoodItemDTO`.
- `foodCatalog/dto/FoodItemMapper` and `foodCatalog/factory/FoodItemFactory`: provider-to-domain conversion and validation.
- `foodCatalog/dto/FoodCatalogResponseMapper`, `FoodCatalogResponseDTO`, and `FoodItemDTO`: domain-to-client response conversion.
- `foodCatalog/service/FoodCatalogException`: all-provider failure, mapped to 503 by `GlobalExceptionHandler`.
