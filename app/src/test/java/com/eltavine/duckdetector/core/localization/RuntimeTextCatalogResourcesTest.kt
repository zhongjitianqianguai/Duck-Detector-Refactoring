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
        val english = strings(File(resourceRoot, "values/$ResourceFile"))
        val chinese = strings(File(resourceRoot, "values-zh-rCN/$ResourceFile"))
        return RuntimeTextCatalog(english.map { (name, source) -> source to chinese.getValue(name) })
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
    }
}
