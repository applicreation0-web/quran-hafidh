package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.graphics.Color;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import com.quransafeguard.hifz.core.VerseRef;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Two REAL KFQC SVG verse-context crops; no Arabic font, text reconstruction or Web download.
 * Identical 345x550 physical page coordinates are cropped without changing SVG glyph paths.
 * The exact sidecar provides the verse boundaries and the coordinates of reviewed word indices.
 * Fails closed (blank with a "see Mushaf" note) if a verse straddles pages or geometry is missing.
 */
final class QcfAtlasVerseStrips extends WebView {
    private static final Pattern VIEWBOX=Pattern.compile("viewBox\\s*=\\s*(['\"])([^'\"]+)\\1");
    private int generation;
    private static final int CHUNK_CHARS=24_000;
    private String pendingEncoded;
    private int pendingGen,pendingSide,pendingNext;
    private boolean disposed;
    private VerseRef left, right;
    private int pageLeft,pageRight;
    private int[] wordsLeft,wordsRight;
    private final int[] renderStatus=new int[2];

    QcfAtlasVerseStrips(Context context) {
        super(context);
        setBackgroundColor(Color.rgb(250,248,240));
        // Avoid renderer tile-memory exhaustion with two genuine vector crops.
        setLayerType(android.view.View.LAYER_TYPE_SOFTWARE,null);
        WebSettings s=getSettings();
        s.setJavaScriptEnabled(true);
        s.setBlockNetworkLoads(true);
        s.setAllowContentAccess(false);
        s.setAllowFileAccess(false);
        s.setDomStorageEnabled(false);
        s.setBuiltInZoomControls(false);
        addJavascriptInterface(new Bridge(),"QcfStrip");
    }

    void show(VerseRef a,int aPage,int[] aWords,VerseRef b,int bPage,int[] bWords) {
        if(disposed || a==null || b==null || aPage<1 || aPage>604 || bPage<1 || bPage>604)
            throw new IllegalArgumentException("Invalid paired Quran excerpt");
        left=a;right=b;pageLeft=aPage;pageRight=bPage;
        wordsLeft=aWords.clone();wordsRight=bWords.clone();
        int gen=++generation;
        pendingEncoded=null;
        renderStatus[0]=0;renderStatus[1]=0;
        String html="<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1'>"
            +"<style>*{box-sizing:border-box}html,body{margin:0;width:100%;height:100%;overflow:hidden;background:#faf8f0}"
            +"main{display:grid;grid-template-columns:1fr 1fr;height:100%;gap:6px;padding:3px}"
            +"section{border-top:1px solid #88847d;min-width:0;min-height:0;display:flex;align-items:center;justify-content:center}"
            +"section .crop{width:100%;height:100%;min-height:0;display:flex;align-items:center;justify-content:center;overflow:hidden}"
            +"section svg{display:block;width:100%;height:100%;max-width:100%;max-height:100%}"
            +"span{font:12px sans-serif;color:#524f49;text-align:center}</style></head>"
            +"<body><main><section><div class='crop' id='crop0'><span>Chargement de l'extrait QCF…</span></div></section>"
            +"<section><div class='crop' id='crop1'><span>Chargement de l'extrait QCF…</span></div></section></main>"
            +"<script>var gen="+gen+";var parts=[],expect=0,activeSide=-1;"
            +"function receiveStripChunk(side,i,total,part,g){if(g!==gen)return;"
            +"if(total<1||total>100||i!==expect||(activeSide!==-1&&activeSide!==side)){QcfStrip.stripStatus(side,g,false);return;}"
            +"activeSide=side;parts.push(part);expect++;"
            +"if(expect===total){var complete=parts.join('');parts=[];expect=0;activeSide=-1;receiveStrip(side,complete,g);}"
            +"else QcfStrip.chunkAck(side,i,g);}"
            +"function receiveStrip(i,src,g){if(g!==gen)return;var target=document.getElementById('crop'+i);if(!target)return;"
            +"if(!src){target.textContent='Extrait QCF indisponible — ouvrir le Muṣḥaf';QcfStrip.stripStatus(i,g,false);return;}"
            +"try{var raw=atob(src),bytes=new Uint8Array(raw.length);for(var j=0;j<raw.length;j++)bytes[j]=raw.charCodeAt(j);"
            +"var parsed=new DOMParser().parseFromString(new TextDecoder('utf-8').decode(bytes),'image/svg+xml');"
            +"var svg=parsed.documentElement;if(!svg||svg.localName!=='svg'||parsed.getElementsByTagName('parsererror').length||svg.getElementsByTagName('path').length<20)throw Error('Bad QCF excerpt');"
            +"var original=document.importNode(svg,true);target.textContent='';target.appendChild(original);"
            +"requestAnimationFrame(function(){requestAnimationFrame(function(){var box=original.getBoundingClientRect();QcfStrip.stripStatus(i,g,box.width>20&&box.height>10);});});"
            +"}catch(e){target.textContent='Extrait QCF indisponible — ouvrir le Muṣḥaf';QcfStrip.stripStatus(i,g,false);}}"
            +"addEventListener('load',()=>QcfStrip.render(0,gen));"
            +"</script></body></html>";
        loadDataWithBaseURL(null,html,"text/html","UTF-8",null);
    }

