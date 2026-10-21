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
 private static final String ASSET="correspondences/historical_110_v1.json";
 private static final String SHA="b46c0e7d7800cbb1cfe34c72a39328339608d9b4e0ede5397dcd780de32aa8a2";
 private final List<JSONObject> historic=new ArrayList<>();
 private final Map<Integer,List<JSONObject>> byPage=new HashMap<>();
 private final Map<Integer,CorrespondenceQcfSurface.State> remembered=new HashMap<>();
 private GeometryRepository geometry;
 private CorrespondenceRuntime runtime;
 private TextView location,caption,shortLink;
 private LinearLayout folios,relationButtons;
 private HorizontalScrollView strip;
 private CorrespondenceQcfSurface[] visible;
 private int page,seriesOrdinal,referenceWindow;
 private boolean seriesCompare;
 private JSONObject active;

 @Override protected void onCreate(Bundle saved){
  super.onCreate(saved);
  getWindow().setFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN,
      android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN);
  try{
   geometry=GeometryRepository.get(this);
   runtime=CorrespondenceRuntime.load(this);
   loadIndex();
  }catch(Exception badIndex){
   // A missing or altered PRIVATE directory must not be replaced with inferred links.
   historic.clear();byPage.clear();
  }
  int previous=getSharedPreferences("hifz_study",MODE_PRIVATE).getInt("page",1);
  page=clamp(saved==null?getIntent().getIntExtra(StudyReaderActivity.EXTRA_JUMP_PAGE,previous)
      :saved.getInt("folio",previous));
  String id=saved==null?null:saved.getString("relation");
  if(id!=null)for(JSONObject entry:historic)if(id.equals(entry.optString("id"))){active=entry;break;}
  seriesOrdinal=saved==null?0:saved.getInt("occurrence",0);
  referenceWindow=saved==null?0:saved.getInt("referenceWindow",0);
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
  if(!"HAAFIDH_CORRESPONDENCE_HISTORICAL_110_DIRECTORY_V1".equals(root.getString("schema"))
      ||root.getInt("count")!=110||rows.length()!=110)
   throw new IllegalArgumentException("Wrong historical registry");
  for(int i=0;i<rows.length();i++){
   JSONObject e=rows.getJSONObject(i);
   if(!String.format(Locale.ROOT,"R%03d",i+1).equals(e.getString("id")))
    throw new IllegalArgumentException("Non-canonical dossier");
   historic.add(e);
   JSONArray ranges=refs(e);if(ranges==null)continue;
   for(int j=0;j<ranges.length();j++){
    JSONObject r=ranges.optJSONObject(j);if(r==null)continue;
    int surah=r.optInt("surah"),first=r.optInt("start"),last=r.optInt("end");
    if(surah<1||surah>114||first<1||last<first
        ||last>QuranCanon.INSTANCE.ayahCount(surah))continue;
    int fp=geometry.pageForVerse(new VerseRef(surah,first));
    int lp=MawdudiMapModel.lastPageForVerse(new VerseRef(surah,last),geometry);
    if(fp<1||lp>604||fp>lp)continue;
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

  shortLink=Ui.text(this,"",12,false);shortLink.setGravity(Gravity.CENTER);
  shortLink.setPadding(Ui.dp(this,5),Ui.dp(this,6),Ui.dp(this,5),Ui.dp(this,6));
  root.addView(shortLink,new LinearLayout.LayoutParams(-1,Ui.dp(this,42)));
  strip=new HorizontalScrollView(this);strip.setHorizontalScrollBarEnabled(false);
  strip.setFillViewport(false);
  relationButtons=Ui.row(this);relationButtons.setGravity(Gravity.CENTER_VERTICAL);
  strip.addView(relationButtons,new android.widget.FrameLayout.LayoutParams(-2,Ui.dp(this,76)));
  root.addView(strip,new LinearLayout.LayoutParams(-1,Ui.dp(this,76)));

  folios=Ui.row(this);folios.setGravity(Gravity.CENTER);
  root.addView(folios,new LinearLayout.LayoutParams(-1,0,1));
  caption=Ui.text(this,"",11,false);caption.setGravity(Gravity.CENTER);
  caption.setPadding(Ui.dp(this,8),Ui.dp(this,3),Ui.dp(this,8),Ui.dp(this,5));
  root.addView(caption,new LinearLayout.LayoutParams(-1,Ui.dp(this,39)));
  setContentView(root);
  if(active==null)showPlainPage(page);else showActive();
 }
 private void showPlainPage(int wanted){
  page=clamp(wanted);
  active=null;seriesCompare=false;
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
  List<JSONObject> links=byPage.get(page);
  if(links==null||links.isEmpty()){
   TextView empty=Ui.text(this,"Aucune correspondance documentée sur cette page",11,false);
   empty.setPadding(Ui.dp(this,12),0,0,0);relationButtons.addView(empty);
   shortLink.setText("Page "+page+" · aucune relation répertoriée");
   return;
  }
  shortLink.setText("Page "+page+" · "+links.size()+" relation"+(links.size()>1?"s":""));
  for(JSONObject row:links)addRelationNode(row);
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
  TextView id=Ui.text(this,row.optString("id")+(approved?" ✓":" · proposé"),10,false);
  id.setGravity(Gravity.CENTER);id.setSingleLine(true);
  cell.addView(id,new LinearLayout.LayoutParams(-1,Ui.dp(this,26)));
  cell.setContentDescription(row.optString("id")+" · "+type+
      (approved?" · vérifiée":" · proposition non certifiée"));
  cell.setFocusable(true);cell.setClickable(true);
  cell.setOnClickListener(v->{active=row;seriesOrdinal=0;referenceWindow=0;seriesCompare=false;showActive();});
  relationButtons.addView(cell,new LinearLayout.LayoutParams(Ui.dp(this,108),Ui.dp(this,73)));
 }
 private void showActive(){
  if(active==null){showPlainPage(page);return;}
  JSONArray ranges=refs(active);
  if(ranges==null||ranges.length()==0){returnToPage();return;}
  seriesOrdinal=Math.max(0,Math.min(ranges.length()-1,seriesOrdinal));
  referenceWindow=Math.max(0,Math.min(ranges.length()-1,referenceWindow));
  release();
  boolean series="REPETITION".equals(active.optString("type"))
      &&ranges.length()>2;
  boolean pair=!series||seriesCompare;
  int begin=series?seriesOrdinal:referenceWindow;
  int end=pair?Math.min(ranges.length(),begin+2):begin+1;
  if(end<=begin){returnToPage();return;}
  // An internal relation or two verses on one physical folio needs only one Muṣḥaf.
  boolean samePage=end-begin==2&&samePhysicalPage(ranges.optJSONObject(begin),ranges.optJSONObject(begin+1));
  int count=samePage?1:end-begin;
  visible=new CorrespondenceQcfSurface[count];
  for(int i=0;i<count;i++){
   int position=begin+i;
   JSONObject a=ranges.optJSONObject(position);
   if(a==null)continue;
   CorrespondenceDocument.Focus focus=samePage
       ?mergedFocus(a,ranges.optJSONObject(begin+1)):focus(a);
   if(focus==null)continue;
   LinearLayout column=Ui.column(this);
   TextView where=Ui.text(this,(count>1?(i==0?"A · ":"B · "):"")+ref(a),11,true);
   where.setGravity(Gravity.CENTER);
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
  caption.setText(("CENTRE".equals(active.optString("type"))||"R033".equals(active.optString("id")))&&!approved
      ?"Centre interne : mots non certifiés · aucun faux surlignage"
      :active.optString("title")+" · "+
       (series?(seriesOrdinal+1)+"/"+ranges.length():"folio(s) QCF originaux"));
  if(series)renderOccurrenceRail(ranges);else renderComparisonRail(ranges);
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
  if(active!=null&&("CENTRE".equals(active.optString("type")) || "R033".equals(active.optString("id")))){
   // A whole-ayah gray polygon is NEVER evidence for an intra-ayah centre.
   // Only an independently certified relation with exact QCF word boxes may
   // request segment-specific highlighting, otherwise preserve the real folio.
   CorrespondenceModel.Relation approved=runtime==null?null:runtime.find(active.optString("id"));
   if(approved!=null&&approved.type==CorrespondenceModel.Type.CENTRE
       &&runtime.geometry!=null&&CorrespondenceDocument.displayable(approved,runtime.geometry))
    return CorrespondenceDocument.focus(approved,0,0,runtime.geometry);
   return new CorrespondenceDocument.Focus(s,a,b,after,before,fp,lp,fp,false);
  }
  return new CorrespondenceDocument.Focus(s,a,b,after,before,fp,lp,fp,true);
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
  visible=null;folios.removeAllViews();
 }
 private void rememberViews(){
  if(active==null||visible==null)return;
  JSONArray r=refs(active);if(r==null)return;
  int initial="REPETITION".equals(active.optString("type"))&&r.length()>2?seriesOrdinal:referenceWindow;
  for(int i=0;i<visible.length;i++)if(visible[i]!=null)
   remembered.put(initial+i,visible[i].state());
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
 private void renderComparisonRail(JSONArray ranges){
  relationButtons.removeAllViews();
  addRailAction("‹",()->{referenceWindow=Math.max(0,referenceWindow-1);showActive();});
  TextView label=Ui.text(this,"Références "+(referenceWindow+1)+"–"+
      Math.min(ranges.length(),referenceWindow+2)+"/"+ranges.length(),12,true);
  label.setGravity(Gravity.CENTER);
  relationButtons.addView(label,new LinearLayout.LayoutParams(Ui.dp(this,195),Ui.dp(this,65)));
  addRailAction("›",()->{referenceWindow=Math.min(Math.max(0,ranges.length()-2),referenceWindow+1);showActive();});
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
  String message=active.optString("title")+"\n"+
      (approved?"Relation vérifiée.":"Classification proposée, non certifiée.")+
      "\n\n"+active.optString("reason")+
      "\n\nSource : "+active.optString("source");
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
  state.putBoolean("seriesCompare",seriesCompare);
  super.onSaveInstanceState(state);
 }
 @Override protected void onDestroy(){release();super.onDestroy();}
 @Override public void onBackPressed(){
  if(active!=null)returnToPage();else super.onBackPressed();
 }
}
