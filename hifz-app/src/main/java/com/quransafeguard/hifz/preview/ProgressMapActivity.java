package com.quransafeguard.hifz.preview;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
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
    private Button backButton;
    private LinearLayout juzOverview, pageDetails;
    private TextView juzHeading;
    private final JuzProgressBar[] juzBars=new JuzProgressBar[30];
    private final LinearLayout[] juzCards=new LinearLayout[30];
    private int selectedJuz=0;
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
        backButton=Ui.iconButton(this,"‹","Retour",v->{
            if(selectedJuz>0)showAllPages();else finish();
        });
        top.addView(backButton);
        TextView title = Ui.bookText(this, "Progression", 18, true);
        Ui.weight(title, 1f);
        title.setGravity(Gravity.CENTER);
        top.addView(title);
        TextView balance = Ui.text(this, "", 1f, false);
        balance.setMinWidth(Ui.dp(this, 44));
        top.addView(balance, new LinearLayout.LayoutParams(Ui.dp(this, 44), Ui.dp(this, 44)));
        root.addView(top);

        // 30 printed-page gauges, then the actual QCF pages of the selected Juz.
        // No explanatory banner, redundant selection dialog, or extra grouping.
        juzOverview=Ui.column(this);
        juzOverview.setPadding(0,0,0,0);
        root.addView(juzOverview);
        buildJuzOverview();

        pageDetails=Ui.column(this);
        pageDetails.setPadding(0,0,0,0);
        pageDetails.setVisibility(View.GONE);
        juzHeading=Ui.bookText(this,"",18f,true);
        juzHeading.setPadding(Ui.dp(this,8),Ui.dp(this,8),0,0);
        pageDetails.addView(juzHeading);
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
        grid.setContentDescription("Pages du juz");
        if(savedInstanceState!=null){
            int restored=savedInstanceState.getInt("freeze2SelectedJuz",0);
            if(restored>=1 && restored<=30)showJuz(restored);
        }

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
    /** Miniature pages ARE the 30 gauges. Six across on BOOX, three on phones. */
    private void buildJuzOverview() {
        boolean boox=getResources().getConfiguration().smallestScreenWidthDp>=600;
        int cols=boox?6:3;
        LinearLayout row=null;
        for(int j=1;j<=30;j++){
            if((j-1)%cols==0){
                row=Ui.row(this);
                row.setPadding(0,0,0,0);
                juzOverview.addView(row);
            }
            int index=j-1;
            final int selected=j;
            LinearLayout item=Ui.column(this);
            item.setPadding(Ui.dp(this,4),Ui.dp(this,5),Ui.dp(this,4),Ui.dp(this,7));
            item.setGravity(Gravity.CENTER_HORIZONTAL);
            item.setBackgroundColor(Ui.PAPER);
            item.setClickable(true);
            item.setFocusable(true);
            item.setMinimumHeight(Ui.dp(this,96));
            item.setOnClickListener(v->showJuz(selected));

            JuzProgressBar page=new JuzProgressBar(this);
            page.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
            item.addView(page);
            TextView label=Ui.bookText(this,"Juz "+j,boox?16f:14f,true);
            label.setGravity(Gravity.CENTER);
            label.setPadding(0,Ui.dp(this,3),0,0);
            item.addView(label);
            juzCards[index]=item;
            juzBars[index]=page;
            item.setContentDescription("Juz "+j);

            LinearLayout.LayoutParams cell=new LinearLayout.LayoutParams(
                0,LinearLayout.LayoutParams.WRAP_CONTENT,1f);
            cell.setMargins(Ui.dp(this,2),0,Ui.dp(this,2),Ui.dp(this,3));
            row.addView(item,cell);
        }
    }

    /** Replace the 30-entry overview with the exact QCF page range for this Juz. */
    private void showJuz(int juz) {
        int first=juzStartPage(juz),last=juz==30?604:juzStartPage(juz+1)-1;
        selectedJuz=juz;
        grid.setPageWindow(first,last);
        juzOverview.setVisibility(View.GONE);
        pageDetails.setVisibility(View.VISIBLE);

        juzHeading.setText("Juz "+juz);
        Ui.setIconDescription(backButton,"Retour aux 30 juz");
        grid.setContentDescription("Juz "+juz+" · pages "+first+" à "+last);
        if(progressScroll!=null)progressScroll.post(()->progressScroll.scrollTo(0,0));
    }
    /** A single back step to the 30 gauges, not to a redundant 604-page map. */
    private void showAllPages() {
        selectedJuz=0;
        grid.setPageWindow(1,604); // restore the internal view before hiding it
        pageDetails.setVisibility(View.GONE);
        juzOverview.setVisibility(View.VISIBLE);

        Ui.setIconDescription(backButton,"Retour");
        grid.setContentDescription("Pages du juz");
        if(progressScroll!=null)progressScroll.post(()->progressScroll.scrollTo(0,0));
    }

    @Override public void onBackPressed() {
        if(selectedJuz>0)showAllPages();
        else super.onBackPressed();
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

    /**
     * Touches every one of the ~8820 physical lines (a HashSet lookup each, against
     * progressionSnapshotV6's three sets) — cheap on a phone, but this app targets weak e-ink CPUs
     * too, so it runs off the main thread rather than risk a visible hitch opening this screen.
     */
    private void loadStatuses() {
        io.execute(() -> {
            int[] statuses;
            int[][] juzCounts;
            try {
                statuses = computeStatuses();
                // Read-only line levels, each physical QCF line accounted for once.
                juzCounts=JuzProgressSummary.summarize(
                    geometry,prefs.progressionSnapshotV6());
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
                    int[] c=juzCounts[i];
                    int total=c[JuzProgressSummary.TOTAL];
                    int learned=c[JuzProgressSummary.LEARNED];
                    int stabilized=c[JuzProgressSummary.STABILIZED];
                    int acquired=c[JuzProgressSummary.ACQUIRED];
                    juzBars[i].setLevels(learned,stabilized,acquired,total);
                }
            });
        });
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
