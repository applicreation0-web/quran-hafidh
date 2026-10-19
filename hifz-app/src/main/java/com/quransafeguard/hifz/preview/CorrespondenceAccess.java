package com.quransafeguard.hifz.preview;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import static com.quransafeguard.hifz.preview.CorrespondenceModel.*;

/**
 * On-demand read-only access capability. No screen is wired by this class and no
 * relation is approved here. A caller must supply the already-gated direct index.
 */
final class CorrespondenceAccess {
    static final int DEFAULT_VISIBLE_LIMIT=2;
    enum Surface { LECTURE, ATLAS, SABQI }

    /** Exact return context. The screen mode and reader snapshot stay opaque. */
    static final class Origin {
        final Surface surface;
        final String mode;
        final ReaderSnapshot snapshot;
        final VerseRange workedRange;
        Origin(Surface surface,String mode,ReaderSnapshot snapshot,VerseRange workedRange) {
            require(surface!=null && mode!=null && snapshot!=null && workedRange!=null,"complete read-only origin required");
            this.surface=surface; this.mode=mode; this.snapshot=snapshot; this.workedRange=workedRange;
        }
    }

    /** Only visible contains contextual affordances. Discovery is separate and on demand. */
    static final class Result {
        final Origin origin;
        final List<Relation> visible;
        private final List<Relation> remaining;
        private Result(Origin origin,List<Relation> direct) {
            this.origin=origin;
            int split=Math.min(DEFAULT_VISIBLE_LIMIT,direct.size());
            visible=frozen(direct.subList(0,split));
            remaining=frozen(direct.subList(split,direct.size()));
        }
        boolean hasLinks() { return !visible.isEmpty(); }
        boolean hasOthers() { return !remaining.isEmpty(); }
        List<Relation> others() { return remaining; }
    }

    private final Index index;
    private final Geometry geometry;
    CorrespondenceAccess(Index index,Geometry geometry) {
        require(index!=null && geometry!=null,"approved direct index and geometry required");
        this.index=index; this.geometry=geometry;
    }
    Result at(Origin origin,int surah,int ayah) {
        require(origin!=null,"origin required");
        require(surah>=1 && surah<=114 && ayah>=1 && ayah<=geometry.ayahCount(surah),
            "invalid target verse");
        require(origin.workedRange.contains(surah,ayah),"target outside explicitly worked interval");
        VerseRange original=origin.workedRange;
        VerseRange checked=new VerseRange(original.surah,original.start,original.end,geometry);
        require(checked.firstPage==original.firstPage && checked.lastPage==original.lastPage,
            "worked interval geometry mismatch");
        List<Relation> direct=new ArrayList<>(); Set<String> seen=new LinkedHashSet<>();
        // Query this target once. Never visit a sibling endpoint or expand an interval.
        for(Relation relation:index.at(surah,ayah)) {
            if(!CorrespondenceDocument.displayable(relation,geometry)) continue;
            boolean focal=false;
            for(Anchor anchor:relation.anchors)
                if(anchor.sourceAttested && anchor.range.contains(surah,ayah)) { focal=true; break; }
            if(focal && seen.add(relation.id)) direct.add(relation);
        }
        return new Result(origin,direct);
    }

    private static <T> List<T> frozen(List<T> list) {
        return Collections.unmodifiableList(new ArrayList<>(list));
    }
    private static void require(boolean condition,String message) {
        if(!condition) throw new IllegalArgumentException(message);
    }
}
