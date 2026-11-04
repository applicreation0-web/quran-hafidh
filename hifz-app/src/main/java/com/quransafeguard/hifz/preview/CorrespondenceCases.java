package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Strictly offline, read-only editorial demonstration.
 * These four cases are HISTORICALLY EDITORIALLY REVIEWED; they are NOT a
 * production-approved correspondence catalogue or a QCF/BOOX certification.
 * Never silently promote the historical 80/24/6 into this index.
 */
final class CorrespondenceCases {
    static final class Entry {
        final String id, type, title, mechanism, source;
        final List<VerseRef> verses;
        Entry(String id, String type, String title, String mechanism, String source,
              VerseRef... verses) {
            if (verses.length < 2) throw new IllegalArgumentException("A relationship needs two positions");
            this.id=id; this.type=type; this.title=title; this.mechanism=mechanism;
            this.source=source; this.verses=Collections.unmodifiableList(Arrays.asList(verses));
        }
        boolean contains(VerseRef verse){return verse != null && verses.contains(verse);}
        VerseRef partner(VerseRef source,int offset) {
            int index=verses.indexOf(source);
            if(index<0) throw new IllegalArgumentException("Unrelated source");
            int shift=1 + Math.floorMod(offset,verses.size()-1);
            return verses.get((index+shift)%verses.size());
        }
    }

    private static VerseRef v(int surah,int ayah) {return new VerseRef(surah,ayah);}
    private static Entry rahman() {
        int[] verses={13,16,18,21,23,25,28,30,32,34,36,38,40,42,45,47,49,51,53,55,
                       57,59,61,63,65,67,69,71,73,75,77};
        VerseRef[] refs=new VerseRef[verses.length];
        for(int i=0;i<verses.length;i++)refs[i]=v(55,verses[i]);
        return new Entry("R048","Répétition","Le refrain d'Ar-Raḥmān",
            "La même formule est répétée 31 fois; ceci ne démontre pas une symétrie globale.",
            "J3, texte arabe croisé; Jalālayn 55:13 (référence historique)",refs);
    }
    private static final List<Entry> PILOT=Collections.unmodifiableList(Arrays.asList(
        new Entry("R030","Répétition","Rappel aux enfants d'Israël",
            "Les versets 2:47 et 2:122 sont textuellement identiques.",
            "J3, comparaison du texte arabe sur deux témoins",v(2,47),v(2,122)),
        new Entry("R037","Progression","La vision de Yūsuf et son accomplissement",
            "12:100 renvoie explicitement à la vision relatée en 12:4.",
            "Jalālayn 12:4 et 12:100; présélection documentaire",v(12,4),v(12,100)),
        new Entry("R047","Répétition","Le refrain d'Al-Qamar",
            "Même verset répété en 54:17, 54:22, 54:32 et 54:40.",
            "J3, comparaison du texte arabe sur deux témoins",v(54,17),v(54,22),v(54,32),v(54,40)),
        rahman()
    ));
    private CorrespondenceCases(){}

    static boolean enabled(){return BuildConfig.CORRESPONDENCES_EDITORIAL_PREVIEW;}
    /** Stable JUnit fixture; never a production admission switch. */
    static List<Entry> editorialPilotForTest(){return PILOT;}
    static List<Entry> forVerse(VerseRef verse){
        if(!enabled() || verse==null)return Collections.emptyList();
        List<Entry> result=new ArrayList<>();
        for(Entry candidate:PILOT)if(candidate.contains(verse))result.add(candidate);
        return Collections.unmodifiableList(result);
    }
    /** Last indexed anchor in traversal order, bounded by the confirmed endpoint. */
    static VerseRef lastValidatedAnchor(List<VerseRef> traversal,VerseRef end){
        if(!enabled()||end==null)return null;
        VerseRef anchor=null;
        for(VerseRef verse:traversal){
            if(!forVerse(verse).isEmpty())anchor=verse;
            if(end.equals(verse))return anchor;
        }
        return null;
    }
    static Entry forIdAndVerse(String id,VerseRef verse){
        if(!enabled()||id==null||verse==null)return null;
        for(Entry entry:PILOT)if(entry.id.equals(id)&&entry.contains(verse))return entry;
        return null;
    }
    static List<VerseRef> versesOnPage(int page,GeometryRepository geometry){
        if(!enabled()||geometry==null||page<1||page>604)return Collections.emptyList();
        LinkedHashSet<VerseRef> found=new LinkedHashSet<>();
        for(Entry candidate:PILOT)for(VerseRef verse:candidate.verses)
            if(geometry.pageForVerse(verse)==page)found.add(verse);
        return Collections.unmodifiableList(new ArrayList<>(found));
    }
}