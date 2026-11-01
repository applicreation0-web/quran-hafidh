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
    private static final String SAFE_LINE_CUTS_SHA256="54335a140838909f3a2d596da3aa1afc2307fb02ace042ca12ae0614cb3155f0";
    private int generation;
    private static final int CHUNK_CHARS=24_000;
    private String pendingEncoded;
    private int pendingGen,pendingSide,pendingNext;
    private boolean disposed;
    private Runnable renderedListener;
    private int tafsirFocusSide=-1; // -1: symmetric pair; 0/1: the exact QCF pivot for selected Tafsir
    private VerseRef left, right;
    private int pageLeft,pageRight;
    private int[] wordsLeft,wordsRight,cropAnchorsLeft,cropAnchorsRight;
    private final int[] renderStatus=new int[2];

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

    void setOnRendered(Runnable listener){renderedListener=listener;}
    /** Reuse the ONE authenticated QCF excerpt WebView in Tafsir: never decode
     * another 600KB Quran SVG or generate Arabic text for a badge. */
    void setTafsirFocusSide(int side){
        if(side< -1||side>1)throw new IllegalArgumentException("Invalid Tafsir side");
        tafsirFocusSide=side;
        if(!disposed)evaluateJavascript("if(window.focusQcf)focusQcf("+side+");",null);
    }
    int tafsirFocusSideForTest(){return tafsirFocusSide;}
    /** Invalidate old bridge callbacks before any new comparison label is shown. */
    void clearComparison(){
        ++generation;pendingEncoded=null;renderStatus[0]=0;renderStatus[1]=0;
        stopLoading();
        loadDataWithBaseURL(null,"<html><body style='margin:0;background:#faf8f0'></body></html>",
            "text/html","UTF-8",null);
    }
    int generationForTest(){return generation;}

    void show(VerseRef a,int aPage,int[] aWords,VerseRef b,int bPage,int[] bWords) {
        show(a,aPage,aWords,aWords,b,bPage,bWords,bWords);
    }

    void show(VerseRef a,int aPage,int[] aWords,int[] aCropAnchors,
              VerseRef b,int bPage,int[] bWords,int[] bCropAnchors) {
        if(aWords==null||bWords==null||aCropAnchors==null||bCropAnchors==null)
            throw new IllegalArgumentException("QCF difference and location arrays are required");
        if(disposed || a==null || b==null || aPage<1 || aPage>604 || bPage<1 || bPage>604)
            throw new IllegalArgumentException("Invalid paired Quran excerpt");
        left=a;right=b;pageLeft=aPage;pageRight=bPage;
        wordsLeft=aWords.clone();wordsRight=bWords.clone();
        cropAnchorsLeft=aCropAnchors.clone();cropAnchorsRight=bCropAnchors.clone();
        int gen=++generation;
        pendingEncoded=null;
        renderStatus[0]=0;renderStatus[1]=0;
        String html="<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1'>"
            +"<style>*{box-sizing:border-box}html,body{margin:0;width:100%;height:100%;overflow:hidden;background:#faf8f0}"
            +"main{display:grid;grid-template-columns:1fr 1fr;height:100%;gap:6px;padding:3px}"
            +"section{border-top:1px solid #88847d;min-width:0;min-height:0;display:flex;align-items:center;justify-content:center}"
            +"section .crop{width:100%;height:100%;min-height:0;display:flex;align-items:center;justify-content:center;overflow:hidden}"
            +"section svg{display:block;flex:none;max-width:none;max-height:none;transform-origin:center center}"
            +"span{font:12px sans-serif;color:#524f49;text-align:center}</style></head>"
            +"<body><main><section><div class='crop' id='crop0'><span>Chargement de l'extrait QCF…</span></div></section>"
            +"<section><div class='crop' id='crop1'><span>Chargement de l'extrait QCF…</span></div></section></main>"
            +"<script>var gen="+gen+";var parts=[],expect=0,activeSide=-1,qcfNodes=[null,null],focusedSide="+tafsirFocusSide+";"
            // Paired source SVGs use one and ONLY one physical pixel/source-unit
            // ratio. Independent 100% SVG fitting had enlarged one pivot more
            // than its counterpart, making memorisation comparisons misleading.
            // Paired SVG excerpts always use exactly one physical glyph scale.
            // Focus on independently audited word boxes, not unequal 1-to-4 row
            // source windows. The actual QCF path geometry is never transformed.
            +"function fitPairedQcf(){if(!qcfNodes[0]||!qcfNodes[1])return false;"
            +"var dims=[],scale=Infinity;for(var i=0;i<2;i++){"
            +"var node=qcfNodes[i],vb=node.viewBox.baseVal,box=document.getElementById('crop'+i);"
            +"if(!box||!vb||vb.width<=0||vb.height<=0||box.clientWidth<=0||box.clientHeight<=0)return false;"
            +"var fx=+node.getAttribute('data-qcf-focus-x'),fy=+node.getAttribute('data-qcf-focus-y');"
            +"var fw=+node.getAttribute('data-qcf-focus-w'),fh=+node.getAttribute('data-qcf-focus-h');"
            +"if(!isFinite(fx)||!isFinite(fy)||!isFinite(fw)||!isFinite(fh)||fw<=0||fh<=0"
            +"||fx<0||fx>vb.width||fy<0||fy>vb.height)return false;"
            +"dims.push({w:vb.width,h:vb.height,fx:fx,fy:fy,fw:fw,fh:fh});"
            +"var contextWidth=Math.max(154,fw+84),contextHeight=Math.max(46,fh+18);"
            +"scale=Math.min(scale,box.clientWidth/contextWidth,box.clientHeight/contextHeight);}"
            +"if(!isFinite(scale)||scale<=0)return false;"
            +"for(var j=0;j<2;j++){var el=qcfNodes[j],d=dims[j];"
            +"el.style.width=(d.w*scale).toFixed(3)+'px';el.style.height=(d.h*scale).toFixed(3)+'px';"
            +"el.style.transform='translate('+((d.w/2-d.fx)*scale).toFixed(3)+'px,'"
            +"+((d.h/2-d.fy)*scale).toFixed(3)+'px)';"
            +"el.setAttribute('data-qcf-shared-scale',scale.toFixed(8));"
            +"el.setAttribute('data-qcf-focus-centered','true');}return true;}"
            // Selecting a Tafsir tab must not hide the other source-QCF pivot.
            +"function focusQcf(side){focusedSide=side;var main=document.querySelector('main');"
            +"if(!main)return;main.style.gridTemplateColumns='1fr 1fr';"
            +"document.getElementById('crop0').parentNode.style.display='flex';"
            +"document.getElementById('crop1').parentNode.style.display='flex';"
            +"main.setAttribute('data-qcf-tafsir-tab',''+side);"
            +"requestAnimationFrame(function(){requestAnimationFrame(fitPairedQcf);});}"
            +"addEventListener('resize',function(){requestAnimationFrame(fitPairedQcf);});"
            +"addEventListener('load',function(){if(window.ResizeObserver){var ro=new ResizeObserver(fitPairedQcf);"
            +"ro.observe(document.getElementById('crop0'));ro.observe(document.getElementById('crop1'));}});"
            +"function receiveStripChunk(side,i,total,part,g){if(g!==gen)return;"
            +"if(total<1||total>100||i!==expect||(activeSide!==-1&&activeSide!==side)){QcfStrip.stripStatus(side,g,false);return;}"
            +"activeSide=side;parts.push(part);expect++;"
            +"if(expect===total){var complete=parts.join('');parts=[];expect=0;activeSide=-1;receiveStrip(side,complete,g);}"
            +"else QcfStrip.chunkAck(side,i,g);}"
            +"function receiveStrip(i,src,g){if(g!==gen)return;var target=document.getElementById('crop'+i);if(!target)return;"
            +"if(!src){target.textContent='Extrait QCF indisponible — ouvrir le Muṣḥaf';QcfStrip.stripStatus(i,g,false);return;}"
            +"try{var raw=atob(src),bytes=new Uint8Array(raw.length);for(var j=0;j<raw.length;j++)bytes[j]=raw.charCodeAt(j);"
            +"var parsed=new DOMParser().parseFromString(new TextDecoder('utf-8').decode(bytes),'image/svg+xml');"
            +"var svg=parsed.documentElement;if(!svg||svg.localName!=='svg'||parsed.getElementsByTagName('parsererror').length||!svg.querySelector('path.ayahPolygon'))throw Error('Bad QCF excerpt');"
            +"var original=document.importNode(svg,true);target.textContent='';target.appendChild(original);"
            +"qcfNodes[i]=original;"
            +"requestAnimationFrame(function(){requestAnimationFrame(function(){"
            +"if(qcfNodes[0]&&qcfNodes[1]&&!fitPairedQcf()){requestAnimationFrame(fitPairedQcf);}"
            +"var box=original.getBoundingClientRect(),vb=original.viewBox.baseVal;"
            +"var sourceReady=vb&&vb.width>0&&vb.height>0&&original.querySelector('path.ayahPolygon');"
            +"var visualReady=sourceReady&&box.width>20&&box.height>10;"
            +"QcfStrip.stripStatus(i,g,!!visualReady);});});"
            +"}catch(e){target.textContent='Extrait QCF indisponible — ouvrir le Muṣḥaf';QcfStrip.stripStatus(i,g,false);}}"
            +"addEventListener('load',()=>{focusQcf(focusedSide);QcfStrip.render(0,gen);});"
            +"</script></body></html>";
        loadDataWithBaseURL(null,html,"text/html","UTF-8",null);
    }

    /** Immutable, 100%-QCF source. Returns empty when the geometry does not validate. */
    static String cropSvg(String source,List<WordGeometryRepository.WordBox> pageWords,
                          VerseRef target,int[] underlineWords,List<GeometryRepository.LineMeta> physicalLines,double[] safeCuts) {
        return cropSvg(source,pageWords,target,underlineWords,underlineWords,physicalLines,safeCuts);
    }

    static String cropSvg(String source,List<WordGeometryRepository.WordBox> pageWords,
                          VerseRef target,int[] underlineWords,int[] windowAnchors,
                          List<GeometryRepository.LineMeta> physicalLines,double[] safeCuts) {
        if(source==null||pageWords==null||target==null||underlineWords==null||windowAnchors==null
                ||windowAnchors.length==0)return "";
        try {
            int[] focus=underlineWords.length==0?windowAnchors:underlineWords;
            return cropSvg(source,pageWords,target,underlineWords,windowAnchors,
                physicalLines,safeCuts,QcfPivotBalance.single(pageWords,target,focus));
        }catch(IllegalArgumentException invalid){return "";} // no guess for invalid source annotations
    }

    private static String cropSvg(String source,List<WordGeometryRepository.WordBox> pageWords,
                          VerseRef target,int[] underlineWords,int[] windowAnchors,
                          List<GeometryRepository.LineMeta> physicalLines,double[] safeCuts,
                          QcfPivotBalance.Selection selection) {
        if(source==null||pageWords==null||target==null||selection==null)return "";
        List<WordGeometryRepository.WordBox> found=new ArrayList<>();
        for(WordGeometryRepository.WordBox word:pageWords)
            if(word.surah==target.getSurah()&&word.ayah==target.getAyah())found.add(word);
        if(found.isEmpty())return "";
        double x0=0,x1=345;
        StringBuilder clips=new StringBuilder();
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
        // An absence is not a Quran word: crop around the approved common
        // anchor, but underline only genuinely present contrast words.
        double[] rows=QcfAtlasLineWindow.bounds(pageWords,physicalLines,target,selection.wordNumbers(),originY,safeCuts);
        if(rows==null)return "";
        double y0=rows[0],y1=rows[1];
        // Reframe only this *source SVG crop* around exact audited QCF word boxes.
        // Full 345:550 page views elsewhere are never changed, and no glyph is rebuilt.
        // Fail open to the entire 345-unit line when geometry/line safety is uncertain.
        double[] xWindow=horizontalWordWindow(pageWords,physicalLines,target,
            selection.wordNumbers(),originY,y0,y1);
        x0=xWindow[0];x1=xWindow[1];
        // The first actual KFQC page has a shifted SVG viewBox. The audited word
        // boxes are page-relative 0..345/550; translate them to the original SVG
        // coordinate origin before cropping or adding a difference underline.
        String vb=String.format(Locale.ROOT,"viewBox='%.2f %.2f %.2f %.2f'",
            originX+x0,originY+y0,x1-x0,y1-y0);
        String cropped=source.substring(0,m.start())+vb+source.substring(m.end());
        // Clip at the page edge and outside the complete selected source lines.
        clips.append(String.format(Locale.ROOT,
            "<rect x='%.2f' y='%.2f' width='%.2f' height='%.2f'/>",
            originX+x0,originY+y0,x1-x0,y1-y0));
        int rootStart=cropped.indexOf("<svg");
        int rootEnd=rootStart<0?-1:cropped.indexOf('>',rootStart);
        if(rootEnd<0||!cropped.contains("</svg>"))return "";
        String clipId="atlas-word-window-"+target.getSurah()+"-"+target.getAyah();
        String selectedClipId="atlas-balanced-words-"+target.getSurah()+"-"+target.getAyah();
        StringBuilder visibleBoxes=new StringBuilder();
        for(WordGeometryRepository.WordBox word:selection.words){
            // Original vector ink is only revealed at the verified equal-count
            // QCF word boxes, with a 2.7-unit harakat/diacritics safety margin.
            double xL=Math.max(x0,word.x0-2.7),xR=Math.min(x1,word.x1+2.7);
            double yT=Math.max(y0,word.y0-2.7),yB=Math.min(y1,word.y1+2.7);
            if(xR<=xL||yB<=yT||word.x0<x0||word.x1>x1||word.y0<y0||word.y1>y1)
                return "";
            visibleBoxes.append(String.format(Locale.ROOT,
                "<rect x='%.2f' y='%.2f' width='%.2f' height='%.2f'/>",
                originX+xL,originY+yT,xR-xL,yB-yT));
        }
        cropped=cropped.substring(0,rootEnd+1)
            +"<defs><clipPath id='"+clipId+"'>"+clips+"</clipPath>"
            +"<clipPath id='"+selectedClipId+"'>"+visibleBoxes+"</clipPath></defs>"
            +"<g clip-path='url(#"+clipId+")'><g clip-path='url(#"+selectedClipId+")'>"
            +cropped.substring(rootEnd+1).replace("</svg>","</g></g></svg>");
        // Change the intrinsic SVG aspect ratio as well; no stretched / reflowed glyphs.
        cropped=cropped.replaceFirst("width='345' height='550'",
            String.format(Locale.ROOT,"width='%.2f' height='%.2f'",x1-x0,y1-y0));
        // Focal geometry references the genuine QCF word boxes, in SVG units.
        // On a true omission, the common-word anchor supplies a position only;
        // it is NEVER painted as a missing replacement or underlined.
        int[] visualFocus=underlineWords.length==0?windowAnchors:underlineWords;
        StringBuilder pivotTint=new StringBuilder(),focusHoles=new StringBuilder();
        StringBuilder strokes=new StringBuilder("<g id='exact-word-differences' fill='none' stroke='#121211' stroke-width='0.85'>");
        for(int index:visualFocus){
            WordGeometryRepository.WordBox matching=null;
            for(WordGeometryRepository.WordBox w:found)
                if(w.word==index){matching=w;break;}
            if(matching==null)return "";
            double xL=Math.max(x0,matching.x0-1.7),yT=Math.max(y0,matching.y0-1.4);
            double xR=Math.min(x1,matching.x1+1.7),yB=Math.min(y1,matching.y1+1.4);
            if(xR<=xL||yB<=yT)return "";
            String geometry=String.format(Locale.ROOT,
                "x='%.2f' y='%.2f' width='%.2f' height='%.2f'",
                originX+xL,originY+yT,xR-xL,yB-yT);
            focusHoles.append("<rect ").append(geometry).append(" fill='black'/>");
            if(underlineWords.length>0){
                pivotTint.append("<rect ").append(geometry)
                    .append(" rx='1.5' fill='#b9b9b9' fill-opacity='0.22'/>");
                double y=Math.min(549,matching.y1+1.5);
                strokes.append(String.format(Locale.ROOT,
                    "<line x1='%.2f' y1='%.2f' x2='%.2f' y2='%.2f'/>",
                    originX+matching.x0,originY+y,originX+matching.x1,originY+y));
            }
        }
        String attributes=String.format(Locale.ROOT,
            " data-qcf-focus-x='%.2f' data-qcf-focus-y='%.2f'"
            +" data-qcf-focus-w='%.2f' data-qcf-focus-h='%.2f'"
            +" data-qcf-before-words='%d' data-qcf-after-words='%d'"
            +" data-qcf-visible-words='%d' data-qcf-pivot-words='%d'"
            +" data-qcf-omission='%s'",
            selection.centerX-x0,selection.centerY-y0,
            selection.width,selection.height,
            selection.before,selection.after,selection.words.size(),selection.pivotWords,
            underlineWords.length==0?"true":"false");
        cropped=cropped.replaceFirst("<svg","<svg"+attributes);
        strokes.append("</g>");
        String maskId="atlas-comparison-pivot-mask-"+target.getSurah()+"-"+target.getAyah();
        // EXACTLY the same paper veil on both source excerpts. The anchor on
        // the omission side stays clear, but has no grey box or underline.
        String focus="<defs><mask id='"+maskId+"' maskUnits='userSpaceOnUse'>"
            +String.format(Locale.ROOT,
                "<rect x='%.2f' y='%.2f' width='%.2f' height='%.2f' fill='white'/>",
                originX+x0,originY+y0,x1-x0,y1-y0)
            +focusHoles+"</mask></defs>"
            +"<g id='atlas-comparison-pivot-tone'>"+pivotTint
            +String.format(Locale.ROOT,
                "<rect x='%.2f' y='%.2f' width='%.2f' height='%.2f' fill='#faf8f0' fill-opacity='0.36' mask='url(#%s)'/>",
                originX+x0,originY+y0,x1-x0,y1-y0,maskId)
            +"</g>";
        return cropped.replace("</svg>",focus+strokes+"</svg>");
    }

    /**
     * Optional horizontal focus on the original page's QCF word rectangles.
     * We do not transliterate, reflow or paint a single Arabic character.
     * Always keep a source-context word on each side, and fall back to the
     * complete 345-unit physical row on ambiguous/trans-line geometries.
     */
    static double[] horizontalWordWindow(List<WordGeometryRepository.WordBox> words,
            List<GeometryRepository.LineMeta> lines, VerseRef verse,int[] anchors,
            double originY,double y0,double y1) {
        final double[] full={0,345};
        if(words==null||lines==null||verse==null||anchors==null||anchors.length==0)
            return full;
        int row=-1,min=Integer.MAX_VALUE,max=0;
        for(int index:anchors){
            WordGeometryRepository.WordBox matched=null;
            for(WordGeometryRepository.WordBox w:words)
                if(w.surah==verse.getSurah()&&w.ayah==verse.getAyah()&&w.word==index){
                    matched=w;break;
                }
            if(matched==null)return full;
            int found=-1;
            double cy=(matched.y0+matched.y1)/2+originY;
            for(int i=0;i<lines.size();i++)
                if(cy>=lines.get(i).top&&cy<lines.get(i).bottom){
                    if(found>=0)return full;found=i;
                }
            if(found<0||(row>=0&&found!=row))return full;
            row=found;min=Math.min(min,index);max=Math.max(max,index);
        }
        // A line-window forced to retain neighbouring rows must stay complete
        // horizontally; otherwise an unrelated Quran word could be clipped.
        int physicalRows=0;
        for(GeometryRepository.LineMeta line:lines){
            double cy=(line.top+line.bottom)/2-originY;
            if(cy>=y0&&cy<=y1)physicalRows++;
        }
        if(physicalRows!=1)return full;
        double left=345,right=0;
        for(WordGeometryRepository.WordBox w:words){
            if(w.surah!=verse.getSurah()||w.ayah!=verse.getAyah()
                    ||w.word<min-1||w.word>max+1)continue;
            double cy=(w.y0+w.y1)/2+originY;
            if(cy<lines.get(row).top||cy>=lines.get(row).bottom)continue;
            left=Math.min(left,w.x0);right=Math.max(right,w.x1);
        }
        if(!Double.isFinite(left)||!Double.isFinite(right)||right<=left)return full;
        left=Math.max(0,left-8);right=Math.min(345,right+8);
        // Never bisect any other source word box touched by the crop edge.
        for(int iter=0;iter<words.size();iter++){
            boolean extended=false;
            for(WordGeometryRepository.WordBox w:words){
                double cy=(w.y0+w.y1)/2+originY;
                if(cy<lines.get(row).top||cy>=lines.get(row).bottom)continue;
                if(w.x0<left&&w.x1>left){
                    left=Math.max(0,w.x0-3);extended=true;
                }
                if(w.x0<right&&w.x1>right){
                    right=Math.min(345,w.x1+3);extended=true;
                }
            }
            if(!extended)break;
        }
        if(right-left<35||right-left>255)return full;
        for(int index:anchors){
            boolean inside=false;
            for(WordGeometryRepository.WordBox w:words)
                if(w.surah==verse.getSurah()&&w.ayah==verse.getAyah()
                        &&w.word==index&&w.x0>=left&&w.x1<=right){inside=true;break;}
            if(!inside)return full;
        }
        return new double[]{left,right};
    }

    /** Audited raster measurement only; the displayed vectors stay byte exact. */
    static double[] safeLineCuts(Context context,int page,String source){
        try(java.io.InputStream input=context.getAssets().open("mutashabihat/qcf_safe_line_cuts_v1.json");
            java.io.ByteArrayOutputStream output=new java.io.ByteArrayOutputStream()){
            byte[] block=new byte[8192];int count;
            while((count=input.read(block))!=-1)output.write(block,0,count);
            if(!SAFE_LINE_CUTS_SHA256.equals(sha256(output.toByteArray())))return null;
            org.json.JSONObject root=new org.json.JSONObject(output.toString("UTF-8"));
            if(root.getInt("schema")!=1||root.getInt("raster_scale")!=4
                ||root.getInt("minimum_blank_band_source_units")<2
                ||!"88bf38206b8f4cf6123d2355034953f376d2446d".equals(root.getString("source_commit")))return null;
            org.json.JSONObject measured=root.getJSONObject("pages").getJSONObject(Integer.toString(page));
            if(!sha256(source.getBytes(StandardCharsets.UTF_8)).equals(measured.getString("sha256")))return null;
            org.json.JSONArray cuts=measured.getJSONArray("cuts");
            double[] result=new double[cuts.length()];
            for(int i=0;i<result.length;i++)result[i]=cuts.isNull(i)?Double.NaN:cuts.getDouble(i);
            return result;
        }catch(Exception missingAudit){return null;}
    }

    private static String sha256(byte[] bytes)throws java.security.NoSuchAlgorithmException{
        byte[] digest=java.security.MessageDigest.getInstance("SHA-256").digest(bytes);
        StringBuilder hash=new StringBuilder();
        for(byte value:digest)hash.append(String.format(Locale.ROOT,"%02x",value&255));
        return hash.toString();
    }

    static List<GeometryRepository.LineMeta> physicalLines(GeometryRepository geometry,int page){
        List<GeometryRepository.LineMeta> result=new ArrayList<>();
        for(int i=0;i<geometry.lineCount();i++){
            GeometryRepository.LineMeta line=geometry.line(i);
            if(line.page==page)result.add(line);
        }
        return result;
    }

    String renderStatusForTest(){return renderStatus[0]+"/"+renderStatus[1];}
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
                if(bothRenderedForTest()&&renderedListener!=null)renderedListener.run();
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
            int[] window=side==0?cropAnchorsLeft:cropAnchorsRight;
            String encoded="";
            try{
                GeometryRepository geometry=GeometryRepository.get(getContext());
                // A two-page ayah is NOT arbitrarily truncated into a claimed full excerpt.
                if(geometry.pageForVerse(verse)==page
                    && MawdudiMapModel.lastPageForVerse(verse,geometry)==page){
                    String full=MapQcfThumbnails.readSvg(getContext(),page);
                    // A single verified count governs both Quran excerpts.
                    WordGeometryRepository words=WordGeometryRepository.shared(getContext());
                    QcfPivotBalance.Selection[] balanced=QcfPivotBalance.paired(
                        words.wordsForPage(pageLeft),left,
                        wordsLeft.length==0?cropAnchorsLeft:wordsLeft,
                        words.wordsForPage(pageRight),right,
                        wordsRight.length==0?cropAnchorsRight:wordsRight);
                    String excerpt=cropSvg(full,words.wordsForPage(page),
                        verse,difference,window,physicalLines(geometry,page),
                        safeLineCuts(getContext(),page,full),balanced[side]);
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
