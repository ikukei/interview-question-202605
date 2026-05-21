# Local Coding Progress

Last updated: 2026-05-18

## Goal

Implement the feature management take-home project in subdirectories:

1. `backend`: Spring Boot Maven backend, H2 local database, Oracle production profile placeholder.
2. `web-admin`: Vue admin frontend that calls backend APIs.
3. `java-sdk`: Java SDK for feature evaluation.
4. `frontend-sdk`: TypeScript frontend SDK for feature evaluation.
5. `java-demo`: Java command-line demo using `java-sdk`.
6. `vue-demo`: Vue demo page using `frontend-sdk`.
7. `python-sdk`: Python SDK for feature evaluation (stdlib only, no external deps).
8. `python-demo`: Python command-line demo using `python-sdk`.

## Architecture

```
web-admin (Vue)          vue-demo (Vue)         java-demo (CLI)
     |                        |                       |
     |           frontend-sdk (TS)        java-sdk (Java)
     |                        |                       |
     +------------------------+-----------------------+
                              |
                    backend (Spring Boot)
                              |
                     H2 (local) / Oracle (prod)
```

**Evaluation flow (immutable snapshot):**

1. Admin configures flag → Configure = flag + rules per app/environment.
2. Admin publishes → `PublishService` builds a snapshot JSON and stores it in `ff_config_snapshot`.
3. SDK calls `POST /api/v1/evaluations:batch` → `EvaluationService` reads the latest snapshot from `SnapshotCache`.
4. `EvaluationEngine` evaluates each flag against the request context.

## Current Database Schema

Six tables. All tables have `created_at` and `updated_at` (except `ff_config_snapshot` which has `published_at`).

```sql
-- app catalog
ff_application (
  id           NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  app_key      VARCHAR(100) UNIQUE NOT NULL,
  name         VARCHAR(200),
  owner        VARCHAR(200),
  created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
)

-- logical flag definition (global, not per-app)
ff_flag (
  id           NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  flag_key     VARCHAR(200) UNIQUE NOT NULL,
  description  VARCHAR(500),
  type         VARCHAR(50),          -- e.g. "boolean"
  release_key  VARCHAR(100),
  enabled      NUMBER(1) DEFAULT 1,  -- global kill switch: 1=on, 0=off
  created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
)

-- per-app/environment configuration
ff_flag_config (
  id                  NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  flag_id             NUMBER NOT NULL,
  app_key             VARCHAR(100) NOT NULL,
  environment         VARCHAR(50) NOT NULL,
  enabled             NUMBER(1) DEFAULT 1,  -- per-app/env toggle
  release_key         VARCHAR(100),
  rollout_percentage  NUMBER(3) DEFAULT 100,
  status              VARCHAR(50) DEFAULT 'active',  -- 'active' | 'archived'
  created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
)

-- targeting rules (one per config in current impl)
ff_rule (
  id                  NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  config_id           NUMBER NOT NULL,
  condition_json      CLOB,   -- Map format: {"region":["Asia"],"subject":"vip"}
  rollout_percentage  NUMBER(3) DEFAULT 100,
  created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
)

-- immutable published snapshot
ff_config_snapshot (
  id            NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  app_key       VARCHAR(100),
  environment   VARCHAR(50),
  version       NUMBER,
  checksum      VARCHAR(64),
  snapshot_json CLOB,
  published_by  VARCHAR(200),
  published_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
)

-- audit / change log
ff_change_event (
  id            NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  actor         VARCHAR(200),
  action        VARCHAR(100),
  resource_type VARCHAR(100),
  resource_key  VARCHAR(200),
  before_json   CLOB,
  after_json    CLOB,
  created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
)
```

**Key design rules:**
- `ff_flag.enabled = 0` → global kill switch; all apps/envs see `FLAG_DISABLED`.
- `ff_flag_config.status = 'archived'` → config excluded from snapshots entirely.
- `ff_flag_config.enabled = 0` → config included in snapshot but evaluates to `FLAG_DISABLED`.
- `ff_rule.condition_json` uses Map format: `{"region": ["Asia","North America"], "subject": "vip"}`.
- Rollout bucket: `SHA-256(flagKey + ":" + subjectKey) % 100`; enabled if bucket < `rolloutPercentage`.

