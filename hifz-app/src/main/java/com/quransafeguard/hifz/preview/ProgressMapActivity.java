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

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Whole-Mushaf overview: one cell per page, patterned (never colored) by status, so it stays
 * legible on e-ink. Reported directly (twice): a page's status used to be read off historical
 * range-based promotion buckets that only the Sabqi/Renforcement track ever updates, so crediting
 * an Itqan/Stabilisation line never moved anything here. Switching straight to the per-line schema6
 * state (STABILIZED = à stabiliser) fixed that but broke something else: a declared Itqan/Ancrage
 * range (itqanRanges()/unconsolidatedPromotedRanges(), set once in Settings or at install) starts
 * every one of its lines in state NONE — completeStabilizationBlockV6's own comment confirms NONE
 * is "Ancrage material['s] normal starting state" — so a huge declared range not yet individually
 * touched by a real Stabilisation block showed as "pas commencé" instead of "à stabiliser",
 * indistinguishable from a page never declared for any track at all.
 *
 * A page's status now combines both signals correctly: ACQUIRED (progressionSnapshotV6) = Acquis;
 * declared Itqan-eligible (effectiveItqanRanges = itqanRanges + every Sabqi-fed promotion) but not
 * yet ACQUIRED = à stabiliser, whatever its NONE/LEARNED/STABILIZED sub-state; Sabqi's own LEARNED
 * = en apprentissage; neither = pas commencé. This is the only page status that still needs a
 * declared-range check — Sabqi's own front-to-back walk has no equivalent "declared but not yet
 * reached" backlog, since its LEARNED state is set the moment each day's lesson happens. Two live
 * estimates (Apprentissage, Stabilisation) sit above the grid, computed from the same buckets
 * Diagnostic uses for its own equivalent estimates.
 */
public final class ProgressMapActivity extends android.app.Activity {
    private HifzPrefs prefs;
    private GeometryRepository geometry;
    private ProgressGridView grid;
    private TextView apprentissageEtaValue;
    private TextView stabilisationEtaValue;
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

        apprentissageEtaValue = etaBox(root, "Estimation fin Apprentissage");
        stabilisationEtaValue = etaBox(root, "Estimation fin Stabilisation");

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

    /** A bordered panel holding a title and a value line, filled in once loadStatuses() finishes. */
    private TextView etaBox(LinearLayout root, String title) {
        LinearLayout box = Ui.column(this);
        Ui.panel(box);
        box.addView(Ui.text(this, title, 13f, true));
        TextView value = Ui.text(this, "Calcul en cours…", 12.5f, false);
        value.setTextColor(Ui.MUTED);
        value.setPadding(0, Ui.dp(this, 2), 0, 0);
        box.addView(value);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = Ui.dp(this, 8);
        root.addView(box, params);
        return value;
    }

    /**
     * Touches every one of the ~8820 physical lines (a HashSet lookup each, against
     * progressionSnapshotV6's three sets) — cheap on a phone, but this app targets weak e-ink CPUs
     * too, so it runs off the main thread rather than risk a visible hitch opening this screen.
     */
    private void loadStatuses() {
        io.execute(() -> {
            int[] statuses;
            String apprentissageEta;
            String stabilisationEta;
            try {
                statuses = computeStatuses();
                apprentissageEta = weeksEtaSummary(prefs.sabqiLinesRemaining(geometry),
                    PreviewConfig.SABQI_LINES * prefs.learningDaysPerWeek());
                stabilisationEta = weeksEtaSummary(prefs.stabilizationLinesRemaining(geometry),
                    PreviewConfig.STABILIZATION_WEEKLY_LINES);
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
                apprentissageEtaValue.setText(apprentissageEta);
                stabilisationEtaValue.setText(stabilisationEta);
            });
        });
    }

    /**
     * Same shape as SettingsActivity's Diagnostic estimates: remaining lines divided by a weekly
     * pace, ceiling-rounded, projected from today.
     */
    private static String weeksEtaSummary(int remaining, int weeklyPace) {
        if (remaining == 0) return "à jour";
        int weeks = (remaining + weeklyPace - 1) / weeklyPace;
        return remaining + " ligne(s) restante(s) · ~" + weeks + " semaine(s) · ~" + HifzClock.today().plusWeeks(weeks);
    }

    private int[] computeStatuses() {
        HifzPrefs.ProgressionSnapshot progression = prefs.progressionSnapshotV6();
        ArrayList<GeometryRepository.LineMeta> allLines = new ArrayList<>();
        for (int i = 0; i < geometry.lineCount(); i++) allLines.add(geometry.line(i));
        // Declared Itqan-eligible corpus (the base range plus every Sabqi-fed promotion) — a line
        // in here starts in state NONE (see class doc) and stays "à stabiliser" through NONE,
        // LEARNED and STABILIZED alike, until it actually reaches ACQUIRED.
        Set<String> itqanEligible = CorpusLinePolicy.ownedLineIds(prefs.effectiveItqanRanges(), allLines);
        int[] statuses = new int[605];

        for (int page = 1; page <= 604; page++) {
            boolean anyStabiliser = false, anyApprentissage = false, anyAcquis = false;
            for (String lineId : geometry.lineIdsOnPage(page)) {
                if (progression.acquired.contains(lineId)) anyAcquis = true;
                else if (itqanEligible.contains(lineId)) anyStabiliser = true;
                else if (progression.learned.contains(lineId)) anyApprentissage = true;
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
}
