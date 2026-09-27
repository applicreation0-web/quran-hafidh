package com.quransafeguard.hifz.preview;

import android.app.Activity;
import android.app.AlertDialog;

/**
 * The compact Révision selector (P2): Révision active, Quiz spatial, Entretien — free order.
 * A dialogue, not a new Activity (only SpatialQuizActivity itself is new). Carries no progression
 * logic of its own; it only reports today's done/to-do status and defers the actual navigation
 * to the caller.
 */
final class RevisionSelector {
    private RevisionSelector() {}

    interface Choice {
        void openActiveRevision();
        void openPassiveRevision();
        void openSpatialQuiz();
    }

    static void show(Activity activity, HifzPrefs prefs, GeometryRepository geometry,
                      HifzSpeedStore speedStore, Choice choice) {
        String today = HifzClock.today().toString();
        boolean activeDone = today.equals(prefs.lastActiveMurajaahDate());
        boolean passiveDone = today.equals(prefs.lastMurajaahDate());
        String maintenanceCue = geometry != null
            ? MaintenanceCoveragePolicy.minutes(
                prefs.acquiredLineCountV6(), geometry.lineCount(), speedStore.maintenanceSecondsPerLine()) + " min"
            : "durée variable";
        String[] items = {
            "Révision active · 15 min · " + (activeDone ? "fait" : "à faire"),
            "Quiz spatial · ≤15 min · facultatif",
            "Entretien · " + maintenanceCue + " · " + (passiveDone ? "fait" : "à faire"),
        };
        new AlertDialog.Builder(activity)
            .setTitle("Révision")
            .setItems(items, (dialog, which) -> {
                if (which == 0) choice.openActiveRevision();
                else if (which == 1) choice.openSpatialQuiz();
                else choice.openPassiveRevision();
            })
            .show();
    }
}
