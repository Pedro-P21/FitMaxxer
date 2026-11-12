### US003 - Food Catalog

## 1. Requirements Engineering

##### 1.1. User Story Description
As a User, I want to be able to search through a catalog of multiple foods, so that I can find and select items to log.

##### 1.2. Use Case Specifications
**From the user story:**
- The user can input a search query (food name, brand, or barcode) to search through the food catalog.
- The application queries the Open Food Facts API and maps the external data into domain entities (`FoodItem`) and value objects (`FoodId`, `FoodName`, `Barcode`, `NutrientComposition`, `ServingSize`).
- The user receives a unified list of matching food items displaying their name, barcode, serving size, and nutritional composition.
- The user can select a specific food item from the results to view its details or select it for logging.

**From the glossary and domain analysis:**
- **FoodItem (Entity Root):** The core domain entity representing a consumable food item with a unique identity (`FoodId`).
- **FoodId (Value Object):** The unique identifier for a food item.
- **FoodName (Value Object):** The validated name/description of the food item.
- **Barcode (Value Object):** The optional product barcode used for scanning.
- **NutrientComposition (Value Object):** The breakdown of calories and macronutrients (protein, carbs, fats) per serving or per 100g.
- **ServingSize (Value Object):** The standardized portion size associated with the food item's nutritional values.

##### 1.3. Acceptance Criteria

- **AC1:** The user must be able to search for food items by keyword or barcode.
- **AC2:** The application must aggregate search results from multiple configured food data sources.
- **AC3:** Search results must be normalized into a unified DTO format regardless of the originating data source.
- **AC4:** If no food items match the search query, the user must receive a clear informational message.
- **AC5:** The user must be able to select a food item from the results list to inspect its complete nutritional breakdown.
- **AC6:** The search and selection process must be fast and responsive, handling external API timeouts gracefully.

##### 1.4. Found out Dependencies
- **US008 (Authentication and Account Management):** The user must be authenticated to perform searches and interact with logging features.
- **External Network Access:** Required for querying external food catalog APIs.

##### 1.5. Input and Output Data
**Input Data:**
- Search term (string) or barcode (string).
- Optional filters (e.g., source preference, category).

**Output Data:**
- A list of matching food items with summary nutritional data.
- Detailed nutritional profile of a selected food item.
- Error or empty state messages when searches yield no results or upstream APIs fail.

##### 1.6. System Sequence Diagram
![US003_SSD.svg](US003_SSD.svg)


##### 1.7. Assumptions and Decisions
- External API responses will be mapped to internal domain value objects/DTOs to avoid tight coupling with third-party schemas.
- Search responses are cached by normalized query. `FoodCatalogSearchService` also populates food details by provider-qualified food ID on both search-cache hits and misses. Each cache has a 500-entry limit and expires entries 24 hours after write; neither performs background refresh.
- More than one API´s may be used simultaneously to provide a comprehensive catalog. 

## 2. Analysis

### 2.1. Relevant Domain Model Excerpt
![food_catalog_aggregate.svg](../global-artifacts/domainModel/domainModelExcerpts/food_catalog_aggregate.svg)

## 3. Design

### 3.1. Rationale

| Architectural Component / Pattern | Choice & Implementation                                                                 | Rationale & Benefit                                                                                         |
|:----------------------------------|:----------------------------------------------------------------------------------------|:------------------------------------------------------------------------------------------------------------|
| **Adapter Pattern**               | Dedicated client adapters for each external food API (`OpenFoodFactsAdapter`).          | Isolates third-party API specifics, authentication rules, and payload parsing from the core business logic. |
| **Facade / Aggregator Service**   | `FoodCatalogSearchService` populates food details; the cached aggregator queries configured providers.       | Unifies multi-source search into a single coherent operation for the application layer.                     |
| **Data Normalization**            | Map diverse external JSON schemas into a unified internal `FoodItemDTO` / domain model. | Ensures consistent data representation for the UI and downstream logging use cases (US002/US004).           |

### 3.2. Design Decisions & Architecture
1. **Decoupled Integration:** Each external food source operates independently behind a common interface, allowing new providers to be added or removed without impacting the application core.
2. **Resilience & Fault Tolerance:** Integration with external APIs includes timeout configurations and exception handling so that a failure in one provider does not crash the entire search transaction.

### 3.3. Sequence Diagram (SD)
![US003_SD.svg](US003_SD.svg)