    /** Immutable, 100%-QCF source. Returns empty when the geometry does not validate. */
    static String cropSvg(String source,List<WordGeometryRepository.WordBox> pageWords,
                          VerseRef target,int[] underlineWords) {
        if(source==null||pageWords==null||target==null||underlineWords==null)return "";
        List<WordGeometryRepository.WordBox> found=new ArrayList<>();
        for(WordGeometryRepository.WordBox word:pageWords)
            if(word.surah==target.getSurah()&&word.ayah==target.getAyah())found.add(word);
        if(found.isEmpty())return "";
        double x0=345,y0=550,x1=0,y1=0;
        for(WordGeometryRepository.WordBox word:found) {
            x0=Math.min(x0,word.x0);y0=Math.min(y0,word.y0);
            x1=Math.max(x1,word.x1);y1=Math.max(y1,word.y1);
        }
        x0=Math.max(0,x0-8);y0=Math.max(0,y0-8);
        x1=Math.min(345,x1+8);y1=Math.min(550,y1+8);
        if(x1-x0<15||y1-y0<10)return "";
        Matcher m=VIEWBOX.matcher(source);
        if(!m.find())return "";
        String[] view=m.group(2).trim().replace(',',' ').split("\\s+");
        if(view.length!=4)return "";
        double originX,originY;
        try {
            originX=Double.parseDouble(view[0]);
            originY=Double.parseDouble(view[1]);
            if(!Double.isFinite(originX)||!Double.isFinite(originY)
                ||Double.parseDouble(view[2])!=345||Double.parseDouble(view[3])!=550)return "";
        }catch(NumberFormatException invalid){return "";}
        // The first actual KFQC page has a shifted SVG viewBox. The audited word
        // boxes are page-relative 0..345/550; translate them to the original SVG
        // coordinate origin before cropping or adding a difference underline.
        String vb=String.format(Locale.ROOT,"viewBox='%.2f %.2f %.2f %.2f'",
            originX+x0,originY+y0,x1-x0,y1-y0);
        String cropped=source.substring(0,m.start())+vb+source.substring(m.end());
        // Change the intrinsic SVG aspect ratio as well; no stretched / reflowed glyphs.
        cropped=cropped.replaceFirst("width='345' height='550'",
            String.format(Locale.ROOT,"width='%.2f' height='%.2f'",x1-x0,y1-y0));
        StringBuilder strokes=new StringBuilder("<g id='exact-word-differences' fill='none' stroke='#121211' stroke-width='0.85'>");
        for(int index:underlineWords){
            if(index<1)return ""; // no guess for invalid source annotations
            WordGeometryRepository.WordBox matching=null;
            for(WordGeometryRepository.WordBox w:found)
                if(w.word==index){matching=w;break;}
            if(matching==null)return ""; // source word count must match audited QCF sidecar
            double y=Math.min(549,matching.y1+1.5);
            strokes.append(String.format(Locale.ROOT,
                "<line x1='%.2f' y1='%.2f' x2='%.2f' y2='%.2f'/>",
                originX+matching.x0,originY+y,originX+matching.x1,originY+y));
        }
        strokes.append("</g>");
        if(!cropped.contains("</svg>"))return "";
        return cropped.replace("</svg>",strokes+"</svg>");
    }

