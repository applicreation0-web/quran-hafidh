package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;
import java.time.LocalDate;

/** Earn the actual frozen Sabqi QCF protocol (legacy ×37 or new ×45). */
final class AndroidSabqiProofFixture11712 {
    private AndroidSabqiProofFixture11712(){}
    static GeometryRepository.FiveLineBlock earn(HifzPrefs prefs,GeometryRepository geometry,LocalDate date){
        int start=prefs.sabqiLineCursor();
        if(start<0){start=geometry.firstLineIndex(prefs.sabqiStart());prefs.setSabqiLineCursor(start);}
        GeometryRepository.FiveLineBlock block=geometry.fiveLineBlock(start,geometry.lastLineIndex(prefs.sabqiEnd()));
        if(!date.equals(prefs.openSabqiLesson(geometry,date)))
            throw new AssertionError("Original physical lesson date must be frozen");
        String token=prefs.sabqiLessonToken();
        if(token.isEmpty())throw new AssertionError("Physical Sabqi lesson token missing");
        for(int rep=0;rep<prefs.sabqiLessonTargetReps();rep++)
            if(!prefs.recordSabqiRepetition(token,rep,false))
                throw new AssertionError("Unverified Sabqi repetition "+rep);
        return block;
    }
    static boolean complete(HifzPrefs prefs,GeometryRepository geometry,LocalDate date,String label){
        GeometryRepository.FiveLineBlock block=earn(prefs,geometry,date);
        return prefs.completeSabqiBlockV6(block.startLineIndex,block.endLineIndex,
            block.endLineIndex+1,block.lineIds,date.toString(),label);
    }
}
