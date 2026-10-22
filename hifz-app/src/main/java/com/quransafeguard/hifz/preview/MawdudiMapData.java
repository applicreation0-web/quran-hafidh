package com.quransafeguard.hifz.preview;
import android.content.Context;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.*;

/** Display headings are separate presentation metadata, never Quran/Hifz data. */
final class MawdudiMapData {
 static final class Macro {
  final int surah,start,end;final String id,label;
  Macro(int s,int a,int b,String t){this(String.format(Locale.ROOT,"s%03da%03db%03d",s,a,b),s,a,b,t);}
  Macro(String id,int s,int a,int b,String t){
   if(s<1||s>114||a<1||b<a||!String.format(Locale.ROOT,"s%03da%03db%03d",s,a,b).equals(id))
    throw new IllegalArgumentException("Mismatched Atlas macro identity");
   this.id=id;surah=s;start=a;end=b;label=t;
  }
 }
 private final List<Macro> all;
 private final AtlasDisplayCatalog catalog;
 private final Map<String,int[]> unitRanges,passageRanges;
 private MawdudiMapData(List<Macro> rows,AtlasDisplayCatalog catalog,
                        Map<String,int[]> units,Map<String,int[]> passages){
  all=Collections.unmodifiableList(new ArrayList<>(rows));this.catalog=catalog;
  unitRanges=units;passageRanges=passages;
 }
 static String read(Context c,String asset)throws Exception {
  try(InputStream in=c.getAssets().open(asset);ByteArrayOutputStream out=new ByteArrayOutputStream()) {
   byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1){if(out.size()+n>2_000_000)throw new IllegalStateException("oversized map pack");out.write(b,0,n);}
   PrivateDisplayIntegrity.verify(asset,out.toByteArray());
   return new String(out.toByteArray(),StandardCharsets.UTF_8);
  }
 }
 static MawdudiMapData load(Context c,MawdudiPassageIndex index)throws Exception {
  AtlasPrivateV2 source=AtlasPrivateV2.load(c,index);
  Map<String,int[]> units=new LinkedHashMap<>(),passages=new LinkedHashMap<>();
  for(AtlasPrivateV2.Unit u:source.units())units.put(u.id,new int[]{u.surah,u.start,u.end});
  for(MawdudiPassageIndex.Passage p:index.all())
   passages.put(p.id,new int[]{p.surah,p.firstAyah,p.lastAyah});
  // An absent optional asset preserves the exact legacy path. Once present,
  // all read/validation errors propagate: a broken update cannot revive legacy labels.
  AtlasDisplayCatalog catalog=AtlasDisplayCatalog.readOptional(new AtlasDisplayCatalog.AssetSource(){
   public String[] list(String directory)throws IOException{return c.getAssets().list(directory);}
   public InputStream open(String path)throws IOException{return c.getAssets().open(path);}
  },units,passages);
  if(catalog!=null){
   // Personal BOOX debug uses the exact editorial approvals, NOT source/rights gates.
   // Ordinary builds and all publication/release checks keep the certified path.
   if(BuildConfig.ATLAS_PRIVATE_EDITORIAL_PREVIEW&&
      (catalog.approvedUnits.size()!=366||catalog.approvedPassages.size()!=1053))
    throw new IllegalStateException("Private Atlas preview requires all 1419 user-approved titles");
   if(BuildConfig.AYAH_THEMES_FINAL_PRIVATE&&!BuildConfig.ATLAS_PRIVATE_EDITORIAL_PREVIEW
       &&catalog.units.size()!=366)
    throw new IllegalStateException("Atlas private APK blocked: 366 verified titles required");
   List<Macro> rows=new ArrayList<>();
   for(AtlasPrivateV2.Unit u:source.units())
    rows.add(new Macro(u.id,u.surah,u.start,u.end,
      BuildConfig.ATLAS_PRIVATE_EDITORIAL_PREVIEW?
        catalog.privatePreviewUnitTitle(u.id):catalog.unitTitle(u.id,u.ordinal)));
   return new MawdudiMapData(rows,catalog,units,passages);
  }
  if(BuildConfig.ATLAS_PRIVATE_EDITORIAL_PREVIEW)
   throw new IllegalStateException("Private Atlas preview needs the integrity-pinned 1419-title catalogue");
  // Existing debug versions can still load without the newly approved label
  // asset; malformed or tampered copies must not silently fall back.
  java.util.Map<String,String> approved=null;
  try{approved=readApprovedTitles(c,source);}
  catch(java.io.FileNotFoundException notYetPackaged){/* structural IDs remain valid */}
  // No next private APK may display placeholder "Unité n" for the 246 missing
  // editorial headings. The previous 120-title asset remains usable only for
  // non-final previews until a separately approved 366-title asset is SHA-pinned.
  if (BuildConfig.AYAH_THEMES_FINAL_PRIVATE &&
      (approved == null || approved.size() != 366))
   throw new IllegalStateException("Atlas private APK blocked: 366 verified titles required");
  List<Macro> rows=new ArrayList<>();
  for(AtlasPrivateV2.Unit u:source.units()){
   String title=approved==null?null:approved.get(u.id);
   rows.add(new Macro(u.id,u.surah,u.start,u.end,
       title==null?"Unité "+u.ordinal+" · "+u.start+"–"+u.end:title));
  }
  return new MawdudiMapData(rows,null,units,passages);
 }
 static MawdudiMapData parse(String raw,MawdudiPassageIndex index)throws JSONException {
  JSONObject root=new JSONObject(raw);
  if(!"MAWDUDI_SOURCE_SAFE_MACROS_V1".equals(root.optString("schema")))throw new IllegalStateException("wrong macro schema");
  JSONArray rows=root.getJSONArray("macros");if(rows.length()!=173)throw new IllegalStateException("missing macro");
  List<Macro> macros=new ArrayList<>();int i=0;
  for(int s=1;s<=114;s++) {
   List<MawdudiPassageIndex.Passage> ps=index.forSurah(s);int last=ps.get(ps.size()-1).lastAyah;
   int[][] exact=s==2?new int[][]{{1,20},{21,29},{30,39},{40,120},{121,141},{142,152},{153,251},{252,260},{261,283},{284,286}}:
    s==3?new int[][]{{1,32},{33,63},{64,120},{121,200}}:
    s==4?new int[][]{{1,35},{36,42},{43,43},{44,57},{58,72},{73,100},{101,103},{104,104},{105,135},{136,175},{176,176}}:
    s==5?new int[][]{{1,10},{11,26},{27,32},{33,40},{41,50},{51,69},{70,86},{87,108},{109,119},{120,120}}:
    s==6?new int[][]{{1,12},{13,24},{25,32},{33,73},{74,90},{91,108},{109,154},{155,160},{161,165}}:
    s==7?new int[][]{{1,10},{11,25},{26,53},{54,58},{59,171},{172,174},{175,179},{180,198},{199,206}}:
    s==8?new int[][]{{1,41},{42,54},{55,59},{60,66},{67,71},{72,75}}:
    s==9?new int[][]{{1,12},{13,37},{38,72},{73,90},{91,110},{111,118},{119,127},{128,129}}:new int[][]{{1,last}};
   for(int[] range:exact) {
    JSONObject row=rows.getJSONObject(i++);
    String expected=exact.length==1?"WHOLE_SURAH_ONLY_NO_NEW_SPLIT_INVENTED":s==5&&range[0]==120?"SOURCE_CONCLUSION_FROM_INTRODUCTION":(s==2||s>=4)?"SOURCE_BOUNDARY_CONFIRMED_IN_INTRODUCTION":"SOURCE_BOUNDARY_CONFIRMED_FROM_PDF";
    if(row.getInt("surah")!=s||row.getInt("start")!=range[0]||row.getInt("end")!=range[1]
       ||!expected.equals(row.getString("status")))throw new IllegalStateException("unapproved macro boundary");
    String label=row.getString("label");if(label.trim().isEmpty()||label.length()>1000)throw new IllegalStateException("missing macro label");
    macros.add(new Macro(s,range[0],range[1],label));
   }
  }
  // Parsed legacy/synthetic maps use the same validated display identities as
  // loaded maps. The screen resolves both unit and passage titles by these IDs.
  Map<String,int[]> units=new LinkedHashMap<>(),passages=new LinkedHashMap<>();
  for(Macro m:macros)units.put(m.id,new int[]{m.surah,m.start,m.end});
  for(MawdudiPassageIndex.Passage p:index.all())
   passages.put(p.id,new int[]{p.surah,p.firstAyah,p.lastAyah});
  return new MawdudiMapData(macros,null,units,passages);
 }
 /** Approved private summaries: exact edition-safe labels, SHA-256 pinned and unit-bound.
  * Never treat them as literal titles copied from Maududi.
  */
 private static java.util.Map<String,String> readApprovedTitles(Context c,AtlasPrivateV2 index)throws Exception{
  final String asset="atlas_private/atlas_approved_unit_titles_366_v1.json";
  final String checksum="421277c59519f2b904de23bbc1e0eee6faeb569e33ed6b425b4b9235cf82ff79";
  byte[] bytes;
  try(InputStream in=c.getAssets().open(asset);ByteArrayOutputStream out=new ByteArrayOutputStream()){
   byte[] buffer=new byte[4096];int n;
   while((n=in.read(buffer))!=-1){
    if(out.size()+n>150_000)throw new IllegalStateException("oversized approved headings");
    out.write(buffer,0,n);
   }
   bytes=out.toByteArray();
  }
  StringBuilder digest=new StringBuilder();
  for(byte b:java.security.MessageDigest.getInstance("SHA-256").digest(bytes))
   digest.append(String.format(java.util.Locale.ROOT,"%02x",b&255));
  if(!checksum.equals(digest.toString()))throw new IllegalStateException("Atlas label digest mismatch");
  JSONObject root=new JSONObject(new String(bytes,StandardCharsets.UTF_8));
  if(!"HAAFIDH_ATLAS_USER_APPROVED_EDITORIAL_LABELS_V1".equals(root.optString("schema"))
       ||root.optInt("unit_index_count")!=366||root.optInt("titled_unit_count")!=120
       ||root.optBoolean("original_quotes"))throw new IllegalStateException("unapproved Atlas labels");
  java.util.Map<String,AtlasPrivateV2.Unit> source=new java.util.HashMap<>();
  for(AtlasPrivateV2.Unit unit:index.units())source.put(unit.id,unit);
  JSONArray entries=root.getJSONArray("labels");
  if(entries.length()!=120)throw new IllegalStateException("incomplete approved Atlas labels");
  java.util.Map<String,String> labels=new java.util.HashMap<>();
  for(int i=0;i<entries.length();i++){
   JSONObject item=entries.getJSONObject(i);
   String id=item.getString("id");
   AtlasPrivateV2.Unit unit=source.get(id);
   if(unit==null||unit.surah!=item.getInt("surah")||unit.start!=item.getInt("start")
       ||unit.end!=item.getInt("end")
       ||!"USER_APPROVED_EDITORIAL_SOURCE_SUMMARY".equals(item.getString("origin")))
    throw new IllegalStateException("mismatched Atlas label");
   String label=item.getString("title").trim();
   if(label.isEmpty()||label.length()>240||labels.put(id,label)!=null)
    throw new IllegalStateException("duplicate/invalid Atlas label");
  }
  return labels;
 }
 int count(){return all.size();}
 AtlasDisplayCatalog displayCatalog(){return catalog;}
 String displayUnitTitle(String id){
  int[] r=unitRanges.get(id);
  if(r==null)throw new IllegalArgumentException("Unknown Atlas unit");
  return AtlasDisplayTitleFormatter.display(at(r[0],r[1]).label);
 }
 String displayPassageTitle(String id,String admissibleSource){
  if(!passageRanges.containsKey(id))throw new IllegalArgumentException("Unknown canonical passage");
  String selected=catalog==null?(admissibleSource==null?"":admissibleSource):
   BuildConfig.ATLAS_PRIVATE_EDITORIAL_PREVIEW?catalog.privatePreviewPassageTitle(id):
      catalog.passageTitle(id,admissibleSource);
  return AtlasDisplayTitleFormatter.displayPassage(selected);
 }
 String displayContextTitle(String passageId,String unitId,String admissibleSource){
  int[] p=passageRanges.get(passageId),u=unitRanges.get(unitId);
  if(p==null||u==null||p[0]!=u[0])throw new IllegalArgumentException("Unknown presentation parents");
  String selected=catalog!=null?
    (BuildConfig.ATLAS_PRIVATE_EDITORIAL_PREVIEW?
      catalog.privatePreviewContextTitle(passageId,unitId,admissibleSource):
      catalog.contextTitle(passageId,unitId,admissibleSource)):
    AtlasPassageContext.displayTitle(p[0],p[1],p[2],u[1],u[2],admissibleSource);
  // Source-side range labels remain intact, but the approved BOOX Atlas subtitle
  // must never expose a numeric "partie du passage" string. The canonical
  // Ayah Themes heading is still named explicitly as a partial excerpt.
  if(selected!=null&&selected.matches("^\\d+:\\d+.*\\bpartie du passage\\b.*"))
    selected=displayPassageTitle(passageId,admissibleSource);
  return AtlasDisplayTitleFormatter.displayPassage(selected);
 }
 List<Macro> forSurah(int s){List<Macro> r=new ArrayList<>();for(Macro m:all)if(m.surah==s)r.add(m);return r;}
 Macro at(int s,int a){for(Macro m:all)if(m.surah==s&&m.start<=a&&m.end>=a)return m;throw new IllegalArgumentException("macro verse");}
}

