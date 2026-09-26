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

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import com.eltavine.duckdetector.R
import java.util.Locale

object DisplayTextLocalizer {
    @Volatile
    private var simplifiedChineseCatalog: RuntimeTextCatalog? = null

    fun translate(
        context: Context,
        text: String,
    ): String {
        val locale = context.resources.configuration.locales[0] ?: Locale.getDefault()
        if (!locale.prefersSimplifiedChinese()) return text

        val catalog = simplifiedChineseCatalog ?: synchronized(this) {
            simplifiedChineseCatalog ?: loadSimplifiedChineseCatalog(context.applicationContext)
                .also { simplifiedChineseCatalog = it }
        }
        return catalog.translate(text)
    }

    private fun loadSimplifiedChineseCatalog(context: Context): RuntimeTextCatalog {
        val englishContext = context.forLocale(Locale.ENGLISH)
        val chineseContext = context.forLocale(Locale.SIMPLIFIED_CHINESE)
        val english = englishContext.resources.getStringArray(R.array.runtime_localization_catalog)
        val chinese = chineseContext.resources.getStringArray(R.array.runtime_localization_catalog)
        check(english.size == chinese.size) {
            "Runtime localization catalog is misaligned: ${english.size} English entries, ${chinese.size} Chinese entries."
        }
        return RuntimeTextCatalog(english.zip(chinese))
    }

    private fun Context.forLocale(locale: Locale): Context {
        val localizedConfiguration = Configuration(resources.configuration).apply {
            setLocales(LocaleList(locale))
            setLayoutDirection(locale)
        }
        return createConfigurationContext(localizedConfiguration)
    }

}
internal fun Locale.prefersSimplifiedChinese(): Boolean {
    if (LocaleLanguage.parse(language) != LocaleLanguage.CHINESE) return false
    return when (ChineseScript.parse(script)) {
        ChineseScript.SIMPLIFIED -> true
        ChineseScript.TRADITIONAL -> false
        null -> ChineseRegion.parse(country) == null
    }
}

private enum class LocaleLanguage {
    CHINESE;

    companion object {
        private val byCode = mapOf(
            Locale.SIMPLIFIED_CHINESE.language.lowercase(Locale.ROOT) to CHINESE,
        )

        fun parse(value: String): LocaleLanguage? = byCode[value.lowercase(Locale.ROOT)]
    }
}

private enum class ChineseScript {
    SIMPLIFIED,
    TRADITIONAL;

    companion object {
        private val byCode = mapOf(
            "hans" to SIMPLIFIED,
            "hant" to TRADITIONAL,
        )

        fun parse(value: String): ChineseScript? = byCode[value.lowercase(Locale.ROOT)]
    }
}

private enum class ChineseRegion {
    TAIWAN,
    HONG_KONG,
    MACAO;

    companion object {
        private val byCode = mapOf(
            "tw" to TAIWAN,
            "hk" to HONG_KONG,
            "mo" to MACAO,
        )

        fun parse(value: String): ChineseRegion? = byCode[value.lowercase(Locale.ROOT)]
    }
}
