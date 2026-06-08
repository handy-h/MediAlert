# 药箱库存管家 - API 文档

## 1. 数据实体 API

### 1.1 Medication（药品实体）

```kotlin
@Entity(tableName = "medications")
data class Medication(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val genericName: String,           // 通用名
    val brandName: String?,            // 商品名
    val specification: String?,        // 规格文本（仅显示）
    val packageUnit: String,           // 包装单位：盒/支/瓶
    val dosageForm: String,            // 剂型：片/粒/ml/g
    val packageSize: Int,              // 每包装含量
    val currentStock: Double,          // 当前库存（总剂型数）
    val frequencyType: FrequencyType,  // 频率类型
    val frequencyValue: Int,           // 频率值
    val dailyDosage: Double,           // 每次剂量
    val startDate: LocalDate?,         // 开始日期（null=立即开始）
    val isActive: Boolean = true,      // 是否启用
    val createdAt: Long = System.currentTimeMillis()
)
```

#### 方法

| 方法 | 返回类型 | 说明 |
|------|----------|------|
| `dailyConsumption()` | Double | 计算日消耗量 |
| `depletionDate()` | LocalDate | 计算耗尽日期 |
| `daysUntilDepletion()` | Int | 计算距离耗尽天数 |
| `alert4DayDateTime()` | LocalDateTime | 提前4天提醒时间（22:00） |
| `alert1DayDateTime()` | LocalDateTime | 提前1天提醒时间（14:00） |
| `getStockDisplay()` | String | 格式化库存显示（如"2盒6片"） |

#### 频率类型

```kotlin
enum class FrequencyType {
    EVERY_X_DAYS,    // 每X天（如每2天1片 → 日消耗=0.5片）
    EVERY_XTH_DAY    // 每隔X天（如每隔2天1片 → 日消耗=0.33片）
}
```

### 1.2 StockLog（库存日志）

```kotlin
@Entity(tableName = "stock_logs")
data class StockLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicationId: Long,            // 关联药品ID
    val type: StockLogType,            // 操作类型
    val quantity: Double,              // 变更数量（正=增加，负=减少）
    val reason: String?,               // 操作原因
    val timestamp: Long = System.currentTimeMillis()
)
```

#### 日志类型

```kotlin
enum class StockLogType {
    PURCHASE,      // 采购补货
    MISSED_DOSE,   // 漏服补回
    LOST,          // 丢失/提前消耗
    ADJUSTMENT     // 手动调整
}
```

---

## 2. 数据库 DAO API

### 2.1 MedicationDao

```kotlin
@Dao
interface MedicationDao {
    // 查询所有启用的药品
    @Query("SELECT * FROM medications WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getAllActive(): Flow<List<Medication>>

    // 查询所有停用的药品
    @Query("SELECT * FROM medications WHERE isActive = 0 ORDER BY createdAt DESC")
    fun getAllInactive(): Flow<List<Medication>>

    // 查询所有药品
    @Query("SELECT * FROM medications ORDER BY createdAt DESC")
    fun getAll(): Flow<List<Medication>>

    // 根据ID查询
    @Query("SELECT * FROM medications WHERE id = :id")
    suspend fun getById(id: Long): Medication?

    // 插入药品，返回自增ID
    @Insert
    suspend fun insert(medication: Medication): Long

    // 更新药品
    @Update
    suspend fun update(medication: Medication)

    // 删除药品
    @Delete
    suspend fun delete(medication: Medication)

    // 更新库存（直接设置值）
    @Query("UPDATE medications SET currentStock = :stock WHERE id = :id")
    suspend fun updateStock(id: Long, stock: Double)

    // 设置启用/停用状态
    @Query("UPDATE medications SET isActive = :active WHERE id = :id")
    suspend fun setActive(id: Long, active: Boolean)
}
```

### 2.2 StockLogDao

```kotlin
@Dao
interface StockLogDao {
    // 查询某药品的所有日志
    @Query("SELECT * FROM stock_logs WHERE medicationId = :medicationId ORDER BY timestamp DESC")
    fun getLogsForMedication(medicationId: Long): Flow<List<StockLog>>

    // 插入日志
    @Insert
    suspend fun insert(log: StockLog)

    // 删除某药品的所有日志
    @Query("DELETE FROM stock_logs WHERE medicationId = :medicationId")
    suspend fun deleteLogsForMedication(medicationId: Long)
}
```

---

## 3. Repository API

### 3.1 MedicationRepository

