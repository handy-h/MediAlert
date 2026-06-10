# UI 优化：大字体适配 MedicationCard

**问题**: 用户手机设置了大字体，MedicationCard 中：
- 三个操作按钮（补货/消耗/停用）文字被截断为 `+ ...` / `- ...` / `|| ...`
- 标题行中 StatusBadge 和药名互相挤压
- 库存+耗尽日期横排时文本溢出

**修改文件**: `ui/components/MedicationCard.kt`

**具体改动**:

1. **操作按钮 OutlinedButton → OutlinedIconButton**
   - 去掉按钮文字，改为纯图标（Add/Remove/Pause）
   - 语义保留在 contentDescription（无障碍）
   - 按钮居中均匀分布（spacedBy + CenterHorizontally）

2. **标题行布局优化**
   - 编辑图标缩小至 40×40dp（内图标 20dp）
   - 编辑图标 + StatusBadge 放入右侧独立 Row
   - 药名 Column 使用 weight(1f, fill=false) 避免过度伸展
   - 药名允许 maxLines=2 + Ellipsis

3. **库存/耗尽日期 Row → Column**
   - 从横向 SpaceBetween 改为纵向排列
   - 各自独占整行宽度，不再互相挤压

**构建状态**: ✅ BUILD SUCCESSFUL
**APK**: `app/build/outputs/apk/debug/app-debug.apk`
