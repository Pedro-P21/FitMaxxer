# Frontend implementation: what changed and how it works

> This walkthrough records the initial authentication/onboarding increment.
> Today, food logging, and item actions have since been implemented; see
> [US003/US004/US009 frontend changes](loggingFrontend.md) for the current logging flow.

This document explains the changes made when preparing FitMaxxer for Flutter and implementing the first screens from Figma. It covers the shared components, authentication, session persistence, onboarding, and the Today destination. The earlier backend preparation is documented separately in section 11 so it is clear which changes belong to which increment.

The latest screen implementation changed the frontend and its native platform configuration. It reused the existing backend endpoints and JWT authentication. It did not introduce a new backend session table, change the database schema, or implement refresh tokens.

## 1. What the application does now

I replaced the Flutter counter example with this flow:

```text
App starts
  ├─ No saved token → Welcome → Register or Login
  ├─ Expired/unreadable token → Session Expired → Login
  └─ Saved token with a future expiry → Request profile from backend
       ├─ Profile exists → Today
       ├─ Profile is missing (404) → Onboarding
       ├─ Token rejected (401) → Session Expired
       └─ Other failure → Error with a retry button

Registration → Create account → Login automatically → Request profile
Onboarding → Baseline → Activity and goal → Save profile and show targets → Today
```

An unreadable token means its expected JWT structure or expiry cannot be read, rather than a failure to access secure storage. A secure-storage read failure opens the retry screen.

Today displays real data returned by the daily-log API. Its food logging action and History, Recipes, and Profile tabs are not connected yet. Those require their own frontend screens and flows.

## 2. How the code is organised

I kept the folder structure we discussed instead of introducing an `app/` folder.

| Folder | Responsibility | Example |
|---|---|---|
| `lib/models/` | Represent profile data and selectable goals | `UserProfile`, `ProfileDraft`, `GoalOption` |
| `lib/providers/` | Coordinate session state and application flow | `SessionProvider` |
| `lib/services/` | Send API requests and access secure storage | `AuthService`, `ProfileService`, `ApiClient`, `SecureTokenStore` |
| `lib/views/` | Build complete screens and handle local form interaction | `AuthView`, `OnboardingView` |
| `lib/widgets/` | Provide reusable visual components | `AppButton`, `AppTextField`, `FlowScaffold` |
| `lib/theme/` | Define shared colours, typography, gradients, and dimensions | `AppColors`, `AppTheme` |

For example, submitting Login follows this path:

```text
AuthView._submit()
  → SessionProvider.authenticate()
    → AuthService.login()
      → ApiClient.request()
        → POST /api/auth/login
```

These are frontend responsibilities. The backend remains responsible for authentication, ownership checks, persistence, domain rules, and nutritional calculations. I did not reproduce its DDD aggregates or factories in Flutter.

`SessionProvider` extends Flutter's `ChangeNotifier`. Calling `notifyListeners()` tells the application to rebuild the affected UI. `main.dart` listens through `ListenableBuilder`; no additional `provider` package was added.

Local details such as the selected onboarding step and form controllers remain in the views. The provider holds the session, current profile, onboarding draft, loading status, and application errors.

## 3. Shared styling and components

### `AppColors` — `lib/theme/app_colors.dart`

I filled in the colour definitions from the Figma component library, including background, text, muted text, blue, borders, pale surfaces, destructive feedback, and gradient colours.

Screens now refer to these names instead of independently defining the same colours. Changing `AppColors.background`, for example, changes the shared background definition.

### `AppTheme` — `lib/theme/app_theme.dart`

I added `AppTheme.light` and applied it to `MaterialApp` in `main.dart`. It defines Inter typography, the light colour scheme, text selection colours, and reusable text styles.

It also defines shared dimensions, including an 8-point spacing unit, an 18-point component radius, and 20-point screen padding. The layouts use the actual Figma spacing where it differs from those foundations.

The primary button, targets card, Today card, and session-expired illustration have gradient definitions appropriate to their Figma shapes.

### `AppButton` — `lib/widgets/app_button.dart`

I added three appearances: primary, secondary, and destructive. The primary button has the blue gradient; the others use pale blue or pale red backgrounds.

