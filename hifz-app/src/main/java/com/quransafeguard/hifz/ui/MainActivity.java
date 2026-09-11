package com.quransafeguard.hifz.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.quransafeguard.hifz.storage.HifzProgressStore;
import com.quransafeguard.hifz.storage.HifzScheduleStore;

import java.time.LocalDate;

/** Quran Hifz home. No app-blocking or device-type routing exists in this application. */
public final class MainActivity extends Activity {
    private LinearLayout root;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
    }

    @Override protected void onResume() {
        super.onResume();
        buildHome();
    }

    private void buildHome() {
        root = Ui.column(this);
        TextView title = Ui.text(this, "Quran Hifz", 28, true);
        root.addView(title);
        root.addView(Ui.text(this, "BOOX · local · Mushaf de Médine", 14, false));

        root.addView(Ui.button(this, "Lecture / Étude", v -> startActivity(new Intent(this, StudyReaderActivity.class))));
        root.addView(Ui.button(this, "Mémorisation libre", v -> startActivity(new Intent(this, FreeMemActivity.class))));
        root.addView(Ui.button(this, "Parcours Hifz", v -> openHifz()));

        HifzProgressStore progress = new HifzProgressStore(this);
        if (progress.isConfigured()) {
            HifzScheduleStore.Pending pending = new HifzScheduleStore(this).nextPending(LocalDate.now());
            if (pending != null) {
                String status = pending.overdue ? "À replanifier" : "Aujourd’hui";
                root.addView(Ui.text(this, status + " : " + HifzProgramActivity.displayMode(pending.mode)
                    + " · " + pending.scheduledDate, 15, true));
            }
            if (isDebuggable()) {
                root.addView(Ui.button(this, "Tests Hifz · sans impact", v ->
                    startActivity(new Intent(this, HifzDiagnosticsActivity.class))));
            }
        } else {
            root.addView(Ui.text(this, "Parcours Hifz : configuration initiale requise (4 bornes).", 14, false));
        }
        setContentView(root);
    }

    private boolean isDebuggable() {
        return (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
    }

    private void openHifz() {
        HifzProgressStore progress = new HifzProgressStore(this);
        startActivity(new Intent(this, progress.isConfigured() ? HifzProgramActivity.class : HifzSetupActivity.class));
    }
}
