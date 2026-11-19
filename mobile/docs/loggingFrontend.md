# Today and food logging — US003, US004, US009

This increment implements food catalog browsing inside the logging flow, adding food to today's diary, and editing/moving/deleting today's items. There is no standalone catalog tab. US005 history and US010 meal duplication are covered in [Food journal and meal copying](historyFrontend.md). The backend endpoints and domain code are unchanged.

## User flow

1. From Today, tap **Log food**, an empty meal's **+**, or a populated meal's calorie total. A meal-specific action preselects that meal; the top-level action initially selects Breakfast, which can be changed before adding.
2. Search by name or tap **Scan barcode**.
3. Scanning opens the camera. A detected numeric product barcode opens a confirmation screen with its digits prefilled. **Find food** calls the catalog API; no photograph is uploaded.
4. Select a search result to view nutrition and choose a quantity, compatible unit, and destination meal.
5. Add the food. The server response replaces the displayed diary, and a short confirmation appears.
6. Tap a logged item to edit it. Saving a quantity returns to Today. Move and delete have their own selection/confirmation screens.

Swiping a meal header left reveals a green background with a white **+**. Completing the swipe (40% of the row width) opens the catalog with that meal selected, without removing the row. A partial swipe returns to rest. The calorie total or empty-meal **+** remains tappable.

Meal rows are compact. Empty meals show **Nothing logged yet** and **+**. A meal with one item shows only that food's name below the meal name, tappable for editing; its calories appear once as the add-food button on the right. Meals with multiple items show **Show logged items**, which toggles item rows inside the same card. Each expanded row shows the food, quantity, and calories and opens editing when tapped. Meal expansion is independent and persists while the Today screen remains mounted.

Barcode confirmation allows correcting the scanned digits. The camera supports common product barcode formats, suppresses repeated detections, stops while backgrounded, and is disposed when leaving the screen. If the camera cannot start, the screen explains the permission requirement and offers retry/cancel. A real camera is needed to verify recognition; the automated flow uses an injected decoded barcode.

The search UI has loading, empty, and connection-error states. Typing does not send catalog requests. Name searches run only when the user taps Search or submits the keyboard’s Search action. Duplicate submissions while the same search is loading are ignored. Barcode lookup runs only after Find food is tapped. Older responses cannot overwrite a newer search or a cleared query. No matching product is different from a failed request. Barcode matching keeps the original text for API requests, preserving leading zeroes, and recognises equivalent UPC/EAN numbers that differ only by leading zero padding. An unrelated returned product is not selected.

## Code and responsibilities

| File | Responsibility |
|---|---|
| `models/nutrition.dart` | Nutrient values, portion validation, supported-unit detection, and frontend nutrition previews |
| `models/catalog_food.dart` | Typed catalog food with serving metadata and nutrition basis |
| `models/daily_log.dart` | Typed diary, meals, and individually identifiable logged items |
| `services/food_catalog_service.dart` | Text/barcode lookup through the existing catalog endpoint |
| `services/daily_log_service.dart` | Read Today, add food, change quantity, move, and delete |
| `providers/food_catalog_provider.dart` | Explicit search, search state, and stale-response protection |
| `providers/daily_log_provider.dart` | Shared confirmed diary, read/write state, errors, and brief confirmations |
| `views/today_view.dart` | Render real totals and individually tappable meal items; start logging/editing flows |
| `views/food_search_view.dart` | Name search, search-result selection, loading/empty/error states, and scanner navigation |
| `views/barcode_scanner_view.dart` | Native camera lifecycle and local barcode decoding |
| `views/barcode_confirmation_view.dart` | Confirm/correct a scanned barcode and retrieve the matching food |
| `views/food_details_view.dart` | Portion selection, compatible units, live nutrition preview, and logging |
| `views/edit_log_item_view.dart` | Quantity editing in the existing unit and preview of the resulting daily total |
| `views/move_log_item_view.dart` | Choose another meal, with current totals and current meal indicated |
| `views/delete_log_item_view.dart` | Confirm deleting one entry and preview the daily total afterwards |
| `widgets/nutrition_card.dart` | Reusable nutrition cards and compact information/confirmation banners |

The shared `FlowScaffold` now accepts a contextual Back label. Existing buttons, inputs, dropdowns, theme, local icons, and SVG progress tracks are reused.

`main.dart` observes transitions away from the ready session stage and removes nested logging routes. An expired session therefore cannot leave an edit/search screen displayed above the Session Expired screen.

## Existing API calls

| Action | Method and path | Body |
|---|---|---|
| Search or barcode lookup | `GET /api/foods?query=...` | None; query encoded in the URL |
| Read Today | `GET /api/daily-log/today` | None |
| Add food | `POST /api/daily-log/today/items` | `foodId`, `mealType`, `quantity`, `quantityUnit` |
| Change amount | `PATCH /api/daily-log/today/items/{id}/quantity` | `quantity` |
| Move item | `PATCH /api/daily-log/today/items/{id}/meal` | `destinationMealType` |
| Delete entry | `DELETE /api/daily-log/today/items/{id}` | None |

