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

import org.junit.Assert.assertEquals
import org.junit.Test

class RuntimeTextCatalogTest {

    private val catalog = RuntimeTextCatalog(
        listOf(
            "Warning" to "警告",
            "State" to "状态",
            "Verified" to "已验证",
            "Metrics" to "指标",
            "DANGER" to "危险",
            "OVERVIEW" to "概览",
            "Single-use EC" to "一次性 EC",
            "skipped" to "已跳过",
            "Unavailable kind" to "不可用类别",
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
    fun leavesUnknownTechnicalEvidenceUntouched() {
        assertEquals("ro.boot.verifiedbootstate=green", catalog.translate("ro.boot.verifiedbootstate=green"))
    }
}