    /** Flush two previous excerpt SVGs before allocating any new QCF pages. */
    void releaseAtlasComparison(Runnable onCleared){
        if(disposed){if(onCleared!=null)onCleared.run();return;}
        final int token=++generation;
        pendingEncoded=null;
        renderStatus[0]=0;renderStatus[1]=0;
        left=null;right=null;wordsLeft=null;wordsRight=null;
        final boolean[] delivered={false};
        Runnable finish=()->{
            if(delivered[0])return;
            delivered[0]=true;
            if(disposed||token!=generation)return;
            setWebViewClient(new android.webkit.WebViewClient());
            if(onCleared!=null)onCleared.run();
        };
        setWebViewClient(new android.webkit.WebViewClient(){
            @Override public void onPageFinished(WebView view,String url){
                if("about:blank".equals(url))finish.run();
            }
        });
        loadUrl("about:blank");
        postDelayed(finish,700L);
    }

    boolean bothRenderedForTest(){return renderStatus[0]==1&&renderStatus[1]==1;}
    int failureCountForTest(){return (renderStatus[0]<0?1:0)+(renderStatus[1]<0?1:0);}

    @Override public void destroy() {
        if(disposed)return;
        disposed=true;++generation;pendingEncoded=null;
        removeJavascriptInterface("QcfStrip");stopLoading();
        super.destroy();
    }

    private void sendChunk(){
        if(disposed||pendingEncoded==null||pendingGen!=generation)return;
        int index=pendingNext;
        int total=(pendingEncoded.length()+CHUNK_CHARS-1)/CHUNK_CHARS;
        if(index>=total||total>100)return;
        int start=index*CHUNK_CHARS;
        String part=pendingEncoded.substring(start,Math.min(start+CHUNK_CHARS,pendingEncoded.length()));
        pendingNext++;
        evaluateJavascript("if(window.receiveStripChunk)receiveStripChunk("+pendingSide+","+index+","+total+",'"+part+"',"+pendingGen+");",null);
    }

    private final class Bridge {
        @JavascriptInterface public void chunkAck(int side,int index,int gen){
            post(()->{
                if(!disposed&&pendingEncoded!=null&&gen==generation&&gen==pendingGen
                    &&side==pendingSide&&pendingNext==index+1)sendChunk();
            });
        }
        @JavascriptInterface public void stripStatus(int side,int gen,boolean success){
            post(()->{
                if(disposed||gen!=generation||side<0||side>1)return;
                renderStatus[side]=success?1:-1;
                if(side==pendingSide&&gen==pendingGen)pendingEncoded=null;
                if(side==0 && renderStatus[1]==0)
                    evaluateJavascript("if(window.QcfStrip)QcfStrip.render(1,"+gen+");",null);
            });
        }
        @JavascriptInterface public void render(int side,int gen){
            if(disposed || gen!=generation || (side!=0&&side!=1))return;
            VerseRef verse=side==0?left:right;
            int page=side==0?pageLeft:pageRight;
            int[] difference=side==0?wordsLeft:wordsRight;
            String encoded="";
            try{
                GeometryRepository geometry=GeometryRepository.get(getContext());
                // A two-page ayah is NOT arbitrarily truncated into a claimed full excerpt.
                if(geometry.pageForVerse(verse)==page
                    && MawdudiMapModel.lastPageForVerse(verse,geometry)==page){
                    String full=MapQcfThumbnails.readSvg(getContext(),page);
                    String excerpt=cropSvg(full,
                        WordGeometryRepository.shared(getContext()).wordsForPage(page),
                        verse,difference);
                    if(!excerpt.isEmpty())encoded=android.util.Base64.encodeToString(
                        excerpt.getBytes(StandardCharsets.UTF_8),android.util.Base64.NO_WRAP);
                }
            }catch(Exception missingAsset){android.util.Log.w("QcfAtlasStrips","Exact excerpt unavailable",missingAsset);}
            final String safe=encoded;
            post(()->{
                if(disposed||gen!=generation)return;
                if(safe.isEmpty()){
                    evaluateJavascript("if(window.receiveStrip)receiveStrip("+side+",'',"+gen+");",null);
                }else{
                    pendingEncoded=safe;pendingGen=gen;pendingSide=side;pendingNext=0;
                    sendChunk();
                }
            });
        }
    }
}
