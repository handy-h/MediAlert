# GitHub Actions CI/CD 配置指南

## 项目: MediAlert (Android)

## 流水线概述

本 CI/CD 流水线包含以下阶段:

```
代码提交
    ↓
代码质量检查 (Detekt + Android Lint + 安全扫描 + ktlint)
    ↓
单元测试 (JVM 测试 + 覆盖率)
    ↓
集成测试 (Android 模拟器测试 - API 29/33/34)
    ↓
构建 Debug APK
    ↓
构建 Release APK/AAB (仅 main 分支和 release 事件)
    ↓
发布到 GitHub Releases (仅 release 事件)
    ↓
通知 (Slack/Email)
```

## 触发条件

| 事件 | 触发分支 | 执行阶段 |
|------|---------|---------|
| Push | main, develop, feature/** | 全部 (Release 构建仅限 main) |
| Pull Request | main, develop | 代码质量 + 单元测试 + 集成测试 + Debug 构建 |
| Release Published | - | 完整流水线 + GitHub 发布 |

## 必需的环境变量

在 GitHub Repository Settings → Secrets and variables → Actions 中配置:

### 签名密钥 (必需)

```bash
# 生成 Debug 签名密钥 (如没有)
keytool -genkey -v -keystore debug.keystore -alias debug -keyalg RSA -keysize 2048 -validity 10000

# 编码为 Base64
base64 -i debug.keystore | pbcopy  # macOS
base64 -i debug.keystore -w 0 | xclip -selection clipboard  # Linux
```

| Secret 名称 | 说明 | 获取方式 |
|------------|------|---------|
| `DEBUG_SIGNING_KEY` | Debug 签名密钥 Base64 | `base64 -i debug.keystore` |
| `DEBUG_KEY_ALIAS` | Debug 密钥别名 | 创建密钥时指定 |
| `DEBUG_KEY_PASSWORD` | Debug 密钥密码 | 创建密钥时设置 |
| `RELEASE_SIGNING_KEY` | Release 签名密钥 Base64 | `base64 -i release.jks` |
| `RELEASE_KEY_ALIAS` | Release 密钥别名 | 创建密钥时指定 |
| `RELEASE_KEY_PASSWORD` | Release 密钥密码 | 创建密钥时设置 |
| `RELEASE_STORE_PASSWORD` | Release 密钥库密码 | 创建密钥时设置 |

### 可选配置

| Secret 名称 | 说明 | 用途 |
|------------|------|------|
| `CODECOV_TOKEN` | Codecov 令牌 | 代码覆盖率上传 |
| `SLACK_WEBHOOK_URL` | Slack Webhook URL | 构建通知 |
| `EMAIL_USERNAME` | SMTP 邮箱用户名 | 邮件通知 |
| `EMAIL_PASSWORD` | SMTP 邮箱密码 | 邮件通知 |
| `NOTIFICATION_EMAIL` | 通知接收邮箱 | 邮件通知 |
| `FIREBASE_APP_ID` | Firebase App ID | Firebase 分发 |
| `FIREBASE_SERVICE_ACCOUNT` | Firebase 服务账号 | Firebase 分发 |

## 流水线特性

### 1. 代码质量检查
- **Detekt**: Kotlin 静态代码分析，SARIF 报告上传到 GitHub Security
- **Android Lint**: Android 特定问题检查
- **ktlint**: Kotlin 代码格式检查
- **Dependency Check**: 依赖漏洞扫描 (OWASP)

### 2. 测试策略
- **单元测试**: JVM 环境运行，快速反馈
- **集成测试**: 多 API 级别模拟器测试 (API 29, 33, 34)
- **代码覆盖率**: JaCoCo 生成报告，上传 Codecov

### 3. 构建产物
- **Debug APK**: 每个 PR/Push 都构建，用于快速测试
- **Release APK**: 仅 main 分支和 release 事件，带签名
- **Release AAB**: Android App Bundle，用于 Google Play 发布

### 4. 安全特性
- 签名密钥仅存在于构建阶段，构建后自动清理
- Gradle Wrapper 验证
- 依赖漏洞扫描
- APK/AAB 签名验证

### 5. 通知机制
- **Slack**: 构建成功/失败通知，带查看详情按钮
- **Email**: 构建失败时发送邮件

### 6. 并发控制
- 同一分支/PR 只保留最新运行，自动取消旧的
- 生产环境部署串行执行

### 7. 超时控制
- 每个 job 都有合理的超时时间
- 防止长时间挂起消耗资源

## 本地测试

在推送前本地验证:

```bash
# 代码质量检查
./gradlew lintDebug
./gradlew detekt
./gradlew ktlintCheck

# 单元测试
./gradlew testDebugUnitTest

# 构建 Debug APK
./gradlew assembleDebug

# 构建 Release APK (需要配置签名)
./gradlew assembleRelease
```

## 故障排查

### 常见问题

1. **Gradle 缓存问题**
   ```bash
   ./gradlew clean
   rm -rf ~/.gradle/caches
   ```

2. **模拟器测试超时**
   - 检查 API 级别是否支持
   - 增加 `emulator-options` 中的超时时间

3. **签名失败**
   - 确认 Secrets 已正确配置
   - 验证 Base64 编码是否正确

4. **内存不足**
   - 调整 `GRADLE_OPTS` 中的 JVM 参数
   - 增加 GitHub Actions runner 内存限制

## 优化建议

1. **Gradle 缓存**: 已启用 `actions/setup-java` 和 `gradle/actions/setup-gradle` 的缓存功能
2. **并行执行**: 代码质量检查和单元测试可以并行
3. **条件执行**: Release 构建仅在 main 分支执行
4. **产物保留**: Debug 产物保留 7 天，Release 保留 14 天
5. **并发控制**: 自动取消旧的运行，节省资源

## 扩展功能

### 添加 Google Play 发布

在 `deploy-google-play` job 中添加:

```yaml
  deploy-google-play:
    name: Deploy to Google Play
    runs-on: ubuntu-latest
    needs: build-release
    if: github.ref == 'refs/heads/main'
    steps:
      - name: Download AAB
        uses: actions/download-artifact@v4
        with:
          name: release-aab
          path: ./artifacts

      - name: Publish to Google Play
        uses: r0adkll/upload-google-play@v1
        with:
          serviceAccountJsonPlainText: ${{ secrets.PLAY_STORE_SERVICE_ACCOUNT }}
          packageName: com.handy.medialert
          releaseFiles: ./artifacts/*.aab
          track: internal
          status: completed
```

### 添加 Firebase 分发

```yaml
  deploy-firebase:
    name: Deploy to Firebase App Distribution
    runs-on: ubuntu-latest
    needs: build-debug
    steps:
      - name: Download Debug APK
        uses: actions/download-artifact@v4
        with:
          name: debug-apk
          path: ./artifacts

      - name: Upload to Firebase
        uses: wzieba/Firebase-Distribution-Github-Action@v1
        with:
          appId: ${{ secrets.FIREBASE_APP_ID }}
          serviceCredentialsFileContent: ${{ secrets.FIREBASE_SERVICE_ACCOUNT }}
          groups: testers
          file: ./artifacts/app-debug.apk
```
