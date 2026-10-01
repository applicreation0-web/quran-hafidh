package com.quransafeguard.hifz.preview;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.SystemClock;
import android.view.MotionEvent;
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
import java.util.Collections;
import java.util.List;

/** Offline Mushaf renderer using the exact packaged KFQC SVG page. */
public final class MushafView extends WebView {
    public interface Listener {
        void onVerseTap(VerseRef verse);
        void onReady();
        void onError(String message);
        void onPageShown(int page);
        default void onSurfaceTap() {}
        /** Arabic-book semantics: +1 means next canonical page and is triggered by a right swipe. */
        default void onPageSwipe(int delta) {}
    }

    private static final String INLINE_NONCE = "hifz-local";
    private static final String SCRIPT_TAG = "<script src=\"reader.js\"></script>";
    private static final String SVG_SLOT = "<!--MUSHAF_SVG-->";
    private static final long MIN_PAGE_TIMEOUT_MS = 8_000L;
    static final long MAX_PAGE_TIMEOUT_MS = 20_000L;

    private Listener listener;
    private boolean listenerNotified;
    private boolean ready;
    private Runnable pending;
    private final HifzPrefs prefs;
    private final EinkController eink = new EinkController();
    private String maskEntropy;

    private int requestedPage = 0;
    private boolean pageShown;
    private boolean retried;
    private List<VerseRef> lastSelection;
    private List<String> lastLineIds;
    private int lastMask;
    private boolean lastStrictLineFocus;
    private List<VerseRef> currentHighlights = Collections.emptyList();
    private String landmarkStartLineId;
    private String landmarkEndLineId;
    private boolean maskFollowsSelection = true;
    private float touchDownX, touchDownY;
    private long loadStartedAtMs;
    private long observedRenderMs;

    private final Runnable watchdog = new Runnable() {
        @Override public void run() {
            if (pageShown || requestedPage == 0) return;
            if (!retried) {
                retried = true;
                load(requestedPage, lastSelection, lastLineIds, lastMask);
            } else {
                showFailure("Page " + requestedPage + " non affichée (délai dépassé). Revenez puis rouvrez la lecture.");
            }
        }
    };

    @SuppressLint("SetJavaScriptEnabled")
    public MushafView(Context context) {
        super(context);
        prefs = new HifzPrefs(context);
        maskEntropy = prefs.maskEntropyFor("reader_default");
        setBackgroundColor(android.graphics.Color.rgb(250, 248, 242));
        WebSettings s = getSettings();
        s.setJavaScriptEnabled(true);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setDomStorageEnabled(false);
        s.setBlockNetworkLoads(true);
        s.setBuiltInZoomControls(false);
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

    static long timeoutAfterObservedRender(long observedMs) {
        if (observedMs <= 0L) return MIN_PAGE_TIMEOUT_MS;
        long tripled = observedMs > MAX_PAGE_TIMEOUT_MS / 3L
            ? MAX_PAGE_TIMEOUT_MS
            : observedMs * 3L;
        return Math.min(MAX_PAGE_TIMEOUT_MS, Math.max(MIN_PAGE_TIMEOUT_MS, tripled));
    }

    static long updateObservedRender(long previousObservedMs, long renderMs) {
        if (renderMs <= 0L) return Math.max(0L, Math.min(MAX_PAGE_TIMEOUT_MS, previousObservedMs));
        return Math.min(MAX_PAGE_TIMEOUT_MS, renderMs);
    }

    public void setMaskEntropy(String value) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException("mask entropy required");
        maskEntropy = value;
    }

