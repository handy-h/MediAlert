# AGENTS.md
## 规则书写规范

- **所有规则必须使用中文书写**，包括本文件中的所有章节、规则描述、示例说明等。

---

## Code-Context MCP Tool Usage Rules

当探索、理解和分析本项目代码库时，**必须严格遵守以下工具调用优先级**。禁止直接使用效率低下的模糊搜索（如全局 Grep）或盲目读取大量文件源码，应优先使用专门的 code-context MCP 服务。

### 核心原则

> **搜索优于读取**：能用 `code_search` / `symbol_search` 定位的，绝不直接 `Read` 文件。

### 场景决策

- **场景 A：寻找某种业务逻辑或功能描述**（例如："处理用户认证"、"配置加载相关代码"）
  - 🚀 必须优先调用 `code_search` 进行语义搜索。
- **场景 B：已知具体函数名、类名或变量名**（例如：查找 `LoadConfig` 函数的所有引用）
  - 🚀 必须优先调用 `symbol_search` 进行精确符号查找。
- **场景 C：准备修改或重构某个核心函数、接口**（例如：删除/重命名某函数后影响哪些文件）
  - 🚀 必须在修改前调用 `impact_analysis` 评估影响范围。
- **场景 D：需要了解某个大文件的整体结构**（例如：先看摘要定位函数，再精确读取）
  - 🚀 优先调用 `file_context` 获取结构摘要，严禁直接读取整文件。

### 工具优先级矩阵

| 目标场景 | 首选工具 (MCP) | 禁用/降级工具 | 理由 |
| :--- | :--- | :--- | :--- |
| 按业务含义/功能模糊查找 | `code_search` | 全局 Grep / Glob | 语义搜索能更精准定位跨文件的关联逻辑，减少无关噪音。 |
| 查找特定符号及引用 | `symbol_search` | 文本全局搜索 | 符号查找具备 AST 级别精准度，避免同名字符串干扰。 |
| 评估修改影响/重构 | `impact_analysis` | 人肉追踪 / 逐个文件 Grep | 自动分析调用链，防止遗漏依赖。 |
| 快速熟悉新文件 | `file_context` | 直接读取整文件内容 | 节省 Context Token，先看摘要，按需精读。 |

### 工具选择速查

| 需求 | 工具 | 关键参数 |
|------|------|---------|
| 理解功能实现 | `code_search` | query=自然语言, top_k=5 |
| 查找定义/引用 | `symbol_search` | query=符号名, search_type=all\|definition\|reference |
| 修改前评估影响 | `impact_analysis` | action=delete/rename/modify |
| 了解文件结构 | `file_context` | mode=summary |
| 查看具体实现 | `file_context` | mode=full |

### 执行顺序

```
遇到问题 → code_search 或 symbol_search
         ↓
定位到文件 → file_context(mode=summary)
         ↓
需要细节 → file_context(mode=full)
         ↓
修改代码前 → impact_analysis
```

### 必须遵守的约束

1. **先索引，后搜索**：如果 `code_search` / `symbol_search` 返回空结果，立即执行 `index_project` 再重试
2. **禁止**修改符号前不做 `impact_analysis`
3. **默认** `file_context(mode=summary)`，确认需要细节才用 `full`
4. **默认** `top_k=5`，不盲目取 20
5. **禁止行为**：禁止在未进行符号或语义搜索的情况下，盲目读取超过 3 个以上的文件。
6. **索引维护**：如果发现本地索引未建立，或代码发生重大变更导致搜索结果不匹配，请主动调用 `index_project` 重新构建索引。
7. 会话结束前调用 `token_stats` 查看节省量。

---
## Project Overview

MediAlert (药箱库存管家) — Android app for managing home medicine inventory with depletion-date-based reminders. Kotlin + Jetpack Compose + Room, MVVM architecture.

## Build & Test Commands

```bash
./gradlew assembleDebug          # Build debug APK
./gradlew testDebugUnitTest      # JVM unit tests (fast, no device needed)
./gradlew connectedDebugAndroidTest  # Instrumented tests (requires device/emulator)
./gradlew installDebug           # Build + install on connected device
```

