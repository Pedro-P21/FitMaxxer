# Profile viewing and editing

The Profile tab follows the supplied profile screenshot using the shared app theme. It shows the real user ID, physical baseline, activity, inferred goal, and calorie/macro targets. It omits Review session status, profile ID, and activity multiplier. The API does not return a display name or email, so these are not fabricated. Measurements use metric units, matching the profile API response.

## Flow

- Tap Profile in the bottom navigation to load `GET /api/profiles`. Pull to refresh or retry after an error.
- Edit physical data opens a prefilled form for age, gender, weight, height, optional body fat, activity, and goal. Saving uses `PUT /api/profiles` with metric measurements. The server recalculates nutritional targets. The form explains that a calculated macro split replaces custom macros.
- Edit daily targets allows a calorie target alone, letting the backend recalculate macros when calories change, or manually setting protein, carbs, and fat. Manual macro calories must match the target within the backend's 15 kcal tolerance. If calories are unchanged and no macros are supplied, existing macros remain unchanged.
- Successful saves update the profile card and session cache and show a green confirmation. Failed saves retain the form and the previously saved profile. Duplicate saves are blocked while saving.
- Returning to Today reloads the daily log so updated targets are reflected in its summary. Past daily logs retain their snapshots according to existing backend behavior.
- Log out clears secure token storage, cancels the expiry timer, clears the in-memory token/profile, and returns to signed-out navigation. If secure storage cannot be cleared, the screen reports the failure instead of claiming logout succeeded.
- Session expiry removes any open profile editor using the existing root navigation protection.

## Existing API limits

`GET /api/profiles` provides a complete profile. `PUT /api/profiles` returns nutritional targets only. `ProfileService.update` combines those authoritative targets with the previous profile and successfully submitted physical fields. It does not require a second request to confirm a successful save.

The backend does not persist a separate goal identifier. A standard goal label is inferred from the difference between target and maintenance calories. Other differences are labelled Custom targets. Physical editing retains a custom calorie target when selected.

A null body-fat change means unchanged to the backend. Therefore an existing percentage can be edited but cannot be cleared; the form states this and validates it. An initially absent value can remain absent. No backend endpoint or domain logic was changed.

## Files

| File | Responsibility |
|---|---|
| `lib/views/profile_view.dart` | Profile fetching, cards, retry, confirmation, and logout |
| `lib/views/profile_edit_view.dart` | Physical-data and target forms, validation, and save handling |
| `lib/widgets/app_bottom_navigation.dart` | Shared Today/Profile navigation with selected icon colors |
| `lib/views/today_view.dart` | Opens Profile while retaining Today state and refreshes the log on return |
| `lib/models/profile.dart` | User ID, complete profile serialization, and inferred standard goal |
| `lib/services/profile_service.dart` | Existing GET/POST plus PUT update and response merging |
| `lib/providers/session_provider.dart` | Accepts updated profiles and clears login state on logout |
| `test/profile_flow_test.dart` | View/edit flows, request payloads, validation, failed-save retry, body-fat limitation, expiry, navigation, and logout |

Today, History, Recipes, and Profile are selectable. See [Recipes](recipesFrontend.md) for recipe behavior. See [Food journal and meal copying](historyFrontend.md) for History behavior.
