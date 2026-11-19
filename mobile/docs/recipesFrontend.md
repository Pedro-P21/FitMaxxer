# Recipes — US006

The Recipes tab follows the supplied screenshots and the existing app theme. Cards omit the Breakfast favorite label and ingredient list. Ingredients remain visible in recipe details. All four bottom-navigation tabs are connected.

## Flow

1. Recipes lists the authenticated user's saved recipes, with yield and per-serving calories/macros. The list supports loading, empty, connection-error/retry, pull-to-refresh, and explicit Load more recipes pagination.
2. View recipe fetches the complete saved recipe and displays its original ingredients, their amounts and nutrition contributions, the whole-recipe calories, and per-serving nutrition. Saved recipes have no edit or delete actions.
3. Swipe a recipe card left to reveal a green background and white plus. Completing the swipe opens Log recipe directly, using the saved summary's per-serving nutrition without another detail request. A partial swipe returns to rest and never removes the card.
4. Create a recipe opens an in-memory draft with a name and catalog ingredients. Name validation requires 1–255 characters. Add catalog ingredient reuses the food search and camera/barcode confirmation flow in ingredient-selection mode. Typing does not send requests: Search/keyboard submission and Find food remain explicit actions.
5. Selecting a food opens amount and compatible-unit selection with a live nutrition preview. Adding an ingredient returns it to the draft and does not write to Today. The draft supports quantity/unit adjustments and removing individual ingredients before saving.
6. Continue to yield requires at least one valid ingredient. The yield screen defaults to one serving, accepts fractional yields, and previews total nutrition divided by yield. Back returns to the ingredient draft without losing the name, ingredients, amounts, units, or yield.
7. Save recipe sends only the name, yield, and ingredient catalog references/quantities/units. The backend resolves authoritative catalog nutrition and returns the immutable saved recipe. The confirmation screen offers View recipe and Back to Recipes. A failed save retains the draft; duplicate save taps are blocked while pending.
8. Log this recipe (or the swipe shortcut) opens serving quantity and destination-meal selection, initially one serving and Breakfast. Fractional servings scale the saved per-serving nutrition. Successful logging returns to Today with the complete server response and a green confirmation. Failed logging retains the amount and destination and leaves the previous Today snapshot unchanged.
9. Saved recipe logging remains compatible with the existing edit, move, delete, history, and whole-meal-copy flows. These operations change log entries, not the saved recipe. Session expiry closes nested draft/detail/log routes through existing root session navigation.

The draft is local to creation and is discarded when leaving the creation flow without saving or when the session ends. Saving a recipe does not automatically log it.

## API

| Request | Purpose |
|---|---|
| `GET /api/recipes?page=P&size=20` | Paginated recipe summaries |
| `GET /api/recipes/{recipeId}` | Complete saved recipe and ingredient snapshots |
| `POST /api/recipes` | Create an immutable recipe |
| `POST /api/daily-log/today/recipe-items` | Log a recipe in servings and return the updated Today log |

Create body: `name`, `numberOfServings`, and `ingredients` containing `foodId`, `quantity`, `quantityUnit`. Recipe logging body: `recipeId`, `mealType`, `quantity`. Nutrition calculated on the frontend is a preview; it is never submitted as authoritative nutrition.

Recipe ingredient snapshots contain nested nutrients in their original basis. Details scale those snapshots to the recorded ingredient amounts. Catalog serving conversions use the same existing portion rules as food logging; no gram-to-milliliter density is invented. The frontend uses the existing positive quantity validation (up to four decimal places, below 100,000,000) for draft amounts, yields, and logged servings. The backend's recipe creation supports a wider decimal range, while recipe logging uses the existing daily-log decimal limits.

No backend endpoint, domain model, or database change was required.

## Files

| File | Responsibility |
|---|---|
| `lib/models/recipe.dart` | Summaries, details, immutable-snapshot reading, paginated results, and editable draft data with nutrition previews |
| `lib/services/recipe_service.dart` | List, detail, and create API calls |
| `lib/providers/recipes_provider.dart` | Pagination/retry, creation state, duplicate-save guard, and stale-list protection |
| `lib/services/daily_log_service.dart` | Adds the existing recipe logging endpoint |
| `lib/providers/daily_log_provider.dart` | Recipe logging uses shared atomic snapshot replacement and confirmation behavior |
| `lib/views/recipes_view.dart` | Compact cards, swipe shortcut, creation entry, list states, and pagination |
| `lib/views/recipe_create_view.dart` | Name and ingredient draft, removal, and retained draft navigation |
| `lib/views/ingredient_amount_view.dart` | Selected catalog ingredient, quantity/unit controls, and contribution preview |
| `lib/views/recipe_yield_view.dart` | Yield, total/per-serving preview, immutable-save notice, and creation request |
| `lib/views/recipe_saved_view.dart` | Successful-save confirmation and subsequent navigation |
| `lib/views/recipe_details_view.dart` | Read-only saved ingredient and nutrition details |
| `lib/views/recipe_log_view.dart` | Servings, destination meal, preview, and log request |
| `lib/views/food_search_view.dart`, `barcode_confirmation_view.dart` | Existing logging flow plus ingredient-selection mode returning a draft ingredient |
| `lib/widgets/ingredient_editor.dart` | Shared editable ingredient amount and compatible-unit selection |
| `lib/widgets/nutrition_card.dart` | Optional decimal macro presentation for recipe previews; existing screens retain their previous presentation |
| `lib/widgets/journal_card.dart` | Rounded cards fill the available width consistently |
| `lib/views/today_view.dart`, `profile_view.dart`, `widgets/app_bottom_navigation.dart` | Recipes tab and navigation integration |
| `test/recipe_fixtures.dart` | Oats/milk catalog and recipe API examples |
| `test/recipe_provider_test.dart` | Calculation, payload, pagination/retry, save-race, and recipe logging integrity tests |
| `test/recipe_flow_test.dart` | Name/barcode ingredient selection, draft retention/removal, creation, details, fractional logging, swipe, retry, duplicate-save, and expiry flows |

## Verification

`flutter analyze` and `flutter test` are the project checks. Rendered previews of the recipe list, ingredient, yield, saved confirmation, details, and log screens were compared with the screenshots. Temporary preview code was removed after review.

For a phone check, create a recipe using name search and a scanned barcode, change quantities and return from yield to confirm draft retention, save it, inspect the saved details, and log half a serving. Confirm updated Today totals, then verify the recipe swipe shortcut. Camera recognition itself still requires a real camera; tests inject the decoded barcode.
