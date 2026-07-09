# 快速开始

## 首次配置

1. **配置 GitHub Secrets** (详见 SECRETS.md)
   - 至少配置 Debug 签名密钥
   - 配置 Release 签名密钥 (用于发布)

2. **推送代码触发流水线**
   ```bash
   git add .github/workflows/
   git commit -m "ci: add GitHub Actions CI/CD pipeline"
   git push origin main
   ```

3. **查看运行状态**
   - 打开 GitHub 仓库 → Actions 标签页
   - 查看流水线执行结果

## 日常开发流程

### 功能开发
```bash
# 创建功能分支
git checkout -b feature/new-feature

# 开发代码...

# 推送触发 CI
git push origin feature/new-feature
```

### 查看结果
- PR 页面会显示 CI 检查状态
- 点击详情查看测试报告和构建产物

### 发布版本
1. 在 GitHub 创建 Release
2. 流水线自动构建并上传 Release APK/AAB
3. 下载产物进行分发

## 本地调试

```bash
# 运行所有检查
./gradlew check

# 运行单元测试
./gradlew testDebugUnitTest

# 构建 Debug APK
./gradlew assembleDebug

# 构建 Release APK
./gradlew assembleRelease
```
