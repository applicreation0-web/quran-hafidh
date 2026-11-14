package com.quransafeguard.hifz.preview;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.quransafeguard.hifz.core.CadenceAction;
import com.quransafeguard.hifz.core.VerseRef;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiPredicate;

import static com.quransafeguard.hifz.preview.CapHifzEvidence.Track;

public final class CapHifzActivity extends Activity {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.FRANCE);
    private static final DateTimeFormatter DAY_MONTH = DateTimeFormatter.ofPattern("d MMM", Locale.FRANCE);
    private static final String[] TRACKS = {"Sabqi", "Itqān", "Active matin", "Active soir"};

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private HifzPrefs prefs;
    private DashboardLedger ledger;
    private PostFinalRecallStore finals;
    private LinearLayout body;
    private GeometryRepository geometry;
    private LocalDate today;
    private LocalDate monday;
    private LocalDate selectedWeek;
    private List<CapHifzEvidence.Lesson> lessons;
    private Set<String> acquired;
    private Map<String, PostFinalRecallStore.LineDates> finalDates;
    private Map<String, LocalDate> learningDates;
    private Map<String, LocalDate> stabilizationDates;
    private BiPredicate<Track, LocalDate> received;
    private int year;
    private int scenario = 90;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = HifzPrefs.readOnly(this);
        ledger = DashboardLedger.readOnly(this);
        finals = new PostFinalRecallStore(this);
        today = HifzClock.today();
        monday = today.minusDays(today.getDayOfWeek().getValue() - 1);
        year = state == null ? today.getYear() : state.getInt("year", today.getYear());
        scenario = state == null ? 90 : state.getInt("scenario", 90);
        long week = state == null ? monday.toEpochDay() : state.getLong("week", monday.toEpochDay());
        selectedWeek = LocalDate.ofEpochDay(week);

        LinearLayout root = Ui.column(this);
        root.setBackgroundColor(Ui.PAPER);
        root.setPadding(Ui.dp(this, 16), 0, Ui.dp(this, 16), 0);
        LinearLayout header = Ui.row(this);
        header.addView(Ui.iconButton(this, "‹", "Retour", v -> finish()));
        TextView title = Ui.notebookTitle(this, "Cap Hifz", 26);
        title.setGravity(Gravity.CENTER);
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        header.addView(new View(this), new LinearLayout.LayoutParams(Ui.dp(this, 48), 1));
        root.addView(header);
        root.addView(Ui.divider(this));
        ScrollView scroll = new ScrollView(this);
        body = Ui.column(this);
        body.setPadding(0, Ui.dp(this, 6), 0, Ui.dp(this, 14));
        scroll.addView(body);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
        Ui.respectSystemBars(this, root, 0, 0, 0, 0);
        line("Chargement…", 15);
        io.execute(() -> {
            try {
                GeometryRepository loaded = GeometryRepository.get(getApplicationContext());
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    geometry = loaded;
                    try {
                        loadEvidence();
                        render();
                    } catch (RuntimeException invalid) {
                        android.util.Log.e("CapHifz", "Unable to read Hifz evidence", invalid);
                        unavailable();
                    }
                });
            } catch (RuntimeException unavailable) {
                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed()) unavailable();
                });
            }
        });
    }

    private void unavailable() {
        body.removeAllViews();
        line("Suivi temporairement indisponible.", 16);
    }

    private void loadEvidence() {
        HifzPrefs.ProgressionSnapshot progression = prefs.progressionSnapshotV6();
        acquired = progression.acquired;
        Set<String> canonical = new HashSet<>();
        for (int i = 0; i < geometry.lineCount(); i++) canonical.add(geometry.line(i).id);
        lessons = CapHifzEvidence.lessons(prefs.dailyLessonReceiptsJson(), canonical);
        learningDates = CapHifzEvidence.firstDates(lessons, Track.SABQI, today);
        stabilizationDates = CapHifzEvidence.firstDates(lessons, Track.ITQAN, today);
        finalDates = finals.lineDates(prefs.lastLearningConsolidationDate(), prefs.lastRecentSabqiReviewDate(), today);
        ActiveReviewSlots active = prefs.activeReviewSlots();
        Map<Track, Set<LocalDate>> dates = new EnumMap<>(Track.class);
        dates.put(Track.SABQI, new HashSet<>(ledger.completedDates(HifzSessionActivity.SABQI, prefs.programStartDate(), today)));
        dates.put(Track.ITQAN, new HashSet<>(ledger.completedDates(HifzSessionActivity.ITQAN, prefs.programStartDate(), today)));
        dates.put(Track.MORNING, new HashSet<>());
        dates.put(Track.EVENING, new HashSet<>());
        for (CapHifzEvidence.Lesson lesson : lessons) dates.get(lesson.track).add(lesson.date);
        for (DashboardLedger.Record row : active.receipts()) {
            ActiveReviewSlots.Slot slot = ActiveReviewSlots.Slot.fromMode(row.type);
            if (slot != null && !row.label.trim().isEmpty())
                dates.get(slot == ActiveReviewSlots.Slot.MORNING ? Track.MORNING : Track.EVENING).add(row.date);
        }
        received = (track, date) -> dates.get(track).contains(date);
    }

    private void render() {
        int offset = ((ScrollView) body.getParent()).getScrollY();
        body.removeAllViews();
        LocalDate start = prefs.programStartDate();
        LocalDate coverage = ledger.coverageStartDate();
        line("Début " + start.format(DATE) + "  ·  Reçus depuis " + (coverage.isBefore(start) ? start : coverage).format(DATE), 14);
        heading("Assiduité");
        Map<Track, CapHifzEvidence.Count> totals = counts(start, today);
        for (Track track : Track.values()) line(TRACKS[track.ordinal()] + "  " + totals.get(track).rate(), 15);
        line("Séances Sabqi et Itqān vérifiables au rythme 3/3", 13);
        LocalDate firstWeek = start.minusDays(start.getDayOfWeek().getValue() - 1);
        nav("Du " + selectedWeek.format(DAY_MONTH) + " au " + selectedWeek.plusDays(6).format(DAY_MONTH),
            "semaine", selectedWeek.isAfter(firstWeek), selectedWeek.isBefore(monday),
            delta -> { selectedWeek = selectedWeek.plusWeeks(delta); render(); });
        LocalDate end = selectedWeek.equals(monday) ? today : selectedWeek.plusDays(6);
        Map<Track, CapHifzEvidence.Count> weekly = counts(selectedWeek, end);
        line("Sabqi " + weekly.get(Track.SABQI).fraction() + "  ·  Itqān " + weekly.get(Track.ITQAN).fraction()
            + "  ·  Active " + weekly.get(Track.MORNING).fraction() + " matin / "
            + weekly.get(Track.EVENING).fraction() + " soir", 15);
        long earned = finalDates.entrySet().stream().filter(row -> {
            LocalDate proof = row.getValue().acquired;
            LocalDate first = earlier(learningDates.get(row.getKey()), stabilizationDates.get(row.getKey()));
            return acquired.contains(row.getKey()) && first != null && !first.isBefore(selectedWeek)
                && !first.isAfter(proof) && !proof.isAfter(end);
        }).count();
        line(earned + (earned < 2 ? " ligne acquise documentée" : " lignes acquises documentées"), 14);
        if (selectedWeek.equals(monday)) showCarryovers();
        button(body, "Historique des acquis ›", v -> showHistory());

        body.addView(Ui.divider(this));
        heading("Simulations");
        LinearLayout choices = Ui.row(this);
        for (int percent : new int[]{80, 90, 100}) {
            TextView item = button(choices, percent + " %", v -> { scenario = percent; render(); });
            item.setContentDescription("Simulation à " + percent + " pour cent" + (scenario == percent ? ", sélectionnée" : ""));
            item.setSelected(scenario == percent);
            if (scenario == percent) item.setPaintFlags(item.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG);
            item.setGravity(Gravity.CENTER);
            item.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));
        }
        body.addView(choices);
        renderForecast();
        body.post(() -> ((ScrollView) body.getParent()).scrollTo(0, offset));
    }

    private Map<Track, CapHifzEvidence.Count> counts(LocalDate from, LocalDate through) {
        LocalDate start = prefs.programStartDate();
        LocalDate coverage = ledger.coverageStartDate();
        int sabqi = prefs.sabqiLineCursor();
        boolean stopped = sabqi > geometry.lastLineIndex(prefs.sabqiEnd());
        // A dated Recommended schedule is reconstructible. Old manual ratios were never journaled.
        return CapHifzEvidence.count(from.isBefore(start) ? start : from, through, today, coverage,
            prefs.activeReviewSlots().programmeStart(), day -> WeeklyProgramPolicy.actionFor(
                day.getDayOfWeek(), prefs.weeklyProgramFor(day), prefs.learningDaysPerWeek()),
            day -> prefs.weeklyProgramFor(day) == HifzPrefs.WeeklyProgram.RECOMMENDED && !stopped,
            day -> prefs.weeklyProgramFor(day) == HifzPrefs.WeeklyProgram.RECOMMENDED && !prefs.weeklyItqanPageClosed(day),
            received);
    }

    private void showCarryovers() {
        List<String> open = new ArrayList<>();
        if (prefs.sabqiReportDate(today) != null) open.add("Sabqi");
        if (prefs.hasItqanForecastCarryover(today)) open.add("Itqān");
        if (prefs.activeReviewSlots().earlierCurrentDraftExists(today)) open.add("Révision active");
        line("Reprises ouvertes  " + (open.isEmpty() ? "aucune" : String.join(" · ", open)), 14);
    }

    private void renderForecast() {
        int cursor = prefs.sabqiLineCursor();
        if (cursor < 0) cursor = geometry.firstLineIndex(prefs.sabqiStart());
        HifzPrefs.WeeklyProgram programme = prefs.weeklyProgramMode();
        RoadmapPolicy.Decision decision = programme == HifzPrefs.WeeklyProgram.RECOMMENDED
            || cursor >= geometry.lineCount() ? null : prefs.currentRoadmapDecision(geometry);
        int ratio = decision != null ? decision.learningDays : prefs.learningDaysPerWeek();
        int sabqiDays = programme == HifzPrefs.WeeklyProgram.RECOMMENDED ? 3 : ratio;
        line(sabqiDays + " jours Sabqi  ·  " + (6 - sabqiDays) + " jours Itqān par semaine", 14);
        boolean postNas = prefs.itqanPostNasMaintenance();
        boolean tail = !postNas && prefs.itqanRotationState().leg == ItqanRotationPolicy.Leg.TAIL_HUJURAT_NAS;
        ItqanFourSessionSnapshot frozen = tail ? prefs.forecastItqanSnapshot(geometry) : null;
        int nextItqan = tail ? prefs.forecastItqanLine(geometry) : -1;
        List<ItqanFourSessionPolicy.Session> remaining = java.util.Collections.emptyList();
        if (frozen != null && frozen.weeklyPage()) {
            remaining = new ArrayList<>(frozen.plan.sessions.subList(frozen.sessionIndex, frozen.plan.sessions.size()));
            ItqanFourSessionPolicy.Session slot = remaining.get(0);
            List<ItqanFourSessionPolicy.Block> blocks = slot.blocks.subList(frozen.blockIndex, slot.blocks.size());
            int lines = blocks.stream().mapToInt(block -> block.lineIds.size()).sum();
            remaining.set(0, new ItqanFourSessionPolicy.Session(slot.index, blocks, lines));
            List<String> current = blocks.get(0).lineIds;
            nextItqan = geometry.linesForExactIds(current).get(0).globalIndex;
        }
        if (tail && frozen == null && prefs.hasLegacyItqanForecastCheckpoint()) {
            ItqanFourSessionPolicy.Plan carryover = prefs.forecastNajmCarryover(geometry, today);
            if (carryover != null) {
                remaining = carryover.sessions;
                nextItqan = geometry.linesForExactIds(remaining.get(0).blocks.get(0).lineIds).get(0).globalIndex;
            }
        }
        boolean tailForecast = tail && programme == HifzPrefs.WeeklyProgram.RECOMMENDED
            && (!remaining.isEmpty() || frozen == null && !prefs.hasLegacyItqanForecastCheckpoint());
        Set<String> stabilized = new HashSet<>(prefs.progressionSnapshotV6().stabilized);
        stabilized.addAll(acquired);
        boolean canReachFath = geometry.lastLineIndex(prefs.sabqiEnd()) >= geometry.lastLineIndex(SabqiRoute.ROUTE_END);
        boolean forecastSabqi = sabqiDays > 0 && canReachFath && cursor >= geometry.firstLineIndex(prefs.sabqiStart());
        LocalDate blockedWeek = prefs.itqanClosedThisWeek(today) ? monday : null;
        List<CapHifzForecast.Quarter> quarters = CapHifzForecast.quarters(geometry, today, cursor,
            nextItqan, postNas, date -> WeeklyProgramPolicy.actionFor(date.getDayOfWeek(), programme, sabqiDays),
            forecastSabqi, tailForecast, remaining, stabilized, blockedWeek, scenario / 100.0);
        int lastYear = quarters.get(quarters.size() - 1).date.getYear();
        year = Math.max(today.getYear(), Math.min(year, lastYear));
        nav(Integer.toString(year), "année", year > today.getYear(), year < lastYear,
            delta -> { year += delta; render(); });
        boolean showItqan = tail;
        for (CapHifzForecast.Quarter row : quarters) {
            if (row.date.getYear() != year) {
                if (row.firstTailComplete) showItqan = false;
                continue;
            }
            line(row.date.format(DATE), 15).setTypeface(null, android.graphics.Typeface.BOLD);
            if (forecastSabqi || row.sabqiComplete) line("Sabqi  " + position(row.sabqi), 15);
            else line("Sabqi  Échéance non déterminée", 15);
            if (showItqan) {
                if (row.firstTailComplete) line("Itqān  " + position(ItqanRotationPolicy.TAIL_END), 15);
                else if (!tailForecast) line("Itqān  Échéance non déterminée", 15);
                else line("Itqān  " + position(row.itqan), 15);
            }
            if (row.firstTailComplete) showItqan = false;
            body.addView(Ui.divider(this));
        }
        if (postNas) line("Maintien après An-Nās", 14);
    }

    private void showHistory() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout history = Ui.column(this);
        history.setPadding(Ui.dp(this, 18), 0, Ui.dp(this, 18), 0);
        scroll.addView(history);
        List<GeometryRepository.LineMeta> rows = new ArrayList<>();
        for (int i = geometry.lineCount() - 1; i >= 0; i--)
            if (acquired.contains(geometry.line(i).id)) rows.add(geometry.line(i));
        LinearLayout pagination = Ui.row(this);
        LinearLayout entries = Ui.column(this);
        history.addView(pagination);
        history.addView(entries);
        int[] page = {0};
        TextView previous = button(pagination, "‹", v -> {});
        TextView counter = Ui.text(this, "", 14, false);
        counter.setGravity(Gravity.CENTER);
        pagination.addView(counter, new LinearLayout.LayoutParams(0, -2, 1));
        TextView next = button(pagination, "›", v -> {});
        previous.setContentDescription("Entrées précédentes");
        next.setContentDescription("Entrées suivantes");
        Runnable update = () -> {
            entries.removeAllViews();
            previous.setEnabled(page[0] > 0);
            next.setEnabled((page[0] + 1) * 8 < rows.size());
            counter.setText(rows.isEmpty() ? "" : (page[0] * 8 + 1) + "–"
                + Math.min((page[0] + 1) * 8, rows.size()) + " / " + rows.size());
            if (rows.isEmpty()) entries.addView(Ui.text(this, "Aucun acquis enregistré", 15, false));
            for (int i = page[0] * 8; i < Math.min((page[0] + 1) * 8, rows.size()); i++) {
                GeometryRepository.LineMeta row = rows.get(i);
                LocalDate learned = earlier(learningDates.get(row.id), stabilizationDates.get(row.id));
                PostFinalRecallStore.LineDates dates = finalDates.get(row.id);
                String label = "p. " + row.page + " · l. " + geometry.printedLineNumber(row)
                    + "  " + position(row.verses.get(0))
                    + "\nPremier reçu  " + date(learned)
                    + "\nStabilisation  " + date(stabilizationDates.get(row.id))
                    + "\nAcquis attesté au  " + date(dates == null ? null : dates.acquired)
                    + "\nDernière révision datée  non déterminée";
                TextView item = Ui.text(this, label, 15, false);
                item.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 8));
                entries.addView(item);
                entries.addView(Ui.divider(this));
            }
            scroll.scrollTo(0, 0);
        };
        previous.setOnClickListener(v -> { page[0]--; update.run(); });
        next.setOnClickListener(v -> { page[0]++; update.run(); });
        update.run();
        new AlertDialog.Builder(this).setTitle("Historique des acquis").setView(scroll)
            .setPositiveButton("Fermer", null).show();
    }

    private static LocalDate earlier(LocalDate a, LocalDate b) {
        if (a == null) return b;
        return b == null || a.isBefore(b) ? a : b;
    }

    private static String date(LocalDate date) { return date == null ? "non déterminé" : date.format(DATE); }

    private static String position(VerseRef ref) {
        return ref == null ? "Position non déterminée" : QuranSurahNames.name(ref.getSurah()) + " " + ref.getAyah()
            + " · Juz " + CapHifzForecast.juz(ref) + " · Hizb " + CapHifzForecast.hizb(ref);
    }

    private void heading(String value) { body.addView(Ui.notebookTitle(this, value, 21)); }

    private TextView line(String value, int size) {
        TextView text = Ui.text(this, value, size, false);
        text.setPadding(0, Ui.dp(this, 3), 0, Ui.dp(this, 3));
        body.addView(text);
        return text;
    }

    private TextView button(LinearLayout parent, String text, View.OnClickListener click) {
        TextView item = Ui.text(this, text, 16, true);
        item.setMinWidth(Ui.dp(this, 48));
        item.setMinHeight(Ui.dp(this, 48));
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setFocusable(true);
        item.setOnClickListener(click);
        parent.addView(item);
        return item;
    }

    private void nav(String label, String unit, boolean back, boolean forward, java.util.function.IntConsumer move) {
        LinearLayout row = Ui.row(this);
        TextView previous = button(row, "‹", v -> move.accept(-1));
        previous.setEnabled(back);
        previous.setContentDescription(unit + " précédente");
        TextView current = Ui.notebookTitle(this, label, 20);
        current.setGravity(Gravity.CENTER);
        row.addView(current, new LinearLayout.LayoutParams(0, -2, 1));
        TextView next = button(row, "›", v -> move.accept(1));
        next.setEnabled(forward);
        next.setContentDescription(unit + " suivante");
        body.addView(row);
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        state.putInt("year", year);
        state.putInt("scenario", scenario);
        state.putLong("week", selectedWeek.toEpochDay());
        super.onSaveInstanceState(state);
    }

    @Override protected void onDestroy() {
        io.shutdownNow();
        super.onDestroy();
    }
}
