# Local Coding Progress

Last updated: 2026-05-14

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
- Backend JPA entities added:
  - `ApplicationEntity`
  - `FlagEntity`
  - `RuleEntity`
  - `ConfigSnapshotEntity`
  - `AuditLogEntity`
- Backend repositories added.
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

### In Progress

- Maven Surefire automatic test execution is blocked by incomplete local provider cache; test source exists, but local Surefire providers are incomplete.

### Local Environment Notes

- Global Maven settings force `D:/Java/maven-repository`, which is not writable from this workspace.
- Added project-local `.mvn/settings.xml`.
- Use `mvn -s .mvn/settings.xml ...` for local commands.
- Current fallback: use cached dependencies in `D:/Java/maven-repository`; backend is Spring Boot Web + H2 JDBC, not JPA, because Spring Data JPA is not cached locally and network approval did not complete.
- Added `.mvn/offline-settings.xml` for offline builds against existing `D:/Java/maven-repository` cache.
- Replaced temporary H2 `systemPath` dependency with standard Maven runtime dependency.
- Restored Spring Boot `3.5.14`; replaced `spring-boot-starter-test` with cached JUnit `4.13.2` + AssertJ `3.19.0` so tests can run offline with cached Surefire.
- Added Maven compiler `-parameters` support after local API test found Spring MVC parameter-name binding issue.
- Added CORS origins for `http://127.0.0.1:5173` and `http://127.0.0.1:5174` after browser showed `TypeError: Failed to fetch`.

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
