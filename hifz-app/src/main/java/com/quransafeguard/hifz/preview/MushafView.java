package com.quransafeguard.hifz.preview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.webkit.JavascriptInterface;
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
 * page locally, injects that exact SVG directly into an in-memory HTML document, and the local
 * reader script only handles verse taps / selection / masks. There is no pseudo-network origin,
 * no runtime download and no INTERNET permission.
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
    private static final String MUSHAF_TAG = "<div id=\"mushaf\"></div>";

    private Listener listener;
    private boolean ready;
    private Runnable pending;
    private JSONObject geometryPages;
    private final HifzPrefs prefs;
    private final EinkController eink = new EinkController();

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
        });
    }

    public void setListener(Listener value) {
        listener = value;
        if (value != null && !ready) {
            ready = true;
            post(value::onReady);
        }
    }

    public void show(int page, List<VerseRef> selection, List<String> lineIds, int maskPercent) {
        try {
            String html = readAssetText("hifzreader/index.html");
            String javascript = readAssetText("hifzreader/reader.js");
            String svg = readPageSvg(page);
            String geometry = readPageGeometry(page);
            if (!html.contains(SCRIPT_TAG) || !html.contains(MUSHAF_TAG)) {
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
            html = html
                .replace("script-src 'self';", "script-src 'nonce-" + INLINE_NONCE + "';")
                .replace("connect-src 'self'", "connect-src 'none'")
                .replace(MUSHAF_TAG, "<div id=\"mushaf\">" + svg + "</div>")
                .replace(SCRIPT_TAG, inline);
            loadData(html, "text/html", "UTF-8");
        } catch (Throwable error) {
            report("Erreur Mushaf page " + page + ": " + safeMessage(error));
        }
    }

    public void setMask(int maskPercent) {
        runWhenReady(() -> evaluateJavascript(
            "window.HifzReader&&window.HifzReader.setMask(" + Math.max(0, Math.min(100, maskPercent)) + ");",
            null
        ));
        eink.mask(this);
    }

    public void setSelection(List<VerseRef> selection, List<String> lineIds) {
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
        removeJavascriptInterface("HifzNative");
        stopLoading();
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
        String path = String.format("mushaf/hafs/kfqc/svg-br/%03d.svg.br", page);
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
            post(() -> { if (listener != null) listener.onVerseTap(new VerseRef(surah, ayah)); });
        }

        @JavascriptInterface public void error(String message) { report(message); }

        @JavascriptInterface public void pageShown(int page) {
            post(() -> {
                setContentDescription("Mushaf page " + page);
                eink.page(MushafView.this, prefs);
                if (listener != null) listener.onPageShown(page);
            });
        }
    }
}
