package com.applicreation0.quransafeguard

/**
 * Pure rendering contract for Qurtubi/Qushayri source rows.
 *
 * verseStart/verseEnd/segment remain provenance only. They are deliberately not
 * rendered as headings or verse numbers. The source-provided English verse
 * translation stays first, followed by the source commentary, with no generated
 * explanatory text in between.
 */
internal data class SourceBackedTafsirSegment(
    val verseStart: Int,
    val verseEnd: Int,
    val segment: Int,
    val translation: String,
    val commentary: String
)

internal fun renderSourceBackedTafsirSegments(
    rows: List<SourceBackedTafsirSegment>
): List<TafsirRun> = buildList {
    rows.forEachIndexed { index, row ->
        if (index > 0) add(TafsirRun(TafsirRunStyle.REGULAR, "\n"))
        if (row.translation.isNotBlank()) {
            add(TafsirRun(TafsirRunStyle.BOLD_ITALIC, row.translation.trim()))
            if (row.commentary.isNotBlank()) {
                add(TafsirRun(TafsirRunStyle.REGULAR, "\n"))
            }
        }
        if (row.commentary.isNotBlank()) {
            add(TafsirRun(TafsirRunStyle.REGULAR, row.commentary.trim()))
        }
    }
}
