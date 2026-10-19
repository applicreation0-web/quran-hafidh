package com.quransafeguard.hifz.preview;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.quransafeguard.hifz.core.VerseRef;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeSet;
import static com.quransafeguard.hifz.preview.CorrespondenceModel.*;

/** Read-only contextual affordances; never opens content until explicitly touched. */
final class CorrespondenceEntryPoints {
 private CorrespondenceEntryPoints(){}
 static CorrespondenceAccess.Origin origin(Activity activity,CorrespondenceAccess.Surface surface,String mode,
   int surah,int ayah,int page,String unitId,String passageId,String mask,String selection,int first,int last){
  final GeometryRepository qcf=GeometryRepository.get(activity);
  Geometry geometry=new Geometry(){
   public int ayahCount(int s){return com.quransafeguard.hifz.core.QuranCanon.INSTANCE.ayahCount(s);}
   public int firstPage(int s,int a){return qcf.line(qcf.firstLineIndex(new VerseRef(s,a))).page;}
   public int lastPage(int s,int a){return qcf.line(qcf.lastLineIndex(new VerseRef(s,a))).page;}
   public boolean hasSegmentGeometry(int s,int a,int f,int l){return false;}
  };
  return new CorrespondenceAccess.Origin(surface,mode,new ReaderSnapshot(surah,ayah,page,value(unitId),value(passageId),value(mask),1,value(selection)),new VerseRange(surah,first,last,geometry));
 }
 static void populate(Activity activity,LinearLayout host,CorrespondenceAccess.Origin supplied){
  populateContexts(activity,host,supplied==null?Collections.emptyList():Collections.singletonList(supplied));
 }
 private static void populateContexts(Activity activity,LinearLayout host,List<CorrespondenceAccess.Origin> supplied){
  if(host==null)return;host.removeAllViews();host.setVisibility(View.GONE);
  if(supplied.isEmpty()||activity.isFinishing())return;
  CorrespondenceRuntime runtime=CorrespondenceRuntime.load(activity);
  if(runtime.geometry==null||runtime.catalog.relations.isEmpty())return;
  try {
   List<CorrespondenceAccess.Origin> origins=new ArrayList<>();
   for(CorrespondenceAccess.Origin o:supplied)origins.add(rebind(o,runtime.geometry));
   CorrespondenceAccess.Choices links=new CorrespondenceAccess(runtime.catalog.index,runtime.geometry).over(origins);
   if(links.visible.isEmpty())return;
   for(CorrespondenceAccess.Link link:links.visible)host.addView(button(activity,link.relation,v->launch(activity,link.relation,link.origin)));
   if(!links.remaining.isEmpty()){
    ImageView more=CorrespondenceQcfSurface.action(activity,R.drawable.ic_ui_info,"Autres correspondances",v->{
     List<CorrespondenceAccess.Link> others=links.remaining;String[] labels=new String[others.size()];
     for(int i=0;i<labels.length;i++)labels[i]=others.get(i).relation.presentation.shortTitle;
     new AlertDialog.Builder(activity).setTitle("Autres correspondances").setItems(labels,(d,which)->launch(activity,others.get(which).relation,others.get(which).origin)).show();
    });host.addView(more,new LinearLayout.LayoutParams(Ui.dp(activity,48),Ui.dp(activity,48)));
   }
   host.setVisibility(View.VISIBLE);
  }catch(RuntimeException invalidContext){host.removeAllViews();host.setVisibility(View.GONE);}
 }
 static void populateSabqi(Activity activity,LinearLayout host,String mode,List<VerseRef> selected,int page,int mask,List<String> lineIds){
  if(host==null)return;host.removeAllViews();host.setVisibility(View.GONE);
  if(!"SABQI".equals(mode)||selected==null||selected.isEmpty())return;
  // Preserve each exact contiguous selection independently, including a surah boundary or gaps.
  Map<Integer,TreeSet<Integer>> bySurah=new LinkedHashMap<>();
  for(VerseRef v:selected){TreeSet<Integer> ayahs=bySurah.get(v.getSurah());if(ayahs==null){ayahs=new TreeSet<>();bySurah.put(v.getSurah(),ayahs);}ayahs.add(v.getAyah());}
  List<CorrespondenceAccess.Origin> contexts=new ArrayList<>();
  for(Map.Entry<Integer,TreeSet<Integer>> group:bySurah.entrySet()){
   int first=-1,last=-1;
   for(int ayah:group.getValue()){
    if(first>=0&&ayah!=last+1){contexts.add(sabqiOrigin(activity,mode,group.getKey(),first,last,page,mask,selected,lineIds));first=-1;}
    if(first<0)first=ayah;last=ayah;
   }
   if(first>=0)contexts.add(sabqiOrigin(activity,mode,group.getKey(),first,last,page,mask,selected,lineIds));
  }
  populateContexts(activity,host,contexts);
 }
 private static CorrespondenceAccess.Origin sabqiOrigin(Activity a,String mode,int s,int first,int last,int page,int mask,List<VerseRef> selected,List<String> lines){
  return origin(a,CorrespondenceAccess.Surface.SABQI,mode,s,first,page,"","",Integer.toString(mask),selected.toString()+"|"+lines,first,last);
 }
 private static ImageView button(Activity activity,Relation r,View.OnClickListener click){
  ImageView button=CorrespondenceQcfSurface.action(activity,CorrespondenceScreen.icon(r.type),r.presentation.shortTitle,click);
  button.setImageTintList(null);button.setLayoutParams(new LinearLayout.LayoutParams(Ui.dp(activity,48),Ui.dp(activity,48)));return button;
 }
 private static void launch(Activity activity,Relation relation,CorrespondenceAccess.Origin origin){
  Intent intent=new Intent(activity,CorrespondenceActivity.class);intent.putExtra("relationId",relation.id);intent.putExtra("origin",encode(origin));activity.startActivity(intent);
 }
 static CorrespondenceAccess.Origin rebind(CorrespondenceAccess.Origin origin,Geometry geometry){
  return new CorrespondenceAccess.Origin(origin.surface,origin.mode,origin.snapshot,new VerseRange(origin.workedRange.surah,origin.workedRange.start,origin.workedRange.end,geometry));
 }
 static Bundle encode(CorrespondenceAccess.Origin o){
  Bundle b=new Bundle();ReaderSnapshot s=o.snapshot;b.putString("surface",o.surface.name());b.putString("mode",o.mode);
  b.putInt("surah",s.surah);b.putInt("ayah",s.ayah);b.putInt("page",s.page);b.putString("unit",s.unitId);b.putString("passage",s.passageId);
  b.putString("mask",s.mask);b.putString("selection",s.selection);b.putDouble("zoom",s.zoom);b.putInt("x",s.scrollX);b.putInt("y",s.scrollY);
  b.putInt("workedSurah",o.workedRange.surah);b.putInt("first",o.workedRange.start);b.putInt("last",o.workedRange.end);return b;
 }
 static CorrespondenceAccess.Origin decode(Bundle b,Geometry g){
  if(b==null)throw new IllegalArgumentException("origin missing");
  return new CorrespondenceAccess.Origin(CorrespondenceAccess.Surface.valueOf(b.getString("surface")),b.getString("mode"),
   new ReaderSnapshot(b.getInt("surah"),b.getInt("ayah"),b.getInt("page"),b.getString("unit"),b.getString("passage"),b.getString("mask"),b.getDouble("zoom"),b.getString("selection"),b.getInt("x"),b.getInt("y")),
   new VerseRange(b.getInt("workedSurah"),b.getInt("first"),b.getInt("last"),g));
 }
 private static String value(String s){return s==null?"":s;}
}
