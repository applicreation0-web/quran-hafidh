package com.quransafeguard.hifz.preview;

import android.app.Application;

/** Recover any interrupted import before opening Hifz preferences or screens. */
public final class HifzRecoveryApplication extends Application {
    @Override public void onCreate() {
        super.onCreate();
        HifzBackup.recoverIfNecessary(this);
    }
}
