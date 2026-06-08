## MediAlert 药箱库存管家 — 单元测试计划

**版本**: v1.0
**日期**: 2026-06-08
**范围**: PRD 6.1 节定义的所有单元测试 + 数据层集成测试

---

### 1. 测试策略概览

本项目采用分层测试策略，按测试金字塔从底到顶分为三层：纯逻辑单元测试（无需 Android 框架，运行在 JVM 上）、带 Mock 的组件单元测试（使用 mockk 隔离 Android 依赖）、Room 数据库集成测试（运行在 Android Instrumented 环境）。

**测试框架与依赖**：

| 工具 | 用途 |
|------|------|
| JUnit 4 | 测试运行器与断言基础 |
| mockk | Kotlin 友好的 Mock 框架，用于隔离 Android 依赖 |
| kotlinx-coroutines-test | 协程与 Flow 的测试支持 |
| Room In-Memory DB | 数据库集成测试 |
| AndroidX Test | Instrumented 测试基础 |

---

### 2. 测试模块划分

#### 2.1 Medication 实体测试 (MedicationTest.kt)

**优先级**: P0 — 核心业务逻辑，必须全覆盖

| 测试用例 | 描述 | 输入 | 预期输出 |
|---------|------|------|---------|
| `dailyConsumption_everyXDays` | 每X天频率的日消耗量 | frequencyType=EVERY_X_DAYS, frequencyValue=2, dailyDosage=1.0 | 0.5 |
| `dailyConsumption_everyXthDay` | 每隔X天频率的日消耗量 | frequencyType=EVERY_XTH_DAY, frequencyValue=2, dailyDosage=3.0 | 1.0 |
| `dailyConsumption_zeroDivisor` | 频率值为0时的安全降级 | frequencyValue=0, dailyDosage=2.0 | 2.0 |
| `depletionDate_withStartDate` | 有开始日期时的耗尽日期 | startDate=2026-06-01, stock=28, dailyConsumption=1.0 | 2026-06-29 |
| `depletionDate_withoutStartDate` | 无开始日期时使用今天 | startDate=null | today + daysLeft |
| `daysUntilDepletion` | 距耗尽天数 | 构造已知耗尽日期 | 正确的天数差 |
| `getStockDisplay_packagesAndRemainder` | 混合包装+零散显示 | stock=34, packageSize=14 | "2盒6片" |
| `getStockDisplay_packagesOnly` | 仅整包装显示 | stock=28, packageSize=14 | "2盒" |
| `getStockDisplay_remainderOnly` | 仅零散显示 | stock=6, packageSize=14 | "6片" |
| `getStockDisplay_zeroStock` | 零库存显示 | stock=0 | "0片" |
| `alert4DayDateTime` | 第一阶段提醒时间 | depletionDate=2026-06-29 | 2026-06-25 22:00 |
| `alert1DayDateTime` | 第二阶段提醒时间 | depletionDate=2026-06-29 | 2026-06-28 14:00 |
| `alertColorStatus_urgent` | 紧急状态颜色判定 | daysUntilDepletion=1 | 红色 |
| `alertColorStatus_warning` | 警告状态颜色判定 | daysUntilDepletion=3 | 黄色 |
| `alertColorStatus_normal` | 正常状态颜色判定 | daysUntilDepletion=10 | 绿色 |

#### 2.2 MedicationRepository 测试 (MedicationRepositoryTest.kt)

**优先级**: P0 — 数据操作层，使用 mockk 模拟 DAO

| 测试用例 | 描述 | 验证点 |
|---------|------|--------|
| `addMedication_insertsAndReturnsId` | 添加药品 | DAO.insert 被正确调用 |
| `addStock_updatesStockAndLogs` | 补货操作 | 库存正确增加，StockLog 被记录 |
| `reduceStock_updatesStockAndLogs` | 消耗操作 | 库存正确减少，StockLog 被记录 |
| `reduceStock_doesNotGoBelowZero` | 库存不低于零 | coerceAtLeast(0.0) 生效 |
| `addStock_medicatioNotFound` | 药品不存在时 | 不执行更新，不崩溃 |
| `deleteMedication` | 删除药品 | DAO.delete 被调用 |
| `setMedicationActive_toggle` | 切换启用/停用 | DAO.setActive 被正确调用 |

