# Feature Management Service Demo

A take-home sized feature flag management system: backend, admin UI, multi-platform SDKs, and live demo clients.

## Project Structure

```
backend/          Spring Boot REST API + rule engine + snapshot publisher
web-admin/        Vue 3 admin UI  — create flags, configure, publish
vue-demo/         Vue 3 demo page — shows live flag evaluations
frontend-sdk/     TypeScript SDK (used by web-admin and vue-demo)
java-sdk/         Java SDK
java-demo/        Java CLI demo — polls backend and prints results
python-sdk/       Python SDK (stdlib only, no extra dependencies)
python-demo/      Python CLI demo — polls backend and prints results
android-sdk/      Kotlin SDK stub (interface defined, not implemented)
ios-sdk/          Swift SDK stub  (interface defined, not implemented)
```

## Prerequisites

- JDK 17+
- Maven 3.9+
- Node.js 18+
- Python 3.10+

## Build

```bash
# 1. Build backend jar, Java SDK, and Java demo
mvn -pl backend,java-sdk,java-demo -am -DskipTests package

# 2. Build the TypeScript SDK (required by both Vue apps)
cd frontend-sdk
npm install
npm run build
cd ..

# 3. Install Vue app dependencies
cd web-admin
npm install
cd ..

cd vue-demo
npm install
cd ..
```

## Run

### Quick start

Double-click **`start-all-local.cmd`** (or run it from a terminal) to launch all five processes at once. Logs go to `logs/`.

### Start each process manually

Open a separate terminal for each:

**1. Backend** — `http://127.0.0.1:8080`

```cmd
java -jar backend\target\backend-0.1.0-SNAPSHOT.jar --spring.profiles.active=local
```

**2. web-admin** — `http://127.0.0.1:5173`

```cmd
cd web-admin
npm run dev -- --host 127.0.0.1 --port 5173
```

**3. vue-demo** — `http://127.0.0.1:5174`

```cmd
cd vue-demo
npm run dev -- --host 127.0.0.1 --port 5174
```

**4. java-demo**

```cmd
cd python-demo
python main.py http://127.0.0.1:8080 python-demo-user Asia vi <YYYYMMDD> 5
```

> The classpath also needs the Jackson jars from your local Maven cache. `start-all-local.cmd` resolves these automatically.

**5. python-demo**

```cmd
cd python-demo
python main.py http://127.0.0.1:8080 python-demo-user Asia vip <YYYYMMDD> 5
```

On first start the backend automatically seeds a demo flag — no manual setup needed.

## Demo Data (seeded automatically)

| Field | Value |
|---|---|
| Flag key | `google-sso` |
| Type | `boolean` |
| Apps | `vue-demo`, `java-demo`, `python-demo` |
| Environment | `local` |
| Target regions | `Asia`, `North America` |
| Target subject | `vip` |
| Rollout | 100 % |

A request with `region=Asia` and `subject=vip` returns:

```json
{ "flagKey": "google-sso", "enabled": true, "reasonCode": "RULE_MATCH" }
```

## Try the API

**Evaluate one flag:**

```bash
curl -X POST http://localhost:8080/api/v1/evaluations/flags/google-sso \
  -H "Content-Type: application/json" \
  -d '{
    "appKey": "vue-demo",
    "environment": "local",
    "context": { "subjectKey": "user-001", "region": "Asia", "subject": "vip" }
  }'
```

**Evaluate all flags (batch):**

```bash
curl -X POST "http://localhost:8080/api/v1/evaluations:batch" \
  -H "Content-Type: application/json" \
  -d '{
    "appKey": "vue-demo",
    "environment": "local",
    "flagKeys": ["google-sso"],
    "context": { "subjectKey": "user-001", "region": "Asia", "subject": "vip" }
  }'
```

**List flags:**

```bash
curl "http://localhost:8080/api/v1/flags?appKey=vue-demo&environment=local"
```

## H2 Database Console

While the backend is running, open the browser console at:

```
http://localhost:8080/h2-console

JDBC URL : jdbc:h2:file:./data/feature-flags;MODE=Oracle;DATABASE_TO_UPPER=false
User     : sa
Password : (leave blank)
```

## Architecture Diagrams

Pre-generated PNGs in the project root:

- `arch-logical-current.png` — current four-layer logical architecture
- `arch-network-current.png` — current network topology and ports
- `arch-improved.png` — industry-standard target architecture (CDN, Redis, PostgreSQL, observability)

## Production Notes

- Switch to Oracle by activating the `prod-oracle` Maven profile and setting `spring.profiles.active=prod`.
- The evaluation path reads from an in-memory snapshot cache — no database hit per request.
- See `coding.md` for full API contracts, DB schema, and improvement roadmap.
