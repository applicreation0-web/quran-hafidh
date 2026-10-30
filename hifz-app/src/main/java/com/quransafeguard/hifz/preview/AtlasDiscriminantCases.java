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
    // One-based lexical positions checked against Quranic Arabic Corpus.
    // Android QCF sidecar geometry and genuine SVG crops are independently gated in CI.
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
            "https://www.islamweb.org/ar/library/content/244/134",new int[]{17,20,21},new int[]{4,7,8}),
        new Entry(2,60,7,160,"Verbe remplacé",
            "Al-Baqarah : infajarat — écoulement abondant.",
            "Al-Aʿrāf : inbajasat — jaillissement.",
            "Les deux descriptions évoquent l'eau issue de la pierre. "
                +"Le choix des verbes distingue l'abondance de l'apparition initiale.",
            "Al-Karmānī, Al-Burhān fī Mutashābih al-Qurʾān",
            "https://ablibrary.net/book_content/b/12494/74?lang=ar",new int[]{9},new int[]{16}),
        new Entry(2,58,7,161,"Variations multiples",
            "Al-Baqarah : « entrez », puis mangez abondamment.",
            "Al-Aʿrāf : « habitez », puis mangez.",
            "La première formulation insiste sur l'entrée dans la ville ; "
                +"la seconde sur le séjour. Plusieurs autres différences existent dans ces versets.",
            "Fatḥ al-Bayān, commentaire de 7:161",
            "https://tafsir.app/fath-albayan/7/161",new int[]{3,6,10},new int[]{4,7}),
        // User-priority intra-surah confusion. Ten identical opening words;
        // the 11th word branches: وَاذْكُرُوا versus وَاسْمَعُوا.
        // Provenance: user's own recurring error, verified against Qur'an 2:63/2:93,
        // not attributed to al-'Abbad's book.
        new Entry(2,63,2,93,"Même début, puis bifurcation",
            "2:63 : وَاذْكُرُوا — et rappelez-vous.",
            "2:93 : وَاسْمَعُوا — et écoutez.",
            "Après خُذُوا مَا آتَيْنَاكُمْ بِقُوَّةٍ, le 11e mot diverge. "
                +"2:63 poursuit avec le rappel du contenu et la piété ; "
                +"2:93 rapporte la réponse « nous avons entendu et désobéi ».",
            "Ajout prioritaire de l'utilisateur ; Coran 2:63 et 2:93",
            "https://quran.com/2/63",new int[]{11},new int[]{11})
    ));

    /** Strict lookup for contextual actions: never show an unrelated comparison.
     * Atlas's original open-from-map flow may still intentionally start at the first pilot. */
    static int indexFor(int surah,int ayah) {
        if(surah<1||surah>114||ayah<1)return -1;
        for(int i=0;i<PILOT.size();i++){
            Entry c=PILOT.get(i);
            if((c.first.getSurah()==surah && c.first.getAyah()==ayah)
                || (c.second.getSurah()==surah && c.second.getAyah()==ayah))return i;
        }
        return -1;
    }
    /** Exact source-backed verse match. Unlisted verses must never advertise a link. */
    static Entry forVerse(int surah,int ayah) {
        for(Entry entry:PILOT) {
            if(entry.first.getSurah()==surah && entry.first.getAyah()==ayah
                ||entry.second.getSurah()==surah && entry.second.getAyah()==ayah)
                return entry;
        }
        return null;
    }

    static int preferredIndex(int surah,int ayah) {
        int exact=indexFor(surah,ayah);
        return exact>=0?exact:0;
    }
}
