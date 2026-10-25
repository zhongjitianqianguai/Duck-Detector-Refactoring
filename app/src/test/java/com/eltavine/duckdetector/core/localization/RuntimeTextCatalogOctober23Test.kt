package com.eltavine.duckdetector.core.localization

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RuntimeTextCatalogOctober23Test {
    private val catalog by lazy { catalog() }

    @Test
    fun rawArgumentsPreserveStructuredEvidenceAndReorderedValues() {
        val catalog = RuntimeTextCatalog(listOf(
            "Verified" to "已验证",
            "Observed" to "已观测",
            "Evidence %1\$r; state %2\$s" to "状态 %2\$s；证据 %1\$r",
        ))
        val raw = "{1: [\"Observed\", \"Verified and raw\"], 2: \"a; b\"}"
        assertEquals("状态 已验证；证据 $raw", catalog.translate("Evidence $raw; state Verified"))
    }

    @Test
    fun rkpMetadataIsTranslatedWithoutChangingCborNotation() {
        val raw = "{1: [\"APPROVED\", \"NOT_APPROVED\"], 2: \"Observed and Unknown; raw\"}"
        assertEquals("配置供应字段 5（CBOR）：$raw", catalog.translate("Provisioning key 5 (CBOR): $raw"))
        val text = "Provisioning key 5 (CBOR): $raw\n" +
            "Provisioning key 5 display was truncated at 8192 characters.\n" +
            "Informational only: approval fields have undocumented semantics. " +
            "CTS status cannot be determined from these fields, including on RKP V3."
        val translated = catalog.translate(text)
        assertTrue(translated.contains(raw))
        assertTrue(translated.contains("显示内容已截断为 8192 个字符"))
        assertTrue(translated.contains("不能根据这些字段判断 CTS 状态"))
        assertFalse(translated.contains("Informational only"))
    }

    @Test
    fun translatesNetworkEmptyFailureVariantsAndKeepsRawTransports() {
        assertEquals("调用方默认网络；VPN 传输仅是背景信息，不能证明此应用的请求经过代理。",
            catalog.translate("caller default;  VPN transport is context and does not establish proxying of this app's requests."))
        assertEquals("仅限调用方 UID 的范围。结果为 null 可能表示已断开连接或访问受阻。 ",
            catalog.translate("Calling UID scope. A null result can mean disconnected or blocked. "))
        assertEquals("已观测；VPN, WIFI", catalog.translate("Observed; VPN, WIFI"))
        assertEquals("已观测；接口 tun0；共 2 条路由（IPv4 1，IPv6 1，默认路由 1）",
            catalog.translate("Observed; tun0; 2 routes (IPv4 1, IPv6 1, default 1)"))
        assertEquals("已观测；共 2 条路由（IPv4 1，IPv6 1，默认路由 1）",
            catalog.translate("Observed; 2 routes (IPv4 1, IPv6 1, default 1)"))
        val module = "Exact module.prop ID only; version 1.2, beta.  Protected or renamed paths are outside coverage; " +
            "metadata can be modified. Does not identify the running core or mechanism."
        assertTrue(catalog.translate(module).startsWith("仅按 module.prop 中完全匹配的 ID 检查；版本 1.2, beta。"))
    }

    @Test
    fun translatesOptionalLocationContextWithAndWithoutFailureText() {
        assertEquals("最多创建两个合成对象；不获取真实位置。字段：LATITUDE, LONGITUDE。 ",
            catalog.translate("Up to two synthetic objects; no real fix. Fields: LATITUDE, LONGITUDE. "))
        assertEquals("最多创建两个合成对象；不获取真实位置。字段：none。公开的 setMock 需要 API 31；更早版本不支持。",
            catalog.translate("Up to two synthetic objects; no real fix. Fields: none.  Public setMock requires API 31; older releases are unsupported."))
        val cache = catalog.translate("Permission: NONE; location switch: unknown; sample start (elapsed ms): not sampled. " +
            "Disabled by default; enable in setup or settings.")
        assertEquals("权限：NONE；位置开关：unknown；采样起始时间（已运行毫秒数）：not sampled。默认关闭; 可在初始设置或设置中启用。", cache)
        assertEquals("时间范围：STALE；仅为缓存元数据，不代表服务当前状态。 ",
            catalog.translate("Age bucket: STALE; cached metadata only, not current service state. "))
    }

    @Test
    fun translatesSemicolonJoinedFailureArgumentsWithoutDroppingTheirMeaning() {
        assertEquals("最多创建两个合成对象；不获取真实位置。字段：none。访问被拒绝; 权限或 AppOps 可能限制了调用方。",
            catalog.translate("Up to two synthetic objects; no real fix. Fields: none. Access denied; permission or AppOps may restrict the caller."))
        val parent = "Parent preload (PID=42, entry CPU=7): A−B=N/A; A=N/A, B=N/A; " +
            "A slower=0/0; blocks positive/negative/zero=0/0/0; migrations=1; complete=false; errno=11. " +
            "Legacy pinned parent diagnostic; unavailable for PoC classification. Partial-sample medians are diagnostic only. "
        val translated = catalog.translate(parent)
        assertTrue(translated.contains("旧版固定 CPU 的父进程诊断; 不能用于 PoC 分类。"))
        assertTrue(translated.contains("不完整样本的中位数仅供诊断。"))
        assertFalse(translated.contains("Legacy"))
        assertFalse(translated.contains("Partial-sample"))
    }

    @Test
    fun translatesCompleteParentAndChildTimingEvidenceWithLimits() {
        val parent = "Parent preload (PID=42, entry CPU=7): A−B=-10 ns; A=100 ns, B=110 ns; " +
            "A slower=1/512; blocks positive/negative/zero=0/16/0; migrations=0; complete=true; errno=0. " +
            "This parent sample supplies the timing verdict. "
        val translated = catalog.translate(parent)
        assertTrue(translated.startsWith("父进程预加载（PID=42，初始 CPU=7）：A−B=-10 ns"))
        assertTrue(translated.contains("正/负/零分块数=0/16/0"))
        assertTrue(translated.endsWith("此父进程样本用于时序判定。 "))
        val child = "Pinned child diagnostic (PID=43, CPU=7): paired A−B=N/A; A=N/A, B=N/A; A slower=0/0; " +
            "blocks positive/negative/zero=0/0/0; CPU migrations=0; collection=Unavailable. " +
            "Child timing is diagnostic only and never replaces the parent sample. " +
            "Stock A and B already take different kernel paths. Timing direction is supporting information, " +
            "not proof of hiding, a specific root tool, or a clean environment."
        val childText = catalog.translate(child)
        assertTrue(childText.startsWith("固定 CPU 的子进程诊断（PID=43，CPU=7）"))
        assertTrue(childText.contains("绝不替代父进程样本"))
        assertTrue(childText.contains("不能证明存在隐藏、具体 Root 工具或环境干净"))
        assertEquals("A−B 时序差为正", catalog.translate("Positive A−B timing asymmetry"))
    }

    @Test
    fun translatesTimingRulesWithCpuSwitchesBetweenCompletedPairs() {
        val method = "A=invalid type, B=same-length newline prefix; both must return EINVAL. " +
            "512 AB/BA pairs, 32 warmups; CLOCK_MONOTONIC_RAW; paired A−B > 0 ns is positive, " +
            "0 ns is baseline, and < 0 ns is negative. Block counts are diagnostic only. " +
            "Measured in the preload parent without changing CPU affinity. " +
            "CPU switches between completed pairs are allowed. Incomplete measurements are normally " +
            "inconclusive, except migration-only samples with at least 256 accepted pairs, positive " +
            "A−B, and at least 90% A-slower pairs. " +
            "One retained preload is one measurement."
        val translated = catalog.translate(method)
        assertEquals("A 为无效类型，B 为等长的换行前缀；两者都必须返回 EINVAL。" +
            "执行 512 对 AB/BA 和 32 次预热；使用 CLOCK_MONOTONIC_RAW；" +
            "成对 A−B 大于 0 ns 为正，小于 0 ns 为负，等于 0 ns 为基线。分块计数仅供诊断。" +
            "在预加载父进程中测量，不更改 CPU 亲和性。" +
            "允许在已完成的采样对之间切换 CPU。不完整的测量通常无法判定，" +
            "但若仅因 CPU 迁移而不完整、至少接受了 256 对采样、A−B 为正" +
            "且至少 90% 的采样对中 A 更慢，则仍判为阳性。每次保留的预加载仅算一次测量。", translated)
        assertEquals("$translated\n父进程预加载：N/A（未采集）。 ",
            catalog.translate("$method\nParent preload: N/A (not collected). "))
    }

    @Test
    fun keepsMachineValuesUnchanged() {
        for (raw in listOf("com.google.android.gms", "com.vincenthzr.locationspoofer", "/sys/fs/selinux/status",
            "http.proxyHost", "APPROVED", "NOT_APPROVED", "ROUND=1\t128\t256", "2026-10-23T00:00:00Z")) {
            assertEquals(raw, catalog.translate(raw))
        }
    }

    @Test
    fun translatesProfileMatchesAndRkpAuthenticationFailure() {
        assertEquals("已检查 5 个应用 · 匹配 1 项", catalog.translate("5 apps checked · 1 match(es)"))
        assertEquals("由于本地证书链签名验证失败，配置供应信息未通过认证。",
            catalog.translate("Provisioning info was not authenticated because local chain signatures failed."))
    }

    private fun catalog(): RuntimeTextCatalog {
        val root = listOf(File("src/main/res"), File("app/src/main/res"))
            .first { File(it, "values/runtime_localization_catalog.xml").isFile }
        val repository = generateSequence(root.canonicalFile) { it.parentFile }.drop(4).first()
        val resourceRoots = listOf("uihelper", "core", "feature").flatMap { directory ->
            File(repository, directory).walkTopDown().onEnter {
                it.name !in setOf("build", ".git", ".gradle", ".cxx")
            }.filter { it.isDirectory && it.name == "res" && it.parentFile?.name == "main" &&
                it.parentFile?.parentFile?.name == "src" }.toList()
        } + root.canonicalFile
        fun strings(directory: String): Map<String, String> = resourceRoots.flatMap {
            File(it, directory).listFiles().orEmpty().toList()
        }
            .filter { it.extension == "xml" }.flatMap { file ->
                val nodes = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
                    .getElementsByTagName("string")
                (0 until nodes.length).map { index ->
                    val node = nodes.item(index)
                    node.attributes.getNamedItem("name").nodeValue to node.textContent.replace("\\'", "'")
                }
            }.toMap()
        val english = strings("values")
        val chinese = strings("values-zh-rCN")
        val items = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(File(root, "values/runtime_localization_catalog.xml")).getElementsByTagName("item")
        return RuntimeTextCatalog((0 until items.length).map { index ->
            val key = items.item(index).textContent.substringAfter("@string/")
            // Match Android's default-resource fallback for untranslated module strings.
            english.getValue(key) to (chinese[key] ?: english.getValue(key))
        })
    }
}
