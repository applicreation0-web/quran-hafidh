package com.quransafeguard.hifz.preview;

import java.util.*;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Synthetic presentation metadata only. No private source titles or Hifz state. */
public final class AtlasDisplayCatalogTest {
    private static final String U="s005a011b026", P="s005a007b011";
    private static Map<String,int[]> units() {
        Map<String,int[]> m=new LinkedHashMap<>(); m.put(U,new int[]{5,11,26}); return m;
    }
    private static Map<String,int[]> passages() {
        Map<String,int[]> m=new LinkedHashMap<>(); m.put(P,new int[]{5,7,11}); return m;
    }
    private static JSONObject entry(String level,String id,int s,int a,int b)throws Exception {
        return new JSONObject().put("level",level).put("id",id).put("surah",s)
            .put("start",a).put("end",b).put("title","Synthetic approved label")
            .put("origin","ORIGINAL_EDITORIAL_SUMMARY")
            .put("source_ref","Synthetic work").put("edition","Synthetic edition")
            .put("locator","Verse "+s+":"+a).put("review_id","synthetic-review-1")
            .put("user_approved",true).put("source_verified",true)
            .put("independent_verified",true).put("redistributable",true);
    }
    private static String pack(JSONArray rows)throws Exception {
        return new JSONObject().put("schema","HAAFIDH_DISPLAY_CATALOG_V1")
            .put("revision",1).put("entries",rows).toString();
    }
    private static AtlasDisplayCatalog parse(JSONArray rows)throws Exception {
        return AtlasDisplayCatalog.parse(pack(rows),units(),passages());
    }
    @Test public void editorialApprovalSurvivesWithoutSourceCertificationOrRuntimeRights()throws Exception {
        for(String gate:new String[]{"source_verified","independent_verified","redistributable"}) {
            AtlasDisplayCatalog c=parse(new JSONArray().put(entry("UNIT",U,5,11,26).put(gate,false)));
            assertEquals(gate,"Synthetic approved label",c.approvedUnits.get(U));
            assertTrue(c.units.isEmpty());
            assertFalse(c.unitTitle(U,1).equals("Synthetic approved label"));
        }
    }
    @Test public void originalPassageHeadingCanExceedEditorialSummaryLimitWithoutTruncation()throws Exception {
        char[] letters=new char[302];Arrays.fill(letters,'x');String exact=new String(letters);
        JSONObject e=entry("PASSAGE",P,5,7,11).put("origin","ORIGINAL_AYAH_THEMES_TITLE")
            .put("title",exact).put("redistributable",false);
        AtlasDisplayCatalog c=parse(new JSONArray().put(e));
        assertEquals(exact,c.approvedPassages.get(P));assertTrue(c.passages.isEmpty());
        reject(new JSONArray().put(entry("PASSAGE",P,5,7,11).put("title",exact)));
        char[] oversized=new char[1001];Arrays.fill(oversized,'x');
        reject(new JSONArray().put(e.put("title",new String(oversized))));
    }
    @Test public void originalPassageTitlesRemainDistinctFromEditorialSummaries()throws Exception {
        JSONObject e=entry("PASSAGE",P,5,7,11).put("origin","ORIGINAL_AYAH_THEMES_TITLE")
            .put("source_verified",false).put("independent_verified",false).put("redistributable",false);
        AtlasDisplayCatalog c=parse(new JSONArray().put(e));
        assertTrue(c.passages.isEmpty());
        assertEquals("Admissible source",c.passageTitle(P,"Admissible source"));
        reject(new JSONArray().put(entry("UNIT",U,5,11,26).put("origin","ORIGINAL_AYAH_THEMES_TITLE")));
    }
    @Test public void canonicalMacroIdentityIsStableAndExplicitMismatchFails() {
        assertEquals(U,new MawdudiMapData.Macro(5,11,26,"Synthetic").id);
        assertEquals(U,new MawdudiMapData.Macro(U,5,11,26,"Synthetic").id);
        try { new MawdudiMapData.Macro(P,5,11,26,"Synthetic");fail("Changed unit identity accepted"); }
        catch(IllegalArgumentException expected) {}
    }
    @Test public void unapprovedWordingIsNotAnEditorialApprovalAndApprovedMapsAreImmutable()throws Exception {
        AtlasDisplayCatalog c=parse(new JSONArray().put(entry("UNIT",U,5,11,26).put("user_approved",false)));
        assertTrue(c.approvedUnits.isEmpty());
        c=parse(new JSONArray().put(entry("UNIT",U,5,11,26).put("source_verified",false)));
        try { c.approvedUnits.put(U,"Replacement");fail("Editorial approval mutable"); }
        catch(UnsupportedOperationException expected) {}
    }
    @Test public void absentEntriesAreNeutralNotAnInvalidFixedCount()throws Exception {
        AtlasDisplayCatalog c=parse(new JSONArray());
        assertTrue(c.units.isEmpty());assertTrue(c.passages.isEmpty());assertTrue(c.contexts.isEmpty());
    }
    @Test public void supports120And366UnitsAnd1053PassagesWithoutHistoricalQuota()throws Exception {
        int[] lengths={7,286,200,176,120,165,206};
        for(int count:new int[]{120,366,1053}) {
            String level=count==1053?"PASSAGE":"UNIT";
            Map<String,int[]> source=new LinkedHashMap<>();JSONArray entries=new JSONArray();
            int remaining=count;
            for(int s=1;s<=lengths.length&&remaining>0;s++) for(int a=1;a<=lengths[s-1]&&remaining>0;a++,remaining--) {
                String id=String.format(Locale.ROOT,"s%03da%03db%03d",s,a,a);
                source.put(id,new int[]{s,a,a});entries.put(entry(level,id,s,a,a));
            }
            AtlasDisplayCatalog c=AtlasDisplayCatalog.parse(pack(entries),level.equals("UNIT")?source:units(),level.equals("PASSAGE")?source:passages());
            assertEquals(count,level.equals("UNIT")?c.units.size():c.passages.size());
        }
    }
    @Test public void aMissingSourceIndependentUserOrRightsGateCannotExposeCandidate()throws Exception {
        for(String gate:new String[]{"user_approved","source_verified","independent_verified","redistributable"}) {
            JSONObject e=entry("UNIT",U,5,11,26).put(gate,false);
            assertTrue(gate,parse(new JSONArray().put(e)).units.isEmpty());
        }
    }
    @Test public void local511CaptionNeverReplacesTheFullCanonicalPassage()throws Exception {
        JSONObject e=entry("CONTEXT",P,5,11,11).put("unit_id",U);
        AtlasDisplayCatalog c=parse(new JSONArray().put(e));
        assertEquals("Synthetic approved label",c.contextTitle(P,U));
        assertTrue(c.passages.isEmpty());assertTrue(c.units.isEmpty());
    }
    @Test public void oneParentPassageCanHaveThreeDistinctLocalCaptions()throws Exception {
        String parent="s051a001b023";Map<String,int[]> us=new LinkedHashMap<>(),ps=new LinkedHashMap<>();
        ps.put(parent,new int[]{51,1,23});JSONArray rows=new JSONArray();
        for(int[] bounds:new int[][]{{1,14},{15,19},{20,23}}) {
            String uid=String.format(Locale.ROOT,"s051a%03db%03d",bounds[0],bounds[1]);
            us.put(uid,new int[]{51,bounds[0],bounds[1]});
            rows.put(entry("CONTEXT",parent,51,bounds[0],bounds[1]).put("unit_id",uid));
        }
        AtlasDisplayCatalog c=AtlasDisplayCatalog.parse(pack(rows),us,ps);
        assertEquals(3,c.contexts.size());assertTrue(c.passages.isEmpty());
    }
    @Test public void duplicateOrChangedCanonicalIdentityFailsEvenWhenUnapproved()throws Exception {
        JSONObject e=entry("UNIT",U,5,11,26);
        reject(new JSONArray().put(e).put(new JSONObject(e.toString()).put("user_approved",false)));
        reject(new JSONArray().put(entry("UNIT",U,5,12,26)));
        reject(new JSONArray().put(entry("PASSAGE",P,5,11,11)));
        reject(new JSONArray().put(entry("UNIT","s005a012b026",5,12,26)));
    }
    @Test public void contextMustEqualExactIntersectionAndHaveBothParents()throws Exception {
        reject(new JSONArray().put(entry("CONTEXT",P,5,7,11).put("unit_id",U)));
        reject(new JSONArray().put(entry("CONTEXT",P,5,11,11).put("unit_id","missing")));
        reject(new JSONArray().put(entry("CONTEXT",P,5,11,11)));
    }
    @Test public void publishedEntriesNeedTraceableProvenanceAndNoProtectedQuote()throws Exception {
        for(String key:new String[]{"source_ref","edition","locator","review_id"})
            reject(new JSONArray().put(entry("UNIT",U,5,11,26).put(key,"")));
        reject(new JSONArray().put(entry("UNIT",U,5,11,26).put("origin","SOURCE_QUOTE")));
        reject(new JSONArray().put(entry("UNIT",U,5,11,26).put("title","line1\nline2")));
    }
    @Test public void levelSeparatesUnitAndPassageWithTheSameStableId()throws Exception {
        Map<String,int[]> index=Collections.singletonMap("s001a001b007",new int[]{1,1,7});
        JSONArray entries=new JSONArray().put(entry("UNIT","s001a001b007",1,1,7))
            .put(entry("PASSAGE","s001a001b007",1,1,7));
        AtlasDisplayCatalog c=AtlasDisplayCatalog.parse(pack(entries),index,index);
        assertEquals(1,c.units.size());assertEquals(1,c.passages.size());
    }
    @Test public void approvalFlagsMustBeActualBooleansNotCoercedStrings()throws Exception {
        reject(new JSONArray().put(entry("UNIT",U,5,11,26).put("independent_verified","true")));
    }
    @Test public void returnedTitlesCannotBeMutatedAfterValidation()throws Exception {
        AtlasDisplayCatalog c=parse(new JSONArray().put(entry("UNIT",U,5,11,26)));
        try{c.units.put(U,"Unreviewed replacement");fail("Mutable catalogue");}
        catch(UnsupportedOperationException expected){}
    }
    @Test public void canonicalCoordinatesCannotBeRoundedOrCoercedFromStrings()throws Exception {
        reject(new JSONArray().put(entry("UNIT",U,5,11,26).put("surah",5.5)));
        reject(new JSONArray().put(entry("UNIT",U,5,11,26).put("start","11")));
    }
    @Test public void androidStringCoercionCannotFabricateTitleOrProvenance()throws Exception {
        for(Object value:new Object[]{JSONObject.NULL,Boolean.TRUE,new JSONObject()}) {
            reject(new JSONArray().put(entry("UNIT",U,5,11,26).put("title",value)));
            for(String key:new String[]{"source_ref","edition","locator","review_id"})
                reject(new JSONArray().put(entry("UNIT",U,5,11,26).put(key,value)));
        }
    }
    private static final class Assets implements AtlasDisplayCatalog.AssetSource {
        final Map<String,byte[]> files=new HashMap<>();
        boolean denyOpen;
        public String[] list(String directory) { return files.isEmpty()?new String[0]:new String[]{"atlas_display_catalog_v1.json"}; }
        public java.io.InputStream open(String path)throws java.io.IOException {
            if(denyOpen)throw new java.io.FileNotFoundException("present but unreadable");
            return new java.io.ByteArrayInputStream(files.get(path));
        }
        Assets present(String raw) { files.put("atlas/atlas_display_catalog_v1.json",raw.getBytes(java.nio.charset.StandardCharsets.UTF_8));return this; }
    }
    @Test public void absentOptionalAssetPreservesLegacySelection()throws Exception {
        assertNull(AtlasDisplayCatalog.readOptional(new Assets(),units(),passages()));
    }
    @Test public void presentOptionalAssetIsOrdinaryVersionedMetadata()throws Exception {
        Assets assets=new Assets().present(pack(new JSONArray().put(entry("UNIT",U,5,11,26))));
        AtlasDisplayCatalog c=AtlasDisplayCatalog.readOptional(assets,units(),passages());
        assertEquals("Synthetic approved label",c.unitTitle(U,2));
        assertEquals(1,c.revision);
    }
    @Test public void malformedOrUnreadablePresentAssetNeverBecomesMissing()throws Exception {
        for(String raw:new String[]{"{", "{}", pack(new JSONArray().put(entry("UNIT",U,5,12,26)))}) {
            try{AtlasDisplayCatalog.readOptional(new Assets().present(raw),units(),passages());fail("Present bad asset fell back");}
            catch(JSONException|IllegalArgumentException expected){}
        }
        Assets denied=new Assets().present(pack(new JSONArray()));denied.denyOpen=true;
        try{AtlasDisplayCatalog.readOptional(denied,units(),passages());fail("Unreadable present asset fell back");}
        catch(java.io.IOException expected){}
    }
    @Test public void invalidUtf8CannotBecomeAReplacementCharacterApprovedTitle()throws Exception {
        Assets assets=new Assets().present(pack(new JSONArray().put(entry("UNIT",U,5,11,26))));
        byte[] raw=assets.files.get(AtlasDisplayCatalog.ASSET);
        byte[] marker="Synthetic approved label".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        for(int i=0;i<raw.length-marker.length;i++) {
            if(raw[i]==marker[0]&&raw[i+1]==marker[1]) { raw[i]=(byte)0x80;break; }
        }
        try{AtlasDisplayCatalog.readOptional(assets,units(),passages());fail("Invalid UTF-8 admitted");}
        catch(java.io.IOException expected){}
    }
    @Test public void pendingUnitIsNeutralAndPendingPassageKeepsAdmissibleSource()throws Exception {
        JSONArray entries=new JSONArray().put(entry("UNIT",U,5,11,26).put("source_verified",false))
            .put(entry("PASSAGE",P,5,7,11).put("redistributable",false));
        AtlasDisplayCatalog c=parse(entries);
        assertEquals("Unité 2 · 11–26",c.unitTitle(U,2));
        assertEquals("Synthetic source",c.passageTitle(P,"Synthetic source"));
    }
    @Test public void approvedLocal511DoesNotReplaceTheFullPassageDisplay()throws Exception {
        AtlasDisplayCatalog c=parse(new JSONArray().put(entry("CONTEXT",P,5,11,11).put("unit_id",U)));
        assertEquals("Synthetic approved label",c.contextTitle(P,U,"Synthetic source"));
        assertEquals("Synthetic source",c.passageTitle(P,"Synthetic source"));
    }
    @Test public void fullPassageApprovalCannotLeakIntoAnUnapprovedLocalContext()throws Exception {
        AtlasDisplayCatalog c=parse(new JSONArray().put(entry("PASSAGE",P,5,7,11)));
        assertEquals("5:11 · partie du passage 5:7–11",c.contextTitle(P,U,"Synthetic source"));
        assertEquals("Synthetic approved label",c.passageTitle(P,"Synthetic source"));
    }
    @Test public void canonicalMapsAndOrderRemainUntouchedAndLaterMutationCannotChangeDisplay()throws Exception {
        Map<String,int[]> us=units(),ps=passages();
        AtlasDisplayCatalog c=AtlasDisplayCatalog.parse(pack(new JSONArray()),us,ps);
        assertEquals(U,new ArrayList<>(us.keySet()).get(0));
        assertArrayEquals(new int[]{5,11,26},us.get(U));
        assertArrayEquals(new int[]{5,7,11},ps.get(P));
        us.get(U)[1]=12;ps.clear();
        assertEquals("Unité 2 · 11–26",c.unitTitle(U,2));
        assertEquals("5:11 · partie du passage 5:7–11",c.contextTitle(P,U,"Synthetic source"));
    }
    private static void reject(JSONArray rows)throws Exception {
        try{parse(rows);fail("Invalid catalogue accepted");}catch(JSONException|IllegalArgumentException expected){}
    }
}
