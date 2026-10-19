package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.brotli.dec.BrotliInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** A single authentic QCF folio. No script interface, network, corpus or learning state. */
final class CorrespondenceQcfSurface extends LinearLayout {
 static final int PAPER=0xfffaf8f2;
 static final class State {
  final int page,x,y;final float zoom;
  State(int page,float zoom,int x,int y){this.page=page;this.zoom=zoom;this.x=x;this.y=y;}
 }
 private final WebView web;
 private final TextView position;
 private final ImageButton previous,next;
 private final ExecutorService decoder=Executors.newSingleThreadExecutor();
 private Future<?> pending;
 private CorrespondenceDocument.Focus focus;
 private volatile boolean disposed;
 private volatile int generation;
 private int page;
 private boolean loading;
 private State restore;
 private State requestedState;
 private String expectedDocument="";
 private final java.util.Map<Integer,State> pageStates=new java.util.HashMap<>();
 private float downX,downY;

 CorrespondenceQcfSurface(Context context){
  super(context);setOrientation(VERTICAL);setBackgroundColor(PAPER);
  web=new WebView(context);web.setBackgroundColor(PAPER);
  WebSettings settings=web.getSettings();
  settings.setJavaScriptEnabled(false);settings.setAllowFileAccess(false);settings.setAllowContentAccess(false);
  settings.setBlockNetworkLoads(true);settings.setBlockNetworkImage(true);settings.setDomStorageEnabled(false);
  settings.setSupportMultipleWindows(false);settings.setSaveFormData(false);
  settings.setSupportZoom(true);settings.setBuiltInZoomControls(true);settings.setDisplayZoomControls(false);
  settings.setUseWideViewPort(false);settings.setLoadWithOverviewMode(false);
  web.setOverScrollMode(View.OVER_SCROLL_NEVER);
  web.setWebViewClient(new WebViewClient(){
   @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request){return true;}
   @Override public boolean shouldOverrideUrlLoading(WebView view,String url){return true;}
   @Override public void onPageFinished(WebView view,String url){
    if(disposed||restore==null||!expectedDocument.equals(url))return;
    loading=false;updateButtons();
    State target=restore;restore=null;
    // The ready callback may belong to the old document. Only restore the selected page.
    if(target.page==page)view.post(()->{if(!disposed&&!loading&&target.page==page)view.scrollTo(target.x,target.y);});
   }
  });
  web.setOnTouchListener((view,event)->{
   if(event.getActionMasked()==MotionEvent.ACTION_DOWN){downX=event.getX();downY=event.getY();}
   if(event.getActionMasked()==MotionEvent.ACTION_UP){
    float dx=event.getX()-downX,dy=event.getY()-downY;
    if(zoom()<=1.05f&&Math.abs(dx)>Ui.dp(context,56)&&Math.abs(dx)>Math.abs(dy)*1.4f){
     step(dx>0?1:-1);return true;
    }
   }
   return false;
  });
  addView(web,new LayoutParams(LayoutParams.MATCH_PARENT,0,1));
  LinearLayout footer=new LinearLayout(context);footer.setGravity(Gravity.CENTER_VERTICAL);
  previous=action(context,R.drawable.ic_ui_back,"Page QCF précédente",v->step(-1));
  next=action(context,R.drawable.ic_ui_back,"Page QCF suivante",v->step(1));next.setRotation(180);
  position=new TextView(context);position.setTextColor(Ui.INK);position.setTextSize(11);position.setGravity(Gravity.CENTER);
  footer.addView(previous,new LayoutParams(Ui.dp(context,48),Ui.dp(context,48)));
  footer.addView(position,new LayoutParams(0,LayoutParams.WRAP_CONTENT,1));
  footer.addView(next,new LayoutParams(Ui.dp(context,48),Ui.dp(context,48)));
  addView(footer,new LayoutParams(LayoutParams.MATCH_PARENT,Ui.dp(context,48)));
 }
 static ImageButton action(Context context,int resource,String description,OnClickListener click){
  ImageButton button=new ImageButton(context);button.setImageResource(resource);
  // Keep the individual vector's black/grey/white fills. Ui.setButtonIcon applies a global tint.
  button.setImageTintList(null);button.setBackgroundColor(Color.TRANSPARENT);button.setContentDescription(description);
  button.setTooltipText(description);button.setPadding(Ui.dp(context,12),Ui.dp(context,12),Ui.dp(context,12),Ui.dp(context,12));
  button.setMinimumWidth(Ui.dp(context,48));button.setMinimumHeight(Ui.dp(context,48));
  button.setStateListAnimator(null);button.setOnClickListener(click);return button;
 }
 void show(CorrespondenceDocument.Focus requested,State state){
  if(disposed)throw new IllegalStateException("Destroyed QCF surface");
  focus=requested;pageStates.clear();int target=state==null?focus.initialPage:state.page;
  if(target<focus.firstPage||target>focus.lastPage)target=focus.initialPage;
  load(new State(target,state==null?1f:state.zoom,state==null?0:state.x,state==null?0:state.y));
 }
 State state(){return loading&&requestedState!=null?requestedState:new State(page,zoom(),web.getScrollX(),web.getScrollY());}
 void restoreViewport(State state){
  web.post(()->{
   if(disposed||loading||state.page!=page)return;
   float factor=state.zoom/zoom();
   if(Math.abs(factor-1f)>0.01f)web.zoomBy(factor);
   web.scrollTo(state.x,state.y);
  });
 }
 private float zoom(){return Math.max(0.1f,web.getScale()/getResources().getDisplayMetrics().density);}
 private void step(int delta){
  if(disposed||focus==null||loading)return;
  int target=page+delta;if(target<focus.firstPage||target>focus.lastPage)return;
  pageStates.put(page,state());
  State remembered=pageStates.get(target);
  load(remembered==null?new State(target,1f,0,0):remembered);
 }
 private void load(State state){
  page=state.page;loading=true;restore=null;requestedState=state;
  int request=++generation;if(pending!=null)pending.cancel(true);
  position.setText("p. "+page+" · chargement");updateButtons();
  CorrespondenceDocument.Focus requestFocus=focus;
  // Do not clear the old folio while decoding: avoid a white/black flash on E-Ink.
  pending=decoder.submit(()->{
   try{
    String svg=readSvg(getContext(),state.page);
    CorrespondenceDocument.Render rendered=CorrespondenceDocument.render(svg,requestFocus,state.page);
    String html=CorrespondenceDocument.html(rendered.svg);
    web.post(()->{
     if(disposed||generation!=request)return;
     restore=state;web.setInitialScale(Math.max(1,Math.round(state.zoom*100)));
     // Synthetic document identity only: no request is made to this reserved host.
     // It lets onPageFinished reject an older page after a fast occurrence change.
     expectedDocument="https://qcf.invalid/document/"+request;
     web.loadDataWithBaseURL(expectedDocument,html,"text/html","UTF-8",null);
     position.setText("p. "+page+(rendered.focusVerified?"":" · VISUEL_NON_VALIDÉ"));updateButtons();
    });
   }catch(Exception unavailable){
    web.post(()->{
     if(disposed||generation!=request)return;
     loading=false;position.setText("p. "+page+" · QCF indisponible");updateButtons();
     // An old folio must never be labelled as this missing page.
     web.loadDataWithBaseURL(null,"<html><body style='background:#faf8f2'></body></html>","text/html","UTF-8",null);
    });
   }
  });
 }
 private void updateButtons(){previous.setEnabled(!loading&&focus!=null&&page>focus.firstPage);next.setEnabled(!loading&&focus!=null&&page<focus.lastPage);}
 private static String readSvg(Context context,int page)throws Exception{
  String path=String.format(Locale.ROOT,"mushaf/hafs/kfqc/svg-br/%03d.svg.br",page);
  try(InputStream compressed=context.getAssets().open(path);BrotliInputStream input=new BrotliInputStream(compressed);ByteArrayOutputStream output=new ByteArrayOutputStream()){
   byte[] buffer=new byte[8192];int count;
   while((count=input.read(buffer))!=-1){
    if(Thread.currentThread().isInterrupted())throw new InterruptedException("QCF decode cancelled");
    if(output.size()+count>850_000)throw new IllegalStateException("Oversized authentic QCF asset");
    output.write(buffer,0,count);
   }
   String svg=new String(output.toByteArray(),StandardCharsets.UTF_8);
   if(!svg.contains("viewBox=\"0 0 345 550\"")||!svg.contains("</svg>"))throw new IllegalStateException("QCF coordinate space missing");
   return svg;
  }
 }
 void destroy(){
  if(disposed)return;disposed=true;++generation;
  if(pending!=null)pending.cancel(true);decoder.shutdownNow();
  web.stopLoading();web.setOnTouchListener(null);web.setWebViewClient(new WebViewClient());
  removeView(web);web.destroy();
 }
}
