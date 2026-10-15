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

    /**
     * Read-only navigation state for the 1,053 canonical passage boundaries.
     * Keeps map/reader location intact without consulting or changing Hifz stores.
     * Titles and licensed commentary remain a separate, fail-closed editorial gate.
     */
    static final class Navigation {
        final MawdudiPassageIndex index;
        final GeometryRepository geometry;
        private MawdudiPassageIndex.Passage current;
        private int page;
        private int mapScrollY;
        private int commentaryScrollY;
        private boolean commentaryOpen;

        Navigation(MawdudiPassageIndex index, GeometryRepository geometry, int surah, int ayah) {
            if (index == null || geometry == null)
                throw new IllegalArgumentException("canonical index and geometry required");
            this.index=index;
            this.geometry=geometry;
            select(index.at(surah,ayah));
        }
        MawdudiPassageIndex.Passage passage() { return current; }
        int page() { return page; }
        int mapScrollY() { return mapScrollY; }
        int commentaryScrollY() { return commentaryScrollY; }
        boolean commentaryOpen() { return commentaryOpen; }
        void setMapScrollY(int y) { mapScrollY=Math.max(0,y); }
        void setCommentaryScrollY(int y) { commentaryScrollY=Math.max(0,y); }
        void setCommentaryOpen(boolean value) { commentaryOpen=value; }
        void selectVerse(int surah,int ayah) { select(index.at(surah,ayah)); }
        void select(MawdudiPassageIndex.Passage passage) {
            if (passage==null || passage.ordinal<0 || passage.ordinal>=index.count()
                || index.all().get(passage.ordinal)!=passage)
                throw new IllegalArgumentException("foreign or missing canonical passage");
            current=passage;
            page=firstPage(passage,geometry);
            mapScrollY=0;
            commentaryScrollY=0;
            commentaryOpen=false;
        }
        /** Passage-level navigation (the QCF page arrows never skip passages). */
        boolean movePassage(int direction) {
            if (direction!=1 && direction!=-1)
                throw new IllegalArgumentException("passage step must be +/-1");
            MawdudiPassageIndex.Passage next = direction>0 ? index.next(current) : index.previous(current);
            if(next==null)return false;
            select(next);
            return true;
        }
        /** QCF navigation stays within the exact selected passage, including its last verse. */
        boolean turnPage(int direction) {
            if (direction!=1 && direction!=-1)
                throw new IllegalArgumentException("page step must be +/-1");
            int next=clampPage(current,page+direction,geometry);
            if(next==page)return false;
            page=next;
            return true;
        }
        List<VerseRef> highlighted() {
            return highlightedVerses(current,page,geometry);
        }
        VerseRef readerJumpVerse() {
            List<VerseRef> selected=highlighted();
            if(selected.isEmpty())throw new IllegalStateException("no QCF verses on passage page");
            return selected.get(0);
        }
        Snapshot snapshot() {
            return new Snapshot(current.id,current.ordinal,page,mapScrollY,
                    commentaryScrollY,commentaryOpen);
        }
        /** Validate the same stable passage ID before restoring any reading position. */
        void restore(Snapshot saved) {
            if(saved==null || saved.ordinal<0 || saved.ordinal>=index.count())
                throw new IllegalArgumentException("unknown saved passage");
            MawdudiPassageIndex.Passage candidate=index.all().get(saved.ordinal);
            if(!candidate.id.equals(saved.id))
                throw new IllegalArgumentException("stale canonical source; refuse silent re-target");
            select(candidate);
            page=clampPage(candidate,saved.page,geometry);
            mapScrollY=Math.max(0,saved.mapScrollY);
            commentaryScrollY=Math.max(0,saved.commentaryScrollY);
            commentaryOpen=saved.commentaryOpen;
        }
    }
    static final class Snapshot {
        final String id;
        final int ordinal,page,mapScrollY,commentaryScrollY;
        final boolean commentaryOpen;
        Snapshot(String id,int ordinal,int page,int mapY,int commentY,boolean open) {
            this.id=id;this.ordinal=ordinal;this.page=page;
            this.mapScrollY=mapY;this.commentaryScrollY=commentY;
            this.commentaryOpen=open;
        }
    }

}
