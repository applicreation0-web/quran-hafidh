package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * The reader's right-gutter Hizb/Rubʿ écusson (spec UI pass 2, §28). Only a canonical boundary
 * that really starts on the page (QuranRubBoundaries, 240 audited rows, at most one per page)
 * produces a mark, placed at the exact height where its starting verse begins: the verse's first
 * real word (quran-ws box) when available, otherwise its own canonical line band. No boundary,
 * or a boundary whose verse cannot be located on that page, means no mark (fail-closed).
 */
final class RubGutterMark {
    private RubGutterMark() {}

    /** {position 0..3, hizb, verse, top, bottom, wordBox?} or null. */
    static JSONObject forPage(GeometryRepository geometry, JSONArray verseWordBoxes, int page) {
        int[] row = QuranRubBoundaries.boundaryOnPage(page);
        if (row == null || geometry == null) return null;
        VerseRef verse = new VerseRef(row[1], row[2]);
        GeometryRepository.LineMeta line;
        try {
            line = geometry.line(geometry.firstLineIndex(verse));
        } catch (RuntimeException absent) {
            return null;
        }
        if (line.page != page) return null;
        try {
            JSONObject mark = new JSONObject()
                .put("position", row[5])
                .put("hizb", row[4])
                .put("verse", verse.toString())
                .put("top", line.top)
                .put("bottom", line.bottom);
            JSONArray first = verseWordBoxes == null ? null : verseWordBoxes.optJSONArray(0);
            if (first != null && first.length() == 4) mark.put("wordBox", first);
            return mark;
        } catch (JSONException error) {
            return null;
        }
    }
}
