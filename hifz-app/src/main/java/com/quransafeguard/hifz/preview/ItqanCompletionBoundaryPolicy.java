package com.quransafeguard.hifz.preview;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Decides whether immediately following, unconsumed lines from the next Itqan
 * sub-block ACTUALLY close the physical Mushaf page or the current surah.
 * Never borrows a line from another surah or across a physical page boundary.
 */
final class ItqanCompletionBoundaryPolicy {
    private ItqanCompletionBoundaryPolicy() {}

    private static boolean onlySurah(GeometryRepository.LineMeta line,int surah) {
        if(line==null || line.verses.isEmpty())return false;
        for(com.quransafeguard.hifz.core.VerseRef ref:line.verses)
            if(ref.getSurah()!=surah)return false;
        return true;
    }

    static ItqanPageCompletionPolicy.Offer offer(GeometryRepository geometry,
            List<String> ownLineIds,List<String> nextSubblockLineIds) {
        if(geometry==null || ownLineIds==null || ownLineIds.isEmpty())
            throw new IllegalArgumentException("actual Itqan sub-block required");
        List<GeometryRepository.LineMeta> own=geometry.linesForExactIds(ownLineIds);
        if(nextSubblockLineIds==null || nextSubblockLineIds.size()<=1)
            return ItqanPageCompletionPolicy.evaluate(ownLineIds,own,Collections.emptyList());
        List<GeometryRepository.LineMeta> next=geometry.linesForExactIds(nextSubblockLineIds);
        GeometryRepository.LineMeta end=own.get(own.size()-1);
        if(end.verses.isEmpty())return ItqanPageCompletionPolicy.evaluate(
            ownLineIds,own,Collections.emptyList());
        int surah=end.verses.get(0).getSurah();
        if(!onlySurah(end,surah))return ItqanPageCompletionPolicy.evaluate(
            ownLineIds,own,Collections.emptyList());
        List<String> pageLineIds=geometry.lineIdsOnPage(end.page);
        if(pageLineIds.isEmpty())return ItqanPageCompletionPolicy.evaluate(
            ownLineIds,own,Collections.emptyList());
        List<GeometryRepository.LineMeta> pageLines=geometry.linesForExactIds(pageLineIds);
        int pageFinal=pageLines.get(pageLines.size()-1).globalIndex;
        int maxTake=Math.min(2,next.size()-1); // never erase all of the next sub-block
        ArrayList<GeometryRepository.LineMeta> trailing=new ArrayList<>();
        int lastIndex=end.globalIndex;
        for(int i=0;i<maxTake;i++) {
            GeometryRepository.LineMeta candidate=next.get(i);
            if(candidate.globalIndex!=lastIndex+1 || candidate.page!=end.page
                    || !onlySurah(candidate,surah))break;
            trailing.add(candidate);
            lastIndex=candidate.globalIndex;
            boolean endOfPhysicalPage=lastIndex==pageFinal;
            boolean endOfSurah=lastIndex+1==geometry.lineCount()
                || !onlySurah(geometry.line(lastIndex+1),surah);
            if(endOfPhysicalPage || endOfSurah)
                return ItqanPageCompletionPolicy.evaluate(ownLineIds,own,trailing);
        }
        return ItqanPageCompletionPolicy.evaluate(ownLineIds,own,Collections.emptyList());
    }
}
