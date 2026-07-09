# GitHub Actions Secrets 配置清单

## 必需配置

### 签名密钥

#### Debug 签名
1. 生成 Debug 密钥 (如果还没有):
   ```bash
   keytool -genkey -v -keystore debug.keystore -alias debug -keyalg RSA -keysize 2048 -validity 10000 -storepass debug123 -keypass debug123
   ```

2. 转换为 Base64:
   ```bash
   # macOS
   base64 -i debug.keystore | pbcopy
   
   # Linux
   base64 -i debug.keystore -w 0 | xclip -selection clipboard
   
   # Windows PowerShell
   [Convert]::ToBase64String([IO.File]::ReadAllBytes("debug.keystore")) | Set-Clipboard
   ```

3. 在 GitHub 添加 Secrets:
   - `DEBUG_SIGNING_KEY`: Base64 编码的密钥内容
   - `DEBUG_KEY_ALIAS`: `debug`
   - `DEBUG_KEY_PASSWORD`: `debug123`

#### Release 签名
1. 使用已有的 `release.jks` (项目根目录已有)

2. 转换为 Base64:
   ```bash
   # Windows PowerShell
   [Convert]::ToBase64String([IO.File]::ReadAllBytes("release.jks")) | Set-Clipboard
   ```

3. 在 GitHub 添加 Secrets:
   - `RELEASE_SIGNING_KEY`: Base64 编码的 release.jks
   - `RELEASE_KEY_ALIAS`: 你的密钥别名
   - `RELEASE_KEY_PASSWORD`: 你的密钥密码
   - `RELEASE_STORE_PASSWORD`: 你的密钥库密码

## 可选配置

### 代码覆盖率
- `CODECOV_TOKEN`: 从 https://codecov.io 获取

### Slack 通知
- `SLACK_WEBHOOK_URL`: 从 Slack App 管理页面获取

### 邮件通知
- `EMAIL_USERNAME`: SMTP 邮箱地址
- `EMAIL_PASSWORD`: SMTP 密码或应用专用密码
- `NOTIFICATION_EMAIL`: 接收通知的邮箱地址

### Firebase 部署 (可选)
- `FIREBASE_APP_ID`: Firebase App ID
- `FIREBASE_SERVICE_ACCOUNT`: Firebase 服务账号 JSON 内容

## 配置步骤

1. 打开 GitHub 仓库页面
2. 点击 Settings → Secrets and variables → Actions
3. 点击 "New repository secret"
4. 输入 Secret 名称和值
5. 点击 "Add secret"

## 验证配置

推送代码后，在 Actions 标签页查看流水线运行状态。

## 安全提示

⚠️ **重要**: `release.jks` 文件已存在于仓库中，这存在安全风险。建议：

1. 从 Git 历史中移除 `release.jks`:
   ```bash
   git filter-branch --force --index-filter \
     'git rm --cached --ignore-unmatch release.jks' \
     --prune-empty --tag-name-filter cat -- --all
   ```

2. 或者使用 `git-filter-repo`:
   ```bash
   pip install git-filter-repo
   git filter-repo --path release.jks --invert-paths
   ```

3. 将 `release.jks` 添加到 `.gitignore`

4. 在 GitHub Secrets 中配置 `RELEASE_SIGNING_KEY`

5. 强制推送清理后的历史（注意：这会重写 Git 历史，团队需要重新克隆）
