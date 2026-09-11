package com.quransafeguard.hifz.eink;

import android.graphics.Rect;
import android.view.View;

import com.onyx.android.sdk.api.device.epd.EpdController;
import com.onyx.android.sdk.api.device.epd.UpdateMode;

/**
 * The only place in Quran Hifz that talks directly to the Onyx SDK.
 * Domain, Mushaf and memorization code remain BOOX-independent.
 */
public final class BooxEinkController {
    private static final int FULL_CLEAN_PAGE_INTERVAL = 8;

    private int pagesSinceFullClean;

    /** Text-optimized stable mode for the Mushaf body. */
    public void attachReader(View reader) {
        if (reader == null) return;
        try {
            EpdController.setViewDefaultUpdateMode(reader, UpdateMode.REGAL);
        } catch (RuntimeException ignored) {
            // Android emulator/non-Onyx runtime: ordinary View invalidation remains functional.
        }
    }

    /** Called after a new Mushaf page has actually reached the renderer. */
    public void pageChanged(View reader) {
        if (reader == null) return;
        pagesSinceFullClean++;
        if (pagesSinceFullClean >= FULL_CLEAN_PAGE_INTERVAL) {
            fullClean(reader);
            return;
        }
        try {
            EpdController.invalidate(reader, UpdateMode.REGAL);
        } catch (RuntimeException ignored) {
            reader.invalidate();
        }
    }

    /** Small counters, controls and focus changes: cheap partial update. */
    public void localChanged(View view) {
        if (view == null) return;
        try {
            EpdController.invalidate(view, UpdateMode.GU);
        } catch (RuntimeException ignored) {
            view.invalidate();
        }
    }

    /** Partial refresh constrained to a changed mask/focus rectangle. */
    public void localChanged(View view, Rect dirty) {
        if (view == null) return;
        if (dirty == null || dirty.isEmpty()) {
            localChanged(view);
            return;
        }
        view.invalidate(dirty);
    }

    /** Explicit ghosting cleanup. Kept rare because GC is visibly slower on E-Ink. */
    public void fullClean(View view) {
        if (view == null) return;
        pagesSinceFullClean = 0;
        try {
            EpdController.invalidate(view, UpdateMode.GC);
        } catch (RuntimeException ignored) {
            view.invalidate();
        }
    }

    public int getPagesSinceFullClean() {
        return pagesSinceFullClean;
    }
}
