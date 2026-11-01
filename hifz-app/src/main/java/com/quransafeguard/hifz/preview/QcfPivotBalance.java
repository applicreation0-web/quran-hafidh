package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

/**
 * Read-only comparison geometry. Both ORIGINAL QCF passages receive the same
 * number of real, sequential words BEFORE and AFTER their reviewed pivot.
 * A missing pivot word uses the approved common-word anchor for location ONLY.
 * Never alters Quran paths, Hifz engines, repetition counters or saved state.
 */
final class QcfPivotBalance {
    private QcfPivotBalance(){}
    private static final int MAX_CONTEXT=2;
    private static final double MAX_FOCUS_WIDTH=265.0;
    private static final double MAX_FOCUS_HEIGHT=92.0;

    static final class Selection {
        final List<WordGeometryRepository.WordBox> words;
        final int before,after,pivotWords;
        final double centerX,centerY,width,height;
        Selection(List<WordGeometryRepository.WordBox> visible,int amount,int pivotWords){
            if(visible.isEmpty())throw new IllegalArgumentException("Empty QCF pivot");
            this.words=Collections.unmodifiableList(new ArrayList<>(visible));
            this.before=amount;this.after=amount;this.pivotWords=pivotWords;
            double minX=Double.POSITIVE_INFINITY,minY=Double.POSITIVE_INFINITY;
            double maxX=Double.NEGATIVE_INFINITY,maxY=Double.NEGATIVE_INFINITY;
            for(WordGeometryRepository.WordBox w:visible){
                minX=Math.min(minX,w.x0);minY=Math.min(minY,w.y0);
                maxX=Math.max(maxX,w.x1);maxY=Math.max(maxY,w.y1);
            }
            centerX=(minX+maxX)/2;centerY=(minY+maxY)/2;
            width=maxX-minX;height=maxY-minY;
        }
        int[] wordNumbers(){
            int[] result=new int[words.size()];
            for(int i=0;i<words.size();i++)result[i]=words.get(i).word;
            return result;
        }
    }

    private static final class Side {
        final TreeMap<Integer,WordGeometryRepository.WordBox> words=new TreeMap<>();
        final int first,last;
        final int availableBefore,availableAfter;
        Side(List<WordGeometryRepository.WordBox> page,VerseRef verse,int[] pivot){
            if(page==null||verse==null||pivot==null||pivot.length==0)
                throw new IllegalArgumentException("Missing original QCF pivot");
            for(WordGeometryRepository.WordBox w:page){
                if(w.surah!=verse.getSurah()||w.ayah!=verse.getAyah())continue;
                if(w.word<1||!Double.isFinite(w.x0)||!Double.isFinite(w.x1)
                        ||!Double.isFinite(w.y0)||!Double.isFinite(w.y1)
                        ||w.x0<0||w.x1>345||w.x0>=w.x1||w.y0<0||w.y1>550||w.y0>=w.y1
                        ||words.put(w.word,w)!=null)
                    throw new IllegalArgumentException("Invalid or duplicate original QCF word");
            }
            if(words.isEmpty())throw new IllegalArgumentException("Missing QCF verse geometry");
            int low=Integer.MAX_VALUE,high=Integer.MIN_VALUE;
            for(int i:pivot){
                if(i<1||!words.containsKey(i))throw new IllegalArgumentException("Unverified QCF pivot word "+i);
                low=Math.min(low,i);high=Math.max(high,i);
            }
            for(int i=low;i<=high;i++)
                if(!words.containsKey(i))throw new IllegalArgumentException("Incomplete pivot phrase");
            first=low;last=high;
            int before=0,after=0;
            while(words.containsKey(first-before-1))before++;
            while(words.containsKey(last+after+1))after++;
            availableBefore=before;availableAfter=after;
        }
        Selection selected(int eachSide){
            if(eachSide<0||eachSide>availableBefore||eachSide>availableAfter)
                throw new IllegalArgumentException("Unbalanced QCF context");
            ArrayList<WordGeometryRepository.WordBox> visible=new ArrayList<>();
            for(int i=first-eachSide;i<=last+eachSide;i++){
                WordGeometryRepository.WordBox w=words.get(i);
                if(w==null)throw new IllegalArgumentException("Missing QCF context word");
                visible.add(w);
            }
            return new Selection(visible,eachSide,last-first+1);
        }
    }

    static Selection[] paired(List<WordGeometryRepository.WordBox> pageLeft,VerseRef left,int[] anchorsLeft,
            List<WordGeometryRepository.WordBox> pageRight,VerseRef right,int[] anchorsRight){
        Side a=new Side(pageLeft,left,anchorsLeft);
        Side b=new Side(pageRight,right,anchorsRight);
        int shared=Math.min(MAX_CONTEXT,
            Math.min(Math.min(a.availableBefore,a.availableAfter),
                Math.min(b.availableBefore,b.availableAfter)));
        // Never shrink ONLY one side to fit a device; reduce both together.
        for(int n=shared;n>=0;n--){
            Selection first=a.selected(n),second=b.selected(n);
            if(n==0||(first.width<=MAX_FOCUS_WIDTH&&second.width<=MAX_FOCUS_WIDTH
                    &&first.height<=MAX_FOCUS_HEIGHT&&second.height<=MAX_FOCUS_HEIGHT)){
                return new Selection[]{first,second};
            }
        }
        throw new IllegalStateException("No verified Quran focus window");
    }

    static Selection single(List<WordGeometryRepository.WordBox> page,VerseRef verse,int[] anchors){
        return paired(page,verse,anchors,page,verse,anchors)[0];
    }
}
