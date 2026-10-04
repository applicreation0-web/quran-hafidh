package com.quransafeguard.hifz.preview;

import android.app.Activity;
import android.app.Application;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.quransafeguard.hifz.core.VerseRef;

import java.util.ArrayList;
import java.util.List;

/**
 * Legacy class name retained for source stability. The audio surface is now strictly inline:
 * no modal window, no dim layer, and no overlap with the Mushaf.
 */
final class HifzAudioDialog {
    private final Activity activity;
    private final MushafView mushaf;
    private final HifzAudioPack pack;
    private final HifzPrefs prefs;
    private final ArrayList<VerseRef> queue;
    private final EinkController eink = new EinkController();
    private LinearLayout host;
    private LinearLayout panel;
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
        @Override public void onActivityPaused(Activity a) { if (a == activity) detachInline(); }
        @Override public void onActivityStopped(Activity a) {}
        @Override public void onActivitySaveInstanceState(Activity a, Bundle b) {}
        @Override public void onActivityDestroyed(Activity a) { if (a == activity) detachInline(); }
    };

    HifzAudioDialog(Activity activity, MushafView mushaf, List<VerseRef> verses) {
        this.activity = activity;
        this.mushaf = mushaf;
        this.pack = new HifzAudioPack(activity);
        this.prefs = new HifzPrefs(activity);
        this.queue = new ArrayList<>(verses == null ? java.util.Collections.emptyList() : verses);
    }

    void attachInline(LinearLayout target) {
        if (target == null) return;
        if (!pack.installed()) {
            Toast.makeText(activity, "Pack audio non installé · Paramètres › Audio", Toast.LENGTH_LONG).show();
            return;
        }
        if (queue.isEmpty()) {
            Toast.makeText(activity, "Sélectionnez un verset.", Toast.LENGTH_LONG).show();
            return;
        }

        detachInline();
        host = target;
        host.removeAllViews();
        host.setVisibility(View.VISIBLE);
        host.setPadding(Ui.dp(activity, 6), 0, Ui.dp(activity, 6), 0);

        panel = Ui.row(activity);
        panel.setGravity(Gravity.CENTER_VERTICAL);
        panel.setPadding(Ui.dp(activity, 4), 0, Ui.dp(activity, 2), 0);

        title = Ui.bookText(activity, "Al-Husary Muʿallim", 11.5f, true);
        title.setSingleLine(true);
        title.setGravity(Gravity.CENTER_VERTICAL);
        title.setPadding(Ui.dp(activity, 4), 0, Ui.dp(activity, 6), 0);
        Ui.weight(title, 1f);
        panel.addView(title);

        Button previous = Ui.iconButton(activity, "‹", "Verset précédent", v -> previous());
        playPause = Ui.iconButton(activity, "▶", "Lire / pause", v -> toggle());
        Button next = Ui.iconButton(activity, "›", "Verset suivant", v -> next());
        Button repeat = Ui.iconButton(activity, "↻", "Répéter le verset", v -> playCurrent(false));
        Button close = Ui.iconButton(activity, "×", "Fermer", v -> detachInline());
        panel.addView(previous);
        panel.addView(playPause);
        panel.addView(next);
        panel.addView(repeat);
        panel.addView(close);
        host.addView(panel, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        host.addView(Ui.divider(activity));

        registerLifecycle();
        updateIdentity();
        setPlayIcon(false);
        eink.local(host, prefs);
    }

    void detachInline() {
        releasePlayerOnly();
        mushaf.setAudioVerse(null);
        LinearLayout currentHost = host;
        host = null;
        panel = null;
        title = null;
        playPause = null;
        if (currentHost != null) {
            currentHost.removeAllViews();
            currentHost.setVisibility(View.GONE);
            eink.local(currentHost, prefs);
        }
        unregisterLifecycle();
    }

    void close() { detachInline(); }

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
            setPlayIcon(false);
            return;
        }
        if (player != null && paused) {
            player.start();
            paused = false;
            setPlayIcon(true);
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
                    setPlayIcon(true);
                } catch (Throwable error) {
                    releasePlayerOnly();
                    mushaf.setAudioVerse(null);
                    Toast.makeText(activity, "Lecture audio impossible", Toast.LENGTH_LONG).show();
                }
            });
            candidate.setOnCompletionListener(done -> {
                if (player != candidate) return;
                releasePlayerOnly();
                setPlayIcon(false);
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
        if (host != null) eink.local(host, prefs);
    }

    private void setPlayIcon(boolean playing) {
        if (playPause == null) return;
        Ui.setButtonIcon(playPause, playing ? R.drawable.ic_ui_pause : R.drawable.ic_ui_play);
        playPause.setContentDescription(playing ? "Pause" : "Lire");
        if (host != null) eink.local(host, prefs);
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
        setPlayIcon(false);
    }
}
