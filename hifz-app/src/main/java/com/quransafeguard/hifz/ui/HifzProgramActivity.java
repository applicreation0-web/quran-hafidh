package com.quransafeguard.hifz.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.quransafeguard.hifz.storage.HifzProgressStore;
import com.quransafeguard.hifz.storage.HifzScheduleStore;

import java.time.LocalDate;

/** Structured Sabqi/Itqan/Murajaah entry point. */
public final class HifzProgramActivity extends android.app.Activity {
    private HifzProgressStore progress;
    private HifzScheduleStore schedule;
    private LinearLayout root;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        progress = new HifzProgressStore(this);
        schedule = new HifzScheduleStore(this);
    }

    @Override protected void onResume() {
        super.onResume();
        if (!progress.isConfigured()) {
            startActivity(new Intent(this, HifzSetupActivity.class));
            finish();
            return;
        }
        build();
    }

    private void build() {
        root = Ui.column(this);
        root.addView(Ui.text(this, "Parcours Hifz", 24, true));
        root.addView(Ui.text(this,
            "Sabqi : " + progress.sabqiStart() + " → " + progress.sabqiEnd() + "\n" +
            "Itqān : " + progress.itqanStart() + " → " + progress.itqanEnd(), 14, false));

        HifzScheduleStore.Pending pending = schedule.nextPending(LocalDate.now());
        if (pending == null) {
            root.addView(Ui.text(this, "Toutes les séances prévues jusqu’à aujourd’hui sont terminées.", 17, true));
        } else {
            String status = pending.overdue ? "À replanifier" : "Séance du jour";
            String label = displayMode(pending.mode);
            TextView pendingText = Ui.text(this,
                status + " · " + pending.scheduledDate + "\n" + label +
                (pending.overdue ? "\nLe quota reste inchangé : aucune pénalité ni double séance automatique." : ""),
                17, true);
            root.addView(pendingText);
            root.addView(Ui.button(this, "Commencer " + label, v -> openSession(pending)));
        }

        root.addView(Ui.button(this, "Modifier les quatre bornes", v -> startActivity(new Intent(this, HifzSetupActivity.class))));
        root.addView(Ui.button(this, "Retour", v -> finish()));
        setContentView(root);
    }

    private void openSession(HifzScheduleStore.Pending pending) {
        Intent i = new Intent(this, HifzSessionActivity.class);
        i.putExtra(HifzSessionActivity.EXTRA_MODE, pending.mode);
        i.putExtra(HifzSessionActivity.EXTRA_SCHEDULE_DATE, pending.scheduledDate.toString());
        startActivity(i);
    }

    static String displayMode(String mode) {
        if (HifzScheduleStore.SABQI.equals(mode)) return "Sabqi · matin";
        if (HifzScheduleStore.SABQI_REVIEW.equals(mode)) return "Sabqi · révision du soir";
        if (HifzScheduleStore.ITQAN.equals(mode)) return "Itqān";
        return "Murājaʿah";
    }
}