```kotlin
class MedicationRepository(
    private val medicationDao: MedicationDao,
    private val stockLogDao: StockLogDao,
    private val context: Context
) {
    // 获取启用的药品列表（Flow）
    fun getAllActiveMedications(): Flow<List<Medication>>

    // 获取停用的药品列表（Flow）
    fun getAllInactiveMedications(): Flow<List<Medication>>

    // 获取所有药品（Flow）
    fun getAllMedications(): Flow<List<Medication>>

    // 根据ID获取药品（同步）
    suspend fun getMedicationById(id: Long): Medication?

    // 添加药品，返回新ID
    suspend fun addMedication(medication: Medication): Long

    // 更新药品信息
    suspend fun updateMedication(medication: Medication)

    // 删除药品（连带删除日志）
    suspend fun deleteMedication(medication: Medication)

    // 增加库存（自动记录PURCHASE日志）
    suspend fun addStock(medicationId: Long, quantity: Double, reason: String? = null)

    // 减少库存（自动记录LOST日志）
    suspend fun reduceStock(medicationId: Long, quantity: Double, reason: String? = null)

    // 设置药品启用/停用状态
    suspend fun setMedicationActive(medicationId: Long, active: Boolean)
}
```

---

## 4. ViewModel API

### 4.1 MedicationViewModel

```kotlin
class MedicationViewModel(application: Application) : AndroidViewModel(application) {
    // 状态流
    val activeMedications: StateFlow<List<Medication>>
    val inactiveMedications: StateFlow<List<Medication>>

    // 添加药品（异步）
    fun addMedication(
        genericName: String,
        brandName: String?,
        specification: String?,
        packageUnit: String,
        dosageForm: String,
        packageSize: Int,
        currentStock: Double,
        frequencyType: FrequencyType,
        frequencyValue: Int,
        dailyDosage: Double,
        startDate: LocalDate?
    )

    // 更新药品信息
    fun updateMedication(medication: Medication)

    // 删除药品
    fun deleteMedication(medicationId: Long)

    // 根据ID获取药品（注意：同步调用，可能返回null）
    fun getMedicationById(id: Long): Medication?

    // 增加库存（自动重新计算提醒）
    fun addStock(medicationId: Long, quantity: Double, reason: String?)

    // 减少库存（自动重新计算提醒）
    fun reduceStock(medicationId: Long, quantity: Double, reason: String?)

    // 停用药品（取消提醒）
    fun deactivateMedication(medicationId: Long)

    // 启用药品（重新注册提醒）
    fun activateMedication(medicationId: Long)

    // 重新注册所有提醒（切换日历时调用）
    fun refreshAllReminders(calendarId: Long)

    // 导出CSV，返回文件路径或null
    fun exportToCsv(context: Context): String?
}
```

---

## 5. 闹钟管理 API

### 5.1 AlarmScheduler

```kotlin
class AlarmScheduler(private val context: Context) {
    // 为药品注册提前1天的闹钟提醒
    fun scheduleAlarm(medication: Medication)

    // 取消某药品的闹钟
    fun cancelAlarm(medicationId: Long)

    // 批量重新注册闹钟（开机时调用）
    fun rescheduleAllAlarms(medications: List<Medication>)
}
```

### 5.2 AlarmReceiver

```kotlin
class AlarmReceiver : BroadcastReceiver() {
    // 接收闹钟广播，显示通知
    override fun onReceive(context: Context, intent: Intent)
}
```

**Intent Extra 参数：**
| 参数名 | 类型 | 说明 |
|--------|------|------|
| `medication_id` | Long | 药品ID |
| `medication_name` | String | 药品名称 |
| `alert_type` | String | 提醒类型（"1day"） |

### 5.3 BootReceiver

```kotlin
class BootReceiver : BroadcastReceiver() {
    // 开机时重新注册所有闹钟
    override fun onReceive(context: Context, intent: Intent)
}
```

---

## 6. 日历管理 API

### 6.1 CalendarManager

```kotlin
class CalendarManager(private val context: Context) {
    // 获取所有可写的日历账户
    fun getCalendars(): List<CalendarInfo>

    // 为药品添加日历提醒事件（提前4天）
    fun addMedicationAlert(calendarId: Long, medication: Medication): Long?

    // 删除日历事件
    fun deleteEvent(eventId: Long)

    data class CalendarInfo(
        val id: Long,
        val displayName: String,
        val accountName: String
    )
}
```

---

## 7. UI 组件 API

### 7.1 MedicationCard

```kotlin
@Composable
fun MedicationCard(
    medication: Medication,           // 药品数据
    onAddStock: () -> Unit,           // 补货回调
    onReduceStock: () -> Unit,        // 消耗回调
    onDeactivate: () -> Unit,         // 停用回调
    modifier: Modifier = Modifier
)
```

