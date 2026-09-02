# Duck Detector ADB CLI

CLI 通过 ADB shell 直接访问 Duck Detector 的现有扫描能力，读取与应用界面相同的扫描结果，不另起一套判断逻辑。接口只允许 ADB shell、Root 和应用自身访问，普通第三方应用不能调用。

启动扫描：

```sh
adb shell am start -W -n com.eltavine.duckdetector/.MainActivity -a com.eltavine.duckdetector.action.CLI_SCAN
```

轮询状态，直到 `scanning=false` 且 `pending=0`：

```sh
adb shell content read --uri content://com.eltavine.duckdetector.cli/status
```

读取需关注的异常：

```sh
adb shell content read --uri content://com.eltavine.duckdetector.cli/anomalies
```

读取完整扫描报告：

```sh
adb shell content read --uri content://com.eltavine.duckdetector.cli/report
```

查看内置帮助：

```sh
adb shell content read --uri content://com.eltavine.duckdetector.cli/help
```

以上命令默认用于仅连接一个设备的情况；连接多个设备时，可在每条命令的 `adb` 后添加 `-s <设备序列号>`。

`anomalies` 包含 `DANGER`、`WARNING` 和检测失败产生的 `ERROR`；正常信息提示和 `CLEAR` 不会被误列为异常。完整报告可能较长，因此通过 `content read` 流式输出，避免 Binder 返回值大小限制。
