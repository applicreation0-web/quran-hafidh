package com.quransafeguard.hifz.preview;

import android.app.Activity;
import android.app.AlertDialog;

import com.quransafeguard.hifz.core.HifzSchedule;

/**
 * The compact Révision selector (P2): Révision active, Entretien — free order. A dialogue, not a
 * new Activity. Carries no progression logic of its own; it only reports today's done/to-do status
 * and defers the actual navigation to the caller.
 */
final class RevisionSelector {
    private RevisionSelector() {}

    interface Choice {
        void openActiveRevision();
        void openPassiveRevision();
    }

    static void show(Activity activity, HifzPrefs prefs, GeometryRepository geometry,
                      HifzSpeedStore speedStore, Choice choice) {
        String today = HifzClock.today().toString();
        boolean activeDone = today.equals(prefs.lastActiveMurajaahDate());
        boolean passiveDone = today.equals(prefs.lastMurajaahDate());
        String maintenanceCue = HifzSchedule.MAINTENANCE_MINUTES + " min";
        String[] items = {
            "Révision active · Amorces · " + HifzSchedule.ACTIVE_REVIEW_MINUTES + " min · " + (activeDone ? "fait" : "à faire"),
            "Entretien · lecture · " + maintenanceCue + " · " + (passiveDone ? "fait" : "à faire"),
        };
        new AlertDialog.Builder(activity)
            .setTitle("Révision")
            .setItems(items, (dialog, which) -> {
                if (which == 0) choice.openActiveRevision();
                else choice.openPassiveRevision();
            })
            .show();
    }
}
