package com.quransafeguard.hifz.preview;

import android.os.Build;
import android.view.View;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/** Rendering-only BOOX helper. It never enables/disables product functionality. */
public final class EinkController {
    private int pageChangesSinceFull = 0;
    private int localChangesSinceFull = 0;
    private int maskChangesSinceFull = 0;
    private int audioChangesSinceFull = 0;

    public boolean isEink(HifzPrefs prefs) {
        if (prefs.forceEink()) return true;
        String maker = (Build.MANUFACTURER + " " + Build.BRAND).toLowerCase();
        return maker.contains("onyx") || maker.contains("boox");
    }

    /**
     * Small changes (counter, selection marker) prefer a BOOX partial mode.
     * A periodic GC cleanup prevents long sessions from accumulating ghosting.
     */
    public void local(View view, HifzPrefs prefs) {
        if (view == null) return;
        if (!isEink(prefs)) { view.invalidate(); return; }
        localChangesSinceFull++;
        if (localChangesSinceFull >= PreviewConfig.EINK_LOCAL_CHANGES_BEFORE_FULL_CLEAN_WORKING) {
            fullAndReset(view);
            return;
        }
        if (!tryOnyxPartial(view)) view.invalidate();
    }

    /** Audio highlighting changes verse-by-verse for long periods, so use a stricter cleanup cadence. */
    public void audio(View view, HifzPrefs prefs) {
        if (view == null) return;
        if (!isEink(prefs)) { view.invalidate(); return; }
        audioChangesSinceFull++;
        if (audioChangesSinceFull >= PreviewConfig.EINK_AUDIO_CHANGES_BEFORE_FULL_CLEAN_WORKING) {
            fullAndReset(view);
            return;
        }
        if (!tryOnyxPartial(view)) view.invalidate();
    }

    /** Mask changes touch a larger area than a simple counter/audio outline. */
    public void mask(View view, HifzPrefs prefs) {
        if (view == null) return;
        if (!isEink(prefs)) { view.invalidate(); return; }
        maskChangesSinceFull++;
        if (maskChangesSinceFull >= PreviewConfig.EINK_MASK_CHANGES_BEFORE_FULL_CLEAN_WORKING) {
            fullAndReset(view);
            return;
        }
        if (!tryOnyxPartial(view)) view.invalidate();
    }

    public void page(View view, HifzPrefs prefs) {
        if (view == null) return;
        if (!isEink(prefs)) { view.invalidate(); return; }
        pageChangesSinceFull++;
        if (pageChangesSinceFull >= PreviewConfig.EINK_FULL_CLEAN_PAGE_INTERVAL_WORKING) {
            fullAndReset(view);
        } else if (!tryOnyxPartial(view)) {
            view.invalidate();
        }
    }

    public void cycleCompleted(View view, HifzPrefs prefs) {
        if (view == null) return;
        if (isEink(prefs)) full(view); else view.invalidate();
        resetCounters();
    }

    private void fullAndReset(View view) {
        full(view);
        resetCounters();
    }

    private void resetCounters() {
        pageChangesSinceFull = 0;
        localChangesSinceFull = 0;
        maskChangesSinceFull = 0;
        audioChangesSinceFull = 0;
    }

    private void full(View view) {
        if (tryOnyxMode(view, true)) return;
        // Safe fallback: invalidate only. No artificial black/white flash on unknown devices.
        view.invalidate();
    }

    private boolean tryOnyxPartial(View view) {
        return tryOnyxMode(view, false);
    }

    /**
     * BOOX SDK is accessed by reflection so Quran Hifz keeps no vendor dependency.
     * Partial preference order: REGAL (best anti-ghosting text mode), then GU, then DU.
     * Full preference order: GC, then any enum containing FULL.
     */
    private boolean tryOnyxMode(View view, boolean wantFull) {
        try {
            Class<?> controller = Class.forName("com.onyx.android.sdk.api.device.epd.EpdController");
            Class<?> modeClass = Class.forName("com.onyx.android.sdk.api.device.epd.UpdateMode");
            Object chosen = null;
            Object[] values = modeClass.getEnumConstants();
            if (values != null) {
                String[] preferred = wantFull ? new String[] {"GC", "FULL"} : new String[] {"REGAL", "GU", "DU"};
                for (String target : preferred) {
                    for (Object value : values) {
                        String name = value.toString().toUpperCase();
                        if (name.equals(target) || (target.equals("FULL") && name.contains("FULL"))) {
                            chosen = value;
                            break;
                        }
                    }
                    if (chosen != null) break;
                }
            }
            if (chosen == null) return false;
            for (Method method : controller.getMethods()) {
                Class<?>[] params = method.getParameterTypes();
                if (Modifier.isStatic(method.getModifiers()) && params.length == 2 &&
                    View.class.isAssignableFrom(params[0]) && params[1].isAssignableFrom(modeClass)) {
                    method.invoke(null, view, chosen);
                    return true;
                }
            }
        } catch (Throwable ignored) { }
        return false;
    }
}
