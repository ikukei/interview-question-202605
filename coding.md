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

## Modeling Notes After Review (2026-05-17)

### Current State

The current implementation still uses the richer rule-engine model:

- `ff_application`
- `ff_flag`
- `ff_rule`
- `ff_config_snapshot`
- `ff_audit_log`

This means the simplification plan above has not been implemented yet. The current code can express generic conditions through `ff_rule.condition_json`, including `region equals us-east` and `region in us-east,us-west`.

### Why a Fully Flat Flag Table Is Not Enough

A completely flat `ff_flag` table is too limited for real feature management.

Important cases that break a single flat record:

- The same flag can be used in multiple environments, such as `local`, `dev`, `uat`, and `prod`.
- The same flag can target multiple regions.
- The same flag can be associated with one or more releases.
- The same flag can gradually roll out to 1%, 5%, 25%, and then 100% of users.
- The same flag may need to roll back from 100% to 25%, or be fully disabled as a kill switch.
- The same flag may only apply to app versions within a range, such as `>= 2.5.0` and `< 3.0.0`.

So the better direction is:

```text
Keep the flag definition simple.
Keep runtime configuration scoped and versioned.
Keep targeting rules flexible.
Keep every publish or rollback as an immutable snapshot.
```

### Recommended Future Data Model

If the project is refined further, use this model instead of putting all dimensions directly into `ff_flag`.

```text
ff_flag
  id
  flag_key
  name
  description
  type
  owner
  status

ff_flag_config
  id
  flag_id
  app_key
  environment
  enabled
  default_value
  status

ff_rule
  id
  config_id
  priority
  condition_json
  rollout_percentage
  variation_value
  status

ff_release_binding
  id
  config_id
  release_key

ff_config_snapshot
  id
  app_key
  environment
  version
  checksum
  snapshot_json
  published_by
  published_at

ff_change_event
  id
  flag_id
  config_id
  app_key
  environment
  event_type
  from_value_json
  to_value_json
  from_snapshot_version
  to_snapshot_version
  actor
  reason
  created_at
```

### Environment Promotion

Typical flow:

```text
local -> dev -> uat -> prod
```

This should be represented as promotion of configuration, not mutation of one shared row.

Example:

```text
ff_flag
  flag_key = new-checkout

ff_flag_config
  flag_id = 1, environment = local, enabled = true
  flag_id = 1, environment = dev,   enabled = true
  flag_id = 1, environment = uat,   enabled = true
  flag_id = 1, environment = prod,  enabled = false
```

A promotion action should:

1. Read source environment config.
2. Copy it to target environment config.
3. Publish a new target environment snapshot.
4. Write a `PROMOTE` event to `ff_change_event`.

Suggested API:

```text
POST /api/v1/flags/{flagKey}/promote
```

Request:

```json
{
  "appKey": "checkout-service",
  "fromEnvironment": "uat",
  "toEnvironment": "prod",
  "actor": "demo-user"
}
```

### Progressive Rollout

After a flag reaches production, it often should not immediately go to all users.

Typical flow:

```text
prod 1% -> 5% -> 25% -> 50% -> 100%
```

This should be represented on `ff_rule.rollout_percentage`, not on `ff_flag.enabled` alone.

Evaluation should use deterministic hashing:

```text
bucket = hash(flagKey + ":" + subjectKey) % 100
enabled if bucket < rolloutPercentage
```

This keeps user assignment stable across requests.

Every rollout change should create:

- a new config snapshot
- a `ROLLOUT_INCREASE` or `ROLLOUT_DECREASE` event

### Rollback, Ramp-Down, and Kill Switch

Failure scenarios must support:

- `100% -> 25% -> 5%`
- rollback to an earlier snapshot
- emergency full disable

Do not delete history. Rollback is also a new event and should produce a new snapshot.

Useful event types:

```text
PROMOTE
DEMOTE
ROLLOUT_INCREASE
ROLLOUT_DECREASE
ROLLBACK_SNAPSHOT
KILL_SWITCH
CONFIG_UPDATE
PUBLISH
```

Example kill switch event:

```json
{
  "eventType": "KILL_SWITCH",
  "environment": "prod",
  "fromValue": {
    "enabled": true
  },
  "toValue": {
    "enabled": false
  },
  "reason": "checkout error rate increased"
}
```

### Version Range Targeting

App version targeting is required for mobile or frontend clients.

Do not compare versions as plain strings.

Use a strict version format:

```text
MAJOR.MINOR.PATCH
```

Examples:

```text
2.5.0
2.10.1
3.0.0
```

Supported operators:

```text
versionEq
versionGt
versionGte
versionLt
versionLte
versionBetween
```

Example conditions:

```json
[
  {
    "attribute": "appVersion",
    "operator": "versionGte",
    "value": "2.5.0"
  },
  {
    "attribute": "appVersion",
    "operator": "versionLt",
    "value": "3.0.0"
  }
]
```

This means:

```text
Enable from 2.5.0.
Disable before 3.0.0.
```

Invalid versions should either be rejected when saving the rule or return a clear reason code such as `INVALID_VERSION`.

### Multiple Region Targeting

The current rule engine can already represent multiple regions using the existing `in` operator.

Current single-region example:

```json
{
  "attribute": "region",
  "operator": "equals",
  "value": "us-east"
}
```

Multiple-region example:

```json
{
  "attribute": "region",
  "operator": "in",
  "value": "us-east,us-west,eu-central"
}
```

