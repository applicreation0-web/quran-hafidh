package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.content.Intent;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ListPopupWindow;
import android.widget.ArrayAdapter;
import android.view.ViewGroup;

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
    private List<CapHifzForecast.Quarter> forecastQuarters = java.util.Collections.emptyList();
    private int checkpointIndex = 0;
    // UI-only cursor: true is the authentic CURRENT working position.
    // Exploring years/surah completions never changes receipts or saved progress.
    private boolean todayMode = true;
    private TextView todayShortcut;
    private VerseRef currentSabqiVerse;
    private VerseRef currentTailVerse;
    private List<MilestoneChoice> shownCheckpoints = new ArrayList<>();
    private TextView checkpointLabel;
    private CapHifzTimelineView timeline;
    private TextView previousYearNavigator;
    private TextView previousNavigator;
    private TextView nextNavigator;
    private TextView nextYearNavigator;
    private LinearLayout yearNavigatorGroup;
    // Instrumented screenshot comparison ONLY. Default official UX = arrows.
    private boolean dropdownPreview;
    private ListPopupWindow milestoneDropdown;
    private boolean historyMode = false;
    private int acquiredIndex = 0;
    private List<AcquiredChoice> acquiredLines = new ArrayList<>();

    CapHifzPanel(Context context) {
        super(context);
        this.context = context;
        setOrientation(VERTICAL);
        setPadding(0, Ui.dp(context, 3), 0, 0);
        LinearLayout heading = Ui.row(context);
        TextView title = Ui.notebookTitle(context, "Cap Hifdh", 19);
        heading.addView(title, new LinearLayout.LayoutParams(0, Ui.dp(context, 48), 1f));
        title.setGravity(Gravity.CENTER_VERTICAL);
        todayShortcut = Ui.text(context, "↺ Aujourd’hui", 15, false);
        todayShortcut.setGravity(Gravity.CENTER);
        todayShortcut.setMinHeight(Ui.dp(context, 48));
        todayShortcut.setMinWidth(Ui.dp(context, 100));
        todayShortcut.setContentDescription("Revenir à la situation réelle d’aujourd’hui dans Cap Hifdh");
        todayShortcut.setOnClickListener(v -> goToday());
        heading.addView(todayShortcut);
        addView(heading);
        body = Ui.column(context);
        // History and navigator keep their reserved height on any BOOX font size.
        // The forecast graph alone expands into leftover height.
        if (context.getResources().getConfiguration().smallestScreenWidthDp >= 600)
            addView(body, new LinearLayout.LayoutParams(-1, 0, 1f));
        else addView(body);
        line("Chargement…", 13);
    }

    void refresh(GeometryRepository geometry) {
        this.geometry = geometry;
        prefs = HifzPrefs.readOnly(context);
        ledger = DashboardLedger.readOnly(context);
        finals = new PostFinalRecallStore(context);
        today = HifzClock.today();
        // A freshly opened home starts in reality, but coming BACK from the
        // Mushaf must not throw away a future year/step being explored.
        // Explicit goToday() alone resets an exploration in one gesture.
        if (todayMode) {
            year = today.getYear();
            checkpointIndex = 0;
        }
        historyMode = false;
        acquiredLines = new ArrayList<>();
        monday = today.minusDays(today.getDayOfWeek().getValue() - 1);
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
        forecastQuarters = quarters;
        year = Math.max(today.getYear(), Math.min(year, lastYear));
        // Forecast navigation must never toggle the authentic completion
        // state. If An-Nās is predicted for 2028, 2031 shows only Entretien.
        boolean projectedPostNas = !postNas && tailForecast
            && CapHifzForecast.projectedTailFinishedBeforeYear(quarters, year);
        boolean showMastery = tailForecast && !projectedPostNas;
        // Once An-Nās is complete, remove the Maîtrise rail entirely from
        // Cap Hifdh. Maintenance remains available in its own Parcours
        // activity; an empty second rail must not consume BOOX space.
        boolean showEntretien = false;
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
            currentSabqiVerse = sabqiCurrentVerse;
            currentTailVerse = tailCurrentVerse;
            timeline = new CapHifzTimelineView(context, geometry, quarters, today,
                forecastSabqi, showMastery,
                ignoredYear -> nextCheckpoint(),
                (sabqi, milestone) -> selectForecastMilestone(sabqi, milestone), year,
                sabqiCurrentVerse, tailCurrentVerse, showEntretien,
                maintenanceVerse, maintenanceJuz, projectedPostNas);
            boolean boox = context.getResources().getConfiguration().smallestScreenWidthDp >= 600;
            LinearLayout.LayoutParams graphLayout = boox
                ? new LinearLayout.LayoutParams(-1, 0, 1f)
                : new LinearLayout.LayoutParams(-1, Ui.dp(context,
                    forecastSabqi && showMastery ? 184 : 108));
            body.addView(timeline, graphLayout);
        }
        // One compact row: double chevrons change year; single chevrons
        // browse actual dated forecast checkpoints, including year crossings.
        // No added BOOX row, modal, scrollbar or persisted progress.
        LinearLayout navigator = Ui.row(context);
        // Explicit, local YEAR group. The selected year is visible between
        // its own two arrows; the adjacent controls only browse SOURATE ends.
        LinearLayout yearGroup = Ui.row(context);
        yearNavigatorGroup = yearGroup;
        TextView previousYear = button(yearGroup, "‹", v -> moveForecastYear(-1));
        labelNavigator(previousYear, "‹", "");
        previousYearNavigator = previousYear;
        TextView yearBadge = Ui.text(context, "", spaciousBoox() ? 18f : 16f, true);
        String yearText = "ANNÉE\n" + year;
        android.text.SpannableString markedYear = new android.text.SpannableString(yearText);
        markedYear.setSpan(new android.text.style.RelativeSizeSpan(.63f), 0, 5,
            android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        yearBadge.setText(markedYear);
        yearBadge.setMaxLines(2);
        yearBadge.setIncludeFontPadding(false);
        yearBadge.setGravity(Gravity.CENTER);
        yearBadge.setContentDescription("Année de prévision " + year);
        yearGroup.addView(yearBadge, new LinearLayout.LayoutParams(
            Ui.dp(context, spaciousBoox() ? 70 : 60), Ui.dp(context, 58)));
        TextView nextYear = button(yearGroup, "›", v -> moveForecastYear(1));
        labelNavigator(nextYear, "›", "");
        nextYearNavigator = nextYear;
        navigator.addView(yearGroup);

        if (!dropdownPreview) {
            TextView previous = button(navigator, "‹", v -> {
                if (historyMode) {
                    acquiredIndex = Math.min(acquiredIndex + 1, acquiredLines.size() - 1);
                    updateCheckpointLabel();
                } else moveForecastStep(-1);
            });
            labelNavigator(previous, "‹", "JALON");
            previousNavigator = previous;
        } else previousNavigator = null;

        checkpointLabel = button(navigator, "", v -> {
            if (historyMode) {
                if (!acquiredLines.isEmpty())
                    openMushaf(acquiredLines.get(acquiredIndex).verse);
            } else if (todayMode) {
                openMushaf(currentSabqiVerse != null ? currentSabqiVerse : currentTailVerse);
            } else if (dropdownPreview) {
                showDropdownPreview();
            } else if (!shownCheckpoints.isEmpty())
                openMushaf(shownCheckpoints.get(checkpointIndex).verse);
            else if (year < lastYear) moveForecastYear(1);
        });
        checkpointLabel.setTextSize(spaciousBoox() ? 15f : 13.7f);
        checkpointLabel.setMaxLines(2);
        checkpointLabel.setEllipsize(android.text.TextUtils.TruncateAt.END);
        checkpointLabel.setLayoutParams(new LinearLayout.LayoutParams(0,
            Ui.dp(context, 58), 1f));

        if (!dropdownPreview) {
            TextView next = button(navigator, "›", v -> {
                if (historyMode) {
                    acquiredIndex = Math.max(0, acquiredIndex - 1);
                    updateCheckpointLabel();
                } else moveForecastStep(1);
            });
            labelNavigator(next, "›", "JALON");
            nextNavigator = next;
        } else nextNavigator = null;
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
     * ONE selectable forecast checkpoint for EVERY completed sourate; the two
     * tracks are never confused. Quarter samples and Hizb crossings remain
     * available to the forecast engine, but are not fake surah completions.
     * Fātiḥah is excluded; 2..48 = Acquisition, 49..114 = Maîtrise.
     */
    private List<MilestoneChoice> checkpointsForYear(
            List<CapHifzForecast.Quarter> quarters, int selectedYear) {
        List<MilestoneChoice> choices = new ArrayList<>();
        for (CapHifzForecast.Quarter row : quarters) {
            for (CapHifzForecast.Milestone point : row.sabqiSurahFinishes) {
                if (point.date.getYear() != selectedYear) continue;
                choices.add(new MilestoneChoice(point.date, point.verse,
                    QuranSurahNames.rawName(point.verse.getSurah())
                        + " v." + point.verse.getAyah(), true));
            }
            for (CapHifzForecast.Milestone point : row.tailSurahFinishes) {
                if (point.date.getYear() != selectedYear) continue;
                choices.add(new MilestoneChoice(point.date, point.verse,
                    QuranSurahNames.rawName(point.verse.getSurah())
                        + " v." + point.verse.getAyah(), false));
            }
        }
        choices.sort(Comparator.comparing((MilestoneChoice c) -> c.date)
            .thenComparing(c -> c.sabqi ? 0 : 1)
            .thenComparingInt(c -> GeometryRepository.ordinal(c.verse)));
        return choices;
    }

    private void nextCheckpoint() {
        if (historyMode) {
            if (acquiredLines.isEmpty()) return;
            acquiredIndex = (acquiredIndex + 1) % acquiredLines.size();
            updateCheckpointLabel();
        } else moveForecastStep(1);
    }

    /** One-step return to actual saved cursors. No scenario or receipt write. */
    private void goToday() {
        if (geometry == null) return;
        todayMode = true;
        historyMode = false;
        checkpointIndex = 0;
        year = HifzClock.today().getYear();
        refresh(geometry); // reload real read-only V6 evidence; no forecast shortcut
    }

    /** Touching a rail dot selects its SURAH finish, updating all 3 rings,
     * both progress scales and the inline label. The label opens Mushaf.
     * Only the terminal goal diamond outside the year's dots opens directly. */
    private void selectForecastMilestone(boolean sabqi, CapHifzForecast.Milestone point) {
        if (historyMode || point == null || point.verse == null) return;
        for (int i = 0; i < shownCheckpoints.size(); i++) {
            MilestoneChoice choice = shownCheckpoints.get(i);
            if (choice.sabqi == sabqi && choice.verse.equals(point.verse)
                    && choice.date.equals(point.date)) {
                checkpointIndex = i;
                todayMode = false;
                updateCheckpointLabel();
                return;
            }
        }
        // A terminal diamond is a genuine Mushaf jump, but not a fake
        // milestone of an earlier selected year.
        if (point.verse.equals(sabqi ? SabqiRoute.ROUTE_END : ItqanRotationPolicy.TAIL_END))
            openMushaf(point.verse);
    }

    private void moveForecastYear(int direction) {
        if (historyMode || direction != -1 && direction != 1) return;
        int destination = year + direction;
        if (destination < today.getYear() || destination > lastForecastYear) return;
        year = destination;
        todayMode = false;
        checkpointIndex = 0;
        render();
    }

    private java.util.function.IntUnaryOperator forecastCheckpointCounts() {
        return selectedYear -> checkpointsForYear(forecastQuarters, selectedYear).size();
    }

    private void moveForecastStep(int direction) {
        if (historyMode || direction != -1 && direction != 1) return;
        // From Today there is no future surah finish selected yet.
        if (todayMode) {
            if (direction < 0) return;
            todayMode = false;
            if (shownCheckpoints.isEmpty()) {
                CapHifzNavigation.Cursor next = CapHifzNavigation.move(year, 0, 1,
                    today.getYear(), lastForecastYear, forecastCheckpointCounts());
                if (next.year != year) {
                    year = next.year;
                    checkpointIndex = next.index;
                    render();
                    return;
                }
            }
            updateCheckpointLabel();
            return;
        }
        CapHifzNavigation.Cursor moved = CapHifzNavigation.move(year, checkpointIndex,
            direction, today.getYear(), lastForecastYear, forecastCheckpointCounts());
        if (moved.year == year && moved.index == checkpointIndex) return;
        boolean changedYear = moved.year != year;
        year = moved.year;
        checkpointIndex = moved.index;
        if (changedYear) render();
        else updateCheckpointLabel();
    }

    private void updateCheckpointLabel() {
        if (checkpointLabel == null) return;
        if (todayShortcut != null) {
            todayShortcut.setEnabled(!todayMode || historyMode);
            todayShortcut.setAlpha(todayMode && !historyMode ? .56f : 1f);
        }
        if (timeline != null) timeline.setTodayMode(todayMode && !historyMode);
        if (yearNavigatorGroup != null)
            yearNavigatorGroup.setVisibility(historyMode ? View.GONE : View.VISIBLE);
        if (previousYearNavigator != null) {
            previousYearNavigator.setVisibility(historyMode ? View.GONE : View.VISIBLE);
            previousYearNavigator.setEnabled(!historyMode && year > today.getYear());
            previousYearNavigator.setContentDescription("Prévision : année précédente");
        }
        if (nextYearNavigator != null) {
            nextYearNavigator.setVisibility(historyMode ? View.GONE : View.VISIBLE);
            nextYearNavigator.setEnabled(!historyMode && year < lastForecastYear);
            nextYearNavigator.setContentDescription("Prévision : année suivante");
        }
        CapHifzNavigation.Cursor position = new CapHifzNavigation.Cursor(year, checkpointIndex);
        if (previousNavigator != null) {
            previousNavigator.setEnabled(historyMode
                ? acquiredIndex + 1 < acquiredLines.size()
                : !todayMode && CapHifzNavigation.canMove(position, -1, today.getYear(),
                    lastForecastYear, forecastCheckpointCounts()));
            previousNavigator.setContentDescription(historyMode
                ? "Acquis plus ancien" : "Jalon prévisionnel précédent");
        }
        if (nextNavigator != null) {
            nextNavigator.setEnabled(historyMode
                ? acquiredIndex > 0
                : CapHifzNavigation.canMove(position, 1, today.getYear(),
                    lastForecastYear, forecastCheckpointCounts()));
            nextNavigator.setContentDescription(historyMode
                ? "Acquis plus récent" : "Jalon prévisionnel suivant");
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
        if (todayMode) {
            if (timeline != null) timeline.setSelection(null, true, null);
            String sabqi = currentSabqiVerse == null ? "—"
                : QuranSurahNames.name(currentSabqiVerse.getSurah())
                    + " v." + currentSabqiVerse.getAyah();
            String itqan = currentTailVerse == null ? "—"
                : QuranSurahNames.name(currentTailVerse.getSurah())
                    + " v." + currentTailVerse.getAyah();
            checkpointLabel.setText("Aujourd’hui · " + today.format(FULL_DATE)
                + "\nAcq. " + sabqi + " · Maîtr. " + itqan);
            checkpointLabel.setContentDescription(
                "Situation réelle au " + today.format(FULL_DATE)
                + ". Position de travail Acquisition : " + sabqi
                + ". Position de travail Maîtrise : " + itqan
                + ". Aucune progression anticipée.");
            return;
        }
        if (shownCheckpoints.isEmpty()) {
            checkpointLabel.setText(year + " · Aucun jalon · « » années");
            checkpointLabel.setContentDescription(
                "Cap Hifdh " + year + " : aucun jalon prévisionnel, changer d'année");
            if (timeline != null) timeline.setSelection(null, true);
            return;
        }
        MilestoneChoice choice = shownCheckpoints.get(checkpointIndex);
        String track = choice.sabqi ? "Acquisition" : "Maîtrise";
        checkpointLabel.setText(dropdownPreview
            ? "JALONS ▾  " + track + " · " + (checkpointIndex + 1)
                + "/" + shownCheckpoints.size() + "\n"
                + QuranSurahNames.name(choice.verse.getSurah()) + " · v."
                + choice.verse.getAyah() + " · " + choice.date.format(FULL_DATE)
            : checkpointDisplay(choice));
        checkpointLabel.setContentDescription((dropdownPreview
            ? "Choisir un jalon dans le menu : " : "Ouvrir le Mushaf depuis Cap Hifdh : ")
            + track + ", "
            + choice.date.format(FULL_DATE) + ", "
            + QuranSurahNames.name(choice.verse.getSurah())
            + " " + choice.verse.getAyah() + ". Prévision, pas acquis.");
        if (timeline != null)
            timeline.setSelection(choice.date, choice.sabqi, choice.verse);
    }

    /** Compact Quranic landmark: one true sourate and ayah, with forecast date. */
    private CharSequence checkpointDisplay(MilestoneChoice choice) {
        String lane = choice.sabqi ? "Acquisition" : "Maîtrise";
        // Line 1 identifies which frise owns the selected surah finish;
        // line 2 is its actual canonical final ayah and projected date.
        return lane + " · jalon " + (checkpointIndex + 1) + "/"
            + shownCheckpoints.size() + " · fin prévue\n"
            + QuranSurahNames.name(choice.verse.getSurah()) + " · v."
            + choice.verse.getAyah() + " · " + choice.date.format(FULL_DATE);
    }

    /** Screenshot-only comparison variant: every item is the SAME authentic,
     * forecast-dated final verse as with the arrows. This is not a setting. */
    private void showDropdownPreview() {
        if (!dropdownPreview || historyMode || shownCheckpoints.isEmpty()
                || checkpointLabel == null) return;
        List<String> titles = new ArrayList<>();
        for (MilestoneChoice choice : shownCheckpoints)
            titles.add((choice.sabqi ? "Acquisition" : "Maîtrise") + "  ·  "
                + QuranSurahNames.name(choice.verse.getSurah()) + " · v."
                + choice.verse.getAyah() + "  ·  " + choice.date.format(FULL_DATE));
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                context, android.R.layout.simple_list_item_1, titles) {
            @Override public View getView(int index, View recycled, ViewGroup parent) {
                TextView entry = Ui.text(context, getItem(index),
                    spaciousBoox() ? 16f : 14f, index == checkpointIndex);
                entry.setPadding(Ui.dp(context, 14), Ui.dp(context, 8),
                    Ui.dp(context, 14), Ui.dp(context, 8));
                entry.setMinHeight(Ui.dp(context, 58));
                entry.setMaxLines(2);
                entry.setEllipsize(android.text.TextUtils.TruncateAt.END);
                return entry;
            }
        };
        milestoneDropdown = new ListPopupWindow(context);
        milestoneDropdown.setAdapter(adapter);
        milestoneDropdown.setAnchorView(checkpointLabel);
        milestoneDropdown.setWidth(Math.min(
            context.getResources().getDisplayMetrics().widthPixels - Ui.dp(context, 30),
            Math.max(checkpointLabel.getWidth(), Ui.dp(context, 330))));
        milestoneDropdown.setHeight(Ui.dp(context, Math.min(292, 60 * titles.size())));
        milestoneDropdown.setBackgroundDrawable(
            new android.graphics.drawable.ColorDrawable(Ui.PAPER));
        milestoneDropdown.setModal(true);
        milestoneDropdown.setOnItemClickListener((parent, item, index, id) -> {
            milestoneDropdown.dismiss();
            checkpointIndex = index;
            updateCheckpointLabel();
        });
        milestoneDropdown.show();
    }

    /** Available only from Android instrumentation, not exposed as a user
     * preference or a second production navigation mode. */
    void setDropdownPreviewForCapture(boolean enabled) {
        if (dropdownPreview == enabled) return;
        if (milestoneDropdown != null) milestoneDropdown.dismiss();
        dropdownPreview = enabled;
        render();
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
        // 48dp is the accessible minimum. The weighted BOOX graph needs the
        // freed height to render both full sourah/date rows at font 150%.
        int historyDp = context.getResources().getConfiguration().smallestScreenWidthDp >= 600 ? 48 : 64;
        history.setMinHeight(Ui.dp(context, historyDp));
        // TextView.setMinHeight is not honored when the fixed BOOX root
        // compresses WRAP_CONTENT. Reserve a measurable hit target instead.
        history.setLayoutParams(new LinearLayout.LayoutParams(-1, Ui.dp(context, historyDp)));
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

    /** BOOX: keep ONE 48dp navigation row but distinguish the YEAR pair
     * from the SOURATE-FINISH step pair, using the same Patrick Hand family. */
    private void labelNavigator(TextView view, String glyph, String kind) {
        String content = glyph + "\n" + kind;
        android.text.SpannableString marked = new android.text.SpannableString(content);
        marked.setSpan(new android.text.style.RelativeSizeSpan(1.6f), 0, glyph.length(),
            android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        marked.setSpan(new android.text.style.RelativeSizeSpan(.70f),
            glyph.length() + 1, content.length(),
            android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (kind.isEmpty()) {
            view.setText(glyph);
            view.setTextSize(spaciousBoox() ? 28f : 25f);
            view.setMaxLines(1);
        } else {
            view.setTextSize(spaciousBoox() ? 16f : 14f);
            view.setText(marked);
            view.setMaxLines(2);
        }
        view.setGravity(Gravity.CENTER);
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
