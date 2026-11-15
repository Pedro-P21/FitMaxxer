# Daily Log Write Consistency

A daily log is unique per authenticated user and calendar date. Its meal groupings are unique per daily log and meal type, including Snacks.

## Transaction boundary

`DailyLogApplicationService` manages writes using `TransactionTemplate`. Catalog details and saved-recipe DTOs are resolved before the write transaction. Factories create entities and value objects inside each attempt. Historical copy-source reads, destination changes, saving, and response mapping are part of the write transaction.

The current date is fixed for the logical operation. A retry cannot redirect an operation to another day if midnight occurs between attempts.

## Existing logs

`DailyLogRepository.findByUserIdAndDateForUpdate` selects the owner's daily-log root with `PESSIMISTIC_WRITE`. Meals and items are read after acquiring the root lock. Adding, copying, editing quantities, moving, and deleting entries all use this lookup. The lock is released when the transaction commits or rolls back.

The query supplies a 2-second lock-wait hint and a 3-second query-timeout hint. The H2 concurrency tests exercise the lock timeout. Ordinary current-day and history reads use unlocked queries. Different users' daily logs remain independently writable.

`EditUserNutritionalProfileService.editUserProfile` acquires today's existing daily-log lock before loading/modifying the profile. The profile and current-day target snapshot are saved in the same transaction.

## First-log creation

A missing row cannot be locked. `uk_daily_log_user_date` prevents two requests from creating separate logs for the same user/date. `executeWrite` retries that specific unique-constraint conflict after the failed transaction has rolled back, up to three attempts. Each retry reloads the destination and constructs fresh entities and value objects. Catalog/recipe DTO resolution is not repeated.

Other integrity failures are not retried. Lock/query timeouts and exhausted creation conflicts return HTTP `409` with a retry message. Deliberately repeating a meal-copy request remains a separate addition; these safeguards are not request deduplication.

When invoked inside a caller-owned transaction, write operations join that transaction. Creation conflicts propagate as a conflict without local retry; the owner must roll back before retrying the full transaction. An inner operation never commits independently of its caller.

## Database constraints and development data

- `daily_log`: `UNIQUE (user_id, log_date)`.
- `daily_log_meal`: `UNIQUE (daily_log_id, meal_type)`.

The development database uses an empty schema with these constraints. Automated tests use isolated in-memory databases, including tests that start genuinely overlapping transactions and check final persisted state.