The backend can evaluate this today, because `EvaluationEngine` supports `in`.

Current limitation:

```text
web-admin does not expose a rule editor, so users cannot conveniently configure multiple regions from the UI.
```

### Recommended Direction

Do not fully flatten the model.

Better interview-ready model:

```text
ff_flag         = logical feature definition
ff_flag_config  = app/environment-scoped runtime config
ff_rule         = targeting and rollout rules
ff_snapshot     = immutable published runtime artifact
ff_change_event = audit, promotion, rollback, rollout history
```

This model can answer the quiz questions:

- Is it enabled?
- For whom?
- In which region?
- Associated with which release?

It can also support realistic lifecycle operations:

- environment promotion
- gradual rollout
- rollback
- kill switch
- version range targeting
- multiple-region targeting

## Implementation Plan: Minimal Evolution Model and Web Admin UX (2026-05-17)

### User Request

Refactor the project toward the minimal 6-table evolution model and update `web-admin` so the demo is easier to understand:

1. Show backend server at the top of `web-admin`.
2. Split admin flow into two steps:
   - Create flag definition.
   - Configure flag for apps, environment, regions, subject group, release, and rollout.
3. Use label `flag` in the UI instead of `flagKey`.
4. Remove `name` from UI; keep description.
5. Use `value` as free text input and also provide quick true/false selection.
6. App selection should be a multi-select list with demo apps such as `vue-demo`, `java-demo`, `python-demo`, `go-demo`.
7. Region selection should be a multi-select list using five continents.
8. Subject group should be selected from user groups such as internal users and VIP.
9. Release should be a text input with quick choices for the next 7 days in `yyyyMMdd` format.
10. Environment should support `local`, `dev`, `sit`, `uat`, `prod`.
11. Show environment promotion flow `local -> dev -> uat -> prod` with promotion buttons.
12. Show rollout slider and buttons: increase, decrease, full, kill switch.
13. Version targeting is intentionally out of scope for now.
14. `condition_json` should use a simple JSON object format, for example:

```json
{
  "region": "Asia,Europe",
  "subject": "vip"
}
```

Do not use generic operator shape such as:

```json
{
  "attribute": "appVersion",
  "operator": "versionLt",
  "value": "3.0.0"
}
```

15. `Submit` and `Approve` buttons are fake demo buttons and should show a tip that they are unavailable in the demo.
16. `Publish` is the real action that publishes the snapshot used by demos.
17. Evaluation section should use dropdowns wherever possible.
18. SDKs and demos should support the same app, region, subject group, release-related attributes.
19. Update `coding.md` continuously.
20. Commit in smaller checkpoints where possible.

### Target Minimal 6 Tables

```text
ff_application
ff_flag
ff_flag_config
ff_rule
ff_config_snapshot
ff_change_event
```

### Implementation Strategy

Keep the public demo small while improving the model:

- `ff_flag`: logical definition only. Store `flag_key`, description, type, status.
- `ff_flag_config`: app/environment-scoped config. Store app, environment, enabled, value, release, rollout percentage.
- `ff_rule`: targeting condition for one config. Store `condition_json` as a simple JSON object.
- `ff_config_snapshot`: immutable published artifact.
- `ff_change_event`: business event log replacing the previous generic audit direction.
- `ff_application`: app catalog for selectable demo apps.

### Progress

- 2026-05-17: Plan recorded.
- 2026-05-17: Backend refactor started. Added `ff_flag_config`, moved publish snapshots to app/environment configs, changed `ff_rule.condition_json` to generic JSON-object matching, and renamed persisted audit storage to `ff_change_event`.
- 2026-05-17: Web admin updated to two-step flow: create logical flag, then configure apps/regions/subject/release/environment/rollout; Submit and Approve are demo-only tips, Publish is the real snapshot action.
- 2026-05-17: SDKs and demos updated to carry `region`, `subject`, and `release`. Defaults now use `vue-demo` and `java-demo`.
- 2026-05-17: Verification so far: `mvn -s .mvn/offline-settings.xml -DskipTests -pl backend,java-sdk,java-demo -am package` passed. `web-admin`, `frontend-sdk`, and `vue-demo` builds passed. Backend JUnit test execution and `spring-boot:run` are blocked in the sandbox by denied writes under `D:/Java/maven-repository`; escalation attempts timed out.
- 2026-05-17: Tried to make the requested incremental commit, but the sandbox cannot create `.git/index.lock` (`Permission denied`). Escalated git attempt also timed out in auto-review. Pending commit message: `Implement minimal feature config model`.
- 2026-05-17: Local run attempt: sandbox can start the backend with the generated `backend-run.args` manual classpath, but background processes are reclaimed when a shell command finishes. Escalated attempts to start long-running local processes timed out twice.
- 2026-05-17: Runtime fix applied during startup test: H2 2.x treats `value` as a sensitive keyword, so `ff_flag_config.value` was renamed to database column `flag_value` while keeping API/UI field name `value`.
- 2026-05-17: To run locally from PowerShell until Maven repository permissions are fixed:

```powershell
cd D:\YuHui\Studio\Projects_2026\interview-question-202605
java @backend-run.args
```

```powershell
cd D:\YuHui\Studio\Projects_2026\interview-question-202605\web-admin
npm.cmd run dev -- --host 127.0.0.1
```

```powershell
cd D:\YuHui\Studio\Projects_2026\interview-question-202605\vue-demo
npm.cmd run dev -- --host 127.0.0.1
```

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
