package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.QuranCanon;
import com.quransafeguard.hifz.core.VerseRef;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Read-only adapter to the shipped QCF indexes. Geometry is not a centre/source approval. */
final class CorrespondenceGeometry implements CorrespondenceModel.Geometry {
    /** Page source abstraction shared by the live sidecar and its parsed-file validation. */
    interface WordPages {
        List<WordGeometryRepository.WordBox> forPage(int page);
    }
    private final GeometryRepository geometry;
    private final WordPages words;

    CorrespondenceGeometry(GeometryRepository geometry,WordGeometryRepository words) {
        this(geometry,pageSource(words));
    }
    CorrespondenceGeometry(GeometryRepository geometry,WordPages words) {
        if(geometry==null || words==null) throw new IllegalArgumentException("QCF geometry and word source required");
        this.geometry=geometry; this.words=words;
    }
    private static WordPages pageSource(WordGeometryRepository words) {
        if(words==null) throw new IllegalArgumentException("word geometry required");
        return words::wordsForPage;
    }
    @Override public int ayahCount(int surah) { return QuranCanon.INSTANCE.ayahCount(surah); }
    @Override public int firstPage(int surah,int ayah) {
        return geometry.line(geometry.firstLineIndex(new VerseRef(surah,ayah))).page;
    }
    @Override public int lastPage(int surah,int ayah) {
        return geometry.line(geometry.lastLineIndex(new VerseRef(surah,ayah))).page;
    }
    @Override public boolean hasSegmentGeometry(int surah,int ayah,int firstWord,int lastWord) {
        if(surah<1 || surah>114 || ayah<1 || ayah>ayahCount(surah) ||
            firstWord<1 || lastWord<firstWord) return false;
        try {
            int first=firstPage(surah,ayah),last=lastPage(surah,ayah);
            if(first<1 || last<first || last>604) return false;
            Set<Integer> found=new HashSet<>();
            for(int page=first;page<=last;page++) {
                List<WordGeometryRepository.WordBox> pageWords=words.forPage(page);
                if(pageWords==null) return false;
                for(WordGeometryRepository.WordBox word:pageWords) {
                    if(word==null) return false;
                    if(word.surah!=surah || word.ayah!=ayah ||
                        word.word<firstWord || word.word>lastWord) continue;
                    if(!validBox(word) || !found.add(word.word)) return false;
                }
            }
            // Every requested index must occur exactly once, including middle words.
            // Use long arithmetic to fail closed for enormous/malformed endpoints.
            return found.size()==(long)lastWord-firstWord+1;
        } catch(RuntimeException unavailableGeometry) {
            return false;
        }
    }
    private static boolean validBox(WordGeometryRepository.WordBox word) {
        return finite(word.x0) && finite(word.y0) && finite(word.x1) && finite(word.y1) &&
            word.x0>=0 && word.y0>=0 && word.x1<=345 && word.y1<=550 &&
            word.x1>word.x0 && word.y1>word.y0;
    }
    private static boolean finite(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }
}
