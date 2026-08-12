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
        assertEquals("状态：已验证", catalog.translate("State: Verified"))
        assertEquals("  指标:", catalog.translate("  Metrics:"))
        assertEquals("  [危险] TEE", catalog.translate("  [DANGER] TEE"))
        assertEquals("----- 概览 -----", catalog.translate("----- OVERVIEW -----"))
        assertEquals("一次性 EC 已跳过。", catalog.translate("Single-use EC skipped."))
        assertEquals("不可用类别=KEY_NOT_FOUND", catalog.translate("Unavailable kind=KEY_NOT_FOUND"))
    }

    @Test
    fun leavesUnknownTechnicalEvidenceUntouched() {
        assertEquals("ro.boot.verifiedbootstate=green", catalog.translate("ro.boot.verifiedbootstate=green"))
    }
}
