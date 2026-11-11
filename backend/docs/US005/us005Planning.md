### US005 - Daily History

## 1. Requirements Engineering

##### 1.1. User Story Description
As a User, I want to see the past history of my daily logs, so that I can review what I ate on previous days.

##### 1.2. Use Case Specifications
**From the user story:**
- The authenticated user can review saved daily logs for a selected month and year.
- A past log shows the foods recorded for that date, grouped by meal, in the same format used to review today's log.
- The user can distinguish each log by its date and review its daily nutrition summary.

**From the glossary and domain analysis:**
- **DailyLog (Aggregate Root):** One user's log for one calendar date. It owns the meals and meal items recorded for that date.
- **Meal:** A grouping of meal items by `Breakfast`, `Lunch`, `Dinner`, or `Snacks`.
- **MealItem:** A recorded food and quantity, with its log-time `FoodSnapshot` and `PortionQuantity`.
- **FoodSnapshot (Value Object):** The food name, optional brand/barcode, nutrient values and basis, and catalog serving definition captured when the item was logged.
- **DailyGoalSnapshot (Value Object):** The calorie and macro targets associated with that daily log.
- **DailyNutritionSummary:** The logged intake, that date's target snapshot, and remaining/over-target values calculated for the selected day.

##### 1.3. Acceptance Criteria
- **AC1:** An authenticated user can request history for a specific month and year.
- **AC2:** The application validates that the month and year are valid, not in the future, and within the supported history window.
- **AC3:** The application returns only the authenticated user's saved logs in the requested month, ordered newest first. If the selected month is the current month, today's log is excluded.
- **AC4:** Each displayed log includes its date, foods grouped by meal, logged quantities and units, and its daily nutrition summary.
- **AC5:** A past log uses the food and target snapshots stored with that log. Changes to the food catalog or the user's current profile do not rewrite history.
- **AC6:** The history contains only logs owned by the authenticated user. A user cannot retrieve another user's logs.
- **AC7:** If the selected month contains no saved logs, the application returns an empty history and the CLI explains that no logs are available for that month.
- **AC8:** US005 is read-only. Adding items remains in US004; editing or removing historical entries remains outside this story.

##### 1.4. Found out Dependencies
- **US004 (Meal Logging and Daily Tracking):** Creates and persists daily logs, meals, meal items, food snapshots, portion quantities, and daily target snapshots.
- **US008 (Authentication and Account Management):** Supplies the authenticated user identity used to scope history queries and protect log ownership.

##### 1.5. Input and Output Data
**Input Data:**
- Authenticated user identity from the security principal.
- Requested month and year, supplied as GET query parameters.
- No client-supplied user ID or current date is accepted.

**Output Data:**
- Saved daily logs from the requested month, ordered from most recent to oldest.
- For each date: meals and their food items, quantities and units, nutrient details, daily totals, target values, and remaining/over-target values.
- An empty result when there are no saved logs for the requested month.
- Validation errors for invalid, future, or out-of-range month/year requests.
- Authentication, authorization, and persistence error responses as applicable.

##### 1.6. System Sequence Diagram (SSD)
![US005_SSD.svg](US005_SSD.svg)[US005 SSD source](./US005_SSD.puml)


##### 1.7. Assumptions and Decisions
- History consists of dates for which a `DailyLog` was actually persisted. Dates with no log are omitted rather than shown as empty days.
- The user selects a month and year. These values are sent as GET query parameters; GET requests do not carry a request body.
- The application derives the current date and the selected month's date range using its configured clock and timezone. Today's log is excluded when the requested month is the current month.
- Invalid months, future months, and requests older than the configured 10-year lookback window are rejected. `daily-log.history.max-lookback-years` can be adjusted by configuration.
- Logs are returned newest first so the user sees the most recent activity in that month first.
- The repository query is bounded to one month and scoped to the authenticated user.
- Historical values come from the persisted `FoodSnapshot` and `DailyGoalSnapshot`; history does not call US003 or read the user's current nutritional profile.
- The selected log's consumed totals and remaining values are calculated using that log's recorded foods and target snapshot.
- All meal types supported by US004, including multiple `Snacks` entries, are retained in the historical display.
- US005 does not change the daily log persistence model or allow historical log edits.

## 2. Analysis

### 2.1. Relevant Domain Model Excerpt
The existing `DailyLog` aggregate is already the unit of persisted daily history. Each log has a user identity and date, owns its meals and meal items, and stores the target snapshot used for that date. Each meal item owns the food and portion data captured when it was logged.

![meal_and_daily_log_aggregate.svg](../global-artifacts/domainModel/domainModelExcerpts/meal_and_daily_log_aggregate.svg)

History can therefore be read from persisted `DailyLog` aggregates and mapped to the existing daily-log response shape. Each `DailyLog` has its food and target snapshots and can calculate the logged intake and remaining values for its date without consulting the current food catalog or profile. The response mapper maps those results into transport DTOs.

## 3. Design

### 3.1. Rationale

