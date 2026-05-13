# 应用更新发布说明

## 1. 更新 APK

每次发布新版本时，把新的 APK 上传到 GitHub Releases。

建议命名保持稳定，例如：

- `app-release.apk`

对应的下载地址示例：

- `https://github.com/kevySZ/MiniLedger/releases/download/v1.0.2/app-release.apk`

## 2. 更新 `update.json`

应用会优先从下面几个地址拉取版本清单：

- `https://mirror.ghproxy.com/https://raw.githubusercontent.com/kevySZ/MiniLedger/main/update.json`
- `https://raw.githubusercontent.com/kevySZ/MiniLedger/main/update.json`
- `https://cdn.jsdelivr.net/gh/kevySZ/MiniLedger@main/update.json`

所以每次发版后，需要同步更新仓库根目录的 `update.json`。

字段说明：

- `versionCode`: 新版本的内部版本号，必须大于当前安装包。
- `versionName`: 用户看到的版本号。
- `title`: 检测到更新时弹窗标题。
- `changelog`: 更新说明列表。
- `apkUrl`: GitHub Releases 原始下载地址。
- `apkMirrors`: 加速下载地址列表，应用会按顺序尝试。
- `sha256`: 可选，填入后会在下载完成后做校验。
- `force`: 是否强制更新。

示例：

```json
{
  "versionCode": 3,
  "versionName": "1.0.2",
  "title": "发现新版本 1.0.2",
  "changelog": [
    "优化关于页在线更新流程",
    "新增 GitHub Releases 检测、下载与安装",
    "完善安装未知来源应用的授权引导"
  ],
  "apkUrl": "https://github.com/kevySZ/MiniLedger/releases/download/v1.0.2/app-release.apk",
  "apkMirrors": [
    "https://mirror.ghproxy.com/https://github.com/kevySZ/MiniLedger/releases/download/v1.0.2/app-release.apk"
  ],
  "sha256": "",
  "force": false
}
```

## 3. 生效方式

`update.json` 提交到 `main` 后，App 下次点击“检测更新”就会按最新清单检查版本。
