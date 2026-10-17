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

/** Physical QCF page-scale Atlas: sourah -> sourced units -> Ayah Themes passages -> authentic QCF thumbnails. No Hifz mutations. */
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
 private TextView heading;
 private ScrollView titlePanel;
 private boolean titleExpanded;
 private LinearLayout icons;
 private TextView passageTitle;
 private MapQcfThumbnails thumbnails;
 private int sunHeight;
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
   overview=saved.getBoolean("map114Overview",true)&&macros.forSurah(surah).size()>1;
   passageOpen=saved.getBoolean("map114PassageOpen",false);
   int restored=saved.getInt("map114MacroSelected",activeMacro);
   if(restored>=0&&restored<macros.forSurah(surah).size())activeMacro=restored;
   MawdudiMapData.Macro restoredUnit=macro();
   page=Math.max(geometry.pageForVerse(new VerseRef(surah,restoredUnit.start)),
       Math.min(MawdudiMapModel.lastPageForVerse(new VerseRef(surah,restoredUnit.end),geometry),
          saved.getInt("map114Page",page)));
   int first=saved.getInt("map114FocusStart",focusStart),last=saved.getInt("map114FocusEnd",focusEnd);
   if(current().contains(first)&&current().contains(last)&&first<=last){focusStart=first;focusEnd=last;}
  }
 }
 private MawdudiPassageIndex.Passage current(){return passages.get(selected);}
 private void choose(int s,int ayah) {
  surah=s;passages=index.forSurah(s);MawdudiPassageIndex.Passage p=index.at(s,ayah);
  selected=passages.indexOf(p);page=MawdudiMapModel.firstPage(p,geometry);focusStart=p.firstAyah;focusEnd=p.lastAyah;
  passageOpen=false;overview=macros.forSurah(s).size()>1;
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
  overview=overviewMode&&macros.forSurah(surah).size()>1;
  if(overview)passageOpen=false;
  closeTitle();
  if(commentary!=null){commentary.close();commentary=null;}
  if(axis!=null){
   axis.removeAllViews();
   axis.addView(overviewGraph,
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
  top.addView(Ui.iconButton(activity,"","Retour",v->goBack()),new LinearLayout.LayoutParams(Ui.dp(activity,48),Ui.dp(activity,48)));
  heading=Ui.text(activity,"",12f,false);heading.setMaxLines(2);heading.setEllipsize(TextUtils.TruncateAt.END);
  heading.setClickable(true);heading.setFocusable(true);heading.setOnClickListener(v->chooseUnit());
  top.addView(heading,new LinearLayout.LayoutParams(0,Ui.dp(activity,48),1));
  top.addView(Ui.iconButton(activity,"","Choisir une sourate",v->QuranSurahNames.showSurahPicker(activity,s->{choose(s,1);setLevel(true);})),new LinearLayout.LayoutParams(Ui.dp(activity,48),Ui.dp(activity,48)));
  root.addView(top);
  // Use the current window (including density/zoom and system-bar insets),
  // then its measured content bounds; physical display dimensions can be stale.
  int windowWidth=activity.getResources().getDisplayMetrics().widthPixels;
  if(android.os.Build.VERSION.SDK_INT>=30){
   WindowMetrics metrics=activity.getWindowManager().getCurrentWindowMetrics();
   android.graphics.Insets insets=metrics.getWindowInsets().getInsets(
       WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());
   windowWidth=metrics.getBounds().width()-insets.left-insets.right;
  }
  sunHeight=atlasHeight(windowWidth,activity.getResources().getDisplayMetrics().heightPixels);
  axis=new HorizontalScrollView(activity);axis.setFillViewport(true);
  axis.setHorizontalScrollBarEnabled(false);
  graph=new Graph();overviewGraph=new OverviewGraph();
  axis.addView(overviewGraph,
      new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,sunHeight));
  root.addView(axis,new LinearLayout.LayoutParams(-1,sunHeight));
  root.addOnLayoutChangeListener((v,left,topEdge,right,bottom,oldLeft,oldTop,oldRight,oldBottom)->{
   int height=atlasHeight(right-left,bottom-topEdge);
   if(height!=sunHeight){sunHeight=height;refresh();}
  });
  passageTitle=Ui.text(activity,"",13.5f,true);
  passageTitle.setGravity(Gravity.CENTER);
  passageTitle.setMaxLines(3);passageTitle.setEllipsize(TextUtils.TruncateAt.END);
  passageTitle.setOnClickListener(v->showCurrentTitle());
  passageTitle.setMinHeight(Ui.dp(activity,56));
  titlePanel=new ScrollView(activity);titlePanel.setFillViewport(false);
  titlePanel.setVerticalScrollBarEnabled(true);
  titlePanel.addView(passageTitle,new ScrollView.LayoutParams(-1,-2));
  root.addView(titlePanel,new LinearLayout.LayoutParams(-1,Ui.dp(activity,56)));
  thumbnails=new MapQcfThumbnails(activity,new MapQcfThumbnails.Listener(){
   @Override public void onPageChosen(int p){onThumbnailPage(p);}
   @Override public void onOpenReading(int p){onThumbnailPage(p);openReader();}
  });
  root.addView(thumbnails,new LinearLayout.LayoutParams(-1,0,1));
  mushaf=new MushafView(activity);mushaf.setListener(this);
  root.addView(mushaf,new LinearLayout.LayoutParams(-1,0,1));
  // Page badges and swipes replace both page arrows and passage arrows.
  icons=Ui.row(activity);icons.setGravity(Gravity.CENTER);
  icons.setMinimumHeight(Ui.dp(activity,56));
  LinearLayout.LayoutParams tafsirPos=new LinearLayout.LayoutParams(Ui.dp(activity,48),Ui.dp(activity,48));
  tafsirPos.rightMargin=Ui.dp(activity,36);
  icons.addView(Ui.iconButton(activity,"","Mosaïque des pages",v->{passageOpen=false;refresh();}),tafsirPos);
  LinearLayout.LayoutParams readerPos=new LinearLayout.LayoutParams(Ui.dp(activity,48),Ui.dp(activity,48));
  readerPos.leftMargin=Ui.dp(activity,36);
  icons.addView(Ui.iconButton(activity,"","Lecture avec amorces",v->openReader()),readerPos);
  root.addView(icons);
  activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
  activity.setContentView(root);refresh();
 }
 MushafView mushaf(){return mushaf;}
 private void closeTitle(){
  if(!titleExpanded)return;
  titleExpanded=false;
  passageTitle.setMaxLines(3);passageTitle.setEllipsize(TextUtils.TruncateAt.END);
  titlePanel.getLayoutParams().height=Ui.dp(activity,56);titlePanel.requestLayout();titlePanel.scrollTo(0,0);
 }
 private void openCommentary(){
  if(overview){openMacro(activeMacro);return;}
  if(!passageOpen)return;
  closeTitle();
  if(commentary!=null)commentary.close();
  commentary=new MapJalalaynDialog(activity,surah,focusStart,focusEnd,focusStart);
  commentary.show();
 }
 private void showTitle(String text){
  if(text==null||text.trim().isEmpty())return;
  if(titleExpanded){closeTitle();return;}
  titleExpanded=true;passageTitle.setMaxLines(Integer.MAX_VALUE);passageTitle.setEllipsize(null);
  passageTitle.setText(text);
  // The full caption has its own bounded space; it never overlays Quran text.
  int available=activity.findViewById(android.R.id.content).getHeight();
  int width=Math.max(1,titlePanel.getWidth());
  passageTitle.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),
      View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));
  int pageBudget=available-sunHeight-Ui.dp(activity,48+48+100+140+32);
  int limit=Math.max(Ui.dp(activity,56),Math.min(Math.min(Ui.dp(activity,180),available/4),pageBudget));
  titlePanel.getLayoutParams().height=Math.min(limit,Math.max(Ui.dp(activity,56),passageTitle.getMeasuredHeight()));
  titlePanel.requestLayout();titlePanel.scrollTo(0,0);
 }
 private void showCurrentTitle(){
  if(overview){openMacro(activeMacro);return;}
  showTitle(macro().label); // The selected unit's approved caption, never a PDF.
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
  if(direction!=1&&direction!=-1)return;
  closeTitle();
  int next=selected+direction;
  if(next<0||next>=passages.size())return;
  MawdudiPassageIndex.Passage passage=passages.get(next);
  if(!graph.inMacro(next)){
   // The current passage can span multiple units (4:101-104 spans U7/U8).
   // Resolve the next passage's actual unit instead of assuming one unit step.
   activeMacro=macroIndex(surah,direction>0?passage.firstAyah:passage.lastAyah);
  }
  selected=next;focusStart=passage.firstAyah;focusEnd=passage.lastAyah;
  passageOpen=true;
  page=geometry.pageForVerse(new VerseRef(surah,focusStart));
  refresh();
 }
 private void select(int n,int a,int b,String title) {
  closeTitle();selected=n;focusStart=a;focusEnd=b;passageOpen=true;
  page=geometry.pageForVerse(new VerseRef(surah,a));refresh();
 }
 void refresh(){
  if(heading==null)return;
  MawdudiMapData.Macro unit=macro();
  String surahLabel=QuranSurahNames.labelFor(surah);
  int total=macros.forSurah(surah).size();
  if(overview){
   heading.setText(surahLabel+" · "+total+" unités");
   passageTitle.setText("");
  }else{
   heading.setText(surahLabel+" · U"+(activeMacro+1)+"/"+total);
   passageTitle.setText(unit.label);
  }
  heading.setContentDescription("Changer d'unité · "+heading.getText());
  int desiredHeight=sunHeight;
  ViewGroup.LayoutParams vp=axis.getLayoutParams();
  if(vp.height!=desiredHeight){vp.height=desiredHeight;axis.setLayoutParams(vp);}
  View child=axis.getChildAt(0);
  if(child!=null){
   ViewGroup.LayoutParams cp=child.getLayoutParams();
   if(cp.height!=desiredHeight){cp.height=desiredHeight;child.setLayoutParams(cp);}
  }
  graph.requestLayout();graph.invalidate();
  overviewGraph.requestLayout();overviewGraph.invalidate();
  renderPage();
 }
 private void renderPage(){
  mushaf.setVisibility(View.GONE);
  passageTitle.setVisibility(overview?View.GONE:View.VISIBLE);
  thumbnails.setVisibility(View.VISIBLE);
  icons.setVisibility(overview?View.GONE:View.VISIBLE);
  MawdudiMapData.Macro unit=macro();
  if(overview){
   java.util.List<MawdudiPassageIndex.Passage> all=index.forSurah(surah);
   int first=geometry.pageForVerse(new VerseRef(surah,1));
   int last=MawdudiMapModel.lastPageForVerse(
       new VerseRef(surah,all.get(all.size()-1).lastAyah),geometry);
   page=Math.max(first,Math.min(last,page));
   thumbnails.setMosaic(true);
   thumbnails.showPages(first,last,page,surah,unit.start,unit.end,true,0,0);
   return;
  }
  int first=geometry.pageForVerse(new VerseRef(surah,unit.start));
  int last=MawdudiMapModel.lastPageForVerse(new VerseRef(surah,unit.end),geometry);
  page=Math.max(first,Math.min(last,page));
  thumbnails.setMosaic(!passageOpen);
  thumbnails.showPages(first,last,page,surah,unit.start,unit.end,true,0,0);
 }
 private void onThumbnailPage(int desired){
  closeTitle();if(overview)openMacro(activeMacro);
  MawdudiMapData.Macro unit=macro();
  int start=geometry.pageForVerse(new VerseRef(surah,unit.start));
  int end=MawdudiMapModel.lastPageForVerse(new VerseRef(surah,unit.end),geometry);
  if(passageOpen){start=Math.min(start,MawdudiMapModel.firstPage(current(),geometry));end=Math.max(end,MawdudiMapModel.lastPage(current(),geometry));}
  desired=Math.max(start,Math.min(end,desired));
  int currentFirst=geometry.pageForVerse(new VerseRef(surah,focusStart));
  int currentLast=MawdudiMapModel.lastPageForVerse(new VerseRef(surah,focusEnd),geometry);
  if(!passageOpen||desired<currentFirst||desired>currentLast){
   for(int n=0;n<passages.size();n++){
    MawdudiPassageIndex.Passage p=passages.get(n);
    if(graph.inMacro(n)&&desired>=MawdudiMapModel.firstPage(p,geometry)&&desired<=MawdudiMapModel.lastPage(p,geometry)){
     selected=n;focusStart=p.firstAyah;focusEnd=p.lastAyah;passageOpen=true;break;
    }
   }
  }
  page=desired;refresh();
 }
 private void turn(int d){onThumbnailPage(page+d);}
 private void moveWithinUnit(int d){
  int next=selected+d;
  if(next>=0&&next<passages.size()&&graph.inMacro(next))movePassage(d);
 }
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
 boolean back(){if(commentary!=null&&commentary.isShowing()){commentary.close();commentary=null;return true;}if(titleExpanded){closeTitle();return true;}if(passageOpen){passageOpen=false;refresh();return true;}if(!overview){if(macros.forSurah(surah).size()>1){setLevel(true);return true;}return false;}return false;}
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
 @Override public void onError(String error){passageTitle.setText("Muṣḥaf indisponible");}
 @Override public void onPageSwipe(int delta){turn(delta);}
 @Override public void onVerseTap(VerseRef verse){openReader();}
 @Override public void onSurfaceTap(){openReader();}

 /** Stable unit coordinates and a physical Mushaf page-scale strip. */
 private int atlasHeight(int width,int height){
  int minimum=Ui.dp(activity,macros.forSurah(surah).size()>6?190:148);
  int desired=minimum;
  // Keep a useful thumbnail viewport after headings and both navigation rows.
  int budget=height-Ui.dp(activity,48+56+48+100+180);
  return Math.max(minimum,Math.min(desired,budget));
 }
 private float atlasX(int index,int total,int width){
  int columns=Math.min(6,total);
  return width*((index%columns)+.5f)/columns;
 }
 private float atlasY(int index,int total){
  return Ui.dp(activity,total>6&&index>=6?76:27);
 }
 private int atlasUnitAt(float x,float y,int width){
  int count=macros.forSurah(surah).size(),winner=-1;
  float best=Ui.dp(activity,25)*Ui.dp(activity,25);
  for(int i=0;i<count;i++){
   float dx=x-atlasX(i,count,width),dy=y-atlasY(i,count);
   float d=dx*dx+dy*dy;if(d<best){best=d;winner=i;}
  }
  return winner;
 }
 private void drawAtlas(Canvas c,Paint p,int width,int height){
  java.util.List<MawdudiMapData.Macro> units=macros.forSurah(surah);
  int total=units.size();int columns=Math.min(6,total);
  p.setAntiAlias(true);p.setColor(Ui.INK);
  p.setStrokeWidth(Ui.dp(activity,1));p.setTextAlign(Paint.Align.CENTER);
  // All units stay visible at fixed places; no sun rays, no card tiles.
  for(int i=0;i<total;i++){
   float x=atlasX(i,total,width),y=atlasY(i,total);
   p.setStyle(Paint.Style.STROKE);
   if(i%columns!=columns-1&&i+1<total&&(i+1)/columns==i/columns)
    c.drawLine(x+Ui.dp(activity,8),y,atlasX(i+1,total,width)-Ui.dp(activity,8),y,p);
   p.setStyle(Paint.Style.FILL);
   c.drawCircle(x,y,Ui.dp(activity,i==activeMacro?8:4),p);
   if(i==activeMacro){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Ui.dp(activity,1));c.drawCircle(x,y,Ui.dp(activity,12),p);p.setStyle(Paint.Style.FILL);}
   p.setTextSize(Ui.dp(activity,11));c.drawText("U"+(i+1),x,y-Ui.dp(activity,16),p);
  }
  int firstPage=geometry.pageForVerse(new VerseRef(surah,1));
  java.util.List<MawdudiPassageIndex.Passage> all=index.forSurah(surah);
  int lastPage=MawdudiMapModel.lastPageForVerse(new VerseRef(surah,all.get(all.size()-1).lastAyah),geometry);
  int pageCount=Math.max(1,lastPage-firstPage+1);
  float left=Ui.dp(activity,17),right=width-Ui.dp(activity,17);
  float top=Ui.dp(activity,total>6?106:58),bottom=top+Ui.dp(activity,5);
  p.setColor(0xffcecdc7);p.setStyle(Paint.Style.FILL);c.drawRoundRect(left,top,right,bottom,Ui.dp(activity,3),Ui.dp(activity,3),p);p.setColor(Ui.INK);
  MawdudiMapData.Macro active=macro();
  int a=geometry.pageForVerse(new VerseRef(surah,active.start));
  int b=MawdudiMapModel.lastPageForVerse(new VerseRef(surah,active.end),geometry);
  float x1=left+(a-firstPage)*(right-left)/pageCount;
  float x2=left+(b-firstPage+1)*(right-left)/pageCount;
  p.setStyle(Paint.Style.FILL);
  c.drawRoundRect(x1,top,Math.max(x1+Ui.dp(activity,2),x2),bottom,Ui.dp(activity,3),Ui.dp(activity,3),p);
  float cursor=left+(page-firstPage+.5f)*(right-left)/pageCount;
  p.setColor(Ui.PAPER);c.drawCircle(cursor,(top+bottom)/2,Ui.dp(activity,7),p);
  p.setColor(Ui.INK);c.drawCircle(cursor,(top+bottom)/2,Ui.dp(activity,5),p);
  p.setStyle(Paint.Style.STROKE);
  for(MawdudiMapData.Macro unit:units){
   int pg=geometry.pageForVerse(new VerseRef(surah,unit.start));
   float tick=left+(pg-firstPage)*(right-left)/pageCount;
   c.drawCircle(tick,(top+bottom)/2,Ui.dp(activity,1),p);
  }
  p.setStyle(Paint.Style.FILL);p.setTextSize(Ui.dp(activity,10));
  p.setTextAlign(Paint.Align.LEFT);c.drawText("Pages "+firstPage+" à "+lastPage,left,bottom+Ui.dp(activity,16),p);
  p.setTextAlign(Paint.Align.RIGHT);c.drawText("U"+(activeMacro+1)+" · "+a+" à "+b,right,bottom+Ui.dp(activity,16),p);
 }
 private final class OverviewGraph extends View {
  final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
  OverviewGraph(){super(activity);setFocusable(true);}
  int position(int ignored){return 0;}
  @Override protected void onMeasure(int w,int h){
   setMeasuredDimension(Math.max(1,MeasureSpec.getSize(w)),
       axis==null||axis.getLayoutParams()==null?sunHeight:axis.getLayoutParams().height);
  }
  @Override protected void onDraw(Canvas canvas){drawAtlas(canvas,paint,getWidth(),getHeight());}
  @Override public boolean onTouchEvent(MotionEvent event){
   if(event.getAction()!=MotionEvent.ACTION_UP)return true;
   int i=atlasUnitAt(event.getX(),event.getY(),getWidth());
   if(i<0)return true;
   openMacro(i);performClick();return true;
  }
  @Override public boolean performClick(){super.performClick();return true;}
 }
 /**
  * Stable unit atlas and a five-point chronological passage window.
  * Swipes browse the original passages within the selected unit; the approved
  * surah 27 subnodes remain display-only. No passage or title is synthesized.
  */
 private final class Graph extends View {
  final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
  float downX,downY;
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
  String selectedTitle(){
   // Whole-passage focus must not borrow the first visual subnode title.
   for(Node n:nodes())if(n.index==selected&&n.first==focusStart&&n.last==focusEnd)return n.title;
   return titles.titleForPassage(current().id);
  }
  @Override protected void onMeasure(int w,int h){
   setMeasuredDimension(Math.max(1,MeasureSpec.getSize(w)),sunHeight);
  }
  private int firstVisible(java.util.List<Node> items){
  return Math.max(0,Math.min(Math.max(0,items.size()-5),selectedOrdinalZero(items)-2));
 }
 private float passageX(int j,int visible,int width){return width*(j+.5f)/visible;}
 private float passageY(int height){return height-Ui.dp(activity,26);}
 @Override protected void onDraw(Canvas canvas){
  int width=getWidth(),height=getHeight();
  drawAtlas(canvas,paint,width,height);
  java.util.List<Node> items=nodes();if(items.isEmpty())return;
  int first=firstVisible(items),visible=Math.min(5,items.size());
  int selectedIndex=selectedOrdinalZero(items);
  float y=passageY(height);
  paint.setColor(Ui.INK);paint.setStrokeWidth(Ui.dp(activity,1));
  paint.setStyle(Paint.Style.STROKE);
  if(visible>1)canvas.drawLine(passageX(0,visible,width),y,passageX(visible-1,visible,width),y,paint);
  for(int j=0;j<visible;j++){
   int ordinal=first+j;Node n=items.get(ordinal);
   float x=passageX(j,visible,width);
   paint.setStyle(Paint.Style.FILL);
   canvas.drawCircle(x,y,Ui.dp(activity,ordinal==selectedIndex?7:3),paint);
   paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(Ui.dp(activity,9));
   canvas.drawText(n.first==n.last?String.valueOf(n.first):n.first+"–"+n.last,x,y-Ui.dp(activity,13),paint);
  }
 }
 @Override public boolean onTouchEvent(MotionEvent event){
  if(event.getAction()==MotionEvent.ACTION_DOWN){downX=event.getX();downY=event.getY();return true;}
  if(event.getAction()!=MotionEvent.ACTION_UP)return true;
  float dx=event.getX()-downX,dy=event.getY()-downY;
  if(Math.abs(dx)>Ui.dp(activity,40)&&Math.abs(dx)>Math.abs(dy)*1.2f){moveWithinUnit(dx>0?1:-1);return true;}
  int width=getWidth(),height=getHeight();
  int unit=atlasUnitAt(event.getX(),event.getY(),width);
  if(unit>=0){openMacro(unit);performClick();return true;}
  java.util.List<Node> items=nodes();if(items.isEmpty())return true;
  int first=firstVisible(items),visible=Math.min(5,items.size());
  for(int j=0;j<visible;j++){
   float x=passageX(j,visible,width),y=passageY(height);
   if(Math.abs(event.getX()-x)<Ui.dp(activity,27)&&Math.abs(event.getY()-y)<Ui.dp(activity,25)){
    Node node=items.get(first+j);
    select(node.index,node.first,node.last,node.title);
    performClick();return true;
   }
  }
  return true;
 }
 @Override public boolean performClick(){super.performClick();return true;}
 }
}
