package com.eltavine.duckdetector.core.localization

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RuntimeTextCatalogOctober31Test {
    private val resourceRoot = listOf(File("src/main/res"), File("app/src/main/res"))
        .first { File(it, "values/runtime_localization_catalog.xml").isFile }
    private val repository = generateSequence(resourceRoot.canonicalFile) { it.parentFile }.drop(4).first()
    private val catalog by lazy { catalog() }

    @Test
    fun everyNewResourceHasChineseAndExactlyOneCatalogRegistration() {
        val en = strings(File(resourceRoot, "values/sync_20261031_strings.xml"))
        val zh = strings(File(resourceRoot, "values-zh-rCN/sync_20261031_strings.xml"))
        assertEquals(en.keys, zh.keys)
        val nodes = xml(File(resourceRoot, "values/runtime_localization_catalog.xml")).getElementsByTagName("item")
        val keys = (0 until nodes.length).map { nodes.item(it).textContent.substringAfter("@string/") }
        val tokens = Regex("%(?:\\d+\\$)?[dsrcv]")
        en.forEach { (key, source) ->
            assertEquals(key, 1, keys.count { it == key })
            assertEquals(key, tokens.findAll(source).map { it.value }.toSet(),
                tokens.findAll(zh.getValue(key)).map { it.value }.toSet())
            assertTrue(key, zh.getValue(key).any { it in '\u4e00'..'\u9fff' })
        }
    }

    @Test
    fun actualWrapperExplanationRetainsItsBudgetAndEvidenceLimits() {
        val source = File(repository, "feature/nativeroot/presentation/src/main/kotlin/com/eltavine/duckdetector/" +
            "features/nativeroot/presentation/KernelSuWrapperLeakPresentation.kt").readText()
        val body = source.substringAfter("append(\"Reopens").substringBefore("append(\"collection=")
        val text = "Reopens" + body.substringBefore('"') +
            Regex("append\\(\"([^\"]*)\"\\)").findAll(body.substringAfter('"'))
                .joinToString("") { it.groupValues[1] }.replace("\\n", "")
        val translated = catalog.translate(text.trim())
        assertTrue(translated.contains("最多 768 次"))
        assertTrue(translated.contains("不发起 Root 请求"))
        assertTrue(translated.contains("后续扫描复用结果，新扫描器有独立预算"))
        assertTrue(translated.contains("不能确认 KSU 或 Root"))
        assertTrue(translated.contains("进程退出后仍然存在"))
        assertTrue(translated.contains("尚未经过跨设备校准"))
        assertFalse(translated.contains("Thresholds are provisional"))
    }

    @Test
    fun wrapperNoCandidateIsNotPresentedAsACleanOrMeasuredResult() {
        assertEquals("没有 wrapper 候选项；未评估",
            catalog.translate("No wrapper candidate; not evaluated"))
        assertEquals("未观察到泄漏特征", catalog.translate("Leak pattern not observed"))
        assertEquals("文件对象数量重复增长", catalog.translate("Repeated file-object growth"))
        assertEquals("采集状态=NO_CANDIDATE；errno=0；已完成轮数=0；重新打开次数=0",
            catalog.translate("collection=NO_CANDIDATE; errno=0; rounds=0; reopens=0"))
    }

    @Test
    fun stalePreloadAndRecoveryFailuresRemainDistinct() {
        val checks = mapOf(
            "App Zygote preload belongs to an earlier scan." to "App Zygote 预加载证据来自更早的一次扫描。",
            "App Zygote preload generation was already consumed." to "这一代 App Zygote 预加载证据已经使用过。",
            "App Zygote preload build revision mismatch." to "App Zygote 预加载构建提交不匹配。",
            "App Zygote generation fence could not be persisted." to "无法持久化 App Zygote 代次隔离状态。",
            "App Zygote carrier children did not retire before recovery deadline." to "App Zygote 承载子进程未在恢复时限内退出。",
            "App Zygote was reused after waiting for retirement." to "等待退出后仍复用了同一个 App Zygote。",
        )
        checks.forEach { (source, target) -> assertEquals(target, catalog.translate(source)) }
        val source = "Not started: an app zygote carrier was stopped before it connected earlier in this " +
            "scan, and starting another would stop the app's other helper processes again."
        assertTrue(catalog.translate(source).contains("再次启动会再次停止应用的其他辅助进程"))
    }

    @Test
    fun importantNoticeAlreadyHasAllSevenUpstreamChineseResources() {
        val root = File(repository, "feature/settings/ui/src/main/res")
        val english = strings(File(root, "values/strings.xml"))
        val chinese = strings(File(root, "values-zh-rCN/strings.xml"))
        val keys = english.keys.filter { it.startsWith("settings_important_") }
        assertEquals(7, keys.size)
        keys.forEach { assertTrue(it, chinese.getValue(it).any { char -> char in '\u4e00'..'\u9fff' }) }
    }

    @Test
    fun wrapperSamplesAndPreloadProtocolRemainVerbatim() {
        for (raw in listOf("15600/64; 15600/32; 15856/64", "DUCK_PRELOAD_VERSION_CODE=1124",
            "DUCK_PRELOAD_APK=/data/app/com.example/base.apk", "DUCK_PRELOAD_BUILD_REVISION=8acd43e6",
            "/proc/self/fd/21", "NO_CANDIDATE")) {
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
