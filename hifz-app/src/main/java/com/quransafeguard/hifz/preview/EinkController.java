package com.quransafeguard.hifz.preview;

import android.os.Build;
import android.view.View;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/** Rendering-only BOOX helper. It never enables/disables product functionality. */
public final class EinkController {
    private int pageChangesSinceFull = 0;

    public boolean isEink(HifzPrefs prefs) {
        if (prefs.forceEink()) return true;
        String maker = (Build.MANUFACTURER + " " + Build.BRAND).toLowerCase();
        return maker.contains("onyx") || maker.contains("boox");
    }

    public void local(View view) {
        if (view != null) view.invalidate();
    }

    public void mask(View view) {
        if (view != null) view.invalidate();
    }

    public void page(View view, HifzPrefs prefs) {
        if (view == null) return;
        if (!isEink(prefs)) { view.invalidate(); return; }
        pageChangesSinceFull++;
        if (pageChangesSinceFull >= PreviewConfig.EINK_FULL_CLEAN_PAGE_INTERVAL_WORKING) {
            full(view);
            pageChangesSinceFull = 0;
        } else {
            view.invalidate();
        }
    }

    public void cycleCompleted(View view, HifzPrefs prefs) {
        if (view == null) return;
        if (isEink(prefs)) full(view); else view.invalidate();
        pageChangesSinceFull = 0;
    }

    private void full(View view) {
        if (tryOnyx(view)) return;
        // Safe fallback: invalidate only. No artificial black/white flash on unknown devices.
        view.invalidate();
    }

    private boolean tryOnyx(View view) {
        try {
            Class<?> controller = Class.forName("com.onyx.android.sdk.api.device.epd.EpdController");
            Class<?> modeClass = Class.forName("com.onyx.android.sdk.api.device.epd.UpdateMode");
            Object gc = null;
            Object[] values = modeClass.getEnumConstants();
            if (values != null) {
                for (Object value : values) {
                    String name = value.toString().toUpperCase();
                    if (name.equals("GC") || name.contains("FULL")) { gc = value; break; }
                }
            }
            if (gc == null) return false;
            for (Method method : controller.getMethods()) {
                Class<?>[] params = method.getParameterTypes();
                if (Modifier.isStatic(method.getModifiers()) && params.length == 2 &&
                    View.class.isAssignableFrom(params[0]) && params[1].isAssignableFrom(modeClass)) {
                    method.invoke(null, view, gc);
                    return true;
                }
            }
        } catch (Throwable ignored) { }
        return false;
    }
}
