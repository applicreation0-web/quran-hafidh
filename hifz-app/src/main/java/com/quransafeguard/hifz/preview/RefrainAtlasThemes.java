package com.quransafeguard.hifz.preview;

import android.app.Activity;
import com.quransafeguard.hifz.core.VerseRef;
import java.util.Locale;

/**
 * Read-only refrain captions from the SAME canonical Atlas UNIT/PASSAGE titles
 * used in AtlasMapActivity. The arrays below select literal English excerpts;
 * they are not an independent thematic catalogue or a Tafsir attribution.
 * Full titles/ranges/bibliography remain untouched in Atlas private assets.
 */
final class RefrainAtlasThemes {
    private final MawdudiPassageIndex passages;
    private final MawdudiMapData atlas;
    private final AyahThemesCueIndex ayahThemes;

    // Every excerpt below is checked at runtime against the original Atlas
    // UNIT or PASSAGE title containing the exact refrain verse.
    private static final String[] RAHMAN_EXCERPTS = {
        "created man", "Creation", "easts", "oceans", "products", "ships",
        "perish", "tasks", "Allah", "jurisdiction", "run away",
        "Sinners", "punished", "hell", "punished", "Righteous", "gardens",
        "springs", "fruits", "paradise", "Gardens", "Blessings", "Glory",
        "Gardens", "Blessings", "Glory", "Gardens", "Blessings", "Glory",
        "Gardens", "Lord’s Glory"
    };
    private static final String[] MURSALAT_EXCERPTS = {
        "winds", "rain", "angels", "judgement", "disbelievers",
        "hell", "deny", "righteous", "warning", "disbelievers"
    };
    // Proper names: these are Quranic story labels (54:9,18,23,33),
    // NOT a claim that Atlas creates four distinct Ayah Themes passages.
    private static final String[] QAMAR_PEOPLES = {"Nūḥ", "ʿĀd", "Thamud", "Lut"};
    private static final String[] QAMAR_FOLLOWING = {"ʿĀd", "Thamud", "Lut", "Pharaoh"};

    private RefrainAtlasThemes(MawdudiPassageIndex p,MawdudiMapData a,AyahThemesCueIndex t){
        passages=p;atlas=a;ayahThemes=t;
    }
    static RefrainAtlasThemes load(Activity activity) {
        try {
            if(!BuildConfig.ATLAS_PRIVATE_EDITORIAL_PREVIEW) return null;
            MawdudiPassageIndex passages=MawdudiPassageIndex.load(activity);
            AyahThemesCueIndex titles=AyahThemesCueIndex.load(activity);
            if(titles==null) return null;
            MawdudiMapData atlas=MawdudiMapData.load(activity,passages);
            return new RefrainAtlasThemes(passages,atlas,titles);
        } catch(Exception missingOrCorrupt) {
            return null; // Never invent a fallback theme or disclose private data.
        }
    }
    private static int surah(String ref){return Integer.parseInt(ref.split(":")[0]);}
    private static int ayah(String ref){return Integer.parseInt(ref.split(":")[1]);}

    String unitTitle(String ref){
        return atlas.displayUnitTitle(atlas.at(surah(ref),ayah(ref)).id);
    }
    String passageTitle(String ref){
        MawdudiPassageIndex.Passage p=passages.at(surah(ref),ayah(ref));
        return atlas.displayPassageTitle(p.id,ayahThemes.titleForPassage(p.id));
    }
    String passageId(String ref){
        return passages.at(surah(ref),ayah(ref)).id;
    }

    private static boolean contains(String source,String selection){
        return source.toLowerCase(Locale.ROOT).contains(selection.toLowerCase(Locale.ROOT));
    }
    static String selection(String id,int zeroBased){
        if("R048".equals(id))return RAHMAN_EXCERPTS[zeroBased];
        if("R047".equals(id))return QAMAR_PEOPLES[zeroBased];
        if("R059".equals(id))return MURSALAT_EXCERPTS[zeroBased];
        throw new IllegalArgumentException("Unknown refrain");
    }
    /** Exact 1–2 word source excerpt, or Quranic named people for Al-Qamar. */
    String keyword(String id,String[] node){
        int i=Integer.parseInt(node[3])-1;
        String chosen=selection(id,i);
        String fullPassage=passageTitle(node[5]);
        String fullUnit=unitTitle(node[5]);
        if("R047".equals(id) && i==1) {
            // Al-'Ad is named explicitly in 54:18; the Atlas passage 54:9-22
            // groups Noah and 'Ad, while Atlas UNIT 54:18-42 has a broader title.
            if(!"54:22".equals(node[5]))throw new IllegalStateException("Qamar anchor moved");
            return chosen;
        }
        if(!contains(fullPassage,chosen) && !contains(fullUnit,chosen))
            throw new IllegalStateException("Non-Atlas cue at "+node[5]+": "+chosen);
        return chosen;
    }
    /** Short context labels derived from canonical Atlas units, not synthesized. */
    String context(String ref){
        String unit=unitTitle(ref);
        if(contains(unit,"Creation"))return "Creation";
        if(contains(unit,"Revelation"))return "Revelation";
        if(contains(unit,"Reckoning"))return "Reckoning";
        if(contains(unit,"Paradise"))return "Paradise";
        if(contains(unit,"Warning"))return "Warning";
        if(contains(unit,"Resurrection"))return "Resurrection";
        if(contains(unit,"Nations"))return "Nations";
        // If no short exact source excerpt exists, keep the original title.
        return unit;
    }
    String before(String id,String[] node){
        if("R047".equals(id))return QAMAR_PEOPLES[Integer.parseInt(node[3])-1];
        return context(node[8]);
    }
    String after(String id,String[] node){
        if("R047".equals(id))return QAMAR_FOLLOWING[Integer.parseInt(node[3])-1];
        return context(node[11]);
    }
}
