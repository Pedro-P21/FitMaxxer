# Local Flutter / Backend Integration

This guide connects the Android and iOS app in `mobile/` to the Spring Boot API in `backend/`. All paths below assume the existing `FitMaxxer` repository layout. Flutter screens and application networking are implemented separately; these instructions describe the API and local connection setup.

## Start the backend

From `FitMaxxer/backend`, use Java 21 and provide the backend's signing secret through the shell or IDE environment:

```bash
export JWT_SECRET="$(openssl rand -hex 32)"
./gradlew bootRun
```

`JWT_SECRET` must be at least 32 UTF-8 bytes. It belongs only to the backend, never in Flutter. Keep the same local value across restarts if you want previously issued tokens to remain valid. Spring Boot does not automatically load this repository's `.env` file; an IDE/plugin may provide it, otherwise export the variable explicitly.

Default startup runs the REST API at `http://127.0.0.1:8080` without asking for terminal input. Stop it with Ctrl+C. The development database uses the existing file under `backend/data`; ordinary startup updates its schema without resetting its data.

Other local modes:

```bash
# Existing interactive menus plus the API
./gradlew bootRun --args='--spring.profiles.active=cli'

# Physical phones on the same local network; no interactive menus
./gradlew bootRun --args='--spring.profiles.active=mobile-dev'

# Optional: both modes
./gradlew bootRun --args='--spring.profiles.active=mobile-dev,cli'
```

`mobile-dev` binds the server to `0.0.0.0`. This is a listening address, not an address to put in the app. Use your computer's LAN IP in the app. `SERVER_PORT` or `--server.port=8081` can override the default port if another process uses it. All calls must point to the same running backend instance.

## Choose the app's API base URL

| Where the client runs | API base URL | Backend mode |
|---|---|---|
| Request examples / terminal on the Mac | `http://127.0.0.1:8080` | default |
| Android Studio emulator | `http://10.0.2.2:8080` | default |
| iOS Simulator on the same Mac | `http://127.0.0.1:8080` | default |
| Physical Android phone or iPhone | `http://<your-Mac-LAN-IP>:8080` | `mobile-dev` |

The base URL does not include `/api`. On an Android emulator, `10.0.2.2` is the host-loopback alias. On a physical phone, `localhost` is the phone itself. For a physical device, keep the Mac and phone on a network that allows device-to-device connections and allow Java's incoming connection if the Mac firewall prompts.

Use one configurable base URL in the Flutter API service, rather than hardcoding it in widgets. A later implementation can read `API_BASE_URL` with `String.fromEnvironment` and launch with, for example:

```bash
flutter run --dart-define=API_BASE_URL=http://10.0.2.2:8080
```

Passing this flag alone does not configure networking; the Flutter code must read it. No Dart changes are included in this backend-preparation step.

### Native platform setup

When implementing the client, declare Android's `android.permission.INTERNET`. Depending on the HTTP client/platform integration, local HTTP can require a development-only cleartext/ATS exception. Physical iOS LAN access also requires a meaningful `NSLocalNetworkUsageDescription` and the user's Local Network permission. Configure only the relevant debug/local settings while working with this HTTP backend.

