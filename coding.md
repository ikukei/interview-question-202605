# Local Coding Progress

Last updated: 2026-05-15

## Goal

Implement the feature management take-home project in subdirectories:

1. `backend`: Spring Boot Maven backend, H2 local database, Oracle production profile placeholder, unit tests.
2. `web-admin`: Vue admin frontend that calls backend APIs.
3. `java-sdk`: Java SDK for feature evaluation.
4. `frontend-sdk`: TypeScript frontend SDK for feature evaluation.
5. `java-demo`: Java command-line demo using `java-sdk`.
6. `vue-demo`: Vue demo page using `frontend-sdk`.

## Current Status

### Completed

- Root Maven multi-module `pom.xml` created.
- `.gitignore` created.
- `backend` module created.
- Backend local H2 configuration added.
- Backend production Oracle profile placeholder added.
- Backend entities added:
  - `ApplicationEntity`
  - `FlagEntity`
  - `RuleEntity`
  - `ConfigSnapshotEntity`
  - `AuditLogEntity`
- Backend repositories refactored to use standard Spring Boot JdbcTemplate instead of custom FeatureDataSource:
  - ApplicationRepository
  - FlagRepository
  - RuleRepository
  - ConfigSnapshotRepository
  - AuditLogRepository
  - SchemaInitializer
- Backend DTO records added.
- Backend services added:
  - `FlagService`
  - `PublishService`
  - `EvaluationService`
  - `EvaluationEngine`
  - `SnapshotCache`
  - `AuditService`
- Backend controllers added:
  - `FlagController`
  - `PublishController`
  - `EvaluationController`
- Backend CORS config added for local Vue apps.
- Backend local demo seed data added for:
  - app: `checkout-service`
  - environment: `local`
  - flag: `new-checkout`
  - rule: `region equals us-east -> true`
- Backend unit tests added for `EvaluationEngine`.
- `java-sdk` module created using Java `HttpClient`.
- `java-demo` module created with command-line feature evaluation output.
- `frontend-sdk` TypeScript package created.
- `web-admin` Vue project created.
- `vue-demo` Vue project created.
- Backend packaged successfully with local offline Maven workaround.
- Backend started successfully via PowerShell Job.
- Backend `/api/v1/apps` verified and returned seeded `checkout-service`.
- Backend evaluation endpoint verified:
  - `flagKey=new-checkout`
  - `region=us-east`
  - returned `value=true`
  - reason `RULE_MATCH`
- Java SDK demo verified via direct `java -cp`; it printed `Feature value: true`.
- Frontend dependencies installed successfully after using proxy `127.0.0.1:7890`.
- `frontend-sdk`, `web-admin`, and `vue-demo` production builds passed.
- Added `npm start` aliases for both Vue apps.
- README added with local run instructions and environment notes.

### Refactoring Summary (2026-05-15)

#### Why refactor from custom FeatureDataSource to JdbcTemplate?

1. **Standard Spring Boot convention**: Spring Boot autoconfigures DataSource and JdbcTemplate automatically when using standard `spring.datasource` properties.
2. **Less boilerplate code**: No need to manually manage JDBC connections and statement resources with try-with-resources - JdbcTemplate handles this internally.
3. **Better integration**: JdbcTemplate supports GeneratedKeyHolder for capturing auto-generated IDs, simplifying insert operations.
4. **Easier extension**: JdbcTemplate provides convenience methods for common CRUD operations that we can leverage later.

#### Changes made:

1. **Updated configuration** (`application-local.yml` and `application-prod.yml`):
   - Renamed `feature.datasource` prefix to standard `spring.datasource`
   - Spring Boot will automatically create DataSource, JdbcTemplate, and TransactionManager beans

2. **Refactored all Repository classes**:
   - Changed dependencies from `FeatureDataSource` to `JdbcTemplate`
   - Replaced manual connection/statement management with JdbcTemplate methods:
     - `jdbcTemplate.query()` for select operations
     - `jdbcTemplate.update()` for insert/update/delete operations
     - `GeneratedKeyHolder` for capturing auto-generated IDs
     - `jdbcTemplate.execute()` for DDL statements

3. **Removed FeatureDataSource class**:
   - Custom driver loading class is no longer needed
   - Spring Boot handles driver class loading automatically

4. **Simplified SchemaInitializer**:
   - Removed InitializingBean interface and afterPropertiesSet method
   - Directly called initializeSchema() from constructor for simplicity

### In Progress

- Maven Surefire automatic test execution is blocked by incomplete local provider cache; test source exists, but local Surefire providers are incomplete.

### Local Environment Notes

- Global Maven settings force `D:/Java/maven-repository`, which is not writable from this workspace.
- Added project-local `.mvn/settings.xml`.
- Use `mvn -s .mvn/settings.xml` for local commands.
- Current fallback: use cached dependencies in `D:/Java/maven-repository`; backend is Spring Boot Web + H2 JDBC, not JPA, because Spring Data JPA is not cached locally and network approval did not complete.
- Added `.mvn/offline-settings.xml` for offline builds against existing `D:/Java/maven-repository` cache.
- Replaced temporary H2 `systemPath` dependency with standard Maven runtime dependency.
- Restored Spring Boot `3.5.14`; replaced `spring-boot-starter-test` with cached JUnit `4.13.2` + AssertJ `3.19.0` so tests can run offline with cached Surefire.
- Added Maven compiler `-parameters` support after local API test found Spring MVC parameter-name binding issue.
- Added CORS origins for `http://127.0.0.1:5173` and `http://127.0.0.1:5174` after browser showed `TypeError: Failed to fetch`.

