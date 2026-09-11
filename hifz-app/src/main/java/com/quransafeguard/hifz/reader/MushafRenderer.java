package com.quransafeguard.hifz.reader;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
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

    private final ExecutorService loader = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "quran-hifz-mushaf-loader");
        t.setDaemon(true);
        return t;
    });
    private final AtomicInteger generation = new AtomicInteger();
    private final LruCache<Integer, SVG> cache = new LruCache<>(3);
    private final MushafRepository repository;

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

    public void showPage(int page) {
        if (closed) throw new IllegalStateException("MushafRenderer is closed");
        if (page < MushafRepository.FIRST_PAGE || page > MushafRepository.LAST_PAGE) {
            throw new IllegalArgumentException("Mushaf page must be 1..604");
        }

        // Every navigation invalidates any older in-flight load, even when this page is cached.
        final int ticket = generation.incrementAndGet();
        SVG cached = cache.get(page);
        if (cached != null) {
            document = cached;
            currentPage = page;
            invalidate();
            if (listener != null) listener.onPageChanged(page);
            return;
        }

        loader.execute(() -> {
            try {
                MushafRepository.Page source = repository.load(page);
                SVG parsed = SVG.getFromInputStream(new ByteArrayInputStream(source.svgUtf8));
                RectF box = parsed.getDocumentViewBox();
                if (box == null || box.width() <= 0f || box.height() <= 0f) {
                    throw new IllegalArgumentException("Mushaf page has no valid viewBox: " + page);
                }
                post(() -> {
                    if (closed || ticket != generation.get()) return;
                    cache.put(page, parsed);
                    document = parsed;
                    currentPage = page;
                    invalidate();
                    if (listener != null) listener.onPageChanged(page);
                });
            } catch (Throwable error) {
                post(() -> {
                    if (closed || ticket != generation.get()) return;
                    if (listener != null) listener.onError(page, error);
                });
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
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.WHITE);
        SVG svg = document;
        if (svg == null || getWidth() <= 0 || getHeight() <= 0) return;
        svg.renderToCanvas(canvas, new RectF(0f, 0f, getWidth(), getHeight()));
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        generation.incrementAndGet();
        loader.shutdownNow();
        cache.evictAll();
        document = null;
    }
}