    public void setListener(Listener value) {
        listener = value;
        if (value != null && !listenerNotified) {
            listenerNotified = true;
            post(value::onReady);
        }
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            touchDownX = event.getX();
            touchDownY = event.getY();
        } else if (event.getActionMasked() == MotionEvent.ACTION_UP) {
            float dx = event.getX() - touchDownX;
            float dy = event.getY() - touchDownY;
            float threshold = 60f * getResources().getDisplayMetrics().density;
            if (Math.abs(dx) >= threshold && Math.abs(dx) > Math.abs(dy) * 1.35f) {
                MotionEvent cancel = MotionEvent.obtain(event);
                cancel.setAction(MotionEvent.ACTION_CANCEL);
                super.onTouchEvent(cancel);
                cancel.recycle();
                if (listener != null) listener.onPageSwipe(dx > 0 ? 1 : -1);
                return true;
            }
        }
        return super.onTouchEvent(event);
    }

    public void show(int page, List<VerseRef> selection, List<String> lineIds, int maskPercent) {
        show(page, selection, lineIds, maskPercent, false);
    }

    /**
     * A fractionated Stabilisation block's lineIds can include a boundary verse whose own
     * physical lines straddle the split (CorpusLinePolicy assigns each line to its earliest
     * verse, so a verse can start in one block and continue into lines owned by the next).
     * The default whole-verse shading then greys out lines beyond the block's real 6-8 line
     * working set. strictLineFocus=true shades exactly lineIds instead, like J10's view.
     */
    public void show(int page, List<VerseRef> selection, List<String> lineIds, int maskPercent, boolean strictLineFocus) {
        lastStrictLineFocus = strictLineFocus;
        retried = false;
        load(page, selection, lineIds, maskPercent);
    }

    /** J10-only view: shade exactly the requested physical lines, never whole verse polygons. */
    public void showLineFocus(int page, List<VerseRef> selection, List<String> lineIds) {
        lastStrictLineFocus = true;
        retried = false;
        load(page, selection, lineIds, 0);
    }

    private void load(int page, List<VerseRef> selection, List<String> lineIds, int maskPercent) {
        removeCallbacks(watchdog);
        requestedPage = page;
        loadStartedAtMs = SystemClock.elapsedRealtime();
        pageShown = false;
        ready = false;
        pending = null;
        lastSelection = selection;
        lastLineIds = lineIds;
        lastMask = maskPercent;
        boolean strictLineFocus = lastStrictLineFocus;
        try {
            String html = readAssetText("hifzreader/index.html");
            String javascript = readAssetText("hifzreader/reader.js");
            String svg = readPageSvg(page);
            String geometry = lineIds.isEmpty() ? null : GeometryRepository.get(getContext()).pageGeometryJson(page);
            if (!html.contains(SCRIPT_TAG) || !html.contains(SVG_SLOT)) throw new IllegalStateException("reader template incomplete");
            if (!html.contains("'nonce-" + INLINE_NONCE + "'")) throw new IllegalStateException("reader CSP nonce missing");
            JSONArray verses = new JSONArray();
            for (VerseRef ref : selection) verses.put(ref.toString());
            JSONArray lines = new JSONArray();
            for (String id : lineIds) lines.put(String.valueOf(id));
            JSONArray highlights = new JSONArray();
            for (VerseRef ref : currentHighlights) highlights.put(ref.toString());
            JSONObject boot = new JSONObject()
                .put("page", page)
                .put("selection", verses)
                .put("lines", lines)
                .put("mask", Math.max(0, Math.min(100, maskPercent)))
                .put("maskEntropy", maskEntropy)
                .put("eink", eink.isEink(prefs))
                .put("strictLineFocus", strictLineFocus)
                .put("highlights", highlights)
                .put("landmarkStart", landmarkStartLineId)
                .put("landmarkEnd", landmarkEndLineId)
                .put("maskFollowsSelection", maskFollowsSelection)
                .put("geometry", geometry == null ? JSONObject.NULL : new JSONObject(geometry));
            String inline = "<script nonce=\"" + INLINE_NONCE + "\">window.HIFZ_BOOT=" +
                boot.toString().replace("</", "<\\/") + ";\n" + javascript + "</script>";
            html = html.replace(SCRIPT_TAG, inline).replace(SVG_SLOT, svg);
            loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
            postDelayed(watchdog, timeoutAfterObservedRender(observedRenderMs));
        } catch (Throwable error) {
            showFailure("Erreur Mushaf page " + page + " : " + safeMessage(error));
        }
    }

    public void setMask(int maskPercent) {
        lastMask = maskPercent;
        runWhenReady(() -> evaluateJavascript(
            "window.HifzReader&&window.HifzReader.setMask(" + Math.max(0, Math.min(100, maskPercent)) + ");",
            ignored -> post(() -> eink.mask(this, prefs))));
    }

    public void setSelection(List<VerseRef> selection, List<String> lineIds) {
        lastSelection = selection;
        lastLineIds = lineIds;
        JSONArray verses = new JSONArray();
        for (VerseRef ref : selection) verses.put(ref.toString());
        JSONArray lines = new JSONArray();
        for (String id : lineIds) lines.put(String.valueOf(id));
        final String geometry;
        try {
            geometry = lineIds.isEmpty() ? null : GeometryRepository.get(getContext()).pageGeometryJson(requestedPage);
        } catch (Throwable error) {
            report("Géométrie de sélection indisponible : " + safeMessage(error));
            return;
        }
        runWhenReady(() -> {
            StringBuilder script = new StringBuilder("window.HifzReader&&(");
            if (geometry != null) script.append("window.HifzReader.setGeometry(").append(geometry).append("),");
            script.append("window.HifzReader.setSelection(").append(verses).append(',').append(lines).append("));");
            evaluateJavascript(script.toString(), ignored -> post(() -> eink.local(this, prefs)));
        });
    }

    /**
     * Personal weak-spot flags: a light, thin outline drawn on top of any mask (never a filled
     * shade, to minimize E-Ink ink coverage/ghosting risk). Persists across page loads on this
     * view instance like maskEntropy, so a page swipe redraws it without the caller resending it.
     */
    public void setHighlightVerses(List<VerseRef> verses) {
        currentHighlights = verses == null ? Collections.emptyList() : verses;
        JSONArray array = new JSONArray();
        for (VerseRef ref : currentHighlights) array.put(ref.toString());
        runWhenReady(() -> evaluateJavascript(
            "window.HifzReader&&window.HifzReader.setHighlights(" + array.toString() + ");",
            ignored -> post(() -> eink.local(this, prefs))));
    }

    /**
     * Révision active synchronization landmarks: half of the flagged start/end line (by cell
     * count, reading-order aware) is excluded from masking entirely so it always stays visible,
     * the other half of that same line still masks normally. Pass null for either id to clear it
     * (e.g. a single-line page has no separate start/end).
     */
    public void setLandmarkLines(String startLineId, String endLineId) {
        landmarkStartLineId = startLineId;
        landmarkEndLineId = endLineId;
        String startArg = startLineId == null ? "null" : JSONObject.quote(startLineId);
        String endArg = endLineId == null ? "null" : JSONObject.quote(endLineId);
        runWhenReady(() -> evaluateJavascript(
            "window.HifzReader&&window.HifzReader.setLandmarks(" + startArg + "," + endArg + ");",
            ignored -> post(() -> eink.local(this, prefs))));
    }

    /**
     * Sabqi/Itqan's selection IS the memorization block sharing physical lines with un-selected
     * neighbor verses, so masking must stay clipped to the selected verses' own shapes there
     * (the default, true). Murajaah's selection only flags the last verse actually revised for
     * display — it must not also shrink the mask pool down to that one verse's own shape.
     */
    public void setMaskFollowsSelection(boolean value) {
        maskFollowsSelection = value;
        runWhenReady(() -> evaluateJavascript(
            "window.HifzReader&&window.HifzReader.setMaskFollowsSelection(" + value + ");",
            ignored -> post(() -> eink.local(this, prefs))));
    }

    /** Independent whole-verse audio highlight; it never changes the Hifz selection/mask. */
    public void setAudioVerse(VerseRef verse) {
        String value = verse == null ? "null" : JSONObject.quote(verse.toString());
        runWhenReady(() -> evaluateJavascript(
            "window.HifzReader&&window.HifzReader.setAudioVerse(" + value + ");",
            ignored -> post(() -> eink.audio(this, prefs))));
    }

    /** Runtime recovery path: E-Ink may be changed after the initial WebView boot. */
    public void setEink(boolean enabled) {
        runWhenReady(() -> evaluateJavascript(
            "window.HifzReader&&window.HifzReader.setEink(" + enabled + ");",
            ignored -> post(() -> eink.local(this, prefs))));
    }

    /** Move only if the selected verse would be obscured by the phone Tafsir panel. */
    public void revealSelectionAboveBottomPanel() {
        runWhenReady(() -> evaluateJavascript(
            "window.HifzReader&&window.HifzReader.revealSelection(0.46);",
            ignored -> post(() -> eink.local(this, prefs))));
    }

    public void clearReveal() {
        runWhenReady(() -> evaluateJavascript(
            "window.HifzReader&&window.HifzReader.clearReveal&&window.HifzReader.clearReveal();",
            ignored -> post(() -> eink.local(this, prefs))));
    }

    public void localCounterChanged() { eink.local(this, prefs); }
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

    private static String escapeHtml(String value) {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }

    private void showFailure(String message) {
        post(() -> {
            removeCallbacks(watchdog);
            ready = false;
            pageShown = false;
            pending = null;
            setContentDescription("Erreur Mushaf : " + message);
            String html = "<!doctype html><html><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">" +
                "<style>html,body{margin:0;width:100%;height:100%;background:#faf8f2;color:#121211;font-family:sans-serif}" +
                "body{display:flex;align-items:center;justify-content:center;text-align:center}.box{max-width:28em;padding:24px}" +
                "h2{font-size:18px;margin:0 0 12px}p{font-size:15px;line-height:1.45;margin:0}</style></head>" +
                "<body><div class=\"box\"><h2>Mushaf indisponible</h2><p>" + escapeHtml(message) + "</p></div></body></html>";
            loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
            if (listener != null) listener.onError(message);
        });
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
        @JavascriptInterface public void error(String message) { showFailure("Erreur d’affichage Mushaf : " + message); }
        @JavascriptInterface public void pageShown(int page) {
            post(() -> {
                if (page != requestedPage) return;
                pageShown = true;
                long renderMs = Math.max(1L, SystemClock.elapsedRealtime() - loadStartedAtMs);
                observedRenderMs = updateObservedRender(observedRenderMs, renderMs);
                removeCallbacks(watchdog);
                setContentDescription("Mushaf page " + page);
                eink.page(MushafView.this, prefs);
                if (listener != null) listener.onPageShown(page);
            });
        }
    }
}