References: [Android emulator addressing](https://developer.android.com/studio/run/emulator-networking-address), [Flutter network policies](https://docs.flutter.dev/release/breaking-changes/network-policy-ios-android), [Apple local network privacy](https://developer.apple.com/documentation/technotes/tn3179-understanding-local-network-privacy), [Apple local ATS exceptions](https://developer.apple.com/documentation/bundleresources/information-property-list/nsapptransportsecurity/nsallowslocalnetworking).

Native Android/iOS clients are not subject to browser CORS rules. CORS is not required for these targets.

## Check connectivity before logging in

```http
GET /api/health
```

This public endpoint returns HTTP `200` and:

```json
{"status":"UP"}
```

It checks that the HTTP application is reachable; it is not a database or external food-provider diagnostic. The other public endpoints are registration and login. Business endpoints require a bearer token.

## First integration flow

1. Register with `POST /api/auth/register`. A successful registration returns `201` with no body and does not automatically create a nutritional profile.
2. Log in with `POST /api/auth/login` using email or user ID in `identifier`.
3. Store the returned access token in the client session and attach it to protected requests. Use platform secure storage if persisting a session.
4. Call `GET /api/profiles`. A new account receives `404` until it creates a profile through `POST /api/profiles/personal-data`.
5. Open today's log with `GET /api/daily-log/today`. A profile is required for the empty view and first-log creation (`409` if missing).
6. Search foods, add an item, and display the returned updated daily log.

Login request:

```json
{"identifier":"mobile-dev-user","password":"MobileDevPass123!"}
```

Login response:

```json
{"token":"<access-token>","type":"Bearer"}
```

Protected-request headers:

```http
Authorization: Bearer <access-token>
Accept: application/json, application/problem+json
Content-Type: application/json
```

`Content-Type` is needed for JSON request bodies. GET requests use query parameters and have no body.

Access tokens last 15 minutes by default (`app.jwt.access-token-lifetime=PT15M`). There is no refresh-token or server logout endpoint. Clear the client session on logout. On a protected-request `401`, clear the unusable token and return to login; invalid login credentials also return `401` but should remain on the login screen. Authentication remains enforced by the backend.

## API contract

All paths are relative to the base URL. Successful responses are JSON unless marked empty. All user-specific lookups use the bearer token's identity; request bodies do not choose another owner.

| Method / path | Input | Successful response |
|---|---|---|
| `GET /api/health` | none | `200`: `{status}` |
| `POST /api/auth/register` | `{email, password, userId}` | `201`: empty |
| `POST /api/auth/login` | `{identifier, password}` | `200`: `{token, type}` |
| `GET /api/profiles` | none | `200`: full saved profile |
| `POST /api/profiles/personal-data` | `{age, weight, height, gender, activityLevel, goalType, bodyFatPercentage?, unitSystem}` | `200`: goal summary |
| `PUT /api/profiles` | selected fields: `{age?, weightKg?, heightCm?, gender?, bodyFatPercentage?, activityLevel?, calorieTarget?, macroBreakdown?, goalType?, unitSystem?}` | `200`: goal summary |
| `GET /api/foods?query=...` | food name or barcode; URL-encode the query | `200`: `{items, message}` |
| `GET /api/daily-log/today` | none | `200`: daily log |
| `POST /api/daily-log/today/items` | `{foodId, mealType, quantity, quantityUnit}` | `200`: updated daily log |
| `POST /api/daily-log/today/recipe-items` | `{recipeId, mealType, quantity}`; quantity is recipe servings | `200`: updated daily log |
| `PATCH /api/daily-log/today/items/{itemId}/quantity` | `{quantity}` in the recorded unit | `200`: updated daily log |
| `PATCH /api/daily-log/today/items/{itemId}/meal` | `{destinationMealType}` | `200`: updated daily log |
| `DELETE /api/daily-log/today/items/{itemId}` | none | `200`: updated daily log; final deletion returns an empty view |
| `GET /api/daily-log/history?month=...&year=...` | valid month/year in the supported history window | `200`: `{year, month, logs}` |
| `POST /api/daily-log/today/meal-copies` | `{sourceDate, sourceMealType, destinationMealType}` | `200`: updated daily log |
| `POST /api/recipes` | `{name, numberOfServings, ingredients:[{foodId, quantity, quantityUnit}]}` | `201`: saved recipe, plus `Location` header |
| `GET /api/recipes?page=0&size=20` | page >= 0, size 1–100 | `200`: `{items, page, size, totalElements, totalPages}` |
| `GET /api/recipes/{recipeId}` | own saved recipe ID | `200`: recipe details |

Profile creation uses `weight`/`height`; profile editing and reading use `weightKg`/`heightCm`. Read measurements are metric. Nullable edit fields mean no change; null body-fat values do not provide a clearing operation. `macroBreakdown` uses `{proteinGrams, carbsGrams, fatsGrams}`.

The full profile response contains `profileId`, `userId`, `age`, `weightKg`, `heightCm`, `gender`, nullable `bodyFatPercentage`, `activityLevel`, `activityFactor`, `maintenanceCalories`, `calorieTarget`, `proteinGrams`, `carbsGrams`, and `fatsGrams`. The UI can omit internal identifiers and the activity multiplier as the CLI does. Goal summaries contain `message`, `maintenanceCalories`, `calorieTarget`, `proteinGrams`, `carbsGrams`, and `fatsGrams`.

Food search items contain `foodId`, `name`, nullable `brand`/`barcode`, `servingQuantity`, `servingUnit`, calories/macros, and `nutrientBasis`. Reuse the exact returned `foodId`; do not infer it from the name or barcode. Supported logging units are `SERVING`, `GRAM`, and `MILLILITER`, subject to the food's compatible basis. Recipes are logged in servings. Named-portion creation is not exposed by the logging request.

Supported meal types are `Breakfast`, `Lunch`, `Dinner`, and `Snacks`. Each has one grouping per daily log. Dates use `YYYY-MM-DD`. Today comes from the backend clock/timezone, not the phone's date. History excludes today and uses a configurable month-based 10-year lookback by default. Copy a whole nonempty historical meal into any destination; existing entries remain and repeated copies add new entries.

A recipe detail contains `recipeId`, `name`, `numberOfServings`, `ingredients`, `totalNutrition`, and `perServingNutrition`. Recipe list items are summaries; request the detail endpoint for ingredient rows. Nutrition objects use `caloriesKcal`, `proteinGrams`, `carbohydrateGrams`, and `fatGrams`. The profile's macro field names differ (`carbsGrams`/`fatsGrams`), so use the appropriate DTO mapping.

### Daily-log response

```json
{
  "date": "2026-11-15",
  "meals": [
    {
      "type": "Breakfast",
      "items": [],
      "consumed": {"caloriesKcal": 0, "proteinGrams": 0, "carbohydrateGrams": 0, "fatGrams": 0}
    }
  ],
  "nutritionSummary": {
    "consumed": {"caloriesKcal": 0, "proteinGrams": 0, "carbohydrateGrams": 0, "fatGrams": 0},
    "targets": {"caloriesKcal": 2000, "proteinGrams": 150, "carbohydrateGrams": 200, "fatGrams": 67},
    "remaining": {"caloriesKcal": 2000, "proteinGrams": 150, "carbohydrateGrams": 200, "fatGrams": 67}
  }
}
```

The example abbreviates `meals`; the real response includes all four supported types. Each logged item contains `itemId`, `foodName`, nullable `brand`/`barcode`, `quantity`, `quantityUnit`, nullable `portionLabel`, `catalogServingQuantity`, `catalogServingUnit`, `caloriesKcal`, `proteinGrams`, `carbohydrateGrams`, `fatGrams`, and `nutrientBasis`.

**Item nutrient fields describe the stored nutrient basis, not necessarily the consumed contribution.** Use the meal's `consumed` and the daily `nutritionSummary` for progress totals. Quantities and nutrient values may be fractional. Negative `remaining` values mean a target was exceeded.

## Error responses

Errors use `application/problem+json`:

```json
{
  "type": "about:blank",
  "title": "Not Found",
  "status": 404,
  "detail": "No nutritional profile found. Create your profile first.",
  "instance": "/api/profiles"
}
```

Branch on the HTTP status and show `detail` as a user-facing explanation; do not depend on exact wording as an error identifier. Allow additional JSON fields. `401` responses for protected resources preserve the `WWW-Authenticate: Bearer ...` challenge; malformed bearer-token transport can return `400`.

| Status | Typical meaning |
|---|---|
| `400` | invalid JSON, invalid values/query parameters, unsupported unit/type/date, or duplicate registration validation |
| `401` | invalid login credentials, missing token, invalid token, or expired session |
| `403` | authenticated request lacks permission |
| `404` | missing or inaccessible resource, including a profile that has not been created |
| `409` | profile prerequisite missing, concurrent write conflict/timeout, or integrity conflict |
| `500` | unexpected server failure; details remain in server logs |
| `503` | food provider temporarily unavailable |

Treat connection failures separately from HTTP errors: the request may never have reached the server. Avoid automatically replaying mutations after an ambiguous network timeout, since the server may already have committed them. Deliberate repeated meal copies are valid additions. The backend's first-log retry only replays a transaction known to have rolled back.

## Request examples and checks

Open [api.http](api.http) in an HTTP request runner. Start with health, registration, login, profile creation, profile view, and today's log. Copy the login response's `token` into the file's `@token` variable. Other examples require IDs returned by food search, recipes, or logged items. Set history dates to real existing past logs. The file contains development example credentials, not account secrets.

From `backend/`, `./gradlew test` runs the startup, authentication/error-format, API, and existing business regression tests against isolated databases. Backend preparation does not modify Flutter application code or reset development data.

## Authentication and onboarding UI

Run from `mobile/` with the backend running and `JWT_SECRET` configured in its process:

```sh
flutter run
```

The default API address is `http://10.0.2.2:8080` on Android and
`http://127.0.0.1:8080` on the iOS simulator. For a physical phone, run the
backend's `mobile-dev` profile and pass the Mac's LAN address:

```sh
flutter run --dart-define=API_BASE_URL=http://192.168.1.42:8080
```

Replace the example IP with your Mac's address. Debug builds allow local HTTP.
On iOS, grant the local network permission when prompted. Release iOS builds use
`Info.plist` without the debug HTTP exception; development builds use
`Info-Debug.plist`.

Registration creates the account, logs in automatically and securely saves only
the access token. Existing sessions are restored until token expiry (the backend
currently has no refresh endpoint). A missing profile opens onboarding; other
profile errors offer retry. Finishing baseline and goal entry saves the profile
and displays the backend-calculated targets. The password minimum is checked
only on registration in the frontend.

Imperial inputs are converted to metric before the profile request. Goal labels
are defined in `lib/models/goal_option.dart`, retaining all seven backend values.
The targets screen's **Adjust targets** returns to goal selection; the Back
button there returns to baseline details. **Calculate my targets** saves the
profile; **Start my day** opens Today.

Today now supports searching/scanning and logging foods, quantity editing,
moving and deleting items. See [the logging walkthrough](loggingFrontend.md)
for US003/US004/US009. History, Recipes and Profile navigation remain inactive;
meal duplication (US010) is not included.

```sh
flutter test
flutter analyze
```

The current Flutter SDK reports informational deprecations for its Material
import; these are separate from application validation errors.

## Backend activity in the console

The backend logs each `/api/` request with a readable functionality name and a
request number, followed by its result and elapsed time. Restart the backend
after changing the code to enable the new filter. No frontend setting is needed.

```text
API #1 START Login
API #1 COMPLETED Login status=200 durationMs=85
API #2 START View nutritional profile
API #2 REJECTED View nutritional profile status=404 durationMs=4
API #3 START Save personal data and calculate targets
API #3 COMPLETED Save personal data and calculate targets status=200 durationMs=12
```

`START` means an API call was attempted, including calls rejected by security.
`COMPLETED` means the HTTP response was below 400; `REJECTED` indicates a 4xx
response; `FAILED` indicates a 5xx response or an exception escaping the filter.
A profile 404 can be expected for a new user who needs onboarding.

Request numbers are unique within the running backend process and restart from
one after a restart. Use the number to match starts and results when requests
interleave. Activity messages include no request/response bodies, tokens, passwords, user
identifiers, raw URLs, or query values. They describe HTTP activity, not internal
method calls or CLI operations. Non-API paths such as the H2 console are skipped.

The filter is
`backend/configuration/ApiActivityLoggingFilter.java` under the Java source
package. Its action list names the current endpoints; new unrecognised API paths
are logged as `Other API request`. It runs before Spring Security so rejected
protected requests are visible too.

To suppress these messages, set the following Spring configuration property:

```properties
logging.level.com.pedropacheco.nutrition.backend.configuration.ApiActivityLoggingFilter=OFF
```

## Saved API address for a physical phone

The API address can now be saved in `config/development.local.json` inside the
Flutter project:

```json
{
  "API_BASE_URL": "http://192.168.0.4:8080"
}
```

This local file is ignored by Git. It currently contains the development Mac's
LAN address; update it if the Mac changes networks or receives a different IP.
For a fresh checkout, copy `config/development.example.json` to
`config/development.local.json` and set the correct address.

With `mobile/` open as the IntelliJ/Android Studio project, select
**FitMaxxer — Phone**, select the physical device and click Run. That saved run
configuration passes the JSON file to Flutter. The existing local `main.dart`
run configuration has also been set to use the file. **FitMaxxer — Emulator**
keeps the built-in emulator/simulator addresses.

From a terminal in `mobile/`, the equivalent command is:

```sh
flutter run --dart-define-from-file=config/development.local.json
```

The file is consumed at build/run time, not loaded dynamically from the phone.
Changing it requires a full stop and rerun. Launching directly from Xcode does
not read this JSON automatically; launch through the saved Flutter configuration
or the Flutter command above to apply it. The backend must still run with the
`mobile-dev` profile, and FitMaxxer needs Local Network permission on iOS.
