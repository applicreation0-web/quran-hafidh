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

/**
 * Simple offline Mushaf renderer.
 *
 * Every page is already packaged in the APK as %03d.svg.br. Android decompresses the requested
 * page locally, injects that exact SVG into an in-memory HTML document, and the local reader
 * script only handles verse taps / selection / masks. No runtime download, no INTERNET permission.
 *
 * Corrections 2026-09-11 (audit Claude) :
 *  1. The template slot is an explicit HTML comment (<!--MUSHAF_SVG-->). The previous marker
 *     "<div id=\"mushaf\"></div>" did not exist in index.html, so show() always failed.
 *  2. loadDataWithBaseURL() instead of loadData(): loadData() builds a data: URL where '#'
 *     starts a fragment and '%xx' is decoded, which truncated the page at the first CSS colour.
 *  3. "ready" now means "the JavaScript of the CURRENT page is loaded". It is reset on every
 *     show(), so setMask()/setSelection() are queued instead of being silently lost.
 *  4. Watchdog: if the page does not report pageShown in time, retry once, then report a visible
 *     error. A WebView renderer crash no longer kills the app: the Activity is recreated.
 */
public final class MushafView extends WebView {
    public interface Listener {
        void onVerseTap(VerseRef verse);
        void onReady();
        void onError(String message);
        void onPageShown(int page);
    }

    private static final String INLINE_NONCE = "hifz-local";
    private static final String SCRIPT_TAG = "<script src=\"reader.js\"></script>";
    private static final String SVG_SLOT = "<!--MUSHAF_SVG-->";
    private static final long PAGE_TIMEOUT_MS = 4_000L;

    private Listener listener;
    private boolean listenerNotified;
    private boolean ready;          // JavaScript of the current page is loaded
    private Runnable pending;       // latest mask/selection request made while loading
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
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return true;
            }

            @Override public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                // Returning true keeps the app process alive. This WebView instance is now unusable:
                // recreate the screen (page/progress are persisted by the Activities).
                removeCallbacks(watchdog);
                report("Moteur d’affichage redémarré.");
                Context c = getContext();
                if (c instanceof Activity && !((Activity) c).isFinishing()) {
                    post(((Activity) c)::recreate);
                }
                return true;
            }
        });
    }

    /** Notifies onReady() once: the view is attached and show() may be called. */
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
        pending = null; // superseded by the boot values of this page
        lastSelection = selection;
        lastLineIds = lineIds;
        lastMask = maskPercent;
        try {
            String html = readAssetText("hifzreader/index.html");
            String javascript = readAssetText("hifzreader/reader.js");
            String svg = readPageSvg(page);
            String geometry = readPageGeometry(page);
            if (!html.contains(SCRIPT_TAG) || !html.contains(SVG_SLOT)) {
                throw new IllegalStateException("reader template incomplete");
            }
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
            // String.replace(CharSequence, CharSequence) is literal (no regex, no '$' groups).
            html = html
                .replace("script-src 'self';", "script-src 'nonce-" + INLINE_NONCE + "';")
                .replace("connect-src 'self'", "connect-src 'none'")
                .replace(SCRIPT_TAG, inline)
                .replace(SVG_SLOT, svg);
            // No URL parsing of the content: '#', '%' and non-ASCII characters are safe here.
            loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
            postDelayed(watchdog, PAGE_TIMEOUT_MS);
        } catch (Throwable error) {
            report("Erreur Mushaf page " + page + " : " + safeMessage(error));
        }
    }

    public void setMask(int maskPercent) {
        lastMask = maskPercent;
        runWhenReady(() -> evaluateJavascript(
            "window.HifzReader&&window.HifzReader.setMask(" + Math.max(0, Math.min(100, maskPercent)) + ");",
            null
        ));
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
            "window.HifzReader&&window.HifzReader.setSelection(" + verses + "," + lines + ");",
            null
        ));
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

    private void runWhenReady(Runnable action) {
        if (ready) action.run(); else pending = action;
    }

    private String readAssetText(String path) throws Exception {
        try (InputStream input = getContext().getAssets().open(path)) {
            return readUtf8(input);
        }
    }

    private String readPageSvg(int page) throws Exception {
        if (page < 1 || page > 604) throw new IllegalArgumentException("page outside 1..604");
        String path = String.format(java.util.Locale.ROOT, "mushaf/hafs/kfqc/svg-br/%03d.svg.br", page);
        try (InputStream raw = getContext().getAssets().open(path);
             BrotliInputStream input = new BrotliInputStream(raw)) {
            String svg = readUtf8(input);
            if (!svg.contains("<svg") || !svg.contains("</svg>")) {
                throw new IllegalStateException("invalid SVG payload");
            }
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

    private void report(String message) {
        post(() -> { if (listener != null) listener.onError(message); });
    }

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
                try {
                    listener.onVerseTap(new VerseRef(surah, ayah));
                } catch (IllegalArgumentException invalidVerse) {
                    report("Verset invalide ignoré : " + surah + ":" + ayah);
                }
            });
        }

        @JavascriptInterface public void error(String message) { report(message); }

        @JavascriptInterface public void pageShown(int page) {
            post(() -> {
                if (page != requestedPage) return; // stale callback from a previous load
                pageShown = true;
                removeCallbacks(watchdog);
                setContentDescription("Mushaf page " + page);
                eink.page(MushafView.this, prefs);
                if (listener != null) listener.onPageShown(page);
            });
        }
    }
}