All requests use the existing API client and bearer token. The catalog result already contains the metadata needed for food details; no extra details endpoint was added. The backend still validates permissions, current-day restrictions, amounts, nutrient scaling, and transactional integrity.

## Quantities and nutrition

Catalog and logged-item nutrient fields describe their nutrition basis: per 100 grams, per 100 milliliters, or per serving. They are not always the amount consumed. Frontend previews scale those snapshots using the same rules as the backend's nutrient scaling service:

- **Per 100 g:** `nutrients × grams / 100`.
- **Per 100 ml:** `nutrients × milliliters / 100`.
- **Per serving:** `nutrients × servings`.
- A mass/volume entry can be converted to servings only when the catalog supplies a compatible measured serving.
- No density conversion between grams and milliliters is invented. An unmeasured label such as “bowl” does not imply a weight.

For example, **1.5 × 100 g** is displayed as **150 g** and sent as `quantity: 150`, `quantityUnit: "GRAM"`. If the food contains 100 kcal per 100 g, the preview shows 150 kcal. A different food may offer servings only or milliliters instead of grams.

Validation requires a positive finite quantity, up to four decimal places, and fewer than 100 million base units, matching the stored amount's precision. Invalid amounts do not enable Add/Save. The API remains authoritative; preview values do not replace the summary returned after a write.

Editing retains the saved item's unit. **Move is disabled while there is an unsaved quantity change.** Save the amount first, then reopen the item to move it. Deleting an item uses its saved portion for the confirmation preview, even if an unsaved quantity was entered on the edit screen. Keep item returns without deleting anything.

Existing recipe entries can be edited/moved/deleted through these same logged-item operations if present in the diary. Recipe creation, browsing, and initially logging recipes are separate frontend work.

## Consistency and feedback

`DailyLogProvider` serialises local writes with its busy flag. Double taps do not start duplicate writes. A GET response started before a write cannot overwrite the newer mutation response. These are frontend safeguards; the backend's own locking and uniqueness protections remain in place.

A successful mutation uses the complete server-returned diary. It does not optimistically change local totals and then attempt to reconstruct the server result. On failure, the last confirmed diary remains available, and the user can retry or refresh. Writes are not automatically retried because adding food twice would create duplicate entries.

A successful add, quantity save, move, or delete shows a confirmation for approximately five seconds. Quantity confirmation includes the saved amount/unit. Move confirmation describes unchanged daily totals. Moving does not change the item's quantity or nutrition; the destination meal changes.

Deleting the final item uses the same delete request as other entries. The backend handles removal of an empty daily-log object and returns the empty Today summary. The frontend does not delete meals or daily-log objects itself. Adding food again uses the normal logging endpoint.

Today displays backend calorie/macro totals, per-item calories derived from saved snapshots, and the backend log date. Progress rings/bars cap visually at 100%, while numeric over-target amounts remain visible. Negative remaining macro values appear as red “g over” labels. A calorie-over-target message appears even when no action confirmation is currently showing. Pull-to-refresh and returning from the background reload Today.

There is no offline persistence or queued write mechanism in this increment. An interrupted write may require refreshing the diary to check the server's result before submitting again.

## Native configuration

Added [`mobile_scanner`](https://pub.dev/packages/mobile_scanner) for camera decoding. On iOS, both the normal and debug plist now explain camera use through `NSCameraUsageDescription`. Android declares camera permission and an optional camera hardware feature. No photo-library permission or image-upload backend was added.

The plugin can update generated platform registration files through Flutter dependency resolution. Existing signing/team configuration is retained.

## Design and verification

The Figma connector continued returning its Starter-plan call limit when retried. Implementation therefore uses the supplied screenshots, previously retrieved Figma component specifications, and existing local assets, as agreed. No claim of fresh access to the blocked frames or exact pixel equality is made. Rendered previews of search, empty/error states, details, edit, move, delete, and Today were reviewed; the meal layout was compacted after that review. Temporary preview code is removed after verification.

The final Flutter suite passed all **35 tests**. Automated coverage includes:

- Mass/volume/serving nutrition scaling and incompatible-unit rejection.
- Quantity precision and barcode format checks.
- Out-of-order searches and late GET responses.
- Duplicate-write prevention and retention of data after failed writes.
- Full search → log → quantity save → move → delete-final-item flow.
- Move being disabled before saving an amount.
- Barcode confirmation before lookup, preservation of leading zeroes, and rejection of an unrelated catalog result.
- Empty and connection-error UI, including retry.
- Multiple individual items, over-target totals, and session expiry inside a nested route.
- Existing authentication, onboarding, and shared-widget tests.

The HTTP tests use mocks; barcode flow tests inject a decoded barcode and do not exercise real camera recognition. The iOS simulator debug build with the camera plugin passed. The Android debug APK build also passed after the local Android tooling finished installing the missing NDK, SDK platforms, and CMake. The existing Gradle/AGP/Kotlin version advisories remain. Static analysis reports 24 informational Material import deprecation notices and no application errors or warnings; its exit status remains nonzero for those notices.