#### 2.3 TypeConverter 测试 (ConvertersTest.kt)

**优先级**: P1 — 数据持久化的序列化/反序列化

| 测试用例 | 描述 | 输入 | 预期输出 |
|---------|------|------|---------|
| `fromLocalDate_validDate` | 日期转字符串 | 2026-06-08 | "2026-06-08" |
| `fromLocalDate_null` | 空日期处理 | null | null |
| `toLocalDate_validString` | 字符串转日期 | "2026-06-08" | LocalDate(2026,6,8) |
| `toLocalDate_null` | 空字符串处理 | null | null |
| `roundTrip` | 双向转换一致性 | LocalDate → String → LocalDate | 值不变 |

#### 2.4 AlarmScheduler 测试 (AlarmSchedulerTest.kt)

**优先级**: P1 — 闹钟调度逻辑

| 测试用例 | 描述 | 验证点 |
|---------|------|--------|
| `scheduleAlarm_futureTime_schedulesExactAlarm` | 未来时间正常调度 | setExactAndAllowWhileIdle 被调用 |
| `scheduleAlarm_pastTime_skips` | 过去时间跳过 | 不调用任何 alarm 方法 |
| `scheduleAlarm_inactiveMedication_skips` | 已耗尽药品跳过 | daysUntilDepletion ≤ 0 时不注册 |
| `cancelAlarm_cancelsCorrectPendingIntent` | 取消指定药品闹钟 | PendingIntent 用正确 requestCode 取消 |
| `rescheduleAllAlarms_onlyActive` | 批量重注册只处理启用的 | 停用药品被 filter 过滤 |

#### 2.5 DAO 集成测试 (MedicationDaoTest.kt, StockLogDaoTest.kt)

**优先级**: P0 — 数据库操作正确性

| 测试用例 | 描述 | 验证点 |
|---------|------|--------|
| `insert_and_getById` | 插入后按 ID 查询 | 数据完整一致 |
| `insert_multiple_filterActive` | 多条插入，仅查询启用 | isActive 过滤正确 |
| `update_modifiesData` | 更新药品 | 字段变更生效 |
| `delete_removesData` | 删除药品 | 查询返回 null |
| `updateStock_changesStockValue` | 更新库存 | currentStock 字段正确 |
| `setActive_togglesStatus` | 切换状态 | isActive 字段正确 |
| `stockLog_insertAndQuery` | 库存日志写入与查询 | 按时间倒序返回 |
| `stockLog_cascadeDelete` | 药品删除后日志级联清除 | 关联日志被自动删除 |
| `converters_localDate` | TypeConverter 数据库层面 | LocalDate 存取一致 |

---

### 3. 测试覆盖矩阵

| 模块 | 文件 | P0 用例 | P1 用例 | 预计行数 |
|------|------|---------|---------|---------|
| Medication 实体 | MedicationTest.kt | 10 | 5 | ~200 |
| Repository | MedicationRepositoryTest.kt | 7 | 0 | ~150 |
| TypeConverter | ConvertersTest.kt | 3 | 2 | ~60 |
| AlarmScheduler | AlarmSchedulerTest.kt | 3 | 2 | ~120 |
| DAO 集成 | MedicationDaoTest.kt | 6 | 0 | ~150 |
| DAO 集成 | StockLogDaoTest.kt | 3 | 0 | ~80 |
| **合计** | **6 个测试文件** | **32** | **9** | **~760** |

---

### 4. 测试命名规范

所有测试用例遵循 `方法名_场景描述` 的命名格式，例如：
`dailyConsumption_everyXDays_returnsCorrectRate`
`addStock_medicatioNotFound_doesNothing`

---

### 5. CI 集成建议

单元测试（JVM 测试）应在每次 push 时自动执行：

```bash
# 运行所有 JVM 单元测试
./gradlew testDebugUnitTest

# 运行 Instrumented 测试（需要设备/模拟器）
./gradlew connectedDebugAndroidTest

# 生成覆盖率报告
./gradlew jacocoTestReport
```

---

### 6. 后续扩展

v1.1 版本应补充以下测试：ViewModel 层测试（需 mockk Application）、CSV 导出功能测试、UI Compose 组件测试（使用 ui-test-junit4）、以及合并提醒逻辑测试。