/**
 * A canonical Ayah Themes passage may cross Atlas unit boundaries.
 * The local context is display-only; it never creates a new source passage,
 * changes its ID, or attributes the complete source heading to a partial range.
 */
final class AtlasPassageContext {
    private AtlasPassageContext() {}

    static int intersectionStart(int sourceStart, int unitStart) {
        return Math.max(sourceStart, unitStart);
    }

    static int intersectionEnd(int sourceEnd, int unitEnd) {
        return Math.min(sourceEnd, unitEnd);
    }

    static String displayTitle(int surah, int sourceStart, int sourceEnd,
                               int unitStart, int unitEnd, String original) {
        if (surah < 1 || surah > 114 || sourceStart < 1 || unitStart < 1 ||
                sourceEnd < sourceStart || unitEnd < unitStart)
            throw new IllegalArgumentException("Invalid canonical or Atlas boundary");
        int start = intersectionStart(sourceStart, unitStart);
        int end = intersectionEnd(sourceEnd, unitEnd);
        if (start > end)
            throw new IllegalArgumentException("No passage/Atlas intersection");
        // Full passage: the untouched source title remains the correct caption.
        if (start == sourceStart && end == sourceEnd)
            return original == null ? "" : original;
        // Partial passage: NEVER attribute the full source heading to this unit.
        // Use a grounded reference until a local title is reviewed and approved.
        String portion = surah + ":" + start + (start == end ? "" : "–" + end);
        String source = surah + ":" + sourceStart
                + (sourceStart == sourceEnd ? "" : "–" + sourceEnd);
        return portion + " · partie du passage " + source;
    }
}

