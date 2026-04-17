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

### Issue #4: IBKR Parsers (Flex XML + CSV)

- xmlutil not needed — IBKR Flex XML is flat self-closing elements, regex extraction works
- IBKR Flex: filter `levelOfDetail="EXECUTION"` to avoid double-counting (ORDER + EXECUTION)
- IBKR Flex: sells have negative quantity — take `abs()` for the domain model
- IBKR CSV: multi-section state machine — section name in col 0, row type in col 1
- IBKR CSV: Financial Instrument Information section appears after Trades — need backfill
  pass to attach ISINs to already-parsed trades
- IBKR CSV: DataDiscriminator filtering — skip ClosedLot/OpenLot rows
- IBKR CSV: datetime has embedded comma ("2024-01-15, 09:30:00") — RFC 4180 quoting handles it
- IBKR dividend description format: `AAPL(US0378331005) Cash Dividend...` — regex to extract symbol

### Issue #3: Parser Framework + Lightyear CSV Parser

- In-house RFC 4180 CSV parser needed (CsvParser.kt) — handles quoted fields, embedded
  commas, escaped quotes, CRLF/LF
- Parser pattern: BrokerParser interface → canParse(content) + parse(content) → ParseResult
- Lightyear CSV has DD/MM/YYYY dates — `parseDdMmYyyy()` helper
- Lightyear maps CUSTODY_FEE and FX_FEE → FEE; doesn't break out fees per trade
- Cash transactions (DEPOSIT/WITHDRAWAL) have null ticker, null quantity — parser must handle
- ImportedTransaction is a parser-output DTO, not the domain Transaction model

### Issue #2: Domain Models + Database Schema

- SQLDelight wasmJs: use `createDefaultWebWorkerDriver()` — avoids `js()` restriction
- Kotlin/Wasm: `js()` calls must be top-level expressions, not inside function bodies
- expect/actual classes produce Beta warnings in Kotlin 2.3.20 (harmless)
- SQL reserved words: use `txn` as table name instead of `transaction`
- Holding.fromTransactions() — proportional cost basis reduction on sells:
  `costReduction = totalCost * (soldQty / totalQty)`
- Tests run via `./gradlew wasmJsTest` — executes in ChromeHeadless

### Issue #1: Build Config + Dependencies

- kotlin-csv doesn't support wasmJs — CSV parsing must be done in-house in commonMain
- `compose.runtime`, `compose.foundation` etc. are deprecated accessors in Compose MP 1.10.x
  (warnings only, still works — migrate to direct Maven coordinates later)
- compileSdk 36 required — a transitive dependency pulled it in
- `compileKotlinAndroid` is ambiguous in KMP — use `compileDebugKotlinAndroid`
- `wasmJs {}` block needs `@OptIn(ExperimentalWasmDsl::class)`
