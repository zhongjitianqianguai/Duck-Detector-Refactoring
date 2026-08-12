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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
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
        if (!locale.isSimplifiedChinese()) return text

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

    private fun Locale.isSimplifiedChinese(): Boolean {
        if (!language.equals("zh", ignoreCase = true)) return false
        if (script.equals("Hans", ignoreCase = true)) return true
        if (script.equals("Hant", ignoreCase = true)) return false
        return !country.uppercase(Locale.ROOT).let { region ->
            region == "TW" || region == "HK" || region == "MO"
        }
    }
}

@Composable
fun localizedDisplayText(text: String): String {
    val context = LocalContext.current
    val localeTag = context.resources.configuration.locales[0]?.toLanguageTag().orEmpty()
    return remember(text, localeTag) {
        DisplayTextLocalizer.translate(context, text)
    }
}
