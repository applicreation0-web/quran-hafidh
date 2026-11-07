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
    private ScrollView progressScroll;
    private LinearLayout juzOverview, pageDetails;
    private TextView gridHelp, juzHeading;
    private final JuzProgressBar[] juzBars=new JuzProgressBar[30];
    private final TextView[] juzPercentLabels=new TextView[30];
    private final TextView[] juzCountLabels=new TextView[30];
    private final LinearLayout[] juzCards=new LinearLayout[30];
    private int selectedJuz=0;
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
        progressScroll=scroll;
        scroll.setFillViewport(true);
        LinearLayout root = Ui.column(this);
        scroll.addView(root);

        LinearLayout top = Ui.row(this);
        top.setPadding(0, 0, 0, Ui.dp(this, 2));
        top.addView(Ui.iconButton(this, "‹", "Retour", v -> finish()));
        TextView title = Ui.bookText(this, "Progression", 18, true);
        Ui.weight(title, 1f);
        title.setGravity(Gravity.CENTER);
        top.addView(title);
        TextView balance = Ui.text(this, "", 1f, false);
        balance.setMinWidth(Ui.dp(this, 44));
        top.addView(balance, new LinearLayout.LayoutParams(Ui.dp(this, 44), Ui.dp(this, 44)));
        root.addView(top);

        // Single useful hierarchy: 30 canonical Juz with real V6 acquisition gauges,
        // then the authentic numbered physical pages of ONE selected Juz.
        // No additional classification, dialog picker, or default 604-cell wall.
        gridHelp = Ui.text(this,
            "30 juz · toucher un juz pour afficher ses pages", 14f, false);
        if (getResources().getConfiguration().smallestScreenWidthDp >= 600)
            gridHelp.setTextSize(19f);
        gridHelp.setTextColor(Ui.MUTED);
        gridHelp.setPadding(Ui.dp(this, 4), Ui.dp(this, 10), Ui.dp(this, 4), Ui.dp(this, 5));
        root.addView(gridHelp);

        juzOverview=Ui.column(this);
        juzOverview.setPadding(0,0,0,0);
        root.addView(juzOverview);
        buildJuzOverview();

        pageDetails=Ui.column(this);
        pageDetails.setPadding(0,0,0,0);
        pageDetails.setVisibility(View.GONE);
        LinearLayout pageHeader=Ui.row(this);
        pageHeader.addView(Ui.smallButton(this,"Tous les juz",v->showAllPages()));
        juzHeading=Ui.bookText(this,"",18f,true);
        juzHeading.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        pageHeader.addView(juzHeading,new LinearLayout.LayoutParams(
            0,Ui.dp(this,48),1f));
        pageDetails.addView(pageHeader);
        pageDetails.addView(legend());
        grid = new ProgressGridView(this);
        grid.setOnPageTapped(page -> {
            Intent intent = new Intent(this, StudyReaderActivity.class);
            intent.putExtra(StudyReaderActivity.EXTRA_JUMP_PAGE, page);
            startActivity(intent);
        });
        LinearLayout.LayoutParams gridParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        gridParams.topMargin = Ui.dp(this, 4);
        pageDetails.addView(grid, gridParams);
        root.addView(pageDetails);
        grid.setContentDescription("Pages du juz · toucher pour ouvrir en Lecture");
        if(savedInstanceState!=null){
            int restored=savedInstanceState.getInt("freeze2SelectedJuz",0);
            if(restored>=1 && restored<=30)showJuz(restored);
        }

        // Estimates follow the grid. The decoding legend stays above, visible at entry.
        root.addView(Ui.divider(this));
        apprentissageEtaValue = etaBox(root, "Fin Apprentissage");
        stabilisationEtaValue = etaBox(root, "Fin Stabilisation");

        setContentView(scroll);
        Ui.respectSystemBars(this, scroll, 0, 0, 0, 0);
        loadStatuses();
    }

    @Override protected void onSaveInstanceState(Bundle out) {
        out.putInt("freeze2SelectedJuz",selectedJuz);
        super.onSaveInstanceState(out);
    }

    private static int juzStartPage(int juz) {
        if(juz<1||juz>30)throw new IllegalArgumentException("Juz must be in 1..30");
        // Canonical indexed QCF 240 rub' boundary table; eight rub' per Juz.
        return QuranRubBoundaries.rowForIndex((juz-1)*8+1)[3];
    }
    /** Exactly 30 tappable Juz entries, two columns on BOOX, one on phones.
     * No invented groups and no computed completion until V6 snapshot is loaded. */
    private void buildJuzOverview() {
        boolean tablet=getResources().getConfiguration().smallestScreenWidthDp>=600;
        LinearLayout currentRow=null;
        for(int j=1;j<=30;j++){
            if(tablet && (j-1)%2==0){
                currentRow=Ui.row(this);
                currentRow.setPadding(0,0,0,0);
                juzOverview.addView(currentRow);
            }
            int index=j-1;
            int first=juzStartPage(j);
            int last=j==30?604:juzStartPage(j+1)-1;
            LinearLayout item=Ui.column(this);
            item.setPadding(Ui.dp(this,tablet?12:8),Ui.dp(this,10),
                Ui.dp(this,tablet?12:8),Ui.dp(this,10));
            item.setBackgroundColor(Ui.PAPER);
            item.setMinimumHeight(Ui.dp(this,tablet?102:92));
            item.setClickable(true);
            item.setFocusable(true);
            item.setContentDescription("Juz "+j+" · pages "+first+" à "+last+
                " · progression en cours de calcul · toucher pour afficher les pages");
            int selected=j;
            item.setOnClickListener(v->showJuz(selected));
            LinearLayout heading=Ui.row(this);
            heading.setGravity(Gravity.CENTER_VERTICAL);
            TextView name=Ui.bookText(this,"Juz "+j,tablet?20f:18f,true);
            heading.addView(name,new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT,1f));
            TextView percent=Ui.text(this,"—",tablet?19f:17f,true);
            heading.addView(percent);
            item.addView(heading);
            JuzProgressBar bar=new JuzProgressBar(this);
            LinearLayout.LayoutParams barParams=new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,Ui.dp(this,10));
            barParams.topMargin=Ui.dp(this,7);
            barParams.bottomMargin=Ui.dp(this,5);
            item.addView(bar,barParams);
            TextView info=Ui.text(this,
                "Pages "+first+"–"+last+" · calcul en cours",
                tablet?15f:13f,false);
            info.setTextColor(Ui.MUTED);
            item.addView(info);
            juzCards[index]=item;
            juzBars[index]=bar;
            juzPercentLabels[index]=percent;
            juzCountLabels[index]=info;
            if(tablet){
                LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(
                    0,LinearLayout.LayoutParams.WRAP_CONTENT,1f);
                params.setMargins(Ui.dp(this,3),0,Ui.dp(this,3),Ui.dp(this,3));
                currentRow.addView(item,params);
            }else{
                juzOverview.addView(item);
                juzOverview.addView(Ui.divider(this));
            }
        }
    }

    /** Replace the 30-entry overview with the exact QCF page range for this Juz. */
    private void showJuz(int juz) {
        int first=juzStartPage(juz),last=juz==30?604:juzStartPage(juz+1)-1;
        selectedJuz=juz;
        grid.setPageWindow(first,last);
        juzOverview.setVisibility(View.GONE);
        pageDetails.setVisibility(View.VISIBLE);
        gridHelp.setText("Juz "+juz+" · toucher une page pour ouvrir le Mushaf");
        juzHeading.setText("Pages "+first+"–"+last);
        grid.setContentDescription("Juz "+juz+" · pages physiques "+first+" à "+last+
            " · toucher une page pour ouvrir le Mushaf");
        if(progressScroll!=null)progressScroll.post(()->progressScroll.scrollTo(0,0));
    }
    /** A single back step to the 30 gauges, not to a redundant 604-page map. */
    private void showAllPages() {
        selectedJuz=0;
        grid.setPageWindow(1,604); // restore the internal view before hiding it
        pageDetails.setVisibility(View.GONE);
        juzOverview.setVisibility(View.VISIBLE);
        gridHelp.setText("30 juz · toucher un juz pour afficher ses pages");
        grid.setContentDescription("Pages masquées · choisir un juz");
        if(progressScroll!=null)progressScroll.post(()->progressScroll.scrollTo(0,0));
    }

    private LinearLayout legend() {
        LinearLayout box = Ui.column(this);
        box.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 4));
        LinearLayout line = null;
        int[] statuses = {ProgressGridView.ACQUIS, ProgressGridView.STABILISER, ProgressGridView.APPRENTISSAGE, ProgressGridView.VIDE};
        String[] labels = {"Acquis", "À stabiliser", "En apprentissage", "Pas commencé"};
        for (int i = 0; i < statuses.length; i++) {
            LinearLayout row = Ui.row(this);
            row.setPadding(Ui.dp(this, 4), 0, Ui.dp(this, 8), 0);
            row.setGravity(Gravity.CENTER_VERTICAL);
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
            boolean wideBoox = getResources().getConfiguration().smallestScreenWidthDp >= 600;
            int size = Ui.dp(this, wideBoox ? 22 : 14);
            row.addView(swatch, new LinearLayout.LayoutParams(size, size));
            TextView label = Ui.text(this, labels[i], wideBoox ? 18f : 11f, false);
            label.setTextColor(Ui.MUTED);
            label.setPadding(Ui.dp(this, 4), 0, 0, 0);
            row.addView(label);
            if (i % 2 == 0) {
                line = Ui.row(this);
                line.setGravity(Gravity.CENTER);
                box.addView(line);
            }
            line.addView(row, new LinearLayout.LayoutParams(0, Ui.dp(this, wideBoox ? 36 : 24), 1f));
        }
        return box;
    }

    /** "Fin Apprentissage ········ value": a plain line under a hairline, never a bordered panel. */
    private TextView etaBox(LinearLayout root, String title) {
        LinearLayout row = Ui.row(this);
        row.setPadding(Ui.dp(this, 2), Ui.dp(this, 6), Ui.dp(this, 2), Ui.dp(this, 6));
        TextView label = Ui.text(this, title, 12.5f, true);
        Ui.weight(label, 1f);
        row.addView(label);
        TextView value = Ui.text(this, "Calcul en cours…", 12f, false);
        value.setTextColor(Ui.MUTED);
        value.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        row.addView(value);
        root.addView(row);
        root.addView(Ui.divider(this));
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
            int[][] juzCounts;
            try {
                statuses = computeStatuses();
                apprentissageEta = weeksEtaSummary(prefs.sabqiLinesRemaining(geometry),
                    PreviewConfig.SABQI_LINES * prefs.learningDaysPerWeek());
                stabilisationEta = weeksEtaSummary(prefs.stabilizationLinesRemaining(geometry),
                    PreviewConfig.STABILIZATION_WEEKLY_LINES);
                // Read-only acquisition: only entirely acquired verses increase a Juz gauge.
                juzCounts=JuzProgressSummary.summarize(
                    geometry,prefs.progressionSnapshotV6().acquired);
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
                for(int i=0;i<30;i++){
                    int total=juzCounts[i][0],acquired=juzCounts[i][1];
                    int percent=JuzProgressSummary.percent(acquired,total);
                    int juz=i+1,first=juzStartPage(juz);
                    int last=juz==30?604:juzStartPage(juz+1)-1;
                    juzBars[i].setPercent(percent);
                    juzPercentLabels[i].setText(percent+" %");
                    juzCountLabels[i].setText("Pages "+first+"–"+last+" · "
                        +acquired+"/"+total+" versets acquis");
                    juzCards[i].setContentDescription("Juz "+juz+
                        " · "+percent+" pour cent · "+acquired+" sur "+total+
                        " versets entièrement acquis · toucher pour afficher ses pages");
                }
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
