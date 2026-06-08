# 药箱库存管家 (MediAlert) - 产品需求文档 (PRD)

**版本**: v1.0  
**日期**: 2026-06-08  
**包名**: com.handy.medialert  
**应用名称**: 药箱库存管家

---

## 1. 产品概述

### 1.1 产品定位
家庭药箱库存预警管理应用，帮助用户追踪药品库存、计算耗尽时间、并在库存不足时通过系统日历和闹钟发出提醒。

### 1.2 目标用户
- 需要长期服用药物的患者及家属
- 管理家庭常备药的家庭成员
- 需要追踪多品种药品库存的护理人员

### 1.3 核心价值
- **精准库存计算**: 支持混合包装输入（如"3盒+6片"），自动折算为总剂量
- **智能预警**: 两阶段提醒（提前4天日历提醒 + 提前1天闹钟提醒）
- **合并购药**: 自动合并3天内将耗尽的药品，减少购药次数
- **离线可用**: 本地数据库，无需网络

---

## 2. 功能需求

### 2.1 药品管理

#### 2.1.1 药品录入
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| 通用名 | 文本 | 是 | 药品通用名称，如"厄贝沙坦" |
| 商品名 | 文本 | 否 | 商品名，如"安博维" |
| 规格 | 文本 | 否 | 如"80/12.5"，仅作显示 |
| 包装单位 | 枚举 | 是 | 盒/支/瓶 |
| 剂型 | 枚举 | 是 | 片/粒/ml/g |
| 每包装含量 | 整数 | 是 | 如每盒14片 |
| 当前库存 | 混合输入 | 是 | X包装 + Y零散剂型 |
| 用药频率 | 枚举 | 是 | 每X天 / 每隔X天 |
| 频率值 | 整数 | 是 | X的值 |
| 每次剂量 | 小数 | 是 | 如1.5片 |
| 开始日期 | 日期 | 否 | 不填则立即开始 |

#### 2.1.2 药品编辑
- 可修改：通用名、商品名、规格
- 不可修改：包装信息、库存、频率（需删除重建）

#### 2.1.3 药品删除
- 删除时清空所有关联库存日志
- 删除前需二次确认

#### 2.1.4 药品状态
- **启用**: 正常计算库存和提醒
- **停用**: 暂停计算，保留数据，可重新启用

### 2.2 库存管理

#### 2.2.1 库存显示
- 主界面显示当前库存（如"2盒6片"）
- 按耗尽日期升序排列（倒计时排行）

#### 2.2.2 补货操作
- 输入：X包装 + Y零散剂型
- 原因（可选）：采购、漏服补回
- 自动更新库存并重新计算提醒

#### 2.2.3 消耗操作
- 输入：X包装 + Y零散剂型
- 原因（可选）：丢失、提前服用
- 自动更新库存并重新计算提醒

#### 2.2.4 库存日志
- 记录所有库存变更操作
- 类型：PURCHASE（采购）、MISSED_DOSE（漏服）、LOST（丢失）、ADJUSTMENT（调整）

### 2.3 预警系统

#### 2.3.1 两阶段提醒
| 阶段 | 触发时间 | 方式 | 内容 |
|------|----------|------|------|
| 第一阶段 | 耗尽前4天 22:00 | 系统日历事件 | "🛒 购药提醒：{药品名}" |
| 第二阶段 | 耗尽前1天 14:00 | 闹钟通知 | "⏰ 药品明天耗尽 - {药品名}" |

#### 2.3.2 合并提醒
- 自动识别3天内将耗尽的药品
- 合并到最早提醒日
- 显示工作日/周末标识