## Simplification Plan (2026-05-16)

### Background

After reviewing against `Align_Expert_Software_Engineer_R2_Quiz.md`, the quiz requires
the explainability model to answer exactly four questions for any evaluated flag:

1. is it enabled?
2. For whom?
3. In which region?
4. Associated with which release?

The current implementation built a generic rule engine (`ff_rule` table, `conditionJson`,
four operators, priority ordering, per-rule `enabled`) that goes well beyond what the quiz
requires. The goal of this plan is to remove the over-engineering so the demo is focused,
readable, and directly maps to the quiz requirements.

### Design Decision: replace the rule engine with flat flag fields

Instead of a separate rule table with generic conditions, move the targeting dimensions
directly onto the flag:

| Field | Quiz question answered |
|---|---|
| `enabled` | is it enabled? |
| `targetUserId` | For whom? (exact user match) |
| `targetRegion` | In which region? |
| `releaseKey` | Associated with which release? |

Evaluation logic becomes a simple sequence of checks — no rule engine, no JSON parsing,
no priority loop:

```
if !flag.enabled          → FLAG_DISABLED
if targetRegion set
   && context.region != targetRegion  → NO_REGION_MATCH
if targetUserId set
   && context.userId != targetUserId  → NO_USER_MATCH
→ MATCH, return flag value
```

### What gets removed

- `ff_rule` table and `RuleEntity`
- `RuleRepository`
- `conditionJson` and all operator logic (`equals`, `notEquals`, `contains`, `in`)
- `rolloutPercentage` and rollout bucket hashing (not required by quiz)
- `SnapshotRule` and `SnapshotCondition` model classes
- `AddRuleRequest` and `RuleResponse` DTOs
- `FlagController.addRule` endpoint
- `EvaluationEngine` class (logic moves inline into `EvaluationService`)
- `EvaluationEngineTest` (replaced by simpler tests on `EvaluationService`)

### What gets added to `ff_flag`

Two new columns:

```sql
target_user_id  VARCHAR  -- nullable, exact match on context.userId
target_region   VARCHAR  -- nullable, exact match on context.region
```

### Evaluation result reason codes

| Code | Meaning |
|---|---|
| `FLAG_DISABLED` | flag.enabled is false |
| `NO_REGION_MATCH` | targetRegion set but context.region does not match |
| `NO_USER_MATCH` | targetUserId set but context.userId does not match |
| `MATCH` | all conditions passed, flag value returned |
| `FLAG_NOT_FOUND` | flag does not exist in snapshot |

### Explainability response after simplification

```json
{
  "flagKey": "new-checkout",
  "enabled": true,
  "value": "true",
  "reasonCode": "MATCH",
  "targetRegion": "us-east",
  "targetUserId": null,
  "releaseKey": "release-2026-05-checkout",
  "snapshotVersion": 3,
  "evaluatedAt": "2026-05-16T10:00:00Z"
}
```

Every field maps directly to one of the four quiz questions.

### web-admin UI after simplification

Flag form adds two optional fields:

```
Target Region  [ us-east        ]   (leave blank = match all regions)
Target User ID [ user-123       ]   (leave blank = match all users)
```

No separate rule section, no condition rows, no operator dropdown.
The full demo flow becomes: create flag → set targetRegion → publish → evaluate → see explain.

### Implementation steps

1. Add `target_user_id` and `target_region` columns to `ff_flag` in `SchemaInitializer`.
2. Add fields to `FlagEntity`, `CreateFlagRequest`, `UpdateFlagRequest`, `FlagResponse`.
3. Add fields to `SnapshotFlag` model.
4. Rewrite evaluation logic in `EvaluationService` (remove `EvaluationEngine`).
5. Update `PublishService.toSnapshotFlag` to include new fields.
6. Update `ExplainResponse` / `EvaluationResponse` DTOs.
7. Remove `ff_rule` table, `RuleEntity`, `RuleRepository`, rule-related DTOs and endpoints.
8. Update `DemoDataInitializer` seed data (remove `addRule` call, set `targetRegion=us-east`).
9. Replace `EvaluationEngineTest` with tests covering the four reason codes.
10. Update `web-admin` flag form to show `targetRegion` and `targetUserId` fields.

## Remaining Work

1. Run `mvn test` from repository root.
2. If network is available, restore normal H2 Maven dependency and remove local system jar workaround.
3. Fix Maven Surefire/JUnit provider cache so `EvaluationEngineTest` runs automatically.
4. Run backend locally:
   - `mvn -pl backend spring-boot:run`
5. Verify backend endpoint returns `true` for `new-checkout` when `region=us-east`.
6. Run Java CLI demo.
7. Install/build frontend projects:
   - `frontend-sdk`
   - `web-admin`
   - `vue-demo`
8. Start `web-admin` on port `5173`.
9. Start `vue-demo` on port `5174`.
10. Verify browser-visible feature value in Vue demo.

## Expected Local Demo Data

The backend seeds this data in the `local` profile:

```text
appKey: checkout-service
environment: local
flagKey: new-checkout
subjectKey: any non-empty user id
region: us-east
expected value: true
```

If `region` is not `us-east`, the expected value is `false`.

## Notes

- The project intentionally implements an interview-sized demo, not a full production feature flag platform.
- Oracle is documented through the `prod` Spring profile, but the local demo uses H2.
- The backend uses immutable published snapshots and an in-memory latest snapshot cache.
- The Java SDK and frontend SDK currently call the remote evaluation API directly.
