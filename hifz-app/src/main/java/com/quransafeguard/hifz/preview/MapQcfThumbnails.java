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
 interface Listener {
  void onPageChosen(int page);
  default void onOpenReading(int page){onPageChosen(page);}
  default void onRendered(int page, boolean success){}
 }
 private String renderKey="";
 private volatile boolean disposed;
 private int passageFirstPage,passageLastPage;
 private final Listener listener;
 private int start,end,selected;
 private int markSurah,markFirst,markLast;
 private boolean markOnlyBoundaries;
 private boolean mosaicMode;
 private boolean atlasSinglePageCompact;
 private int mosaicFirst=-1,mosaicGeneration,fullGeneration;
 /** Never send a 600 KB+ SVG as one oversized UTF-16 evaluateJavascript statement. */
 private static final int COMPACT_CHUNK_CHARS=24_000;
 private String pendingCompactSvg;
 private int pendingCompactPage,pendingCompactGeneration,pendingCompactNext;
 private final java.util.LinkedHashMap<Integer,String> vectorCache=
     new java.util.LinkedHashMap<Integer,String>(8,0.75f,true){
      @Override protected boolean removeEldestEntry(java.util.Map.Entry<Integer,String> entry){return size()>6;}
     };
 private synchronized String cachedSvg(int page)throws Exception{
  String svg=vectorCache.get(page);
  if(svg==null){svg=readSvg(getContext(),page);vectorCache.put(page,svg);}
  return svg;
 }
 /** The two QCF modes have separate render cache identities. */
 void setMosaic(boolean value) {
  if(disposed||mosaicMode==value)return;
  mosaicMode=value;mosaicFirst=-1;renderKey="";
 }
 /** Exact single QCF page, no Atlas unit-page pills: used in the two-way comparison. */
 void setAtlasSinglePageCompact(boolean enabled) {
  if(atlasSinglePageCompact==enabled)return;
  atlasSinglePageCompact=enabled;renderKey="";
 }
 MapQcfThumbnails(Context context,Listener listener){
  super(context);this.listener=listener;
  setBackgroundColor(Color.rgb(250,248,242));
  // Original large KFQC SVGs can overflow Chromium GPU tile memory when
  // a BOOX or emulator holds two pages and two close-ups simultaneously.
  // Software layers favour predictable monochrome rendering over GPU tiling.
  setLayerType(android.view.View.LAYER_TYPE_SOFTWARE,null);
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
  if(disposed)return;
  if(s<0||s>114||firstAyah<0||lastAyah<0||firstAyah>lastAyah)throw new IllegalArgumentException("QCF verse highlight range");
  if(first<1||last>604||last<first)throw new IllegalArgumentException("QCF thumbnail range");
  int chosen=Math.max(first,Math.min(last,selectedPage));
  if(atlasSinglePageCompact && first!=last)
   throw new IllegalArgumentException("Compact Atlas preview must show one exact QCF page");
  String key=first+":"+last+":"+chosen+":"+s+":"+firstAyah+":"+lastAyah+":"+boundariesOnly+":"+focusFirstPage+":"+focusLastPage+":"+mosaicMode+":"+atlasSinglePageCompact;
  if(key.equals(renderKey))return;
  renderKey=key;start=first;end=last;selected=chosen;
  if(mosaicFirst<first||mosaicFirst>last||chosen<mosaicFirst||chosen>=mosaicFirst+6)
   mosaicFirst=first+((chosen-first)/6)*6;
  markSurah=s;markFirst=firstAyah;markLast=lastAyah;markOnlyBoundaries=boundariesOnly;
  passageFirstPage=focusFirstPage;passageLastPage=focusLastPage;
  drawChunk();
 }
 private void step(int delta){
  if(disposed)return;
  int next=Math.max(start,Math.min(end,selected+(delta<0?-1:1)));
  if(next!=selected&&listener!=null)listener.onPageChosen(next);
 }
 static String readSvg(Context context,int page)throws Exception{
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
 private String highlightExactVersePolygons(String svg,int surah,int firstAyah,int lastAyah,boolean boundariesOnly){
  if(surah==0||firstAyah==0)return svg;
  StringBuilder css=new StringBuilder("<style>");
  if(boundariesOnly){
   appendSelector(css,surah,firstAyah);
   if(lastAyah!=firstAyah){css.append(',');appendSelector(css,surah,lastAyah);}
  }else{
   for(int a=firstAyah;a<=lastAyah;a++){
    if(a>firstAyah)css.append(',');
    appendSelector(css,surah,a);
   }
  }
  css.append("{fill:#999!important;fill-opacity:0.23!important;stroke:#333!important;stroke-width:0.25!important;}</style>");
  return svg.replace("</svg>",css.toString()+"</svg>");
 }
 private void appendSelector(StringBuilder css,int surah,int ayah){
  css.append("path.ayahPolygon[surah='").append(surah)
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
      .append("<style>*{box-sizing:border-box}html,body{margin:0;width:100%;height:100%;overflow:hidden;background:#faf8f0;color:#121211;font:12px sans-serif}body{display:flex;flex-direction:column}")
      .append(".grid{display:grid;flex:1;width:100%;min-height:0;padding:5px;gap:5px;align-content:center;grid-template-columns:repeat(")
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
      .append("' loading='eager' decoding='sync'><b>").append(p).append("</b></button>");
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
      .append("var mosaicPaintReady=false;function requestNext(){if(nextPage<=lastPage)MapPages.thumb(nextPage++,generation);else{mosaicPaintReady=true;MapPages.ready(generation);}}")
      .append("function receiveThumb(p,src,gen){if(gen!==generation)return;var el=document.getElementById('qcf'+p);")
      .append("if(!el){requestNext();return;}")
      .append("el.onload=function(){el.onload=null;el.onerror=null;requestAnimationFrame(function(){requestAnimationFrame(requestNext);});};")
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
  // Decompression and SVG highlighting run on the JavaScript bridge worker.
  // Keep every page swipe off the BOOX/phone Android main thread.
  final int generation=++fullGeneration;
  pendingCompactSvg=null;
  StringBuilder html=new StringBuilder(4000);
  html.append("<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1,maximum-scale=1'>")
   .append("<style>*{box-sizing:border-box}html,body{margin:0;width:100%;height:100%;overflow:hidden;background:#faf8f0;color:#121211;font:14px sans-serif}")
   .append("body{display:flex;flex-direction:column}.tile{flex:1;min-height:0;width:100%;display:flex;justify-content:center;padding:4px 8px;overflow:hidden}")
   .append(".paper{width:100%;height:100%;min-height:0;display:flex;align-items:center;justify-content:center;overflow:hidden;touch-action:none}")
   .append(".paper img{display:block;flex:none;object-fit:contain;width:auto;height:auto;max-width:100%;max-height:100%}")
   .append(".paper svg{display:block;flex:none;max-width:100%;max-height:100%;width:100%;height:100%;overflow:hidden}")
   .append("footer{height:60px;flex:none;text-align:center;padding:2px 6px 3px}")
   .append("nav{height:54px;display:flex;gap:4px;align-items:center;justify-content:center;overflow:hidden}")
   .append("button{flex:none;width:44px;height:44px;min-width:44px;min-height:44px;border:1px solid #b7b6b0;border-radius:50%;background:transparent;color:#121211;font:14px sans-serif;padding:0}")
   .append("button.passage{border-width:2px;font-weight:bold}button.even{background:#121211;color:#faf8f0;border-color:#121211}button.odd{background:#faf8f0;color:#121211;border-color:#98978f}button[aria-current=page]{width:50px;height:50px;border:2px solid #121211;font-weight:bold;outline:2px solid #121211;outline-offset:2px}")
   .append("button[hidden]{display:none}</style><script>")
   .append("function fit(){var img=document.querySelector('.paper img'),box=document.querySelector('.paper');if(!img||!img.naturalWidth||!box||box.clientWidth<48||box.clientHeight<72)return;var ratio=img.naturalWidth/img.naturalHeight;var w=Math.max(1,Math.min(box.clientWidth-4,(box.clientHeight-4)*ratio));img.style.width=w+'px';img.style.height=(w/ratio)+'px';}")
   .append("function pills(){var nav=document.querySelector('nav');if(!nav)return;var count=Math.max(1,Math.min(7,Math.floor((nav.clientWidth-12)/50)));if(count>1&&count%2===0)count--;var first=Math.max(")
   .append(start).append(",Math.min(").append(end).append("-count+1,").append(selected).append("-Math.floor(count/2)));nav.querySelectorAll('button').forEach(b=>b.hidden=Number(b.textContent)<first||Number(b.textContent)>=first+count);}")
   .append("var touch=null,ignoreClick=0;document.addEventListener('touchstart',e=>{if(e.touches.length===1)touch={x:e.touches[0].clientX,y:e.touches[0].clientY};},{passive:true});")
   .append("document.addEventListener('touchend',e=>{if(!touch)return;var dx=e.changedTouches[0].clientX-touch.x,dy=e.changedTouches[0].clientY-touch.y;touch=null;if(Math.abs(dx)>40&&Math.abs(dx)>Math.abs(dy)*1.2){e.preventDefault();ignoreClick=Date.now()+400;MapPages.step(dx>0?1:-1);}});")
   .append("function readPage(p){if(Date.now()>ignoreClick)MapPages.open(p);}function choosePage(p){if(Date.now()>ignoreClick)MapPages.choose(p);}</script></head><body>");
  html.append("<main class='tile selected'><div class='paper' role='button' tabindex='0' aria-label='Lecture page ")
      .append(selected).append("' onclick='readPage(").append(selected)
      .append(")'>");
  if (!atlasSinglePageCompact)
   html.append("<img alt='Page ").append(selected).append(" entière' onload='fit()'>");
  else
   html.append("<span id='qcf-loading' style='font-size:12px'>Chargement du Mushaf QCF…</span>");
  html.append("</div></main>");
  if (!atlasSinglePageCompact) {
  html.append("<footer><nav aria-label='Pages de l’unité'>");
  for(int p=start;p<=end;p++){
   html.append("<button class='page ").append(p%2==0?"even":"odd").append(p>=passageFirstPage&&p<=passageLastPage?" passage":"").append("' aria-label='Page ").append(p).append(p%2==0?" paire":" impaire").append("'");
   if(p==selected)html.append(" aria-current='page'");
   html.append(" onclick='choosePage(").append(p).append(")'>").append(p).append("</button>");
  }
  html.append("</nav></footer>");
  }
  html.append("<script>var fullGeneration=").append(generation).append(";var currentSvgPage=").append(selected)
      .append(";var inlineQcf=").append(atlasSinglePageCompact?"true":"false").append(";")
      .append("var fullParts=[],fullExpected=0;")
      .append("function receiveFullChunk(i,total,part,gen){if(gen!==fullGeneration)return;")
      .append("if(i!==fullExpected||total<1||total>100){MapPages.fullStatus(currentSvgPage,gen,false);return;}")
      .append("fullParts.push(part);fullExpected++;")
      .append("if(fullExpected===total){var joined=fullParts.join('');fullParts=[];receiveFull(joined,gen);}")
      .append("else MapPages.fullChunkAck(i,gen);}")
      .append("function receiveFull(src,gen){if(gen!==fullGeneration)return;var box=document.querySelector('.paper');if(!box)return;")
      .append("if(!src){MapPages.fullStatus(currentSvgPage,gen,false);return;}")
      .append("if(inlineQcf){try{")
      .append("var raw=atob(src),bytes=new Uint8Array(raw.length);for(var i=0;i<raw.length;i++)bytes[i]=raw.charCodeAt(i);")
      .append("var parsed=new DOMParser().parseFromString(new TextDecoder('utf-8').decode(bytes),'image/svg+xml');")
      .append("var svg=parsed.documentElement;if(!svg||svg.localName!=='svg'||parsed.getElementsByTagName('parsererror').length)throw Error('SVG XML invalid');")
      .append("var paths=svg.getElementsByTagName('path').length;if(paths<20)throw Error('Missing original QCF paths');")
      .append("var imported=document.importNode(svg,true);box.textContent='';box.appendChild(imported);")
      .append("requestAnimationFrame(function(){requestAnimationFrame(function(){var rect=imported.getBoundingClientRect();MapPages.fullStatus(currentSvgPage,gen,rect.width>40&&rect.height>60);});});")
      .append("}catch(err){box.textContent='Page QCF indisponible';MapPages.fullStatus(currentSvgPage,gen,false);}return;}")
      .append("var img=document.querySelector('.paper img');if(!img){MapPages.fullStatus(currentSvgPage,gen,false);return;}")
      .append("img.onload=function(){fit();requestAnimationFrame(function(){fit();requestAnimationFrame(function(){MapPages.fullStatus(currentSvgPage,gen,true);});});};")
      .append("img.onerror=function(){img.alt='Page QCF indisponible';MapPages.fullStatus(currentSvgPage,gen,false);};")
      .append("img.src='data:image/svg+xml;base64,'+src;}")
      .append("new ResizeObserver(()=>{fit();pills();}).observe(document.body);pills();")
      .append("window.addEventListener('load',()=>MapPages.full(").append(selected).append(",").append(generation).append("));")
      .append("</script></body></html>");
  loadDataWithBaseURL(null,html.toString(),"text/html","UTF-8",null);
 }
 /** Acknowledged 24 KB chunks keep WebView IPC well under Android Binder limits. */
 private void sendCompactChunk(){
  if(disposed||pendingCompactSvg==null||mosaicMode
      ||pendingCompactGeneration!=fullGeneration||pendingCompactPage!=selected)return;
  int index=pendingCompactNext;
  int total=(pendingCompactSvg.length()+COMPACT_CHUNK_CHARS-1)/COMPACT_CHUNK_CHARS;
  if(index>=total||total>100)return;
  int start=index*COMPACT_CHUNK_CHARS;
  String chunk=pendingCompactSvg.substring(start,Math.min(pendingCompactSvg.length(),start+COMPACT_CHUNK_CHARS));
  pendingCompactNext++;
  evaluateJavascript("if(window.receiveFullChunk)receiveFullChunk("+index+","+total+",'"+chunk+"',"+pendingCompactGeneration+");",null);
 }

 /** Drop the previous full SVG document before a different Atlas pair is decoded.
  * A new 600 KB vector on top of two former page DOMs can exhaust Chromium tiles. */
 void releaseAtlasComparison(Runnable onCleared){
  if(disposed||!atlasSinglePageCompact){if(onCleared!=null)onCleared.run();return;}
  final int token=++fullGeneration;
  pendingCompactSvg=null;renderKey="";
  synchronized(this){vectorCache.clear();}
  final boolean[] delivered={false};
  Runnable finish=()->{
   if(delivered[0])return;
   delivered[0]=true;
   if(disposed||token!=fullGeneration)return;
   setWebViewClient(new android.webkit.WebViewClient());
   if(onCleared!=null)onCleared.run();
  };
  setWebViewClient(new android.webkit.WebViewClient(){
   @Override public void onPageFinished(WebView view,String url){
    if("about:blank".equals(url))finish.run();
   }
  });
  loadUrl("about:blank");
  // Never strand the reader if an older BOOX WebView omits the blank-page callback.
  postDelayed(finish,700L);
 }
 /** Invalidate queued bridge work before releasing the native WebView. */
 @Override public void destroy(){
  if(disposed)return;
  disposed=true;++fullGeneration;++mosaicGeneration;
  pendingCompactSvg=null;
  removeJavascriptInterface("MapPages");stopLoading();super.destroy();
 }
 private final class Bridge {
  @JavascriptInterface public void full(int page,int generation){
   if(disposed||page<1||page>604)return;
   final int s,a,b;
   final boolean only;
   synchronized(MapQcfThumbnails.this){
    if(disposed||mosaicMode||page!=selected||generation!=fullGeneration)return;
    s=markSurah;a=markFirst;b=markLast;only=markOnlyBoundaries;
   }
   String encoded;
   try{
    String highlighted=highlightExactVersePolygons(cachedSvg(page),s,a,b,only);
    encoded=android.util.Base64.encodeToString(
        highlighted.getBytes(StandardCharsets.UTF_8),android.util.Base64.NO_WRAP);
   }catch(Exception unavailable){
    android.util.Log.e("HaafidhAtlasQCF","Failed to load authentic SVG page "+page,unavailable);
    encoded="";
   }
   final String prepared=encoded;
   post(()->{
    if(disposed||mosaicMode||generation!=fullGeneration||page!=selected)return;
    if(atlasSinglePageCompact&&!prepared.isEmpty()){
     pendingCompactSvg=prepared;
     pendingCompactPage=page;pendingCompactGeneration=generation;pendingCompactNext=0;
     sendCompactChunk();
    }else{
     evaluateJavascript("if(window.receiveFull)receiveFull('"+prepared+"',"+generation+");",null);
    }
   });
  }
  /** Decode on the WebView JavaScript bridge worker, never on Android's UI thread.
   * Only inject the prepared authentic SVG after validating its generation.
   */
  @JavascriptInterface public void thumb(int page,int generation){
   if(disposed||page<1||page>604)return;
   String encoded;
   try{
    String original=cachedSvg(page);
    encoded=android.util.Base64.encodeToString(
        original.getBytes(StandardCharsets.UTF_8),android.util.Base64.NO_WRAP);
   }catch(Exception unavailable){encoded="";}
   final String prepared=encoded;
   post(()->{
    if(disposed||!mosaicMode||generation!=mosaicGeneration||page<mosaicFirst||page>Math.min(end,mosaicFirst+5))return;
    evaluateJavascript("if(window.receiveThumb)receiveThumb("+page+",'"+prepared+"',"+generation+");",null);
   });
  }
  @JavascriptInterface public void ready(int generation){post(()->{if(!disposed&&mosaicMode&&generation==mosaicGeneration)invalidate();});}
  @JavascriptInterface public void fullChunkAck(int index,int generation){
   post(()->{
    if(!disposed&&pendingCompactSvg!=null&&generation==pendingCompactGeneration
        &&pendingCompactNext==index+1&&generation==fullGeneration)sendCompactChunk();
   });
  }
  @JavascriptInterface public void fullStatus(int page,int generation,boolean success){
   post(()->{
    if(!disposed&&!mosaicMode&&generation==fullGeneration&&page==selected){
     pendingCompactSvg=null;
     if(listener!=null)listener.onRendered(page,success);
    }
   });
  }
  @JavascriptInterface public void shift(int delta){post(()->{if(!disposed)MapQcfThumbnails.this.shiftMosaic(delta);});}
  @JavascriptInterface public void step(int delta){post(()->MapQcfThumbnails.this.step(delta));}
  @JavascriptInterface public void choose(int page){if(page>=start&&page<=end)post(()->{if(!disposed&&page>=start&&page<=end&&listener!=null)listener.onPageChosen(page);});}
  @JavascriptInterface public void open(int page){if(page>=start&&page<=end)post(()->{if(!disposed&&page>=start&&page<=end&&listener!=null)listener.onOpenReading(page);});}
 }
}
