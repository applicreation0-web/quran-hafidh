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
            +"section svg{display:block;flex:none;max-width:none;max-height:none}"
            +"span{font:12px sans-serif;color:#524f49;text-align:center}</style></head>"
            +"<body><main><section><div class='crop' id='crop0'><span>Chargement de l'extrait QCF…</span></div></section>"
            +"<section><div class='crop' id='crop1'><span>Chargement de l'extrait QCF…</span></div></section></main>"
            +"<script>var gen="+gen+";var parts=[],expect=0,activeSide=-1,qcfNodes=[null,null],focusedSide="+tafsirFocusSide+";"
            // Paired source SVGs use one and ONLY one physical pixel/source-unit
            // ratio. Independent 100% SVG fitting had enlarged one pivot more
            // than its counterpart, making memorisation comparisons misleading.
            +"function fitPairedQcf(){"
            +"if(focusedSide>=0){var one=qcfNodes[focusedSide],slot=document.getElementById('crop'+focusedSide);"
            +"if(!one||!slot)return false;var oneBox=one.viewBox.baseVal;"
            +"if(!oneBox||oneBox.width<=0||oneBox.height<=0||slot.clientWidth<=0||slot.clientHeight<=0)return false;"
            +"var zoom=Math.min(slot.clientWidth/oneBox.width,slot.clientHeight/oneBox.height);"
            +"one.style.width=(oneBox.width*zoom).toFixed(3)+'px';"
            +"one.style.height=(oneBox.height*zoom).toFixed(3)+'px';"
            +"one.setAttribute('data-qcf-tafsir-focus',''+focusedSide);return true;}"
            +"if(!qcfNodes[0]||!qcfNodes[1])return false;"
            +"var dims=[],scale=Infinity;for(var i=0;i<2;i++){var node=qcfNodes[i],vb=node.viewBox.baseVal;"
            +"var box=document.getElementById('crop'+i);if(!box||!vb||vb.width<=0||vb.height<=0||box.clientWidth<=0||box.clientHeight<=0)return false;"
            +"dims.push({w:vb.width,h:vb.height});scale=Math.min(scale,box.clientWidth/vb.width,box.clientHeight/vb.height);}"
            +"if(!isFinite(scale)||scale<=0)return false;"
            +"for(var j=0;j<2;j++){var el=qcfNodes[j];"
            +"el.style.width=(dims[j].w*scale).toFixed(3)+'px';el.style.height=(dims[j].h*scale).toFixed(3)+'px';"
            +"el.setAttribute('data-qcf-shared-scale',scale.toFixed(8));}return true;}"
            +"function focusQcf(side){focusedSide=side;var main=document.querySelector('main');"
            +"if(!main)return;main.style.gridTemplateColumns=side<0?'1fr 1fr':'1fr';"
            +"var a=document.getElementById('crop0').parentNode,b=document.getElementById('crop1').parentNode;"
            +"a.style.display=(side<0||side===0)?'flex':'none';"
            +"b.style.display=(side<0||side===1)?'flex':'none';"
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
            +"var visualReady=focusedSide>=0&&focusedSide!==i?sourceReady:(box.width>20&&box.height>10);"
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
        double[] rows=QcfAtlasLineWindow.bounds(pageWords,physicalLines,target,windowAnchors,originY,safeCuts);
        if(rows==null)return "";
        double y0=rows[0],y1=rows[1];
        // Reframe only this *source SVG crop* around exact audited QCF word boxes.
        // Full 345:550 page views elsewhere are never changed, and no glyph is rebuilt.
        // Fail open to the entire 345-unit line when geometry/line safety is uncertain.
        double[] xWindow=horizontalWordWindow(pageWords,physicalLines,target,
            windowAnchors,originY,y0,y1);
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
        cropped=cropped.substring(0,rootEnd+1)
            +"<defs><clipPath id='"+clipId+"'>"+clips+"</clipPath></defs>"
            +"<g clip-path='url(#"+clipId+")'>"+cropped.substring(rootEnd+1)
                .replace("</svg>","</g></svg>");
        // Change the intrinsic SVG aspect ratio as well; no stretched / reflowed glyphs.
        cropped=cropped.replaceFirst("width='345' height='550'",
            String.format(Locale.ROOT,"width='%.2f' height='%.2f'",x1-x0,y1-y0));
        // VISUAL OVERLAY ONLY in the original QCF COMPARISON EXCERPTS.
        // Full Mushaf SVGs, glyph paths, word indices and recitation pages
        // remain byte-for-byte unchanged. On an absent-word side, do not
        // invent a marked pivot or fade an innocent common-word anchor.
        StringBuilder pivotTint=new StringBuilder();
        StringBuilder focusHoles=new StringBuilder();
        StringBuilder strokes=new StringBuilder("<g id='exact-word-differences' fill='none' stroke='#121211' stroke-width='0.85'>");
        for(int index:underlineWords){
            if(index<1)return ""; // no guess for invalid source annotations
            WordGeometryRepository.WordBox matching=null;
            for(WordGeometryRepository.WordBox w:found)
                if(w.word==index){matching=w;break;}
            if(matching==null)return ""; // source word count must match audited QCF sidecar
            // Anchored to VERIFIED individual word boxes: never tint entire
            // Quran lines, verses or neighboring source words.
            double focusX=Math.max(x0,matching.x0-1.7);
            double focusY=Math.max(y0,matching.y0-1.4);
            double focusRight=Math.min(x1,matching.x1+1.7);
            double focusBottom=Math.min(y1,matching.y1+1.4);
            if(focusRight<=focusX||focusBottom<=focusY)return "";
            String geometry=String.format(Locale.ROOT,
                "x='%.2f' y='%.2f' width='%.2f' height='%.2f'",
                originX+focusX,originY+focusY,focusRight-focusX,focusBottom-focusY);
            pivotTint.append("<rect ").append(geometry)
                .append(" rx='1.5' fill='#b9b9b9' fill-opacity='0.22'/>");
            focusHoles.append("<rect ").append(geometry).append(" fill='black'/>");
            double y=Math.min(549,matching.y1+1.5);
            strokes.append(String.format(Locale.ROOT,
                "<line x1='%.2f' y1='%.2f' x2='%.2f' y2='%.2f'/>",
                originX+matching.x0,originY+y,originX+matching.x1,originY+y));
        }
        strokes.append("</g>");
        if(!cropped.contains("</svg>"))return "";
        if(underlineWords.length==0)return cropped.replace("</svg>",strokes+"</svg>");
        String maskId="atlas-comparison-pivot-mask-"+target.getSurah()+"-"+target.getAyah();
        // An alpha paper veil on surrounding words creates a subtle fade.
        // The exact selected word boxes remain clear, with light-grey tint.
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
                    String excerpt=cropSvg(full,
                        WordGeometryRepository.shared(getContext()).wordsForPage(page),
                        verse,difference,window,physicalLines(geometry,page),safeLineCuts(getContext(),page,full));
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
