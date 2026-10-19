package com.quransafeguard.hifz.preview;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.LinkedHashSet;

/** Isolated, demand-only reader. No production relation is provided or inferred here. */
public final class CorrespondenceScreen extends Dialog {
 public interface Listener { void onClosed(CorrespondenceModel.ReaderSnapshot origin); }
 private final Activity activity;
 private final CorrespondenceModel.Relation relation;
 private final CorrespondenceModel.Geometry geometry;
 private final CorrespondenceModel.Navigation navigation;
 private final Listener listener;
 private final CorrespondenceQcfSurface[] surfaces;
 private final LinearLayout[] panels;
 private LinearLayout body;
 private TextView heading;
 private OccurrenceRail rail;
 private boolean closed;
 private int occurrence,windowStart;
 private CorrespondenceQcfSurface.State[] comparison;

 /** Returns false without constructing a dialog when any editorial/visual/QA gate is closed. */
 public static boolean show(Activity activity,CorrespondenceModel.Relation relation,
                            CorrespondenceModel.ReaderSnapshot origin,CorrespondenceModel.Geometry geometry,
                            Listener listener){
  if(relation==null||!relation.displayable())return false;
  if(activity==null||activity.isFinishing()||activity.isDestroyed()||origin==null||geometry==null)return false;
  new CorrespondenceScreen(activity,relation,origin,geometry,listener).show();return true;
 }
 private CorrespondenceScreen(Activity activity,CorrespondenceModel.Relation relation,
                              CorrespondenceModel.ReaderSnapshot origin,CorrespondenceModel.Geometry geometry,Listener listener){
  super(activity);this.activity=activity;this.relation=relation;this.geometry=geometry;this.listener=listener;
  navigation=new CorrespondenceModel.Navigation(relation,origin);
  int count=relation.mode==CorrespondenceModel.Mode.TWO_PAGES?2:1;
  surfaces=new CorrespondenceQcfSurface[count];panels=new LinearLayout[count];
  setOnDismissListener(dialog->{
   if(closed)return;closed=true;
   for(CorrespondenceQcfSurface surface:surfaces)if(surface!=null)surface.destroy();
   if(listener!=null)listener.onClosed(navigation.returnToOrigin());
  });
 }
 @Override protected void onCreate(Bundle state){
  super.onCreate(state);requestWindowFeature(Window.FEATURE_NO_TITLE);
  LinearLayout root=new LinearLayout(activity);root.setOrientation(LinearLayout.VERTICAL);
  root.setBackgroundColor(CorrespondenceQcfSurface.PAPER);
  LinearLayout toolbar=new LinearLayout(activity);toolbar.setGravity(Gravity.CENTER_VERTICAL);
  toolbar.addView(CorrespondenceQcfSurface.action(activity,R.drawable.ic_ui_back,"Retour",v->back()),square(48));
  ImageView symbol=new ImageView(activity);symbol.setImageResource(icon(relation.type));symbol.setImageTintList(null);
  symbol.setContentDescription(label(relation.type));symbol.setPadding(4,4,4,4);toolbar.addView(symbol,square(36));
  heading=new TextView(activity);heading.setTextColor(Ui.INK);heading.setTextSize(15);heading.setMaxLines(2);
  toolbar.addView(heading,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));
  toolbar.addView(CorrespondenceQcfSurface.action(activity,R.drawable.ic_ui_info,"Sources de la correspondance",v->sources()),square(48));
  root.addView(toolbar,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,Ui.dp(activity,56)));
  if(relation.mode==CorrespondenceModel.Mode.SERIES){
   rail=new OccurrenceRail();root.addView(rail,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,Ui.dp(activity,64)));
  }
  body=new LinearLayout(activity);body.setOrientation(LinearLayout.HORIZONTAL);
  root.addView(body,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1));
  for(int p=0;p<surfaces.length;p++){
   final int pane=p;LinearLayout panel=new LinearLayout(activity);panel.setOrientation(LinearLayout.VERTICAL);
   LinearLayout bar=new LinearLayout(activity);bar.setGravity(Gravity.CENTER_VERTICAL);
   TextView ref=new TextView(activity);ref.setTextColor(Ui.INK);ref.setTextSize(12);
   CorrespondenceModel.VerseRange range=relation.originals.get(p);
   ref.setText(reference(range));bar.addView(ref,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));
   if(surfaces.length==2)bar.addView(CorrespondenceQcfSurface.action(activity,R.drawable.ic_ui_focus,"Agrandir le feuillet "+(p==0?"A":"B"),v->enlarge(pane)),square(48));
   if(relation.mode!=CorrespondenceModel.Mode.SERIES)
    panel.addView(bar,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,Ui.dp(activity,48)));
   surfaces[p]=new CorrespondenceQcfSurface(activity);panel.addView(surfaces[p],new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1));
   panels[p]=panel;
   surfaces[p].show(CorrespondenceDocument.focus(relation,p,0,geometry),null);
  }
  setContentView(root);layoutPanels();updateHeading();
 }
 @Override public void onStart(){
  super.onStart();Window window=getWindow();
  if(window!=null){window.setBackgroundDrawable(new ColorDrawable(CorrespondenceQcfSurface.PAPER));window.setWindowAnimations(0);
   window.setDimAmount(0);window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT);}
 }
 private LinearLayout.LayoutParams square(int dp){int size=Ui.dp(activity,dp);return new LinearLayout.LayoutParams(size,size);}
 private static String reference(CorrespondenceModel.VerseRange r){return r.surah+":"+r.start+(r.end==r.start?"":"–"+r.end);}
 private void remember(int pane){
  CorrespondenceQcfSurface.State state=surfaces[pane].state();
  CorrespondenceModel.Pane old=navigation.pane(pane);
  navigation.setPane(pane,state.page,state.zoom,old.selection,state.x,state.y);
 }
 private void enlarge(int pane){
  if(navigation.enlargedPane()>=0)return;
  comparison=new CorrespondenceQcfSurface.State[surfaces.length];
  for(int p=0;p<surfaces.length;p++){comparison[p]=surfaces[p].state();remember(p);}
  navigation.enlarge(pane);layoutPanels();surfaces[pane].restoreViewport(comparison[pane]);updateHeading();
 }
 private void layoutPanels(){
  // The WebViews remain the same objects: no page reload or mirrored A/B state.
  body.removeAllViews();int enlarged=navigation.enlargedPane();
  for(int p=0;p<panels.length;p++)if(enlarged<0||enlarged==p)
   body.addView(panels[p],new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.MATCH_PARENT,1));
 }
 private void back(){
  if(navigation.enlargedPane()>=0){
   int pane=navigation.enlargedPane();remember(pane);
   // Return restores the comparison's two independent positions exactly as selected.
   navigation.showComparison();layoutPanels();
   for(int p=0;p<surfaces.length;p++){
    CorrespondenceQcfSurface.State target=comparison[p];
    if(surfaces[p].state().page!=target.page)surfaces[p].show(CorrespondenceDocument.focus(relation,p,occurrence,geometry),target);
    else surfaces[p].restoreViewport(target);
   }
   updateHeading();return;
  }
  dismiss();
 }
 @Override public void onBackPressed(){back();}
 private void select(int target){
  if(target<0||target>=relation.anchors.size()||target==occurrence)return;
  remember(0);navigation.selectOccurrence(target);occurrence=target;
  CorrespondenceModel.Pane pane=navigation.pane(0);
  surfaces[0].show(CorrespondenceDocument.focus(relation,0,target,geometry),
   new CorrespondenceQcfSurface.State(pane.page,(float)pane.zoom,pane.scrollX,pane.scrollY));
  windowStart=Math.max(0,Math.min(relation.anchors.size()-rail.count(),occurrence-rail.count()/2));rail.invalidate();updateHeading();
 }
 private void updateHeading(){
  String text=label(relation.type);
  if(relation.mode==CorrespondenceModel.Mode.SERIES)text+=" · "+(occurrence+1)+"/"+relation.anchors.size()+" · "+reference(navigation.activeAnchor().range);
  if(navigation.enlargedPane()>=0)text+=" · "+(navigation.enlargedPane()==0?"A":"B");
  heading.setText(text);
 }
 private void sources(){
  LinkedHashSet<String> locators=new LinkedHashSet<>();
  for(CorrespondenceModel.Anchor anchor:relation.anchors)locators.add(anchor.sourceLocator);
  StringBuilder message=new StringBuilder(relation.id);
  for(String locator:locators)message.append("\n\n").append(locator);
  if(relation.type==CorrespondenceModel.Type.CENTRE)message.append("\n\nVISUEL_NON_VALIDÉ : aucune géométrie de segment central fournie à ce rendu. Page authentique intacte.");
  new AlertDialog.Builder(activity).setTitle("Sources").setMessage(message).setPositiveButton("Fermer",null).show();
 }
 private final class OccurrenceRail extends View {
  final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);float downX,downY;
  OccurrenceRail(){super(activity);setFocusable(true);setContentDescription("Occurrences. Balayer latéralement pour parcourir la frise; flèches clavier pour sélectionner.");}
  int count(){return Math.min(Math.min(5,relation.anchors.size()),getWidth()==0?5:Math.max(1,getWidth()/Ui.dp(activity,48)));}
  @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){
   super.onSizeChanged(w,h,oldw,oldh);windowStart=Math.max(0,Math.min(relation.anchors.size()-count(),occurrence-count()/2));
  }
  float x(int index){return getWidth()*(index+.5f)/count();}
  @Override protected void onDraw(Canvas canvas){
   super.onDraw(canvas);paint.setColor(Ui.INK);paint.setStrokeWidth(Ui.dp(activity,1));float y=Ui.dp(activity,31);
   if(count()>1)canvas.drawLine(x(0)+Ui.dp(activity,8),y,x(count()-1)-Ui.dp(activity,8),y,paint);
   for(int j=0;j<count();j++){
    int ordinal=windowStart+j;boolean active=ordinal==occurrence;
    paint.setStyle(Paint.Style.FILL);canvas.drawCircle(x(j),y,Ui.dp(activity,active?9:5),paint);
    if(active){paint.setStyle(Paint.Style.STROKE);canvas.drawCircle(x(j),y,Ui.dp(activity,14),paint);}
   }
   paint.setStyle(Paint.Style.FILL);paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(Ui.dp(activity,12));
   if(windowStart>0)canvas.drawText("‹",Ui.dp(activity,5),y+Ui.dp(activity,4),paint);
   if(windowStart+count()<relation.anchors.size())canvas.drawText("›",getWidth()-Ui.dp(activity,5),y+Ui.dp(activity,4),paint);
  }
  void shift(int delta){windowStart=Math.max(0,Math.min(relation.anchors.size()-count(),windowStart+delta));invalidate();}
  @Override public boolean onTouchEvent(MotionEvent event){
   if(event.getActionMasked()==MotionEvent.ACTION_DOWN){downX=event.getX();downY=event.getY();return true;}
   if(event.getActionMasked()!=MotionEvent.ACTION_UP)return true;
   float dx=event.getX()-downX,dy=event.getY()-downY;
   if(Math.abs(dx)>Ui.dp(activity,40)&&Math.abs(dx)>Math.abs(dy)*1.2f){shift(dx>0?-1:1);return true;}
   if(Math.abs(event.getY()-Ui.dp(activity,31))<=Ui.dp(activity,24)&&getWidth()>0){
    int slot=Math.max(0,Math.min(count()-1,(int)(event.getX()*count()/getWidth())));select(windowStart+slot);performClick();
   }
   return true;
  }
  @Override public boolean onGenericMotionEvent(MotionEvent event){
   if(event.getAction()==MotionEvent.ACTION_SCROLL){float delta=event.getAxisValue(MotionEvent.AXIS_HSCROLL);if(delta==0)delta=-event.getAxisValue(MotionEvent.AXIS_VSCROLL);if(delta!=0){shift(delta>0?1:-1);return true;}}
   return super.onGenericMotionEvent(event);
  }
  @Override public boolean onKeyDown(int key,KeyEvent event){
   if(key==KeyEvent.KEYCODE_DPAD_RIGHT||key==KeyEvent.KEYCODE_DPAD_LEFT){select(occurrence+(key==KeyEvent.KEYCODE_DPAD_RIGHT?1:-1));return true;}
   return super.onKeyDown(key,event);
  }
  @Override public boolean performClick(){super.performClick();announceForAccessibility("Occurrence "+(occurrence+1)+" sur "+relation.anchors.size());return true;}
 }
 static int icon(CorrespondenceModel.Type type){
  switch(type){
   case REPETITION:return R.drawable.ic_corr_relation_repetition;
   case RESSEMBLANCE:return R.drawable.ic_corr_relation_ressemblance;
   case OPPOSITION:return R.drawable.ic_corr_relation_opposition;
   case PROGRESSION:return R.drawable.ic_corr_relation_progression;
   case ALTERNANCE:return R.drawable.ic_corr_relation_alternance;
   case INVERSION:return R.drawable.ic_corr_relation_inversion;
   case CENTRE:return R.drawable.ic_corr_relation_centre;
   case DEBUT_FIN_INCLUSIO:return R.drawable.ic_corr_relation_debut_fin_inclusio;
   default:throw new IllegalArgumentException("Unknown relation icon");
  }
 }
 static String label(CorrespondenceModel.Type type){
  switch(type){
   case REPETITION:return "Répétition";case RESSEMBLANCE:return "Ressemblance";case OPPOSITION:return "Opposition";
   case PROGRESSION:return "Progression";case ALTERNANCE:return "Alternance";case INVERSION:return "Inversion";
   case CENTRE:return "Centre";case DEBUT_FIN_INCLUSIO:return "Début–Fin";
   default:throw new IllegalArgumentException("Unknown relation label");
  }
 }
}
