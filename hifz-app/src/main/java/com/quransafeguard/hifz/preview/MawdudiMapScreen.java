package com.quransafeguard.hifz.preview;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.*;
import android.widget.*;
import com.quransafeguard.hifz.core.VerseRef;
import org.json.*;
import java.util.*;

/** One linear graphical axis and one genuine QCF page. No Tafsir or Hifz state access. */
final class MawdudiMapScreen implements MushafView.Listener {
 private final Activity activity;
 private final GeometryRepository geometry;
 private final MawdudiPassageIndex index;
 private final AyahThemesCueIndex titles;
 private final MawdudiMapData macros;
 private final JSONArray subnodes;
 private int surah,selected,page,focusStart,focusEnd;
 private boolean ready,overview=true;
 private int activeMacro;
 private java.util.List<MawdudiPassageIndex.Passage> passages;
 private MushafView mushaf;
 private Graph graph;
 private OverviewGraph overviewGraph;
 private HorizontalScrollView axis;
 private Button previousPageButton,nextPageButton;
 private LinearLayout pageActions;
 private TextView heading,pageLabel;
 private LinearLayout icons;
 private Dialog popup;
 private MapJalalaynDialog commentary;
 MawdudiMapScreen(Activity h,GeometryRepository g,MawdudiPassageIndex i,AyahThemesCueIndex t,
                   MawdudiMapData m,int s,int a,Bundle saved)throws Exception {
  if(t==null)throw new IllegalStateException("private titles unavailable");
  activity=h;geometry=g;index=i;titles=t;macros=m;
  JSONArray sub=new JSONArray();
  try {
   JSONObject special=new JSONObject(MawdudiMapData.read(h,"ayah_themes_private/special_27.json"));
   if(!"s027a067b082".equals(special.getString("parent_passage_id"))||special.getInt("canonical_passage_count_increment")!=0)
    throw new IllegalStateException("wrong display-only parent");
   sub=special.getJSONArray("display_only_subnodes");int[][] bounds={{67,75},{76,81},{82,82}};
   if(sub.length()!=3)throw new IllegalStateException("wrong approved subnode count");
   for(int k=0;k<3;k++)if(sub.getJSONObject(k).getInt("start")!=bounds[k][0]||sub.getJSONObject(k).getInt("end")!=bounds[k][1]||sub.getJSONObject(k).getString("title").trim().isEmpty())throw new IllegalStateException("unapproved visual subnode");
  } catch(Exception missing) {if(BuildConfig.AYAH_THEMES_FINAL_PRIVATE)throw missing;}
  subnodes=sub;
  surah=saved==null?s:saved.getInt("map114Surah",s);
  int ayah=saved==null?a:saved.getInt("map114Ayah",a);
  choose(surah,ayah);
  if(saved!=null) {
   overview=saved.getBoolean("map114Overview",true);
   int restored=saved.getInt("map114MacroSelected",activeMacro);
   if(restored>=0&&restored<macros.forSurah(surah).size())activeMacro=restored;
   page=MawdudiMapModel.clampPage(current(),saved.getInt("map114Page",page),geometry);
   int first=saved.getInt("map114FocusStart",focusStart),last=saved.getInt("map114FocusEnd",focusEnd);
   if(current().contains(first)&&current().contains(last)&&first<=last){focusStart=first;focusEnd=last;}
  }
 }
 private MawdudiPassageIndex.Passage current(){return passages.get(selected);}
 private void choose(int s,int ayah) {
  surah=s;passages=index.forSurah(s);MawdudiPassageIndex.Passage p=index.at(s,ayah);
  selected=passages.indexOf(p);page=MawdudiMapModel.firstPage(p,geometry);focusStart=p.firstAyah;focusEnd=p.lastAyah;
  overview=true;
  activeMacro=macroIndex(s,ayah);
 }
 private int macroIndex(int s,int ayah){
  java.util.List<MawdudiMapData.Macro> items=macros.forSurah(s);
  for(int k=0;k<items.size();k++)
   if(ayah>=items.get(k).start&&ayah<=items.get(k).end)return k;
  throw new IllegalArgumentException("Macro coverage missing");
 }
 private MawdudiMapData.Macro macro(){return macros.forSurah(surah).get(activeMacro);}
 private void setLevel(boolean overviewMode){
  overview=overviewMode;
  closeTitle();
  if(commentary!=null){commentary.close();commentary=null;}
  if(axis!=null){
   axis.removeAllViews();
   axis.addView(overview?overviewGraph:graph,
       new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,Ui.dp(activity,78)));
  }
  refresh();
 }
 private void openMacro(int ordinal){
  java.util.List<MawdudiMapData.Macro> items=macros.forSurah(surah);
  if(ordinal<0||ordinal>=items.size())return;
  MawdudiMapData.Macro chosen=items.get(ordinal);
  // Every unit opens at its own first verse, not at the previous arbitrary
  // cursor or an unrelated page. A source passage crossing a unit boundary
  // remains a single unmodified Ayah Themes passage in the canonical index.
  MawdudiPassageIndex.Passage first=index.at(surah,chosen.start);
  selected=passages.indexOf(first);
  // Never split an original Ayah Themes passage at a Maududi boundary.
  // E.g. 2:120–121 remains one passage in both adjoining units.
  focusStart=first.firstAyah;
  focusEnd=first.lastAyah;
  page=geometry.pageForVerse(new VerseRef(surah,chosen.start));
  activeMacro=ordinal;
  setLevel(false);
 }
 private void goBack(){
  if(back())return;
  activity.finish();
 }
 void display() {
  LinearLayout root=Ui.column(activity);root.setBackgroundColor(Ui.PAPER);
  LinearLayout top=Ui.row(activity);top.setGravity(Gravity.CENTER_VERTICAL);
  top.addView(Ui.iconButton(activity,"","Retour",v->goBack()),new LinearLayout.LayoutParams(Ui.dp(activity,42),Ui.dp(activity,44)));
  heading=Ui.text(activity,"",12f,false);heading.setMaxLines(2);heading.setEllipsize(TextUtils.TruncateAt.END);
  heading.setClickable(true);heading.setFocusable(true);heading.setOnClickListener(v->chooseUnit());
  top.addView(heading,new LinearLayout.LayoutParams(0,Ui.dp(activity,44),1));
  top.addView(Ui.iconButton(activity,"","Choisir une sourate",v->QuranSurahNames.showSurahPicker(activity,s->{choose(s,1);setLevel(true);})),new LinearLayout.LayoutParams(Ui.dp(activity,42),Ui.dp(activity,44)));
  root.addView(top);
  axis=new HorizontalScrollView(activity);axis.setHorizontalScrollBarEnabled(false);
  graph=new Graph();overviewGraph=new OverviewGraph();
  axis.addView(overview?overviewGraph:graph,new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,Ui.dp(activity,78)));
  root.addView(axis,new LinearLayout.LayoutParams(-1,Ui.dp(activity,78)));
  mushaf=new MushafView(activity);mushaf.setListener(this);root.addView(mushaf,new LinearLayout.LayoutParams(-1,0,1));
  // The Mushaf remains the largest view. Page arrows only exist on multipage
  // passages; the permanent arrows change *passages*, never pages.
  pageActions=Ui.row(activity);
  pageActions.setGravity(Gravity.CENTER_VERTICAL);
  previousPageButton=Ui.iconButton(activity,"","Page précédente",v->turn(-1));
  pageActions.addView(previousPageButton,new LinearLayout.LayoutParams(Ui.dp(activity,44),Ui.dp(activity,38)));
  pageLabel=Ui.text(activity,"",11f,false);pageLabel.setGravity(Gravity.CENTER);
  pageActions.addView(pageLabel,new LinearLayout.LayoutParams(0,Ui.dp(activity,38),1));
  nextPageButton=Ui.iconButton(activity,"","Page suivante",v->turn(1));
  pageActions.addView(nextPageButton,new LinearLayout.LayoutParams(Ui.dp(activity,44),Ui.dp(activity,38)));
  root.addView(pageActions);
  icons=Ui.row(activity);icons.setGravity(Gravity.CENTER_VERTICAL);
  icons.addView(Ui.iconButton(activity,"","Passage précédent du corpus",v->movePassage(-1)),
      new LinearLayout.LayoutParams(0,Ui.dp(activity,44),1));
  TextView complete=Ui.text(activity,"Aa",16f,true);
  complete.setGravity(Gravity.CENTER);complete.setClickable(true);complete.setFocusable(true);
  complete.setContentDescription("Titre complet du passage Ayah Themes");
  complete.setOnClickListener(v->showCurrentTitle());
  icons.addView(complete,new LinearLayout.LayoutParams(0,Ui.dp(activity,44),1));
  icons.addView(Ui.iconButton(activity,"","Tafsir",v->openCommentary()),
      new LinearLayout.LayoutParams(0,Ui.dp(activity,44),1));
  icons.addView(Ui.iconButton(activity,"","Lecture",v->openReader()),
      new LinearLayout.LayoutParams(0,Ui.dp(activity,44),1));
  icons.addView(Ui.iconButton(activity,"","Passage suivant du corpus",v->movePassage(1)),
      new LinearLayout.LayoutParams(0,Ui.dp(activity,44),1));
  root.addView(icons);
  activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
  activity.setContentView(root);refresh();
 }
 MushafView mushaf(){return mushaf;}
 private void closeTitle(){if(popup!=null&&popup.isShowing())popup.dismiss();popup=null;}
 private void openCommentary(){
  if(overview){openMacro(activeMacro);return;}
  closeTitle();
  if(commentary!=null)commentary.close();
  commentary=new MapJalalaynDialog(activity,surah,focusStart,focusEnd,focusStart);
  commentary.show();
 }
 private void showTitle(String text){if(text==null||text.trim().isEmpty())return;closeTitle();popup=SemanticTitlePopup.showEnglishFull(activity,text,null);SemanticTitlePopup.positionAboveIcons(activity,popup,icons);}
 private void showCurrentTitle(){
  if(overview){openMacro(activeMacro);return;}
  if(surah==27&&focusStart>=67&&focusEnd<=82&&subnodes.length()==3){
   try {for(int k=0;k<subnodes.length();k++){
    JSONObject child=subnodes.getJSONObject(k);
    if(child.getInt("start")==focusStart&&child.getInt("end")==focusEnd){
     showTitle(child.getString("title"));return;
    }
   }}catch(JSONException invalid){return;}
  }
  showTitle(titles.titleForPassage(current().id));
 }
 private void chooseUnit(){
  PopupMenu chooser=new PopupMenu(activity,heading);
  java.util.List<MawdudiMapData.Macro> units=macros.forSurah(surah);
  for(int k=0;k<units.size();k++){
   MawdudiMapData.Macro u=units.get(k);
   chooser.getMenu().add(0,k,k,"Unité "+(k+1)+" · "+surah+":"+u.start+"–"+u.end);
  }
  final int markerOption=100000;
  chooser.getMenu().add(1,markerOption,units.size()+1,
      "Repères U dans le Mushaf : "+(MapUnitMarkers.enabled(activity)?"activés":"désactivés"));
  chooser.setOnMenuItemClickListener(item->{
   if(item.getItemId()==markerOption){
    MapUnitMarkers.toggle(activity);
    if(ready)applyUnitMarkers();
   } else openMacro(item.getItemId());
   return true;
  });
  chooser.show();
 }
 private void movePassage(int direction){
  if(overview){openMacro(activeMacro);return;}
  closeTitle();
  int next=selected+direction;
  while(next>=0&&next<passages.size()&&!graph.inMacro(next))next+=direction;
  if(next<0||next>=passages.size()){
   int target=activeMacro+direction;
   if(target>=0&&target<macros.forSurah(surah).size())openMacro(target);
   return;
  }
  MawdudiPassageIndex.Passage p=passages.get(next);
  selected=next;focusStart=Math.max(p.firstAyah,macro().start);
  focusEnd=Math.min(p.lastAyah,macro().end);
  page=geometry.pageForVerse(new VerseRef(surah,focusStart));
  refresh();
 }
 private void select(int n,int a,int b,String title) {
  selected=n;focusStart=a;focusEnd=b;page=geometry.pageForVerse(new VerseRef(surah,a));refresh();showTitle(title);
 }
 void refresh(){
  if(heading==null)return;
  // The private macro pack currently contains placeholder labels, NOT authentic
  // Maududi headings. Show only verified structural ranges; never invent titles.
  MawdudiMapData.Macro unit=macro();
  String surahLabel=QuranSurahNames.labelFor(surah);
  if(overview){
   heading.setText(surahLabel+" · Unité "+(activeMacro+1)+"/"+macros.forSurah(surah).size()+" · Choisir une unité");
   heading.setContentDescription("Choisir une unité de la sourate");
  } else {
   int within=0,total=0;
   for(int i=0;i<passages.size();i++)if(graph.inMacro(i)){
    total++;if(i<=selected)within++;
   }
   heading.setText(surahLabel+" · Unité "+(activeMacro+1)+"/"+macros.forSurah(surah).size()
       +" · Passage "+within+"/"+total);
   heading.setContentDescription("Changer d'unité · "+heading.getText());
  }
  graph.requestLayout();graph.invalidate();
  overviewGraph.requestLayout();overviewGraph.invalidate();
  axis.post(()->axis.scrollTo(Math.max(0,
      (overview?overviewGraph.position(activeMacro):graph.position(selected))-axis.getWidth()/3),0));
  renderPage();
 }
 private void renderPage(){
  if(!ready)return;
  MawdudiPassageIndex.Passage p=current();page=MawdudiMapModel.clampFocusPage(surah,focusStart,focusEnd,page,geometry);
  java.util.List<VerseRef> visible=new ArrayList<>();
  for(VerseRef v:MawdudiMapModel.highlightedVerses(p,page,geometry))if(v.getAyah()>=focusStart&&v.getAyah()<=focusEnd)visible.add(v);
  java.util.List<String> lines=geometry.lineIdsForVerseRange(new VerseRef(surah,focusStart),new VerseRef(surah,focusEnd));
  mushaf.setHighlightVerses(visible);mushaf.show(page,visible,lines,0,true);
  int startPage=geometry.pageForVerse(new VerseRef(surah,focusStart));
  int endPage=MawdudiMapModel.lastPageForVerse(new VerseRef(surah,focusEnd),geometry);
  boolean multiple=endPage>startPage;
  pageActions.setVisibility(multiple?View.VISIBLE:View.GONE);
  previousPageButton.setEnabled(page>startPage);
  nextPageButton.setEnabled(page<endPage);
  previousPageButton.setAlpha(page>startPage?1f:.25f);
  nextPageButton.setAlpha(page<endPage?1f:.25f);
  pageLabel.setText("Page "+page+" · "+surah+":"+focusStart+"–"+focusEnd);
 }
 private void turn(int d){int next=MawdudiMapModel.clampFocusPage(surah,focusStart,focusEnd,page+d,geometry);if(next==page)return;closeTitle();page=next;renderPage();}
 void openReader(){
  java.util.List<VerseRef> visible=MawdudiMapModel.highlightedVerses(current(),page,geometry);if(visible.isEmpty())return;
  Intent intent=new Intent(activity,StudyReaderActivity.class).putExtra(StudyReaderActivity.EXTRA_JUMP_PAGE,page)
   .putExtra(StudyReaderActivity.EXTRA_JUMP_VERSE,visible.get(0).toString()).putExtra(StudyReaderActivity.EXTRA_MAP_PREVIEW,true)
   .putExtra("map114Surah",surah).putExtra("map114PassageId",current().id).putExtra("map114FocusStart",focusStart).putExtra("map114FocusEnd",focusEnd);
  closeTitle();activity.startActivity(intent);
 }
 void save(Bundle b){b.putBoolean("map114Overview",overview);b.putInt("map114MacroSelected",activeMacro);b.putInt("map114Surah",surah);b.putInt("map114Ayah",current().firstAyah);b.putInt("map114Page",page);b.putInt("map114FocusStart",focusStart);b.putInt("map114FocusEnd",focusEnd); }
 boolean back(){if(commentary!=null&&commentary.isShowing()){commentary.close();commentary=null;return true;}if(popup!=null&&popup.isShowing()){closeTitle();return true;}if(!overview){setLevel(true);return true;}return false;}
 void destroy(){closeTitle();if(commentary!=null){commentary.close();commentary=null;}if(mushaf!=null)mushaf.destroySafely();}
 @Override public void onReady(){ready=true;renderPage();}
 @Override public void onPageShown(int p){
  if(p!=page)return;
  fitMapPage();
 }
 /** Refit only this map's WebView using its *measured* viewport, not CSS 100vh
  * which may refer to the containing Android window and clip QCF line 15.
  * Does not modify the shared Mushaf renderer or its exact SVG coordinates.
  */
 private void fitMapPage(){
  if(mushaf==null)return;
  String js="(function(){var host=document.getElementById('box'),page=document.getElementById('mushaf');"+
   "if(!host||!page)return;var svg=page.querySelector('svg');"+
   "var vb=svg&&svg.viewBox&&svg.viewBox.baseVal;"+
   "var ratio=vb&&vb.width&&vb.height?vb.width/vb.height:345/550;"+
   "var width=Math.max(10,Math.min(host.clientWidth-72,(host.clientHeight-6)*ratio));"+
   "page.style.width=width+'px';page.style.maxWidth='none';page.style.maxHeight=(host.clientHeight-6)+'px';"+
   "})();";
  mushaf.evaluateJavascript(js,ignored->applyUnitMarkers());
 }
 private void applyUnitMarkers(){
  if(mushaf==null||!ready)return;
  MapUnitMarkers.show(mushaf,page,surah,macros.forSurah(surah),geometry);
 }
 @Override public void onError(String error){pageLabel.setText("Muṣḥaf indisponible");}
 @Override public void onPageSwipe(int delta){turn(delta);}
 @Override public void onVerseTap(VerseRef verse){openReader();}
 @Override public void onSurfaceTap(){openReader();}

 /** Level one: source-backed macro boundaries only; no fabricated Maududi titles.
  * A one-unit surah is shown honestly as a complete surah, not a made-up theme.
  * Choosing a unit opens its constituent original Ayah Themes passages.
  */
 private final class OverviewGraph extends View {
  final Paint pen=new Paint(Paint.ANTI_ALIAS_FLAG);
  final int step=Ui.dp(activity,178);
  OverviewGraph(){super(activity);setFocusable(true);}
  int position(int n){return Math.max(0,n)*step;}
  @Override protected void onMeasure(int w,int h){
   setMeasuredDimension(Math.max(step,macros.forSurah(surah).size()*step),Ui.dp(activity,78));
  }
  @Override protected void onDraw(Canvas c){
   java.util.List<MawdudiMapData.Macro> units=macros.forSurah(surah);
   float y=Ui.dp(activity,47);
   pen.setColor(Ui.INK);pen.setStrokeWidth(Ui.dp(activity,1));
   if(units.size()>1)c.drawLine(step/2f,y,Math.max(step/2f,getWidth()-step/2f),y,pen);
   Rect clip=c.getClipBounds();
   for(int k=0;k<units.size();k++){
    float x=step*(k+.5f);
    if(x+step<clip.left||x-step>clip.right)continue;
    MawdudiMapData.Macro m=units.get(k);
    // Genuine thematic *titles* are not in this pack. Numbered discourse
    // boundaries exist only for surahs 3 and 9.
    String label="Unité "+(k+1);
    pen.setTextAlign(Paint.Align.CENTER);
    pen.setTextSize(Ui.dp(activity,12));pen.setColor(Ui.INK);
    c.drawText(label,x,Ui.dp(activity,20),pen);
    pen.setColor(Ui.PAPER);c.drawCircle(x,y,Ui.dp(activity,10),pen);
    pen.setColor(Ui.INK);c.drawCircle(x,y,Ui.dp(activity,k==activeMacro?5:3),pen);
    pen.setTextSize(Ui.dp(activity,10));pen.setColor(Ui.INK);
    c.drawText(surah+":"+m.start+"–"+m.end,x,Ui.dp(activity,71),pen);
   }
  }
  @Override public boolean onTouchEvent(MotionEvent event){
   if(event.getAction()!=MotionEvent.ACTION_UP)return true;
   int i=(int)(event.getX()/step);
   if(i<0||i>=macros.forSurah(surah).size())return false;
   openMacro(i);performClick();return true;
  }
  @Override public boolean performClick(){super.performClick();return true;}
 }
 private final class Graph extends View {
  final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);final int step=Ui.dp(activity,178);
  Graph(){super(activity);setFocusable(true);}
  boolean split(int n){MawdudiPassageIndex.Passage p=passages.get(n);return p.surah==27&&p.firstAyah==67&&p.lastAyah==82&&subnodes.length()==3;}
  boolean inMacro(int n){
   MawdudiPassageIndex.Passage p=passages.get(n);
   MawdudiMapData.Macro m=macro();
   return p.firstAyah<=m.end&&p.lastAyah>=m.start;
  }
  int units(int n){return !inMacro(n)?0:split(n)?3:1;}
  int position(int n){int x=0;for(int k=0;k<n;k++)x+=units(k)*step;return x;}
  @Override protected void onMeasure(int w,int h){setMeasuredDimension(Math.max(1,position(passages.size())),Ui.dp(activity,78));}
  @Override protected void onDraw(Canvas canvas){
   paint.setColor(Ui.INK);paint.setStrokeWidth(Ui.dp(activity,1));float y=Ui.dp(activity,47);
   canvas.drawLine(step/2f,y,Math.max(step/2f,getWidth()-step/2f),y,paint);
   Rect clip=canvas.getClipBounds();
   for(int n=0;n<passages.size();n++)for(int child=0;child<units(n);child++) {
    float x=position(n)+step*(child+.5f);if(x+step<clip.left||x-step>clip.right)continue;
    MawdudiPassageIndex.Passage p=passages.get(n);String title=titles.titleForPassage(p.id);int a=p.firstAyah,b=p.lastAyah;
    try{if(split(n)){JSONObject sub=subnodes.getJSONObject(child);title=sub.getString("title");a=sub.getInt("start");b=sub.getInt("end");}}catch(JSONException invalid){continue;}
    if(title==null)continue;
    paint.setColor(Ui.INK);paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(Ui.dp(activity,12));
    // Two compact original title lines above one unobtrusive point. Full title on tap.
    String first=title,second="";if(paint.measureText(title)>step-16){int cut=paint.breakText(title,true,step-20,null);int space=title.lastIndexOf(' ',cut);if(space>0)cut=space;first=title.substring(0,cut);second=title.substring(cut).trim();}
    canvas.drawText(first,x,Ui.dp(activity,15),paint);
    if(!second.isEmpty())canvas.drawText(TextUtils.ellipsize(second,new android.text.TextPaint(paint),step-16,TextUtils.TruncateAt.END).toString(),x,Ui.dp(activity,29),paint);
    paint.setColor(Ui.PAPER);canvas.drawCircle(x,y,Ui.dp(activity,10),paint);
    paint.setColor(Ui.INK);canvas.drawCircle(x,y,Ui.dp(activity,n==selected?5:3),paint);
    paint.setColor(Ui.INK);paint.setTextSize(Ui.dp(activity,10));canvas.drawText(surah+":"+a+"–"+b,x,Ui.dp(activity,71),paint);
   }
  }
  @Override public boolean onTouchEvent(MotionEvent event){
   if(event.getAction()!=MotionEvent.ACTION_UP)return true;
   float x=event.getX();for(int n=0;n<passages.size();n++)if(x>=position(n)&&x<position(n+1)) {
    MawdudiPassageIndex.Passage p=passages.get(n);int child=Math.min(units(n)-1,(int)((x-position(n))/step));
    try{if(split(n)){JSONObject sub=subnodes.getJSONObject(child);select(n,sub.getInt("start"),sub.getInt("end"),sub.getString("title"));}
     else select(n,p.firstAyah,p.lastAyah,titles.titleForPassage(p.id));}catch(JSONException invalid){return false;}
    performClick();return true;
   }return false;
  }
  @Override public boolean performClick(){super.performClick();return true;}
 }
}
