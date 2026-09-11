package com.quransafeguard.hifz.ui;

import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.quransafeguard.hifz.core.EligibleCorpus;
import com.quransafeguard.hifz.core.QuranCanon;
import com.quransafeguard.hifz.core.VerseRange;
import com.quransafeguard.hifz.core.VerseRef;
import com.quransafeguard.hifz.data.GeometryRepository;
import com.quransafeguard.hifz.data.LineGeometryRepository;
import com.quransafeguard.hifz.reader.ReaderSurface;
import com.quransafeguard.hifz.storage.HifzProgressStore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Candidate-only manual diagnostics. This Activity never writes Hifz progress, repetition,
 * scheduling or completion state. It exists so real device/BOOX rendering can be exercised
 * independently of today's scheduled task.
 */
public final class HifzDiagnosticsActivity extends android.app.Activity implements ReaderSurface.Listener {
    private static final String TEST_SABQI = "SABQI";
    private static final String TEST_ITQAN = "ITQAN";

    private final ExecutorService loader = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "quran-hifz-diagnostics");
        t.setDaemon(true);
        return t;
    });

    private HifzProgressStore progress;
    private LineGeometryRepository geometry;
    private ReaderSurface surface;
    private TextView status;
    private String testMode = TEST_SABQI;
    private int currentPage = 1;
    private int maskPercent = 25;
    private int sabqiStartLine = -1;
    private VerseRef itqanCursor;
    private GeometryRepository.AyahRegion focus;
    private List<LineGeometryRepository.PageLine> activeLines = Collections.emptyList();

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        progress = new HifzProgressStore(this);
        geometry = LineGeometryRepository.get(this);
        if (!progress.isConfigured()) {
            Toast.makeText(this, "Configurez d’abord les quatre bornes Hifz.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        buildUi();
        loadSabqi(true);
    }

    private void buildUi() {
        LinearLayout root = Ui.column(this);
        root.setPadding(0, 0, 0, 0);

        TextView title = Ui.text(this, "Tests Hifz · aucun curseur modifié", 17, true);
        title.setGravity(Gravity.CENTER);
        title.setPadding(Ui.dp(this, 8), Ui.dp(this, 4), Ui.dp(this, 8), Ui.dp(this, 3));
        root.addView(title);

        LinearLayout modes = Ui.row(this);
        modes.setPadding(Ui.dp(this, 8), 0, Ui.dp(this, 8), 0);
        Button sabqi = Ui.smallButton(this, "Sabqi 1 page", v -> loadSabqi(true));
        Button itqan = Ui.smallButton(this, "Itqān", v -> loadItqan(false));
        Ui.weight(sabqi, 1f);
        Ui.weight(itqan, 1f);
        modes.addView(sabqi);
        modes.addView(itqan);
        root.addView(modes);

        status = Ui.text(this, "Préparation…", 13, false);
        status.setPadding(Ui.dp(this, 12), 1, Ui.dp(this, 12), 2);
        root.addView(status);

        surface = new ReaderSurface(this);
        surface.setListener(this);
        root.addView(surface, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout masks = Ui.row(this);
        masks.setPadding(Ui.dp(this, 5), 0, Ui.dp(this, 5), 0);
        for (int percent : new int[]{0, 25, 50, 75, 100}) {
            Button b = Ui.smallButton(this, percent + "%", v -> {
                maskPercent = percent;
                applyOverlay();
                refreshStatus(null);
            });
            Ui.weight(b, 1f);
            masks.addView(b);
        }
        root.addView(masks);

        LinearLayout nav = Ui.row(this);
        nav.setPadding(Ui.dp(this, 8), 0, Ui.dp(this, 8), Ui.dp(this, 4));
        Button next = Ui.smallButton(this, "Cas suivant", v -> {
            if (TEST_SABQI.equals(testMode)) loadSabqi(true);
            else loadItqan(true);
        });
        Button close = Ui.smallButton(this, "Fermer", v -> finish());
        Ui.weight(next, 1f);
        Ui.weight(close, 1f);
        nav.addView(next);
        nav.addView(close);
        root.addView(nav);

        setContentView(root);
    }

    private void loadSabqi(boolean requireSinglePage) {
        testMode = TEST_SABQI;
        status.setText("Recherche d’un bloc Sabqi de 5 lignes…");
        loader.execute(() -> {
            try {
                List<LineGeometryRepository.PageLine> all = geometry.allLines();
                int start = sabqiStartLine < 0
                    ? geometry.firstLineIndex(progress.sabqiCursor())
                    : Math.min(all.size() - 5, sabqiStartLine + 5);
                int corpusEnd = geometry.firstLineIndex(progress.sabqiEnd());
                LineGeometryRepository.FiveLineBlock chosen = null;
                int chosenStart = start;
                int attempts = 0;
                for (int i = start; i + 4 < all.size() && i <= corpusEnd && attempts < 9000; i++, attempts++) {
                    LineGeometryRepository.FiveLineBlock candidate = geometry.fiveLineBlock(i);
                    if (ordinal(candidate.endVerse) > ordinal(progress.sabqiEnd())) break;
                    boolean onePage = candidate.lines.get(0).page == candidate.lines.get(candidate.lines.size() - 1).page;
                    if (!requireSinglePage || onePage) {
                        chosen = candidate;
                        chosenStart = i;
                        break;
                    }
                }
                if (chosen == null) {
                    chosenStart = geometry.firstLineIndex(progress.sabqiStart());
                    chosen = geometry.fiveLineBlock(chosenStart);
                }
                final LineGeometryRepository.FiveLineBlock block = chosen;
                final int finalStart = chosenStart;
                runOnUiThread(() -> {
                    sabqiStartLine = finalStart;
                    activeLines = block.lines;
                    currentPage = block.lines.get(0).page;
                    focus = null;
                    surface.showPage(currentPage);
                    refreshStatus("Sabqi · 5 lignes · " + block.startVerse + " → " + block.endVerse
                        + " · page " + currentPage
                        + (block.lines.get(block.lines.size() - 1).page == currentPage ? " · 1 page" : " · multi-page"));
                });
            } catch (Throwable error) {
                runOnUiThread(() -> onError(error));
            }
        });
    }

    private void loadItqan(boolean advance) {
        testMode = TEST_ITQAN;
        status.setText("Préparation d’une page Itqān…");
        loader.execute(() -> {
            try {
                EligibleCorpus corpus = EligibleCorpus.Companion.of(
                    new VerseRange(progress.itqanStart(), progress.itqanEnd()));
                VerseRef cursor = itqanCursor == null ? progress.itqanCursor() : itqanCursor;
                if (advance && itqanCursor != null) {
                    VerseRef next = QuranCanon.INSTANCE.next(itqanCursor);
                    if (next != null && corpus.contains(next)) cursor = next;
                    else cursor = progress.itqanStart();
                }
                LineGeometryRepository.VerseUnit unit = geometry.eligiblePageUnit(cursor, corpus);
                VerseRef nextCursor = unit.end;
                runOnUiThread(() -> {
                    itqanCursor = nextCursor;
                    activeLines = unit.lines;
                    currentPage = unit.page;
                    focus = null;
                    surface.showPage(currentPage);
                    refreshStatus("Itqān · page " + unit.page + " · " + unit.start + " → " + unit.end + " · ×30");
                });
            } catch (Throwable error) {
                runOnUiThread(() -> onError(error));
            }
        });
    }

    private void refreshStatus(String prefix) {
        String base = prefix;
        if (base == null) {
            base = TEST_SABQI.equals(testMode) ? "Sabqi · test rendu" : "Itqān · test rendu";
        }
        status.setText(base + " · masque " + maskPercent + "%\nAucun état Hifz n’est enregistré depuis cet écran.");
    }

    private void applyOverlay() {
        List<LineGeometryRepository.PageLine> visible = new ArrayList<>();
        for (LineGeometryRepository.PageLine line : activeLines) {
            if (line.page == currentPage) visible.add(line);
        }
        surface.setMemorizationState(visible, focus, maskPercent);
    }

    @Override public void onPageChanged(int page) {
        currentPage = page;
        applyOverlay();
    }

    @Override public void onVerseTapped(GeometryRepository.AyahRegion verse) {
        focus = verse;
        applyOverlay();
    }

    @Override public void onPageSwipe(int delta) {
        int next = Math.max(1, Math.min(604, currentPage + delta));
        if (next != currentPage) surface.showPage(next);
    }

    @Override public void onError(Throwable error) {
        Toast.makeText(this, "Test Hifz : " + error.getMessage(), Toast.LENGTH_LONG).show();
    }

    @Override public boolean onKeyDown(int code, KeyEvent event) {
        if (code == KeyEvent.KEYCODE_PAGE_UP) {
            onPageSwipe(-1);
            return true;
        }
        if (code == KeyEvent.KEYCODE_PAGE_DOWN) {
            onPageSwipe(1);
            return true;
        }
        return super.onKeyDown(code, event);
    }

    @Override protected void onDestroy() {
        loader.shutdownNow();
        if (surface != null) surface.close();
        super.onDestroy();
    }

    private static int ordinal(VerseRef ref) {
        return QuranCanon.INSTANCE.ordinal(ref);
    }
}
