package com.eltavine.duckdetector.core.localization

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RuntimeTextCatalogOctober25Test {
    private val resourceRoot = listOf(File("src/main/res"), File("app/src/main/res"))
        .first { File(it, "values/runtime_localization_catalog.xml").isFile }
    private val repository = generateSequence(resourceRoot.canonicalFile) { it.parentFile }.drop(4).first()
    private val catalog by lazy { catalog() }

    @Test
    fun registersEveryNewResourceWithMatchingChineseArguments() {
        val english = strings(File(resourceRoot, "values/sync_20261025_strings.xml"))
        val chinese = strings(File(resourceRoot, "values-zh-rCN/sync_20261025_strings.xml"))
        assertEquals(english.keys, chinese.keys)
        val items = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(File(resourceRoot, "values/runtime_localization_catalog.xml")).getElementsByTagName("item")
        val keys = (0 until items.length).map { items.item(it).textContent.substringAfter("@string/") }
        val tokens = Regex("%(?:\\d+\\$)?[dsrc]")
        english.forEach { (key, source) ->
            assertEquals(key, 1, keys.count { it == key })
            assertEquals(key, tokens.findAll(source).map { it.value }.toSet(),
                tokens.findAll(chinese.getValue(key)).map { it.value }.toSet())
            assertTrue(key, chinese.getValue(key).any { it in '\u4e00'..'\u9fff' })
        }
    }

    @Test
    fun translatesAdbSocketEvidenceWithoutChangingPathsOrErrno() {
        assertEquals("不存在", catalog.translate("Absent"))
        assertEquals("3 项运行状态矛盾", catalog.translate("3 runtime contradiction(s)"))
        assertEquals("存在，但 init 显示已停止", catalog.translate("Present, init reports stopped"))
        assertEquals("存在（安全标签拒绝访问）", catalog.translate("Present (label denied)"))
        val detail = "stat(/dev/socket/adbd) succeeded, confirming the socket path was resolvable. " +
            "listing=readable, directory=ok, socket=ok, control=errno=2"
        assertEquals("stat(/dev/socket/adbd) 成功，确认套接字路径可解析。" +
            "目录列表=可读取，目录=成功，套接字=成功，对照=errno=2", catalog.translate(detail))
        assertEquals("抽象套接字（@jdwp-control）=已连接", catalog.translate("abstract(@jdwp-control)=connected"))
        val contradiction = "adbd's JDWP control socket answered in both samples while init.svc.adbd reported stopped. " +
            "adbd closes that listener when the process init reaps goes away."
        assertEquals("两次采样中 adbd 的 JDWP 控制套接字均有响应，但 init.svc.adbd 显示已停止。" +
            "当 init 回收的进程退出时，adbd 会关闭该监听器。", catalog.translate(contradiction))
    }

    @Test
    fun translatesBoundedNativeMountParagraphIncludingAllEightRanges() {
        val paths = (0..8).map { "/data/user/0/com.example.raw/part$it" }
        val detail = "Window: /dev/cpuset=0:10 .. /mnt/user/0/emulated=0:99; visible unique devices=9. " +
            "Only interior intervals between visible devices are compared. " +
            "Window stops before app-private tmpfs churn; coverage is partial. " +
            "FUSE ceiling: /storage/emulated/0=0:120. " +
            (0..7).joinToString("") { "Gap 0:${12 + it * 3}..0:${13 + it * 3} between ${paths[it]} and ${paths[it + 1]}. " } +
            "Gap runs=10. Only the first eight ranges are shown. " +
            "Overlayfs records=2; layer pseudo-devices cannot be fully accounted for from this view. " +
            "Released IDs, kernel-only allocations and other namespaces can explain gaps. " +
            "These are non-atomic observations, not proof of root or Vector; bind mounts " +
            "reuse a superblock and may leave no additional anonymous device number."
        val translated = catalog.translate(detail)
        assertTrue(translated.startsWith("观察区间：/dev/cpuset=0:10 .. /mnt/user/0/emulated=0:99；"))
        paths.forEach { assertTrue(it, translated.contains(it)) }
        (0..7).forEach { assertTrue(translated.contains("编号空缺 0:${12 + it * 3}..0:${13 + it * 3}。")) }
        assertTrue(translated.contains("FUSE 上界：/storage/emulated/0=0:120。"))
        assertTrue(translated.contains("连续编号空缺共 10 段。仅显示前八个区间。"))
        assertTrue(translated.contains("Overlayfs 记录共 2 条"))
        assertTrue(translated.contains("不能证明存在 Root 或 Vector"))
        for (english in listOf("Window", "Gap ", "Only ", "Released IDs", "coverage is partial", "layer pseudo-devices")) {
            assertFalse(english, translated.contains(english))
        }
    }

    @Test
    fun translatesUnavailableMountAnchorsWithoutInventingAbsence() {
        val detail = "No usable tmpfs/cgroup floor. /tmp: stat errno=2; /dev/cpuset: statfs errno=13; "
        val translated = catalog.translate(detail)
        assertTrue(translated.startsWith("没有可用的 tmpfs/cgroup 下界。"))
        assertTrue(translated.contains("/tmp：stat 失败，errno=2"))
        assertTrue(translated.contains("/dev/cpuset：statfs 失败，errno=13"))
        assertEquals("锚点顺序不可用：/dev/cpuset=0:90，/storage/emulated/0=0:80。",
            catalog.translate("Anchor order is not usable: /dev/cpuset=0:90, /storage/emulated/0=0:80."))
    }

    @Test
    fun translatesRiskWarningsAndOptionalSigningFailure() {
        assertEquals("观察到网络风险指标；背景信息不完整",
            catalog.translate("Network risk indicators observed; context incomplete"))
        assertEquals("观察到缓存的模拟位置", catalog.translate("Cached mock location observed"))
        assertEquals("调用方默认网络；VPN 传输不能证明此应用的请求经过代理。",
            catalog.translate("caller default;  VPN transport does not establish proxying of this app's requests."))
        val prefix = "com.example.proxy; Identity unverified; signature read: Unavailable; "
        for (failure in listOf("", "Access denied.")) {
            val translated = catalog.translate(prefix + failure + " version code: 20261025. Installed does not mean running.")
            assertTrue(translated.contains("com.example.proxy"))
            assertTrue(translated.contains("版本代码：20261025。已安装不代表正在运行。"))
            assertFalse(translated.contains("signature read"))
            assertFalse(translated.contains("Access denied"))
        }
        val summary = strings(File(resourceRoot, "values/sync_20261025_strings.xml"))
            .getValue("runtime_sync25_location_warning")
        assertTrue(catalog.translate(summary).contains("所有检查均不能验证真实位置"))
    }

    @Test
    fun translatesActualTimingExplanationAndMigrationOnlyPositiveSuffix() {
        val path = File(repository, "feature/selinux/presentation/src/main/kotlin/com/eltavine/duckdetector/" +
            "features/selinux/presentation/SelinuxAvcLookupPresentation.kt")
        val section = path.readText().substringAfter("\"A=invalid type").substringBefore("(if (parent.attempted)")
        val source = "A=invalid type" + section.substringBefore('"') +
            Regex("\"([^\"\\n]*)\"").findAll(section.substringAfter('"')).joinToString("") { it.groupValues[1] }
                .replace("\\n", "")
        val translated = catalog.translate(source.trim())
        assertTrue(translated.contains("至少接受了 256 对采样"))
        assertTrue(translated.contains("至少 90% 的采样对中 A 更慢"))
        assertFalse(translated.contains("Incomplete measurements"))
        val suffix = catalog.translate("This parent sample supplies the timing verdict. " +
            "Migration-only partial sample met the 90% A-slower criterion.")
        assertTrue(suffix.contains("此父进程样本用于时序判定"))
        assertTrue(suffix.contains("仅因 CPU 迁移而不完整的样本"))
    }

    @Test
    fun leavesMachineProtocolAndEvidenceValuesUntouched() {
        for (raw in listOf("ANON_DEVICE_STATE=OBSERVED", "INIT_STOPPED_WITH_ADB_JDWP_SOCKET",
            "/dev/socket/adbd", "@jdwp-control", "init.svc.adbd", "0:12..0:13", "com.example.proxy")) {
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
        val items = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(File(resourceRoot, "values/runtime_localization_catalog.xml")).getElementsByTagName("item")
        return RuntimeTextCatalog((0 until items.length).map { index ->
            val key = items.item(index).textContent.substringAfter("@string/")
            english.getValue(key) to (chinese[key] ?: english.getValue(key))
        })
    }

    private fun strings(file: File): Map<String, String> {
        val nodes = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file).getElementsByTagName("string")
        return (0 until nodes.length).associate { index ->
            val node = nodes.item(index)
            node.attributes.getNamedItem("name").nodeValue to node.textContent.replace("\\'", "'")
        }
    }
}
