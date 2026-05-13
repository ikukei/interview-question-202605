# Feature Management Service Demo

This repository contains a take-home sized feature management system.

## Modules

- `backend`: Spring Boot backend with H2 local database and Oracle production profile placeholder.
- `java-sdk`: Java SDK for remote feature evaluation.
- `java-demo`: Java command-line demo using `java-sdk`.
- `frontend-sdk`: TypeScript frontend SDK.
- `web-admin`: Vue admin UI for managing flags, publishing snapshots, and testing evaluation.
- `vue-demo`: Vue demo page using `frontend-sdk`.

## Verified Backend Flow

The backend seeds demo data in the `local` profile:

```text
appKey: checkout-service
environment: local
flagKey: new-checkout
rule: region equals us-east -> true
```

The real evaluation endpoint was verified locally:

```text
flagKey: new-checkout
value: true
reasonCode: RULE_MATCH
snapshotVersion: 1
releaseKey: release-2026-05-checkout
```

The Java CLI demo was also verified locally and prints:

```text
Feature flag: new-checkout
Feature value: true
Enabled: true
Reason: RULE_MATCH
Snapshot version: 1
Release: release-2026-05-checkout
```

## Local Maven Notes

This machine has a global Maven configuration pointing to `D:/Java/maven-repository`.

For this workspace, use:

```powershell
mvn -gs .mvn/offline-settings.xml -s .mvn/offline-settings.xml -o -DskipTests clean package
```

The current local environment could not download new dependencies because network approval did not complete. To keep the backend runnable here, the cached H2 jar was copied to:

```text
backend/lib/h2-1.3.168.jar
```

This is a local workaround. In a normal networked environment, replace the system-scoped H2 dependency with a normal Maven H2 runtime dependency.

## Run Backend

```powershell
mvn -gs .mvn/offline-settings.xml -s .mvn/offline-settings.xml -o -pl backend spring-boot:run
```

Backend URL:

```text
http://localhost:8080
```

## Test Evaluation Endpoint

```powershell
$body = @{
  appKey = "checkout-service"
  environment = "local"
  defaultValue = "false"
  context = @{
    subjectKey = "local-test-user"
    attributes = @{
      region = "us-east"
      platform = "powershell"
    }
  }
} | ConvertTo-Json -Depth 5

Invoke-RestMethod `
  -Uri "http://localhost:8080/api/v1/evaluations/flags/new-checkout" `
  -Method Post `
  -Body $body `
  -ContentType "application/json"
```

## Run Java Demo

The Maven exec plugin was not available in the local offline cache, so the demo was verified with direct `java -cp`.

After packaging:

```powershell
$cp = @(
  "java-demo\target\classes",
  "java-sdk\target\classes",
  "D:\Java\maven-repository\com\fasterxml\jackson\core\jackson-databind\2.21.2\jackson-databind-2.21.2.jar",
  "D:\Java\maven-repository\com\fasterxml\jackson\core\jackson-core\2.21.2\jackson-core-2.21.2.jar",
  "D:\Java\maven-repository\com\fasterxml\jackson\core\jackson-annotations\2.21\jackson-annotations-2.21.jar"
) -join ";"

java -cp $cp com.example.featuredemo.JavaFeatureDemo
```

## Frontend

Install dependencies when network access is available:

```powershell
cd frontend-sdk
npm.cmd install
npm.cmd run build

cd ..\web-admin
npm.cmd install
npm.cmd start

cd ..\vue-demo
npm.cmd install
npm.cmd start
```

Expected local URLs:

```text
web-admin: http://127.0.0.1:5173
vue-demo:  http://127.0.0.1:5174
```

The current local environment could not install Vue/Vite dependencies because npm network access was blocked.

## Production Notes

- Production database profile is documented as Oracle in `backend/src/main/resources/application-prod.yml`.
- Demo runtime uses local H2.
- The backend evaluates against published immutable snapshots and keeps the latest snapshot in an in-memory cache.
- Production extensions would include Redis, SDK local snapshot caching, streaming distribution, RBAC, audit dashboards, and OpenTelemetry.
