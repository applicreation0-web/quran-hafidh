package com.quransafeguard.hifz.preview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import org.brotli.dec.BrotliInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Semantic route ONLY: authentic KFQC SVG page miniatures, not approximate text
 * boxes or screenshots. A single complete page stays inside its measured viewport. Unit pages
 * remain reachable through round page badges and reader-direction swipes.
 */
final class MapQcfThumbnails extends WebView {
 interface Listener { void onPageChosen(int page); default void onOpenReading(int page){onPageChosen(page);} }
 private String renderKey="";
 private int passageFirstPage,passageLastPage;
 private final Listener listener;
 private int start,end,selected;
 private int markSurah,markFirst,markLast;
 private boolean markOnlyBoundaries;
 private boolean mosaicMode;
 private int mosaicFirst=-1,mosaicGeneration;
 private final java.util.LinkedHashMap<Integer,String> vectorCache=
     new java.util.LinkedHashMap<Integer,String>(8,0.75f,true){
      @Override protected boolean removeEldestEntry(java.util.Map.Entry<Integer,String> entry){return size()>6;}
     };
 private String cachedSvg(int page)throws Exception{
  String svg=vectorCache.get(page);
  if(svg==null){svg=readSvg(getContext(),page);vectorCache.put(page,svg);}
  return svg;
 }
 /** The two QCF modes have separate render cache identities. */
 void setMosaic(boolean value) {
  if(mosaicMode==value)return;
  mosaicMode=value;mosaicFirst=-1;renderKey="";
 }
 MapQcfThumbnails(Context context,Listener listener){
  super(context);this.listener=listener;
  setBackgroundColor(Color.rgb(250,248,242));
  WebSettings settings=getSettings();settings.setJavaScriptEnabled(true);
  settings.setAllowFileAccess(false);settings.setAllowContentAccess(false);
  settings.setBlockNetworkLoads(true);settings.setDomStorageEnabled(false);
  settings.setBuiltInZoomControls(false);
  addJavascriptInterface(new Bridge(),"MapPages");
 }
 void showPages(int first,int last,int selectedPage){
  showPages(first,last,selectedPage,0,0,0,false);
 }
 void showPages(int first,int last,int selectedPage,int s,int firstAyah,int lastAyah,boolean boundariesOnly){
  showPages(first,last,selectedPage,s,firstAyah,lastAyah,boundariesOnly,boundariesOnly?0:first,boundariesOnly?0:last);
 }
 void showPages(int first,int last,int selectedPage,int s,int firstAyah,int lastAyah,boolean boundariesOnly,int focusFirstPage,int focusLastPage){
  if(s<0||s>114||firstAyah<0||lastAyah<0||firstAyah>lastAyah)throw new IllegalArgumentException("QCF verse highlight range");
  if(first<1||last>604||last<first)throw new IllegalArgumentException("QCF thumbnail range");
  int chosen=Math.max(first,Math.min(last,selectedPage));
  String key=first+":"+last+":"+chosen+":"+s+":"+firstAyah+":"+lastAyah+":"+boundariesOnly+":"+focusFirstPage+":"+focusLastPage+":"+mosaicMode;
  if(key.equals(renderKey))return;
  renderKey=key;start=first;end=last;selected=chosen;
  if(mosaicFirst<first||mosaicFirst>last||chosen<mosaicFirst||chosen>=mosaicFirst+6)
   mosaicFirst=first+((chosen-first)/6)*6;
  markSurah=s;markFirst=firstAyah;markLast=lastAyah;markOnlyBoundaries=boundariesOnly;
  passageFirstPage=focusFirstPage;passageLastPage=focusLastPage;
  drawChunk();
 }
 private void step(int delta){
  int next=Math.max(start,Math.min(end,selected+(delta<0?-1:1)));
  if(next!=selected&&listener!=null)listener.onPageChosen(next);
 }
 private static String readSvg(Context context,int page)throws Exception{
  String path=String.format(Locale.ROOT,"mushaf/hafs/kfqc/svg-br/%03d.svg.br",page);
  try(InputStream compressed=context.getAssets().open(path);
      BrotliInputStream input=new BrotliInputStream(compressed);
      ByteArrayOutputStream out=new ByteArrayOutputStream()){
   byte[] block=new byte[8192];int n;
   while((n=input.read(block))!=-1){
    if(out.size()+n>850_000)throw new IllegalStateException("Oversized QCF page; compare audited SVG corpus maximum 762348 bytes");
    out.write(block,0,n);
   }
   String svg=new String(out.toByteArray(),StandardCharsets.UTF_8);
   if(!svg.contains("<svg")||!svg.contains("</svg>"))throw new IllegalStateException("Invalid QCF page");
   // Original QCF viewBox is 345x550, but its SVG has no intrinsic dimensions.
   // The WebView default 300x150 distorted the aspect ratio and produced blank bands.
   return svg.replaceFirst("<svg ", "<svg width='345' height='550' ");
  }
 }
 /** The original SVG contains transparent exact 'ayahPolygon' paths, one
  * per physical verse shape. Highlight those paths directly, never fabricate
  * line-based boxes that also cover neighboring verses. */
 private String highlightExactVersePolygons(String svg){
  if(markSurah==0||markFirst==0)return svg;
  StringBuilder css=new StringBuilder("<style>");
  if(markOnlyBoundaries){
   appendSelector(css,markFirst);
   if(markLast!=markFirst){css.append(',');appendSelector(css,markLast);}
  }else{
   for(int a=markFirst;a<=markLast;a++){
    if(a>markFirst)css.append(',');
    appendSelector(css,a);
   }
  }
  css.append("{fill:#999!important;fill-opacity:0.23!important;stroke:#333!important;stroke-width:0.25!important;}</style>");
  return svg.replace("</svg>",css.toString()+"</svg>");
 }
 private void appendSelector(StringBuilder css,int ayah){
  css.append("path.ayahPolygon[surah='").append(markSurah)
     .append("'][ayah='").append(ayah).append("']");
 }
 /** Multiple authentic QCF SVG pages, fitted together with no vertical scroll.
  * Page taps select a page; the parent then opens the original full-page preview.
  */
 private void drawMosaic(){
  int count=Math.min(6,end-mosaicFirst+1);
  int columns=count<=3?count:count==4?2:3;
  int windowLast=mosaicFirst+count-1;
  int generation=++mosaicGeneration;
  int rows=(count+columns-1)/columns;
  StringBuilder html=new StringBuilder(8192);
  html.append("<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1'>")
      .append("<style>*{box-sizing:border-box}html,body{margin:0;width:100%;height:100%;overflow:hidden;background:#faf8f0;color:#121211;font:12px sans-serif}")
      .append(".grid{display:grid;width:100%;height:100%;min-height:0;padding:5px;gap:5px;align-content:center;grid-template-columns:repeat(")
      .append(columns).append(",minmax(0,1fr));grid-template-rows:repeat(").append(rows).append(",minmax(0,1fr))}")
      .append("button{border:1px solid #999;background:white;padding:2px;min-width:0;min-height:0;display:flex;flex-direction:column;align-items:center;justify-content:center;overflow:hidden}")
      .append("button.current{border:2px solid #121211;outline:2px solid #121211;outline-offset:-3px}")
      .append("img{object-fit:contain;flex:1;width:auto;height:auto;min-width:0;min-height:0;max-width:100%;max-height:calc(100% - 18px);aspect-ratio:345/550;pointer-events:none}")
      .append("b{display:inline-block;min-height:14px;font-size:10px;line-height:14px;border-radius:9px;padding:0 4px;font-weight:500}")
      .append(".even b{background:#121211;color:#faf8f0}.odd b{background:#faf8f0;color:#121211;border:1px solid #888}")
      .append(".current b{outline:1px solid #121211;outline-offset:1px;font-weight:bold}</style></head><body><div class='grid'>");
  for(int p=mosaicFirst;p<=windowLast;p++){
   html.append("<button class='").append(p%2==0?"even":"odd").append(p==selected?" current":"")
      .append("' aria-label='Page ").append(p).append("' onclick='MapPages.choose(").append(p).append(")'>")
      .append("<img id='qcf").append(p).append("' alt='Muṣḥaf ").append(p)
      .append("' loading='lazy'><b>").append(p).append("</b></button>");
  }
  // Preserve the original SVG vectors. Rasterizing at 318x486 blurred
  // QCF text when WebView or the E-Ink display scaled a thumbnail.
  // Decode one authentic local page at a time, no canvas or PNG conversion.
  html.append("</div>");
  if(end-start>=6)html.append("<div style=\'height:24px;text-align:center;font:12px sans-serif\'>")
      .append("<button style=\'height:23px;width:40px\' onclick=\'MapPages.shift(-1)\'>‹</button> ")
      .append(mosaicFirst).append("–").append(windowLast).append(" / ").append(start).append("–").append(end)
      .append(" <button style=\'height:23px;width:40px\' onclick=\'MapPages.shift(1)\'>›</button></div>");
  html.append("<script>")
      .append("var nextPage=").append(mosaicFirst).append(",lastPage=").append(windowLast).append(",generation=").append(generation).append(";")
      .append("function requestNext(){if(nextPage<=lastPage)MapPages.thumb(nextPage++,generation);}")
      .append("function receiveThumb(p,src,gen){if(gen!==generation)return;var el=document.getElementById('qcf'+p);")
      .append("if(!el){requestNext();return;}")
      .append("el.onload=function(){el.onload=null;el.onerror=null;requestNext();};")
      .append("el.onerror=function(){el.alt='Miniature indisponible';el.onload=null;el.onerror=null;requestNext();};")
      .append("if(!src){el.onerror();return;}el.src='data:image/svg+xml;base64,'+src;}")
      .append("window.addEventListener('load',requestNext);</script></body></html>");
  loadDataWithBaseURL(null,html.toString(),"text/html","UTF-8",null);
 }
 private void shiftMosaic(int delta){
  if(!mosaicMode||delta==0)return;
  int next=Math.max(start,Math.min(start+((end-start)/6)*6,mosaicFirst+(delta>0?6:-6)));
  if(next==mosaicFirst)return;
  mosaicFirst=next;drawMosaic();
 }
 private void drawChunk(){
  if(mosaicMode){drawMosaic();return;}
  StringBuilder html=new StringBuilder(4000);
  html.append("<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1,maximum-scale=1'>")
   .append("<style>*{box-sizing:border-box}html,body{margin:0;width:100%;height:100%;overflow:hidden;background:#faf8f0;color:#121211;font:14px sans-serif}")
   .append("body{display:flex;flex-direction:column}.tile{flex:1;min-height:0;width:100%;display:flex;justify-content:center;padding:4px 8px;overflow:hidden}")
   .append(".paper{width:100%;height:100%;min-height:0;display:flex;align-items:center;justify-content:center;overflow:hidden;touch-action:none}")
   .append(".paper img{display:block;flex:none;object-fit:contain;width:1px;height:1px;max-width:100%;max-height:100%}")
   .append("footer{height:100px;flex:none;text-align:center;padding:2px 6px 8px}.range{height:24px;line-height:24px;font-size:12px}")
   .append("nav{height:64px;display:flex;gap:4px;align-items:center;justify-content:center;overflow:hidden}")
   .append("button{flex:none;width:48px;height:48px;min-width:48px;min-height:48px;border:1px solid #b7b6b0;border-radius:50%;background:transparent;color:#121211;font:14px sans-serif;padding:0}")
   .append("button.passage{border-width:2px;font-weight:bold}button.even{background:#121211;color:#faf8f0;border-color:#121211}button.odd{background:#faf8f0;color:#121211;border-color:#98978f}button[aria-current=page]{width:56px;height:56px;border:2px solid #121211;font-weight:bold;outline:2px solid #121211;outline-offset:2px}")
   .append("button[hidden]{display:none}</style><script>")
   .append("function fit(){var img=document.querySelector('.paper img'),box=document.querySelector('.paper');if(!img||!img.naturalWidth||!box)return;var ratio=img.naturalWidth/img.naturalHeight;var w=Math.max(1,Math.min(box.clientWidth-4,(box.clientHeight-4)*ratio));img.style.width=w+'px';img.style.height=(w/ratio)+'px';}")
   .append("function pills(){var nav=document.querySelector('nav');if(!nav)return;var count=Math.max(1,Math.min(7,Math.floor((nav.clientWidth-12)/54)));if(count>1&&count%2===0)count--;var first=Math.max(")
   .append(start).append(",Math.min(").append(end).append("-count+1,").append(selected).append("-Math.floor(count/2)));nav.querySelectorAll('button').forEach(b=>b.hidden=Number(b.textContent)<first||Number(b.textContent)>=first+count);}")
   .append("var touch=null,ignoreClick=0;document.addEventListener('touchstart',e=>{if(e.touches.length===1)touch={x:e.touches[0].clientX,y:e.touches[0].clientY};},{passive:true});")
   .append("document.addEventListener('touchend',e=>{if(!touch)return;var dx=e.changedTouches[0].clientX-touch.x,dy=e.changedTouches[0].clientY-touch.y;touch=null;if(Math.abs(dx)>40&&Math.abs(dx)>Math.abs(dy)*1.2){e.preventDefault();ignoreClick=Date.now()+400;MapPages.step(dx>0?1:-1);}});")
   .append("function readPage(p){if(Date.now()>ignoreClick)MapPages.open(p);}function choosePage(p){if(Date.now()>ignoreClick)MapPages.choose(p);}</script></head><body>");
  try{
   String svg=highlightExactVersePolygons(readSvg(getContext(),selected));
   String base64=android.util.Base64.encodeToString(svg.getBytes(StandardCharsets.UTF_8),android.util.Base64.NO_WRAP);
   html.append("<main class='tile selected'><div class='paper' role='button' tabindex='0' aria-label='Lecture page ").append(selected)
       .append("' onclick='readPage(").append(selected).append(")'><img alt='Page ").append(selected)
       .append(" entière' onload='fit()' src='data:image/svg+xml;base64,").append(base64).append("'></div></main>");
  }catch(Exception invalid){html.append("<main class='tile'>Page indisponible</main>");}
  html.append("<footer><div class='range'>").append(start==end?"Page ":"Pages ").append(start);
  if(end!=start)html.append(" à ").append(end);
  html.append("</div><nav aria-label='Pages de l’unité'>");
  for(int p=start;p<=end;p++){
   html.append("<button class='page ").append(p%2==0?"even":"odd").append(p>=passageFirstPage&&p<=passageLastPage?" passage":"").append("' aria-label='Page ").append(p).append(p%2==0?" paire":" impaire").append("'");
   if(p==selected)html.append(" aria-current='page'");
   html.append(" onclick='choosePage(").append(p).append(")'>").append(p).append("</button>");
  }
  html.append("</nav></footer><script>new ResizeObserver(()=>{fit();pills();}).observe(document.body);fit();pills();</script></body></html>");
  loadDataWithBaseURL(null,html.toString(),"text/html","UTF-8",null);
 }
 private final class Bridge {
  /** Sequential on-device rasterization of authentic local QCF SVG pages.
   * Never loads multiple full pages in one HTML document.
   */
  @JavascriptInterface public void thumb(int page,int generation){
   if(page<1||page>604)return;
   post(()->{
    if(!mosaicMode||generation!=mosaicGeneration||page<mosaicFirst||page>Math.min(end,mosaicFirst+5))return;
    try{
     String original=cachedSvg(page);
     String encoded=android.util.Base64.encodeToString(
         original.getBytes(StandardCharsets.UTF_8),android.util.Base64.NO_WRAP);
     evaluateJavascript("if(window.receiveThumb)receiveThumb("+page+",'"+encoded+"',"+generation+");",null);
    }catch(Exception unavailable){
     evaluateJavascript("if(window.receiveThumb)receiveThumb("+page+",'',"+generation+");",null);
    }
   });
  }
  @JavascriptInterface public void shift(int delta){post(()->MapQcfThumbnails.this.shiftMosaic(delta));}
  @JavascriptInterface public void step(int delta){post(()->MapQcfThumbnails.this.step(delta));}
  @JavascriptInterface public void choose(int page){if(page>=start&&page<=end)post(()->{if(listener!=null)listener.onPageChosen(page);});}
  @JavascriptInterface public void open(int page){if(page>=start&&page<=end)post(()->{if(listener!=null)listener.onOpenReading(page);});}
 }
}
