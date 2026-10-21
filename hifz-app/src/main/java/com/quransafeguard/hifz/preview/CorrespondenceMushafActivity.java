package com.quransafeguard.hifz.preview;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.text.InputType;
import com.quransafeguard.hifz.core.QuranCanon;
import com.quransafeguard.hifz.core.VerseRef;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

/** Mushaf-first read-only relations. The 110 documentary dossiers remain
 * unchanged and no proposed literary classification is granted approval. */
public final class CorrespondenceMushafActivity extends Activity {
 private static final String ASSET="correspondences/reviewed_110_v2.json";
 private static final String SHA="d9867ba3ade9c8902fa98cad76cfe92662a7bfc0a9a30946cdd1707c230c6bb7";
 private final List<JSONObject> historic=new ArrayList<>();
 private final Map<Integer,List<JSONObject>> byPage=new HashMap<>();
 private final Map<Integer,CorrespondenceQcfSurface.State> remembered=new HashMap<>();
 private GeometryRepository geometry;
 private CorrespondenceRuntime runtime;
 private TextView location,caption,shortLink;
 private ImageView selectedGlyph;
 private LinearLayout folios,relationButtons;
 private HorizontalScrollView strip;
 private CorrespondenceQcfSurface[] visible;
 private int[] visiblePositions;
 private int page,seriesOrdinal,referenceWindow,alternanceRole,comparisonPane,centreSegment=2;
 private boolean seriesCompare,privateRegistryVerified,comparisonExpanded,exploringTypes;
 private JSONObject active;

