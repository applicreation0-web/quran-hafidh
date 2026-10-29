package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Small manually reviewed pilot: no scraped source text, no generated Quran wording,
 * no speculative 30-case catalogue. Only references, concise authored distinctions
 * and source provenance are included. Actual Quran is always rendered from local KFQC.
 */
final class AtlasDiscriminantCases {
    static final class Entry {
        final VerseRef first, second;
        final String kind, firstCue, secondCue, explanation, source, sourceUrl;
        /** Audited one-based Quran word positions; empty until individually checked. */
        final int[] firstWordIndices, secondWordIndices;
        Entry(int s1,int a1,int s2,int a2,String kind,String cue1,String cue2,
              String explanation,String source,String url,int[] words1,int[] words2) {
            first=new VerseRef(s1,a1);second=new VerseRef(s2,a2);
            this.kind=kind;firstCue=cue1;secondCue=cue2;this.explanation=explanation;
            this.source=source;sourceUrl=url;
            firstWordIndices=words1.clone();secondWordIndices=words2.clone();
        }
    }
    private AtlasDiscriminantCases() {}
    private static final int[] NONE = {};
    static final List<Entry> PILOT = Collections.unmodifiableList(Arrays.asList(
        new Entry(30,37,39,52,"Mot remplacé",
            "Ar-Rūm : yaraw (« voir »).",
            "Az-Zumar : yaʿlamū (« savoir »).",
            "Le contexte précédant 39:52 évoque la prétention à un savoir. "
                +"La différence de verbe doit être retenue avec ce contexte, non isolément.",
            "Tadars, comparaison 777 ; Quranpedia, note 935",
            "https://tadars.com/tdbr/comparable/777",new int[]{2},new int[]{2}),
        new Entry(6,151,17,31,"Formulation et ordre",
            "Al-Anʿām : pauvreté déjà présente ; « vous, puis eux ».",
            "Al-Isrāʾ : peur de la pauvreté à venir ; « eux, puis vous ».",
            "La cause exprimée détermine l'ordre du rappel de la subsistance : "
                +"pauvreté actuelle ou pauvreté redoutée.",
            "Ibn Jamāʿa, Kashf al-Maʿānī, question 137",
            "https://www.islamweb.org/ar/library/content/244/134",NONE,NONE),
        new Entry(2,60,7,160,"Verbe remplacé",
            "Al-Baqarah : infajarat — écoulement abondant.",
            "Al-Aʿrāf : inbajasat — jaillissement.",
            "Les deux descriptions évoquent l'eau issue de la pierre. "
                +"Le choix des verbes distingue l'abondance de l'apparition initiale.",
            "Al-Karmānī, Al-Burhān fī Mutashābih al-Qurʾān",
            "https://ablibrary.net/book_content/b/12494/74?lang=ar",NONE,NONE),
        new Entry(2,58,7,161,"Variations multiples",
            "Al-Baqarah : « entrez », puis mangez abondamment.",
            "Al-Aʿrāf : « habitez », puis mangez.",
            "La première formulation insiste sur l'entrée dans la ville ; "
                +"la seconde sur le séjour. Plusieurs autres différences existent dans ces versets.",
            "Fatḥ al-Bayān, commentaire de 7:161",
            "https://tafsir.app/fath-albayan/7/161",NONE,NONE)
    ));

    static int preferredIndex(int surah,int ayah) {
        for(int i=0;i<PILOT.size();i++){
            Entry c=PILOT.get(i);
            if((c.first.getSurah()==surah && c.first.getAyah()==ayah)
                || (c.second.getSurah()==surah && c.second.getAyah()==ayah))return i;
        }
        return 0;
    }
}
