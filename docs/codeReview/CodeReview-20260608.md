## MediAlert 项目深度代码审查报告

**项目概述：** 用药提醒/药箱库存管理 Android 应用，基于 Compose + Room + MVVM 架构。

---

### 严重问题 (Critical)

**[严重] MedicationViewModel.kt:87-93 - `getMedicationById` 同步返回异步结果，编辑页面必定白屏**

```kotlin
fun getMedicationById(id: Long): Medication? {
    var result: Medication? = null
    viewModelScope.launch {
        result = repository.getMedicationById(id)
    }
    return result  // ← 协程尚未执行，result 永远是 null
}
```

`viewModelScope.launch` 创建的协程不会立即执行，方法在协程启动后立刻返回 `result`（此时仍为 `null`）。`EditMedicationScreen.kt:23` 调用此方法后，`medication` 永远为 null，导致编辑页面的 `medication?.let { ... }` 代码块永远不会执行——**编辑功能完全不可用**。

修复建议：改为 StateFlow 暴露数据，或使用 `LaunchedEffect` + `suspend fun` 模式：

```kotlin
// ViewModel 中
fun getMedicationFlow(id: Long): Flow<Medication?> =
    flow { emit(repository.getMedicationById(id)) }

// EditMedicationScreen 中
val medication by viewModel.getMedicationFlow(medicationId)
    .collectAsStateWithLifecycle(initialValue = null)
```

---

**[严重] BootReceiver.kt:17-21 - 不受控的 CoroutineScope 导致协程泄漏**

```kotlin
CoroutineScope(Dispatchers.IO).launch {
    database.medicationDao().getAll().collect { medications ->
        alarmScheduler.rescheduleAllAlarms(medications.filter { it.isActive })
    }
}
```

`CoroutineScope(Dispatchers.IO)` 创建了一个没有任何取消机制的"孤儿"作用域。而 `getAll()` 返回的 `Flow` 是无限流（Room Flow 会持续观察数据变化），`collect` 永远不会自然终止。这个协程将在进程存活期间永远运行，造成资源浪费。

此外 `rescheduleAllAlarms` 在每次数据库变更时都会被重复触发，包括取消并重新注册所有闹钟，效率极低。

修复建议：使用 `goAsync()` 配合一次性查询（非 Flow），或使用 WorkManager：

```kotlin
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val medications = database.medicationDao().getAllSync() // suspend fun，非 Flow
                AlarmScheduler(context).rescheduleAllAlarms(medications)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
```

---

**[严重] MedicationViewModel.kt:158-199 - CSV 导出在主线程执行文件 I/O**

`exportToCsv()` 是一个普通函数，在 SettingsScreen 的 `Modifier.clickable` 中被直接调用。内部的 `CSVWriter(FileWriter(file)).use { ... }` 涉及磁盘写入，阻塞主线程，可能导致 ANR。

此外 `Environment.getExternalStoragePublicDirectory()` 在 Android 10+ (API 29) 已受限，Android 11+ (API 30) 起需要 `MANAGE_EXTERNAL_STORAGE` 权限，当前代码在高版本设备上会直接失败。

修复建议：

```kotlin
fun exportToCsv(context: Context): suspend fun() -> String? {
    return withContext(Dispatchers.IO) {
        // 使用应用私有目录或 MediaStore API
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        // ... 写入逻辑
    }
}
```

---

### 警告 (Warning)

**[警告] Medication.kt:25-28 - `dailyConsumption()` 存在除零风险**

```kotlin
fun dailyConsumption(): Double = when (frequencyType) {
    FrequencyType.EVERY_X_DAYS -> dailyDosage / frequencyValue
    FrequencyType.EVERY_XTH_DAY -> dailyDosage / (frequencyValue + 1)
}
```

当 `frequencyValue` 为 0 时，`EVERY_X_DAYS` 分支会导致 `ArithmeticException`（若 dailyDosage 为整数）或返回 `Infinity/NaN`（Double 运算）。虽然 UI 层有 `toIntOrNull() ?: 1` 的保护，但 Entity 本身缺乏防御。

修复建议：

```kotlin
fun dailyConsumption(): Double {
    val divisor = when (frequencyType) {
        FrequencyType.EVERY_X_DAYS -> frequencyValue
        FrequencyType.EVERY_XTH_DAY -> frequencyValue + 1
    }
    return if (divisor > 0) dailyDosage / divisor else dailyDosage
}
```

---

**[警告] AndroidManifest.xml:9-10 - `SCHEDULE_EXACT_ALARM` 在 Android 12+ 需要用户授权**

Android 12 (API 31) 起，`SCHEDULE_EXACT_ALARM` 默认未授予，需要引导用户在系统设置中手动开启。`AlarmManager.kt:33` 调用 `setExactAndAllowWhileIdle` 时，若权限未授予会抛出 `SecurityException`。代码中没有任何权限检查逻辑。

修复建议：在注册闹钟前检查权限状态：

```kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    if (!alarmManager.canScheduleExactAlarms()) {
        // 引导用户跳转设置页面
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
        context.startActivity(intent)
        return
    }
}
```

---

**[警告] AndroidManifest.xml:12 - `POST_NOTIFICATIONS` 运行时权限未申请**

Android 13 (API 33) 起，发送通知需要 `POST_NOTIFICATIONS` 运行时权限。`AlarmReceiver` 的通知在用户未授权时会被系统静默丢弃。

修复建议：在 MainActivity 或首次启动时请求权限：

```kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
}
```

---

**[警告] MedicationViewModel.kt:23-28 - ViewModel 重复创建 Repository 实例**

