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

package com.eltavine.duckdetector.core.localization

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RuntimeTextCatalogTest {

    private val catalog = RuntimeTextCatalog(
        listOf(
            "Warning" to "警告",
            "Line ending" to "行尾",
            "Typed tokens %1\$d from %2\$s." to "结构化项 %1\$d 来自 %2\$s。",
            "State" to "状态",
            "Verified" to "已验证",
            "Metrics" to "指标",
            "DANGER" to "危险",
            "OVERVIEW" to "概览",
            "Single-use EC" to "一次性 EC",
            "skipped" to "已跳过",
            "Unavailable kind" to "不可用类别",
            "Throne hunt" to "Throne Hunt 探测",
            "Throne hunt counters" to "Throne Hunt 计数",
            "Watch denied" to "监视权限被拒绝",
            "Collection %1\$s; baseline=%2\$d" to "采集状态 %1\$s；baseline=%2\$d",
            "Watching %1\$s for throne hunt IN_OPEN/IN_ACCESS." to
                "正在监视 %1\$s 的 Throne Hunt IN_OPEN/IN_ACCESS 事件。",
            "inotify_add_watch denied for %1\$s: %2\$s" to
                "inotify_add_watch 无法为 %1\$s 安装监视：%2\$s",
            "Scanning %1\$d/%2\$d" to "正在扫描 %1\$d/%2\$d",
            "%1\$d props · %2\$d certs · %3\$d cross-checks" to
                "%1\$d 个属性 · %2\$d 张证书 · %3\$d 项交叉检查",
            "%1\$s reports %2\$d findings" to "%1\$s 报告了 %2\$d 项发现",
            "Start with %1\$s." to "先查看%1\$s。",
            "Custom ROM" to "自定义 ROM",
            "Impact & Guidance" to "影响与建议",
            "HMA Alert" to "HMA 警报",
            "Monitored Package Catalog (%1\$d targets across %2\$d categories)" to
                "监控软件包目录（%1\$d 个目标，分为 %2\$d 类）",
            "%1\$s (%2\$d):" to "%1\$s（%2\$d 个）：",
            "%1\$s [methods: %2\$s]" to "%1\$s [检测方法：%2\$s]",
            "App Version    : %1\$s (Build %2\$d)" to "应用版本：%1\$s（构建 %2\$d）",
            "✖ DANGER" to "✖ 危险",
            "Netlink link boundary: hardware MAC leak detected on %1\$s (%2\$s) (SELinux bypass detected)." to
                "Netlink 链路权限边界：检测到接口 %1\$s 的硬件 MAC 泄露（%2\$s）（检测到 SELinux 绕过）。",
            "SELinux permission boundary breach: ARP/neighbor entry leaked (%1\$s -> %2\$s) on API %3\$d (AOSP netlink_route_socket getneigh restriction bypassed)." to
                "SELinux 权限边界被突破：API %3\$d 上泄露了 ARP/邻居项（%1\$s -> %2\$s）（AOSP netlink_route_socket getneigh 限制被绕过）。",
            "kernelpatch superkey" to "KernelPatch 超级密钥读取检测",
            "yes" to "是",
            "no" to "否",
            "Probed attempts: %1\$d, page faulted in: %2\$d, pre-resident: %3\$d, control resident: %4\$d, control unmapped: %5\$d, page unmapped: %6\$d, mincore errors: %7\$d, kernel: %8\$s, faulting-uaccess scope: %9\$s, usable: %10\$s" to
                "探测次数：%1\$d，页面被调入：%2\$d，探测前已驻留：%3\$d，控制页已驻留：%4\$d，控制页映射失败：%5\$d，探测页映射失败：%6\$d，mincore 错误：%7\$d，内核：%8\$s，faulting-uaccess 范围：%9\$s，可用：%10\$s",
            "Test Result: %1\$s" to "检测结果：%1\$s",
            "CPU %1\$d: cached=%2\$s (%3\$s), mrs=%4\$s, %5\$s" to
                "CPU %1\$d：缓存值=%2\$s（%3\$s），MRS=%4\$s，%5\$s",
            "consistent" to "一致",
            "mismatch" to "不一致",
            "Zygote next mount view" to "Zygote Next 挂载视图",
            "Result" to "结果",
            "Detail" to "详情",
            "READY" to "已就绪",
            "Main mount IDs" to "主进程挂载 ID",
            "Main mount IDs: root=%1\$s, range=%2\$s" to
                "主进程挂载 ID：根=%1\$s，范围=%2\$s",
            "root=%1\$s, range=%2\$s" to "根=%1\$s，范围=%2\$s",
            "Zygote next native service timed out after %1\$d ms." to
                "Zygote Next 原生服务在 %1\$d 毫秒后超时。",
            "Init-managed namespace coverage is unverified: %1\$s." to
                "尚未验证由 init 管理的命名空间覆盖：%1\$s。",
            "namespace identities are missing or equal" to "命名空间身份缺失或相同",
            "Native memory snapshot was unavailable" to "原生内存快照不可用",
            "duckdetector could not be loaded: %1\$s" to "无法加载 duckdetector：%1\$s",
            "PackageManager inventory unavailable: %1\$s" to "PackageManager 清单不可用：%1\$s",
            "No pre-fix delay" to "未发现修复前版本的延迟特征",
            "Key path: %1\$s us, empty key: %2\$s us, diff: %3\$s us, same-core: %4\$s" to
                "密钥路径：%1\$s 微秒，空密钥：%2\$s 微秒，差值：%3\$s 微秒，同核心：%4\$s",
            "wrapper pair disagreed (ret %1\$s/%2\$s, errno %3\$s/%4\$s), no stable baseline to compare the inline svc against" to
                "包装调用两次结果不一致（返回值 %1\$s/%2\$s，errno %3\$s/%4\$s），没有稳定基线可与内联 svc 比较",
            "Not confirmed" to "未确认",
            "StrongBox signing returned in %1\$dus, under the %2\$dus this probe expects of a discrete secure element." to
                "StrongBox 签名耗时 %1\$d 微秒，低于该探针对独立安全元件预期的 %2\$d 微秒。",
        ),
    )

    @Test
    fun translatesExactText() {
        assertEquals("警告", catalog.translate("Warning"))
    }

    @Test
    fun translatesFormattedTextAndPreservesArguments() {
        assertEquals("正在扫描 3/15", catalog.translate("Scanning 3/15"))
        assertEquals(
            "24 个属性 · 3 张证书 · 2 项交叉检查",
            catalog.translate("24 props · 3 certs · 2 cross-checks"),
        )
        assertEquals("TEE 报告了 4 项发现", catalog.translate("TEE reports 4 findings"))
        assertEquals(
            "先查看自定义 ROM 和 TEE。",
            catalog.translate("Start with Custom ROM and TEE."),
        )
    }

    @Test
    fun translatesTypedFormatTokensAndLineSuffixes() {
        assertEquals("结构化项 3 来自 /data/pkg。", catalog.translate("Typed tokens 3 from /data/pkg."))
        assertEquals("行尾。", catalog.translate("Line ending."))
        assertEquals("行尾:", catalog.translate("Line ending:"))
    }

    @Test
    fun classifiesSimplifiedChineseFromTypedLocaleParts() {
        assertTrue(
            Locale.Builder()
                .setLanguage("zh")
                .setRegion("CN")
                .build()
                .prefersSimplifiedChinese(),
        )
        assertTrue(
            Locale.Builder()
                .setLanguage("zh")
                .setScript("Hans")
                .setRegion("TW")
                .build()
                .prefersSimplifiedChinese(),
        )
        assertFalse(
            Locale.Builder()
                .setLanguage("zh")
                .setScript("Hant")
                .setRegion("CN")
                .build()
                .prefersSimplifiedChinese(),
        )
        listOf("TW", "HK", "MO").forEach { region ->
            assertFalse(
                Locale.Builder()
                    .setLanguage("zh")
                    .setRegion(region)
                    .build()
                    .prefersSimplifiedChinese(),
            )
        }
        assertTrue(
            Locale.Builder()
                .setLanguage("zh")
                .setScript("Latn")
                .setRegion("CN")
                .build()
                .prefersSimplifiedChinese(),
        )
        assertFalse(
            Locale.Builder()
                .setLanguage("en")
                .setScript("Hans")
                .build()
                .prefersSimplifiedChinese(),
        )
    }

    @Test
    fun translatesCompositeAndLabelValueText() {
        assertEquals("警告 · 已验证", catalog.translate("Warning · Verified"))
        assertEquals("LSPosed 和 TEE", catalog.translate("LSPosed and TEE"))
        assertEquals("状态：已验证", catalog.translate("State: Verified"))
        assertEquals("  指标:", catalog.translate("  Metrics:"))
        assertEquals("  [危险] TEE", catalog.translate("  [DANGER] TEE"))
        assertEquals("----- 概览 -----", catalog.translate("----- OVERVIEW -----"))
        assertEquals("一次性 EC 已跳过。", catalog.translate("Single-use EC skipped."))
        assertEquals("不可用类别=KEY_NOT_FOUND", catalog.translate("Unavailable kind=KEY_NOT_FOUND"))
    }

    @Test
    fun translatesThroneHuntCompositeAndPreservesRawEvidenceFields() {
        assertEquals(
            "Throne Hunt 探测：监视权限被拒绝 · open=1 access=2",
            catalog.translate("Throne hunt: Watch denied · open=1 access=2"),
        )
        assertEquals(
            "Throne Hunt 计数 · open=3 access=2 raw=4 invalid=1 baseline=2",
            catalog.translate("Throne hunt counters · open=3 access=2 raw=4 invalid=1 baseline=2"),
        )
        assertEquals("采集状态 COLLECTED；baseline=2", catalog.translate("Collection COLLECTED; baseline=2"))
        assertEquals(
            "正在监视 /data/app/pkg 的 Throne Hunt IN_OPEN/IN_ACCESS 事件。",
            catalog.translate("Watching /data/app/pkg for throne hunt IN_OPEN/IN_ACCESS."),
        )
        assertEquals(
            "inotify_add_watch 无法为 /data/app/pkg 安装监视：Permission denied",
            catalog.translate("inotify_add_watch denied for /data/app/pkg: Permission denied"),
        )
        assertEquals(
            "collectionDetail=正在监视 /data/app/pkg 的 Throne Hunt IN_OPEN/IN_ACCESS 事件。",
            catalog.translate("collectionDetail=Watching /data/app/pkg for throne hunt IN_OPEN/IN_ACCESS."),
        )
        assertEquals("collectionOutcome=COLLECTED", catalog.translate("collectionOutcome=COLLECTED"))
    }

    @Test
    fun translatesBracketHeadingsAndExportTemplates() {
        assertEquals("[影响与建议]", catalog.translate("[Impact & Guidance]"))
        assertEquals(
            "[监控软件包目录（18 个目标，分为 3 类）]",
            catalog.translate("[Monitored Package Catalog (18 targets across 3 categories)]"),
        )
        assertEquals("Magisk（4 个）：", catalog.translate("Magisk (4):"))
        assertEquals(
            "Tool (pkg) [检测方法：storage、package]",
            catalog.translate("Tool (pkg) [methods: storage, package]"),
        )
        assertEquals(
            "应用版本：1.2.3（构建 42）",
            catalog.translate("App Version    : 1.2.3 (Build 42)"),
        )
    }

    @Test
    fun translatesPermissionBoundaryTemplatesAndPreservesEvidence() {
        assertEquals(
            "Netlink 链路权限边界：检测到接口 wlan0 的硬件 MAC 泄露（12:34:56:78:9a:bc）（检测到 SELinux 绕过）。",
            catalog.translate(
                "Netlink link boundary: hardware MAC leak detected on wlan0 (12:34:56:78:9a:bc) (SELinux bypass detected).",
            ),
        )
        assertEquals(
            "SELinux 权限边界被突破：API 36 上泄露了 ARP/邻居项（192.168.1.1 -> 12:34:56:78:9a:bc）（AOSP netlink_route_socket getneigh 限制被绕过）。",
            catalog.translate(
                "SELinux permission boundary breach: ARP/neighbor entry leaked (192.168.1.1 -> 12:34:56:78:9a:bc) on API 36 (AOSP netlink_route_socket getneigh restriction bypassed).",
            ),
        )
    }

    @Test
    fun translatesKernelPatchSuperkeyResultAndNestedMeasurement() {
        assertEquals("KernelPatch 超级密钥读取检测", catalog.translate("kernelpatch superkey"))
        assertEquals(
            "检测结果：探测次数：4，页面被调入：1，探测前已驻留：0，控制页已驻留：0，控制页映射失败：0，探测页映射失败：0，mincore 错误：0，内核：6.1.0，faulting-uaccess 范围：是，可用：否",
            catalog.translate(
                "Test Result: Probed attempts: 4, page faulted in: 1, pre-resident: 0, control resident: 0, control unmapped: 0, page unmapped: 0, mincore errors: 0, kernel: 6.1.0, faulting-uaccess scope: yes, usable: no",
            ),
        )
    }

    @Test
    fun translatesArm64CpuIdentityObservation() {
        assertEquals(
            "CPU 3：缓存值=0x410fd034（sysfs），MRS=0x410fd034，一致",
            catalog.translate("CPU 3: cached=0x410fd034 (sysfs), mrs=0x410fd034, consistent"),
        )
    }

    @Test
    fun translatesZygoteNextCopyTextAndErrors() {
        assertEquals(
            "Zygote Next 挂载视图\n结果：已就绪\n主进程挂载 ID：根=42，范围=42..88",
            catalog.translate(
                "Zygote next mount view\nResult: READY\nMain mount IDs: root=42, range=42..88",
            ),
        )
        assertEquals(
            "Zygote Next 原生服务在 1500 毫秒后超时。",
            catalog.translate("Zygote next native service timed out after 1500 ms."),
        )
        assertEquals(
            "尚未验证由 init 管理的命名空间覆盖：命名空间身份缺失或相同。",
            catalog.translate(
                "Init-managed namespace coverage is unverified: namespace identities are missing or equal.",
            ),
        )
    }

    @Test
    fun translatesNativeCollectionAndInventoryFailures() {
        assertEquals(
            "原生内存快照不可用：无法加载 duckdetector：UnsatisfiedLinkError",
            catalog.translate(
                "Native memory snapshot was unavailable: duckdetector could not be loaded: UnsatisfiedLinkError",
            ),
        )
        assertEquals(
            "PackageManager 清单不可用：SecurityException: denied",
            catalog.translate("PackageManager inventory unavailable: SecurityException: denied"),
        )
    }

    @Test
    fun translatesKernelPatchAndVirtualizationMeasurements() {
        assertEquals(
            "密钥路径：12.5000 微秒，空密钥：4.2500 微秒，差值：8.2500 微秒，同核心：是",
            catalog.translate(
                "Key path: 12.5000 us, empty key: 4.2500 us, diff: 8.2500 us, same-core: yes",
            ),
        )
        assertEquals(
            "包装调用两次结果不一致（返回值 -1/-1，errno 2/13），没有稳定基线可与内联 svc 比较",
            catalog.translate(
                "wrapper pair disagreed (ret -1/-1, errno 2/13), no stable baseline to compare the inline svc against",
            ),
        )
    }

    @Test
    fun translatesStrongBoxBulletSeparatedWarnings() {
        assertEquals(
            "未确认 • StrongBox 签名耗时 420 微秒，低于该探针对独立安全元件预期的 2000 微秒。",
            catalog.translate(
                "Not confirmed • StrongBox signing returned in 420us, under the 2000us this probe expects of a discrete secure element.",
            ),
        )
    }

    @Test
    fun leavesUnknownTechnicalEvidenceUntouched() {
        assertEquals("ro.boot.verifiedbootstate=green", catalog.translate("ro.boot.verifiedbootstate=green"))
    }
}
