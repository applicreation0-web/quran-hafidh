package com.quransafeguard.hifz.preview;

import android.app.Activity;
import android.app.Application;
import android.app.Dialog;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.quransafeguard.hifz.core.VerseRef;

import java.util.ArrayList;
import java.util.List;

/** Audio controls deliberately isolated from HifzPrefs/repetition/cursor state. */
final class HifzAudioDialog {
    private final Activity activity;
    private final MushafView mushaf;
    private final HifzAudioPack pack;
    private final ArrayList<VerseRef> queue;
    private Dialog dialog;
    private MediaPlayer player;
    private TextView title;
    private Button playPause;
    private int index;
    private boolean paused;
    private boolean preparing;
    private boolean lifecycleRegistered;

    private final Application.ActivityLifecycleCallbacks lifecycle = new Application.ActivityLifecycleCallbacks() {
        @Override public void onActivityCreated(Activity a, Bundle b) {}
        @Override public void onActivityStarted(Activity a) {}
        @Override public void onActivityResumed(Activity a) {}
        @Override public void onActivityPaused(Activity a) { if (a == activity) close(); }
        @Override public void onActivityStopped(Activity a) {}
        @Override public void onActivitySaveInstanceState(Activity a, Bundle b) {}
        @Override public void onActivityDestroyed(Activity a) { if (a == activity) close(); }
    };

    HifzAudioDialog(Activity activity, MushafView mushaf, List<VerseRef> verses) {
        this.activity = activity;
        this.mushaf = mushaf;
        this.pack = new HifzAudioPack(activity);
        this.queue = new ArrayList<>(verses == null ? java.util.Collections.emptyList() : verses);
    }

    void show() {
        if (!pack.installed()) {
            Toast.makeText(activity, "Pack audio non installé · Paramètres › Audio", Toast.LENGTH_LONG).show();
            return;
        }
        if (queue.isEmpty()) {
            Toast.makeText(activity, "Sélectionnez un verset.", Toast.LENGTH_LONG).show();
            return;
        }

        close();
        dialog = new Dialog(activity);
        LinearLayout shell = Ui.column(activity);
        shell.setPadding(Ui.dp(activity, 14), Ui.dp(activity, 10), Ui.dp(activity, 14), Ui.dp(activity, 10));
        title = Ui.bookText(activity, "Al-Husary Muʿallim", 16, true);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        shell.addView(title);

        LinearLayout row = Ui.row(activity);
        row.setGravity(Gravity.CENTER);
        Button previous = Ui.roundButton(activity, "‹", "Verset précédent", v -> previous());
        playPause = Ui.roundButton(activity, "▶", "Lire / pause", v -> toggle());
        Button next = Ui.roundButton(activity, "›", "Verset suivant", v -> next());
        Button repeat = Ui.roundButton(activity, "↻", "Répéter le verset", v -> playCurrent(false));
        Button close = Ui.roundButton(activity, "×", "Fermer", v -> close());
        row.addView(previous);
        row.addView(playPause);
        row.addView(next);
        row.addView(repeat);
        row.addView(close);
        shell.addView(row);

        TextView source = Ui.text(activity, pack.sourceLabel(), 10.5f, false);
        source.setTextColor(Ui.MUTED);
        source.setGravity(Gravity.CENTER_HORIZONTAL);
        shell.addView(source);

        dialog.setContentView(shell);
        dialog.setOnDismissListener(d -> {
            releasePlayerOnly();
            mushaf.setAudioVerse(null);
            dialog = null;
            unregisterLifecycle();
        });
        dialog.show();
        registerLifecycle();
        Window w = dialog.getWindow();
        if (w != null) {
            w.setLayout(
                Math.min(activity.getResources().getDisplayMetrics().widthPixels - Ui.dp(activity, 24), Ui.dp(activity, 620)),
                ViewGroup.LayoutParams.WRAP_CONTENT);
            w.setWindowAnimations(0);
        }
        updateIdentity();
    }

