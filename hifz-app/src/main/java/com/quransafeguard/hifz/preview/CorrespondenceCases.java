package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

/** Offline, read-only index of individually admitted relations.
 * Historical pilots remain fixtures only; conditional dossiers and the frozen
 * 48 exclusions are absent from CorrespondenceCatalogue.ADMITTED.
 */
final class CorrespondenceCases {
    static final class Entry {
        final String id, type, title, mechanism, source, brief;
        final List<VerseRef> verses;
        private final int[][] partners;
        Entry(String id, String type, String title, String mechanism, String source,
              VerseRef... verses) {
            this(id,type,title,mechanism,source,allPartners(verses.length),verses);
        }
        Entry(String id, String type, String title, String mechanism, String source,
              int[][] allowedPartners, VerseRef... verses) {
            this(id,type,title,mechanism,source,mechanism,allowedPartners,verses);
        }
        Entry(String id, String type, String title, String mechanism, String source, String brief,
              int[][] allowedPartners, VerseRef... verses) {
            if(verses.length<2||allowedPartners.length!=verses.length)
                throw new IllegalArgumentException("A relationship needs explicit partners");
            this.id=id;this.type=type;this.title=title;this.mechanism=mechanism;
            this.source=source;this.brief=brief;this.verses=Collections.unmodifiableList(Arrays.asList(verses.clone()));
            partners=new int[verses.length][];
            for(int i=0;i<verses.length;i++){
                if(allowedPartners[i]==null||allowedPartners[i].length==0)
                    throw new IllegalArgumentException("Missing comparison partners");
                partners[i]=allowedPartners[i].clone();
                java.util.Set<Integer> seen=new java.util.HashSet<>();
                for(int target:partners[i])if(target<0||target>=verses.length||target==i||!seen.add(target))
                    throw new IllegalArgumentException("Invalid comparison partner");
            }
        }
        private static int[][] allPartners(int count){
            int[][] result=new int[count][];
            for(int i=0;i<count;i++){
                result[i]=new int[Math.max(0,count-1)];
                for(int n=1;n<count;n++)result[i][n-1]=(i+n)%count;
            }
            return result;
        }
        boolean contains(VerseRef verse){return verse!=null&&verses.contains(verse);}
        int partnerCount(VerseRef source){
            int index=verses.indexOf(source);
            if(index<0)throw new IllegalArgumentException("Unrelated source");
            return partners[index].length;
        }
        VerseRef partner(VerseRef source,int offset){
            int index=verses.indexOf(source);
            if(index<0)throw new IllegalArgumentException("Unrelated source");
            int[] row=partners[index];
            return verses.get(row[Math.floorMod(offset,row.length)]);
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

    static boolean enabled(){return BuildConfig.CORRESPONDENCES_CATALOGUE;}
    /** Stable JUnit fixture; never a production admission switch. */
    static List<Entry> editorialPilotForTest(){return PILOT;}
    static List<Entry> forVerse(VerseRef verse){
        if(!enabled() || verse==null)return Collections.emptyList();
        List<Entry> result=new ArrayList<>();
        for(Entry candidate:CorrespondenceCatalogue.ADMITTED)if(candidate.contains(verse))result.add(candidate);
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
        for(Entry entry:CorrespondenceCatalogue.ADMITTED)if(entry.id.equals(id)&&entry.contains(verse))return entry;
        return null;
    }
    static List<VerseRef> versesOnPage(int page,GeometryRepository geometry){
        if(!enabled()||geometry==null||page<1||page>604)return Collections.emptyList();
        LinkedHashSet<VerseRef> found=new LinkedHashSet<>();
        for(Entry candidate:CorrespondenceCatalogue.ADMITTED)for(VerseRef verse:candidate.verses)
            if(geometry.pageForVerse(verse)==page)found.add(verse);
        return Collections.unmodifiableList(new ArrayList<>(found));
    }
}