**Gotcha — KSP/Room tmpdir on Windows**: If Room/KSP codegen fails with a tmpdir permission error, set `-Dorg.sqlite.tmpdir` and `-Djava.io.tmpdir` to a writable temp directory in both `app/build.gradle.kts` (`kotlinDaemonJvmArgs`) and `gradle.properties` (`kotlin.daemon.jvmargs` / `systemProp.java.io.tmpdir`).

**Gotcha — JDK 17 required**: Set `JAVA_HOME` to JDK 17 or set `org.gradle.java.home` in `gradle.properties`. JBR 21 is incompatible with Android SDK platform jars (jlink issue).

## Architecture

```
MediAlertApplication          # Application subclass, owns Database + Repository singletons
  └─ AppDatabase (Room v3)    # Entities: Medication, StockLog
       ├─ MedicationDao       # CRUD + active/inactive queries (Flow + suspend)
       └─ StockLogDao         # Insert/query stock change logs

MainActivity                  # Single Activity, sets up Compose content
  └─ MediAlertApp()           # NavHost with string-based routes
       ├─ "list"              → MedicationListScreen
       ├─ "add"               → AddMedicationScreen
       ├─ "edit/{id}"         → EditMedicationScreen
       ├─ "inactive"          → InactiveMedicationsScreen
       ├─ "settings"          → SettingsScreen (calendar, export, import, reset)
       └─ "merged_alerts"     → MergedAlertScreen

MedicationViewModel           # AndroidViewModel, single instance shared across screens
  └─ ReminderManager          # Coordinates AlarmScheduler + CalendarManager
       ├─ AlarmScheduler      # AlarmManager (exact alarm, 1-day-before depletion)
       └─ CalendarManager     # System calendar events (4-day-before depletion)
```

### Navigation

Routes are plain strings (no sealed class / type-safe args). `edit/{medicationId}` passes a Long via path parameter. All screens use `viewModel()` which returns the same `MedicationViewModel` scoped to the Activity.

### Data Flow

1. Room DAOs return `Flow<List<T>>` for reactive queries
2. Repository wraps DAOs with `flowOn(Dispatchers.IO)`
3. ViewModel converts Flows to `StateFlow` via `stateIn(viewModelScope, WhileSubscribed(5000), emptyList())`
4. Screens collect via `collectAsStateWithLifecycle()`
5. All writes go through `viewModelScope.launch { ... }` → repository → DAO suspend functions

### Reminder System (Two-Tier)

| Tier | Trigger | Mechanism | When |
|------|---------|-----------|------|
| Calendar event | `CalendarManager` | System calendar ContentProvider | 4 days before depletion, 22:00 |
| Alarm | `AlarmScheduler` | `AlarmManager.setExactAndAllowWhileIdle` | 1 day before depletion, 14:00 |

**Key**: `ReminderManager.registerReminders()` always cancels old reminders first, then re-registers. After any stock/frequency change, reminders are re-registered automatically.

**Boot persistence**: `BootReceiver` re-registers all alarms on `BOOT_COMPLETED`. Calendar events persist in the system calendar (no re-registration needed).

## Key Domain Logic

### Medication Entity

- `dailyConsumption()`: `dailyDosage / divisor` where divisor = `frequencyValue` for EVERY_X_DAYS, `frequencyValue + 1` for EVERY_XTH_DAY
- `depletionDate()`: `startDate + ceil(currentStock / dailyConsumption())` days. Uses `ceil()` to never underestimate.
- `daysUntilDepletion()`: days from now to depletionDate (can be negative = already depleted)
- `getStockDisplay()`: formats stock as "2盒6片" (packages + remainder)
- `frequencyType` enum: `EVERY_X_DAYS` ("每X天") vs `EVERY_XTH_DAY` ("每隔X天") — these are semantically different (every 2 days ≠ every other day)

### Stock Model

Stock is stored as `Double` (total dosage units, not packages). "3盒+6片" with packageSize=14 → `3*14 + 6 = 48.0`. The `StockInputDialog` computes this conversion in the UI layer.

### Database Migrations

Current version: 3. Migrations are defined in `AppDatabase.kt`:
- v1→v2: Added `calendarEventId` column
- v2→v3: Changed `packageSize` from INTEGER to REAL (recreate table with correct column order — column order in INSERT/SELECT must match entity field declaration)

## Testing

### JVM Unit Tests (`app/src/test/`)

- `MedicationTest` — Pure logic tests for entity methods (dailyConsumption, depletionDate, getStockDisplay)
- `ConvertersTest` — Room TypeConverter round-trip tests
- `MedicationRepositoryTest` — Repository with MockK DAOs

