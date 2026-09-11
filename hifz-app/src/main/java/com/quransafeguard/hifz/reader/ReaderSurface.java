package com.quransafeguard.hifz.reader;

import android.content.Context;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import com.quransafeguard.hifz.data.GeometryRepository;
import com.quransafeguard.hifz.data.LineGeometryRepository;
import com.quransafeguard.hifz.eink.BooxEinkController;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/** One canonical touch/geometry surface shared by Reading, Free Memorization and Hifz sessions. */
public final class ReaderSurface extends FrameLayout implements AutoCloseable {
    public interface Listener {
        void onPageChanged(int page);
        void onVerseTapped(GeometryRepository.AyahRegion verse);
        void onPageSwipe(int delta);
        void onError(Throwable error);
    }

    private final MushafRenderer renderer;
    private final MemorizationOverlayView overlay;
    private final GeometryRepository geometry;
    private final BooxEinkController eink = new BooxEinkController();
    private final ExecutorService loader = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "quran-hifz-geometry-loader");
        t.setDaemon(true);
        return t;
    });
    private final AtomicInteger generation = new AtomicInteger();

    private volatile List<GeometryRepository.AyahRegion> currentRegions = Collections.emptyList();
    private Listener listener;
    private float downX, downY;
    private long downTime;
    private boolean closed;
    private boolean composingPageChange;

    public ReaderSurface(Context context) { this(context, null); }
    public ReaderSurface(Context context, AttributeSet attrs) {
        super(context, attrs);
        geometry = new GeometryRepository(context);
        renderer = new MushafRenderer(context);
        overlay = new MemorizationOverlayView(context);
        overlay.bindRenderer(renderer);
        addView(renderer, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        addView(overlay, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        eink.attachReader(renderer);
        eink.attachOverlay(overlay);

        renderer.setListener(new MushafRenderer.Listener() {
            @Override public void onPageChanged(int page) {
                // Never allow a tap on the new page to resolve against the previous page geometry.
                currentRegions = Collections.emptyList();
                // Let the owner update the mask/focus synchronously, then refresh the composite once.
                composingPageChange = true;
                try {
                    loadPageRegions(page);
                    if (listener != null) listener.onPageChanged(page);
                    eink.pageChanged(ReaderSurface.this);
                } finally {
                    composingPageChange = false;
                }
            }
            @Override public void onError(int page, Throwable error) {
                if (listener != null) listener.onError(error);
            }
        });

        setOnTouchListener((v, event) -> handleTouch(event));
        overlay.setClickable(false);
        overlay.setFocusable(false);
    }

    public void setListener(Listener listener) { this.listener = listener; }
    public int getCurrentPage() { return renderer.getCurrentPage(); }
    public MushafRenderer renderer() { return renderer; }

    public void showPage(int page) { renderer.showPage(page); }

    public void setMemorizationState(List<LineGeometryRepository.PageLine> lines,
                                     GeometryRepository.AyahRegion focus,
                                     int maskPercent) {
        overlay.setSelection(lines, focus);
        overlay.setMaskPercent(maskPercent);
        if (!composingPageChange) eink.localChanged(overlay);
    }

    public void clearMemorizationState() {
        overlay.setSelection(Collections.emptyList(), null);
        overlay.setMaskPercent(0);
        if (!composingPageChange) eink.localChanged(overlay);
    }

    public void revealTemporarily(boolean reveal) {
        overlay.setRevealAll(reveal);
        if (!composingPageChange) eink.localChanged(overlay);
    }

    /** Full E-Ink cleanup after a temporary window/overlay such as the floating Tafsir closes. */
    public void cleanupGhosting() {
        if (!closed) eink.fullClean(this);
    }

    public int getMaskPercent() { return overlay.getMaskPercent(); }

    private void loadPageRegions(int page) {
        final int ticket = generation.incrementAndGet();
        loader.execute(() -> {
            try {
                List<GeometryRepository.AyahRegion> regions = geometry.loadPage(page);
                post(() -> {
                    if (closed || ticket != generation.get() || page != renderer.getCurrentPage()) return;
                    currentRegions = regions;
                });
            } catch (Throwable error) {
                post(() -> { if (!closed && ticket == generation.get() && listener != null) listener.onError(error); });
            }
        });
    }

    private boolean handleTouch(MotionEvent event) {
        if (closed) return false;
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            downX = event.getX();
            downY = event.getY();
            downTime = event.getEventTime();
            return true;
        }
        if (event.getActionMasked() == MotionEvent.ACTION_UP) {
            float dx = event.getX() - downX;
            float dy = event.getY() - downY;
            long elapsed = event.getEventTime() - downTime;
            float density = getResources().getDisplayMetrics().density;
            float swipeThreshold = 64f * density;
            if (Math.abs(dx) >= swipeThreshold && Math.abs(dx) > Math.abs(dy) * 1.25f && elapsed < 900L) {
                if (listener != null) listener.onPageSwipe(PageTurnPolicy.deltaForHorizontalSwipe(dx));
                return true;
            }
            float tapSlop = 18f * density;
            if (Math.abs(dx) <= tapSlop && Math.abs(dy) <= tapSlop && elapsed < 700L) {
                dispatchVerseTap(event.getX(), event.getY());
                return true;
            }
            return true;
        }
        return true;
    }

    private void dispatchVerseTap(float x, float y) {
        PointF doc = renderer.mapViewToDocument(x, y);
        if (doc == null) return;
        for (GeometryRepository.AyahRegion region : currentRegions) {
            if (region.contains(doc.x, doc.y)) {
                if (listener != null) listener.onVerseTapped(region);
                return;
            }
        }
    }

    @Override public void close() {
        if (closed) return;
        closed = true;
        generation.incrementAndGet();
        loader.shutdownNow();
        renderer.close();
        currentRegions = Collections.emptyList();
    }
}