`onPressed` supplies the screen's action. Passing `null` disables the button. Setting `isLoading: true` replaces the label with a progress indicator and blocks taps.

```dart
AppButton(
  label: 'Create account',
  isLoading: session.busy,
  onPressed: submitRegistration,
)
```

The standard minimum heights are 52 for primary buttons and 48 for the others. Content can grow rather than being clipped into a fixed height.

### `AppTextField` — `lib/widgets/app_text_field.dart`

I implemented the Figma label-and-input layout around a native Flutter text field. It supports controllers, focus, keyboard types, autofill hints, password masking, submission actions, and validators.

The component uses `FormField<String>` to participate in form validation. Validation messages appear below the field, and its border reflects focus or an error. Password fields suppress suggestions and autocorrect.

The component creates and disposes its own controller/focus node when none is supplied. When a view supplies them, that view retains ownership and disposes them.

### `AppFeedbackCard` — `lib/widgets/app_feedback_card.dart`

I added the blue title-and-description card used for information and authentication errors in Figma. The caller supplies the message; the component supplies its layout and styling. Its semantics announce updated feedback to assistive technology.

### `AppSelectField` — `lib/widgets/app_select_field.dart`

I added a labelled dropdown field for gender and activity level, using the same white surface and rounded border as the text inputs.

### `FlowScaffold` — `lib/widgets/flow_scaffold.dart`

I added a shared screen layout with an optional Back action, section label, heading, scrollable body, and bottom actions inside the phone's safe area.

This avoids repeating the layout in every authentication/onboarding screen. The body can scroll when space is limited or the keyboard opens. The app uses the real system status bar rather than drawing Figma's example time and battery symbols.

## 4. Screens and navigation

### `main.dart`

I replaced the counter app's root behaviour with session-driven screen selection. `main()` initialises Flutter bindings before starting the app so native plugins can be used.

`_MyAppState.initState()` creates the API client, secure token store, and session provider, then starts session restoration. Tests can supply a different provider without accessing real device storage.

`didChangeAppLifecycleState()` checks token expiry when the app resumes. `dispose()` removes the lifecycle observer and releases the session/API objects it created.

The root listens to these stages:

| Stage | Screen or behaviour |
|---|---|
| `starting` | Loading while reading the saved token |
| `signedOut` | Welcome, Register, or Login |
| `checkingProfile` | Loading while requesting the profile |
| `onboarding` | Baseline, activity/goal, and targets steps |
| `ready` | Today |
| `expired` | Session Expired |
| `retry` | Error with Try again |

Welcome/Register/Login switching is local UI state. Protected destinations are selected from the session stage rather than a separate named-route system.

### `WelcomeView` — `lib/views/welcome_view.dart`

I added the logo, introduction, and two actions from Figma. Get started opens registration; I already have an account opens login.

### `AuthView` — `lib/views/auth_view.dart`

I implemented registration and login in one configurable view because they share most of their structure. Their validation and login-error designs are states of that view, not separate duplicated screens.

`_submit()` validates the form before calling the provider. Registration requires a valid-looking email, a nonempty username, and a password of at least eight characters. Login requires a nonempty identifier and password; it does not impose the new registration minimum on existing accounts.

The password minimum is frontend-only, as requested. The backend password validation was not changed.

Inputs and navigation actions are restricted while authentication is running. After an authentication attempt, the password field is cleared if the view still exists. A wrong-password response displays the login error without treating it as an expired authenticated session.

### `SessionExpiredView` — `lib/views/session_expired_view.dart`

I added the reconnect screen and its Log in again action. That action opens Login after the session has been cleared.

## 5. Backend communication

### `ApiClient` — `lib/services/api_client.dart`

I added `request()` as the shared entry point for HTTP calls. It constructs the URL, serialises request bodies to JSON, reads JSON responses, and applies a 20-second timeout.

For protected calls it sends:

```http
Authorization: Bearer <access-token>
```

Register and Login explicitly use `authenticated: false`. They must work before a token exists and must not expire the current session merely because credentials were incorrect.

