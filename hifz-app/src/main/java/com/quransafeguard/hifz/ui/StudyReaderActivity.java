package com.quransafeguard.hifz.ui;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.quransafeguard.hifz.core.VerseRef;
import com.quransafeguard.hifz.data.GeometryRepository;
import com.quransafeguard.hifz.data.TafsirRepository;
import com.quransafeguard.hifz.reader.ReaderSurface;
import com.quransafeguard.hifz.storage.ReaderStateStore;

import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Lecture / Etude optimized for BOOX.
 * The Mushaf owns the whole app content area. Navigation is gesture/key based and the only
 * persistent content is the page itself; page/Tafsir controls float above it when needed.
 */
public final class StudyReaderActivity extends android.app.Activity implements ReaderSurface.Listener {
    private static final long PAGE_BADGE_MS = 1600L;

    private ReaderSurface surface;
    private ReaderStateStore stateStore;
    private TextView pageBadge;
    private Button tafsirButton;
    private GeometryRepository.AyahRegion selected;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final Runnable hidePageBadge = () -> {
        if (pageBadge != null) pageBadge.setVisibility(View.GONE);
    };
    private final ExecutorService io = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "quran-hifz-tafsir");
        t.setDaemon(true);
        return t;
    });

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        stateStore = new ReaderStateStore(this);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Ui.PAPER);

        surface = new ReaderSurface(this);
        surface.setListener(this);
        root.addView(surface, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ));

        pageBadge = Ui.text(this, "1 / 604", 13, true);
        pageBadge.setGravity(Gravity.CENTER);
        pageBadge.setPadding(Ui.dp(this, 10), Ui.dp(this, 5), Ui.dp(this, 10), Ui.dp(this, 5));
        pageBadge.setBackground(floatingPanelBackground(1));
        pageBadge.setVisibility(View.GONE);
        FrameLayout.LayoutParams badgeLp = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.TOP | Gravity.CENTER_HORIZONTAL
        );
        badgeLp.topMargin = Ui.dp(this, 8);
        root.addView(pageBadge, badgeLp);

        tafsirButton = Ui.smallButton(this, "Tafsir", v -> openTafsir());
        tafsirButton.setPadding(Ui.dp(this, 14), 0, Ui.dp(this, 14), 0);
        tafsirButton.setBackground(floatingPanelBackground(2));
        tafsirButton.setVisibility(View.GONE);
        FrameLayout.LayoutParams tafsirLp = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.END | Gravity.BOTTOM
        );
        tafsirLp.rightMargin = Ui.dp(this, 12);
        tafsirLp.bottomMargin = Ui.dp(this, 12);
        root.addView(tafsirButton, tafsirLp);

        setContentView(root);
        surface.showPage(stateStore.lastPage());
    }

    private GradientDrawable floatingPanelBackground(int strokeDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(Ui.PAPER);
        d.setStroke(Ui.dp(this, strokeDp), Ui.INK);
        d.setCornerRadius(Ui.dp(this, 5));
        return d;
    }

    private void go(int delta) {
        int current = surface.getCurrentPage();
        int page = Math.max(1, Math.min(604, current + delta));
        if (page == current) return;
        selected = null;
        tafsirButton.setVisibility(View.GONE);
        surface.clearMemorizationState();
        surface.showPage(page);
    }

    private void showPageBadge(int page) {
        pageBadge.setText(page + " / 604");
        pageBadge.setVisibility(View.VISIBLE);
        ui.removeCallbacks(hidePageBadge);
        ui.postDelayed(hidePageBadge, PAGE_BADGE_MS);
    }

    @Override public void onPageChanged(int page) {
        stateStore.saveLastPage(page);
        showPageBadge(page);
    }

    @Override public void onVerseTapped(GeometryRepository.AyahRegion verse) {
        selected = verse;
        surface.setMemorizationState(Collections.emptyList(), verse, 0);
        tafsirButton.setText("Tafsir · " + verse.surah + ":" + verse.ayah);
        tafsirButton.setVisibility(View.VISIBLE);
    }

    @Override public void onPageSwipe(int delta) {
        go(delta);
    }

    @Override public void onError(Throwable error) {
        Toast.makeText(this, "Lecture indisponible : " + error.getMessage(), Toast.LENGTH_LONG).show();
    }

    private void openTafsir() {
        final GeometryRepository.AyahRegion region = selected;
        if (region == null) return;
        final VerseRef verse = new VerseRef(region.surah, region.ayah);
        final Dialog dialog = new Dialog(this);

        LinearLayout shell = Ui.column(this);
        shell.setPadding(Ui.dp(this, 14), Ui.dp(this, 10), Ui.dp(this, 14), Ui.dp(this, 12));
        shell.setBackground(floatingPanelBackground(1));

        LinearLayout header = Ui.row(this);
        TextView title = Ui.text(this, "Jalalayn · " + region.surah + ":" + region.ayah, 17, true);
        Ui.weight(title, 1f);
        header.addView(title);

        TextView body = Ui.text(this, "Chargement…", stateStore.tafsirTextSp(), false);
        body.setTextIsSelectable(false);

        Button minus = Ui.smallButton(this, "A−", v -> changeTafsirSize(body, -1f));
        Button plus = Ui.smallButton(this, "A+", v -> changeTafsirSize(body, +1f));
        Button close = Ui.smallButton(this, "×", v -> dialog.dismiss());
        header.addView(minus);
        header.addView(plus);
        header.addView(close);
        shell.addView(header);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(body, new ScrollView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        shell.addView(scroll, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            0,
            1f
        ));

        dialog.setContentView(shell);
        dialog.setCanceledOnTouchOutside(true);
        dialog.setOnDismissListener(ignored -> {
            if (!isFinishing() && selected != null) tafsirButton.setVisibility(View.VISIBLE);
        });
        tafsirButton.setVisibility(View.GONE);
        dialog.show();

        Window w = dialog.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            w.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            int width = Math.round(getResources().getDisplayMetrics().widthPixels * 0.92f);
            int height = Math.round(getResources().getDisplayMetrics().heightPixels * 0.46f);
            w.setLayout(width, height);
            WindowManager.LayoutParams attrs = w.getAttributes();
            attrs.y = Ui.dp(this, 18);
            w.setAttributes(attrs);
        }

        io.execute(() -> {
            try {
                TafsirRepository.Entry entry = new TafsirRepository(this).load(verse);
                final String text;
                if (entry == null) {
                    text = "Aucun commentaire disponible pour ce verset.";
                } else {
                    StringBuilder b = new StringBuilder(entry.commentary);
                    for (String note : entry.notes) b.append("\n\n").append(note);
                    text = b.toString();
                }
                runOnUiThread(() -> body.setText(text));
            } catch (Throwable error) {
                runOnUiThread(() -> body.setText("Tafsir indisponible."));
            }
        });
    }

    private void changeTafsirSize(TextView body, float delta) {
        float current = body.getTextSize() / getResources().getDisplayMetrics().scaledDensity;
        float next = Math.max(14f, Math.min(30f, current + delta));
        body.setTextSize(next);
        stateStore.saveTafsirTextSp(next);
    }

    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_PAGE_UP) {
            go(-1);
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_PAGE_DOWN) {
            go(+1);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override protected void onDestroy() {
        ui.removeCallbacksAndMessages(null);
        io.shutdownNow();
        if (surface != null) surface.close();
        super.onDestroy();
    }
}
