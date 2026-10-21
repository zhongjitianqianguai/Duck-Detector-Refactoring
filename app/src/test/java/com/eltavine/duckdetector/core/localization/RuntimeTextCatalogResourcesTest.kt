/*
 * Copyright 2026 Duck Apps Contributor
 * If you have any questions, suggestions, or other inquiries, please email Eltavine <me@eltavine.com>.
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

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Test

class RuntimeTextCatalogResourcesTest {

    @Test
    fun translatesNightlyEmptyInputDigestEvidenceWithoutChangingTheAlgorithmOrHash() {
        val catalog = catalog()
        assertEquals("空输入摘要", catalog.translate("Empty-input digest"))
        assertEquals(
            "这是空输入的 SHA-256 摘要。libavb 对已加载的 vbmeta 镜像求摘要，因此验证启动不会产生该值。",
            catalog.translate(
                "This is the SHA-256 digest of empty input. libavb digests the loaded vbmeta images, " +
                    "so verified boot does not produce it.",
            ),
        )
        val raw = "ro.boot.vbmeta.digest=e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
        assertEquals(raw, catalog.translate(raw))
        assertEquals("com.google.android.gms", catalog.translate("com.google.android.gms"))
        assertEquals("/sdcard/Android/data/profiles", catalog.translate("/sdcard/Android/data/profiles"))
    }

    @Test
    fun translatesDashboardBuildTimeWithoutChangingTheTimestamp() {
        assertEquals(
            "构建时间（UTC）  2026-10-02 03:00:00",
            catalog().translate("Build Time (UTC)  2026-10-02 03:00:00"),
        )
    }

    @Test
    fun translatesTeeDumpHeadingsFromTheActualResourcePairs() {
        assertEquals(
            "=== [GENERATEKEY 事务原子结构转储] ===\n" +
                "--- [回复数据解析] ---\n  [KeyMetadata（密钥元数据）]：\n" +
                "    keySecurityLevel: 1\n  [Keystore2 回复检查]：\n" +
                "    异常：未评估\n--- [回复原始十六进制数据] ---\n00 01 FF",
            catalog().translate(
                "=== [GENERATEKEY transaction atomic structure dump] ===\n" +
                    "--- [Reply Data Parse] ---\n  [KeyMetadata]:\n" +
                    "    keySecurityLevel: 1\n  [Keystore2 reply checks]:\n" +
                    "    anomaly: not evaluated\n--- [Reply Raw Hex] ---\n00 01 FF",
            ),
        )
    }

    @Test
    fun translatesStatusPageFailuresWithoutChangingPathsOrErrno() {
        assertEquals(
            "已跳过：状态页探测结果不确定（打开 /sys/fs/selinux/status 失败（errno=13）。），" +
                "因此未允许 libselinux 读取 /sys/fs/selinux/status。",
            catalog().translate(
                "Skipped: the status page probe was inconclusive " +
                    "(open of /sys/fs/selinux/status failed (errno=13).), " +
                    "so libselinux was not allowed to read /sys/fs/selinux/status.",
            ),
        )
    }

    private fun catalog(): RuntimeTextCatalog {
        val resourceRoot = listOf(File("src/main/res"), File("app/src/main/res"))
            .first { File(it, "values/$ResourceFile").isFile }
        val nightlyKeys = setOf("bootloader_value_empty_input_digest", "bootloader_detail_empty_input_digest")
        val english = strings(File(resourceRoot, "values/$ResourceFile")) +
            strings(File(resourceRoot, "values/detector_strings.xml")).filterKeys { it in nightlyKeys }
        val chinese = strings(File(resourceRoot, "values-zh-rCN/$ResourceFile")) +
            strings(File(resourceRoot, "values-zh-rCN/detector_strings.xml")).filterKeys { it in nightlyKeys }
        val deltaEnglish = DeltaResourceFiles.flatMap { strings(File(resourceRoot, "values/$it")).toList() }.toMap()
        val deltaChinese = DeltaResourceFiles.flatMap { strings(File(resourceRoot, "values-zh-rCN/$it")).toList() }.toMap()
        assertEquals(deltaEnglish.keys, deltaChinese.keys)
        return RuntimeTextCatalog((english + deltaEnglish).map { (name, source) ->
            source to (chinese + deltaChinese).getValue(name)
        })
    }

    @Test
    fun translatesNewDetectorEvidenceWhilePreservingRawProtocolFields() {
        val catalog = catalog()
        assertEquals("ADB 运行状态", catalog.translate("ADB Runtime"))
        assertEquals("Root 管理器", catalog.translate("Root Managers"))
        assertEquals("Zygote 堆残留", catalog.translate("Zygote heap residue"))
        assertEquals(
            "匹配结果只能证明应用已安装，不能证明设备已获取 Root。",
            catalog.translate("A match is evidence an app is installed, not proof that the device is rooted."),
        )
        assertEquals("属性区修改时间", catalog.translate("Prop area mtimes"))
        assertEquals("/data/key 中出现 D-Soter 固定占位公钥",
            catalog.translate("D-Soter's fixed placeholder public key in /data/key"))
        assertEquals("已记录 3 / 9 项", catalog.translate("3 of 9 recorded"))
        assertEquals(
            "Soter 回复需要复核：ASK 中出现 D-Soter 固定占位公钥; AuthKey 中的签名为 256 个零字节；RSA 签名操作不会产生这种结果。",
            catalog.translate("Soter replies need review: D-Soter's fixed placeholder public key in ASK; " +
                "256 zero bytes as the signature in AuthKey, which no RSA signing operation produces."),
        )
        assertEquals("未请求", catalog.translate("Not requested"))
        assertEquals(
            "此承载进程未采集到 AVC 查询实验结果。 在 app_zygote 预加载期间采集；" +
                "这是保留的测量结果，并非实时检查。",
            catalog.translate("No AVC lookup experiment was captured by this carrier. " +
                "Captured during app_zygote preload; this is a retained measurement, not a live check."),
        )
        val raw = "round=1 arm=2 window=17..29 saturated=false"
        assertEquals(raw, catalog.translate(raw))
    }

    @Test
    fun translatesStatusAbiPartialEvidenceAndKeepsRequestIdsAndErrno() {
        val catalog = catalog()
        assertEquals("状态节点 mmap 接口规范检查", catalog.translate("Status mmap ABI conformance"))
        assertEquals(
            "状态节点允许了原版 selinuxfs 会拒绝的请求：两页共享 mmap 映射、可写私有 mmap 映射。" +
                "整组请求未能完成：状态节点 ABI 子进程在完成整组请求前被 SIGSEGV 终止。",
            catalog.translate(
                "The status node granted requests stock selinuxfs rejects: 2-page shared mmap, " +
                    "writable private mmap. The request set did not finish: " +
                    "Status ABI child was killed by SIGSEGV before it finished the request set.",
            ),
        )
        assertEquals(
            "请求 OFFSET_SHARED 被拒绝（errno=13）。",
            catalog.translate("Request OFFSET_SHARED was rejected (errno=13)."),
        )
        assertEquals(
            "状态节点 ABI 子进程未在 1000 毫秒内完成，发出终止信号 200 毫秒后仍未退出，因此未能回收。",
            catalog.translate("Status ABI child did not finish within 1000 ms, " +
                "and was left behind because it had not died 200 ms after it was stopped."),
        )
        val raw = "OUTCOME=VIOLATION\nVIOLATED=TWO_PAGE_SHARED,WRITABLE_PRIVATE\nPAGE_SIZE=4096"
        assertEquals(raw, catalog.translate(raw))
        assertEquals("com.google.android.gms", catalog.translate("com.google.android.gms"))
    }

    @Test
    fun translatesProvisionalLatencyEvidenceWithoutChangingMeasurements() {
        assertEquals(
            "长密钥路径：4.2810 微秒，空密钥：0.4600 微秒，差值：3.8210 微秒，比值：9.31，" +
                "同一 CPU 核心：yes。仅作为时序辅助证据；该比值既不能确认也不能排除 KernelPatch。",
            catalog().translate("Key path: 4.2810 us, empty key: 0.4600 us, diff: 3.8210 us, " +
                "ratio: 9.31, same-core: yes. Supporting timing evidence only; " +
                "this ratio neither identifies nor excludes KernelPatch."),
        )
        assertEquals("比值偏高（辅助证据）", catalog().translate("Elevated ratio (supporting evidence)"))
        assertEquals("比值低于阈值", catalog().translate("Ratio below threshold"))
    }

    @Test
    fun translatesNewExperimentalEvidenceWithoutPromotingItToRootProof() {
        val catalog = catalog()
        assertEquals("init.rc Hook 时序", catalog.translate("init.rc hook timing"))
        assertEquals(
            "实验性辅助证据；既不能确认也不能排除 KernelSU。",
            catalog.translate("Experimental supporting evidence; does not identify or exclude KernelSU."),
        )
        assertEquals("采集状态：COLLECTED；errno=0；CPU=7",
            catalog.translate("Collection: COLLECTED; errno=0; CPU=7"))
        assertEquals("归属包目录位置", catalog.translate("Owner directory coordinates"))
        assertEquals("注册归属包：com.example.owner（身份未验证）",
            catalog.translate("Registry owner: com.example.owner (identity unverified)"))
        assertEquals(
            "挂载 ID 不必连续。启动和运行时重复观察到不连续，也不能证明存在隐藏挂载。",
            catalog.translate("Mount IDs need not be consecutive. Repeated startup/runtime observations do not establish hidden mounts."),
        )
        assertEquals("2 类启发式证据同时出现", catalog.translate("2 heuristic families converged"))
        for (raw in listOf("com.google.android.gms", "me.weishu.kernelsu.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION",
            "ROUND=0\t128\t256\t512\t1024", "/data/user_de/10/com.example.owner")) {
            assertEquals(raw, catalog.translate(raw))
        }
    }

    @Test
    fun translatesSeccompAndHistoryWhileRetainingEvidenceLimits() {
        val catalog = catalog()
        assertEquals("seccomp 对 getpriority 的干扰", catalog.translate("Seccomp getpriority interference"))
        assertEquals(
            "监听器探测受到权限限制：安装监听器被拒绝，返回 errno 13（EACCES）。 内核版本 6.1。",
            catalog.translate("The listener probe was permission limited: " +
                "the listener install was refused with errno 13 (EACCES). Kernel 6.1."),
        )
        assertEquals(
            "本次开机以来的应用包变更历史包含 3 个受监测应用包。目前是否安装或正在运行尚未确认；正常卸载也可能留下相同记录。",
            catalog.translate("This boot's package change history includes 3 monitored package(s). " +
                "Current installation and running state are unconfirmed; normal removal can leave the same evidence."),
        )
        assertEquals(
            "1/2 个目标完成成对调用；1 个目标在调用前、两次调用之间和调用后均通过调用方对照。" +
                "目标结果：超时 2 次、拒绝 0 次、失败 0 次。是否达到批次时限=false。未通过的对照项：none。",
            catalog.translate("1/2 target(s) completed paired calls; 1 passed caller controls before, between and after. " +
                "Target outcomes: 2 timeout(s), 0 denial(s), 0 failure(s). Batch deadline reached=false. Failed controls: none."),
        )
    }

    @Test
    fun translatesComposedHistoryAndContextInsteadOfOnlyTheirFragments() {
        val catalog = catalog()
        assertEquals(
            "本次开机以来的可见变更中出现了 3 个受监测应用包。此列表混合显示新增、更新和移除记录；不提供事件类型和时间。" +
                "返回的变更序号：7。历史记录在重启后重置，也可能受应用包可见性或隐藏模块过滤。它不能证明当前是否安装、是否正在运行或是否存在隐藏行为。",
            catalog.translate("3 monitored package(s) appeared in visible changes since boot. " +
                "Additions, updates and removals share this list; event type and time are not provided. Returned sequence: 7. " +
                "History resets on reboot and may be filtered by package visibility or hiding modules. " +
                "It cannot establish current installation, running state or concealment."),
        )
        assertEquals(
            "受权限限制。调用方 UID 不在普通应用 UID 范围内。 应用包：com.google.android.gms。UID：10123。",
            catalog.translate("Permission limited. Caller is outside the ordinary application UID range. " +
                "Packages: com.google.android.gms. UIDs: 10123."),
        )
        assertEquals(
            "未观察到兼容的运行时服务。 Thanox 事务未被处理。",
            catalog.translate("No compatible runtime service observed. The Thanox transaction was not handled."),
        )
    }

    private fun strings(file: File): Map<String, String> {
        val elements = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(file).getElementsByTagName("string")
        return (0 until elements.length).associate { index ->
            val element = elements.item(index)
            element.attributes.getNamedItem("name").nodeValue to element.textContent.replace("\\'", "'")
        }
    }

    private companion object {
        const val ResourceFile = "sync_20261002_strings.xml"
        val DeltaResourceFiles = listOf("sync_20261013_strings.xml", "sync_20261017_strings.xml", "sync_20261021_strings.xml")
    }
}
