package com.quransafeguard.hifz.preview;
/** Select source words, never synthesize or reflow Quran text. */
final class QcfExcerptWindow {
    private QcfExcerptWindow(){}
    static boolean includes(int word,int[] marked){
        if(word<1||marked==null)return false;
        for(int index:marked)if(index>0&&Math.abs((long)word-index)<=1)return true;
        return false;
    }
}
