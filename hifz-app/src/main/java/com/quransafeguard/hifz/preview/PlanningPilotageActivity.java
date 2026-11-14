package com.quransafeguard.hifz.preview;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.quransafeguard.hifz.core.CadenceAction;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * BOOX-friendly planning and *advisory* pilotage. All forecast scenarios are
 * independent of Hifz credits and never change the weekly programme.
 */
public final class PlanningPilotageActivity extends Activity {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.FRANCE);
    private HifzPrefs prefs;
    private DashboardLedger ledger;
    private GeometryRepository geometry;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private LinearLayout root;
    private LinearLayout body;
    private TextView yearTitle;
    private int chosenYear;
    private double scenario = 0.90;
    private List<PlanningPilotageForecast.Quarter> quarters;
    private boolean closed;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new HifzPrefs(this);
        ledger = new DashboardLedger(this);
        chosenYear = HifzClock.today().getYear();

        root = Ui.column(this);
        root.setPadding(Ui.dp(this, 20), Ui.dp(this, 9), Ui.dp(this, 20), Ui.dp(this, 9));
        root.setBackgroundColor(Ui.PAPER);
        LinearLayout header = Ui.row(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(Ui.iconButton(this, "‹", "Retour", v -> finish()));
        TextView title = Ui.bookText(this, "Planning & pilotage", 23f, true);
        title.setGravity(Gravity.CENTER);
        Ui.weight(title, 1f);
        header.addView(title);
        header.addView(new View(this), new LinearLayout.LayoutParams(Ui.dp(this, 48), Ui.dp(this, 48)));
        root.addView(header);
        root.addView(Ui.divider(this));

        body = Ui.column(this);
        root.addView(body, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(root);
        Ui.respectSystemBars(this, root, 0, 0, 0, 0);
        info("Lecture du suivi QCF…");
        io.execute(() -> {
            try {
                GeometryRepository loaded = GeometryRepository.get(getApplicationContext());
                runOnUiThread(() -> {
                    if (closed || isFinishing() || isDestroyed()) return;
                    geometry = loaded;
                    render();
                });
            } catch (Exception unavailable) {
                runOnUiThread(() -> {
                    if (closed || isFinishing() || isDestroyed()) return;
                    body.removeAllViews();
                    info("Prévisions indisponibles : géométrie QCF manquante.");
                });
            }
        });
    }

    private TextView label(String content, int size, boolean bold) {
        TextView text = Ui.bookText(this, content, size, bold);
        text.setPadding(Ui.dp(this, 2), Ui.dp(this, 3), Ui.dp(this, 2), Ui.dp(this, 3));
        return text;
    }

    private void info(String text) {
        TextView v = Ui.text(this, text, 14f, false);
        v.setTextColor(Ui.MUTED);
        body.addView(v);
    }

    private void render() {
        body.removeAllViews();
        final LocalDate today = HifzClock.today();
        final int start = prefs.sabqiLineCursor() >= 0 ? prefs.sabqiLineCursor()
            : geometry.firstLineIndex(prefs.sabqiStart());
        // Read the persisted rotation. Opening this advisory page must not
        // bootstrap, consume or advance a real Itqan lesson.
        ItqanRotationPolicy.State rotation = prefs.itqanRotationState();
        final boolean postNas = prefs.itqanPostNasMaintenance();
        final boolean tail = !postNas
            && rotation.leg == ItqanRotationPolicy.Leg.TAIL_HUJURAT_NAS;
        final int itqanPage = tail ? geometry.pageForVerse(rotation.cursor) : 604;
        quarters = PlanningPilotageForecast.quarters(geometry, today, start,
            itqanPage, !tail, 3, scenario);

        body.addView(label("DÉMARRAGE  " + prefs.programStartDate().format(DATE), 15, true));
        info("Programme actuel : Sabqi lun./mer./ven. · Itqān mar./jeu./sam.");
        info("Simulation uniquement : aucune séance ni aucun acquis n'est modifié.");

        LocalDate coverage = ledger.coverageStartDate();
        if (coverage.isBefore(prefs.programStartDate())) coverage = prefs.programStartDate();
        final LocalDate end = today.minusDays(1);
        int planned = 0;
        int done = 0;
        if (!coverage.isAfter(end)) {
            Set<LocalDate> receipts = new HashSet<>(ledger.completedDates(
                HifzSessionActivity.SABQI, coverage, end));
            for (LocalDate date = coverage; !date.isAfter(end); date = date.plusDays(1)) {
                if (WeeklyProgramPolicy.actionFor(date.getDayOfWeek(), prefs.weeklyProgramFor(date),
                        prefs.learningDaysPerWeek()) != CadenceAction.LEARNING) continue;
                planned++;
                if (receipts.contains(date)) done++;
            }
        }
        String stats = planned == 0 ? "Assiduité Sabqi : historique insuffisant"
            : "Assiduité Sabqi vérifiée : " + done + "/" + planned
                + " (" + (done * 100 / planned) + "%)";
        body.addView(label(stats, 16, true));
        info("Reçus couverts depuis le " + coverage.format(DATE)
            + " · Itqān : ne pas compter une page comme 3 séances.");

        LinearLayout choice = Ui.row(this);
        choice.setGravity(Gravity.CENTER_VERTICAL);
        choice.addView(label("HYPOTHÈSE", 15, true), new LinearLayout.LayoutParams(0, -2, 1f));
        for (int percent : new int[]{80, 90, 100}) {
            final double value = percent / 100.0;
            TextView item = label(percent + " %", 15, scenario == value);
            item.setPadding(Ui.dp(this, 10), Ui.dp(this, 8), Ui.dp(this, 10), Ui.dp(this, 8));
            item.setContentDescription("Simuler " + percent + " pour cent d'assiduité");
            item.setMinimumHeight(Ui.dp(this, 44));
            item.setClickable(true);
            item.setOnClickListener(v -> {
                scenario = value;
                render();
            });
            choice.addView(item);
        }
        body.addView(choice);
        body.addView(Ui.divider(this));

        int finalYear = quarters.get(quarters.size() - 1).date.getYear();
        if (chosenYear < today.getYear()) chosenYear = today.getYear();
        if (chosenYear > finalYear) chosenYear = finalYear;
        LinearLayout nav = Ui.row(this);
        nav.setGravity(Gravity.CENTER_VERTICAL);
        TextView previous = label("‹", 28, true);
        previous.setGravity(Gravity.CENTER);
        previous.setMinimumWidth(Ui.dp(this, 52));
        previous.setMinimumHeight(Ui.dp(this, 44));
        previous.setEnabled(chosenYear > today.getYear());
        previous.setOnClickListener(v -> { chosenYear--; render(); });
        nav.addView(previous);
        yearTitle = label(Integer.toString(chosenYear), 22, true);
        yearTitle.setGravity(Gravity.CENTER);
        nav.addView(yearTitle, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView next = label("›", 28, true);
        next.setGravity(Gravity.CENTER);
        next.setMinimumWidth(Ui.dp(this, 52));
        next.setMinimumHeight(Ui.dp(this, 44));
        next.setEnabled(chosenYear < finalYear);
        next.setOnClickListener(v -> { chosenYear++; render(); });
        nav.addView(next);
        body.addView(nav);
        info("FIN DE TRIMESTRE     ·     POSITION PRÉVISIONNELLE");
        for (PlanningPilotageForecast.Quarter row : quarters) {
            if (row.date.getYear() != chosenYear) continue;
            addQuarter(row);
        }
        body.addView(Ui.divider(this));
        PlanningPilotageForecast.Quarter last = quarters.get(quarters.size() - 1);
        body.addView(label("Fin du nouveau Sabqi : " + last.date.format(DATE)
            + " (trimestre estimé)", 15, true));
        info("Itqān : après An-Nās, le Maintien continue ; ce n'est pas une fin de consolidation.");
    }

    private void addQuarter(PlanningPilotageForecast.Quarter q) {
        LinearLayout row = Ui.column(this);
        row.setPadding(Ui.dp(this, 5), Ui.dp(this, 5), Ui.dp(this, 5), Ui.dp(this, 5));
        row.addView(label(q.date.format(DATE), 15, true));
        row.addView(label("Sabqi · " + position(q.sabqi), 14, false));
        if (q.firstTailComplete) row.addView(label("Itqān · Maintien après An-Nās", 14, false));
        else if (q.itqan != null) row.addView(label("Itqān · " + position(q.itqan), 14, false));
        else row.addView(label("Itqān · Rotation en cours", 14, false));
        body.addView(row);
        body.addView(Ui.divider(this));
    }

    private static String position(com.quransafeguard.hifz.core.VerseRef ref) {
        return QuranSurahNames.name(ref.getSurah()) + " " + ref.getAyah()
            + " · Juz " + PlanningPilotageForecast.juz(ref)
            + " · Hizb " + PlanningPilotageForecast.hizb(ref);
    }

    @Override protected void onDestroy() {
        closed = true;
        io.shutdownNow();
        super.onDestroy();
    }
}
