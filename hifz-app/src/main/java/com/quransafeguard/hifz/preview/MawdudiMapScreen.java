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
 private boolean ready,expanded;
 private java.util.List<MawdudiPassageIndex.Passage> passages;
 private MushafView mushaf;
 private Graph graph;
 private HorizontalScrollView axis;
 private TextView heading,pageLabel;
 private LinearLayout icons;
 private Dialog popup;
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
   page=MawdudiMapModel.clampPage(current(),saved.getInt("map114Page",page),geometry);
   int first=saved.getInt("map114FocusStart",focusStart),last=saved.getInt("map114FocusEnd",focusEnd);
   if(current().contains(first)&&current().contains(last)&&first<=last){focusStart=first;focusEnd=last;}
   expanded=saved.getBoolean("map114Expanded",false);
  }
 }
 private MawdudiPassageIndex.Passage current(){return passages.get(selected);}
 private void choose(int s,int ayah) {
  surah=s;passages=index.forSurah(s);MawdudiPassageIndex.Passage p=index.at(s,ayah);
  selected=passages.indexOf(p);page=MawdudiMapModel.firstPage(p,geometry);focusStart=p.firstAyah;focusEnd=p.lastAyah;
 }
 void display() {
  LinearLayout root=Ui.column(activity);root.setBackgroundColor(Ui.PAPER);
  LinearLayout top=Ui.row(activity);top.setGravity(Gravity.CENTER_VERTICAL);
  top.addView(Ui.iconButton(activity,"","Retour",v->activity.finish()),new LinearLayout.LayoutParams(Ui.dp(activity,42),Ui.dp(activity,44)));
  heading=Ui.text(activity,"",12f,false);heading.setMaxLines(2);heading.setEllipsize(TextUtils.TruncateAt.END);
  top.addView(heading,new LinearLayout.LayoutParams(0,Ui.dp(activity,44),1));
  top.addView(Ui.iconButton(activity,"","Choisir une sourate",v->QuranSurahNames.showSurahPicker(activity,s->{choose(s,1);refresh();})),new LinearLayout.LayoutParams(Ui.dp(activity,42),Ui.dp(activity,44)));
  root.addView(top);
  axis=new HorizontalScrollView(activity);axis.setHorizontalScrollBarEnabled(false);
  graph=new Graph();axis.addView(graph,new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,Ui.dp(activity,78)));
  root.addView(axis,new LinearLayout.LayoutParams(-1,Ui.dp(activity,78)));
  mushaf=new MushafView(activity);mushaf.setListener(this);root.addView(mushaf,new LinearLayout.LayoutParams(-1,0,1));
  icons=Ui.row(activity);icons.setGravity(Gravity.CENTER_VERTICAL);
  icons.addView(Ui.iconButton(activity,"","Page précédente",v->turn(-1)),new LinearLayout.LayoutParams(Ui.dp(activity,44),Ui.dp(activity,44)));
  pageLabel=Ui.text(activity,"",12f,false);pageLabel.setGravity(Gravity.CENTER);
  icons.addView(pageLabel,new LinearLayout.LayoutParams(0,Ui.dp(activity,44),1));
  icons.addView(Ui.iconButton(activity,"","Lecture",v->openReader()),new LinearLayout.LayoutParams(Ui.dp(activity,44),Ui.dp(activity,44)));
  icons.addView(Ui.iconButton(activity,"","Focus",v->{expanded=!expanded;axis.setVisibility(expanded?View.GONE:View.VISIBLE);}),new LinearLayout.LayoutParams(Ui.dp(activity,44),Ui.dp(activity,44)));
  icons.addView(Ui.iconButton(activity,"","Page suivante",v->turn(1)),new LinearLayout.LayoutParams(Ui.dp(activity,44),Ui.dp(activity,44)));
  root.addView(icons);
  activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
  activity.setContentView(root);axis.setVisibility(expanded?View.GONE:View.VISIBLE);refresh();
 }
 MushafView mushaf(){return mushaf;}
 private void closeTitle(){if(popup!=null&&popup.isShowing())popup.dismiss();popup=null;}
 private void showTitle(String text){closeTitle();popup=SemanticTitlePopup.showEnglish(activity,text,null);SemanticTitlePopup.positionAboveIcons(activity,popup,icons);}
 private void select(int n,int a,int b,String title) {
  selected=n;focusStart=a;focusEnd=b;page=geometry.pageForVerse(new VerseRef(surah,a));refresh();showTitle(title);
 }
 void refresh(){
  if(heading==null)return;
  MawdudiMapData.Macro macro=macros.at(surah,focusStart);
  heading.setText(QuranSurahNames.labelFor(surah)+" · "+macro.label);heading.setContentDescription(heading.getText());
  graph.requestLayout();graph.invalidate();
  axis.post(()->axis.scrollTo(Math.max(0,graph.position(selected)-axis.getWidth()/3),0));
  renderPage();
 }
 private void renderPage(){
  if(!ready)return;
  MawdudiPassageIndex.Passage p=current();page=MawdudiMapModel.clampFocusPage(surah,focusStart,focusEnd,page,geometry);
  java.util.List<VerseRef> visible=new ArrayList<>();
  for(VerseRef v:MawdudiMapModel.highlightedVerses(p,page,geometry))if(v.getAyah()>=focusStart&&v.getAyah()<=focusEnd)visible.add(v);
  java.util.List<String> lines=geometry.lineIdsForVerseRange(new VerseRef(surah,focusStart),new VerseRef(surah,focusEnd));
  mushaf.setHighlightVerses(visible);mushaf.show(page,visible,lines,0,true);
  pageLabel.setText(page+" · "+geometry.pageForVerse(new VerseRef(surah,focusStart))+"–"+MawdudiMapModel.lastPageForVerse(new VerseRef(surah,focusEnd),geometry));
 }
 private void turn(int d){int next=MawdudiMapModel.clampFocusPage(surah,focusStart,focusEnd,page+d,geometry);if(next==page)return;closeTitle();page=next;renderPage();}
 void openReader(){
  java.util.List<VerseRef> visible=MawdudiMapModel.highlightedVerses(current(),page,geometry);if(visible.isEmpty())return;
  Intent intent=new Intent(activity,StudyReaderActivity.class).putExtra(StudyReaderActivity.EXTRA_JUMP_PAGE,page)
   .putExtra(StudyReaderActivity.EXTRA_JUMP_VERSE,visible.get(0).toString()).putExtra(StudyReaderActivity.EXTRA_MAP_PREVIEW,true)
   .putExtra("map114PassageId",current().id).putExtra("map114FocusStart",focusStart).putExtra("map114FocusEnd",focusEnd);
  closeTitle();activity.startActivity(intent);
 }
 void save(Bundle b){b.putInt("map114Surah",surah);b.putInt("map114Ayah",current().firstAyah);b.putInt("map114Page",page);b.putInt("map114FocusStart",focusStart);b.putInt("map114FocusEnd",focusEnd);b.putBoolean("map114Expanded",expanded);}
 boolean back(){if(popup!=null&&popup.isShowing()){closeTitle();return true;}return false;}
 void destroy(){closeTitle();if(mushaf!=null)mushaf.destroySafely();}
 @Override public void onReady(){ready=true;renderPage();}
 @Override public void onPageShown(int p){}
 @Override public void onError(String error){pageLabel.setText("Muṣḥaf indisponible");}
 @Override public void onPageSwipe(int delta){turn(delta);}
 @Override public void onVerseTap(VerseRef verse){openReader();}
 @Override public void onSurfaceTap(){openReader();}
 private final class Graph extends View {
  final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);final int step=Ui.dp(activity,178);
  final Drawable key=activity.getDrawable(Ui.iconFor("Afficher les amorces",""));
  Graph(){super(activity);setFocusable(true);}
  boolean split(int n){MawdudiPassageIndex.Passage p=passages.get(n);return p.surah==27&&p.firstAyah==67&&p.lastAyah==82&&subnodes.length()==3;}
  int units(int n){return split(n)?3:1;}
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
    // Two compact source-title lines above the unchanged Lucide key; full title is on tap.
    String first=title,second="";if(paint.measureText(title)>step-16){int cut=paint.breakText(title,true,step-20,null);int space=title.lastIndexOf(' ',cut);if(space>0)cut=space;first=title.substring(0,cut);second=title.substring(cut).trim();}
    canvas.drawText(first,x,Ui.dp(activity,15),paint);
    if(!second.isEmpty())canvas.drawText(TextUtils.ellipsize(second,new android.text.TextPaint(paint),step-16,TextUtils.TruncateAt.END).toString(),x,Ui.dp(activity,29),paint);
    paint.setColor(Ui.PAPER);canvas.drawCircle(x,y,Ui.dp(activity,11),paint);
    key.setBounds((int)x-Ui.dp(activity,8),(int)y-Ui.dp(activity,8),(int)x+Ui.dp(activity,8),(int)y+Ui.dp(activity,8));key.setAlpha(n==selected?255:155);key.draw(canvas);
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
