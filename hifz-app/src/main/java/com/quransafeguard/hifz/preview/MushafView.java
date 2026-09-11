package com.quransafeguard.hifz.preview;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.quransafeguard.hifz.core.VerseRef;

import org.brotli.dec.BrotliInputStream;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Offline Mushaf renderer using the exact packaged KFQC SVG page. */
public final class MushafView extends WebView {
    public interface Listener {
        void onVerseTap(VerseRef verse);
        void onReady();
        void onError(String message);
        void onPageShown(int page);
        default void onSurfaceTap() {}
    }

    private static final String INLINE_NONCE = "hifz-local";
    private static final String SCRIPT_TAG = "<script src=\"reader.js\"></script>";
    private static final String SVG_SLOT = "<!--MUSHAF_SVG-->";
    private static final long PAGE_TIMEOUT_MS = 4_000L;

    private Listener listener;
    private boolean listenerNotified;
    private boolean ready;
    private Runnable pending;
    private JSONObject geometryPages;
    private final HifzPrefs prefs;
    private final EinkController eink = new EinkController();

    private int requestedPage = 0;
    private boolean pageShown;
    private boolean retried;
    private List<VerseRef> lastSelection;
    private List<String> lastLineIds;
    private int lastMask;

    private final Runnable watchdog = new Runnable() {
        @Override public void run() {
            if (pageShown || requestedPage == 0) return;
            if (!retried) {
                retried = true;
                load(requestedPage, lastSelection, lastLineIds, lastMask);
            } else {
                report("Page " + requestedPage + " non affichée (délai dépassé). Changez de page ou rouvrez l’écran.");
            }
        }
    };

