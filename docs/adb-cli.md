# Duck Detector ADB CLI

ADB CLI 读取应用界面使用的同一份扫描结果，不会另起一套判断逻辑。接口只允许 ADB shell、Root 和应用自身访问，普通第三方应用不能调用。

```powershell
$device = '192.168.6.17:5555'
$uri = 'content://com.eltavine.duckdetector.cli'

# 启动扫描；命令立即返回
adb -s $device shell content call --uri $uri --method scan

# 轮询状态，直到 scanning=false 且 pending=0
adb -s $device shell content read --uri "$uri/status"

# 读取全部需关注的异常
adb -s $device shell content read --uri "$uri/anomalies"

# 读取完整扫描报告
adb -s $device shell content read --uri "$uri/report"

# 查看手机内置帮助
adb -s $device shell content read --uri "$uri/help"
```

`anomalies` 包含 `DANGER`、`WARNING` 和检测失败产生的 `ERROR`；正常信息提示和 `CLEAR` 不会被误列为异常。完整报告可能较长，因此通过 `content read` 流式输出，避免 Binder 返回值大小限制。
