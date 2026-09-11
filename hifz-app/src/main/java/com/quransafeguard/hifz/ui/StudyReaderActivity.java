package com.quransafeguard.hifz.ui;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.Typeface;
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
import java.util.concurrent.atomic.AtomicInteger;

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
    private final AtomicInteger tafsirGeneration = new AtomicInteger();

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
        final int dialogTicket = tafsirGeneration.incrementAndGet();

        LinearLayout shell = Ui.column(this);
        shell.setPadding(Ui.dp(this, 14), Ui.dp(this, 10), Ui.dp(this, 14), Ui.dp(this, 12));
        shell.setBackground(floatingPanelBackground(1));

        LinearLayout header = Ui.row(this);
        TextView title = Ui.text(this,
            "Tafsir · sourate " + region.surah + " · verset " + region.ayah, 17, true);
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

        LinearLayout editions = Ui.row(this);
        editions.setPadding(0, Ui.dp(this, 4), 0, Ui.dp(this, 5));
        Button jalalayn = Ui.smallButton(this, "Jalalayn", null);
        Button qurtubi = Ui.smallButton(this, "Qurtubi", null);
        Button qushayri = Ui.smallButton(this, "Qushayri", null);
        Ui.weight(jalalayn, 1f);
        Ui.weight(qurtubi, 1f);
        Ui.weight(qushayri, 1f);
        editions.addView(jalalayn);
        editions.addView(qurtubi);
        editions.addView(qushayri);
        shell.addView(editions);

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

        final TafsirRepository.Edition[] current = { stateStore.tafsirEdition() };
        final Runnable refreshSelection = () -> {
            styleEditionButton(jalalayn, current[0] == TafsirRepository.Edition.JALALAYN);
            styleEditionButton(qurtubi, current[0] == TafsirRepository.Edition.QURTUBI);
            styleEditionButton(qushayri, current[0] == TafsirRepository.Edition.QUSHAYRI);
        };

        jalalayn.setOnClickListener(v -> selectEdition(
            TafsirRepository.Edition.JALALAYN, current, refreshSelection, verse, body, dialogTicket));
        qurtubi.setOnClickListener(v -> selectEdition(
            TafsirRepository.Edition.QURTUBI, current, refreshSelection, verse, body, dialogTicket));
        qushayri.setOnClickListener(v -> selectEdition(
            TafsirRepository.Edition.QUSHAYRI, current, refreshSelection, verse, body, dialogTicket));
        refreshSelection.run();

        dialog.setContentView(shell);
        dialog.setCanceledOnTouchOutside(true);
        dialog.setOnDismissListener(ignored -> {
            tafsirGeneration.incrementAndGet();
            if (surface != null) surface.cleanupGhosting();
            if (!isFinishing() && selected != null) tafsirButton.setVisibility(View.VISIBLE);
        });
        tafsirButton.setVisibility(View.GONE);
        dialog.show();

        Window w = dialog.getWindow();
        if (w != null) {
            w.setWindowAnimations(0);
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            w.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            int screenWidth = getResources().getDisplayMetrics().widthPixels;
            int screenHeight = getResources().getDisplayMetrics().heightPixels;
            int width = Math.min(Math.round(screenWidth * 0.92f), Ui.dp(this, 760));
            int height = Math.min(Math.round(screenHeight * 0.48f), Ui.dp(this, 620));
            w.setLayout(width, height);
            WindowManager.LayoutParams attrs = w.getAttributes();
            attrs.y = Ui.dp(this, 18);
            w.setAttributes(attrs);
        }

        loadEdition(verse, current[0], body, dialogTicket);
    }

    private void selectEdition(TafsirRepository.Edition edition,
                               TafsirRepository.Edition[] current,
                               Runnable refreshSelection,
                               VerseRef verse,
                               TextView body,
                               int dialogTicket) {
        if (current[0] == edition) return;
        current[0] = edition;
        stateStore.saveTafsirEdition(edition);
        refreshSelection.run();
        loadEdition(verse, edition, body, dialogTicket);
    }

    private void styleEditionButton(Button button, boolean selected) {
        button.setTypeface(Typeface.DEFAULT, selected ? Typeface.BOLD : Typeface.NORMAL);
        button.setTextSize(selected ? 15f : 14f);
    }

    private void loadEdition(VerseRef verse,
                             TafsirRepository.Edition edition,
                             TextView body,
                             int dialogTicket) {
        final int requestTicket = tafsirGeneration.incrementAndGet();
        body.setText("Chargement · " + edition.displayName + "…");
        io.execute(() -> {
            final String text;
            try {
                TafsirRepository.Entry entry = new TafsirRepository(this).load(verse, edition);
                if (entry == null) {
                    text = "Aucun commentaire " + edition.displayName + " disponible pour ce verset.";
                } else {
                    StringBuilder b = new StringBuilder(entry.commentary);
                    for (String note : entry.notes) b.append("\n\n").append(note);
                    text = b.toString();
                }
            } catch (Throwable error) {
                String message = error.getMessage();
                text = edition.displayName + " indisponible"
                    + (message == null || message.isEmpty() ? "." : " : " + message);
            }
            runOnUiThread(() -> {
                if (isFinishing() || dialogTicket > requestTicket || requestTicket != tafsirGeneration.get()) return;
                body.setText(text);
            });
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
        tafsirGeneration.incrementAndGet();
        ui.removeCallbacksAndMessages(null);
        io.shutdownNow();
        if (surface != null) surface.close();
        super.onDestroy();
    }
}