## Current API Contracts

### Flags

```
GET    /api/v1/flags?appKey=&environment=     → List<FlagResponse>
POST   /api/v1/flags                          → FlagResponse     body: CreateFlagRequest
PUT    /api/v1/flags/{flagKey}                → FlagResponse     body: UpdateFlagRequest
POST   /api/v1/flags/{flagKey}/configure      → void             body: ConfigureFlagRequest
POST   /api/v1/flags/{flagKey}/archive        → void             body: {appKey, environment}
GET    /api/v1/flags/{flagKey}/audit          → List<AuditLogResponse>
POST   /api/v1/flags/{flagKey}/rules          → RuleResponse     body: AddRuleRequest
```

**DTOs:**
```java
CreateFlagRequest(String flagKey, String description, String type, String release)
UpdateFlagRequest(String description, String type, Boolean enabled, String release)
FlagResponse(Long id, Long configId, String flagKey, String appKey, String environment,
    String description, String type, boolean enabled, String releaseKey, String status,
    int rolloutPercentage, String conditionJson, List<RuleResponse> rules)
ConfigureFlagRequest(List<String> appKeys, String environment, List<String> regions,
    String subject, Boolean enabled, Integer rolloutPercentage, String conditionJson)
AddRuleRequest(String appKey, String environment, String conditionJson, int rolloutPercentage)
RuleResponse(Long id, String conditionJson, int rolloutPercentage)
AuditLogResponse(Long id, String actor, String action, String resourceType, String resourceKey,
    String beforeJson, String afterJson, Instant createdAt)
```

`FlagResponse.enabled` = `flag.enabled AND (config == null OR config.enabled)`.
`FlagResponse.status` = config status, or `null` if no config exists.

### Publish

```
POST /api/v1/publish    body: PublishRequest(appKey, environment, publishedBy)  → PublishResult
```

### Evaluation

```
POST /api/v1/evaluations/flags/{flagKey}   body: EvaluationRequest  → EvaluationResponse
POST /api/v1/evaluations:batch             body: BatchEvaluationRequest → List<EvaluationResponse>
POST /api/v1/evaluations/explain           body: EvaluationRequest  → ExplainResponse
GET  /api/v1/evaluations/flags             body: {appKey, environment} → List<String> flagKeys
```

**DTOs:**
```java
EvaluationContext(String subjectKey, String region, String subject, String releaseKey,
    Map<String, String> attributes)
EvaluationRequest(String appKey, String environment, EvaluationContext context)
BatchEvaluationRequest(String appKey, String environment, List<String> flagKeys,
    EvaluationContext context)
EvaluationResponse(String flagKey, boolean enabled, String reasonCode, String matchedRuleId,
    long snapshotVersion, String releaseKey)
ExplainResponse(String flagKey, boolean enabled, String reasonCode, String appKey,
    String environment, String subjectKeyHash, String matchedRuleId,
    List<String> matchedConditions, Integer rolloutBucket, String releaseKey,
    long snapshotVersion, Instant evaluatedAt)
```

**Reason codes:** `MATCH`, `RULE_MATCH`, `FLAG_DISABLED`, `NO_RULE_MATCH`, `FLAG_NOT_FOUND`,
`NO_SNAPSHOT`.

### Applications

```
GET  /api/v1/apps          → List<ApplicationResponse>
POST /api/v1/apps          body: {appKey, name, owner} → ApplicationResponse
GET  /api/v1/apps/{appKey} → ApplicationResponse
PUT  /api/v1/apps/{appKey} body: {name, owner}         → ApplicationResponse
```

### Audit

```
GET /api/v1/audit?flagKey= → List<AuditLogResponse>
```

## Demo Seed Data (DemoDataInitializer)

Active only under Spring profile `local`. Runs on every startup.

1. Creates flag `google-sso` (if not already present):
   - description: "Enables the Google SSO"
   - type: "boolean"
   - release: today's date in `yyyyMMdd` format

