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

### Issue #8: Settings + Manual Entry + Background Refresh

- SQLDelight `selectSetting` on `SELECT value FROM settings WHERE key = ?` returns `String?`
  directly (single TEXT column) — no wrapper type needed
- Manual assets get ticker format `MANUAL-NAME-SLUG` — PriceService naturally skips them
  since they're not STOCK/ETF/CRYPTO asset classes
- expect/actual BackgroundRefresh: WorkManager on Android (needs network constraint),
  `setInterval`/`clearInterval` via `@JsFun` on wasmJs, no-op stub on iOS
- iOS BackgroundRefresh requires BGTaskScheduler + Info.plist config — stub for MVP
- WorkManager RefreshWorker needs WorkerFactory DI for coroutine/DI integration —
  stub for MVP, signals success only
- App.kt wires full DI manually: database created per platform, passed to App() composable
- `remember { }` for all repositories and view models in App.kt — prevents re-creation
  on recomposition
- `LaunchedEffect(currentScreen)` triggers dashboard reload when navigating back

### Issue #7: Dashboard + Charts

- `String.format()` not available in wasmJs — need custom `formatDecimal()` in commonMain
- Koala Plot charts deferred to polish phase — text-based placeholders work for MVP
- HoldingsCalculator is a pure function (no DB access) — easy to test
- Holdings sorted by base-currency value descending for display
- FX conversion tolerance: use `abs(actual - expected) < threshold` in tests

### Issue #6: Price + FX Services

- Frankfurter FX conversion: `value_base = value_foreign / rate` (rate is "1 base = X foreign")
- FX conversion has floating point precision issues — use tolerance in tests
- `kotlin.time.Clock.System.now().toString().take(10)` gives ISO date string — quick hack
  for getting today's date without timezone complexity
- Ktor auto-resolved engine: just `HttpClient { }` in commonMain, no expect/actual needed
- CoinGecko uses coin IDs (lowercase) not ticker symbols
- PriceService routes by AssetClass: STOCK/ETF/BOND → Finnhub→Yahoo fallback, CRYPTO → CoinGecko
- Day-level FX rate caching: ECB updates daily, no need for more frequent refreshes

### Issue #5: Import Flow (Orchestration + File Picker + UI)

- `kotlinx.datetime.Clock` deprecated in 0.7.1 — use `kotlin.time.Clock` with `@OptIn(ExperimentalTime::class)`
- `@JsFun` in wasmJs needs `@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)`
- `crypto.randomUUID()` available in wasmJs via `@JsFun` for UUID generation
- FileKit 0.13.0 API: `PlatformFile` based, not `openFilePicker()` — decouple file
  picking from screen composable via callback parameter
- Import hash: simple polynomial string hash (`31 * hash + charCode`) — deterministic,
  cross-platform, no crypto dependency
- Instrument resolution order: ISIN match → ticker+currency match → create new

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
