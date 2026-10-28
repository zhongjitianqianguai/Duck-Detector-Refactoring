package com.eltavine.duckdetector.core.localization

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RuntimeTextCatalogOctober28Test {
    private val resourceRoot = listOf(File("src/main/res"), File("app/src/main/res"))
        .first { File(it, "values/runtime_localization_catalog.xml").isFile }
    private val repository = generateSequence(resourceRoot.canonicalFile) { it.parentFile }.drop(4).first()
    private val catalog by lazy { catalog() }

    @Test
    fun everyNewPairIsRegisteredWithMatchingTypedArguments() {
        val english = strings(File(resourceRoot, "values/sync_20261028_strings.xml"))
        val chinese = strings(File(resourceRoot, "values-zh-rCN/sync_20261028_strings.xml"))
        assertEquals(english.keys, chinese.keys)
        val items = xml(File(resourceRoot, "values/runtime_localization_catalog.xml"))
            .getElementsByTagName("item")
        val keys = (0 until items.length).map { items.item(it).textContent.substringAfter("@string/") }
        val tokens = Regex("%(?:\\d+\\$)?[dsrcv]")
        english.forEach { (key, source) ->
            assertEquals(key, 1, keys.count { it == key })
            assertEquals(key, tokens.findAll(source).map { it.value }.toSet(),
                tokens.findAll(chinese.getValue(key)).map { it.value }.toSet())
            assertTrue(key, chinese.getValue(key).any { it in '\u4e00'..'\u9fff' })
        }
    }

    @Test
    fun historyNotificationsHaveChineseResourcesWithUnchangedBackupAndCountArguments() {
        val english = strings(File(resourceRoot, "values/strings.xml"))
        val chinese = strings(File(resourceRoot, "values-zh-rCN/strings.xml"))
        for (key in listOf("scan_history_issue_title", "scan_history_recovered", "scan_history_failed")) {
            assertTrue(chinese.containsKey(key))
            assertEquals(Regex("%\\d+\\$[ds]").findAll(english.getValue(key)).map { it.value }.toSet(),
                Regex("%\\d+\\$[ds]").findAll(chinese.getValue(key)).map { it.value }.toSet())
        }
        assertEquals("无法加载扫描历史", catalog.translate("Could not load scan history"))
        assertEquals("未进行比较", catalog.translate("Not compared"))
    }

    @Test
    fun timingObservationKeepsRawSnapshotsAndNumericResults() {
        val raw = "KernelSuTimingCpuSnapshot(present=[0, 1], online=[0, 1], allowed=[0], detail=)"
        val text = "Supporting AArch64 prctl timing observation; not a KernelSU or root identification. Absence does not exclude either.\n" +
            "initialCpus=$raw; finalCpus=$raw\n" +
            "collection=PARTIAL; verdict=INCONCLUSIVE; cleanup=true\n" +
            "cpu=1; exclusions=[offline, not sampled]\n" +
            "round=2; distance=0.251; similarity=0.998; validity=VALID; support=1024; matched=true"
        val translated = catalog.translate(text)
        assertTrue(translated.contains("不能据此确认 KernelSU 或 Root"))
        assertTrue(translated.contains("初始 CPU 快照=$raw；最终 CPU 快照=$raw"))
        assertTrue(translated.contains("采集状态=PARTIAL；判定=INCONCLUSIVE；清理完成=true"))
        assertTrue(translated.contains("CPU=1；排除原因=[离线、未采样]"))
        assertTrue(translated.contains("轮次=2；距离=0.251；相似度=0.998"))
        assertEquals("工作进程已退出；原因未知", catalog.translate("Worker died; cause unknown"))
    }

    @Test
    fun rawSelinuxProtocolLocalizesLabelsAndKeepsQueriesAndResponses() {
        val query = "u:r:app_zygote:s0 -> u:r:kernel:s0:security check_context"
        val response = "ffffffff ffffffff 00000000 ffffffff 1 0"
        val text = "DirtySepolicy 2.2 compatibility=ERROR | " +
            "DirtySepolicy original checks=unexpected SELinux context: u:r:isolated_app:s0 | " +
            "Transactions=8, distinct queries=4 | Failed query=$query | Raw response=$response | " +
            "A valid response is transport evidence, not proof of a clean policy."
        val translated = catalog.translate(text)
        assertTrue(translated.contains("DirtySepolicy 2.2 兼容判定=ERROR"))
        assertTrue(translated.contains("SELinux 上下文不符合预期：u:r:isolated_app:s0"))
        assertTrue(translated.contains("事务数=8，不同查询数=4"))
        assertTrue(translated.contains("失败的查询=$query"))
        assertTrue(translated.contains("原始响应=$response"))
        assertTrue(translated.contains("不能证明策略干净"))
        assertEquals("访问结果无效：$response", catalog.translate("Invalid access result: $response"))
        assertEquals("内核版本不可用，无法确定 DirtySepolicy 初始基线",
            catalog.translate("kernel version unavailable for DirtySepolicy initial baseline"))
    }

    @Test
    fun nativeAndJavaTrackMetadataPreservesContextAndRuleWhileTranslatingItsMeaning() {
        assertEquals("已评估", catalog.translate("Evaluated"))
        assertEquals("拒绝", catalog.translate("Denied"))
        val raw = "Native dedicated=Allowed carrier=u:r:app_zygote:s0 match=yes controls=passed stable=yes trusted=yes " +
            "query=native selinux_check_access reason=cannot check SELinux access"
        val translated = catalog.translate(raw)
        assertTrue(translated.startsWith("专用原生轨道=已允许；承载上下文=u:r:app_zygote:s0"))
        assertTrue(translated.contains("对照=通过"))
        assertTrue(translated.contains("查询=native selinux_check_access"))
        assertTrue(translated.contains("原因=无法检查 SELinux 访问"))
        assertEquals("状态基线=旧版初始状态计数器；avdSeqNo=1",
            catalog.translate("Status baseline=Legacy initial status counters; avdSeqNo=1"))
        assertEquals("状态基线=新版初始状态计数器；avdSeqNo=5（非初始策略代次；可能是重载或策略差异）",
            catalog.translate("Status baseline=Modern initial status counters; avdSeqNo=5 " +
                "(non-initial epoch; reload or policy difference)"))
    }

    @Test
    fun exitHandlerDetailsTranslateCompositeCoverageWithoutAttributingAFramework() {
        val text = "Read 120 exit handlers under libc's mutex from this process; 5 cleared entries. " +
            "Normal finalization can also leave cleared entries. " +
            "No stable application-registration boundary was found. " +
            "Some callback owners could not be resolved in stable executable mappings. " +
            "Entries: 120; cleared: 5; runtime-adjacent gaps: 2; anonymous callbacks: 1. " +
            "These observations do not identify an injection framework or version."
        val translated = catalog.translate(text)
        assertTrue(translated.contains("120 个退出处理器"))
        assertTrue(translated.contains("5 个条目已清空"))
        assertTrue(translated.contains("未找到稳定的应用注册边界"))
        assertTrue(translated.contains("运行时附近空缺：2；匿名回调：1"))
        assertTrue(translated.contains("不能确认注入框架或版本"))
        assertFalse(translated.contains("No stable"))
        assertFalse(translated.contains("Some callback"))
        val path = "/memfd:payload (deleted)"
        val callback = catalog.translate("Handler 17 resolves into $path before application registrations. " +
            "This is heuristic residue, not a framework identity.")
        assertTrue(callback.contains(path))
        assertTrue(callback.contains("不能确认具体框架"))
    }

    @Test
    fun provisioningCborStaysRawWhileNewNoncomparisonWarningIsTranslated() {
        val raw = "{1: [\"APPROVED\", \"Unknown; raw\"], 2: \"fingerprint\"}"
        val text = "Provisioning key 5 (CBOR): $raw Fingerprint and VBMeta were not compared with the current build: " +
            "their certificate field mapping is undocumented. Provisioning metadata may predate a system update."
        val translated = catalog.translate(text)
        assertTrue(translated.contains(raw))
        assertTrue(translated.contains("未将指纹和 VBMeta 与当前系统构建比较"))
        assertTrue(translated.contains("配置供应元数据可能早于系统更新"))
        assertFalse(translated.contains("were not compared"))
    }

    @Test
    fun machinePayloadsAndCompatibilityTokensRemainVerbatim() {
        for (raw in listOf("DIRTY_SEPOLICY_RAW=UNAVAILABLE", "ATEXIT_TABLE_STATE=COLLECTED",
            "u:r:ksu:s0", "/sys/fs/selinux/access", "KernelSuTimingCoreStatus.CPU_MISMATCH",
            "ffffffff ffffffff 00000000 ffffffff 1 0")) {
            assertEquals(raw, catalog.translate(raw))
        }
    }

    private fun catalog(): RuntimeTextCatalog {
        val roots = listOf("uihelper", "core", "feature").flatMap { directory ->
            File(repository, directory).walkTopDown().onEnter {
                it.name !in setOf("build", ".git", ".gradle", ".cxx")
            }.filter { it.isDirectory && it.name == "res" && it.parentFile?.name == "main" &&
                it.parentFile?.parentFile?.name == "src" }.toList()
        } + resourceRoot.canonicalFile
        fun resources(directory: String) = roots.flatMap { File(it, directory).listFiles().orEmpty().toList() }
            .filter { it.extension == "xml" }.flatMap { strings(it).toList() }.toMap()
        val english = resources("values")
        val chinese = resources("values-zh-rCN")
        val items = xml(File(resourceRoot, "values/runtime_localization_catalog.xml")).getElementsByTagName("item")
        return RuntimeTextCatalog((0 until items.length).map { index ->
            val key = items.item(index).textContent.substringAfter("@string/")
            english.getValue(key) to (chinese[key] ?: english.getValue(key))
        })
    }

    private fun xml(file: File) = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)

    private fun strings(file: File): Map<String, String> {
        val nodes = xml(file).getElementsByTagName("string")
        return (0 until nodes.length).associate { index ->
            val node = nodes.item(index)
            node.attributes.getNamedItem("name").nodeValue to node.textContent.replace("\\'", "'")
        }
    }
}
