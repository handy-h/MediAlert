# 药箱库存管家 (MediAlert)

家庭药箱库存预警管理应用。通过记录药品库存和用药频率，自动计算耗尽日期，提前提醒补购。

## 功能特性

### 药品管理
- 记录药品通用名、商品名、规格、包装信息
- 支持多种包装单位（盒/支/瓶/板）和剂型（片/粒/支/ml/g/mg）
- 支持停用/重新启用药品

### 库存管理
- 支持"3盒+6片"混合输入，自动折算为总剂量
- 补货（采购）、提前消耗（丢失）操作，支持备注原因
- 库存日志记录每次变动

### 用药频率
- 支持"每X天Y剂量"和"每隔X天Y剂量"两种模式
- 自动计算日均消耗量和预计耗尽日期

### 预警提醒（双通道）
- **日历提醒**：耗尽前4天，在系统日历创建购药提醒事件（22:00）
- **闹钟提醒**：耗尽前1天，精确闹钟通知（14:00）
- **合并提醒**：3天内将耗尽的药品合并到同一提醒日
- 支持重设所有提醒（删除旧事件后按当前库存重建）
- 开机自动恢复闹钟

### 数据导入导出
- CSV 格式导出到设备文档目录（UTF-8 BOM，兼容 Excel）
- CSV 格式导入，支持中英文频率文本解析

### 其他
- 深色模式：自动跟随系统主题
- 电池优化引导：提示用户关闭电池优化以确保后台提醒正常

## 权限说明

| 权限 | 用途 |
|-----|------|
| READ/WRITE_CALENDAR | 读写系统日历事件 |
| SCHEDULE_EXACT_ALARM | 设置精确闹钟（Android 12+） |
| POST_NOTIFICATIONS | 显示提醒通知（Android 13+） |
| RECEIVE_BOOT_COMPLETED | 开机重新注册闹钟 |

## 技术栈

- **语言**：Kotlin
- **UI**：Jetpack Compose + Material 3
- **数据库**：Room（含 Migration）
- **架构**：MVVM（ViewModel + StateFlow + Repository）
- **提醒**：AlarmManager + 系统日历 ContentProvider
- **导航**：Navigation Compose
- **CSV**：OpenCSV

## 项目结构

```
app/src/main/java/com/handy/medialert/
├── data/
│   ├── entity/          # Medication、StockLog 数据实体
│   ├── dao/             # Room DAO（Flow + suspend）
│   └── database/        # AppDatabase、Migration、TypeConverter
├── repository/          # MedicationRepository（库存操作含日志）
├── alarm/               # AlarmScheduler、AlarmReceiver、BootReceiver
├── calendar/            # CalendarManager（系统日历 CRUD）
├── reminder/            # ReminderManager（协调闹钟+日历）
├── ui/
│   ├── screens/         # 页面（列表/添加/编辑/停用/合并提醒/设置）
│   ├── components/      # 复用组件（MedicationCard、StockInputDialog 等）
│   └── theme/           # Material3 主题配色
├── viewmodel/           # MedicationViewModel（CSV 导入导出）
├── MainActivity.kt      # 单 Activity + NavHost
└── MediAlertApplication.kt  # Application，DB/Repo 单例，通知渠道
```

## 开发环境

- **JDK 17**（JBR 21 与 Android SDK platform jar 不兼容）
- **Android SDK**：compileSdk 34，minSdk 26
- **Gradle**：9.5.0（wrapper）

首次构建前，确认 `JAVA_HOME` 指向 JDK 17。如遇 Room/KSP 编译时 tmpdir 权限错误，参考 `gradle.properties` 中的注释配置临时目录。

## 构建与运行

```bash
# 构建 debug APK
./gradlew assembleDebug

# 安装到设备
adb install app/build/outputs/apk/debug/app-debug.apk

# 运行 JVM 单元测试
./gradlew testDebugUnitTest

# 运行 Instrumented 测试（需设备/模拟器）
./gradlew connectedDebugAndroidTest
```

Windows 下使用 `gradlew.bat` 替代 `./gradlew`。

## 测试

| 类型 | 位置 | 覆盖内容 |
|------|------|----------|
| JVM 单元测试 | `app/src/test/` | Medication 实体逻辑、TypeConverter、Repository |
| Instrumented 测试 | `app/src/androidTest/` | DAO 操作、外键级联、闹钟调度 |

测试框架：JUnit 4 + MockK + kotlinx-coroutines-test
