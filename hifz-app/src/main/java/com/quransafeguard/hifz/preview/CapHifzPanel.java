package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.content.Intent;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.quransafeguard.hifz.core.QuranCanon;
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
    /** Single read-only attendance assumption for Cap Hifdh; not a completion credit. */
    private static final int PROJECTION_ATTENDANCE_PERCENT = 90;

    private boolean spaciousBoox() {
        return context.getResources().getConfiguration().smallestScreenWidthDp >= 800;
    }

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
    private int lastForecastYear;
    private int checkpointIndex = 0;
    private List<MilestoneChoice> shownCheckpoints = new ArrayList<>();
    private TextView checkpointLabel;
    private CapHifzTimelineView timeline;
    private TextView previousNavigator;
    private TextView nextNavigator;
    private boolean historyMode = false;
    private int acquiredIndex = 0;
    private List<AcquiredChoice> acquiredLines = new ArrayList<>();

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
        historyMode = false;
        acquiredLines = new ArrayList<>();
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
        // Focus the home on short-term checkpoints, not dashboard attendance
        // or line-count accounting which already exists elsewhere in the app.
        renderTrajectory();
        showHistorySummary();
    }

    private void renderWeek() {
        LocalDate end = today;
        Map<Track, CapHifzEvidence.Count> weekly = counts(monday, end);
        line("Acquisition " + weekly.get(Track.SABQI).fraction() + "  ·  Maîtrise " + weekly.get(Track.ITQAN).fraction()
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
        // One fixed target, never three competing simulations or a mutable UI setting.
        line("Prévision · 90 % d’assiduité", 12);
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
        if (prefs.sabqiReportDate(today) != null) open.add("Acquisition");
        if (prefs.hasItqanForecastCarryover(today)) open.add("Maîtrise");
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
        // After the first authentic completion of An-Nas, the fixed destination
        // becomes ongoing Maintien. Read only an existing maintenance draft:
        // never predict a Juz not actually allocated by the session engine.
        int maintenanceJuz = -1;
        VerseRef maintenanceVerse = null;
        if (postNas && prefs.usesJuzMaintenance()) {
            try {
                JuzMaintenanceStore.Draft draft = prefs.juzMaintenance().read();
                if (draft != null && draft.juz >= 1 && draft.juz <= 30) {
                    maintenanceJuz = draft.juz;
                    maintenanceVerse = QuranCanon.INSTANCE.fromOrdinal(
                        JuzProgressSummary.boundaries()[draft.juz - 1]);
                }
            } catch (RuntimeException incompleteProof) {
                // Corrupt/open legacy state must not claim a future Juz or
                // disable the owner's read-only Cap Hifdh.
                maintenanceJuz = -1;
                maintenanceVerse = null;
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
            forecastSabqi, tailForecast, remaining, stabilized, blockedWeek, PROJECTION_ATTENDANCE_PERCENT / 100.0);
        // The full horizon is drawn ONCE, independent of the year selected below.
        // Never reuse an illustrative serpentine which can invert physical Juz order.
        // Each lane owns its genuine terminal date; do not repeat a large,
        // ambiguous overall-finish sentence above the roadmap.

        int lastYear = quarters.get(quarters.size() - 1).date.getYear();
        lastForecastYear = lastYear;
        year = Math.max(today.getYear(), Math.min(year, lastYear));
        shownCheckpoints = checkpointsForYear(quarters, year);
        checkpointIndex = Math.max(0, Math.min(checkpointIndex,
            Math.max(0, shownCheckpoints.size() - 1)));
        timeline = null;
        if (forecastSabqi || tailForecast || postNas) {
            // The original one-screen BOOX home stays completely fixed.
            // Tap a real projected sourate/verset to open the QCF Mushaf; tapping
            // empty ink cycles the next checkpoint inline, never a dialog.
            // The real positions below come ONLY from persisted next-line
            // cursors. They are NOT incorrectly labelled "acquired Juz".
            VerseRef sabqiCurrentVerse = cursor >= 0 && cursor < geometry.lineCount()
                && !geometry.line(cursor).verses.isEmpty()
                ? geometry.line(cursor).verses.get(0) : null;
            VerseRef tailCurrentVerse = nextItqan >= 0 && nextItqan < geometry.lineCount()
                && !geometry.line(nextItqan).verses.isEmpty()
                ? geometry.line(nextItqan).verses.get(0) : null;
            timeline = new CapHifzTimelineView(context, geometry, quarters, today,
                forecastSabqi, tailForecast,
                ignoredYear -> nextCheckpoint(),
                (sabqi, milestone) -> openMushaf(milestone.verse), year,
                sabqiCurrentVerse, tailCurrentVerse, postNas,
                maintenanceVerse, maintenanceJuz);
            body.addView(timeline, new LinearLayout.LayoutParams(-1,
                Ui.dp(context, spaciousBoox()
                    ? (forecastSabqi && (tailForecast || postNas) ? 326 : 166)
                    : (forecastSabqi && (tailForecast || postNas) ? 184 : 90))));
        }
        LinearLayout navigator = Ui.row(context);
        TextView previous = button(navigator, "‹", v -> {
            if (historyMode) {
                acquiredIndex = Math.min(acquiredIndex + 1, acquiredLines.size() - 1);
                updateCheckpointLabel();
            } else {
                year--;
                checkpointIndex = 0;
                render();
            }
        });
        // Larger navigation glyphs without a new row or extra screen height.
        previous.setTextSize(spaciousBoox() ? 22f : 20f);
        previousNavigator = previous;

        checkpointLabel = button(navigator, "", v -> {
            if (historyMode) {
                if (!acquiredLines.isEmpty())
                    openMushaf(acquiredLines.get(acquiredIndex).verse);
            } else if (!shownCheckpoints.isEmpty())
                openMushaf(shownCheckpoints.get(checkpointIndex).verse);
            else if (year < lastYear) {
                year++;
                checkpointIndex = 0;
                render();
            }
        });
        checkpointLabel.setTextSize(spaciousBoox() ? 18f : 13f);
        checkpointLabel.setMaxLines(1);
        checkpointLabel.setEllipsize(android.text.TextUtils.TruncateAt.END);
        checkpointLabel.setLayoutParams(new LinearLayout.LayoutParams(0,
            Ui.dp(context, 48), 1f));
        TextView next = button(navigator, "›", v -> {
            if (historyMode) {
                acquiredIndex = Math.max(0, acquiredIndex - 1);
                updateCheckpointLabel();
            } else {
                year++;
                checkpointIndex = 0;
                render();
            }
        });
        next.setTextSize(spaciousBoox() ? 22f : 20f);
        nextNavigator = next;
        body.addView(navigator);
        updateCheckpointLabel();
        // Maintien is now the second roadmap lane, not a stray footer sentence.
    }

    private static final class MilestoneChoice {
        final LocalDate date;
        final VerseRef verse;
        final String label;
        final boolean sabqi;
        final int iconRes;

        MilestoneChoice(LocalDate date, VerseRef verse, String label, boolean sabqi) {
            this(date, verse, label, sabqi, 0);
        }

        MilestoneChoice(LocalDate date, VerseRef verse, String label, boolean sabqi, int iconRes) {
            this.date = date;
            this.verse = verse;
            this.label = label;
            this.sabqi = sabqi;
            this.iconRes = iconRes;
        }
    }

    /**
     * Every entry is an actual dated projected boundary or quarter position.
     * These are read-only hypothetical future positions, not Hifdh receipts.
     * No horizontal overflow, modal dialog, invented Juz or extra home row.
     */
    private List<MilestoneChoice> checkpointsForYear(
            List<CapHifzForecast.Quarter> quarters, int selectedYear) {
        List<MilestoneChoice> choices = new ArrayList<>();
        for (CapHifzForecast.Quarter row : quarters) {
            if (row.date.getYear() != selectedYear) continue;
            for (CapHifzForecast.Milestone p : row.sabqiMilestones)
                choices.add(new MilestoneChoice(p.date, p.verse,
                    QuranSurahNames.rawName(p.verse.getSurah()) + " " + p.verse.getAyah(),
                    true));
            for (CapHifzForecast.Milestone p : row.tailMilestones)
                choices.add(new MilestoneChoice(p.date, p.verse,
                    QuranSurahNames.rawName(p.verse.getSurah()) + " " + p.verse.getAyah(),
                    false));
            if (row.sabqiFinish != null)
                choices.add(new MilestoneChoice(row.sabqiFinish,
                    SabqiRoute.ROUTE_END, "Al-Fatḥ · cible", true));
            if (row.tailFinish != null)
                choices.add(new MilestoneChoice(row.tailFinish,
                    ItqanRotationPolicy.TAIL_END, "An-Nās · cible", false));
            // At low cadence, no Juz may be crossed within this calendar
            // year. A real quarter cursor remains directly browsable instead.
            if (row.sabqi != null)
                choices.add(new MilestoneChoice(row.date, row.sabqi, "T"
                    + ((row.date.getMonthValue() - 1) / 3 + 1), true));
            if (row.itqan != null)
                choices.add(new MilestoneChoice(row.date, row.itqan, "T"
                    + ((row.date.getMonthValue() - 1) / 3 + 1), false));
        }
        choices.sort(Comparator.comparing((MilestoneChoice c) -> c.date)
            .thenComparing(c -> c.sabqi ? 0 : 1));
        return choices;
    }

    private void nextCheckpoint() {
        if (historyMode) {
            if (acquiredLines.isEmpty()) return;
            acquiredIndex = (acquiredIndex + 1) % acquiredLines.size();
            updateCheckpointLabel();
            return;
        }
        if (shownCheckpoints.isEmpty()) return;
        checkpointIndex = (checkpointIndex + 1) % shownCheckpoints.size();
        updateCheckpointLabel();
    }

    private void updateCheckpointLabel() {
        if (checkpointLabel == null) return;
        if (previousNavigator != null) {
            previousNavigator.setEnabled(historyMode
                ? acquiredIndex + 1 < acquiredLines.size() : year > today.getYear());
            previousNavigator.setContentDescription(historyMode
                ? "Acquis plus ancien" : "Année précédente de Cap Hifdh");
        }
        if (nextNavigator != null) {
            nextNavigator.setEnabled(historyMode
                ? acquiredIndex > 0 : year < lastForecastYear);
            nextNavigator.setContentDescription(historyMode
                ? "Acquis plus récent" : "Année suivante de Cap Hifdh");
        }
        if (historyMode) {
            if (timeline != null) timeline.setSelection(null, true);
            if (acquiredLines.isEmpty()) {
                checkpointLabel.setText("Aucun acquis enregistré");
                checkpointLabel.setContentDescription(
                    "Aucun acquis enregistré : toucher l'historique pour revenir à la projection");
                return;
            }
            AcquiredChoice choice = acquiredLines.get(acquiredIndex);
            String date = choice.date == null ? "Sans date" : choice.date.format(DAY_MONTH);
            checkpointLabel.setText("Acquis " + (acquiredIndex + 1) + "/"
                + acquiredLines.size() + " · "
                + QuranSurahNames.name(choice.verse.getSurah()) + " "
                + choice.verse.getAyah() + " · " + date + "  ›");
            checkpointLabel.setContentDescription(
                "Ouvrir un acquis dans le Mushaf depuis Cap Hifdh : "
                + position(choice.verse)
                + ", " + date + ". Attesté dans la progression V6.");
            return;
        }
        if (shownCheckpoints.isEmpty()) {
            checkpointLabel.setText(year + " · Aucun jalon\n‹ › : autre année");
            checkpointLabel.setContentDescription(
                "Cap Hifdh " + year + " : aucun jalon prévisionnel, changer d'année");
            if (timeline != null) timeline.setSelection(null, true);
            return;
        }
        MilestoneChoice choice = shownCheckpoints.get(checkpointIndex);
        String track = choice.sabqi ? "Acquisition" : "Maîtrise";
        checkpointLabel.setText(checkpointDisplay(choice));
        checkpointLabel.setContentDescription("Ouvrir le Mushaf depuis Cap Hifdh : "
            + track + ", "
            + choice.date.format(FULL_DATE) + ", "
            + QuranSurahNames.name(choice.verse.getSurah())
            + " " + choice.verse.getAyah() + ". Prévision, pas acquis.");
        if (timeline != null)
            timeline.setSelection(choice.date, choice.sabqi);
    }

    /** Compact Quranic landmark: one true sourate and ayah, with forecast date. */
    private CharSequence checkpointDisplay(MilestoneChoice choice) {
        return year + " · " + QuranSurahNames.name(choice.verse.getSurah())
            + " · " + choice.verse.getAyah() + " · "
            + choice.date.format(DAY_MONTH) + "  ›";
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

    private static final class AcquiredChoice {
        final VerseRef verse;
        final int page;
        final LocalDate date;

        AcquiredChoice(VerseRef verse, int page, LocalDate date) {
            this.verse = verse;
            this.page = page;
            this.date = date;
        }
    }

    /** Browse the genuinely acquired QCF lines using the SAME home controls.
     *  No AlertDialog, huge modal list, fake receipt or viewport scroll. */
    private List<AcquiredChoice> acquiredChoices() {
        List<AcquiredChoice> choices = new ArrayList<>();
        for (int i = 0; i < geometry.lineCount(); i++) {
            GeometryRepository.LineMeta row = geometry.line(i);
            if (!acquired.contains(row.id) || row.verses.isEmpty()) continue;
            PostFinalRecallStore.LineDates proof = finalDates.get(row.id);
            choices.add(new AcquiredChoice(row.verses.get(0), row.page,
                proof == null ? null : proof.acquired));
        }
        choices.sort(Comparator.comparing((AcquiredChoice c) -> c.date,
            Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(c -> -c.page));
        return choices;
    }

    private void showHistorySummary() {
        long dated = acquired.stream().filter(id -> finalDates.containsKey(id)
            && finalDates.get(id).acquired != null).count();
        String summary = "Acquis ›";
        String spokenSummary = acquired.size() + " lignes acquises, " + dated + " datées";
        TextView history = line(summary, spaciousBoox() ? 16 : 12);
        history.setMinHeight(Ui.dp(context, 64));
        // TextView.setMinHeight is not honored when the fixed BOOX root
        // compresses WRAP_CONTENT. Reserve a measurable hit target instead.
        history.setLayoutParams(new LinearLayout.LayoutParams(-1, Ui.dp(context, 64)));
        history.setGravity(Gravity.CENTER_VERTICAL);
        history.setPadding(Ui.dp(context, 8), 0, Ui.dp(context, 8), 0);
        history.setContentDescription("Parcourir les acquis. " + spokenSummary + ". Sans fenêtre");
        history.setFocusable(true);
        history.setOnClickListener(v -> {
            historyMode = !historyMode;
            if (historyMode) {
                acquiredLines = acquiredChoices();
                acquiredIndex = 0;
                history.setText("Acquis · revenir à la projection  ›");
                history.setContentDescription("Quitter la consultation des acquis");
            } else {
                history.setText(summary);
                history.setContentDescription("Parcourir les acquis. " + spokenSummary + ". Sans fenêtre");
            }
            updateCheckpointLabel();
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
