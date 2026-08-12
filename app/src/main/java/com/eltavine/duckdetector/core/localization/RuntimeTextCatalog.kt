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

import java.util.concurrent.ConcurrentHashMap

/**
 * Translates already-formatted detector text without coupling repositories and mappers to Android resources.
 *
 * Detector models intentionally stay locale-neutral so JVM tests and export code can still exercise them without
 * an Android [android.content.Context]. The UI resolves their English display text at the final rendering boundary.
 */
internal class RuntimeTextCatalog(
    pairs: List<Pair<String, String>>,
) {
    private val exactTranslations: Map<String, String>
    private val templateTranslations: List<TemplateTranslation>
    private val cache = ConcurrentHashMap<String, String>()

    init {
        val usefulPairs = pairs
            .asSequence()
            .filter { (source, target) ->
                source.isNotBlank() && target.isNotBlank() && source != target
            }
            .distinctBy { (source, _) -> source }
            .toList()

        exactTranslations = usefulPairs
            .filterNot { (source, _) -> FormatTokenRegex.containsMatchIn(source) }
            .associate { it }

        templateTranslations = usefulPairs
            .filter { (source, _) -> FormatTokenRegex.containsMatchIn(source) }
            .mapNotNull { (source, target) -> TemplateTranslation.create(source, target) }
            .sortedByDescending { it.literalCharacterCount }
    }

    fun translate(value: String): String {
        if (value.isBlank()) return value
        return cache[value] ?: (translateStructured(value) ?: value).also { translated ->
            cache[value] = translated
        }
    }

    private fun translateStructured(
        value: String,
        depth: Int = 0,
    ): String? {
        if (depth > MaximumNestingDepth) return null
        translateAtom(value, depth)?.let { return it }

        val leadingWhitespace = value.takeWhile(Char::isWhitespace)
        val trailingWhitespace = value.takeLastWhile(Char::isWhitespace)
        val trimmed = value.trim()
        if (trimmed != value) {
            translateStructured(trimmed, depth + 1)?.let { translated ->
                return leadingWhitespace + translated + trailingWhitespace
            }
        }

        CompositeDelimiters.forEach { delimiter ->
            if (!value.contains(delimiter.source)) return@forEach
            val parts = value.split(delimiter.source)
            val translatedParts = parts.map { part ->
                translateStructured(part, depth + 1) ?: part
            }
            if (translatedParts != parts) {
                return translatedParts.joinToString(delimiter.target)
            }
        }

        LineSuffixRegex.matchEntire(value)?.let { match ->
            val body = match.groupValues[1]
            val suffix = match.groupValues[2]
            val translatedBody = translateStructured(body, depth + 1)
            if (translatedBody != null) {
                return translatedBody + if (suffix == ".") ChineseFullStop else suffix
            }
        }

        DecoratedHeadingRegex.matchEntire(value)?.let { match ->
            val translatedHeading = translateStructured(match.groupValues[2], depth + 1)
            if (translatedHeading != null) {
                return match.groupValues[1] + translatedHeading + match.groupValues[3]
            }
        }

        TrailingStatusRegex.matchEntire(value)?.let { match ->
            val translatedSubject = translateStructured(match.groupValues[1], depth + 1)
            val translatedStatus = translateStructured(match.groupValues[2], depth + 1)
            if (translatedSubject != null || translatedStatus != null) {
                return (translatedSubject ?: match.groupValues[1]) + " " +
                    (translatedStatus ?: match.groupValues[2])
            }
        }

        BulletPrefixRegex.matchEntire(value)?.let { match ->
            val translatedBody = translateStructured(match.groupValues[2], depth + 1)
            if (translatedBody != null) {
                return match.groupValues[1] + translatedBody
            }
        }

        BracketPrefixRegex.matchEntire(value)?.let { match ->
            val translatedBadge = translateStructured(match.groupValues[1], depth + 1)
            val translatedBody = translateStructured(match.groupValues[2], depth + 1)
            if (translatedBadge != null || translatedBody != null) {
                return buildString {
                    append('[')
                    append(translatedBadge ?: match.groupValues[1])
                    append("] ")
                    append(translatedBody ?: match.groupValues[2])
                }
            }
        }

        ColonSeparatedRegex.matchEntire(value)?.let { match ->
            val label = match.groupValues[1].trimEnd()
            val detail = match.groupValues[2].trimStart()
            val translatedLabel = translateStructured(label, depth + 1)
            val translatedDetail = translateStructured(detail, depth + 1)
            if (translatedLabel != null || translatedDetail != null) {
                return buildString {
                    append(translatedLabel ?: label)
                    append(ChineseColonDelimiter)
                    append(translatedDetail ?: detail)
                }
            }
        }

        EqualsSeparatedRegex.matchEntire(value)?.let { match ->
            val label = match.groupValues[1].trimEnd()
            val detail = match.groupValues[2].trimStart()
            val translatedLabel = translateStructured(label, depth + 1)
            val translatedDetail = translateStructured(detail, depth + 1)
            if (translatedLabel != null || translatedDetail != null) {
                return (translatedLabel ?: label) + "=" + (translatedDetail ?: detail)
            }
        }

        return null
    }

    private fun translateAtom(
        value: String,
        depth: Int,
    ): String? {
        exactTranslations[value]?.let { return it }
        templateTranslations.forEach { template ->
            template.translate(value) { argument ->
                translateStructured(argument, depth + 1) ?: argument
            }?.let { return it }
        }
        return null
    }

    private data class TemplateTranslation(
        val sourceRegex: Regex,
        val captureIndices: List<Int>,
        val targetTemplate: String,
        val literalCharacterCount: Int,
    ) {
        fun translate(
            value: String,
            localizeArgument: (String) -> String,
        ): String? {
            val match = sourceRegex.matchEntire(value) ?: return null
            val valuesByIndex = mutableMapOf<Int, String>()
            captureIndices.forEachIndexed { capturePosition, argumentIndex ->
                valuesByIndex.putIfAbsent(argumentIndex, match.groupValues[capturePosition + 1])
            }

            var implicitTargetIndex = 1
            return FormatTokenRegex.replace(targetTemplate) { token ->
                val explicitIndex = token.groups[1]?.value?.toIntOrNull()
                val argumentIndex = explicitIndex ?: implicitTargetIndex++
                valuesByIndex[argumentIndex]?.let(localizeArgument) ?: token.value
            }
        }

        companion object {
            fun create(source: String, target: String): TemplateTranslation? {
                val matches = FormatTokenRegex.findAll(source).toList()
                if (matches.isEmpty()) return null

                val captureIndices = mutableListOf<Int>()
                var implicitSourceIndex = 1
                var cursor = 0
                val regexPattern = buildString {
                    append('^')
                    matches.forEach { token ->
                        append(Regex.escape(source.substring(cursor, token.range.first)))
                        val explicitIndex = token.groups[1]?.value?.toIntOrNull()
                        captureIndices += explicitIndex ?: implicitSourceIndex++
                        append(if (token.groups[2]?.value == "d") NumberCapture else TextCapture)
                        cursor = token.range.last + 1
                    }
                    append(Regex.escape(source.substring(cursor)))
                    append('$')
                }

                return TemplateTranslation(
                    sourceRegex = Regex(regexPattern),
                    captureIndices = captureIndices,
                    targetTemplate = target,
                    literalCharacterCount = FormatTokenRegex.replace(source, "").length,
                )
            }
        }
    }

    private companion object {
        val FormatTokenRegex = Regex("%(?:(\\d+)\\$)?([ds])")
        const val NumberCapture = "(-?\\d+)"
        const val TextCapture = "(.+?)"
        const val ChineseColonDelimiter = "："
        const val ChineseFullStop = "。"
        const val MaximumNestingDepth = 8
        val CompositeDelimiters = listOf(
            CompositeDelimiter("\n"),
            CompositeDelimiter(" · "),
            CompositeDelimiter(" | "),
            CompositeDelimiter(" — "),
            CompositeDelimiter(", ", "、"),
            CompositeDelimiter(" and ", " 和 "),
        )
        val LineSuffixRegex = Regex("^(.+?)([.:])$")
        val DecoratedHeadingRegex = Regex("^(\\s*-{3,}\\s*)(.+?)(\\s*-{3,}\\s*)$")
        val TrailingStatusRegex = Regex("^(.+?)\\s+(skipped|failed|unavailable|available|clean|matched|ok)$")
        val BulletPrefixRegex = Regex("^([•-]\\s+)(.+)$")
        val BracketPrefixRegex = Regex("^\\[([^]]+)]\\s+(.+)$")
        val ColonSeparatedRegex = Regex("^(.+?)\\s*:\\s+(.+)$")
        val EqualsSeparatedRegex = Regex("^(.+?)\\s*=\\s*(.+)$")
    }

    private data class CompositeDelimiter(
        val source: String,
        val target: String = source,
    )
}