2. (Re-)configures `google-sso` for both `vue-demo` and `java-demo` in `local` environment:
   - regions: `["Asia", "North America"]`
   - subject: `"vip"`
   - enabled: `true`
   - rolloutPercentage: `100`
   - conditionJson: auto-built from regions/subject → `{"region":["Asia","North America"],"subject":"vip"}`

3. Publishes a snapshot for `vue-demo / local` (published by `demo-seed`).
4. Publishes a snapshot for `java-demo / local` (published by `demo-seed`).
5. Publishes a snapshot for `python-demo / local` (published by `demo-seed`).

**Expected evaluation result** for a request with `region=Asia, subject=vip`:
- `flagKey=google-sso`, `enabled=true`, `reasonCode=RULE_MATCH`

## SDK Interface Contracts

### TypeScript Frontend SDK (`frontend-sdk/src/index.ts`)

```typescript
interface FeatureContext {
  subjectKey: string;
  region?: string;
  subject?: string;
  releaseKey?: string;
  attributes?: Record<string, string>;
}

interface FeatureEvaluation {
  flagKey: string;
  enabled: boolean;
  reasonCode: string;
  matchedRuleId?: string | null;
  snapshotVersion: number;
  releaseKey?: string | null;
}

interface FeatureClient {
  evaluate(flagKey: string, context: FeatureContext): Promise<FeatureEvaluation>;
  boolVariation(flagKey: string, context: FeatureContext, defaultValue?: boolean): Promise<boolean>;
  listFlagKeys(): Promise<string[]>;
  evaluateAll(context: FeatureContext): Promise<FeatureEvaluation[]>;
}
```

### Python SDK (`python-sdk/feature_flag_sdk`)

```python
from feature_flag_sdk import FeatureClient, FeatureContext, FeatureEvaluation

context = FeatureContext(
    subject_key="my-user",
    region="Asia",
    subject="vip",
    release_key="20260518",
    attributes={"platform": "python-demo"},
)

client = FeatureClient(base_url="http://localhost:8080", app_key="python-demo", environment="local")

# evaluate one flag
ev: FeatureEvaluation = client.evaluate("google-sso", context)

# evaluate all flags
evs: list[FeatureEvaluation] = client.evaluate_all(context)

# bool shorthand with fallback
enabled: bool = client.bool_variation("google-sso", context, default_value=False)
```

`FeatureEvaluation` fields: `flag_key`, `enabled`, `reason_code`, `snapshot_version`,
`matched_rule_id`, `release_key`.

No external dependencies — uses `urllib.request` from stdlib. Requires Python 3.10+.

### Java SDK (`java-sdk`)

```java
FeatureContext.builder()
    .subjectKey(String)
    .region(String)
    .subject(String)
    .releaseKey(String)
    .attribute(String key, String value)
    .build()

FeatureEvaluation record: flagKey, enabled, reasonCode, matchedRuleId, snapshotVersion, releaseKey

FeatureClient.builder()
    .baseUrl(String)
    .appKey(String)
    .environment(String)
    .build()

client.evaluate(flagKey, context)    → FeatureEvaluation
client.evaluateAll(context)          → List<FeatureEvaluation>
client.listFlagKeys()                → List<String>
client.boolVariation(flagKey, context, defaultValue) → boolean
```

## Refactoring History

### 2026-05-15 — Initial build and JdbcTemplate migration

- Backend, Java SDK, frontend SDK, web-admin, vue-demo scaffolded.
- Replaced custom `FeatureDataSource` with standard Spring Boot `JdbcTemplate`.
- Backend verified locally.

### 2026-05-17 — Minimal 6-table model and web-admin UX overhaul

- Added `ff_flag_config` table: decouples flag definition from app/environment config.
- Added `ff_change_event` table: business-level audit replacing generic log.
- `ff_rule.condition_json` changed to Map format: `{"region":["Asia"],"subject":"vip"}`.
- Web admin redesigned: two-step flow (create flag → configure for apps/env), pipeline
  promotion UI, rollout slider.
- SDKs and demos updated to carry `region`, `subject`, `releaseKey` context fields.

### 2026-05-18 — DTO and schema cleanup

**Removed from `ff_flag`:** `app_key`, `environment`, `name`, `default_value`, `status` (replaced by `enabled` boolean).