`ApiException` carries a user-facing message and, where available, the HTTP status. The client reads the backend's Problem Details `detail` field, uses a generic message for server failures, and translates connection/timeout failures into retryable messages.

A `401` for a protected request invokes `SessionProvider.expire()`. This only happens if the request's token is still the client's current token, so an old request cannot invalidate a different token simply because it completed late.

A connection error does not delete the token. Backend downtime and rejected authentication have different meanings.

### Connected endpoints

| Method and path | Frontend caller | Purpose |
|---|---|---|
| `POST /api/auth/register` | `AuthService.register()` | Create the account |
| `POST /api/auth/login` | `AuthService.login()` | Obtain the existing backend JWT |
| `GET /api/profiles` | `ProfileService.getProfile()` | Load the profile and choose the destination |
| `POST /api/profiles/personal-data` | `ProfileService.save()` | Save baseline/activity/goal and obtain calculated targets |
| `GET /api/daily-log/today` | `TodayView._load()` | Load today's meals and nutrition summary |

The UI's username is sent as the backend's `userId`. Login sends email or username as `identifier`. Protected profile/log requests use the JWT; they do not send an arbitrary user ID to choose whose data to access.

### Local server addresses

`ApiClient` defaults to `http://10.0.2.2:8080` for the Android emulator and `http://127.0.0.1:8080` for the iOS simulator. On a physical phone, override the address with your Mac's LAN IP:

```sh
# Run from mobile/; replace this example IP.
flutter run --dart-define=API_BASE_URL=http://192.168.1.42:8080
```

These defaults are for local development. They do not deploy the backend, start it automatically, or configure a production HTTPS endpoint.

## 6. How login persistence works

### `TokenStore` and `SecureTokenStore` — `lib/services/token_store.dart`

I added a small storage interface with `read()`, `write()`, and `clear()` so session code does not depend directly on a native plugin. Production uses `SecureTokenStore`; tests use an in-memory implementation.

`SecureTokenStore` uses `flutter_secure_storage` and stores the JWT under `fitmaxxer.accessToken`. The plugin uses platform secure storage, including Keychain on iOS. The password is not persisted.

### `SessionProvider.authenticate()`

When registering, this method calls Register first and then Login. Your registration endpoint returns an empty success response, so automatic login is a second request using the existing Login endpoint.

After login, it saves the token, assigns it to `ApiClient`, schedules its expiry, and requests the profile. A `busy` guard blocks duplicate authentication submissions.

These are separate requests, not one transaction across registration and login. If account creation succeeds but login fails, the account remains created. The app opens Login with an explanatory message rather than inviting the user to register the same account again.

### `restore()`, `expiryOf()`, and `checkExpiry()`

`restore()` reads the stored JWT when the app starts. `expiryOf()` reads its `exp` claim. A token with a future expiry is attached to a profile request; the backend then decides whether to accept it.

Reading `exp` in Flutter is a navigation convenience. It does not verify the token's signature or replace backend authentication.

A timer expires the local session when the token's lifetime ends. `checkExpiry()` also runs when the app returns to the foreground. `expire()` clears the in-memory token, profile, and draft, attempts to remove the saved token, and opens Session Expired. If device storage deletion fails, restoration still rejects a locally expired/unreadable token and the backend still rejects an invalid token.

`loadProfile()` treats only the profile endpoint's `404` as a missing profile. Server, network, or parsing errors open the retry state instead of starting onboarding.

### What survives closing the app?

| Information | Saved on device by this implementation? | Saved by backend? |
|---|---|---|
| JWT access token | Yes, in secure storage | No new session record was added |
| Password | No | Existing backend stores its password hash |
| Completed profile and nutritional targets | No persistent frontend copy | Yes, through the existing profile endpoint |
| Unsubmitted onboarding draft | No; memory only | No |
| Daily meals and logs | No offline cache was added | Yes, through the existing backend |

Closing the app discards its in-memory state. The next launch reloads the token and requests the profile/log again. An unfinished baseline/activity draft is not restored after closing the app.

### What happens when the backend restarts?

Your existing backend issues JWTs with a default lifetime of 15 minutes. A restart does not itself remove the token from the phone or invalidate its signature.

