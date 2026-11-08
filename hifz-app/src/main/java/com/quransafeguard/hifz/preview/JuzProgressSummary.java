package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;
import java.util.List;
import java.util.Set;

/**
 * Read-only QCF line accounting across the 30 canonical Juz.
 * Every physical line belongs to exactly one Juz (the Juz of its first verse).
 * A line crossing a boundary cannot be credited twice or silently discarded.
 * Status order is acquired > stabilized > learned > untouched.
 * No planned range, repetition or estimated progress is credited.
 */
final class JuzProgressSummary {
    static final int JUZ_COUNT=30;
    static final int TOTAL=0,LEARNED=1,STABILIZED=2,ACQUIRED=3;
    private JuzProgressSummary(){}

    static int[] boundaries(){
        int[] start=new int[JUZ_COUNT];
        for(int j=0;j<JUZ_COUNT;j++){
            int[] rub=QuranRubBoundaries.rowForIndex(j*8+1);
            start[j]=GeometryRepository.ordinal(new VerseRef(rub[1],rub[2]));
            if(j>0&&start[j]<=start[j-1])
                throw new IllegalStateException("Non-monotone canonical Juz boundary");
        }
        return start;
    }

    static int ownerJuzIndex(int verseOrdinal, int[] starts) {
        int at = java.util.Arrays.binarySearch(starts, verseOrdinal);
        return at >= 0 ? at : -at - 2;
    }

    /** The first QCF line owned by a juz, even when a folio is shared. */
    static int firstOwnedLine(GeometryRepository geometry, int boundaryOrdinal) {
        VerseRef boundary = com.quransafeguard.hifz.core.QuranCanon.INSTANCE.fromOrdinal(boundaryOrdinal);
        int at = geometry.firstLineIndex(boundary);
        while (at < geometry.lineCount()
                && GeometryRepository.ordinal(CorpusLinePolicy.ownerVerse(geometry.line(at))) < boundaryOrdinal) {
            at++;
        }
        return at;
    }

    static int[] pageWindow(GeometryRepository geometry, int juz) {
        if (juz < 1 || juz > JUZ_COUNT) throw new IllegalArgumentException("Juz must be in 1..30");
        int[] starts = boundaries();
        int from = firstOwnedLine(geometry, starts[juz - 1]);
        int until = juz == JUZ_COUNT ? geometry.lineCount() : firstOwnedLine(geometry, starts[juz]);
        return new int[]{geometry.line(from).page, geometry.line(until - 1).page};
    }

    /** counts[juz][TOTAL,LEARNED,STABILIZED,ACQUIRED] */
    static int[][] fromLines(List<GeometryRepository.LineMeta> lines,
                             Set<String> learned,Set<String> stabilized,Set<String> acquired){
        if(lines==null||learned==null||stabilized==null||acquired==null)
            throw new IllegalArgumentException("Missing physical QCF lines or real progression");
        int[][] counts=new int[JUZ_COUNT][4];
        int[] start=boundaries();
        int previous=-1;
        for(GeometryRepository.LineMeta line:lines){
            if(line==null||line.id==null||line.verses==null||line.verses.isEmpty())
                throw new IllegalStateException("Incomplete QCF line metadata");
            int ordinal=GeometryRepository.ordinal(line.verses.get(0));
            if(ordinal<previous)
                throw new IllegalStateException("QCF physical lines not in verse order");
            previous=ordinal;
            int j=ownerJuzIndex(ordinal,start);
            if(j<0)throw new IllegalStateException("QCF line outside canonical Juz");
            counts[j][TOTAL]++;
            if(acquired.contains(line.id))counts[j][ACQUIRED]++;
            else if(stabilized.contains(line.id))counts[j][STABILIZED]++;
            else if(learned.contains(line.id))counts[j][LEARNED]++;
        }
        int checked=0;
        for(int[] c:counts){
            if(c[TOTAL]<=0||c[LEARNED]+c[STABILIZED]+c[ACQUIRED]>c[TOTAL])
                throw new IllegalStateException("Invalid QCF line totals for Juz");
            checked+=c[TOTAL];
        }
        if(checked!=lines.size())throw new IllegalStateException("A QCF line was omitted");
        return counts;
    }


    /**
     * Exact top-to-bottom QCF statuses, one element per real physical line.
     * A miniature Juz is a positional map, NEVER a bottom-anchored percentage.
     * Every line uses the same canonical owner and precedence as fromLines().
     */
    static int[][] statusesFromLines(List<GeometryRepository.LineMeta> lines,
                                    Set<String> learned,Set<String> stabilized,Set<String> acquired){
        int[][] counts=fromLines(lines,learned,stabilized,acquired);
        int[][] statuses=new int[JUZ_COUNT][];
        for(int j=0;j<JUZ_COUNT;j++)statuses[j]=new int[counts[j][TOTAL]];
        int[] cursor=new int[JUZ_COUNT];
        int[] starts=boundaries();
        for(GeometryRepository.LineMeta line:lines){
            int ordinal=GeometryRepository.ordinal(line.verses.get(0));
            int j=ownerJuzIndex(ordinal,starts);
            int state=acquired.contains(line.id)?ProgressGridView.ACQUIS
                :stabilized.contains(line.id)?ProgressGridView.STABILISER
                :learned.contains(line.id)?ProgressGridView.APPRENTISSAGE
                :ProgressGridView.VIDE;
            statuses[j][cursor[j]++]=state;
        }
        for(int j=0;j<JUZ_COUNT;j++){
            if(cursor[j]!=counts[j][TOTAL])
                throw new IllegalStateException("Juz physical line position mismatch");
        }
        return statuses;
    }

    static int[][] lineStatuses(GeometryRepository geometry,HifzPrefs.ProgressionSnapshot snapshot){
        if(geometry==null||snapshot==null)
            throw new IllegalArgumentException("Real V6 Hifz progression required");
        java.util.ArrayList<GeometryRepository.LineMeta> all=new java.util.ArrayList<>();
        for(int i=0;i<geometry.lineCount();i++)all.add(geometry.line(i));
        return statusesFromLines(all,snapshot.learned,snapshot.stabilized,snapshot.acquired);
    }

    static int[][] summarize(GeometryRepository geometry,HifzPrefs.ProgressionSnapshot snapshot){
        if(geometry==null||snapshot==null)
            throw new IllegalArgumentException("Real V6 Hifz progression required");
        java.util.ArrayList<GeometryRepository.LineMeta> all=new java.util.ArrayList<>();
        for(int i=0;i<geometry.lineCount();i++)all.add(geometry.line(i));
        return fromLines(all,snapshot.learned,snapshot.stabilized,snapshot.acquired);
    }

    static int done(int[] c){
        if(c==null||c.length!=4)throw new IllegalArgumentException("Invalid Juz counts");
        return c[LEARNED]+c[STABILIZED]+c[ACQUIRED];
    }
    static int percent(int done,int total){
        if(total<=0||done<0||done>total)
            throw new IllegalArgumentException("Invalid line progress");
        return (int)((long)done*100L/total);
    }
}
