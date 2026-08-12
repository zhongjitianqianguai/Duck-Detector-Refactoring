/*
 * Copyright 2026 Duck Apps Contributor
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.eltavine.duckdetector.core.cli

object CliContract {
    const val Authority = "com.eltavine.duckdetector.cli"
    const val BaseUri = "content://$Authority"
    const val ActionScan = "com.eltavine.duckdetector.action.CLI_SCAN"

    val HelpText: String = """
        Duck Detector ADB CLI

        启动或刷新扫描：
          adb shell am start -W -n com.eltavine.duckdetector/.MainActivity -a $ActionScan

        读取扫描状态：
          adb shell content read --uri $BaseUri/status

        读取异常列表：
          adb shell content read --uri $BaseUri/anomalies

        读取完整报告：
          adb shell content read --uri $BaseUri/report

        查看帮助：
          adb shell content read --uri $BaseUri/help

        说明：启动命令返回后请轮询 status，直到 scanning=false 且 pending=0。
        接口仅允许 ADB shell、Root 与应用自身访问。
    """.trimIndent()
}

internal object CliAccessPolicy {
    const val RootUid = 0
    const val ShellUid = 2_000

    fun isAllowed(callingUid: Int, ownUid: Int): Boolean {
        return callingUid == RootUid || callingUid == ShellUid || callingUid == ownUid
    }
}
