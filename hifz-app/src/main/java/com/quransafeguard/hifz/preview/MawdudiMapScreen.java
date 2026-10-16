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

/** Sun-shaped BOOX semantic map: sourah -> sourced units -> Ayah Themes passages -> authentic QCF thumbnails. No Hifz mutations. */
final class MawdudiMapScreen implements MushafView.Listener {
 private final Activity activity;
 private final GeometryRepository geometry;
 private final MawdudiPassageIndex index;
 private final AyahThemesCueIndex titles;
 private final MawdudiMapData macros;
 private final JSONArray subnodes;
 private int surah,selected,page,focusStart,focusEnd;
 private boolean ready,overview=true,passageOpen=false;
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
 private TextView passageTitle;
 private MapQcfThumbnails thumbnails;
 private int sunHeight;
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
   passageOpen=saved.getBoolean("map114PassageOpen",false);
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
  if(overviewMode)passageOpen=false;
  closeTitle();
  if(commentary!=null){commentary.close();commentary=null;}
  if(axis!=null){
   axis.removeAllViews();
   axis.addView(overview?overviewGraph:graph,
       new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,sunHeight));
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
  passageOpen=false;
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
  // Screen-width sun remains present at both levels. The details are branches
  // of the selected ray, never an unrelated list or rectangular tile.
  float density=activity.getResources().getDisplayMetrics().density;
  float widthDp=activity.getResources().getDisplayMetrics().widthPixels/density;
  sunHeight=Ui.dp(activity,(int)Math.max(240,Math.min(410,widthDp*0.89f)));
  axis=new HorizontalScrollView(activity);axis.setFillViewport(true);
  axis.setHorizontalScrollBarEnabled(false);
  graph=new Graph();overviewGraph=new OverviewGraph();
  axis.addView(overview?overviewGraph:graph,
      new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,sunHeight));
  root.addView(axis,new LinearLayout.LayoutParams(-1,sunHeight));
  passageTitle=Ui.text(activity,"",13.5f,true);
  passageTitle.setGravity(Gravity.CENTER);
  passageTitle.setMaxLines(3);passageTitle.setEllipsize(TextUtils.TruncateAt.END);
  passageTitle.setOnClickListener(v->showCurrentTitle());
  root.addView(passageTitle,new LinearLayout.LayoutParams(-1,Ui.dp(activity,56)));
  thumbnails=new MapQcfThumbnails(activity,p->onThumbnailPage(p));
  root.addView(thumbnails,new LinearLayout.LayoutParams(-1,Ui.dp(activity,142)));
  mushaf=new MushafView(activity);mushaf.setListener(this);
  root.addView(mushaf,new LinearLayout.LayoutParams(-1,0,1));
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
  if(!passageOpen)return;
  closeTitle();
  if(commentary!=null)commentary.close();
  commentary=new MapJalalaynDialog(activity,surah,focusStart,focusEnd,focusStart);
  commentary.show();
 }
 private void showTitle(String text){if(text==null||text.trim().isEmpty())return;closeTitle();popup=SemanticTitlePopup.showEnglishFull(activity,text,null);SemanticTitlePopup.positionAboveIcons(activity,popup,icons);}
 private void showCurrentTitle(){
  if(overview){openMacro(activeMacro);return;}
  if(!passageOpen)return;
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
  // Passage navigation always retains its canonical source boundaries,
  // including Ayah Themes 2:120–121 across adjacent source units.
  selected=next;focusStart=p.firstAyah;
  focusEnd=p.lastAyah;passageOpen=true;
  page=geometry.pageForVerse(new VerseRef(surah,focusStart));
  refresh();
 }
 private void select(int n,int a,int b,String title) {
  selected=n;focusStart=a;focusEnd=b;passageOpen=true;
  page=geometry.pageForVerse(new VerseRef(surah,a));refresh();showTitle(title);
 }
 void refresh(){
  if(heading==null)return;
  // The private macro pack currently contains placeholder labels, NOT authentic
  // Maududi headings. Show only verified structural ranges; never invent titles.
  MawdudiMapData.Macro unit=macro();
  String surahLabel=QuranSurahNames.labelFor(surah);
  int total=macros.forSurah(surah).size();
  if(overview){
   heading.setText(surahLabel+" · "+total+" unités");
   passageTitle.setText("Touchez un rayon pour ouvrir ses passages");
  }else if(!passageOpen){
   heading.setText(surahLabel+" · Unité "+(activeMacro+1)+"/"+total);
   passageTitle.setText("Unité "+(activeMacro+1)+" · "+surah+":"+unit.start+"–"+unit.end
       +" · "+graph.nodes().size()+" passages");
  }else{
   int within=graph.currentNodeOrdinal();
   int count=graph.nodes().size();
   heading.setText(surahLabel+" · Unité "+(activeMacro+1)+"/"+total+" · Passage "+within+"/"+count);
   passageTitle.setText(surah+":"+focusStart+"–"+focusEnd+" · "+graph.selectedTitle());
  }
  heading.setContentDescription("Changer d'unité · "+heading.getText());
  // When a passage opens, make the QCF page the dominant surface by shrinking
  // the sun, not by asking the reader to zoom in. E-Ink has no transitions.
  int desiredHeight=passageOpen?Math.max(Ui.dp(activity,158),sunHeight/2):sunHeight;
  ViewGroup.LayoutParams vp=axis.getLayoutParams();
  if(vp.height!=desiredHeight){vp.height=desiredHeight;axis.setLayoutParams(vp);}
  View child=axis.getChildAt(0);
  if(child!=null){
   ViewGroup.LayoutParams childParams=child.getLayoutParams();
   if(childParams.height!=desiredHeight){
    childParams.height=desiredHeight;child.setLayoutParams(childParams);
   }
  }
  graph.requestLayout();graph.invalidate();
  overviewGraph.requestLayout();overviewGraph.invalidate();
  renderPage();
 }
 private void renderPage(){
  // The general view never carries a permanently embedded Quran page.
  if(overview){
   passageTitle.setVisibility(View.GONE);thumbnails.setVisibility(View.GONE);
   mushaf.setVisibility(View.GONE);icons.setVisibility(View.GONE);
   pageActions.setVisibility(View.GONE);return;
  }
  passageTitle.setVisibility(View.VISIBLE);
  thumbnails.setVisibility(View.VISIBLE);
  if(!passageOpen){
   mushaf.setVisibility(View.GONE);icons.setVisibility(View.GONE);
   pageActions.setVisibility(View.GONE);
   MawdudiMapData.Macro selectedUnit=macro();
   int first=geometry.pageForVerse(new VerseRef(surah,selectedUnit.start));
   int last=MawdudiMapModel.lastPageForVerse(new VerseRef(surah,selectedUnit.end),geometry);
   thumbnails.showPages(first,last,first);
   return;
  }
  icons.setVisibility(View.VISIBLE);
  mushaf.setVisibility(View.VISIBLE);
  MawdudiPassageIndex.Passage p=current();page=MawdudiMapModel.clampFocusPage(surah,focusStart,focusEnd,page,geometry);
  int first=geometry.pageForVerse(new VerseRef(surah,focusStart));
  int last=MawdudiMapModel.lastPageForVerse(new VerseRef(surah,focusEnd),geometry);
  // On a one-page passage the authentic QCF page is already visible; avoid
  // wasting vertical space and memory on a duplicate miniature.
  boolean multiPage=last>first;
  thumbnails.setVisibility(multiPage?View.VISIBLE:View.GONE);
  if(multiPage)thumbnails.showPages(first,last,page);
  if(!ready)return;
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
 private void onThumbnailPage(int desired){
  if(overview)return;
  if(!passageOpen){
   // A page thumbnail of an opened UNIT resolves to the first original
   // passage on that physical QCF page. Nothing is synthesized or split.
   for(int n=0;n<passages.size();n++){
    MawdudiPassageIndex.Passage p=passages.get(n);
    if(!graph.inMacro(n))continue;
    int first=geometry.pageForVerse(new VerseRef(surah,p.firstAyah));
    int last=MawdudiMapModel.lastPage(p,geometry);
    if(desired>=first&&desired<=last){
     selected=n;focusStart=p.firstAyah;focusEnd=p.lastAyah;
     passageOpen=true;page=desired;refresh();return;
    }
   }return;
  }
  page=MawdudiMapModel.clampFocusPage(surah,focusStart,focusEnd,desired,geometry);
  renderPage();
 }
 private void turn(int d){int next=MawdudiMapModel.clampFocusPage(surah,focusStart,focusEnd,page+d,geometry);if(next==page)return;closeTitle();page=next;renderPage();}
 void openReader(){
  if(overview||!passageOpen)return;
  java.util.List<VerseRef> visible=MawdudiMapModel.highlightedVerses(current(),page,geometry);
  VerseRef jump=null;
  for(VerseRef v:visible)if(v.getAyah()>=focusStart&&v.getAyah()<=focusEnd){jump=v;break;}
  if(jump==null)return;
  Intent intent=new Intent(activity,StudyReaderActivity.class).putExtra(StudyReaderActivity.EXTRA_JUMP_PAGE,page)
   .putExtra(StudyReaderActivity.EXTRA_JUMP_VERSE,jump.toString()).putExtra(StudyReaderActivity.EXTRA_MAP_PREVIEW,true)
   .putExtra("map114Surah",surah).putExtra("map114PassageId",current().id).putExtra("map114FocusStart",focusStart).putExtra("map114FocusEnd",focusEnd);
  closeTitle();activity.startActivity(intent);
 }
 void save(Bundle b){b.putBoolean("map114Overview",overview);b.putBoolean("map114PassageOpen",passageOpen);b.putInt("map114MacroSelected",activeMacro);b.putInt("map114Surah",surah);b.putInt("map114Ayah",current().firstAyah);b.putInt("map114Page",page);b.putInt("map114FocusStart",focusStart);b.putInt("map114FocusEnd",focusEnd); }
 boolean back(){if(commentary!=null&&commentary.isShowing()){commentary.close();commentary=null;return true;}if(popup!=null&&popup.isShowing()){closeTitle();return true;}if(passageOpen){passageOpen=false;refresh();return true;}if(!overview){setLevel(true);return true;}return false;}
 void destroy(){closeTitle();if(commentary!=null){commentary.close();commentary=null;}if(mushaf!=null)mushaf.destroySafely();if(thumbnails!=null)thumbnails.destroy();}
 @Override public void onReady(){ready=true;renderPage();}
 @Override public void onPageShown(int p){
  if(p!=page||!passageOpen)return;
  fitMapPage();
 }
 /** Refit only this map's WebView using its *measured* viewport, not CSS 100vh
  * which may refer to the containing Android window and clip QCF line 15.
  * Does not modify the shared Mushaf renderer or its exact SVG coordinates.
  */
 private void fitMapPage(){
  if(mushaf==null||!passageOpen)return;
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

 /** Layout common to both levels. The stable clockwise ray position is
  * a mnemonic, independent from the physical length of a Maududi unit.
  * A titleless unit is always "Unité N", with its source-verified verse range.
  */
 private float sunCx(int w){return w/2f;}
 private float sunCy(int h){return h*.41f;}
 private float sunRadius(int w,int h){return Math.min(w*.32f,h*.30f);}
 private float sunX(int i,int total,int w,int h){
  return sunCx(w)+sunRadius(w,h)*(float)Math.sin(2*Math.PI*i/Math.max(1,total));
 }
 private float sunY(int i,int total,int w,int h){
  return sunCy(h)-sunRadius(w,h)*(float)Math.cos(2*Math.PI*i/Math.max(1,total));
 }
 private int sunUnitAt(float x,float y,int w,int h){
  int count=macros.forSurah(surah).size(),candidate=-1;
  float best=Ui.dp(activity,36)*Ui.dp(activity,36);
  for(int i=0;i<count;i++){
   float dx=x-sunX(i,count,w,h),dy=y-sunY(i,count,w,h);
   float dist=dx*dx+dy*dy;
   if(dist<best){best=dist;candidate=i;}
  }
  return candidate;
 }
 private void drawSun(Canvas c,Paint p,int w,int h){
  java.util.List<MawdudiMapData.Macro> list=macros.forSurah(surah);
  int count=list.size();
  float cx=sunCx(w),cy=sunCy(h);
  p.setAntiAlias(true);p.setStyle(Paint.Style.STROKE);
  p.setStrokeWidth(Ui.dp(activity,1));p.setColor(Ui.INK);
  c.drawCircle(cx,cy,Ui.dp(activity,37),p);
  p.setTextAlign(Paint.Align.CENTER);p.setStyle(Paint.Style.FILL);
  p.setTextSize(Ui.dp(activity,13));
  String center=QuranSurahNames.labelFor(surah);
  String name=TextUtils.ellipsize(center,new android.text.TextPaint(p),Ui.dp(activity,67),
      TextUtils.TruncateAt.END).toString();
  c.drawText(name,cx,cy+Ui.dp(activity,4),p);
  p.setTextSize(Ui.dp(activity,9));c.drawText(surah+" · "+count+" unités",cx,cy+Ui.dp(activity,19),p);
  for(int k=0;k<count;k++){
   float x=sunX(k,count,w,h),y=sunY(k,count,w,h);
   p.setStyle(Paint.Style.STROKE);
   p.setStrokeWidth(Ui.dp(activity,k==activeMacro?2:1));
   c.drawLine(cx+(x-cx)*.33f,cy+(y-cy)*.33f,x,y,p);
   p.setStyle(Paint.Style.FILL);
   c.drawCircle(x,y,Ui.dp(activity,k==activeMacro?5:3),p);
   float dx=(x-cx)/Math.max(1,sunRadius(w,h));
   float dy=(y-cy)/Math.max(1,sunRadius(w,h));
   float labelX=Math.max(Ui.dp(activity,35),Math.min(w-Ui.dp(activity,35),x+dx*Ui.dp(activity,23)));
   float labelY=Math.max(Ui.dp(activity,11),Math.min(h-Ui.dp(activity,17),y+dy*Ui.dp(activity,17)));
   p.setTextSize(Ui.dp(activity,10));
   c.drawText("Unité "+(k+1),labelX,labelY,p);
   // The selected unit alone shows its canonical source range nearby.
   if(k==activeMacro){
    p.setTextSize(Ui.dp(activity,9));
    MawdudiMapData.Macro unit=list.get(k);
    c.drawText(surah+":"+unit.start+"–"+unit.end,labelX,labelY+Ui.dp(activity,11),p);
   }
  }
 }
 /** Level 1: ten rays for Al-Baqarah, all visible at once, with the
  * clockwise order stable even when the user changes the active unit.
  */
 private final class OverviewGraph extends View {
  final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
  OverviewGraph(){super(activity);setFocusable(true);}
  int position(int ignored){return 0;}
  @Override protected void onMeasure(int w,int h){
   setMeasuredDimension(Math.max(1,MeasureSpec.getSize(w)),
       axis==null||axis.getLayoutParams()==null?sunHeight:axis.getLayoutParams().height);
  }
  @Override protected void onDraw(Canvas canvas){drawSun(canvas,paint,getWidth(),getHeight());}
  @Override public boolean onTouchEvent(MotionEvent event){
   if(event.getAction()!=MotionEvent.ACTION_UP)return true;
   int i=sunUnitAt(event.getX(),event.getY(),getWidth(),getHeight());
   if(i<0)return true;
   openMacro(i);performClick();return true;
  }
  @Override public boolean performClick(){super.performClick();return true;}
 }
 /**
  * Level 2: keep every unit visible in the same radial coordinate system.
  * From the active unit a SECOND branching fan shows passages; the nearby
  * seven nodes scroll through the original corpus, including the approved
  * surah 27 display-only subnodes. No passage or title is synthesized.
  */
 private final class Graph extends View {
  final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
  final class Node {
   final int index,first,last;
   final String title;
   Node(int n,int a,int b,String t){index=n;first=a;last=b;title=t;}
  }
  Graph(){super(activity);setFocusable(true);}
  boolean split(int n){MawdudiPassageIndex.Passage p=passages.get(n);
   return p.surah==27&&p.firstAyah==67&&p.lastAyah==82&&subnodes.length()==3;}
  boolean inMacro(int n){
   MawdudiPassageIndex.Passage p=passages.get(n);
   MawdudiMapData.Macro m=macro();
   return p.firstAyah<=m.end&&p.lastAyah>=m.start;
  }
  int units(int n){return !inMacro(n)?0:split(n)?3:1;}
  int position(int ignored){return 0;}
  java.util.List<Node> nodes(){
   java.util.List<Node> result=new ArrayList<>();
   for(int n=0;n<passages.size();n++){
    if(!inMacro(n))continue;
    MawdudiPassageIndex.Passage p=passages.get(n);
    if(split(n)){
     for(int k=0;k<subnodes.length();k++){
      try{
       JSONObject child=subnodes.getJSONObject(k);
       result.add(new Node(n,child.getInt("start"),child.getInt("end"),child.getString("title")));
      }catch(JSONException invalid){throw new IllegalStateException("invalid approved QCF subnode",invalid);}
     }
    } else result.add(new Node(n,p.firstAyah,p.lastAyah,titles.titleForPassage(p.id)));
   }
   return result;
  }
  int selectedOrdinalZero(java.util.List<Node> nodes){
   for(int k=0;k<nodes.size();k++)
    if(nodes.get(k).index==selected&&nodes.get(k).first==focusStart
        &&nodes.get(k).last==focusEnd)return k;
   for(int k=0;k<nodes.size();k++)if(nodes.get(k).index==selected)return k;
   return 0;
  }
  int currentNodeOrdinal(){java.util.List<Node> items=nodes();return items.isEmpty()?0:selectedOrdinalZero(items)+1;}
  String selectedTitle(){java.util.List<Node> items=nodes();
   return items.isEmpty()?"":items.get(selectedOrdinalZero(items)).title;
  }
  @Override protected void onMeasure(int w,int h){
   setMeasuredDimension(Math.max(1,MeasureSpec.getSize(w)),sunHeight);
  }
  @Override protected void onDraw(Canvas c){
   int w=getWidth(),h=getHeight();
   drawSun(c,paint,w,h);
   java.util.List<Node> items=nodes();
   if(items.isEmpty())return;
   int ix=selectedOrdinalZero(items);
   int n=macros.forSurah(surah).size();
   float x=sunX(activeMacro,n,w,h),y=sunY(activeMacro,n,w,h);
   float hubX=w*.5f,hubY=h*.80f;
   paint.setColor(Ui.INK);paint.setStyle(Paint.Style.STROKE);
   paint.setStrokeWidth(Ui.dp(activity,1));
   Path arm=new Path();arm.moveTo(x,y);
   arm.quadTo(x+(hubX-x)*.3f,hubY,hubX,hubY);
   c.drawPath(arm,paint);
   int first=Math.max(0,Math.min(items.size()-7,ix-3));
   int visible=Math.min(7,items.size());
   for(int j=0;j<visible;j++){
    int index=first+j;
    float px=w*(.10f+.8f*(j+.5f)/visible);
    float py=h*.91f-Ui.dp(activity,(j%2)*7);
    Path branch=new Path();branch.moveTo(hubX,hubY);
    branch.quadTo(px,hubY+Ui.dp(activity,3),px,py);
    c.drawPath(branch,paint);
    paint.setStyle(Paint.Style.FILL);paint.setColor(Ui.INK);
    c.drawCircle(px,py,Ui.dp(activity,index==ix?5:3),paint);
    paint.setTextSize(Ui.dp(activity,9));paint.setTextAlign(Paint.Align.CENTER);
    c.drawText("P"+(index+1),px,Math.min(h-Ui.dp(activity,1),py+Ui.dp(activity,13)),paint);
    paint.setStyle(Paint.Style.STROKE);
   }
  }
  @Override public boolean onTouchEvent(MotionEvent event){
   if(event.getAction()!=MotionEvent.ACTION_UP)return true;
   float x=event.getX(),y=event.getY();int w=getWidth(),h=getHeight();
   int unit=sunUnitAt(x,y,w,h);
   if(unit>=0){openMacro(unit);performClick();return true;}
   java.util.List<Node> items=nodes();if(items.isEmpty())return true;
   int selectedNode=selectedOrdinalZero(items),visible=Math.min(7,items.size());
   int first=Math.max(0,Math.min(items.size()-7,selectedNode-3));
   for(int j=0;j<visible;j++){
    float px=w*(.10f+.8f*(j+.5f)/visible),py=h*.91f-Ui.dp(activity,(j%2)*7);
    if(Math.abs(x-px)<Ui.dp(activity,24)&&Math.abs(y-py)<Ui.dp(activity,29)){
     Node target=items.get(first+j);
     select(target.index,target.first,target.last,target.title);
     performClick();return true;
    }
   }
   return true;
  }
  @Override public boolean performClick(){super.performClick();return true;}
 }
}
