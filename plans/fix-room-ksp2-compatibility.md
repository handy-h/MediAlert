# 修复 Room + KSP2 兼容性问题

## 问题
构建任务 `:app:kspDebugKotlin` 失败，错误信息：`unexpected jvm signature V`

## 根因
Room `2.6.1` 的 KSP 编译器不兼容 KSP `2.3.2`（KSP2）。KSP2 对 `Unit`/`void` 类型的 JVM 签名表示方式发生了变化，导致 Room 处理器在解析 `@Query` 方法的返回类型时崩溃。

## 修复方案

### 修改文件：`app/build.gradle.kts`

将 Room 依赖从 `2.6.1` 升级到 `2.7.1`：

```diff
- implementation("androidx.room:room-runtime:2.6.1")
- implementation("androidx.room:room-ktx:2.6.1")
- ksp("androidx.room:room-compiler:2.6.1")
+ implementation("androidx.room:room-runtime:2.7.1")
+ implementation("androidx.room:room-ktx:2.7.1")
+ ksp("androidx.room:room-compiler:2.7.1")

- androidTestImplementation("androidx.room:room-testing:2.6.1")
+ androidTestImplementation("androidx.room:room-testing:2.7.1")
```

### 影响范围
- Room 2.7.x 向后兼容 2.6.x 的 API，现有的 Entity、DAO、Database 代码无需修改
- 仅修改版本号，不涉及代码改动

### 验证步骤
1. 执行 `gradlew :app:assembleDebug` 确认构建成功
2. 确认无新增编译警告或错误
