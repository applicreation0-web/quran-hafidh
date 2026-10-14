package com.quransafeguard.hifz.preview;

import java.util.List;

/** Canonical QCF 345x550 SVG coordinates. Ambiguous targets FAIL CLOSED. */
final class WordLookupHitTest {
    private WordLookupHitTest() {}

    static final class Box {
        final String key;
        final double left, top, right, bottom;
        Box(String key, double left, double top, double right, double bottom) {
            if(key == null || key.isEmpty() || !Double.isFinite(left)
               || !Double.isFinite(top) || !Double.isFinite(right)
               || !Double.isFinite(bottom) || !(right>left) || !(bottom>top))
                throw new IllegalArgumentException("Invalid Quran word extent");
            this.key=key;
            this.left=left; this.top=top; this.right=right; this.bottom=bottom;
        }
        boolean contains(double x,double y) {
            return x>=left && x<=right && y>=top && y<=bottom;
        }
    }

    /** Zero or multiple enclosing QCF word boxes must never return a guessed gloss. */
    static String exactUnambiguousKey(List<Box> boxes, double x, double y) {
        if(boxes==null || !Double.isFinite(x) || !Double.isFinite(y)
            || x<0 || y<0 || x>345 || y>550)return null;
        String candidate=null;
        for(Box box:boxes){
            if(box!=null && box.contains(x,y)){
                if(candidate!=null)return null;
                candidate=box.key;
            }
        }
        return candidate;
    }
}
