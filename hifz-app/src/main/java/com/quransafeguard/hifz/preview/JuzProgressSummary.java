package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Read-only percentages by canonical Juz (not 30 arbitrary page chunks).
 * The numerator is verses for which EVERY physical QCF line is genuinely
 * ACQUIRED in schema V6. No planned, learned, repeated or stabilized line is
 * counted as acquired. Canonical rub' boundaries classify a split page exactly
 * by verse, avoiding any artificial extra credit at Juz boundaries.
 */
final class JuzProgressSummary {
    static final int JUZ_COUNT=30;
    private JuzProgressSummary(){}

    static int[][] fromVerses(List<VerseRef> all,Set<VerseRef> fullyCredited){
        int[][] counts=new int[JUZ_COUNT][2]; // [juz][all verses, fully acquired verses]
        int[] start=new int[JUZ_COUNT];
        for(int j=0;j<JUZ_COUNT;j++){
            int[] rub=QuranRubBoundaries.rowForIndex(j*8+1);
            start[j]=GeometryRepository.ordinal(new VerseRef(rub[1],rub[2]));
            if(j>0 && start[j]<=start[j-1])
                throw new IllegalStateException("Non-monotone canonical Juz boundary");
        }
        int j=0;
        for(VerseRef verse:all){
            int ord=GeometryRepository.ordinal(verse);
            while(j<JUZ_COUNT-1 && ord>=start[j+1])j++;
            if(ord<start[j])throw new IllegalStateException("Verse outside canonical Juz start");
            counts[j][0]++;
            if(fullyCredited.contains(verse))counts[j][1]++;
        }
        for(int[] pair:counts){
            if(pair[0]<=0 || pair[1]<0 || pair[1]>pair[0])
                throw new IllegalStateException("Invalid Juz verse counts");
        }
        return counts;
    }
    static int[][] summarize(GeometryRepository geometry,Set<String> realAcquiredLineIds){
        if(geometry==null || realAcquiredLineIds==null)
            throw new IllegalArgumentException("Missing real Hifz progression state");
        Set<VerseRef> proven=new HashSet<>(geometry.allVersesFullyCredited(realAcquiredLineIds));
        return fromVerses(geometry.allVersesInOrder(),proven);
    }
    static int percent(int acquired,int total){
        if(total<=0||acquired<0||acquired>total)
            throw new IllegalArgumentException("Unverifiable Juz completion");
        return (int)((long)acquired*100L/total);
    }
}
