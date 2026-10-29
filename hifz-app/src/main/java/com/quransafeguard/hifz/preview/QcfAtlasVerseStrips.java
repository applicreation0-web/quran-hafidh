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
    private boolean disposed;
    private VerseRef left, right;
    private int pageLeft,pageRight;
    private int[] wordsLeft,wordsRight;

    QcfAtlasVerseStrips(Context context) {
        super(context);
        setBackgroundColor(Color.rgb(250,248,240));
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
        String html="<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1'>"
            +"<style>*{box-sizing:border-box}html,body{margin:0;width:100%;height:100%;overflow:hidden;background:#faf8f0}"
            +"main{display:grid;grid-template-columns:1fr 1fr;height:100%;gap:6px;padding:3px}"
            +"section{border-top:1px solid #88847d;min-width:0;min-height:0;display:flex;align-items:center;justify-content:center}"
            +"img{width:100%;height:100%;object-fit:contain;display:block}"
            +"span{font:12px sans-serif;color:#524f49;text-align:center}</style></head>"
            +"<body><main><section><span id='hint0'>Extrait QCF indisponible — ouvrir le Muṣḥaf</span><img id='v0' hidden></section>"
            +"<section><span id='hint1'>Extrait QCF indisponible — ouvrir le Muṣḥaf</span><img id='v1' hidden></section></main>"
            +"<script>var gen="+gen+";"
            +"function receiveStrip(i,src,g){if(g!==gen)return;var im=document.getElementById('v'+i);"
            +"if(!src||!im)return;im.onload=function(){this.hidden=false;document.getElementById('hint'+i).hidden=true};"
            +"im.onerror=function(){im.hidden=true;document.getElementById('hint'+i).hidden=false};"
            +"im.src='data:image/svg+xml;base64,'+src;}"
            +"addEventListener('load',()=>{QcfStrip.render(0,gen);QcfStrip.render(1,gen)});"
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

    @Override public void destroy() {
        if(disposed)return;
        disposed=true;++generation;removeJavascriptInterface("QcfStrip");stopLoading();
        super.destroy();
    }

    private final class Bridge {
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
                evaluateJavascript("if(window.receiveStrip)receiveStrip("+side+",'"+safe+"',"+gen+");",null);
            });
        }
    }
}
