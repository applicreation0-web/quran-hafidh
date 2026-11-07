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
            int j=0;
            while(j<JUZ_COUNT-1&&ordinal>=start[j+1])j++;
            if(ordinal<start[j])throw new IllegalStateException("QCF line outside canonical Juz");
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
