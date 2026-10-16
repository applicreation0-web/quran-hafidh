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
 * boxes or screenshots. A bounded two-page window avoids loading forty
 * complete SVGs into an E-Ink WebView at once. All pages remain reachable.
 */
final class MapQcfThumbnails extends WebView {
 interface Listener { void onPageChosen(int page); }
 private static final int CHUNK=2;
 private final Listener listener;
 private int start,end,selected,chunkStart;
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
  if(first<1||last>604||last<first)throw new IllegalArgumentException("QCF thumbnail range");
  int previousStart=start,previousEnd=end;
  start=first;end=last;selected=Math.max(first,Math.min(last,selectedPage));
  if(previousStart!=first||previousEnd!=last)
   chunkStart=first+((selected-first)/CHUNK)*CHUNK;
  else if(selected<chunkStart||selected>=chunkStart+CHUNK)
   chunkStart=first+((selected-first)/CHUNK)*CHUNK;
  drawChunk();
 }
 private void step(int delta){
  int next=Math.max(start,Math.min(end,chunkStart+delta*CHUNK));
  if(next==chunkStart)return;
  chunkStart=next;drawChunk();
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
   return svg;
  }
 }
 private void drawChunk(){
  StringBuilder html=new StringBuilder(4000);
  html.append("<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1,maximum-scale=1'>")
      .append("<style>*{box-sizing:border-box}html,body{margin:0;width:100%;height:100%;overflow:hidden;background:#faf8f2;color:#121211;font:14px sans-serif}")
      .append("header{display:flex;justify-content:space-between;align-items:center;height:48px;padding:0 8px}")
      .append("button{border:1px solid #bbb;background:transparent;color:#111;font:16px sans-serif;min-height:44px;padding:5px 11px}")
      .append("main{height:calc(100% - 48px);display:flex;justify-content:center;align-items:stretch;gap:8px;padding:4px 8px;overflow:hidden}")
      .append(".tile{display:flex;flex-direction:column;width:calc(50% - 4px);height:100%;min-width:0;min-height:0;text-align:center;background:transparent;border:0;padding:0}")
      .append(".tile.selected{border-bottom:3px solid #111}.tile img{display:block;width:100%;height:calc(100% - 24px);min-height:0;object-fit:contain}")
      .append(".num{font-size:13px;font-weight:bold;line-height:23px;flex:none}</style></head><body><header><button onclick='MapPages.step(-1)' aria-label='Pages précédentes'>‹</button>")
      .append("<span>Pages QCF ").append(chunkStart).append("–").append(Math.min(end,chunkStart+CHUNK-1))
      .append(" / ").append(start).append("–").append(end).append("</span>")
      .append("<button onclick='MapPages.step(1)' aria-label='Pages suivantes'>›</button></header><main>");
  for(int p=chunkStart;p<=Math.min(end,chunkStart+CHUNK-1);p++){
   try{
    String svg=readSvg(getContext(),p);
    String base64=android.util.Base64.encodeToString(svg.getBytes(StandardCharsets.UTF_8),android.util.Base64.NO_WRAP);
    html.append("<button class='tile").append(p==selected?" selected":"").append("' aria-label='Voir page ")
        .append(p).append("' onclick='MapPages.open(").append(p).append(")'><img alt='' src='data:image/svg+xml;base64,")
        .append(base64).append("'><div class='num'>").append(p).append("</div></button>");
   }catch(Exception invalid){
    html.append("<span>Page ").append(p).append(" indisponible</span>");
   }
  }
  html.append("</main></body></html>");
  loadDataWithBaseURL(null,html.toString(),"text/html","UTF-8",null);
 }
 private final class Bridge {
  @JavascriptInterface public void step(int delta){post(()->MapQcfThumbnails.this.step(delta<0?-1:1));}
  @JavascriptInterface public void open(int page){
   if(page<start||page>end)return;
   post(()->{selected=page;if(listener!=null)listener.onPageChosen(page);});
  }
 }
}