Framework: JUnit 4 + MockK + `kotlinx-coroutines-test` (`runTest`)

### Instrumented Tests (`app/src/androidTest/`)

- `MedicationDaoTest` — Room DAO tests with in-memory database
- `StockLogDaoTest` — StockLog DAO + foreign key cascade tests
- `AlarmSchedulerTest` — Alarm scheduling (Android device required)

Framework: AndroidJUnit4 + Espresso + Room testing

**Test patterns**: Tests use `createSampleMedication()` factory methods with sensible defaults. Instrumented tests use `Room.inMemoryDatabaseBuilder().allowMainThreadQueries()`. Coroutine tests use `runTest { }`.

## Conventions

### Code Style

- **Language**: Kotlin, all comments and UI strings in Chinese (with English translations in `values-en/strings.xml`)
- **No DI framework**: Manual dependency injection via `MediAlertApplication` (Application subclass acts as service locator)
- **ViewModel**: `AndroidViewModel` (not plain ViewModel) because it needs `Application` context for SharedPreferences and to access the application-level repository
- **All UI strings resource-backed**: Every user-visible string uses `stringResource(R.string.xxx)` or `context.getString(R.string.xxx)`. Never hardcode strings in Composables.

### Input Filtering

Numeric inputs filter characters inline: `it.filter { c -> c.isDigit() || c == '.' }`. Stock input dialog uses `isDigit()` only for package count (integers) but allows decimals for loose units.

### SharedPreferences

Calendar account ID is stored in `"medialert_prefs"` under key `"calendar_id"`. The `prefs` property is `by lazy` to avoid blocking the main thread during ViewModel initialization.

### ProGuard

Release builds have `isMinifyEnabled = true`. ProGuard rules in `app/proguard-rules.pro` keep Room entities, Kotlin coroutines state machine, and OpenCSV classes. Uses `-dontoptimize` to protect coroutines.

### Repository Pattern

`MedicationRepository` is a thin pass-through to DAOs for most operations. The only non-trivial logic is `addStock`/`reduceStock` which update stock AND insert a `StockLog` entry. `reduceStock` clamps to 0 via `coerceAtLeast(0.0)`.

## Package Structure

```
com.handy.medialert/
├── alarm/           # AlarmScheduler, AlarmReceiver, BootReceiver
├── calendar/        # CalendarManager (system calendar CRUD)
├── data/
│   ├── entity/      # Medication, StockLog (+ enums FrequencyType, StockLogType)
│   ├── dao/         # MedicationDao, StockLogDao
│   └── database/    # AppDatabase, Migrations, TypeConverters
├── repository/      # MedicationRepository
├── reminder/        # ReminderManager (orchestrates alarm + calendar)
├── ui/
│   ├── screens/     # Composable screens (one per route)
│   ├── components/  # Reusable composables (MedicationCard, StockInputDialog, etc.)
│   └── theme/       # Material3 theme (Color, Theme, Type)
├── viewmodel/       # MedicationViewModel
├── MainActivity.kt  # Single Activity + NavHost
└── MediAlertApplication.kt  # Application, DB/Repo singletons, notification channel
```

## Localization

- Default (`values/strings.xml`): Chinese
- English: `values-en/strings.xml`
- CSV import parses frequencies in both Chinese ("每1天1片") and English ("Every 1 days 1 tablets") regex patterns

## Known Pitfalls

1. **Column order in Room migrations**: When recreating tables (v2→v3), column order in `INSERT INTO ... SELECT` must exactly match entity field declaration order, or data silently shifts.
2. **StateFlow timing**: Some operations (export, reset reminders) use `repository.getAllActiveMedications().first()` directly instead of `activeMedications.value` to avoid stale StateFlow snapshots.
3. **Calendar permission graceful degradation**: If calendar permissions are denied, the app falls back to alarm-only reminders. `calendarId` being null is a valid state.
4. **AlarmManager deprecation**: On Android 12+, `canScheduleExactAlarms()` must be checked; if false, falls back to inexact `alarmManager.set()`.
5. **Notification channel**: Created in `MediAlertApplication.onCreate()` with `IMPORTANCE_HIGH`. `AlarmReceiver` has a safety fallback to create it if missing.
