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
 * Offline canonical Mushaf renderer.
 *
 * The WebView never fetches HTTP(S), file:// or content:// resources. HTML and JavaScript are
 * loaded from packaged assets into memory, while the canonical Brotli SVG and per-page geometry
 * are supplied through the private JavaScript bridge. Quran Hifz therefore keeps no INTERNET
 * permission and no broad file/content access.
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

    private Listener listener;
    private boolean ready;
    private Runnable pending;
    private String bootstrapError;
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
                // The reader itself is injected in-memory. Reject every user/script navigation;
                // there are no legitimate navigations in Quran Hifz.
                return true;
            }
        });

        try {
            String html = readAssetText("hifzreader/index.html");
            String javascript = readAssetText("hifzreader/reader.js");
            if (!html.contains(SCRIPT_TAG)) {
                throw new IllegalStateException("reader.js bootstrap tag missing");
            }
            html = html
                .replace("script-src 'self';", "script-src 'nonce-" + INLINE_NONCE + "';")
                .replace("connect-src 'self'", "connect-src 'none'")
                .replace(SCRIPT_TAG, "<script nonce=\"" + INLINE_NONCE + "\">" + javascript + "</script>");
            loadDataWithBaseURL(
                "https://quran-hifz.local/hifzreader/",
                html,
                "text/html",
                "UTF-8",
                null
            );
        } catch (Throwable error) {
            bootstrapError = "Initialisation Mushaf impossible: " + safeMessage(error);
            String fallback = "<!doctype html><html><body style='background:#fff;color:#000'>" +
                "<p>Erreur de chargement du Mushaf.</p></body></html>";
            loadData(fallback, "text/html", "UTF-8");
        }
    }

    public void setListener(Listener value) {
        listener = value;
        if (value == null) return;
        if (bootstrapError != null) {
            String message = bootstrapError;
            post(() -> value.onError(message));
        }
        // The in-memory page may finish before Activity construction installs its listener.
        // Replay readiness so page 1 can never remain an empty, otherwise-valid WebView.
        if (ready) post(value::onReady);
    }

    public void show(int page, List<VerseRef> selection, List<String> lineIds, int maskPercent) {
        JSONArray verses = new JSONArray();
        for (VerseRef ref : selection) verses.put(ref.toString());
        JSONArray lines = new JSONArray();
        for (String id : lineIds) lines.put(id);
        String script = "window.HifzReader&&window.HifzReader.show(" + page + "," + verses + "," + lines + "," +
            Math.max(0, Math.min(100, maskPercent)) + "," + eink.isEink(prefs) + ");";
        runWhenReady(() -> evaluateJavascript(script, null));
    }

    public void setMask(int maskPercent) {
        String script = "window.HifzReader&&window.HifzReader.setMask(" + Math.max(0, Math.min(100, maskPercent)) + ");";
        runWhenReady(() -> evaluateJavascript(script, null));
        eink.mask(this);
    }

    public void setSelection(List<VerseRef> selection, List<String> lineIds) {
        JSONArray verses = new JSONArray();
        for (VerseRef ref : selection) verses.put(ref.toString());
        JSONArray lines = new JSONArray();
        for (String id : lineIds) lines.put(id);
        runWhenReady(() -> evaluateJavascript("window.HifzReader&&window.HifzReader.setSelection(" + verses + "," + lines + ");", null));
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
            return readUtf8(input);
        }
    }

    private synchronized String readPageGeometry(int page) throws Exception {
        if (page < 1 || page > 604) throw new IllegalArgumentException("page outside 1..604");
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

    private void reportBridgeError(String prefix, Throwable error) {
        String message = prefix + ": " + safeMessage(error);
        post(() -> { if (listener != null) listener.onError(message); });
    }

    private final class Bridge {
        @JavascriptInterface public void ready() {
            post(() -> {
                ready = true;
                if (listener != null) listener.onReady();
                if (pending != null) { Runnable r = pending; pending = null; r.run(); }
            });
        }

        @JavascriptInterface public String pageSvg(int page) {
            try {
                return readPageSvg(page);
            } catch (Throwable error) {
                reportBridgeError("Erreur Mushaf page " + page, error);
                return "";
            }
        }

        @JavascriptInterface public String pageGeometry(int page) {
            try {
                return readPageGeometry(page);
            } catch (Throwable error) {
                reportBridgeError("Erreur géométrie page " + page, error);
                return "";
            }
        }

        @JavascriptInterface public void verseTap(int surah, int ayah) {
            post(() -> { if (listener != null) listener.onVerseTap(new VerseRef(surah, ayah)); });
        }

        @JavascriptInterface public void error(String message) {
            post(() -> { if (listener != null) listener.onError(message); });
        }

        @JavascriptInterface public void pageShown(int page) {
            post(() -> {
                // Runtime tests use this semantic marker. It is emitted only after reader.js
                // has parsed and injected the canonical SVG, so a WebView error page cannot
                // masquerade as a successful Mushaf render.
                setContentDescription("Mushaf page " + page);
                eink.page(MushafView.this, prefs);
                if (listener != null) listener.onPageShown(page);
            });
        }
    }
}
