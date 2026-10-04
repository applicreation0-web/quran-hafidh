package com.applicreation0.quransafeguard

/**
 * Display-only normalization for the approved English Jalalayn corpus.
 *
 * The bundled SQLite and its checksum remain unchanged. These substitutions only
 * expand source honorific shorthand while preserving every surrounding English word.
 */
internal object JalalaynHonorificPresentation {
    private val replacements = listOf(
        "(ṣʿa)" to "ﷺ",
        "(ṣ)" to "ﷺ",
        "(ʿa)" to "عليه السلام"
    )

    fun normalize(source: String): String = replacements.fold(source) { text, (from, to) ->
        text.replace(from, to)
    }
}