If the backend restarts with the same `JWT_SECRET` and validation settings, an otherwise accepted token can still work until its original expiry. Changing the signing secret makes old tokens fail validation. Closing either application does not pause or restart token expiry.

For example, a token issued at 10:00 expires at 10:15. A backend restart at 10:08 does not extend it. While the backend is unavailable, profile/log requests show connection errors; expiry can still occur during that downtime.

No refresh token, silent renewal, server-side logout/revocation mechanism, or longer-lived login was added. Persisting the current token only remembers the session for the lifetime already allowed by the backend.

## 7. Profile models and onboarding

### `ProfileDraft` and `UserProfile` — `lib/models/profile.dart`

I added `ProfileDraft` for editable onboarding values and `UserProfile` for the saved profile/targets returned by the server. The same file defines labels and descriptions for the five existing activity levels.

`ProfileDraft.toJson()` converts validated inputs into the existing API request shape. `UserProfile.fromJson()` converts response values into typed Dart fields. `toDraft()` can initialise editable values from a profile. Since the response does not contain a goal identifier, it infers a goal from target calories minus maintenance; an unrecognised adjustment falls back to Maintenance in that draft.

### `OnboardingView` — `lib/views/onboarding_view.dart`

I implemented three steps within one view:

1. Baseline: age, gender, weight, height, optional body fat, and unit selection.
2. Activity and goal: the existing activity levels and all seven goal cards.
3. Targets: calories and macros calculated by the backend, plus the submitted baseline summary.

`_next()` validates baseline input, updates the in-memory draft, and opens activity/goal selection. It does not save the profile yet.

`_changeUnits()` converts the displayed measurements. Imperial height uses feet and inches as separate editable fields. The `kg` and `cm` getters convert back to metric, and requests always send `unitSystem: "METRIC"`. This avoids converting the same input once in Flutter and again in the backend. Switching units does not persist a measurement preference.

Validation follows the existing physical-data bounds: age 1–120, weight above 10 and up to 635 kg, height above 100 and up to 250 cm, and optional body fat from 3–80%. Imperial inputs are checked through their metric equivalents; inches must be from 0 to below 12.

The initial draft starts with empty measurements, Male, Sedentary, and Maintenance. These are UI defaults, not values inferred about the user.

### When does onboarding save data?

`_calculate()` calls `SessionProvider.saveProfile()`, which calls `ProfileService.save()`. The profile endpoint saves personal data and calculates nutrition in the backend.

**Calculate my targets already saves the profile.** Start my day changes the frontend destination to Today; it is not an additional save or confirmation request.

`ProfileService.save()` combines the returned nutrition values with the submitted baseline to construct the frontend profile. The save response does not contain the complete physical profile, so this avoids an immediate second GET. The calorie and macro calculations still come from the server.

Adjust targets returns to activity/goal selection. Back from there returns to baseline. Calculating again uses the existing endpoint's update-or-create behaviour. There is no custom calorie/macro editor in this screen.

If the app closes after the profile was saved but before Start my day, reopening can go directly to Today because the backend now has a profile. There is no separate persisted onboarding-completed flag.

### `GoalOption` — `lib/models/goal_option.dart`

I added a single mapping between app labels and all seven existing backend identifiers:

| App label | API value | Description |
|---|---|---|
| Higher deficit cutting | `AGGRESSIVE_CUTTING` | 500 kcal below maintenance |
| Moderate cutting | `MODERATE_CUTTING` | 300 kcal below maintenance |
| Slight cutting | `LIGHT_CUTTING` | 200 kcal below maintenance |
| Maintenance | `MAINTAIN_WEIGHT` | Matches your estimated maintenance calories |
| Lean bulking | `LIGHT_BULK` | 200 kcal above maintenance |
| Moderate bulking | `MODERATE_BULK` | 300 kcal above maintenance |
| Higher surplus bulking | `AGGRESSIVE_BULK` | 500 kcal above maintenance |

The offsets supply descriptions and help map an existing profile back to a draft. Flutter does not use them to replace the backend's nutrition calculator. The goal cards scroll while the main action stays at the bottom.

