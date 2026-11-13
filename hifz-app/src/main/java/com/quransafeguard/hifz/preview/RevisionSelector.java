package com.quransafeguard.hifz.preview;

import android.app.Activity;
import android.app.AlertDialog;

import java.time.LocalDate;

/** Dated active slots and optional passive reading. */
final class RevisionSelector {
    private RevisionSelector() {}

    interface Choice {
        void openActiveRevision(ActiveReviewSlots.Slot slot);
        void openPassiveRevision();
    }

    static void show(Activity activity, HifzPrefs prefs, GeometryRepository geometry,
                      HifzSpeedStore speedStore, boolean includePassive, Choice choice) {
        LocalDate today = HifzClock.today();
        ActiveReviewSlots store = prefs.activeReviewSlots();
        ActiveReviewSlots.Slot[] slots = ActiveReviewSlots.Slot.values();
        String[] items = new String[slots.length + (includePassive ? 1 : 0)];
        int proposedMinutes = geometry == null ? ActiveReviewSlots.LEGACY_MINUTES
            : MaintenanceCoveragePolicy.minutes(
                prefs.acquiredLineCountV6(), geometry.lineCount(), speedStore.maintenanceSecondsPerLine());
        String maintenanceCue = geometry == null ? "durée variable" : proposedMinutes + " min";
        for (int i = 0; i < slots.length; i++) {
            ActiveReviewSlots.Slot slot = slots[i];
            boolean done = store.receipt(today, slot) != null;
            items[i] = slot.title() + " · " + store.targetMinutes(today, slot, proposedMinutes)
                + " min · " + (done ? "fait" : "à faire");
        }
        if (includePassive) items[slots.length] = "Révision passive · " + maintenanceCue
            + (today.toString().equals(prefs.lastMurajaahDate()) ? " · fait" : " · facultative");
        new AlertDialog.Builder(activity)
            .setTitle("Révision")
            .setItems(items, (dialog, which) -> {
                if (which < slots.length) choice.openActiveRevision(slots[which]);
                else choice.openPassiveRevision();
            })
            .show();
    }
}
