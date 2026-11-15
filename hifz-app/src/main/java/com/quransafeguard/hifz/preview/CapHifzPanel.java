package com.quransafeguard.hifz.preview;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.quransafeguard.hifz.core.VerseRef;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;

import static com.quransafeguard.hifz.preview.CapHifzEvidence.Track;

final class CapHifzPanel extends LinearLayout {
    private static final DateTimeFormatter FULL_DATE = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.FRANCE);
    private static final DateTimeFormatter DAY_MONTH = DateTimeFormatter.ofPattern("d MMM", Locale.FRANCE);

    private HifzPrefs prefs;
    private DashboardLedger ledger;
    private PostFinalRecallStore finals;
    private final LinearLayout body;
    private GeometryRepository geometry;
    private LocalDate today;
    private LocalDate monday;
    private Set<String> acquired;
    private Map<String, PostFinalRecallStore.LineDates> finalDates;
    private Map<String, LocalDate> learningDates;
    private Map<String, LocalDate> stabilizationDates;
    private BiPredicate<Track, LocalDate> received;
    private final Context context;
    private int year;
    private int scenario = 90;

    CapHifzPanel(Context context) {
        super(context);
        this.context = context;
        setOrientation(VERTICAL);
        setPadding(0, Ui.dp(context, 3), 0, 0);
        addView(Ui.notebookTitle(context, "Cap Hifdh", 19));
        body = Ui.column(context);
        addView(body);
        line("Chargement…", 13);
    }

    void refresh(GeometryRepository geometry) {
        this.geometry = geometry;
        prefs = HifzPrefs.readOnly(context);
        ledger = DashboardLedger.readOnly(context);
        finals = new PostFinalRecallStore(context);
        today = HifzClock.today();
        monday = today.minusDays(today.getDayOfWeek().getValue() - 1);
        if (year == 0) year = today.getYear();
        try {
            loadEvidence();
            render();
        } catch (RuntimeException invalid) {
            unavailable();
        }
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
        List<CapHifzEvidence.Lesson> lessons = CapHifzEvidence.lessons(prefs.dailyLessonReceiptsJson(), canonical);
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
        body.removeAllViews();
        renderWeek();
        renderTrajectory();
        showHistorySummary();
    }

    private void renderWeek() {
        LocalDate end = today;
        Map<Track, CapHifzEvidence.Count> weekly = counts(monday, end);
        line("Sabqi " + weekly.get(Track.SABQI).fraction() + "  ·  Itqān " + weekly.get(Track.ITQAN).fraction()
            + "  ·  Active " + weekly.get(Track.MORNING).fraction() + " matin / "
            + weekly.get(Track.EVENING).fraction() + " soir", 15);
        long earned = finalDates.entrySet().stream().filter(row -> {
            LocalDate proof = row.getValue().acquired;
            LocalDate first = earlier(learningDates.get(row.getKey()), stabilizationDates.get(row.getKey()));
            return acquired.contains(row.getKey()) && first != null && !first.isBefore(monday)
                && !first.isAfter(proof) && !proof.isAfter(end);
        }).count();
        line(earned + (earned == 1 ? " ligne acquise cette semaine" : " lignes acquises cette semaine")
            + carryovers(), 12);
    }

    private void renderTrajectory() {
        LinearLayout choices = Ui.row(context);
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

    private String carryovers() {
        List<String> open = new ArrayList<>();
        if (prefs.sabqiReportDate(today) != null) open.add("Sabqi");
        if (prefs.hasItqanForecastCarryover(today)) open.add("Itqān");
        if (prefs.activeReviewSlots().earlierCurrentDraftExists(today)) open.add("Révision active");
        return open.isEmpty() ? "" : " · Reprise " + String.join(", ", open);
    }

    private void renderForecast() {
        int cursor = prefs.sabqiLineCursor();
        if (cursor < 0) cursor = geometry.firstLineIndex(prefs.sabqiStart());
        HifzPrefs.WeeklyProgram programme = prefs.weeklyProgramMode();
        RoadmapPolicy.Decision decision = programme == HifzPrefs.WeeklyProgram.RECOMMENDED
            || cursor >= geometry.lineCount() ? null : prefs.currentRoadmapDecision(geometry);
        int ratio = decision != null ? decision.learningDays : prefs.learningDaysPerWeek();
        int sabqiDays = programme == HifzPrefs.WeeklyProgram.RECOMMENDED ? 3 : ratio;
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
        // The full horizon is drawn ONCE, independent of the year selected below.
        // Never reuse an illustrative serpentine which can invert physical Juz order.
        LocalDate coverageGoal = CapHifzTimelineOrder.firstFullCoverageQuarter(quarters);
        if (CapHifzTimelineOrder.allThirtyJuzAcquired(geometry, acquired)) {
            line("30 Juz acquis attestés", 13);
        } else if (coverageGoal != null) {
            line("Fin estimée à cadence supposée · T"
                + ((coverageGoal.getMonthValue() - 1) / 3 + 1) + " " + coverageGoal.getYear(), 13);
        } else {
            line("Échéance indéterminée · cadence, borne ou reprise à vérifier", 13);
        }

        int lastYear = quarters.get(quarters.size() - 1).date.getYear();
        year = Math.max(today.getYear(), Math.min(year, lastYear));
        if (forecastSabqi || tailForecast) {
            // One whole year is shown at a time, not twenty years squeezed
            // between two endpoints. Direct tap of a milestone opens its
            // canonical QCF verse; a tap elsewhere opens the accessible list.
            CapHifzTimelineView graphic = new CapHifzTimelineView(context, geometry, quarters, today,
                forecastSabqi, tailForecast,
                chosenYear -> showMilestonesForYear(quarters, chosenYear),
                (sabqi, milestone) -> openMushaf(milestone.verse), year);
            body.addView(graphic, new LinearLayout.LayoutParams(-1,
                Ui.dp(context, forecastSabqi && tailForecast ? 110 : 85)));
        }
        List<CapHifzForecast.Quarter> displayed = new ArrayList<>();
        for (CapHifzForecast.Quarter row : quarters) {
            if (row.date.getYear() == year) displayed.add(row);
        }
        LinearLayout details = Ui.row(context);
        TextView previous = button(details, "‹", v -> { year--; render(); });
        previous.setContentDescription("Année précédente");
        previous.setEnabled(year > today.getYear());
        TextView selectedYear = button(details, year + " · Juz / Hizb", v -> showMilestones(displayed));
        selectedYear.setContentDescription("Explorer les Juz et Hizb de " + year
            + " : chaque jalon ouvre la page exacte du Mushaf");
        selectedYear.setGravity(Gravity.CENTER);
        selectedYear.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));
        TextView next = button(details, "›", v -> { year++; render(); });
        next.setContentDescription("Année suivante");
        next.setEnabled(year < lastYear);
        body.addView(details);
        if (postNas) line("Maintien après An-Nās", 12);
    }

    private static final class MilestoneChoice {
        final LocalDate date;
        final VerseRef verse;
        final String title;

        MilestoneChoice(LocalDate date, VerseRef verse, String title) {
            this.date = date;
            this.verse = verse;
            this.title = title;
        }
    }

    private void showMilestonesForYear(List<CapHifzForecast.Quarter> quarters, int chosenYear) {
        List<CapHifzForecast.Quarter> rows = new ArrayList<>();
        for (CapHifzForecast.Quarter row : quarters)
            if (row.date.getYear() == chosenYear) rows.add(row);
        showMilestones(rows);
    }

    /**
     * Detailed drill-down lives in a scrollable Android dialog; the BOOX home
     * itself never scrolls. Milestone dates are forecasts, NOT earned receipts.
     * Every real QCF marker and quarter position opens the corresponding
     * exact verse/page in the original Mushaf, without touching Hifdh state.
     */
    private void showMilestones(List<CapHifzForecast.Quarter> rows) {
        List<MilestoneChoice> choices = new ArrayList<>();
        for (CapHifzForecast.Quarter row : rows) {
            for (CapHifzForecast.Milestone point : row.sabqiMilestones) {
                String marker = point.hizb % 2 == 1
                    ? "Juz " + ((point.hizb + 1) / 2) + " · Hizb " + point.hizb
                    : "Hizb " + point.hizb;
                choices.add(new MilestoneChoice(point.date, point.verse,
                    "Nouveaux versets · " + marker));
            }
            for (CapHifzForecast.Milestone point : row.tailMilestones) {
                String marker = point.hizb % 2 == 1
                    ? "Juz " + ((point.hizb + 1) / 2) + " · Hizb " + point.hizb
                    : "Hizb " + point.hizb;
                choices.add(new MilestoneChoice(point.date, point.verse,
                    "Maîtrise · " + marker));
            }
            if (row.sabqiFinish != null)
                choices.add(new MilestoneChoice(row.sabqiFinish, SabqiRoute.ROUTE_END,
                    "Objectif nouveaux versets · Al-Fatḥ"));
            if (row.tailFinish != null)
                choices.add(new MilestoneChoice(row.tailFinish, ItqanRotationPolicy.TAIL_END,
                    "Objectif maîtrise · An-Nās"));
            if (row.sabqi != null)
                choices.add(new MilestoneChoice(row.date, row.sabqi,
                    "Position de fin de trimestre · Nouveaux versets"));
            if (row.itqan != null)
                choices.add(new MilestoneChoice(row.date, row.itqan,
                    "Position de fin de trimestre · Maîtrise"));
        }
        choices.sort(Comparator.comparing((MilestoneChoice c) -> c.date));
        if (choices.isEmpty()) {
            new AlertDialog.Builder(context)
                .setTitle("Juz et Hizb · " + year)
                .setMessage("Aucun nouveau jalon projeté pour cette année selon la cadence actuelle.")
                .setPositiveButton("Fermer", null).show();
            return;
        }
        List<String> labels = new ArrayList<>();
        for (MilestoneChoice entry : choices) {
            labels.add(entry.date.format(FULL_DATE) + " · Prévision"
                + "\n" + entry.title + " · " + QuranSurahNames.name(entry.verse.getSurah())
                + " " + entry.verse.getAyah() + " · p. " + geometry.pageForVerse(entry.verse)
                + "  ›  Mushaf");
        }
        new AlertDialog.Builder(context)
            .setTitle("Juz et Hizb · " + year + " · ouvrir le Mushaf")
            .setItems(labels.toArray(new String[0]), (dialog, index) ->
                openMushaf(choices.get(index).verse))
            .setPositiveButton("Fermer", null).show();
    }

    private void openMushaf(VerseRef verse) {
        if (verse == null || geometry == null) return;
        int page = geometry.pageForVerse(verse);
        Intent intent = new Intent(context, StudyReaderActivity.class)
            .putExtra(StudyReaderActivity.EXTRA_JUMP_PAGE, page)
            .putExtra(StudyReaderActivity.EXTRA_JUMP_VERSE,
                verse.getSurah() + ":" + verse.getAyah());
        context.startActivity(intent);
    }

    private void showHistorySummary() {
        long dated = acquired.stream().filter(id -> finalDates.containsKey(id)
            && finalDates.get(id).acquired != null).count();
        TextView history = line(acquired.size() + " lignes acquises · " + dated + " datées", 12);
        history.setContentDescription(history.getText() + " · Consulter les dates des acquis");
        history.setFocusable(true);
        history.setOnClickListener(v -> {
            List<String> labels = new ArrayList<>();
            List<VerseRef> targets = new ArrayList<>();
            for (int i = geometry.lineCount() - 1; i >= 0; i--) {
                GeometryRepository.LineMeta row = geometry.line(i);
                if (!acquired.contains(row.id)) continue;
                if (row.verses.isEmpty()) continue;
                targets.add(row.verses.get(0));
                PostFinalRecallStore.LineDates proof = finalDates.get(row.id);
                LocalDate first = earlier(learningDates.get(row.id), stabilizationDates.get(row.id));
                labels.add("p. " + row.page + " · l. " + geometry.printedLineNumber(row)
                    + " · " + position(row.verses.get(0))
                    + "\nAcquis " + formatDate(proof == null ? null : proof.acquired)
                    + " · Premier reçu " + formatDate(first));
            }
            if (labels.isEmpty()) labels.add("Aucun acquis enregistré");
            new AlertDialog.Builder(context).setTitle("Historique des acquis · Mushaf")
                .setItems(labels.toArray(new String[0]), (dialog, index) -> {
                    if (index < targets.size()) openMushaf(targets.get(index));
                }).setPositiveButton("Fermer", null).show();
        });
    }

    private static String formatDate(LocalDate value) {
        return value == null ? "sans date" : value.format(FULL_DATE);
    }

    private static LocalDate earlier(LocalDate a, LocalDate b) {
        if (a == null) return b;
        return b == null || a.isBefore(b) ? a : b;
    }

    private static String position(VerseRef ref) {
        return ref == null ? "Position non déterminée" : QuranSurahNames.name(ref.getSurah()) + " " + ref.getAyah()
            + " · Juz " + CapHifzForecast.juz(ref) + " · Hizb " + CapHifzForecast.hizb(ref);
    }

    private TextView line(String value, int size) {
        TextView text = Ui.text(context, value, size, false);
        text.setIncludeFontPadding(false);
        body.addView(text);
        return text;
    }

    private TextView button(LinearLayout parent, String label, View.OnClickListener click) {
        TextView item = Ui.text(context, label, 14, true);
        item.setMinWidth(Ui.dp(context, 48));
        item.setMinHeight(Ui.dp(context, 48));
        item.setGravity(Gravity.CENTER);
        item.setFocusable(true);
        item.setOnClickListener(click);
        parent.addView(item);
        return item;
    }
}
