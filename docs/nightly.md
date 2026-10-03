# Fork Nightly

本 fork 保留两条开发分支：`master` 用于主线集成，`nightly` 用于发布经过验证的构建。两条分支均保留中文运行时翻译、ADB CLI 和已有界面定制；发布时将主线与发布分支对齐。

`nightly` 发布标签与同名分支是不同对象。推送分支时使用完整引用，避免误推标签：

```sh
git push origin refs/heads/nightly:refs/heads/nightly
```

应用只读取本 fork 的 [Nightly 更新清单](https://github.com/zhongjitianqianguai/Duck-Detector-Refactoring/releases/download/nightly/update.json)，APK 下载地址也必须属于同一仓库的 Nightly 发布。官方仓库的 Nightly APK 不会被此更新源接受。

发布制品须与清单中的提交、版本号、大小和 SHA-256 一致，并保持 APK 签名与已安装版本兼容。当前定制构建使用本机已有调试签名；私钥不得进入仓库或发布附件。

这里只规定分支与发布制品的对应关系，不自动发送 Telegram 消息。原有官方工作流仍保留；本 fork 的 Nightly 发布独立完成。