## 8. Today screen: implemented scope

I added `TodayView` in `lib/views/today_view.dart` as the destination after authentication/onboarding. `_load()` calls the existing Today endpoint; `_refresh()` supports pull-to-refresh and the error screen's Try again action.

The view displays consumed calories, the daily target, remaining calories, macro progress, the item count, and meal entries grouped into Breakfast, Lunch, Dinner, and Snacks. Empty meals show an empty-state message. These values come from the API rather than Figma's example numbers.

If the user is over a target, the view describes the amount as over target. Progress indicators are capped visually while the numeric totals remain visible.

The Today header currently uses the device's local date. Meal details use the API's names and quantities. The application does not yet have a typed daily-log model or a separate daily-log service/provider; this first read-only destination parses its response inside the view.

The Log food control is disabled. The other tab labels and meal plus indicators are visual placeholders without navigation/actions. This increment did not implement food search, logging, item editing/moving/deletion, recipe screens, history, meal copying, or the Profile tab in Flutter. Their backend implementations remain available for later frontend work.

## 9. Dependencies, fonts, and Figma assets

I updated `pubspec.yaml` and its resolved lockfile:

| Dependency | Change and purpose |
|---|---|
| `http` | Already present; now used for backend requests |
| `flutter_secure_storage` | Added for persistent secure token storage |
| `flutter_svg` | Added for rendering the actual Figma SVG assets |

The lockfile also includes these packages' transitive dependencies. Those extra entries are resolved dependencies, not separate features manually added to the app.

I bundled the Inter variable font in `assets/fonts/`, included its OFL license, and registered weights 400, 500, 600, and 700. I downloaded the Figma SVGs into `assets/images/`: the banana barbell, weightlifter, session track/progress, Today track, and four navigation icons. Runtime rendering uses local assets rather than temporary Figma URLs or whole-screen screenshots.

The Linux, macOS, and Windows generated plugin registration files changed when Flutter resolved the native plugins. They register dependencies; they do not mean desktop support was designed or tested.

## 10. Android and iOS configuration

### Android

I changed `android/app/src/main/AndroidManifest.xml` to declare Internet permission, display the FitMaxxer app name, and set `android:allowBackup="false"`. Disabling Android app backup avoids restoring encrypted token data without its corresponding device keys; it applies to Android app backup generally, not only one token entry. Backend data remains stored on the server.

I added `android:usesCleartextTraffic="true"` to the debug manifest for local HTTP development. That debug-only exception was not added to the main manifest.

I changed Android Gradle Plugin from 9.1.0 to 9.1.1 in `android/settings.gradle.kts` because the installed Flutter SDK rejected 9.1.0. I did not broadly upgrade the Gradle wrapper or Kotlin settings.

### iOS

I changed `ios/Runner/Info.plist` to use the FitMaxxer display name. I added `Info-Debug.plist` with local network usage text and an HTTP transport exception for development.

I changed the Xcode project's Debug configuration to use that debug plist. Release and Profile still use the normal plist without the debug HTTP exception. A physical device using local HTTP should therefore be run in Debug for this setup.

I added `DebugProfile.entitlements` and `Release.entitlements` with the Keychain access-groups configuration required by the secure-storage setup, and connected them to the matching Xcode configurations.

These changes prepare local native development; they do not configure deployment, production certificates, a hosted API, or an App Store release.

## 11. Earlier backend preparation for the frontend

These changes preceded the Figma screen implementation. The latest Flutter work reused them.

| File/class | What I changed and why |
|---|---|
| `TerminalRunner` | Added `@ConditionalOnProperty` so the CLI starts only when `app.cli.enabled=true`. Starting the API normally no longer enters the interactive terminal flow. |
| `application.properties` | Set the default CLI flag to false and bound the local API to `127.0.0.1:8080`. |
| `application-cli.properties` | Added an opt-in CLI profile. |
| `application-mobile-dev.properties` | Added a profile binding to `0.0.0.0` so phones on the local network can reach the API. |
| `health/controller/HealthController` | Added public `GET /api/health`, returning `{"status":"UP"}`, for a basic connectivity check. The Flutter startup flow currently requests the profile directly rather than calling health first. |
| `GlobalExceptionHandler` | Standardised controller errors as JSON Problem Details, including invalid input, missing resources, conflicts, authentication/permission failures, unavailable catalog, and safe generic unexpected-error responses. |
| `ApiProblemWriter` | Added the same JSON error shape for security failures that occur before controller handling. |
| `SecurityConfig` | Allowed the health check and integrated the security error writers while retaining JWT authentication and authentication challenge headers. |