### 7.2 StockInputDialog

```kotlin
@Composable
fun StockInputDialog(
    title: String,                    // 对话框标题
    medication: Medication,           // 药品数据（用于显示当前库存）
    isAddition: Boolean,              // true=补货，false=消耗
    onConfirm: (Double, String?) -> Unit,  // 确认回调（数量，原因）
    onDismiss: () -> Unit             // 取消回调
)
```

### 7.3 StatusBadge

```kotlin
@Composable
fun StatusBadge(
    color: Color,    // 徽章颜色
    text: String     // 显示文本
)
```

### 7.4 EmptyState

```kotlin
@Composable
fun EmptyState(
    title: String,    // 主标题
    subtitle: String, // 副标题
    modifier: Modifier = Modifier
)
```

---

## 8. 页面导航 API

### 8.1 导航路由

| 路由 | 页面 | 参数 |
|------|------|------|
| `"list"` | 药品列表（主页） | 无 |
| `"add"` | 添加药品 | 无 |
| `"edit/{medicationId}"` | 编辑药品 | medicationId: Long |
| `"inactive"` | 已停用药品 | 无 |
| `"merged_alerts"` | 合并提醒 | 无 |
| `"settings"` | 设置 | 无 |

### 8.2 MainActivity 导航配置

```kotlin
NavHost(navController = navController, startDestination = "list") {
    composable("list") { MedicationListScreen(...) }
    composable("add") { AddMedicationScreen(...) }
    composable("edit/{medicationId}") { backStackEntry ->
        val medicationId = backStackEntry.arguments?.getString("medicationId")?.toLongOrNull()
        medicationId?.let { EditMedicationScreen(medicationId = it, ...) }
    }
    composable("inactive") { InactiveMedicationsScreen(...) }
    composable("settings") { SettingsScreen(...) }
    composable("merged_alerts") { MergedAlertScreen(...) }
}
```

---

## 9. 工具类 API

### 9.1 Converters（Room类型转换器）

```kotlin
class Converters {
    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String?

    @TypeConverter
    fun toLocalDate(dateString: String?): LocalDate?
}
```

---

## 10. 常量与配置

### 10.1 颜色常量

```kotlin
// 状态颜色
val UrgentRed = Color(0xFFE53935)      // ≤1天
val WarningYellow = Color(0xFFFFB300)  // ≤4天
val NormalGreen = Color(0xFF43A047)    // >4天

// 主题颜色（Light/Dark）
val md_theme_light_primary = Color(0xFF006C4C)
val md_theme_dark_primary = Color(0xFF6CDBAC)
```

### 10.2 提醒时间配置

| 提醒阶段 | 提前天数 | 时间 | 方式 |
|----------|----------|------|------|
| 第一阶段 | 4天 | 22:00 | 日历事件 |
| 第二阶段 | 1天 | 14:00 | 闹钟通知 |

### 10.3 合并规则

- 合并窗口：3天
- 合并逻辑：将3天内将耗尽的药品合并到最早提醒日
- 显示标识：工作日 / 周末

---

## 11. 错误处理

### 11.1 常见错误码

| 场景 | 处理方式 |
|------|----------|
| 日历权限被拒绝 | 显示Toast提示，引导用户到设置开启 |
| 闹钟权限被拒绝 | 显示Toast提示，提醒用户手动设置 |
| 库存计算为负数 | 强制设为0，记录日志 |
| 数据库操作失败 | 抛出异常，上层捕获处理 |

### 11.2 日志记录

```kotlin
// 库存变更自动记录
StockLog(
    medicationId = id,
    type = StockLogType.PURCHASE / LOST / MISSED_DOSE / ADJUSTMENT,
    quantity = 变更数量,
    reason = "用户输入的原因"
)
```

---

## 12. 性能优化

### 12.1 数据库优化
- 使用 Flow 实现响应式数据更新
- 索引：medications.isActive, stock_logs.medicationId
- 懒加载：药品列表分页（未来版本）

### 12.2 UI优化
- 使用 `collectAsStateWithLifecycle()` 避免内存泄漏
- 列表使用 `key` 参数优化重组
- 图片/图标使用矢量图

### 12.3 提醒优化
- 日历事件使用一次性事件（无RRULE）
- 闹钟使用 `setExactAndAllowWhileIdle` 确保触发
- 开机广播重新注册所有闹钟

---

**文档版本**: v1.0  
**最后更新**: 2026-06-08  
**维护者**: MediAlert开发团队
