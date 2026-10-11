package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;
import java.util.*;

/**
 * Pre-An-Nas planning model: the physical end of the second Mushaf page is a
 * WINDOW boundary, not a promise that either page contains 15 text lines.
 *
 * Four date-slots share the exact eligible consecutive physical lines. Each
 * slot contains independently valid same-surah subblocks. A slot may span a
 * surah transition, but a subblock NEVER does. This class is pure/read-only:
 * no progression credit, cursor movement, preferences or saved-state rewrite.
 *
 * The live engine may adopt a window ONLY with a pinned per-session snapshot
 * and atomic per-subblock completion. Existing OPEN Itqan units must finish
 * under their original immutable plan before this policy is used.
 */
final class ItqanFourSessionPolicy {
    static final int SESSIONS=4;
    static final int MAX_BLOCK_LINES=11;

    static final class Block {
        final int surah;
        final List<String> lineIds;
        Block(int surah,List<String> ids){
            this.surah=surah;
            this.lineIds=Collections.unmodifiableList(new ArrayList<>(ids));
        }
    }
    static final class Session {
        final int index;
        final List<Block> blocks;
        final int lineCount;
        Session(int index,List<Block> blocks,int lineCount){
            this.index=index;
            this.blocks=Collections.unmodifiableList(new ArrayList<>(blocks));
            this.lineCount=lineCount;
        }
    }
    static final class Plan {
        final int firstPage,lastPage;
        final boolean fullWindow;
        final List<Session> sessions;
        Plan(int first,int last,boolean full,List<Session> sessions){
            this.firstPage=first;this.lastPage=last;this.fullWindow=full;
            this.sessions=Collections.unmodifiableList(new ArrayList<>(sessions));
        }
        int lines(){int n=0;for(Session s:sessions)n+=s.lineCount;return n;}
    }
    private ItqanFourSessionPolicy(){}

    /** Get the two-page window from the actual eligible contiguous range.
     *  Gaps, page out-of-order, or a non-eligible line end the window rather
     *  than skipping to silently hit a numerical quota. */
    static Plan fromGeometry(GeometryRepository geometry,int firstLine,
                             Set<String> eligibleLineIds){
        if(geometry==null||eligibleLineIds==null||firstLine<0||firstLine>=geometry.lineCount())
            throw new IllegalArgumentException("Canonical Itqan window required");
        GeometryRepository.LineMeta initial=geometry.line(firstLine);
        if(!eligibleLineIds.contains(initial.id))
            throw new IllegalArgumentException("Itqan cursor line not in eligible corpus");
        int endPage=Math.min(604,initial.page+1);
        ArrayList<GeometryRepository.LineMeta> owned=new ArrayList<>();
        for(int i=firstLine;i<geometry.lineCount();i++){
            GeometryRepository.LineMeta l=geometry.line(i);
            if(l.page>endPage||!eligibleLineIds.contains(l.id))break;
            owned.add(l);
        }
        boolean complete=initial.page<604 && owned.get(owned.size()-1).page==endPage
            && owned.get(owned.size()-1).id.equals(
                last(geometry.lineIdsOnPage(endPage)));
        return plan(owned,complete);
    }
    private static <T>T last(List<T> list){
        if(list.isEmpty())throw new IllegalStateException("Page without physical lines");
        return list.get(list.size()-1);
    }
    /**
     * Global 4-way partition balances physical text lines and prefers a cut
     * at a true verse/surah/page boundary. Different subblocks in one date-slot
     * each get ×40; repeated lines across slots are forbidden.
     */
    static Plan plan(List<GeometryRepository.LineMeta> lines,boolean fullWindow){
        if(lines==null||lines.isEmpty())throw new IllegalArgumentException("No eligible lines");
        LinkedHashSet<String> seen=new LinkedHashSet<>();
        int first=lines.get(0).page,last=first,prevIndex=lines.get(0).globalIndex-1;
        int previousSurah=-1;
        for(GeometryRepository.LineMeta line:lines){
            if(line==null||line.verses==null||line.verses.isEmpty()||
                    !seen.add(line.id)||line.globalIndex!=prevIndex+1||
                    line.page<last||line.page>first+1||line.page>604)
                throw new IllegalArgumentException("Non-consecutive or duplicate QCF line");
            int surah=surahOf(line);
            if(previousSurah>surah)throw new IllegalArgumentException("Reordered surah");
            previousSurah=surah;last=line.page;prevIndex=line.globalIndex;
        }
        int n=lines.size(), slots=Math.min(SESSIONS,n);
        double[][] dp=new double[slots+1][n+1];
        int[][] prev=new int[slots+1][n+1];
        for(int s=0;s<=slots;s++)Arrays.fill(dp[s],Double.POSITIVE_INFINITY);
        dp[0][0]=0;
        double target=(double)n/slots;
        for(int s=1;s<=slots;s++){
            for(int end=s;end<=n-(slots-s);end++){
                for(int start=s-1;start<end;start++){
                    if(!Double.isFinite(dp[s-1][start]))continue;
                    int length=end-start;
                    double score=dp[s-1][start]+(length-target)*(length-target);
                    // Prefer no tiny slot when there is enough real content.
                    if(n>=16&&length<5)score+=12*(5-length);
                    if(end<n) {
                        GeometryRepository.LineMeta a=lines.get(end-1),b=lines.get(end);
                        if(surahOf(a)==surahOf(b)
                            && a.verses.get(a.verses.size()-1).equals(b.verses.get(0)))
                            score+=6; // verse cut: last resort only
                        else score-=1.2;
                        if(a.page!=b.page)score-=0.3;
                    }
                    if(score<dp[s][end]){
                        dp[s][end]=score;prev[s][end]=start;
                    }
                }
            }
        }
        int[] cuts=new int[slots+1];cuts[slots]=n;
        for(int s=slots;s>0;s--)cuts[s-1]=prev[s][cuts[s]];
        ArrayList<Session> result=new ArrayList<>();
        for(int s=0;s<SESSIONS;s++){
            int from=s<slots?cuts[s]:n,to=s<slots?cuts[s+1]:n;
            ArrayList<Block> blocks=new ArrayList<>();
            int i=from;
            while(i<to){
                int surah=surahOf(lines.get(i));
                ArrayList<String> ids=new ArrayList<>();
                while(i<to&&surahOf(lines.get(i))==surah&&ids.size()<MAX_BLOCK_LINES)
                    ids.add(lines.get(i++).id);
                blocks.add(new Block(surah,ids));
            }
            result.add(new Session(s+1,blocks,to-from));
        }
        Plan plan=new Plan(first,last,fullWindow,result);
        LinkedHashSet<String> flattened=new LinkedHashSet<>();
        for(Session s:result)for(Block b:s.blocks) {
            if(b.lineIds.isEmpty()||b.lineIds.size()>MAX_BLOCK_LINES)throw new IllegalStateException("invalid Itqan block");
            for(String id:b.lineIds)if(!flattened.add(id))throw new IllegalStateException("double Itqan credit: "+id);
        }
        if(!flattened.equals(seen)||plan.lines()!=n)throw new IllegalStateException("Itqan coverage gap");
        return plan;
    }
    private static int surahOf(GeometryRepository.LineMeta line){
        int s=line.verses.get(0).getSurah();
        for(VerseRef v:line.verses)if(v.getSurah()!=s)
            throw new IllegalArgumentException("Physical line crosses surah: "+line.id);
        return s;
    }
}
