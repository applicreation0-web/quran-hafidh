package com.quransafeguard.hifz.preview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.quransafeguard.hifz.core.VerseRef;

import org.brotli.dec.BrotliInputStream;
import org.json.JSONArray;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

/** Offline canonical Mushaf WebView. Network and file access are disabled by product boundary. */
public final class MushafView extends WebView {
    public interface Listener {
        void onVerseTap(VerseRef verse);
        void onReady();
        void onError(String message);
        void onPageShown(int page);
    }

    private Listener listener;
    private boolean ready;
    private Runnable pending;
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
        // The reader uses an HTTPS-shaped, fully intercepted pseudo-origin. Blocking
        // network loads here prevents WebView from reaching shouldInterceptRequest()
        // on some Android/WebView versions and leaves the reader completely white.
        // Hifz intentionally has no INTERNET permission, CSP is self-only, and every
        // non quran-hifz.local request below is denied, so this does not enable network IO.
        s.setBlockNetworkLoads(false);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);
        addJavascriptInterface(new Bridge(), "HifzNative");
        setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) { return true; }
            @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                if (!"https".equals(uri.getScheme()) || !"quran-hifz.local".equals(uri.getHost())) return denied();
                String path = uri.getPath() == null ? "" : uri.getPath().replaceFirst("^/", "");
                try {
                    if (path.equals("hifzreader/index.html")) return asset("text/html", path);
                    if (path.equals("hifzreader/reader.js")) return asset("application/javascript", path);
                    if (path.equals("reader109/geometry.json")) return asset("application/json", path);
                    if (path.matches("page/[0-9]{1,3}")) {
                        int page = Integer.parseInt(path.substring(path.indexOf('/') + 1));
                        if (page < 1 || page > 604) return denied();
                        InputStream raw = getContext().getAssets().open(String.format("mushaf/hafs/kfqc/svg-br/%03d.svg.br", page));
                        return new WebResourceResponse("image/svg+xml", "UTF-8", new BrotliInputStream(raw));
                    }
                } catch (Throwable error) {
                    post(() -> { if (listener != null) listener.onError("Erreur Mushaf: " + error.getMessage()); });
                }
                return denied();
            }
        });
        loadUrl("https://quran-hifz.local/hifzreader/index.html");
    }

    public void setListener(Listener value) {
        listener = value;
        // loadUrl() starts in the constructor. If the local reader becomes ready
        // before the Activity installs its listener, replay readiness so page 1
        // cannot remain an empty but otherwise valid WebView.
        if (ready && value != null) post(value::onReady);
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

    private WebResourceResponse asset(String mime, String path) throws Exception {
        return new WebResourceResponse(mime, "UTF-8", getContext().getAssets().open(path));
    }

    private WebResourceResponse denied() {
        return new WebResourceResponse("text/plain", "UTF-8", 403, "Blocked", Collections.emptyMap(), new ByteArrayInputStream(new byte[0]));
    }

    private final class Bridge {
        @JavascriptInterface public void ready() {
            post(() -> {
                ready = true;
                if (listener != null) listener.onReady();
                if (pending != null) { Runnable r = pending; pending = null; r.run(); }
            });
        }
        @JavascriptInterface public void verseTap(int surah, int ayah) {
            post(() -> { if (listener != null) listener.onVerseTap(new VerseRef(surah, ayah)); });
        }
        @JavascriptInterface public void error(String message) {
            post(() -> { if (listener != null) listener.onError(message); });
        }
        @JavascriptInterface public void pageShown(int page) {
            post(() -> {
                eink.page(MushafView.this, prefs);
                if (listener != null) listener.onPageShown(page);
            });
        }
    }
}
