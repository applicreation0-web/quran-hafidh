package com.quransafeguard.hifz.preview;

import android.content.Context;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Draft, title-free, read-only Mawdudi/Ayah Themes canonical navigation.
 * Editorial status is NOT implied by numeric checks. Hifz cursors remain untouched.
 * No fallback to Ibn Kathir or any historical documentary index.
 */
final class MawdudiPassageIndex {
    static final String ASSET = "mawdudi_public/passages_v1.csv";
    static final int EXPECTED_PASSAGES = 1053;
    static final int EXPECTED_AYAT = 6236;
    static final int EXPECTED_ORIGINALS = 1044;
    static final int EXPECTED_ADAPTATIONS = 9;
    static final String ORIGINAL = "AYAH_THEMES_LITERAL";
    static final String ADAPTATION = "EDITORIAL_ADAPTATION_NOT_SOURCE_QUOTE";
    // Independently pinned canonical Hafs surah lengths, not an OCR estimate.
    private static final int[] VERSE_COUNTS = {7,286,200,176,120,165,206,75,129,109,123,111,
        43,52,99,128,111,110,98,135,112,78,118,64,77,227,93,88,69,60,34,30,73,54,
        45,83,182,88,75,85,54,53,89,59,37,35,38,29,18,45,60,49,62,55,78,96,
        29,22,24,13,14,11,11,18,12,12,30,52,52,44,28,28,20,56,40,31,50,40,
        46,42,29,19,36,25,22,17,19,26,30,20,15,21,11,8,8,19,5,8,8,11,11,
        8,3,9,5,4,7,3,6,3,5,4,5,6};

    static final class Passage {
        final String id;
        final int surah;
        final int firstAyah;
        final int lastAyah;
        final int ordinal;
        final boolean editorialAdaptation;
        Passage(String id, int surah, int a, int b, int ordinal, boolean adaptation) {
            this.id=id; this.surah=surah; this.firstAyah=a; this.lastAyah=b;
            this.ordinal=ordinal; this.editorialAdaptation=adaptation;
        }
        boolean contains(int ayah) { return firstAyah<=ayah && ayah<=lastAyah; }
    }

    private final List<Passage> all;
    private final List<List<Passage>> bySurah;
    private MawdudiPassageIndex(List<Passage> entries, List<List<Passage>> bySurah) {
        all=Collections.unmodifiableList(entries);
        ArrayList<List<Passage>> immutable=new ArrayList<>();
        for (List<Passage> group:bySurah)
            immutable.add(Collections.unmodifiableList(group));
        this.bySurah=Collections.unmodifiableList(immutable);
    }

    static MawdudiPassageIndex load(Context context) {
        if (context==null) throw new IllegalArgumentException("context required");
        try (InputStream in=context.getAssets().open(ASSET);
             Reader reader=new InputStreamReader(in,StandardCharsets.UTF_8)) {
            return parse(reader);
        } catch(Exception missingOrInvalid) {
            throw new IllegalStateException("Mawdudi canonical navigation unavailable", missingOrInvalid);
        }
    }

    static MawdudiPassageIndex parse(String csv) {
        if (csv==null) throw new IllegalArgumentException("missing canonical data");
        return parse(new StringReader(csv));
    }

    private static MawdudiPassageIndex parse(Reader source) {
        try {
            BufferedReader br=new BufferedReader(source);
            if (!"id,surah,start,end,origin".equals(br.readLine()))
                throw new IllegalArgumentException("unexpected navigation schema");
            ArrayList<Passage> all=new ArrayList<>();
            ArrayList<List<Passage>> bySurah=new ArrayList<>();
            for(int i=0;i<VERSE_COUNTS.length;i++)bySurah.add(new ArrayList<>());
            int surah=1,ayah=1,ayat=0,originals=0,adaptations=0;
            String line;
            while((line=br.readLine())!=null) {
                String[] fields=line.split(",",-1);
                if(fields.length!=5 || surah>114)
                    throw new IllegalArgumentException("invalid canonical row");
                int s=Integer.parseInt(fields[1]);
                int first=Integer.parseInt(fields[2]),last=Integer.parseInt(fields[3]);
                if(s!=surah || first!=ayah || first<1 || last<first ||
                   last>VERSE_COUNTS[surah-1])
                    throw new IllegalArgumentException("out-of-order/duplicate/missing verse at "+fields[0]);
                String expectedId=String.format(Locale.ROOT,"s%03da%03db%03d",s,first,last);
                if(!expectedId.equals(fields[0]))
                    throw new IllegalArgumentException("invalid stable ID");
                boolean adaptation=ADAPTATION.equals(fields[4]);
                if(!adaptation && !ORIGINAL.equals(fields[4]))
                    throw new IllegalArgumentException("unattributed navigation range");
                Passage item=new Passage(fields[0],s,first,last,all.size(),adaptation);
                all.add(item);bySurah.get(s-1).add(item);
                ayat+=last-first+1;
                if(adaptation)adaptations++;else originals++;
                ayah=last+1;
                if(ayah>VERSE_COUNTS[surah-1]){surah++;ayah=1;}
            }
            if(surah!=115 || ayah!=1 || all.size()!=EXPECTED_PASSAGES ||
               ayat!=EXPECTED_AYAT || originals!=EXPECTED_ORIGINALS ||
               adaptations!=EXPECTED_ADAPTATIONS)
                throw new IllegalArgumentException("incomplete or changed canonical source");
            return new MawdudiPassageIndex(all,bySurah);
        } catch(java.io.IOException io) {
            throw new IllegalArgumentException("unreadable canonical source",io);
        } catch(NumberFormatException malformed) {
            throw new IllegalArgumentException("non-numeric canonical bounds",malformed);
        }
    }

    int count(){return all.size();}
    List<Passage> all(){return all;}
    List<Passage> forSurah(int surah){
        if(surah<1 || surah>114)throw new IllegalArgumentException("surah");
        return bySurah.get(surah-1);
    }
    Passage at(int surah,int ayah){
        if(surah<1 || surah>114 || ayah<1 || ayah>VERSE_COUNTS[surah-1])
            throw new IllegalArgumentException("verse outside canon");
        List<Passage> group=forSurah(surah);
        int lo=0,hi=group.size()-1;
        while(lo<=hi){
            int mid=(lo+hi)>>>1;Passage p=group.get(mid);
            if(ayah<p.firstAyah)hi=mid-1;
            else if(ayah>p.lastAyah)lo=mid+1;
            else return p;
        }
        throw new IllegalStateException("missing canonical verse");
    }
    Passage previous(Passage item){return item==null || item.ordinal<=0?null:all.get(item.ordinal-1);}
    Passage next(Passage item){return item==null || item.ordinal>=all.size()-1?null:all.get(item.ordinal+1);}
}
