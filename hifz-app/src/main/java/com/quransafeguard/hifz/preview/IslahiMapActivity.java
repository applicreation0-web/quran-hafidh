package com.quransafeguard.hifz.preview;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.text.Editable;
import android.text.TextWatcher;
import com.quransafeguard.hifz.core.VerseRef;
import org.brotli.dec.BrotliInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Experimental Iṣlāḥī structural map. Only Work03's 27 passages have source
 * metadata; Qaf/Baqara/Araf are explicitly marked as UI demonstrations.
 * No protected commentary and no generated scholarly prose are bundled here.
 * Absolutely no HifzPrefs or memorisation-engine mutation.
 */
public final class IslahiMapActivity extends Activity {
    private static final String TAWBA =
        "1-4,5-6,7-16,17-22,23-28,29-35,36-37,38-42,43-46,47-52,53-57,58-60,61-66,67-70,71-72,73-78,79-84,85-96,97-101,102-106,107-110,111-113,114-116,117-118,119-122,123-129";
    private static final String QAF = "1-5,6-11,12-14,15-18,19-35,36-37,38-45";
    private static final String BAQARA = "63-82,83-96";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final ArrayList<Block> blocks = new ArrayList<>();
    private LinearLayout root;
    private ScrollView scroll;
    private WebView preview;
    private int surah = 0, selected = -1, listScrollY = 0, blocksScrollY = 0;
    private int generation = 0;
    private boolean demo = false;