## 4. External Food Catalog APIs & Multi-Source Architecture

### 4.1. Overview of Integrated APIs
Currently FitMaxxer will only implement one food catalog API, although additional sources will be added in the future:
1. **Open Food Facts:** Global crowd-sourced database, ideal for packaged goods and barcode scanning worldwide.

The system is preppared to support multiple external APIs, allowing for a more diverse and comprehensive catalog, therefore the following diagram illustrates the overall architecture.


### 4.2. How It Works (Architectural Flow)

```
User Search Query ("Bread" or Barcode)
↓
FoodCatalogController
↓
FoodCatalogSearchService
↓
Spring search cache → FoodCatalogAggregatorService (on a miss)
├── OpenFoodFactsClient ──> [Open Food Facts API]
├── AnotherAPIClient ──> [Another API]
↓
Result Normalization & Deduplication
↓
Unified FoodCatalogResponseDTO
↓
FoodCatalogSearchService → FoodDetailsCache (each food by ID, on hits and misses)
↓
Returned to User/UI
```


## 5. Tests

### 5.1. Domain Validations (Bottom-Up Approach)
- Test query sanitization and barcode format validation.

### 5.2. Application Layer & Integration Tests
- Test aggregator behavior when all APIs succeed.
- Test graceful degradation when one or more external API clients time out or return errors.
- Test DTO mapping consistency across different source payloads.
- Verify keyword searches populate food details, cached searches repopulate evicted details, and missing/expired details fall back to catalog lookup.
- Test Open Food Facts barcode response parsing and the nutrition-data fallback.
- An opt-in live test verifies barcode `5601312507994` through the client, adapter, and domain mapper (`OPENFOODFACTS_LIVE=true`).


## 6. Implementation

### 6.1. Overall Flow
1. **User Request:** The user submits a search string or barcode via the UI/CLI.
2. **Orchestration:** The controller delegates to `FoodCatalogSearchService`, which calls the Spring-cached `FoodCatalogAggregatorService`. A search-cache hit bypasses the aggregator method body.
3. **Provider Dispatch:** On a cache miss, the aggregator queries configured providers (currently Open Food Facts).
4. **Normalization & Aggregation:** Provider responses are mapped to domain foods, merged, deduplicated, and mapped to the cached response DTO.
5. **Details Caching & Presentation:** The search facade caches each returned food by ID, even when the response came from the search cache. The UI presents the returned foods. Later US004/US006 lookups read cached details first, falling back to catalog lookup when needed.

### 6.2. Key Methods and Classes

| Class                           | Method              | Responsibility                                                               |
|:--------------------------------|:--------------------|:-----------------------------------------------------------------------------|
| `FoodCatalogController`         | `searchFood(query)` | Entry point for catalog search requests.                                     |
| `FoodCatalogSearchService` | `search(query)` | Delegates to the cached aggregator and populates food details from every returned response. |
| `FoodCatalogAggregatorService` | `search(query)` | Queries providers on search-cache misses, merges results, and returns normalized response DTOs. |
| `FoodDetailsCache` | `get(foodId)` / `put(food)` | Reads and stores normalized food definitions by complete provider-qualified ID. |
| `FoodCatalogQueryService` | `getFoodDetails(foodId)` | Reads cached details first; falls back to the search facade on a miss. |
| `OpenFoodFactsAdapter` (et al.) | `fetchFoods(query)` | Communicates with the specific external API and maps raw data.               |
| `FoodItemDTO`                   | N/A                 | Represents the unified, standardized food item structure across all sources. |

### 6.3. Open Food Facts Barcode Lookup Notes
- The v3.6 product endpoint returns `status: "success"`; the response DTO accepts this string and the legacy numeric status `1`.
- Some v3.6 product responses have an empty `nutriments` object even when the same product has nutrition data in Open Food Facts keyword search and v2.
- `OpenFoodFactsClient.findByBarcode` checks for the four required macronutrient values in the product's declared basis. If they are missing, it retries the lookup against the v2 product endpoint and uses that product when the required values are present. This preserves the v3.6 lookup while handling records whose nutrition is only available through v2.
- Verified with barcode `5601312507994` (Leite Magro, Continente Equilíbrio): v3.6 returned product metadata and serving size but empty nutrients; v2 returned calories, protein, carbohydrates, and fat. The live end-to-end test confirms the adapter maps it into a `FoodItem` with the expected barcode and 35 kcal.