| Architectural Component / Pattern | Choice & Implementation | Rationale & Benefit |
|:-----------------------------------|:-----------------------|:--------------------|
| **Authenticated History Query** | Resolve the owner from the authenticated JWT and query only that user's logs. | Prevents callers from selecting another user's data. |
| **Month-Bounded Repository Query** | Accept month/year as GET query parameters and query the inclusive date range for that month, newest first. | Bounds each request and supports browsing one month at a time. |
| **Snapshot-Based History** | Recalculate summaries from persisted food and target snapshots on each log. | Keeps history independent of current catalog/profile data without storing duplicate summary totals. |
| **History Response Mapping** | `PastHistoryMapper` maps retrieved logs and derived summaries into `LogHistoryResponseDTO`. | Keeps response shaping outside the aggregate and controller. |
| **Configurable Lookback Window** | Reject requests older than `daily-log.history.max-lookback-years` (default 10). | Prevents unreasonable historical queries while allowing the supported window to be configured. |

### 3.2. Design Decisions & Architecture

1. **Use query parameters for month/year.** The endpoint is `GET /api/daily-log/history?month={month}&year={year}`. The bearer token is sent in the `Authorization` header. A GET request body is not used.
2. **Derive the owner and current date on the server.** The authenticated subject supplies the user ID. `LogHistoryService` uses the injected application clock and timezone to determine today's date and calculate the selected month's range.
3. **Validate the requested month.** Reject invalid month/year values, future months, and months before the configured lookback window. The current month is allowed, but today's log is excluded; if today is the first of the month, the result is empty without a repository call.
4. **Return one month's logs newest first.** Query only rows owned by the authenticated user whose log date falls within the calculated range.
5. **Keep summaries derived.** For each retrieved `DailyLog`, call its existing summary calculation with the stored food, portion, and target snapshots. `DailyNutritionSummary` remains transient; it is not persisted.
6. **Keep history read-only.** US005 does not create logs for empty dates or modify stored logs.

### 3.3. Sequence Diagram (SD)
[US005 SD source](./US005_SD.puml)


## 4. Tests

### 4.1. Domain Validations (Bottom-Up Approach)

- Verify daily summaries for stored logs are calculated from their food and target snapshots.
- Verify per-meal and daily values returned for historical logs.


### 4.2. Application Layer & Integration Tests

- A selected past month queries the complete inclusive month range for the authenticated user.
- A selected current month excludes today; on the first of the month it returns an empty list without querying the repository.
- Invalid month values, future months, and months outside the configured lookback window are rejected.
- Results preserve newest-first order and include meal items and calculated nutrition summaries.
- The controller requires authentication and passes the JWT subject plus month/year parameters to the service.
- A month with no saved logs returns an empty history response.


## 5. Implementation

### 5.1. Overall Flow

1. The user selects a month and year in the CLI or requests them from the API.
2. The frontend sends `GET /api/daily-log/history?month=...&year=...` with the bearer token in the Authorization header.
3. The authentication filter validates the token; `LogHistoryController` passes the authenticated user ID and requested month/year to `LogHistoryService`.
4. The service derives today's date from the configured clock, validates the requested month against calendar, future-date, and lookback rules, and calculates the month range. It caps the current month at yesterday.
5. `DailyLogRepository` retrieves only that user's logs within the date range, ordered newest first.
6. `PastHistoryMapper` asks each `DailyLog` to calculate its summary from stored snapshots and maps the logs into `LogHistoryResponseDTO`.
7. The API returns the monthly history; the CLI displays each log grouped by date and meal. An empty month produces an empty list and an informational message.


### 5.2. Key Methods and Classes

| Class / Component | Method / Responsibility |
|:------------------|:------------------------|
| `LogHistoryController` | Authenticated `GET /api/daily-log/history` endpoint; accepts month/year query parameters. |
| `LogHistoryService` | Validates month/year and lookback window, derives date range, and retrieves the authenticated user's logs. |
| `DailyLogRepository` | Queries an owner's logs between the month's start and end dates, newest first. |
| `DailyLog` | Calculates consumed, target, and remaining values from persisted snapshots. |
| `PastHistoryMapper` | Maps daily logs and derived summaries into the history response DTO. |
| `LogHistoryResponseDTO` | Returns the requested month/year and its `DailyLogResponseDTO` entries. |
| `DailyHistoryUI` | Prompts for month/year and displays returned logs in the CLI. |


### 5.3. Data Flow Example

```text
GET /api/daily-log/history?month=10&year=2026
Authorization: Bearer <access-token>
↓
JWT Authentication Filter → authenticated userId
↓
LogHistoryController.getPastHistory(userId, month, year)
↓
LogHistoryService validates request and derives October 2026 date range
↓
DailyLogRepository.findByUserIdAndDateRange(userId, startDate, endDate)
↓
DailyLog aggregates → calculate summaries from persisted snapshots
↓
PastHistoryMapper → LogHistoryResponseDTO(year, month, logs)
↓
200 OK + monthly history (or an empty logs list)
```
