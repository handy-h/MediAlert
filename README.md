# 药箱库存管家 (MediAlert)

家庭药箱库存预警管理应用。

## 功能特性

- **药品管理**：记录药品通用名、商品名、规格、包装信息
- **库存计算**：支持"3盒+6片"混合输入，自动折算为总剂量
- **用药频率**：支持"每X天Y剂量"和"每隔X天Y剂量"两种模式
- **库存预警**：
  - 提前4天：系统日历事件提醒（每晚22:00）
  - 提前1天：闹钟提醒（下午14:00）
  - 合并提醒：3天内将耗尽的药品合并到最早提醒日
- **库存操作**：支持补货（采购/漏服）和提前消耗（丢失）
- **药品状态**：支持停用/重新启用
- **数据导出**：CSV格式导出到下载目录
- **深色模式**：自动跟随系统主题

## 技术栈

- Kotlin + Jetpack Compose
- Room 数据库
- AlarmManager + 系统日历
- MVVM 架构

## 权限说明

| 权限 | 用途 |
|-----|------|
| READ/WRITE_CALENDAR | 读写系统日历事件 |
| SCHEDULE_EXACT_ALARM | 设置精确闹钟 |
| POST_NOTIFICATIONS | 显示提醒通知 |
| RECEIVE_BOOT_COMPLETED | 开机重新注册闹钟 |

## 项目结构

```
app/src/main/java/com/handy/medialert/
├── data/
│   ├── entity/          # 数据实体
│   ├── dao/             # 数据库访问对象
│   └── database/        # Room数据库
├── repository/          # 数据仓库
├── alarm/               # 闹钟管理
├── calendar/            # 日历管理
├── ui/
│   ├── screens/         # 页面
│   ├── components/      # 组件
│   └── theme/           # 主题
├── viewmodel/           # 视图模型
└── MainActivity.kt      # 主入口
```

## 构建

```bash
./gradlew assembleDebug
```

## 安装

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```