    void close() {
        Dialog current = dialog;
        dialog = null;
        if (current != null && current.isShowing()) {
            try { current.dismiss(); } catch (Throwable ignored) {}
        }
        releasePlayerOnly();
        mushaf.setAudioVerse(null);
        unregisterLifecycle();
    }

    private void registerLifecycle() {
        if (lifecycleRegistered) return;
        activity.getApplication().registerActivityLifecycleCallbacks(lifecycle);
        lifecycleRegistered = true;
    }

    private void unregisterLifecycle() {
        if (!lifecycleRegistered) return;
        try { activity.getApplication().unregisterActivityLifecycleCallbacks(lifecycle); } catch (Throwable ignored) {}
        lifecycleRegistered = false;
    }

    private void toggle() {
        if (preparing) return;
        if (player != null && player.isPlaying()) {
            player.pause();
            paused = true;
            playPause.setText("▶");
            return;
        }
        if (player != null && paused) {
            player.start();
            paused = false;
            playPause.setText("Ⅱ");
            return;
        }
        playCurrent(true);
    }

    private void previous() { if (index > 0) { index--; playCurrent(true); } }
    private void next() { if (index + 1 < queue.size()) { index++; playCurrent(true); } }

    private void playCurrent(boolean allowAutoNext) {
        releasePlayerOnly();
        paused = false;
        VerseRef verse = queue.get(index);
        if (!pack.hasVerse(verse)) {
            mushaf.setAudioVerse(null);
            Toast.makeText(activity, "Audio manquant · " + verse, Toast.LENGTH_LONG).show();
            return;
        }
        try {
            final MediaPlayer candidate = new MediaPlayer();
            player = candidate;
            preparing = true;
            if (playPause != null) playPause.setEnabled(false);
            pack.setDataSource(candidate, verse);
            candidate.setOnPreparedListener(ready -> {
                if (player != candidate) return;
                preparing = false;
                if (playPause != null) playPause.setEnabled(true);
                updateIdentity();
                try {
                    player.start();
                    mushaf.setAudioVerse(verse);
                    playPause.setText("Ⅱ");
                } catch (Throwable error) {
                    releasePlayerOnly();
                    mushaf.setAudioVerse(null);
                    Toast.makeText(activity, "Lecture audio impossible", Toast.LENGTH_LONG).show();
                }
            });
            candidate.setOnCompletionListener(done -> {
                if (player != candidate) return;
                releasePlayerOnly();
                if (playPause != null) playPause.setText("▶");
                if (allowAutoNext && index + 1 < queue.size()) {
                    index++;
                    playCurrent(true);
                } else {
                    mushaf.setAudioVerse(null);
                }
            });
            candidate.setOnErrorListener((failed, what, extra) -> {
                if (player == candidate) {
                    releasePlayerOnly();
                    mushaf.setAudioVerse(null);
                    Toast.makeText(activity, "Lecture audio impossible", Toast.LENGTH_LONG).show();
                }
                return true;
            });
            candidate.prepareAsync();
        } catch (Throwable error) {
            releasePlayerOnly();
            mushaf.setAudioVerse(null);
            Toast.makeText(activity, "Lecture audio impossible", Toast.LENGTH_LONG).show();
        }
    }

    private void updateIdentity() {
        if (title == null || queue.isEmpty()) return;
        VerseRef verse = queue.get(index);
        title.setText("Al-Husary Muʿallim · " + verse + " · " + (index + 1) + "/" + queue.size());
    }

    private void releasePlayerOnly() {
        preparing = false;
        if (playPause != null) playPause.setEnabled(true);
        MediaPlayer current = player;
        player = null;
        if (current != null) {
            try { current.setOnPreparedListener(null); } catch (Throwable ignored) {}
            try { current.setOnCompletionListener(null); } catch (Throwable ignored) {}
            try { current.setOnErrorListener(null); } catch (Throwable ignored) {}
            try { current.stop(); } catch (Throwable ignored) {}
            try { current.release(); } catch (Throwable ignored) {}
        }
        paused = false;
    }
}