    @SuppressLint("SetJavaScriptEnabled")
    public MushafView(Context context) {
        super(context);
        prefs = new HifzPrefs(context);
        setBackgroundColor(android.graphics.Color.WHITE);
        WebSettings s = getSettings();
        s.setJavaScriptEnabled(true);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setDomStorageEnabled(false);
        s.setBlockNetworkLoads(true);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);
        addJavascriptInterface(new Bridge(), "HifzNative");
        setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) { return true; }
            @Override public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                removeCallbacks(watchdog);
                report("Moteur d’affichage redémarré.");
                Context c = getContext();
                if (c instanceof Activity && !((Activity) c).isFinishing()) post(((Activity) c)::recreate);
                return true;
            }
        });
    }

    public void setListener(Listener value) {
        listener = value;
        if (value != null && !listenerNotified) {
            listenerNotified = true;
            post(value::onReady);
        }
    }

    public void show(int page, List<VerseRef> selection, List<String> lineIds, int maskPercent) {
        retried = false;
        load(page, selection, lineIds, maskPercent);
    }

    private void load(int page, List<VerseRef> selection, List<String> lineIds, int maskPercent) {
        removeCallbacks(watchdog);
        requestedPage = page;
        pageShown = false;
        ready = false;
        pending = null;
        lastSelection = selection;
        lastLineIds = lineIds;
        lastMask = maskPercent;
        try {
            String html = readAssetText("hifzreader/index.html");
            String javascript = readAssetText("hifzreader/reader.js");
            String svg = readPageSvg(page);
            String geometry = readPageGeometry(page);
            if (!html.contains(SCRIPT_TAG) || !html.contains(SVG_SLOT)) throw new IllegalStateException("reader template incomplete");
            JSONArray verses = new JSONArray();
            for (VerseRef ref : selection) verses.put(ref.toString());
            JSONArray lines = new JSONArray();
            for (String id : lineIds) lines.put(id);
            JSONObject boot = new JSONObject()
                .put("page", page)
                .put("selection", verses)
                .put("lines", lines)
                .put("mask", Math.max(0, Math.min(100, maskPercent)))
                .put("eink", eink.isEink(prefs))
                .put("geometry", new JSONObject(geometry));
            String inline = "<script nonce=\"" + INLINE_NONCE + "\">window.HIFZ_BOOT=" +
                boot.toString().replace("</", "<\\/") + ";\n" + javascript + "</script>";
            html = html
                .replace("script-src 'self';", "script-src 'nonce-" + INLINE_NONCE + "';")
                .replace("connect-src 'self'", "connect-src 'none'")
                .replace(SCRIPT_TAG, inline)
                .replace(SVG_SLOT, svg);
            loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
            postDelayed(watchdog, PAGE_TIMEOUT_MS);
        } catch (Throwable error) {
            report("Erreur Mushaf page " + page + " : " + safeMessage(error));
        }
    }

    public void setMask(int maskPercent) {
        lastMask = maskPercent;
        runWhenReady(() -> evaluateJavascript(
            "window.HifzReader&&window.HifzReader.setMask(" + Math.max(0, Math.min(100, maskPercent)) + ");", null));
        eink.mask(this);
    }

    public void setSelection(List<VerseRef> selection, List<String> lineIds) {
        lastSelection = selection;
        lastLineIds = lineIds;
        JSONArray verses = new JSONArray();
        for (VerseRef ref : selection) verses.put(ref.toString());
        JSONArray lines = new JSONArray();
        for (String id : lineIds) lines.put(id);
        runWhenReady(() -> evaluateJavascript(
            "window.HifzReader&&window.HifzReader.setSelection(" + verses + "," + lines + ");", null));
    }

    public void localCounterChanged() { eink.local(this); }
    public void cycleCompleted() { eink.cycleCompleted(this, prefs); }

    public void destroySafely() {
        removeCallbacks(watchdog);
        pending = null;
        listener = null;
        removeJavascriptInterface("HifzNative");
        stopLoading();
        if (getParent() instanceof ViewGroup) ((ViewGroup) getParent()).removeView(this);
        destroy();
    }

    private void runWhenReady(Runnable action) { if (ready) action.run(); else pending = action; }

    private String readAssetText(String path) throws Exception {
        try (InputStream input = getContext().getAssets().open(path)) { return readUtf8(input); }
    }

    private String readPageSvg(int page) throws Exception {
        if (page < 1 || page > 604) throw new IllegalArgumentException("page outside 1..604");
        String path = String.format(java.util.Locale.ROOT, "mushaf/hafs/kfqc/svg-br/%03d.svg.br", page);
        try (InputStream raw = getContext().getAssets().open(path); BrotliInputStream input = new BrotliInputStream(raw)) {
            String svg = readUtf8(input);
            if (!svg.contains("<svg") || !svg.contains("</svg>")) throw new IllegalStateException("invalid SVG payload");
            return svg;
        }
    }

    private synchronized String readPageGeometry(int page) throws Exception {
        if (geometryPages == null) {
            JSONObject root = new JSONObject(readAssetText("reader109/geometry.json"));
            geometryPages = root.getJSONObject("pages");
        }
        JSONObject pageObject = geometryPages.optJSONObject(String.valueOf(page));
        if (pageObject == null) throw new IllegalStateException("geometry missing for page " + page);
        return pageObject.toString();
    }

    private static String readUtf8(InputStream input) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int count;
        while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
        return new String(output.toByteArray(), StandardCharsets.UTF_8);
    }

    private static String safeMessage(Throwable error) {
        String message = error.getMessage();
        return message == null || message.trim().isEmpty() ? error.getClass().getSimpleName() : message;
    }

    private void report(String message) { post(() -> { if (listener != null) listener.onError(message); }); }

    private final class Bridge {
        @JavascriptInterface public void ready() {
            post(() -> {
                ready = true;
                if (pending != null) { Runnable r = pending; pending = null; r.run(); }
            });
        }

        @JavascriptInterface public void verseTap(int surah, int ayah) {
            post(() -> {
                if (listener == null) return;
                try { listener.onVerseTap(new VerseRef(surah, ayah)); }
                catch (IllegalArgumentException invalidVerse) { report("Verset invalide ignoré : " + surah + ":" + ayah); }
            });
        }

        @JavascriptInterface public void surfaceTap() { post(() -> { if (listener != null) listener.onSurfaceTap(); }); }
        @JavascriptInterface public void error(String message) { report(message); }
        @JavascriptInterface public void pageShown(int page) {
            post(() -> {
                if (page != requestedPage) return;
                pageShown = true;
                removeCallbacks(watchdog);
                setContentDescription("Mushaf page " + page);
                eink.page(MushafView.this, prefs);
                if (listener != null) listener.onPageShown(page);
            });
        }
    }
}