`MediAlertApplication` 已通过 `lazy` 创建了 `database` 和 `repository` 实例，但 `MedicationViewModel` 又自行创建了一套新的实例。这导致应用内存在两个并行的 Database 连接和 Repository 实例，既浪费资源，也可能引发数据不一致。

修复建议：通过 ViewModelFactory 从 Application 获取共享实例：

```kotlin
class MedicationViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as MediAlertApplication
    private val repository = app.repository
    // ...
}
```

---

**[警告] MergedAlertScreen.kt:59-95 - LazyColumn 中使用 `forEach` + `item{}` 替代 `items()`**

```kotlin
mergedAlerts.forEach { (alertDate, meds) ->
    item { ... }  // 每个 item 都会被立即组合，失去虚拟化能力
}
```

这会导致所有列表项一次性全部组合，在药品数量较多时造成性能问题和内存浪费。

修复建议：将 Map 转为 List 并使用 `items()`：

```kotlin
val alertList = remember(mergedAlerts) { mergedAlerts.toList() }
items(alertList, key = { it.first }) { (alertDate, meds) ->
    // ...
}
```

---

**[警告] SettingsScreen.kt:36 - 日历列表在权限授予后不会刷新**

```kotlin
val calendars = remember { calendarManager.getCalendars() }
```

`remember` 只在首次组合时执行一次。用户在授权日历权限后返回，`calendars` 仍为空列表（因为权限检查失败时返回 `emptyList()`），日历选择对话框将显示为空。

修复建议：使用 `mutableStateOf` + 权限回调中重新加载：

```kotlin
var calendars by remember { mutableStateOf(calendarManager.getCalendars()) }
// 在权限回调中：
calendars = calendarManager.getCalendars()
```

---

**[警告] Theme.kt:99-101 - `window.statusBarColor` 在 API 35 中已废弃**

修复建议：使用 `Activity.enableEdgeToEdge()` (activity 1.8+) 替代手动设置状态栏颜色。

---

**[警告] SettingsScreen.kt:100,123 - `Divider()` 在新版 Material3 中已废弃**

修复建议：使用 `HorizontalDivider()` 替代。

---

### 建议 (Suggestion)

**[建议] build.gradle.kts:26 - Release 构建未启用代码混淆**

`isMinifyEnabled = false` 意味着 release 构建不包含代码混淆和资源压缩，影响 APK 体积和代码安全。建议设为 `true` 并完善 proguard 规则（尤其是 Room 和 Coroutines 的 keep 规则）。

---

**[建议] AppDatabase.kt:18 - `exportSchema = false` 且无 Migration 策略**

没有 schema 导出和 Migration 定义，未来数据库结构变更时只能 `fallbackToDestructiveMigration()`，会导致用户数据丢失。建议启用 schema 导出并提前规划 Migration。

---

**[建议] MedicationDao.kt / StockLogDao.kt - Flow 查询建议指定 IO 线程**

Room 的 Flow 查询默认在调用线程 emit。建议在 Repository 层添加 `.flowOn(Dispatchers.IO)` 确保查询在 IO 线程执行：

```kotlin
fun getAllActiveMedications(): Flow<List<Medication>> =
    medicationDao.getAllActive().flowOn(Dispatchers.IO)
```

---

**[建议] AlarmReceiver.kt:64 - 使用了系统内置通知图标**

`android.R.drawable.ic_dialog_alert` 在不同设备上外观不一致，且可能不符合通知图标规范（Android 5.0+ 要求白色轮廓图标）。建议在 `res/drawable` 中添加专用的通知图标。

---

**[建议] MedicationCard.kt:33 / MergedAlertScreen.kt:30 - DateTimeFormatter 在每次重组时重建**

```kotlin
val dateFormatter = DateTimeFormatter.ofPattern("M月d日")  // 每次重组都创建新实例
```

修复建议：提取为 `companion object` 常量或使用 `remember`：

```kotlin
val dateFormatter = remember { DateTimeFormatter.ofPattern("M月d日") }
```

---

**[建议] 全局 - UI 字符串全部硬编码**

所有中文文本（如 "药箱库存管家"、"补充库存"、"暂无药品" 等）直接写在 Kotlin 代码中，未提取到 `strings.xml`。建议逐步提取，以便未来支持国际化。

---

**[建议] AddMedicationScreen.kt - 表单缺少输入验证反馈**

保存按钮仅通过 `genericName.isNotBlank() && packageSize.isNotBlank()` 控制启用状态，但缺少对 frequencyValue = 0、dailyDosage 为空等边界情况的校验提示。建议添加 `supportingText` 显示验证错误信息。

---

### 项目亮点

项目整体架构清晰，分层合理（Entity → DAO → Repository → ViewModel → Screen），代码可读性不错。以下几点值得肯定：

- **Flow + StateFlow 的使用规范**：`activeMedications` 和 `inactiveMedications` 使用 `stateIn(WhileSubscribed(5000))` 是标准的 Flow 转 StateFlow 模式，`collectAsStateWithLifecycle()` 的使用也是正确的生命周期感知方式。
- **Room 外键设计**：`StockLog` 与 `Medication` 之间通过 `ForeignKey(CASCADE)` 关联，删除药品时自动清理库存日志，设计合理。
- **合并提醒算法**：`MergedAlertScreen` 中将 3 天内的购药提醒合并展示的逻辑设计巧妙，用户体验好。
- **库存显示逻辑**：`getStockDisplay()` 将库存拆分为"包装 + 零散"的方式展示（如"2盒3片"），符合用户直觉。
- **StatusBadge 组件**：状态徽章的封装简洁优雅，颜色层级（红 → 黄 → 绿）语义清晰。
