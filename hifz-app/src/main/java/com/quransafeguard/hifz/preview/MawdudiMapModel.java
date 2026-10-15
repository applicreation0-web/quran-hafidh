package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/** Exact QCF location of one canonical Mawdudi navigation passage.
 * No copied/approximated Mushaf text, no editorial title, no Hifz state mutations.
 */
final class MawdudiMapModel {
    private MawdudiMapModel() {}
    static int firstPage(MawdudiPassageIndex.Passage p, GeometryRepository g) {
        if(p==null || g==null)throw new IllegalArgumentException("passage/geometry");
        return g.pageForVerse(new VerseRef(p.surah,p.firstAyah));
    }
    static int lastPage(MawdudiPassageIndex.Passage p, GeometryRepository g) {
        if(p==null || g==null)throw new IllegalArgumentException("passage/geometry");
        // A single ayah may continue onto the next QCF page. pageForVerse()
        // resolves its FIRST page and therefore clips the final page of a
        // passage. Resolve the actual last physical line instead.
        return lastPageForVerse(new VerseRef(p.surah, p.lastAyah), g);
    }
    static int lastPageForVerse(VerseRef last, GeometryRepository g) {
        if (last == null || g == null) throw new IllegalArgumentException("verse/geometry");
        return g.line(g.lastLineIndex(last)).page;
    }
    static int clampPage(MawdudiPassageIndex.Passage p,int desired,GeometryRepository g) {
        return Math.max(firstPage(p,g),Math.min(lastPage(p,g),desired));
    }
    static List<VerseRef> highlightedVerses(MawdudiPassageIndex.Passage p,int page,
                                            GeometryRepository g) {
        if(page<firstPage(p,g) || page>lastPage(p,g))
            return java.util.Collections.emptyList();
        LinkedHashSet<VerseRef> found=new LinkedHashSet<>();
        for(VerseRef v:g.versesOnLines(g.lineIdsOnPage(page)))
            if(v.getSurah()==p.surah && p.contains(v.getAyah())) found.add(v);
        return new ArrayList<>(found);
    }
    /** 0-based printed QCF line of the starting verse, not an invented diagram offset. */
    static int startLineOnFirstPage(MawdudiPassageIndex.Passage p,GeometryRepository g) {
        int line=g.firstLineIndex(new VerseRef(p.surah,p.firstAyah));
        GeometryRepository.LineMeta meta=g.line(line);
        if(meta.page!=firstPage(p,g))throw new IllegalStateException("start line/page mismatch");
        return meta.lineIndexOnPage;
    }
}
