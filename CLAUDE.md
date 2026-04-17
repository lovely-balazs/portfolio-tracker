# Portfolio Tracker

KMP Compose Multiplatform app — tracks holdings across brokers in a single dashboard.

## Targets

- **wasmJs** (primary) — browser via Compose for Web
- **Android** (primary) — Compose on Android
- **iOS** — compiles, not polished

## Stack

- Kotlin 2.3.20, Compose Multiplatform 1.10.3
- SQLDelight (local SQLite, web-worker-driver for wasmJs)
- Ktor HTTP client (platform engines: OkHttp/Android, js/wasmJs)
- kotlinx-serialization, kotlinx-datetime
- Koala Plot (charts), FileKit (file picker)

## Workflow

- Work on `main`, one commit per GitHub issue
- Issues are sequential (Unit 2 → 3 → … → 8)
- Plan: `docs/plans/2026-04-17-001-feat-mvp-portfolio-tracker-plan.md`
- Requirements: `docs/brainstorms/2026-04-17-mvp-scope-requirements.md`

## Build

- `./gradlew compileKotlinWasmJs` — verify wasmJs
- `./gradlew compileDebugKotlinAndroid` — verify Android (needs `local.properties` with `sdk.dir`)
- `./gradlew compileKotlinIosSimulatorArm64` — verify iOS

## Learnings

Things discovered while implementing. Add new entries at the top.

### Issue #1: Build Config + Dependencies

- kotlin-csv doesn't support wasmJs — CSV parsing must be done in-house in commonMain
- `compose.runtime`, `compose.foundation` etc. are deprecated accessors in Compose MP 1.10.x
  (warnings only, still works — migrate to direct Maven coordinates later)
- compileSdk 36 required — a transitive dependency pulled it in
- `compileKotlinAndroid` is ambiguous in KMP — use `compileDebugKotlinAndroid`
- `wasmJs {}` block needs `@OptIn(ExperimentalWasmDsl::class)`