**Removed from `ff_rule`:** `flag_id`, `variation_value`, `priority`, `enabled`.

**DTOs cleaned up:**
- `CreateFlagRequest`: 4 fields only — `flagKey, description, type, release`.
- `UpdateFlagRequest`: `description, type, enabled, release` (no `name`, no `status`).
- `FlagResponse`: removed `name`, `defaultValue`; `enabled` is computed from flag + config.
- `AddRuleRequest`: removed `conditions` array (was dead/incompatible); only `appKey, environment, conditionJson, rolloutPercentage`.
- `RuleResponse`: 3 fields — `id, conditionJson, rolloutPercentage` (no `priority`, `enabled`).
- `EvaluationContext`: removed `subjectGroup` alias, `release` alias; canonical fields are `subjectKey, region, subject, releaseKey`.
- `EvaluationResponse` / `FeatureEvaluation`: removed `value` (was duplicate of `enabled`).

**Dead code removed:**
- `FlagConfigEntity.archiveFlag` no longer calls `setEnabled(false)` — archived configs are already excluded from snapshots by `PublishService`; setting `enabled=false` was redundant.
- `PublishService`: removed `.filter(RuleEntity::isEnabled)` — `RuleEntity.enabled` field was removed.
- `FlagRepository`: removed `findByAppKeyAndEnvironment*` methods that referenced removed columns.
- `RuleRepository`: renamed `findByConfigIdOrderByPriorityAsc` → `findByConfigId` (orders by id).
- `EvaluationEngine.attributeValue`: simplified aliases for `subject`/`subjectGroup`, `release`/`releaseKey`.

**SDKs updated to match:**
- `frontend-sdk`: `evaluateAll` context uses `releaseKey` (not `release`).
- `java-sdk`: `FeatureContext.releaseKey()`, `FeatureEvaluation` has no `value` field, `boolVariation` uses `evaluation.enabled()`.
- `java-demo`: `.releaseKey(releaseKey)`, `eval.enabled()`.
- `vue-demo`: context uses `releaseKey`, card class bound to `item.enabled`.

**New endpoints/services:**
- `AuditController`: `GET /api/v1/audit?flagKey=` returning `List<AuditLogResponse>`.
- `AuditService.getAuditHistory(flagKey)` using `AuditLogRepository.findByResourceKeyContaining`.

## How to Run Locally

```powershell
# 1. Build backend and Java SDK (offline)
mvn -s .mvn/offline-settings.xml -DskipTests -pl backend,java-sdk,java-demo -am package

# 2. Start backend (one of these)
mvn -s .mvn/offline-settings.xml -pl backend spring-boot:run
# or
java @backend-run.args

# 3. Start web-admin (port 5173)
cd web-admin
npm run dev -- --host 127.0.0.1

# 4. Start vue-demo (port 5174)
cd vue-demo
npm run dev -- --host 127.0.0.1

# 5. Run Java CLI demo
cd java-demo
mvn -s .mvn/offline-settings.xml exec:java

# 6. Run Python demo (no install needed)
cd python-demo
python main.py
```

Or use the convenience script in the repository root:

```cmd
start-all-local.cmd
```

## Remaining Work

1. Verify `mvn clean compile` passes on backend (confirmed passing as of 2026-05-18).
2. Run `mvn test` when Surefire/JUnit provider cache is complete.
3. Start backend and manually verify `/api/v1/evaluations:batch` returns `enabled=true` for
   `google-sso` with `region=Asia, subject=vip`.
4. Verify vue-demo and java-demo connect and display flag evaluations.
5. (Optional) Add promotion endpoint `POST /api/v1/flags/{flagKey}/promote`.
6. (Optional) Add version range targeting operator to `EvaluationEngine`.

## Local Environment Notes

- Global Maven settings point to `D:/Java/maven-repository` (read-only from sandbox).
- Use `-s .mvn/offline-settings.xml` for offline builds.
- Spring Boot `3.5.x` with H2 in-memory or file-based store (`backend/data/feature-flags.h2.db`).
- CORS origins configured for `http://127.0.0.1:5173` and `http://127.0.0.1:5174`.
- Oracle production profile placeholder in `application-prod.yml`; local demo uses H2.