 @Override protected void onCreate(Bundle saved){
  super.onCreate(saved);
  getWindow().setFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN,
      android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN);
  try{
   geometry=GeometryRepository.get(this);
   runtime=CorrespondenceRuntime.load(this);
   loadIndex();
   privateRegistryVerified=true;
  }catch(Exception badIndex){
   // A missing or altered PRIVATE directory must not be replaced with inferred links.
   historic.clear();byPage.clear();
  }
  int previous=getSharedPreferences("hifz_study",MODE_PRIVATE).getInt("page",1);
  page=clamp(saved==null?getIntent().getIntExtra(StudyReaderActivity.EXTRA_JUMP_PAGE,previous)
      :saved.getInt("folio",previous));
  String id=saved==null?getIntent().getStringExtra("relationId"):saved.getString("relation");
  if(id!=null)for(JSONObject entry:historic)if(id.equals(entry.optString("id"))){active=entry;break;}
  seriesOrdinal=saved==null?0:saved.getInt("occurrence",0);
  referenceWindow=saved==null?0:saved.getInt("referenceWindow",0);
  alternanceRole=saved==null?0:saved.getInt("alternanceRole",0);
  centreSegment=saved==null?2:saved.getInt("centreSegment",2);
  comparisonExpanded=saved!=null&&saved.getBoolean("comparisonExpanded",false);
  comparisonPane=saved==null?0:saved.getInt("comparisonPane",0);
  if(saved==null&&active!=null&&"REPETITION".equals(active.optString("type"))){
   // When launched from Atlas or Lecture, highlight the occurrence actually
   // touched, not always the first occurrence of the source relation.
   int contextSurah=getIntent().getIntExtra("contextSurah",-1);
   int contextAyah=getIntent().getIntExtra("contextAyah",-1);
   JSONArray positions=refs(active);
   if(positions!=null)for(int i=0;i<positions.length();i++){
    JSONObject range=positions.optJSONObject(i);
    if(range!=null&&range.optInt("surah")==contextSurah
       &&contextAyah>=range.optInt("start")&&contextAyah<=range.optInt("end")){
     seriesOrdinal=i;break;
    }
   }
  }
  seriesCompare=saved!=null&&saved.getBoolean("seriesCompare",false);
  build();
 }

 private static int clamp(int number){return Math.max(1,Math.min(604,number));}
 private static String ref(JSONObject range){
  if(range==null)return "";
  int first=range.optInt("start"),last=range.optInt("end");
  return range.optInt("surah")+":"+first+(first==last?"":"–"+last);
 }
 private JSONArray refs(JSONObject row){return row.optJSONArray("refs");}
 private static boolean classified(String value){
  if(value==null)return false;
  try{CorrespondenceModel.Type.valueOf(value);return true;}
  catch(IllegalArgumentException missing){return false;}
 }
 private void loadIndex()throws Exception{
  byte[] bytes;
  try(InputStream in=getAssets().open(ASSET);ByteArrayOutputStream out=new ByteArrayOutputStream()){
   byte[] buffer=new byte[8192];int n;
   while((n=in.read(buffer))!=-1){
    if(out.size()+n>300000)throw new IllegalStateException("Invalid historical size");
    out.write(buffer,0,n);
   }
   bytes=out.toByteArray();
  }
  StringBuilder sha=new StringBuilder();
  for(byte b:MessageDigest.getInstance("SHA-256").digest(bytes))
   sha.append(String.format(Locale.ROOT,"%02x",b&255));
  if(!SHA.equals(sha.toString()))throw new IllegalStateException("Incorrect private registry");
  JSONObject root=new JSONObject(new String(bytes,StandardCharsets.UTF_8));
  JSONArray rows=root.getJSONArray("entries");
  if(!"HAAFIDH_CORRESPONDENCES_REVIEWED_110_V2".equals(root.getString("schema"))
      ||root.getInt("count")!=110||rows.length()!=110)
   throw new IllegalArgumentException("Wrong historical registry");
  for(int i=0;i<rows.length();i++){
   JSONObject e=rows.getJSONObject(i);
   if(!String.format(Locale.ROOT,"R%03d",i+1).equals(e.getString("id")))
    throw new IllegalArgumentException("Non-canonical dossier");
   historic.add(e);
   JSONArray historical=e.optJSONArray("original_refs");
   JSONArray checkedFocus=refs(e);
   if(historical==null||historical.length()==0||checkedFocus==null||checkedFocus.length()==0)
    throw new IllegalArgumentException("Incomplete original/focal boundaries");
   for(int j=0;j<checkedFocus.length();j++){
    JSONObject focus=checkedFocus.getJSONObject(j);
    boolean contained=false;
    for(int k=0;k<historical.length();k++){
     JSONObject original=historical.getJSONObject(k);
     if(focus.getInt("surah")==original.getInt("surah")
      &&focus.getInt("start")>=original.getInt("start")
      &&focus.getInt("end")<=original.getInt("end")){contained=true;break;}
    }
    if(!contained)throw new IllegalArgumentException("Focus outside immutable Quranic bounds");
   }
   // Rejected and unclassified hypotheses remain accessible only by exact ID;
   // never offer an unsupported pictogram on an unrelated page.
   if(e.optBoolean("excluded_from_recommendations",false))continue;
   JSONArray ranges=refs(e);if(ranges==null||ranges.length()==0)
    throw new IllegalArgumentException("Missing original reference bounds");
   for(int j=0;j<ranges.length();j++){
    JSONObject r=ranges.optJSONObject(j);if(r==null)
     throw new IllegalArgumentException("Malformed original reference");
    int surah=r.optInt("surah"),first=r.optInt("start"),last=r.optInt("end");
    if(surah<1||surah>114||first<1||last<first
        ||last>QuranCanon.INSTANCE.ayahCount(surah))
     throw new IllegalArgumentException("Out-of-surah original verse range");
    int fp=geometry.pageForVerse(new VerseRef(surah,first));
    int lp=MawdudiMapModel.lastPageForVerse(new VerseRef(surah,last),geometry);
    if(fp<1||lp>604||fp>lp)
     throw new IllegalArgumentException("Invalid QCF folio geometry");
    for(int p=fp;p<=lp;p++){
     List<JSONObject> onPage=byPage.get(p);
     if(onPage==null){onPage=new ArrayList<>();byPage.put(p,onPage);}
     if(!onPage.contains(e))onPage.add(e);
    }
   }
  }
 }
 private void build(){
  LinearLayout root=Ui.column(this);root.setBackgroundColor(Ui.PAPER);
  LinearLayout toolbar=Ui.row(this);toolbar.setGravity(Gravity.CENTER_VERTICAL);
  toolbar.addView(CorrespondenceQcfSurface.action(this,R.drawable.ic_ui_back,
      "Retour",v->{if(active==null)finish();else returnToPage();}),
      new LinearLayout.LayoutParams(Ui.dp(this,48),Ui.dp(this,48)));
  LinearLayout header=Ui.column(this);header.setGravity(Gravity.CENTER);
  TextView title=Ui.text(this,"Correspondances",18,true);title.setGravity(Gravity.CENTER);
  location=Ui.text(this,"",11,false);location.setGravity(Gravity.CENTER);
  header.addView(title);header.addView(location);
  toolbar.addView(header,new LinearLayout.LayoutParams(0,Ui.dp(this,56),1));
  TextView find=Ui.text(this,"R…",13,true);find.setGravity(Gravity.CENTER);
  find.setOnClickListener(v->findDossier());
  find.setContentDescription("Ouvrir un dossier ou une page");
  toolbar.addView(find,new LinearLayout.LayoutParams(Ui.dp(this,48),Ui.dp(this,48)));
  toolbar.addView(CorrespondenceQcfSurface.action(this,R.drawable.ic_ui_info,
      "Justification et sources",v->sources()),
      new LinearLayout.LayoutParams(Ui.dp(this,48),Ui.dp(this,48)));
  root.addView(toolbar,new LinearLayout.LayoutParams(-1,Ui.dp(this,56)));
  root.addView(Ui.divider(this));

  LinearLayout relationHeading=Ui.row(this);relationHeading.setGravity(Gravity.CENTER_VERTICAL);
  selectedGlyph=new ImageView(this);
  selectedGlyph.setImageTintList(null);
  selectedGlyph.setPadding(Ui.dp(this,5),Ui.dp(this,4),Ui.dp(this,5),Ui.dp(this,4));
  selectedGlyph.setVisibility(View.GONE);
  relationHeading.addView(selectedGlyph,new LinearLayout.LayoutParams(Ui.dp(this,44),Ui.dp(this,42)));
  shortLink=Ui.text(this,"",12,false);shortLink.setGravity(Gravity.CENTER);
  shortLink.setPadding(Ui.dp(this,5),Ui.dp(this,6),Ui.dp(this,5),Ui.dp(this,6));
  shortLink.setFocusable(true);shortLink.setClickable(true);
  shortLink.setContentDescription("Explorer les correspondances par type ou revenir à la page");
  shortLink.setOnClickListener(v->{
   if(active!=null||!privateRegistryVerified)return;
   exploringTypes=!exploringTypes;
   if(exploringTypes)showTypeExplorer();else renderPageLinks();
  });
  relationHeading.addView(shortLink,new LinearLayout.LayoutParams(0,Ui.dp(this,42),1));
  root.addView(relationHeading,new LinearLayout.LayoutParams(-1,Ui.dp(this,42)));
  strip=new HorizontalScrollView(this);strip.setHorizontalScrollBarEnabled(false);
  strip.setFillViewport(false);
  relationButtons=Ui.row(this);relationButtons.setGravity(Gravity.CENTER_VERTICAL);
  strip.addView(relationButtons,new android.widget.FrameLayout.LayoutParams(-2,Ui.dp(this,76)));
  root.addView(strip,new LinearLayout.LayoutParams(-1,Ui.dp(this,76)));

  folios=Ui.row(this);folios.setGravity(Gravity.CENTER);
  root.addView(folios,new LinearLayout.LayoutParams(-1,0,1));
  caption=Ui.text(this,"",11,false);caption.setGravity(Gravity.CENTER);
  caption.setPadding(Ui.dp(this,8),Ui.dp(this,3),Ui.dp(this,8),Ui.dp(this,5));
  caption.setMaxLines(2);
  root.addView(caption,new LinearLayout.LayoutParams(-1,Ui.dp(this,39)));
  setContentView(root);
  if(active==null)showPlainPage(page);else showActive();
 }
 private void showPlainPage(int wanted){
  page=clamp(wanted);
  active=null;seriesCompare=false;exploringTypes=false;comparisonExpanded=false;
  setActiveGlyph(null);
  release();
  location.setText("Page courante · p. "+page);
  shortLink.setText("Relations de la page · "+byPage.getOrDefault(page,new ArrayList<>()).size());
  caption.setText("Muṣḥaf QCF · tourner les pages pour découvrir les liens");
  CorrespondenceQcfSurface full=new CorrespondenceQcfSurface(this);
  visible=new CorrespondenceQcfSurface[]{full};
  folios.addView(full,new LinearLayout.LayoutParams(0,-1,1));
  full.setPageListener(p->{
   if(active!=null||p==page)return;
   page=p;location.setText("Page courante · p. "+page);
   caption.setText("Muṣḥaf QCF · page "+page);
   renderPageLinks();
  });
  full.show(CorrespondenceDocument.plainPage(page),null);
  renderPageLinks();
 }
 private void renderPageLinks(){
  relationButtons.removeAllViews();
  strip.setVisibility(View.VISIBLE);
  if(!privateRegistryVerified){
   shortLink.setText("Catalogue privé indisponible · lecture QCF uniquement");
   TextView warning=Ui.text(this,"Correspondances non chargées — intégrité à vérifier",11,true);
   warning.setPadding(Ui.dp(this,10),0,0,0);
   relationButtons.addView(warning);
   return;
  }
  List<JSONObject> links=byPage.get(page);
  if(links==null||links.isEmpty()){
   shortLink.setText("Page "+page+" · aucun lien ici · toucher pour explorer les 8 types");
   int before=0,after=0;
   for(int p:byPage.keySet())if(p<page&&p>before)before=p;
     else if(p>page&&(after==0||p<after))after=p;
   if(before>0)addNearestPage(before,"‹");
   if(after>0)addNearestPage(after,"›");
   if(before==0&&after==0)strip.setVisibility(View.GONE);
   return;
  }
  shortLink.setText("Page "+page+" · "+links.size()+" relation"+(links.size()>1?"s":"")+" · toucher pour filtrer");
  for(JSONObject row:links)addRelationNode(row);
 }
 private void addNearestPage(int target,String arrow){
  TextView item=Ui.text(this,arrow+" p. "+target+" · "+byPage.get(target).size()+" lien(s)",13,true);
  item.setGravity(Gravity.CENTER);
  item.setFocusable(true);item.setClickable(true);
  item.setContentDescription("Feuilleter directement la page "+target+" contenant des correspondances");
  item.setOnClickListener(v->showPlainPage(target));
  relationButtons.addView(item,new LinearLayout.LayoutParams(Ui.dp(this,165),Ui.dp(this,72)));
 }
 private void showTypeExplorer(){
  relationButtons.removeAllViews();strip.setVisibility(View.VISIBLE);
  shortLink.setText("Types de correspondances · toucher pour revenir à la page");
  for(CorrespondenceModel.Type type:CorrespondenceModel.Type.values()){
   int target=0,wrapped=0;
   for(int p=1;p<=604;p++){
    List<JSONObject> links=byPage.get(p);
    if(links==null)continue;
    boolean found=false;
    for(JSONObject row:links)if(type.name().equals(row.optString("type"))){found=true;break;}
    if(found){if(wrapped==0)wrapped=p;if(p>=page&&target==0)target=p;}
   }
   final int destination=target==0?wrapped:target;
   LinearLayout cell=Ui.column(this);cell.setGravity(Gravity.CENTER);
   ImageView symbol=new ImageView(this);
   symbol.setImageResource(CorrespondenceScreen.icon(type));symbol.setImageTintList(null);
   symbol.setScaleType(ImageView.ScaleType.FIT_CENTER);
   cell.addView(symbol,new LinearLayout.LayoutParams(Ui.dp(this,38),Ui.dp(this,37)));
   TextView label=Ui.text(this,CorrespondenceScreen.label(type),9,false);
   label.setGravity(Gravity.CENTER);label.setSingleLine(true);cell.addView(label);
   cell.setFocusable(destination>0);cell.setClickable(destination>0);
   cell.setContentDescription(type.name()+(destination>0?" · page "+destination:" · aucun dossier catégorisé"));
   if(destination>0)cell.setOnClickListener(v->showPlainPage(destination));
   relationButtons.addView(cell,new LinearLayout.LayoutParams(Ui.dp(this,113),Ui.dp(this,72)));
  }
 }
 private void addRelationNode(JSONObject row){
  String type=row.optString("type","");
  boolean approved=runtime!=null&&runtime.find(row.optString("id"))!=null;
  LinearLayout cell=Ui.column(this);cell.setGravity(Gravity.CENTER);
  cell.setPadding(Ui.dp(this,6),0,Ui.dp(this,6),0);
  if(classified(type)){
   ImageView symbol=new ImageView(this);
   symbol.setImageResource(CorrespondenceScreen.icon(CorrespondenceModel.Type.valueOf(type)));
   symbol.setImageTintList(null);symbol.setScaleType(ImageView.ScaleType.FIT_CENTER);
   cell.addView(symbol,new LinearLayout.LayoutParams(Ui.dp(this,40),Ui.dp(this,39)));
  }else{
   TextView unknown=Ui.text(this,"?",19,true);unknown.setGravity(Gravity.CENTER);
   cell.addView(unknown,new LinearLayout.LayoutParams(Ui.dp(this,40),Ui.dp(this,39)));
  }
  TextView id=Ui.text(this,row.optString("id")+(approved?" ✓":("ÉTUDE_SANS_CERTIFICATION".equals(row.optString("type_strength"))?" · étude":" · proposé")),10,false);
  id.setGravity(Gravity.CENTER);id.setSingleLine(true);
  cell.addView(id,new LinearLayout.LayoutParams(-1,Ui.dp(this,26)));
  cell.setContentDescription(row.optString("id")+" · "+type+
      (approved?" · vérifiée":" · proposition non certifiée"));
  cell.setFocusable(true);cell.setClickable(true);
  cell.setOnClickListener(v->{active=row;seriesOrdinal=0;referenceWindow=0;alternanceRole=0;centreSegment=2;comparisonExpanded=false;comparisonPane=0;seriesCompare=false;remembered.clear();showActive();});
  relationButtons.addView(cell,new LinearLayout.LayoutParams(Ui.dp(this,108),Ui.dp(this,73)));
 }
 private void showActive(){
  if(active==null){showPlainPage(page);return;}
  setActiveGlyph(active);
  strip.setVisibility(View.VISIBLE);
  JSONArray ranges=refs(active);
  if(ranges==null||ranges.length()==0){returnToPage();return;}
  seriesOrdinal=Math.max(0,Math.min(ranges.length()-1,seriesOrdinal));
  referenceWindow=Math.max(0,Math.min(ranges.length()-1,referenceWindow));
  if(isFourRoleAlternance(ranges)){showFourRoleAlternance(ranges);return;}
  release();
  boolean series="REPETITION".equals(active.optString("type"))
      &&ranges.length()>2;
  boolean pair=!series||seriesCompare;
  int begin=series?seriesOrdinal:referenceWindow;
  // Refrain 31/31 still compares with occurrence 1/31 (not an empty right pane).
  int secondary=series?(begin+1)%ranges.length():begin+1;
  int requested=pair&&ranges.length()>1&&secondary<ranges.length()?2:1;
  // A single folio suffices only when both ranges share its exact page.
  boolean samePage=requested==2&&samePhysicalPage(
      ranges.optJSONObject(begin),ranges.optJSONObject(secondary));
  boolean canExpand=requested==2&&!samePage;
  int count=samePage?1:canExpand&&comparisonExpanded?1:requested;
  visible=new CorrespondenceQcfSurface[count];
  visiblePositions=new int[count];
  for(int i=0;i<count;i++){
   int logicalPane=canExpand&&comparisonExpanded?Math.max(0,Math.min(1,comparisonPane)):i;
   int position=logicalPane==0?begin:secondary;
   visiblePositions[i]=position;
   JSONObject a=ranges.optJSONObject(position);
   if(a==null)continue;
   CorrespondenceDocument.Focus focus=samePage
       ?mergedFocus(a,ranges.optJSONObject(secondary)):focus(a);
   if(!samePage){
    CorrespondenceDocument.Focus certified=approvedFocus(logicalPane,position,series);
    if(certified!=null)focus=certified;
   }
   if(focus==null)continue;
   LinearLayout column=Ui.column(this);
   String prefix=requested==2?(logicalPane==0?"A · ":"B · "):"";
   TextView where=Ui.text(this,prefix+ref(a),11,true);
   where.setGravity(Gravity.CENTER);
   if(canExpand){
    final int chosen=logicalPane;
    where.setFocusable(true);where.setClickable(true);
    where.setContentDescription("Agrandir ou changer le folio "+prefix+ref(a));
    where.setOnClickListener(v->{
     if(comparisonExpanded)comparisonPane=1-comparisonPane;
     else {comparisonPane=chosen;comparisonExpanded=true;}
     showActive();
    });
   }
   column.addView(where,new LinearLayout.LayoutParams(-1,Ui.dp(this,27)));
   CorrespondenceQcfSurface surface=new CorrespondenceQcfSurface(this);
   visible[i]=surface;
   column.addView(surface,new LinearLayout.LayoutParams(-1,0,1));
   folios.addView(column,new LinearLayout.LayoutParams(0,-1,1));
   CorrespondenceQcfSurface.State saved=remembered.get(position);
   surface.show(focus,saved!=null&&saved.page>=focus.firstPage&&saved.page<=focus.lastPage?saved:null);
  }
  location.setText(active.optString("id")+" · "+(series?"Refrain":"Comparaison"));
  String type=active.optString("type","Non établi");
  boolean approved=runtime!=null&&runtime.find(active.optString("id"))!=null;
  shortLink.setText(active.optString("id")+" · "+type+(approved?" · vérifiée":" · proposé"));
  caption.setText("R033".equals(active.optString("id"))
      ?"Āyat al-Kursī : proposition de centre · mots sans géométrie certifiée"
      :"R032".equals(active.optString("id"))
       ?"Structure proposée · cinq membres · centre 2:147–148"
       :active.optString("title")+" · "+
        (series?(seriesOrdinal+1)+"/"+ranges.length():"folio(s) QCF originaux"));
  if(isCentreQibla())renderCentreRail();
  else if(series)renderOccurrenceRail(ranges);
  else renderComparisonRail(ranges,canExpand);
 }
 /** Four QCF verses (A–B–A′–B′) are independent, never falsely
  * merged into one pair of generic gray blocks. Their type remains proposed. */
 private boolean isFourRoleAlternance(JSONArray ranges){
  if(active==null||!"ALTERNANCE".equals(active.optString("type"))
      ||ranges==null||ranges.length()!=2)return false;
  JSONObject a=ranges.optJSONObject(0),b=ranges.optJSONObject(1);
  return a!=null&&b!=null&&a.optInt("end")==a.optInt("start")+1
     &&b.optInt("end")==b.optInt("start")+1
     &&b.optInt("start")==a.optInt("end")+1&&samePhysicalPage(a,b);
 }
 private void showFourRoleAlternance(JSONArray ranges){
  setActiveGlyph(active);
  alternanceRole=Math.max(0,Math.min(3,alternanceRole));
  release();
  JSONObject a=ranges.optJSONObject(0),b=ranges.optJSONObject(1);
  JSONObject current=alternanceRole<2?a:b;
  if(current==null){returnToPage();return;}
  int surah=current.optInt("surah"),verse=current.optInt("start")+alternanceRole%2;
  if(surah<1||surah>114||verse<1||verse>QuranCanon.INSTANCE.ayahCount(surah)){
   returnToPage();return;
  }
  int p=geometry.pageForVerse(new VerseRef(surah,verse));
  int before=verse>1?verse-1:0,after=verse<QuranCanon.INSTANCE.ayahCount(surah)?verse+1:0;
  CorrespondenceDocument.Focus focus=new CorrespondenceDocument.Focus(surah,verse,verse,
      after,before,p,p,p,true);
  CorrespondenceQcfSurface view=new CorrespondenceQcfSurface(this);
  visible=new CorrespondenceQcfSurface[]{view};
  folios.addView(view,new LinearLayout.LayoutParams(0,-1,1));
  view.show(focus,null);
  location.setText(active.optString("id")+" · "+ref(current));
  shortLink.setText("Alternance · "+ref(a)+" ↔ "+ref(b)+" · proposé");
  caption.setText("Lecture hypothétique A–B–A′–B′ · "+surah+":"+verse);
  relationButtons.removeAllViews();
  String[] labels={"A","B","A′","B′"};
  for(int i=0;i<4;i++){
   final int next=i;
   LinearLayout cell=Ui.column(this);cell.setGravity(Gravity.CENTER);
   TextView letter=Ui.text(this,labels[i],14,i==alternanceRole);letter.setGravity(Gravity.CENTER);
   TextView number=Ui.text(this,surah+":"+(a.optInt("start")+i),10,false);number.setGravity(Gravity.CENTER);
   cell.addView(letter);cell.addView(number);
   if(i==alternanceRole){
    GradientDrawable ring=new GradientDrawable();ring.setColor(Ui.PAPER);
    ring.setStroke(Ui.dp(this,2),Ui.INK);ring.setCornerRadius(Ui.dp(this,22));
    cell.setBackground(ring);
   }
   cell.setFocusable(true);cell.setClickable(true);
   cell.setOnClickListener(v->{alternanceRole=next;showFourRoleAlternance(ranges);});
   relationButtons.addView(cell,new LinearLayout.LayoutParams(0,Ui.dp(this,72),1));
  }
 }
 private void setActiveGlyph(JSONObject item){
  if(selectedGlyph==null)return;
  if(item==null){selectedGlyph.setVisibility(View.GONE);return;}
  String type=item.optString("type","");
  if(!classified(type)){selectedGlyph.setVisibility(View.GONE);return;}
  selectedGlyph.setImageResource(CorrespondenceScreen.icon(CorrespondenceModel.Type.valueOf(type)));
  selectedGlyph.setImageTintList(null);
  selectedGlyph.setContentDescription(type+" · "+item.optString("id")
      +" · "+(runtime!=null&&runtime.find(item.optString("id"))!=null?"vérifiée":"proposée"));
  selectedGlyph.setVisibility(View.VISIBLE);
 }
 private boolean samePhysicalPage(JSONObject a,JSONObject b){
  if(a==null||b==null||a.optInt("surah")!=b.optInt("surah"))return false;
  try{
   int pa=geometry.pageForVerse(new VerseRef(a.optInt("surah"),a.optInt("start")));
   int qa=MawdudiMapModel.lastPageForVerse(new VerseRef(a.optInt("surah"),a.optInt("end")),geometry);
   int pb=geometry.pageForVerse(new VerseRef(b.optInt("surah"),b.optInt("start")));
   int qb=MawdudiMapModel.lastPageForVerse(new VerseRef(b.optInt("surah"),b.optInt("end")),geometry);
   return pa==qa&&pa==pb&&pa==qb;
  }catch(Exception invalid){return false;}
 }
 private CorrespondenceDocument.Focus focus(JSONObject row){
  if(row==null)return null;
  int s=row.optInt("surah"),a=row.optInt("start"),b=row.optInt("end");
  if(s<1||s>114||a<1||b<a||b>QuranCanon.INSTANCE.ayahCount(s))return null;
  int fp=geometry.pageForVerse(new VerseRef(s,a));
  int lp=MawdudiMapModel.lastPageForVerse(new VerseRef(s,b),geometry);
  int before=a>1?a-1:0,after=b<QuranCanon.INSTANCE.ayahCount(s)?b+1:0;
  if(isCentreQibla()){
   // Farrin 2010, five verse-level members of 2:142-152 (proposed).
   int[][] members={{142,143},{144,146},{147,148},{149,150},{151,152}};
   int[] selected=members[Math.max(0,Math.min(members.length-1,centreSegment))];
   int centreFirstPage=geometry.pageForVerse(new VerseRef(2,selected[0]));
   int centreLastPage=MawdudiMapModel.lastPageForVerse(new VerseRef(2,selected[1]),geometry);
   return new CorrespondenceDocument.Focus(2,selected[0],selected[1],0,0,
       centreFirstPage,centreLastPage,centreFirstPage,true);
  }
  if(active!=null&&"CENTRE".equals(active.optString("type"))){
   // A whole-ayah gray polygon is NEVER evidence for an intra-ayah centre.
   // Only an independently certified relation with exact QCF word boxes may
   // request segment-specific highlighting, otherwise preserve the real folio.
   CorrespondenceModel.Relation approved=runtime==null?null:runtime.find(active.optString("id"));
   if(approved!=null&&approved.type==CorrespondenceModel.Type.CENTRE
       &&runtime.geometry!=null&&CorrespondenceDocument.displayable(approved,runtime.geometry))
    return CorrespondenceDocument.focus(approved,0,0,runtime.geometry);
   return new CorrespondenceDocument.Focus(s,a,b,after,before,fp,lp,fp,false);
  }
  if(active!=null&&("R011".equals(active.optString("id"))||"R053".equals(active.optString("id")))){
   // The repeated unit is an internal formula, not an entire identical ayah.
   return new CorrespondenceDocument.Focus(s,a,b,after,before,fp,lp,fp,false);
  }
  return new CorrespondenceDocument.Focus(s,a,b,after,before,fp,lp,fp,true);
 }
 /** The trusted runtime's exact anchors outrank the documentary verse range.
  * This preserves existing word-level limits and source checks in approved links. */
 private CorrespondenceDocument.Focus approvedFocus(int pane,int occurrence,boolean series){
  if(active==null||runtime==null||runtime.geometry==null)return null;
  CorrespondenceModel.Relation relation=runtime.find(active.optString("id"));
  if(relation==null||!CorrespondenceDocument.displayable(relation,runtime.geometry))return null;
  try {
   if(relation.mode==CorrespondenceModel.Mode.SERIES&&series
       &&occurrence>=0&&occurrence<relation.anchors.size())
    return CorrespondenceDocument.focus(relation,0,occurrence,runtime.geometry);
   if(relation.mode==CorrespondenceModel.Mode.TWO_PAGES&&pane<2)
    return CorrespondenceDocument.focus(relation,pane,0,runtime.geometry);
   if(relation.mode==CorrespondenceModel.Mode.INTERNAL&&pane==0)
    return CorrespondenceDocument.focus(relation,0,0,runtime.geometry);
  }catch(RuntimeException invalid){return null;}
  return null;
 }
 private CorrespondenceDocument.Focus mergedFocus(JSONObject a,JSONObject b){
  CorrespondenceDocument.Focus first=focus(a),second=focus(b);
  if(first==null||second==null||!samePhysicalPage(a,b))return first;
  CorrespondenceDocument.Focus combined=new CorrespondenceDocument.Focus(
      first.surah,Math.min(first.first,second.first),Math.max(first.last,second.last),
      0,0,first.firstPage,first.lastPage,first.initialPage,true);
  if(runtime!=null&&runtime.geometry!=null){
   List<CorrespondenceModel.VerseRange> discrete=new ArrayList<>();
   discrete.add(new CorrespondenceModel.VerseRange(first.surah,first.first,first.last,runtime.geometry));
   discrete.add(new CorrespondenceModel.VerseRange(second.surah,second.first,second.last,runtime.geometry));
   combined.focalRanges=discrete;
  }
  return combined;
 }
 private void release(){
  rememberViews();
  if(visible!=null)for(CorrespondenceQcfSurface view:visible)if(view!=null)view.destroy();
  visible=null;visiblePositions=null;folios.removeAllViews();
 }
 private void rememberViews(){
  if(active==null||visible==null)return;
  JSONArray r=refs(active);if(r==null)return;
  for(int i=0;i<visible.length;i++)if(visible[i]!=null){
   int position=visiblePositions!=null&&i<visiblePositions.length?visiblePositions[i]:0;
   remembered.put(position,visible[i].state());
  }
 }
 private void returnToPage(){
  release();remembered.clear();showPlainPage(page);
 }
 private void renderOccurrenceRail(JSONArray ranges){
  relationButtons.removeAllViews();
  addRailAction("‹",()->{seriesOrdinal=Math.max(0,seriesOrdinal-1);showActive();});
  int visibleCount=Math.min(5,ranges.length()),first=Math.max(0,Math.min(ranges.length()-visibleCount,seriesOrdinal-2));
  for(int i=first;i<first+visibleCount;i++){
   final int next=i;
   LinearLayout node=Ui.column(this);node.setGravity(Gravity.CENTER);
   TextView counter=Ui.text(this,Integer.toString(i+1),11,true);counter.setGravity(Gravity.CENTER);
   TextView where=Ui.text(this,ref(ranges.optJSONObject(i)),10,false);where.setGravity(Gravity.CENTER);
   node.addView(counter);node.addView(where);
   if(i==seriesOrdinal){
    GradientDrawable outline=new GradientDrawable();outline.setColor(Ui.PAPER);
    outline.setStroke(Ui.dp(this,2),Ui.INK);outline.setCornerRadius(Ui.dp(this,24));
    node.setBackground(outline);
   }
   node.setOnClickListener(v->{seriesOrdinal=next;showActive();});
   relationButtons.addView(node,new LinearLayout.LayoutParams(Ui.dp(this,73),Ui.dp(this,67)));
  }
  addRailAction("›",()->{seriesOrdinal=Math.min(ranges.length()-1,seriesOrdinal+1);showActive();});
  addRailAction(seriesCompare?"1 QCF":"2 QCF",()->{seriesCompare=!seriesCompare;showActive();});
 }
 private void renderComparisonRail(JSONArray ranges,boolean expandable){
  relationButtons.removeAllViews();
  addRailAction("‹",()->{referenceWindow=Math.max(0,referenceWindow-1);showActive();});
  TextView label=Ui.text(this,"Références "+(referenceWindow+1)+"–"+
      Math.min(ranges.length(),referenceWindow+2)+"/"+ranges.length(),12,true);
  label.setGravity(Gravity.CENTER);
  relationButtons.addView(label,new LinearLayout.LayoutParams(Ui.dp(this,195),Ui.dp(this,65)));
  addRailAction("›",()->{referenceWindow=Math.min(Math.max(0,ranges.length()-2),referenceWindow+1);showActive();});
  if(expandable){
   addRailAction(comparisonExpanded?"2 folios":"Agrandir",()->{
    comparisonExpanded=!comparisonExpanded;comparisonPane=0;showActive();
   });
   if(comparisonExpanded)addRailAction("A ↔ B",()->{comparisonPane=1-comparisonPane;showActive();});
  }
 }
 private boolean isCentreQibla(){return active!=null&&"R032".equals(active.optString("id"));}
 private void renderCentreRail(){
  relationButtons.removeAllViews();
  String[] stages={"A","B","C","B′","A′"};
  String[] bounds={"142–143","144–146","147–148","149–150","151–152"};
  for(int i=0;i<5;i++){
   final int selected=i;
   LinearLayout node=Ui.column(this);node.setGravity(Gravity.CENTER);
   TextView letter=Ui.text(this,stages[i],16,true);letter.setGravity(Gravity.CENTER);
   TextView verses=Ui.text(this,bounds[i],10,false);verses.setGravity(Gravity.CENTER);
   node.addView(letter);node.addView(verses);
   if(i==centreSegment){
    GradientDrawable border=new GradientDrawable();border.setColor(Ui.PAPER);
    border.setStroke(Ui.dp(this,2),Ui.INK);border.setCornerRadius(Ui.dp(this,24));
    node.setBackground(border);
   }
   node.setFocusable(true);node.setClickable(true);
   node.setContentDescription(stages[i]+" · versets "+bounds[i]+(i==2?" · centre proposé":" · membre proposé"));
   node.setOnClickListener(v->{centreSegment=selected;remembered.clear();showActive();});
   relationButtons.addView(node,new LinearLayout.LayoutParams(Ui.dp(this,86),Ui.dp(this,68)));
  }
 }
 private void addRailAction(String label,Runnable action){
  TextView item=Ui.text(this,label,14,true);
  item.setGravity(Gravity.CENTER);item.setContentDescription(label);
  item.setClickable(true);item.setFocusable(true);item.setOnClickListener(v->action.run());
  relationButtons.addView(item,new LinearLayout.LayoutParams(Ui.dp(this,60),Ui.dp(this,68)));
 }
 private void sources(){
  if(active==null){
   new AlertDialog.Builder(this).setTitle("Correspondances")
       .setMessage("Les pictogrammes signalent les types proposés, pas une certification. "+
           "Le Muṣḥaf reste la référence. Les 110 dossiers sont conservés sans changement.")
       .setPositiveButton("Fermer",null).show();return;
  }
  boolean approved=runtime!=null&&runtime.find(active.optString("id"))!=null;
  String docStatus=active.optString("documentary_status","");
  String message=active.optString("title")+"\n"+
      (approved?"Relation vérifiée.":
       ("ÉCARTÉ".equals(docStatus)?"Rapprochement documentaire écarté.":
        "Classification littéraire proposée, non certifiée."))+
      "\n\n"+active.optString("reason")+
      "\n\nRéférence : "+active.optString("source")+
      "\n"+active.optString("source_url")+
      "\n\nStatut documentaire : "+docStatus+
      "\nLimites : "+active.optString("focus_rationale");
  new AlertDialog.Builder(this).setTitle(active.optString("id")+" · Sources")
      .setMessage(message).setPositiveButton("Fermer",null).show();
 }
 private void findDossier(){
  EditText field=new EditText(this);
  field.setSingleLine(true);field.setHint("Page 1–604 ou dossier R007");
  new AlertDialog.Builder(this).setTitle("Aller à une page ou à une relation")
      .setView(field)
      .setNegativeButton("Annuler",null)
      .setPositiveButton("Aller",(dialog,which)->{
       String value=field.getText().toString().trim().toUpperCase(Locale.ROOT);
       if(value.matches("R[0-9]{3}")){
        for(JSONObject row:historic)if(value.equals(row.optString("id"))){
         active=row;seriesOrdinal=0;referenceWindow=0;seriesCompare=false;showActive();return;
        }
       }else if(value.matches("[0-9]{1,3}")){
        int next=Integer.parseInt(value);if(next>=1&&next<=604){showPlainPage(next);return;}
       }
       new AlertDialog.Builder(this).setMessage("Référence introuvable.")
           .setPositiveButton("Fermer",null).show();
      }).show();
 }
 @Override protected void onSaveInstanceState(Bundle state){
  state.putInt("folio",page);
  if(active!=null)state.putString("relation",active.optString("id"));
  state.putInt("occurrence",seriesOrdinal);
  state.putInt("referenceWindow",referenceWindow);
  state.putInt("alternanceRole",alternanceRole);
  state.putBoolean("seriesCompare",seriesCompare);
  state.putBoolean("comparisonExpanded",comparisonExpanded);
  state.putInt("comparisonPane",comparisonPane);
  state.putInt("centreSegment",centreSegment);
  super.onSaveInstanceState(state);
 }
 @Override protected void onDestroy(){release();super.onDestroy();}
 @Override public void onBackPressed(){
  if(active!=null)returnToPage();else super.onBackPressed();
 }
}
