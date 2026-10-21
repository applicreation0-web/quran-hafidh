package com.quransafeguard.hifz.preview;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.*;
import android.widget.*;
import com.quransafeguard.hifz.core.VerseRef;
import java.util.*;

/** Atlas unit map and direct authentic full-page preview. Passages belong to Lecture. No Hifz mutations. */
final class MawdudiMapScreen implements MushafView.Listener {
 private final Activity activity;
 private final GeometryRepository geometry;
 private final MawdudiPassageIndex index;
 private final AyahThemesCueIndex titles;
 private final MawdudiMapData macros;
 private int surah,selected,page,focusStart,focusEnd;
 private boolean ready,overview=true,passageOpen=false;
 private int activeMacro;
 private int unitWindowStart; // First of the five visible units; total number of units is unchanged.
 private java.util.List<MawdudiPassageIndex.Passage> passages;
 private MushafView mushaf;
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
  surah=saved==null?s:saved.getInt("map114Surah",s);
  int ayah=saved==null?a:saved.getInt("map114Ayah",a);
  choose(surah,ayah);
  if(saved!=null) {
   overview=saved.getBoolean("map114Overview",true);
   passageOpen=saved.getBoolean("map114PassageOpen",false);
   int restored=saved.getInt("map114MacroSelected",activeMacro);
   if(restored>=0&&restored<macros.forSurah(surah).size())activeMacro=restored;
   unitWindowStart=Math.max(0,Math.min(maxUnitWindowStart(),saved.getInt("map114UnitWindowStart",unitWindowStart)));
   if(!overview)passageOpen=true;
   MawdudiMapData.Macro restoredUnit=macro();
   page=Math.max(geometry.pageForVerse(new VerseRef(surah,restoredUnit.start)),
       Math.min(MawdudiMapModel.lastPageForVerse(new VerseRef(surah,restoredUnit.end),geometry),
          saved.getInt("map114Page",page)));
   String restoredId=saved.getString("map114SelectedPassageId");
   for(int k=0;k<passages.size();k++){
    MawdudiPassageIndex.Passage candidate=passages.get(k);
    if(candidate.id.equals(restoredId)&&candidate.firstAyah<=restoredUnit.end
        &&candidate.lastAyah>=restoredUnit.start){selected=k;break;}
   }
   focusStart=AtlasPassageContext.intersectionStart(current().firstAyah,restoredUnit.start);
   focusEnd=AtlasPassageContext.intersectionEnd(current().lastAyah,restoredUnit.end);
   int first=saved.getInt("map114FocusStart",focusStart),last=saved.getInt("map114FocusEnd",focusEnd);
   if(current().contains(first)&&current().contains(last)&&first<=last
        &&first>=restoredUnit.start&&last<=restoredUnit.end){focusStart=first;focusEnd=last;}
  }
 }
 private MawdudiPassageIndex.Passage current(){return passages.get(selected);}
 private void choose(int s,int ayah) {
  surah=s;passages=index.forSurah(s);MawdudiPassageIndex.Passage p=index.at(s,ayah);
  selected=passages.indexOf(p);page=MawdudiMapModel.firstPage(p,geometry);focusStart=p.firstAyah;focusEnd=p.lastAyah;
  passageOpen=false;overview=true;
  activeMacro=macroIndex(s,ayah);
  focusStart=AtlasPassageContext.intersectionStart(p.firstAyah,macro().start);
  focusEnd=AtlasPassageContext.intersectionEnd(p.lastAyah,macro().end);
  unitWindowStart=centerUnitWindow(activeMacro);
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
  focusStart=AtlasPassageContext.intersectionStart(first.firstAyah,chosen.start);
  focusEnd=AtlasPassageContext.intersectionEnd(first.lastAyah,chosen.end);
  page=geometry.pageForVerse(new VerseRef(surah,chosen.start));
  activeMacro=ordinal;
  unitWindowStart=centerUnitWindow(activeMacro);
  passageOpen=true; // Direct full-page Muṣḥaf: no intermediate miniatures/mosaic.
  setLevel(false);
  // Selecting a unit reveals its complete approved caption immediately.
  showCurrentTitle();
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
  axis=new HorizontalScrollView(activity){
   // The map graph owns horizontal touch gestures, not the empty scroll container.
   @Override public boolean onInterceptTouchEvent(MotionEvent event){return false;}
  };axis.setFillViewport(true);
  axis.setHorizontalScrollBarEnabled(false);
  overviewGraph=new OverviewGraph();
  axis.addView(overviewGraph,
      new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,sunHeight));
  root.addView(axis,new LinearLayout.LayoutParams(-1,sunHeight));
  root.addOnLayoutChangeListener((v,left,topEdge,right,bottom,oldLeft,oldTop,oldRight,oldBottom)->{
   int height=atlasHeight(right-left,bottom-topEdge);
   if(height!=sunHeight){sunHeight=height;refresh();}
  });
  boolean compact=activity.getResources().getDisplayMetrics().heightPixels<Ui.dp(activity,700);
  passageTitle=Ui.text(activity,"",16f,true);
  passageTitle.setTypeface(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD);
  passageTitle.setGravity(Gravity.CENTER);
  passageTitle.setMaxLines(3);passageTitle.setEllipsize(TextUtils.TruncateAt.END);
  passageTitle.setOnClickListener(v->showCurrentTitle());
  passageTitle.setMinHeight(Ui.dp(activity,compact?44:50));
  titlePanel=new ScrollView(activity);titlePanel.setFillViewport(false);
  titlePanel.setVerticalScrollBarEnabled(true);
  titlePanel.addView(passageTitle,new ScrollView.LayoutParams(-1,-2));
  root.addView(titlePanel,new LinearLayout.LayoutParams(-1,Ui.dp(activity,compact?48:56)));
  thumbnails=new MapQcfThumbnails(activity,new MapQcfThumbnails.Listener(){
   @Override public void onPageChosen(int p){onThumbnailPage(p);}
   @Override public void onOpenReading(int p){onThumbnailPage(p);openReader();}
  });
  root.addView(thumbnails,new LinearLayout.LayoutParams(-1,0,1));
  mushaf=new MushafView(activity);mushaf.setListener(this);
  root.addView(mushaf,new LinearLayout.LayoutParams(-1,0,1));
  // Page badges and swipes replace both page arrows and passage arrows.
  icons=Ui.row(activity);icons.setGravity(Gravity.CENTER);
  icons.setMinimumHeight(Ui.dp(activity,compact?48:56));
  LinearLayout.LayoutParams tafsirPos=new LinearLayout.LayoutParams(Ui.dp(activity,48),Ui.dp(activity,48));
  tafsirPos.rightMargin=Ui.dp(activity,36);
  icons.addView(Ui.iconButton(activity,"","Atlas",v->setLevel(true)),tafsirPos);
  LinearLayout.LayoutParams readerPos=new LinearLayout.LayoutParams(Ui.dp(activity,48),Ui.dp(activity,48));
  readerPos.leftMargin=Ui.dp(activity,36);
  icons.addView(Ui.iconButton(activity,"","Lecture",v->openReader()),readerPos);
  root.addView(icons);
  activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
  activity.setContentView(root);refresh();
 }
 MushafView mushaf(){return mushaf;}
 private int captionHeight(){
  return Ui.dp(activity,activity.getResources().getDisplayMetrics().heightPixels<Ui.dp(activity,700)?48:56);
 }
 private void closeTitle(){
  if(!titleExpanded)return;
  titleExpanded=false;
  passageTitle.setMaxLines(3);passageTitle.setEllipsize(TextUtils.TruncateAt.END);
  titlePanel.getLayoutParams().height=captionHeight();titlePanel.requestLayout();titlePanel.scrollTo(0,0);
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
  int width=Math.max(1,Math.max(titlePanel.getWidth(),
      activity.findViewById(android.R.id.content).getWidth()));
  passageTitle.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),
      View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));
  int pageBudget=available-sunHeight-Ui.dp(activity,48+48+100+140+32);
  int limit=Math.max(Ui.dp(activity,56),Math.min(Math.min(Ui.dp(activity,180),available/4),pageBudget));
  titlePanel.getLayoutParams().height=Math.min(limit,Math.max(captionHeight(),passageTitle.getMeasuredHeight()));
  titlePanel.requestLayout();titlePanel.scrollTo(0,0);
 }
 private void showCurrentTitle(){
  showTitle(macros.displayUnitTitle(macro().id)); // The selected unit's approved caption, never a PDF.
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
   passageTitle.setText(macros.displayUnitTitle(unit.id));
   passageTitle.setContentDescription("Unité Atlas · "+macros.displayUnitTitle(unit.id));
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
  overviewGraph.requestLayout();overviewGraph.invalidate();
  renderPage();
 }
 static String cleanPassageCaption(String original){
  return original==null?"":original.replaceFirst("(?i)^\\s*[a-z]\\)\\s+","").trim();
 }
 private void renderPage(){
  mushaf.setVisibility(View.GONE);
  titlePanel.setVisibility(overview?View.GONE:View.VISIBLE);
  passageTitle.setVisibility(overview?View.GONE:View.VISIBLE);
  // ROOT is a map only: keep the selected unit/page cursor but never render
  // a surah-wide QCF mosaic. This also prevents costly SVG decoding on BOOX.
  thumbnails.setVisibility(overview?View.GONE:View.VISIBLE);
  icons.setVisibility(overview?View.GONE:View.VISIBLE);
  MawdudiMapData.Macro unit=macro();
  if(overview){
   // The root has no page previews. Unit selection remains highlighted
   // in the Atlas graph, while its last page cursor is preserved.
   return;
  }
  int first=geometry.pageForVerse(new VerseRef(surah,unit.start));
  int last=MawdudiMapModel.lastPageForVerse(new VerseRef(surah,unit.end),geometry);
  page=Math.max(first,Math.min(last,page));
  // Always show the authentic selected QCF page. Gray shading follows the
  // entire selected Ayah Themes passage, not just its first/last verse.
  thumbnails.setMosaic(false);
  int selectedFirst=geometry.pageForVerse(new VerseRef(surah,focusStart));
  int selectedLast=MawdudiMapModel.lastPageForVerse(new VerseRef(surah,focusEnd),geometry);
  thumbnails.showPages(first,last,page,surah,focusStart,focusEnd,false,
      selectedFirst,selectedLast);
 }
 private void onThumbnailPage(int desired){
  closeTitle();
  if(overview){
   int target=activeMacro;
   for(VerseRef verse:geometry.versesOnLines(geometry.lineIdsOnPage(desired))){
    if(verse.getSurah()!=surah)continue;
    target=macroIndex(surah,verse.getAyah());break;
   }
   openMacro(target);
  }
  MawdudiMapData.Macro unit=macro();
  page=Math.max(geometry.pageForVerse(new VerseRef(surah,unit.start)),
      Math.min(MawdudiMapModel.lastPageForVerse(new VerseRef(surah,unit.end),geometry),desired));
  passageOpen=true;
  // A page switch retains the active passage if it intersects the page; otherwise
  // select the first genuine canonical passage visible on that page.
  if(page<geometry.pageForVerse(new VerseRef(surah,focusStart))
     ||page>MawdudiMapModel.lastPageForVerse(new VerseRef(surah,focusEnd),geometry)){
   for(VerseRef v:geometry.versesOnLines(geometry.lineIdsOnPage(page))){
    if(v.getSurah()!=surah||v.getAyah()<unit.start||v.getAyah()>unit.end)continue;
    MawdudiPassageIndex.Passage p=index.at(surah,v.getAyah());
    selected=passages.indexOf(p);
    focusStart=AtlasPassageContext.intersectionStart(p.firstAyah,unit.start);
    focusEnd=AtlasPassageContext.intersectionEnd(p.lastAyah,unit.end);
    break;
   }
  }
  refresh();
 }
 private void turn(int d){onThumbnailPage(page+d);}
 String selectedPassageTitle(){
  return macros.displayPassageTitle(current().id,titles.titleForPassage(current().id));
 }
 String selectedContextTitle(){
  return macros.displayContextTitle(current().id,macro().id,selectedPassageTitle());
 }
 void openReader(){
  if(overview)return;
  MawdudiMapData.Macro unit=macro();
  Intent intent=new Intent(activity,StudyReaderActivity.class)
      .putExtra(StudyReaderActivity.EXTRA_JUMP_PAGE,page)
      .putExtra(StudyReaderActivity.EXTRA_MAP_PREVIEW,true)
      .putExtra("map114UnitMode",true)
      .putExtra("map114Surah",surah)
      .putExtra("map114UnitOrdinal",activeMacro+1)
      .putExtra("map114FocusStart",unit.start)
      .putExtra("map114FocusEnd",unit.end)
      .putExtra("map114PassageId",current().id)
      .putExtra("map114SelectedStart",current().firstAyah)
      .putExtra("map114SelectedEnd",current().lastAyah);
  closeTitle();
  activity.startActivityForResult(intent,11436);
 }
 void returnFromReader(int returnedSurah,int ordinal,int returnedPage){
  returnFromReader(returnedSurah,ordinal,returnedPage,null,-1,-1);
 }
 void returnFromReader(int returnedSurah,int ordinal,int returnedPage,
                       String passageId,int passageStart,int passageEnd){
  if(returnedSurah!=surah||ordinal!=activeMacro+1)return;
  MawdudiMapData.Macro unit=macro();
  int first=geometry.pageForVerse(new VerseRef(surah,unit.start));
  int last=MawdudiMapModel.lastPageForVerse(new VerseRef(surah,unit.end),geometry);
  page=Math.max(first,Math.min(last,returnedPage));
  // The returned passage ID is authoritative; its exact canonical boundaries
  // prevent choosing a different Ayah Themes passage on the same QCF page.
  MawdudiPassageIndex.Passage matched=null;
  for(MawdudiPassageIndex.Passage candidate:passages){
   if(passageId!=null&&passageId.equals(candidate.id)
      &&candidate.firstAyah==passageStart&&candidate.lastAyah==passageEnd
      &&candidate.firstAyah<=unit.end&&candidate.lastAyah>=unit.start){
    matched=candidate;break;
   }
  }
  if(matched==null) {
   for(VerseRef verse:geometry.versesOnLines(geometry.lineIdsOnPage(page))){
    if(verse.getSurah()==surah&&verse.getAyah()>=unit.start&&verse.getAyah()<=unit.end){
     matched=index.at(surah,verse.getAyah());break;
    }
   }
  }
  if(matched!=null){
   selected=passages.indexOf(matched);
   focusStart=AtlasPassageContext.intersectionStart(matched.firstAyah,unit.start);
   focusEnd=AtlasPassageContext.intersectionEnd(matched.lastAyah,unit.end);
  }
  overview=false;passageOpen=true;
  refresh();
 }
 void save(Bundle b){
  b.putBoolean("map114Overview",overview);
  b.putBoolean("map114PassageOpen",passageOpen);
  b.putInt("map114UnitWindowStart",unitWindowStart);
  b.putInt("map114MacroSelected",activeMacro);
  b.putInt("map114Surah",surah);
  // Restoring at the unit beginning loses a later selected passage on that same page.
  b.putInt("map114Ayah",focusStart);
  b.putInt("map114Page",page);
  b.putString("map114SelectedPassageId",current().id);
  b.putInt("map114FocusStart",focusStart);
  b.putInt("map114FocusEnd",focusEnd);
 }
 boolean back(){
  if(titleExpanded){closeTitle();return true;}
  if(!overview){setLevel(true);return true;}
  return false;
 }
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

 /** Five-position sliding unit rail and permanent physical QCF page overview.
  * No source unit is split, omitted or renumbered; this is a presentation window only.
  */
 private int unitCount(){return macros.forSurah(surah).size();}
 private int visibleUnitCount(){return Math.min(5,unitCount());}
 private int maxUnitWindowStart(){return Math.max(0,unitCount()-visibleUnitCount());}
 private int centerUnitWindow(int ordinal){
  return Math.max(0,Math.min(maxUnitWindowStart(),ordinal-visibleUnitCount()/2));
 }
 private void shiftUnitWindow(int direction){
  int next=Math.max(0,Math.min(maxUnitWindowStart(),unitWindowStart+direction));
  if(next==unitWindowStart)return;
  unitWindowStart=next;
  overviewGraph.invalidate();
 }
 private int atlasHeight(int width,int height){
  // One unit row only. Passage selection and its separate cursor live in Lecture.
  return Ui.dp(activity,height<Ui.dp(activity,700)?64:74);
 }
 private float atlasX(int visibleOrdinal,int width){
  return width*(visibleOrdinal+.5f)/visibleUnitCount();
 }
 private float atlasY(){return Ui.dp(activity,35);}
 private int atlasUnitAt(float x,float y,int width){
  if(Math.abs(y-atlasY())>Ui.dp(activity,25))return -1;
  int nearest=-1;float best=Ui.dp(activity,27)*Ui.dp(activity,27);
  for(int j=0;j<visibleUnitCount();j++){
   float dx=x-atlasX(j,width);
   if(dx*dx<best){best=dx*dx;nearest=unitWindowStart+j;}
  }
  return nearest;
 }
 private void drawAtlas(Canvas c,Paint p,int width,int height){
  java.util.List<MawdudiMapData.Macro> units=macros.forSurah(surah);
  int total=units.size(),visible=visibleUnitCount();
  p.setAntiAlias(true);p.setColor(Ui.INK);
  p.setStrokeWidth(Ui.dp(activity,1));p.setTextAlign(Paint.Align.CENTER);
  float y=atlasY();
  if(visible>1){
   p.setStyle(Paint.Style.STROKE);
   c.drawLine(atlasX(0,width)+Ui.dp(activity,8),y,
       atlasX(visible-1,width)-Ui.dp(activity,8),y,p);
  }
  for(int j=0;j<visible;j++){
   int ordinal=unitWindowStart+j;float x=atlasX(j,width);
   p.setStyle(Paint.Style.FILL);
   c.drawCircle(x,y,Ui.dp(activity,ordinal==activeMacro?9:5),p);
   if(ordinal==activeMacro){
    p.setStyle(Paint.Style.STROKE);
    p.setStrokeWidth(Ui.dp(activity,1));
    c.drawCircle(x,y,Ui.dp(activity,14),p);
    c.drawCircle(x,y,Ui.dp(activity,18),p);
    p.setStyle(Paint.Style.FILL);
   }
   p.setTextSize(Ui.dp(activity,11));
   c.drawText("U"+(ordinal+1),x,y-Ui.dp(activity,22),p);
  }
  // Small edge marks communicate that further units exist off-screen.
  p.setTextSize(Ui.dp(activity,12));
  if(unitWindowStart>0)c.drawText("‹",Ui.dp(activity,5),y+Ui.dp(activity,4),p);
  if(unitWindowStart+visible<total)c.drawText("›",width-Ui.dp(activity,5),y+Ui.dp(activity,4),p);
  // QCF page pastilles are below the Mushaf: no duplicate page rail here.

 }
 private final class OverviewGraph extends View {
  final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
  float downX,downY;
  OverviewGraph(){super(activity);setFocusable(true);}
  int position(int ignored){return 0;}
  @Override protected void onMeasure(int w,int h){
   setMeasuredDimension(Math.max(1,MeasureSpec.getSize(w)),
       axis==null||axis.getLayoutParams()==null?sunHeight:axis.getLayoutParams().height);
  }
  @Override protected void onDraw(Canvas canvas){drawAtlas(canvas,paint,getWidth(),getHeight());}
  @Override public boolean onTouchEvent(MotionEvent event){
   if(event.getAction()==MotionEvent.ACTION_DOWN){
    downX=event.getX();downY=event.getY();return true;
   }
   if(event.getAction()!=MotionEvent.ACTION_UP)return true;
   float dx=event.getX()-downX,dy=event.getY()-downY;
   if(Math.abs(dx)>Ui.dp(activity,40)&&Math.abs(dx)>Math.abs(dy)*1.2f
       &&Math.abs(downY-atlasY())<Ui.dp(activity,30)){
    shiftUnitWindow(dx>0?-1:1);return true;
   }
   int i=atlasUnitAt(event.getX(),event.getY(),getWidth());
   if(i<0)return true;
   openMacro(i);performClick();return true;
  }
  @Override public boolean performClick(){super.performClick();return true;}
 }
}
