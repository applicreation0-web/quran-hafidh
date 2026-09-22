package com.quransafeguard.hifz.preview;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.quransafeguard.hifz.core.VerseRange;
import com.quransafeguard.hifz.core.VerseRef;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Whole-Mushaf overview: one cell per page, patterned (never colored) by status, so it stays
 * legible on e-ink. A page's status is read straight off the same corpus buckets Diagnostic
 * already reports (itqanRanges/consolidatedPromotedRanges = Acquis, unconsolidatedPromotedRanges
 * = à stabiliser, everything up to sabqiEnd() not yet promoted = en apprentissage), so this view
 * can never drift from what those numbers already mean elsewhere in the app.
 */
public final class ProgressMapActivity extends android.app.Activity {
    private HifzPrefs prefs;
    private GeometryRepository geometry;
    private ProgressGridView grid;
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new HifzPrefs(this);
        try {
            geometry = GeometryRepository.get(this);
        } catch (Throwable error) {
            Ui.showFatal(this, "La géométrie du Mushaf est indisponible. Fermez puis rouvrez l’application.");
            return;
        }

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout root = Ui.column(this);
        scroll.addView(root);

        LinearLayout top = Ui.row(this);
        top.setPadding(0, 0, 0, Ui.dp(this, 2));
        top.addView(Ui.iconButton(this, "‹", "Retour", v -> finish()));
        TextView title = Ui.bookText(this, "Carte de progression", 18, true);
        Ui.weight(title, 1f);
        title.setGravity(Gravity.CENTER);
        top.addView(title);
        TextView balance = Ui.text(this, "", 1f, false);
        balance.setMinWidth(Ui.dp(this, 44));
        top.addView(balance, new LinearLayout.LayoutParams(Ui.dp(this, 44), Ui.dp(this, 44)));
        root.addView(top);

        TextView subtitle = Ui.text(this, "604 pages du Mushaf, dans l’ordre canonique.", 12.5f, false);
        subtitle.setTextColor(Ui.MUTED);
        subtitle.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 14));
        root.addView(subtitle);

        root.addView(legend());

        grid = new ProgressGridView(this);
        grid.setOnPageTapped(page -> {
            Intent intent = new Intent(this, StudyReaderActivity.class);
            intent.putExtra(StudyReaderActivity.EXTRA_JUMP_PAGE, page);
            startActivity(intent);
        });
        LinearLayout.LayoutParams gridParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        gridParams.topMargin = Ui.dp(this, 4);
        root.addView(grid, gridParams);

        TextView caption = Ui.text(this,
            "Chaque case reste identifiable par sa trame, jamais par une teinte — lisible sans ghosting en niveaux de gris purs. Touchez une page pour l’ouvrir en Lecture.",
            11.5f, false);
        caption.setTextColor(Ui.MUTED);
        caption.setPadding(0, Ui.dp(this, 12), 0, 0);
        root.addView(caption);

        setContentView(scroll);
        Ui.respectSystemBars(this, scroll, 0, 0, 0, 0);
        loadStatuses();
    }

    private LinearLayout legend() {
        LinearLayout box = Ui.column(this);
        box.setPadding(0, 0, 0, Ui.dp(this, 10));
        int[] statuses = {ProgressGridView.ACQUIS, ProgressGridView.STABILISER, ProgressGridView.APPRENTISSAGE, ProgressGridView.VIDE};
        String[] labels = {"Acquis", "À stabiliser", "En apprentissage", "Pas commencé"};
        for (int i = 0; i < statuses.length; i++) {
            LinearLayout row = Ui.row(this);
            row.setPadding(0, Ui.dp(this, 2), 0, Ui.dp(this, 2));
            final int status = statuses[i];
            View swatch = new View(this) {
                @Override protected void onDraw(Canvas canvas) {
                    Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
                    fill.setColor(Ui.INK);
                    Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
                    stroke.setStyle(Paint.Style.STROKE);
                    stroke.setColor(Ui.LINE);
                    ProgressGridView.drawCell(canvas, new RectF(1, 1, getWidth() - 1, getHeight() - 1), status, fill, stroke);
                }
            };
            int size = Ui.dp(this, 20);
            row.addView(swatch, new LinearLayout.LayoutParams(size, size));
            TextView label = Ui.text(this, labels[i], 12.5f, false);
            label.setPadding(Ui.dp(this, 8), 0, 0, 0);
            row.addView(label);
            box.addView(row);
        }
        return box;
    }

    /**
     * Touches every one of the ~8820 physical lines several times over (lineIdsOnPage/
     * linesForExactIds are both linear scans, called once per page) — cheap on a phone, but this
     * app targets weak e-ink CPUs too, so it runs off the main thread rather than risk a visible
     * hitch opening this screen.
     */
    private void loadStatuses() {
        io.execute(() -> {
            int[] statuses;
            try {
                statuses = computeStatuses();
            } catch (RuntimeException corruptOrUnconfiguredState) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    Toast.makeText(this, "Progression indisponible · ouvrez Diagnostic si le problème persiste.", Toast.LENGTH_LONG).show();
                });
                return;
            }
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                for (int page = 1; page <= 604; page++) grid.setStatus(page, statuses[page]);
                grid.invalidate();
            });
        });
    }

    private int[] computeStatuses() {
        List<VerseRange> acquis = new ArrayList<>(prefs.itqanRanges());
        acquis.addAll(prefs.consolidatedPromotedRanges());
        List<VerseRange> stabiliser = prefs.unconsolidatedPromotedRanges();
        VerseRef sabqiEnd;
        try { sabqiEnd = prefs.sabqiEnd(); } catch (RuntimeException notYetInitialized) { sabqiEnd = null; }
        int sabqiEndOrdinal = sabqiEnd == null ? -1 : GeometryRepository.ordinal(sabqiEnd);
        int[] statuses = new int[605];

        for (int page = 1; page <= 604; page++) {
            List<GeometryRepository.LineMeta> lines = geometry.linesForExactIds(geometry.lineIdsOnPage(page));
            boolean anyStabiliser = false, anyApprentissage = false, anyAcquis = false;
            for (GeometryRepository.LineMeta line : lines) {
                for (VerseRef verse : line.verses) {
                    if (containsVerse(stabiliser, verse)) anyStabiliser = true;
                    else if (containsVerse(acquis, verse)) anyAcquis = true;
                    else if (GeometryRepository.ordinal(verse) <= sabqiEndOrdinal) anyApprentissage = true;
                }
            }
            statuses[page] = anyStabiliser ? ProgressGridView.STABILISER
                : anyApprentissage ? ProgressGridView.APPRENTISSAGE
                : anyAcquis ? ProgressGridView.ACQUIS
                : ProgressGridView.VIDE;
        }
        return statuses;
    }

    @Override protected void onDestroy() {
        io.shutdownNow();
        super.onDestroy();
    }

    private static boolean containsVerse(List<VerseRange> ranges, VerseRef verse) {
        for (VerseRange range : ranges) if (range.contains(verse)) return true;
        return false;
    }
}