    private static final class Block {
        final int surah, first, last;
        final boolean certified;
        Block(int s, int f, int l, boolean c) { surah=s; first=f; last=l; certified=c; }
        String label() { return surah + ":" + first + "–" + last; }
    }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (state != null) {
            surah = state.getInt("surah", 0);
            selected = state.getInt("selected", -1);
            listScrollY = state.getInt("listY", 0);
            blocksScrollY = state.getInt("blocksY", 0);
        }
        render();
    }

    @Override protected void onSaveInstanceState(Bundle out) {
        if (selected < 0 && surah == 0 && scroll != null) listScrollY = scroll.getScrollY();
        if (selected < 0 && surah != 0 && scroll != null) blocksScrollY = scroll.getScrollY();
        out.putInt("surah", surah); out.putInt("selected", selected);
        out.putInt("listY", listScrollY); out.putInt("blocksY", blocksScrollY);
        super.onSaveInstanceState(out);
    }

    private TextView text(String value, int sp, boolean bold) {
        TextView t = Ui.text(this, value, sp, bold);
        t.setTextColor(Ui.INK);
        t.setPadding(Ui.dp(this, 8), Ui.dp(this, 8), Ui.dp(this, 8), Ui.dp(this, 8));
        return t;
    }

    private void header(String title, Runnable back) {
        LinearLayout row = Ui.row(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(Ui.iconButton(this, "‹", "Retour", v -> back.run()));
        TextView label = text(title, 19, true);
        label.setGravity(Gravity.CENTER);
        row.addView(label, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(new View(this), new LinearLayout.LayoutParams(Ui.dp(this, 48), Ui.dp(this, 48)));
        root.addView(row);
        root.addView(Ui.divider(this));
    }

    private void render() {
        closePreview();
        generation++;
        root = Ui.column(this);
        root.setBackgroundColor(Ui.PAPER);
        scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        if (surah == 0) showSurahs();
        else if (selected < 0) showBlocks();
        else showDetail();
        setContentView(root);
        Ui.respectSystemBars(this, root, 0, 0, 0, 0);
    }

    private void showSurahs() {
        header("Carte Iṣlāḥī · TEST", this::finish);
        EditText search = new EditText(this);
        search.setSingleLine(true);
        search.setHint("Chercher n° ou nom arabe");
        search.setTextSize(16);
        search.setContentDescription("Recherche de sourate");
        root.addView(search);
        LinearLayout list = Ui.column(this);
        scroll.addView(list);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            @Override public void onTextChanged(CharSequence s,int start,int before,int count) {
                fillSurahs(list, s.toString());
            }
            @Override public void afterTextChanged(Editable e){}
        });
        fillSurahs(list, "");
        scroll.post(() -> scroll.scrollTo(0,listScrollY));
    }

    private void fillSurahs(LinearLayout list, String query) {
        list.removeAllViews();
        final String q = query.trim();
        for (int s=1; s<=114; s++) {
            String name = QuranSurahNames.rawName(s);
            if (!q.isEmpty() && !name.contains(q) && !Integer.toString(s).contains(q)) continue;
            int id = s;
            String badge = s == 114 ? " · English pilot" : s == 9 ? " · Work03" :
                (s == 2 || s == 50 || s == 7) ? " · Démo" : " · À certifier";
            TextView row = text(s + "  " + QuranSurahNames.name(s) + badge + "  ›", 15, false);
            row.setMinHeight(Ui.dp(this, 50));
            row.setOnClickListener(v -> {
                listScrollY = scroll.getScrollY();
                surah=id; selected=-1; blocksScrollY=0; render();
            });
            list.addView(row);
            list.addView(Ui.divider(this));
        }
    }

    private void populate() {
        blocks.clear(); demo = false;
        String ranges;
        boolean verified;
        if (surah==9) { ranges=TAWBA; verified=true; }
        else if (surah==114) { ranges="1-6"; verified=true; }
        else if (surah==50) { ranges=QAF; verified=false; demo=true; }
        else if (surah==2) { ranges=BAQARA; verified=false; demo=true; }
        else if (surah==7) { ranges="137-171"; verified=false; demo=true; }
        else return;
        for (String part : ranges.split(",")) {
            String[] bounds=part.split("-");
            blocks.add(new Block(surah,Integer.parseInt(bounds[0]),Integer.parseInt(bounds[1]),verified));
        }
    }

    private void showBlocks() {
        populate();
        header(surah + " · " + QuranSurahNames.name(surah), () -> { surah=0; selected=-1; render(); });
        TextView intro = text(demo
            ? "DÉMONSTRATION — ces frontières ne sont pas certifiées comme celles d'Iṣlāḥī."
            : blocks.isEmpty()
            ? "Carte non vérifiée. Aucun bloc ni commentaire inventé."
            : surah==114 ? "Verified block 114:1–6 · source-checked English pilot commentary." : "Work03 — frontières documentées ; texte de l'analyse non disponible dans le livrable.",14,false);
        intro.setTextColor(Ui.MUTED);
        root.addView(intro);
        LinearLayout list = Ui.column(this);
        scroll.addView(list);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1f));
        for(int i=0;i<blocks.size();i++) {
            int index=i;
            Block block=blocks.get(i);
            int p1 = page(block.surah,block.first,true);
            int p2 = page(block.surah,block.last,false);
            TextView b=text(block.label()+"   ·   p. "+p1+(p2>p1?"–"+p2:"")+"   ›",16,true);
            b.setMinHeight(Ui.dp(this,55));
            b.setOnClickListener(v -> {blocksScrollY=scroll.getScrollY();selected=index;render();});
            list.addView(b);list.addView(Ui.divider(this));
        }
        scroll.post(() -> scroll.scrollTo(0,blocksScrollY));
    }

    private int page(int s,int a,boolean first) {
        GeometryRepository g=GeometryRepository.get(this);
        VerseRef ref=new VerseRef(s,a);
        return g.line(first ? g.firstLineIndex(ref):g.lastLineIndex(ref)).page;
    }

    private void showDetail() {
        populate();
        if(selected<0||selected>=blocks.size()) {selected=-1;showBlocks();return;}
        Block b=blocks.get(selected);
        header(b.label(), () -> {selected=-1;render();});
        final boolean nasPilot = b.surah == 114 && b.first == 1 && b.last == 6;
        // Only the verified pilot gets a long, vertically scrollable source-backed detail.
        // Other map blocks keep the original P1 layout and honest availability labels.
        LinearLayout contentHost = root;
        if (nasPilot) {
            scroll = new ScrollView(this);
            scroll.setFillViewport(false);
            contentHost = Ui.column(this);
            scroll.addView(contentHost);
            root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        }
        TextView state=text(nasPilot
             ? "English pilot · commentary notes available offline (not the full original tafsir)."
             : b.certified ? "Frontières consignées par Work03. Vérification indépendante du fac-similé encore requise."
             : "DÉMONSTRATION TECHNIQUE — découpage Iṣlāḥī non certifié.",13,false);
        state.setTextColor(Ui.MUTED);contentHost.addView(state);
        TextView analysis = text(nasPilot
            ? "BLOCK SUMMARY  ·  English editorial digest\\n"
              + IslahiPilotContent.NAS_TITLE + "\\n\\n"
              + IslahiPilotContent.NAS_SUMMARY + "\\n\\n"
              + "Source: Tadabbur-i Qur'an, vol. 9, Central Theme, printed p. 1. "
              + "Editorial summary, not original quotation."
            : "ANALYSE GLOBALE\\nTexte original non fourni. Aucun résumé inventé.", 15, false);
        if (!nasPilot) contentHost.addView(analysis);
        int p1=page(b.surah,b.first,true), p2=page(b.surah,b.last,false);
        TextView indication=text("Mushaf · pages "+p1+(p1==p2?"":" à "+p2)
            +" · toucher une miniature pour ouvrir Lecture",13,true);
        contentHost.addView(indication);
        preview=new WebView(this);
        preview.setBackgroundColor(Ui.PAPER);
        preview.getSettings().setJavaScriptEnabled(true);
        preview.getSettings().setAllowFileAccess(false);
        preview.getSettings().setAllowContentAccess(false);
        preview.addJavascriptInterface(new PreviewBridge(),"IslahiBridge");
        contentHost.addView(preview,nasPilot
            ? new LinearLayout.LayoutParams(-1,Ui.dp(this,275))
            : new LinearLayout.LayoutParams(-1,0,1f));
        if (nasPilot) {
            // Compact block card: grey Mushaf miniature → English summary → one reading action.
            contentHost.addView(analysis);
            android.widget.Button read = Ui.button(this, "Read Tafsir  ›", v ->
                startActivity(IslahiTafsirActivity.forBlock(this, b.surah, b.first, b.last)));
            read.setContentDescription("Read the English Iṣlāḥī pilot notes offline");
            contentHost.addView(read);
        } else {
            TextView notes=text("Gris soutenu : bloc choisi. Gris clair : versets voisins sur les mêmes pages. "
                + "Le tafsīr détaillé d'Iṣlāḥī n'est pas encore certifié.",12,false);
            contentHost.addView(notes);
            if (IslahiPilotContent.isMultiPageNavigationPilot(b.surah, b.first, b.last)) {
                android.widget.Button link = Ui.button(this, "Test page links  ›", v ->
                    startActivity(IslahiTafsirActivity.forBlock(this, b.surah, b.first, b.last)));
                link.setContentDescription("Preview the Mushaf page links; tafsir not certified");
                contentHost.addView(link);
            }
        }
        int token=generation;
        preview.loadDataWithBaseURL(null,"<html><body style='font-family:sans-serif;background:#faf8f0;color:#333'>Chargement des pages réelles…</body></html>","text/html","UTF-8",null);
        executor.execute(() -> {
            try {
                String html=buildPreview(b,p1,p2);
                runOnUiThread(() -> {
                    if(token==generation&&preview!=null&&!isFinishing())
                        preview.loadDataWithBaseURL(null,html,"text/html","UTF-8",null);
                });
            } catch (Exception ex) {
                runOnUiThread(() -> {
                    if(token==generation&&preview!=null)
                        preview.loadDataWithBaseURL(null,
                            "<html><body>Impossible d'afficher la miniature exacte : "+escape(ex.getClass().getSimpleName())+"</body></html>",
                            "text/html","UTF-8",null);
                });
            }
        });
    }

    private final class PreviewBridge {
        @JavascriptInterface public void openPage(int number) {
            runOnUiThread(() -> {
                if(number<1||number>604||selected<0||selected>=blocks.size())return;
                Block b=blocks.get(selected);
                Intent intent=new Intent(IslahiMapActivity.this,StudyReaderActivity.class);
                intent.putExtra(StudyReaderActivity.EXTRA_JUMP_PAGE,number);
                intent.putExtra(StudyReaderActivity.EXTRA_ISLAHI_SURAH,b.surah);
                intent.putExtra(StudyReaderActivity.EXTRA_ISLAHI_START,b.first);
                intent.putExtra(StudyReaderActivity.EXTRA_ISLAHI_END,b.last);
                startActivity(intent);
            });
        }
    }

    private String buildPreview(Block b,int from,int to) throws Exception {
        if(to<from||to>604||from<1||to-from>50)throw new IllegalArgumentException("range");
        StringBuilder html=new StringBuilder(280000);
        html.append("<!doctype html><meta name='viewport' content='width=device-width, initial-scale=1, maximum-scale=1'><style>");
        html.append("body{margin:0;background:#faf8f0;font-family:sans-serif;color:#151515}");
        html.append(".gallery{display:flex;gap:12px;padding:10px;align-items:flex-start;overflow-x:auto;scroll-snap-type:x mandatory}");
        html.append(".card{flex:0 0 190px;scroll-snap-align:start;max-width:190px;border:1px solid #777;padding:5px;background:#fffdf6}");
        html.append(".paper{position:relative;width:100%;cursor:pointer}.paper>svg:first-child{display:block;width:100%;height:auto}");
        html.append(".overlay{position:absolute;top:0;left:0;width:100%;height:100%;pointer-events:none}");
        html.append(".name{text-align:center;font:600 13px sans-serif;padding:4px}");
        html.append("</style><div class='gallery'>");
        WordGeometryRepository wordGeometry=WordGeometryRepository.shared(this);
        for(int page=from;page<=to;page++) {
            html.append("<div class='card'><div class='name'>Page ").append(page)
                .append(" · ").append(page-from+1).append("/").append(to-from+1)
                .append("</div><div class='paper' onclick='IslahiBridge.openPage(").append(page).append(")'>");
            html.append(readSvg(page));
            html.append("<svg class='overlay' viewBox='0 0 345 550' preserveAspectRatio='xMidYMid meet'>");
            int selectedWords=0;
            List<WordGeometryRepository.WordBox> words=wordGeometry.wordsForPage(page);
            if(words.isEmpty())throw new IllegalStateException("missing exact word geometry for "+page);
            for(WordGeometryRepository.WordBox word:words) {
                if(word.surah!=b.surah)continue;
                boolean inside=word.ayah>=b.first&&word.ayah<=b.last;
                boolean neighbor=word.ayah<b.first||word.ayah>b.last;
                if(!inside&&!neighbor)continue;
                String fill=inside?"#595959":"#a9a9a9";
                String opacity=inside?"0.24":"0.08";
                if(inside)selectedWords++;
                double width=word.x1-word.x0, height=word.y1-word.y0;
                if(width<=0||height<=0)continue;
                html.append("<rect x='").append(word.x0).append("' y='").append(word.y0)
                    .append("' width='").append(width).append("' height='").append(height)
                    .append("' fill='").append(fill).append("' fill-opacity='").append(opacity).append("'/>");
            }
            if(selectedWords==0)throw new IllegalStateException("missing selected words on page "+page);
            html.append("</svg></div></div>");
        }
        html.append("</div></html>");
        return html.toString();
    }
    private String readSvg(int page)throws Exception{
        String asset=String.format(Locale.ROOT,"mushaf/hafs/kfqc/svg-br/%03d.svg.br",page);
        try(InputStream raw=getAssets().open(asset);BrotliInputStream in=new BrotliInputStream(raw)){
            ByteArrayOutputStream out=new ByteArrayOutputStream();
            byte[] buf=new byte[8192];int n;
            while((n=in.read(buf))!=-1)out.write(buf,0,n);
            String svg=new String(out.toByteArray(),StandardCharsets.UTF_8);
            if(!svg.contains("<svg")||!svg.contains("</svg>"))throw new IllegalStateException("invalid svg "+page);
            return svg;
        }
    }
    private static String escape(String s) {
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
    }
    private void closePreview() {
        if(preview!=null) {
            preview.removeJavascriptInterface("IslahiBridge");
            preview.stopLoading();
            if(preview.getParent() instanceof ViewGroup)((ViewGroup)preview.getParent()).removeView(preview);
            preview.destroy();preview=null;
        }
    }
    @Override public void onBackPressed() {
        if(selected>=0){selected=-1;render();return;}
        if(surah!=0){surah=0;render();return;}
        super.onBackPressed();
    }
    @Override protected void onDestroy() {
        generation++;executor.shutdownNow();closePreview();super.onDestroy();
    }
}