For example, the frontend can read a `detail` message from this response instead of expecting a plain-text error:

```json
{
  "type": "about:blank",
  "title": "Unauthorized",
  "status": 401,
  "detail": "Invalid credentials.",
  "instance": "/api/auth/login"
}
```

I also added CLI configuration tests, a real-port startup/health test, and Problem Details response tests, and updated existing assertions that expected plain-text errors. These are separate from the 19 Flutter tests described below.

The preparation did not introduce a new JWT design. The signing secret and 15-minute lifetime already existed. Backend uniqueness/locking work, daily-log domain changes, database recreation, and the original user-story implementations belong to earlier MVP work, not this frontend increment.

## 12. Tests and verification

I replaced the counter smoke test and added tests appropriate to the new behaviour:

| File | Coverage |
|---|---|
| `test/shared_widgets_test.dart` | Button actions and disabled/loading behaviour, field validation/reset/controller updates, password masking, and long content with larger text |
| `test/session_provider_test.dart` | Seven goal mappings, registration followed by login, token persistence/restoration, expiry and protected `401`, profile retry versus missing profile, incorrect credentials, partial registration success, duplicate submissions, and profile saving |
| `test/widget_test.dart` | Welcome navigation, invalid registration without HTTP calls, complete registration/onboarding/Today flow, imperial conversion, login error, expired-session navigation, and account-created/login-failed handling |

At the last implementation verification:

- All 19 Flutter tests passed.
- Implemented screen previews were rendered and reviewed against Figma. Temporary preview-generating test files were removed afterwards.
- The iOS simulator debug build passed with the native storage plugin.
- Static analysis reported 15 informational Material import deprecations, with no reported application errors or warnings. `flutter analyze` still returned a nonzero exit status for those notices.
- The Android build passed the initial plugin-version check after the patch update, but then waited on NDK installation. I stopped the stalled build; Android build success is unverified.
- Native plist/project syntax checks and whitespace checks passed.

The HTTP tests use mocked responses and an in-memory token store. They do not prove a live backend/device round trip or real Keychain/Android storage behaviour. A successful iOS build checks compilation and native integration, not a completed live login on a simulator or physical device. That manual verification remains to be done.

## 13. How to run and where to make future changes

Start the backend from `backend/` with `JWT_SECRET` configured in its environment:

```sh
./gradlew bootRun
```

For a physical phone on the same network, use:

```sh
./gradlew bootRun --args='--spring.profiles.active=mobile-dev'
```

Then run the frontend from `mobile/`:

```sh
flutter pub get
flutter run
```

Use the `API_BASE_URL` override from section 5 for physical phones. Keep the same development signing secret between backend restarts when you want unexpired tokens to keep working. Never put the backend signing secret in the Flutter app.

| Desired change | Starting point |
|---|---|
| Colours, fonts, gradients | `AppColors`, `AppTheme` |
| Shared button/input appearance | `lib/widgets/` |
| Goal names and descriptions | `GoalOption` |
| API base URL, headers, timeouts, response handling | `ApiClient` |
| Login/registration request mapping | `AuthService` |
| Session restoration, expiry, and destination selection | `SessionProvider`, `main.dart` |
| Baseline fields, conversions, onboarding steps | `OnboardingView`, `ProfileDraft` |
| Profile API mapping | `ProfileService`, `UserProfile` |
| Today's initial display | `TodayView` |
| Longer-lived login through token renewal | Requires a separate backend authentication design and matching frontend changes |

See [localDevelopment.md](localDevelopment.md) for the fuller local setup and API contracts, and [api.http](api.http) for requests you can run independently of Flutter.
