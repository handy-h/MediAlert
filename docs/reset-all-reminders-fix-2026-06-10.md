# 修复：resetAllReminders 始终提示"无需要提醒的药品"

**问题**: 每次重设提醒都显示 toast "当前没有需要提醒的药品"，但日历事件实际已创建成功。

**根因**: `resetAllReminders()` 和 `refreshAllReminders(calendarId)` 使用 `activeMedications.value`（StateFlow 快照）获取当前活跃药品列表。

`activeMedications` 的初始化：
```kotlin
val activeMedications = repository.getAllActiveMedications()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
```

- 初始值为 `emptyList()`
- `WhileSubscribed(5000)` 在最后一个订阅者离开 5 秒后取消上游订阅

当用户进入 Settings 调用 `resetAllReminders` 时：
- 如果 Room Flow 尚未发出第一帧数据 → snapshot = `emptyList()`
- 如果上游订阅已被取消（5秒窗口过期）→ snapshot = `emptyList()`
- 两种情况都导致 `activeMeds.isEmpty() == true` → 立即 `onResult(0, null)` → toast 显示"0 个药品"

但之前通过日历选择弹窗调用 `refreshAllReminders(calendarId)` 时，可能数据已加载 → 日历事件成功创建 → 用户看到矛盾的现象。

**修复方案**: 直接用 `repository.getAllActiveMedications().first()` 替代 `activeMedications.value`，`first()` 会挂起直到 Room 发出第一帧数据（保证获得当前数据库真实值）。

**修改文件**:
- `viewmodel/MedicationViewModel.kt` — `resetAllReminders()` 和 `refreshAllReminders(calendarId: Long)` 两处

**构建状态**: ✅ BUILD SUCCESSFUL
