# Food journal and meal copying — US005 and US010

The History tab uses the six supplied screenshots, the existing application theme, and the existing backend endpoints. It lists past days only. Today remains in its own tab. The Recipes tab is covered in [Recipes](recipesFrontend.md).

## User flow

1. Open History from Today or Profile. The initial month is the current month, using today's backend date when available. Subsequent visits retain the selected month for the current session.
2. Read daily cards showing date, calories, and macros, ordered newest first. Pull to refresh. Loading, connection failure with retry, and a successfully loaded empty month are distinct states.
3. Tap the month button to choose a year and month. Earlier/later year arrows navigate the supported range. Future months and months older than the backend's default ten-year window are disabled. Selection is applied only by the View button; going back cancels it without a request.
4. Empty history shows No logs here yet, Choose another date, and Go to Today. Choose another date opens the same month/year picker.
5. Open a past day to see its stored daily totals, meal totals, item names, quantities, and scaled item macros. These are read-only. Empty meal names are grouped into a card without a copy action. Every nonempty meal has Copy whole meal to today.
6. The copy screen previews the complete source meal and preselects its original meal type. Any of Breakfast, Lunch, Dinner, and Snacks can be chosen as the destination. Copying preserves all recorded items, quantities, and nutrition and appends to existing destination items.
7. Confirm Copy meal to [destination]. While saving, repeat taps, destination changes, and back navigation are blocked. A failed copy keeps the source and selected destination, shows the error, and allows retry without changing Today locally.
8. A successful response replaces Today's log with the returned snapshot. Both copy screens close and Today shows a green [source meal] copied to [destination] confirmation for five seconds. No extra GET is needed to calculate the new totals or to create the first log. The backend handles first-log creation.
9. A user may return to History and deliberately copy the same meal again. Past records are not modified. Session expiry removes nested past-day, picker, and copy routes through the existing root session handling.

## API

| Request | Purpose |
|---|---|
| `GET /api/daily-log/history?month=M&year=Y` | Authenticated monthly history; response contains `year`, `month`, and complete `logs` |
| `POST /api/daily-log/today/meal-copies` | Copies a complete historical meal; response is the updated Today log |

The copy body contains only `sourceDate` (`YYYY-MM-DD`), `sourceMealType`, and `destinationMealType`. The frontend does not reconstruct source items or call the catalog API. Existing `DailyLog`, `LoggedMeal`, and `LoggedItem` models read stored quantities and nutrition snapshots.

The history provider additionally excludes today/future dates from rendering and filters results to the selected month. Request sequencing prevents a late response from replacing a more recently selected month. The ten-year picker bound matches the backend's default `daily-log.history.max-lookback-years`; changing that backend configuration requires adjusting the frontend bound as well. No backend code was changed.

## Files

| File | Responsibility |
|---|---|
| `lib/models/log_history.dart` | Monthly history response and shared date/month labels |
| `lib/services/daily_log_service.dart` | Monthly history and copy requests in addition to existing logging requests |
| `lib/providers/log_history_provider.dart` | Selected month, sorting/filtering, bounds, loading/error state, stale-response protection |
| `lib/providers/daily_log_provider.dart` | Copy mutation using the existing atomic snapshot update, write guard, and confirmation handling |
| `lib/views/history_view.dart` | Monthly journal list, empty/loading/error states, refresh, month navigation, and opening past days |
| `lib/views/history_month_view.dart` | Year/month selection and valid date range |
| `lib/views/past_daily_log_view.dart` | Read-only daily and meal details with copy actions |
| `lib/views/copy_meal_view.dart` | Source preview, destination selection, confirmation, and failed-copy recovery |
| `lib/widgets/journal_card.dart` | Rounded white or selected pale-blue cards shared across these views |
| `lib/widgets/app_bottom_navigation.dart` | Enables History alongside Today and Profile |
| `lib/views/today_view.dart` | Owns retained history state, switches tabs, and displays the successful copy response |
| `lib/views/profile_view.dart` | Connects Profile to History |
| `test/history_provider_test.dart` | Filtering, bounds, request ordering, empty/error distinction, copy payload, write guard, and failed-write integrity |
| `test/history_flow_test.dart` | Read-only browsing, month selection/cancellation, loading/errors/retry, destination defaults, full copy flow, repeat copying, expiry, and tab navigation |

## Verification

The new tests cover the monthly history API, source meal preservation, backend-response totals, and navigation. Rendered previews of the journal, picker, past day, copy screen, and empty state were inspected against the screenshots; the temporary preview code was removed afterward. Run `flutter analyze` and `flutter test` from `mobile/` for the complete checks.

For a manual check, create a past-day log in development data, browse its month, open a nonempty meal, copy it into a different meal that already has items, and verify that Today contains both the previous and copied items with updated totals. Verify the same flow with an initially empty Today log and try another month with no records.