#### 2.3.3 提醒状态颜色
| 状态 | 天数 | 颜色 |
|------|------|------|
| 紧急 | ≤1天 | 红色 (#E53935) |
| 警告 | ≤4天 | 黄色 (#FFB300) |
| 正常 | >4天 | 绿色 (#43A047) |

### 2.4 数据导出
- 格式：CSV
- 路径：/Download/药箱库存_{时间戳}.csv
- 字段：通用名、商品名、规格、包装、剂型、每包装数量、当前库存、用药频率、耗尽日期、状态

### 2.5 设置
- 日历账户选择：绑定系统日历账户
- 权限管理：日历、闹钟、通知

---

## 3. 非功能需求

### 3.1 性能
- 启动时间 < 2秒
- 药品列表滑动帧率 ≥ 55fps
- 数据库查询 < 100ms

### 3.2 可靠性
- 开机自动重新注册闹钟
- 数据库迁移支持
- 后台被杀后提醒不丢失（依赖系统日历）

### 3.3 兼容性
- 最低支持 Android 8.0 (API 26)
- 目标 Android 14 (API 34)
- 支持深色模式

### 3.4 权限需求
| 权限 | 用途 | 是否必须 |
|------|------|----------|
| READ_CALENDAR | 读取日历账户 | 是 |
| WRITE_CALENDAR | 写入提醒事件 | 是 |
| SCHEDULE_EXACT_ALARM | 设置精确闹钟 | 是 |
| USE_EXACT_ALARM | 使用精确闹钟 | 是 |
| POST_NOTIFICATIONS | 显示通知 | 是 |
| RECEIVE_BOOT_COMPLETED | 开机重注册 | 是 |
| WRITE_EXTERNAL_STORAGE | 导出CSV（API≤28） | 否 |

---

## 4. 技术架构

### 4.1 技术栈
- **语言**: Kotlin
- **UI框架**: Jetpack Compose + Material Design 3
- **数据库**: Room (SQLite)
- **架构**: MVVM
- **导航**: Navigation Compose
- **异步**: Kotlin Coroutines + Flow

### 4.2 项目结构
```
app/src/main/java/com/handy/medialert/
├── data/
│   ├── entity/          # 数据实体（Medication, StockLog）
│   ├── dao/             # 数据库访问对象
│   └── database/        # Room数据库 + TypeConverter
├── repository/          # 数据仓库层
├── alarm/               # 闹钟管理（AlarmManager）
├── calendar/            # 日历管理（CalendarProvider）
├── ui/
│   ├── screens/         # 页面组件
│   ├── components/      # 可复用组件
│   └── theme/           # 主题配置
├── viewmodel/           # 视图模型
├── MainActivity.kt      # 主入口
└── MediAlertApplication.kt # Application类
```

### 4.3 数据模型

#### Medication（药品）
```kotlin
@Entity(tableName = "medications")
data class Medication(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val genericName: String,           // 通用名
    val brandName: String?,            // 商品名
    val specification: String?,        // 规格（仅显示）
    val packageUnit: String,           // 包装单位（盒/支/瓶）
    val dosageForm: String,            // 剂型（片/粒/ml/g）
    val packageSize: Int,              // 每包装含量
    val currentStock: Double,          // 当前库存（总剂型数）
    val frequencyType: FrequencyType,  // 频率类型
    val frequencyValue: Int,           // 频率值
    val dailyDosage: Double,           // 每次剂量
    val startDate: LocalDate?,         // 开始日期
    val isActive: Boolean = true,      // 是否启用
    val createdAt: Long = System.currentTimeMillis()
)
```

#### StockLog（库存日志）
```kotlin
@Entity(tableName = "stock_logs")
data class StockLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicationId: Long,            // 关联药品ID
    val type: StockLogType,            // 操作类型
    val quantity: Double,              // 变更数量（正/负）
    val reason: String?,               // 原因
    val timestamp: Long                // 时间戳
)
```

### 4.4 核心算法

#### 日消耗量计算
```kotlin
fun dailyConsumption(): Double = when (frequencyType) {
    EVERY_X_DAYS -> dailyDosage / frequencyValue      // 每X天 → 日消耗 = 剂量/X
    EVERY_XTH_DAY -> dailyDosage / (frequencyValue + 1) // 每隔X天 → 日消耗 = 剂量/(X+1)
}
```

#### 耗尽日期计算
```kotlin
fun depletionDate(): LocalDate {
    val effectiveStart = startDate ?: LocalDate.now()
    val daysLeft = (currentStock / dailyConsumption()).toInt()
    return effectiveStart.plusDays(daysLeft.toLong())
}
```

#### 库存显示格式化
```kotlin
fun getStockDisplay(): String {
    val packages = (currentStock / packageSize).toInt()
    val remainder = (currentStock % packageSize).toInt()
    return when {
        packages > 0 && remainder > 0 -> "${packages}${packageUnit}${remainder}${dosageForm}"
        packages > 0 -> "${packages}${packageUnit}"
        else -> "${remainder}${dosageForm}"
    }
}
```

---

## 5. UI/UX 设计

### 5.1 页面导航
```
主界面 (MedicationListScreen)
├── 添加药品 (AddMedicationScreen)
├── 编辑药品 (EditMedicationScreen)
├── 已停用药品 (InactiveMedicationsScreen)
├── 合并提醒 (MergedAlertScreen)
└── 设置 (SettingsScreen)
```

### 5.2 主界面布局
- **TopAppBar**: 标题"药箱库存管家" + 设置/已停用/合并提醒图标
- **药品列表**: 按耗尽日期升序排列
- **FloatingActionButton**: 添加药品
- **空状态**: "暂无药品"提示

### 5.3 药品卡片设计
- 标题行：通用名 + 商品名 + 状态徽章
- 信息行：规格 | 包装信息
- 库存行：当前库存 + 耗尽日期（含星期）
- 频率行：用药频率
- 操作行：补货 / 消耗 / 停用

### 5.4 主题配色
- **主色**: #006C4C（深绿色，健康主题）
- **紧急**: #E53935（红色）
- **警告**: #FFB300（黄色）
- **正常**: #43A047（绿色）
- **支持深色模式**: 自动跟随系统

---

## 6. 测试策略

### 6.1 单元测试
- 库存计算逻辑
- 日期计算逻辑
- 频率类型转换

### 6.2 集成测试
- 数据库CRUD操作
- 日历事件创建/删除
- 闹钟注册/取消

### 6.3 手动测试场景
1. 添加药品 → 验证库存显示
2. 修改库存 → 验证提醒重新计算
3. 停用药品 → 验证提醒取消
4. 重启设备 → 验证闹钟恢复
5. 导出CSV → 验证文件内容

---

## 7. 发布清单

### 7.1 构建配置
```kotlin
android {
    compileSdk = 34
    defaultConfig {
        applicationId = "com.handy.medialert"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
}
```

### 7.2 构建命令
```bash
./gradlew assembleDebug    # 调试版
./gradlew assembleRelease  # 发布版
```

### 7.3 安装命令
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## 8. 后续迭代计划

### v1.1 候选功能
- [ ] 药品搜索和筛选
- [ ] 库存历史图表
- [ ] 多用户支持
- [ ] 云同步备份
- [ ] 扫码录入药品
- [ ] 用药记录打卡

### v2.0 候选功能
- [ ] 家庭成员管理
- [ ] 医院/药店导航
- [ ] 药品相互作用检查
- [ ] 语音提醒
- [ ] 智能药盒联动

---

## 9. 附录

### 9.1 相关文档
- [README.md](../../README.md) - 项目简介
- [API文档](./API.md) - 接口文档（待补充）
- [测试报告](./TEST.md) - 测试报告（待补充）

### 9.2 参考资源
- [Android开发者文档](https://developer.android.com/)
- [Jetpack Compose指南](https://developer.android.com/jetpack/compose)
- [Room数据库指南](https://developer.android.com/training/data-storage/room)

---

**文档维护**: 如有更新，请同步修改本文件并记录变更日志。
