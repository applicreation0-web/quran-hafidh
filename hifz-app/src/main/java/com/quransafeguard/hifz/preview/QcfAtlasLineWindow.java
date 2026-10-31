package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;
import java.util.List;

/** Whole source rows; crop edges fall in the clear gap between complete ink extents. */
final class QcfAtlasLineWindow {
    private QcfAtlasLineWindow(){}

    static double[] bounds(List<WordGeometryRepository.WordBox> words,
        List<GeometryRepository.LineMeta> lines,VerseRef target,int[] marked,double originY,double[] safeCuts){
        if(words==null||lines==null||lines.isEmpty()||target==null||marked==null)return null;
        double[] top=new double[lines.size()],bottom=new double[lines.size()];
        java.util.Arrays.fill(top,Double.POSITIVE_INFINITY);
        java.util.Arrays.fill(bottom,Double.NEGATIVE_INFINITY);
        int first=lines.size(),last=-1;
        double underlineBottom=0;
        for(WordGeometryRepository.WordBox word:words){
            double center=(word.y0+word.y1)/2+originY;
            int row=-1;
            for(int i=0;i<lines.size();i++){
                GeometryRepository.LineMeta line=lines.get(i);
                if(center>=line.top&&center<line.bottom){
                    if(row>=0)return null;
                    row=i;
                }
            }
            if(row<0)return null;
            top[row]=Math.min(top[row],word.y0);
            bottom[row]=Math.max(bottom[row],word.y1);
            if(word.surah==target.getSurah()&&word.ayah==target.getAyah())
                for(int index:marked)if(word.word==index)
                    underlineBottom=Math.max(underlineBottom,Math.min(549,word.y1+1.5)+0.425);
            if(word.surah==target.getSurah()&&word.ayah==target.getAyah()
                &&QcfExcerptWindow.includes(word.word,marked)){
                first=Math.min(first,row);last=Math.max(last,row);
            }
        }
        if(last<0||safeCuts==null||safeCuts.length!=lines.size()+1)return null;
        for(int i=0;i<lines.size();i++)if(!Double.isFinite(top[i])||!Double.isFinite(bottom[i]))return null;
        // Some tall diacritics overlap the next physical row vertically. A rectangular
        // crop cannot separate them safely; retain that complete neighbouring row too.
        while(first>0&&(bottom[first-1]>=top[first]||!Double.isFinite(safeCuts[first])))first--;
        while(last+1<lines.size()&&(bottom[last]>=top[last+1]||!Double.isFinite(safeCuts[last+1])
            ||safeCuts[last+1]<underlineBottom))last++;
        double y0=safeCuts[first];
        double y1=safeCuts[last+1];
        if(!Double.isFinite(y0)||!Double.isFinite(y1)||y0<0||y1>550||y1<=y0
            ||y0>top[first]||y1<bottom[last])return null;
        return new double[]{y0,y1};
    }
}
