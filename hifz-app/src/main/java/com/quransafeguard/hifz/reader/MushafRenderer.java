package com.quransafeguard.hifz.reader;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.LruCache;
import android.view.View;

import com.caverock.androidsvg.SVG;
import com.quransafeguard.hifz.data.MushafRepository;

import java.io.ByteArrayInputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/** Native SVG Mushaf renderer. No WebView, JavaScript, URL interception or network stack. */
public final class MushafRenderer extends View implements AutoCloseable {
    public interface Listener {
        void onPageChanged(int page);
        void onError(int page, Throwable error);
    }

    /** User-requested page loads never wait behind speculative prefetch work. */
    private final ExecutorService loader = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "quran-hifz-mushaf-loader");
        t.setDaemon(true);
        return t;
    });
    private final ExecutorService prefetcher = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "quran-hifz-mushaf-prefetch");
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        return t;
    });
    private final AtomicInteger generation = new AtomicInteger();
    private final LruCache<Integer, SVG> cache = new LruCache<>(5);
    private final MushafRepository repository;
    private final RectF renderedContent = new RectF();
    private final RectF documentViewBox = new RectF();

    private Listener listener;
    private SVG document;
    private int currentPage = MushafRepository.FIRST_PAGE;
    private boolean closed;

    public MushafRenderer(Context context) {
        this(context, null);
    }

    public MushafRenderer(Context context, AttributeSet attrs) {
        super(context, attrs);
        repository = new MushafRepository(context);
        setBackgroundColor(Color.WHITE);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public boolean isPageBundled(int page) {
        if (page < MushafRepository.FIRST_PAGE || page > MushafRepository.LAST_PAGE) return false;
        return repository.isBundled(page);
    }

    public RectF getRenderedContentRect() {
        return new RectF(renderedContent);
    }

    public RectF getDocumentViewBox() {
        return new RectF(documentViewBox);
    }

    /** Converts a touch in this View to the original SVG document coordinate space. */
    public PointF mapViewToDocument(float viewX, float viewY) {
        if (document == null || renderedContent.isEmpty() || documentViewBox.isEmpty()) return null;
        if (!renderedContent.contains(viewX, viewY)) return null;
        float x = documentViewBox.left
            + ((viewX - renderedContent.left) / renderedContent.width()) * documentViewBox.width();
        float y = documentViewBox.top
            + ((viewY - renderedContent.top) / renderedContent.height()) * documentViewBox.height();
        return new PointF(x, y);
    }

    /** Converts a rectangle in SVG document coordinates to this View's pixel coordinates. */
    public RectF mapDocumentToView(RectF source) {
        if (document == null || renderedContent.isEmpty() || documentViewBox.isEmpty() || source == null) {
            return null;
        }
        float left = renderedContent.left
            + ((source.left - documentViewBox.left) / documentViewBox.width()) * renderedContent.width();
        float top = renderedContent.top
            + ((source.top - documentViewBox.top) / documentViewBox.height()) * renderedContent.height();
        float right = renderedContent.left
            + ((source.right - documentViewBox.left) / documentViewBox.width()) * renderedContent.width();
        float bottom = renderedContent.top
            + ((source.bottom - documentViewBox.top) / documentViewBox.height()) * renderedContent.height();
        return new RectF(left, top, right, bottom);
    }

    public void showPage(int page) {
        if (closed) throw new IllegalStateException("MushafRenderer is closed");
        if (page < MushafRepository.FIRST_PAGE || page > MushafRepository.LAST_PAGE) {
            throw new IllegalArgumentException("Mushaf page must be 1..604");
        }

        // Every navigation invalidates any older in-flight load, even when this page is cached.
        final int ticket = generation.incrementAndGet();
        SVG cached = cache.get(page);
        if (cached != null) {
            applyDocument(page, cached);
            return;
        }

        loader.execute(() -> {
            try {
                SVG parsed = loadSvg(page);
                post(() -> {
                    if (closed || ticket != generation.get()) return;
                    cache.put(page, parsed);
                    applyDocument(page, parsed);
                });
            } catch (Throwable error) {
                post(() -> {
                    if (closed || ticket != generation.get()) return;
                    if (listener != null) listener.onError(page, error);
                });
            }
        });
    }

    private SVG loadSvg(int page) throws Exception {
        MushafRepository.Page source = repository.load(page);
        SVG parsed = SVG.getFromInputStream(new ByteArrayInputStream(source.svgUtf8));
        RectF box = parsed.getDocumentViewBox();
        if (box == null || box.width() <= 0f || box.height() <= 0f) {
            throw new IllegalArgumentException("Mushaf page has no valid viewBox: " + page);
        }
        return parsed;
    }

    private void applyDocument(int page, SVG parsed) {
        document = parsed;
        RectF box = parsed.getDocumentViewBox();
        documentViewBox.set(box);
        currentPage = page;
        updateRenderedContentRect();
        invalidate();
        if (listener != null) listener.onPageChanged(page);
        prefetchAdjacent(page);
    }

    /**
     * Warm the two likely Arabic-book navigation targets without touching the View or triggering
     * E-Ink refreshes. Cache work runs independently from the user-requested loader.
     */
    private void prefetchAdjacent(int page) {
        prefetchPage(page + 1);
        prefetchPage(page - 1);
    }

    private void prefetchPage(int page) {
        if (closed || page < MushafRepository.FIRST_PAGE || page > MushafRepository.LAST_PAGE) return;
        if (cache.get(page) != null) return;
        prefetcher.execute(() -> {
            if (closed || cache.get(page) != null) return;
            try {
                SVG parsed = loadSvg(page);
                if (!closed && cache.get(page) == null) cache.put(page, parsed);
            } catch (Throwable ignored) {
                // Speculative work must never surface an error or affect the requested page.
            }
        });
    }

    public void nextPage() {
        int next = currentPage + 1;
        if (next <= MushafRepository.LAST_PAGE && isPageBundled(next)) showPage(next);
    }

    public void previousPage() {
        int previous = currentPage - 1;
        if (previous >= MushafRepository.FIRST_PAGE && isPageBundled(previous)) showPage(previous);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateRenderedContentRect();
    }

    private void updateRenderedContentRect() {
        if (document == null || getWidth() <= 0 || getHeight() <= 0 || documentViewBox.isEmpty()) {
            renderedContent.setEmpty();
            return;
        }
        float scale = Math.min(getWidth() / documentViewBox.width(), getHeight() / documentViewBox.height());
        float width = documentViewBox.width() * scale;
        float height = documentViewBox.height() * scale;
        float left = (getWidth() - width) * 0.5f;
        float top = (getHeight() - height) * 0.5f;
        renderedContent.set(left, top, left + width, top + height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.WHITE);
        SVG svg = document;
        if (svg == null || renderedContent.isEmpty()) return;
        // Explicit fit-center: never stretch/reflow the canonical Mushaf page.
        svg.renderToCanvas(canvas, renderedContent);
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        generation.incrementAndGet();
        loader.shutdownNow();
        prefetcher.shutdownNow();
        cache.evictAll();
        document = null;
        renderedContent.setEmpty();
        documentViewBox.setEmpty();
    }
}