/**
 * Versioned presentation metadata, independent of the historical 120-label pack.
 * The optional asset uses ordinary versioned metadata; absence retains legacy.
 * Canonical ranges come from the existing indexes; no source text or Hifz state
 * is created, modified or approved by parsing this document.
 */
final class AtlasDisplayCatalog {
    static final String SCHEMA="HAAFIDH_DISPLAY_CATALOG_V1";
    static final String ASSET="atlas/atlas_display_catalog_v1.json";
    final int revision;
    // Editorial approval retains the exact chosen wording, independently of
    // bibliography certification and permission to expose it at runtime.
    final Map<String,String> approvedUnits,approvedPassages,approvedContexts;
    final Map<String,String> units,passages,contexts;
    private final Map<String,int[]> canonicalUnits,canonicalPassages;

    interface AssetSource {
        String[] list(String directory)throws IOException;
        InputStream open(String path)throws IOException;
    }

    static AtlasDisplayCatalog readOptional(AssetSource assets,Map<String,int[]> units,
                                            Map<String,int[]> passages)throws IOException,JSONException {
        String[] names=assets.list("atlas");
        if(names==null)throw new IOException("Cannot list Atlas display assets");
        if(!Arrays.asList(names).contains("atlas_display_catalog_v1.json"))return null;
        try(InputStream in=assets.open(ASSET);ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] buffer=new byte[4096];int n;
            while((n=in.read(buffer))!=-1){
                if(out.size()+n>2_000_000)throw new IOException("Oversized presentation catalogue");
                out.write(buffer,0,n);
            }
            // String(byte[], UTF_8) silently replaces malformed bytes. A damaged
            // versioned title must be rejected, not displayed with changed text.
            byte[] checkedBytes=out.toByteArray();
            // SHA pin authenticates private editorial text without adding it to GitHub.
            // Never relax certification flags or trust the contents of an arbitrary APK asset.
            if(BuildConfig.ATLAS_PRIVATE_EDITORIAL_PREVIEW){
                boolean exact;
                try {
                    exact=PrivateDisplayIntegrity.matches(
                        checkedBytes,"065ec0dd68c488efacbd9e77635bf9d531b4cbb2b1bc82db7853ad4da0c02c9c");
                }catch(Exception digestUnavailable){
                    throw new IOException("Cannot verify private Atlas editorial catalogue",digestUnavailable);
                }
                if(!exact)throw new IOException("Private Atlas editorial catalogue integrity mismatch");
            }
            String raw=StandardCharsets.UTF_8.newDecoder()
                    .decode(java.nio.ByteBuffer.wrap(checkedBytes)).toString();
            return parse(raw,units,passages);
        }
    }

    private AtlasDisplayCatalog(int revision,Map<String,String> units,
                                Map<String,String> passages,Map<String,String> contexts,
                                Map<String,String> approvedUnits,Map<String,String> approvedPassages,
                                Map<String,String> approvedContexts,Map<String,int[]> canonicalUnits,Map<String,int[]> canonicalPassages) {
        this.revision=revision;
        this.approvedUnits=Collections.unmodifiableMap(approvedUnits);
        this.approvedPassages=Collections.unmodifiableMap(approvedPassages);
        this.approvedContexts=Collections.unmodifiableMap(approvedContexts);
        this.units=Collections.unmodifiableMap(units);
        this.passages=Collections.unmodifiableMap(passages);
        this.contexts=Collections.unmodifiableMap(contexts);
        this.canonicalUnits=copyRanges(canonicalUnits);
        this.canonicalPassages=copyRanges(canonicalPassages);
    }

    private static Map<String,int[]> copyRanges(Map<String,int[]> original){
        Map<String,int[]> copy=new LinkedHashMap<>();
        for(Map.Entry<String,int[]> e:original.entrySet())copy.put(e.getKey(),e.getValue().clone());
        return Collections.unmodifiableMap(copy);
    }

    String unitTitle(String id,int ordinal){
        int[] r=canonicalUnits.get(id);
        if(r==null||ordinal<1)throw new IllegalArgumentException("Unknown Atlas unit");
        String approved=units.get(id);
        return approved==null?"Unité "+ordinal+" · "+r[1]+"–"+r[2]:approved;
    }

    /** Preview-only selection of immutable user-approved editorial text; never certification. */
    String privatePreviewUnitTitle(String id){
        if(!canonicalUnits.containsKey(id))throw new IllegalArgumentException("Unknown Atlas unit");
        String title=approvedUnits.get(id);
        if(title==null)throw new IllegalStateException("Unapproved private Atlas unit");
        return title;
    }

    String privatePreviewPassageTitle(String id){
        if(!canonicalPassages.containsKey(id))throw new IllegalArgumentException("Unknown canonical passage");
        String title=approvedPassages.get(id);
        if(title==null)throw new IllegalStateException("Unapproved private Atlas passage");
        return title;
    }

    String privatePreviewContextTitle(String passageId,String unitId,String admissibleSource){
        int[] p=canonicalPassages.get(passageId),u=canonicalUnits.get(unitId);
        if(p==null||u==null||p[0]!=u[0])throw new IllegalArgumentException("Unknown presentation parents");
        int start=Math.max(u[1],p[1]),end=Math.min(u[2],p[2]);
        if(start>end)throw new IllegalArgumentException("No passage/Atlas intersection");
        String local=approvedContexts.get(passageId+"|"+unitId);
        if(local!=null)return local;
        if(start==p[1]&&end==p[2])return privatePreviewPassageTitle(passageId);
        // A clipped Ayah Themes passage must not inherit its complete source title.
        return AtlasPassageContext.displayTitle(p[0],p[1],p[2],u[1],u[2],admissibleSource);
    }

    String passageTitle(String id,String admissibleSource){
        if(!canonicalPassages.containsKey(id))throw new IllegalArgumentException("Unknown canonical passage");
        String approved=passages.get(id);
        return approved==null?(admissibleSource==null?"":admissibleSource):approved;
    }

    String contextTitle(String passageId,String unitId,String admissibleSource){
        int[] p=canonicalPassages.get(passageId),u=canonicalUnits.get(unitId);
        if(p==null||u==null||p[0]!=u[0])throw new IllegalArgumentException("Unknown presentation parents");
        // Validate even an unlabelled intersection. A full passage label is never
        // inherited by a partial intersection without its own approved context.
        String neutral=AtlasPassageContext.displayTitle(p[0],p[1],p[2],u[1],u[2],admissibleSource);
        String local=contextTitle(passageId,unitId);
        if(local!=null)return local;
        return u[1]<=p[1]&&u[2]>=p[2]?passageTitle(passageId,admissibleSource):neutral;
    }

    String contextTitle(String passageId,String unitId) {
        return contexts.get(passageId+"|"+unitId);
    }

    static AtlasDisplayCatalog parse(String raw,Map<String,int[]> canonicalUnits,
                                     Map<String,int[]> canonicalPassages)throws JSONException {
        if(raw==null||raw.length()>2_000_000||canonicalUnits==null||canonicalPassages==null)
            throw new IllegalArgumentException("Missing or oversized presentation catalogue");
        JSONObject root=new JSONObject(raw);
        int revision=integer(root,"revision");
        if(!SCHEMA.equals(text(root,"schema"))||revision<1)
            throw new IllegalArgumentException("Unsupported presentation catalogue");
        JSONArray entries=root.getJSONArray("entries");
        if(entries.length()>5000)throw new IllegalArgumentException("Too many presentation entries");
        Map<String,String> units=new LinkedHashMap<>(),passages=new LinkedHashMap<>(),contexts=new LinkedHashMap<>();
        Map<String,String> approvedUnits=new LinkedHashMap<>(),approvedPassages=new LinkedHashMap<>(),
                approvedContexts=new LinkedHashMap<>();
        Set<String> seen=new HashSet<>();
        for(int i=0;i<entries.length();i++) {
            JSONObject entry=entries.getJSONObject(i);
            String level=text(entry,"level"),id=text(entry,"id");
            int s=integer(entry,"surah"),a=integer(entry,"start"),b=integer(entry,"end");
            if(s<1||s>114||a<1||b<a)
                throw new IllegalArgumentException("Invalid presentation range");
            Map<String,String> target;
            String key=id;
            if("UNIT".equals(level)) {
                requireRange(canonicalUnits.get(id),s,a,b);
                target=units;
            } else if("PASSAGE".equals(level)) {
                requireRange(canonicalPassages.get(id),s,a,b);
                target=passages;
            } else if("CONTEXT".equals(level)) {
                String unitId=text(entry,"unit_id");
                int[] unit=canonicalUnits.get(unitId),passage=canonicalPassages.get(id);
                if(unit==null||passage==null||unit.length!=3||passage.length!=3||unit[0]!=s||passage[0]!=s)
                    throw new IllegalArgumentException("Unknown presentation parents");
                requireRange(new int[]{s,Math.max(unit[1],passage[1]),Math.min(unit[2],passage[2])},s,a,b);
                key=id+"|"+unitId;
                target=contexts;
            } else throw new IllegalArgumentException("Unknown presentation level");
            // Duplicate pending entries are invalid too: approvals must not depend on order.
            if(!seen.add(level+"|"+key))throw new IllegalArgumentException("Duplicate presentation identity");
            String rawTitle=text(entry,"title");
            String title=rawTitle.trim();
            String origin=text(entry,"origin");
            boolean originalPassage="PASSAGE".equals(level)&&"ORIGINAL_AYAH_THEMES_TITLE".equals(origin);
            if(title.isEmpty()||title.length()>(originalPassage?1000:240))
                throw new IllegalArgumentException("Invalid presentation title");
            for(int k=0;k<rawTitle.length();k++)if(Character.isISOControl(rawTitle.charAt(k)))
                throw new IllegalArgumentException("Control character in presentation title");
            if(!"ORIGINAL_EDITORIAL_SUMMARY".equals(origin)&&!originalPassage)
                throw new IllegalArgumentException("Unsupported presentation title origin");
            for(String field:new String[]{"source_ref","edition","locator","review_id"})
                if(text(entry,field).trim().isEmpty())
                    throw new IllegalArgumentException("Missing presentation provenance");
            // Read every flag, even when a previous gate is false, so malformed metadata fails closed.
            boolean user=approval(entry,"user_approved"),source=approval(entry,"source_verified"),
                    independent=approval(entry,"independent_verified"),rights=approval(entry,"redistributable");
            if(user) {
                Map<String,String> approved="UNIT".equals(level)?approvedUnits:
                        "PASSAGE".equals(level)?approvedPassages:approvedContexts;
                approved.put(key,rawTitle);
            }
            if(user&&source&&independent&&rights)target.put(key,title);
        }
        return new AtlasDisplayCatalog(revision,units,passages,contexts,approvedUnits,approvedPassages,approvedContexts,canonicalUnits,canonicalPassages);
    }

    private static void requireRange(int[] source,int s,int a,int b) {
        if(source==null||source.length!=3||source[0]!=s||source[1]!=a||source[2]!=b)
            throw new IllegalArgumentException("Changed canonical presentation boundary");
    }

    private static boolean approval(JSONObject entry,String field)throws JSONException {
        Object value=entry.get(field);
        if(!(value instanceof Boolean))throw new IllegalArgumentException("Approval flag must be a boolean");
        return (Boolean)value;
    }

    private static int integer(JSONObject entry,String field)throws JSONException {
        Object value=entry.get(field);
        if(!(value instanceof Number))throw new IllegalArgumentException("Coordinate must be a number");
        double number=((Number)value).doubleValue();
        if(!Double.isFinite(number)||number<Integer.MIN_VALUE||number>Integer.MAX_VALUE||number!=Math.rint(number))
            throw new IllegalArgumentException("Coordinate must be an exact integer");
        return (int)number;
    }

    private static String text(JSONObject entry,String field)throws JSONException {
        Object value=entry.get(field);
        if(!(value instanceof String))throw new IllegalArgumentException("Presentation text must be a string");
        return (String)value;
    }
}
