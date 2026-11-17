package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;

import com.quransafeguard.hifz.core.VerseRef;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.IntConsumer;

/**
 * Concise BOOX journey: authentic current cursor, a dated near-term milestone
 * and the next surah transition, then the canonical terminal objective.
 *
 * Stations have equal spacing ON PURPOSE. They represent ordered steps, not
 * proportional time, acquisition percentage, or earned credits. All dates
 * come only from CapHifzForecast's read-only physical line projection.
 *
 * Touching either future marker or the terminal marker opens the exact QCF
 * Mushaf verse. Touching empty ink cycles the existing inline navigator.
 * No dialog, no scroll, no state/credit write.
 */
final class CapHifzTimelineView extends View {
    private static final int INK = Color.rgb(22, 22, 22);
    private static final int MID = Color.rgb(79, 79, 79);
    private static final int FAINT = Color.rgb(161, 158, 150);
    private static final float SECOND_ROW = 96f;
    // Keep circular nodes clear of the caption beneath them at BOOX 150%.
    private static final float TRACK_Y = 34f;
    private static final DateTimeFormatter SHORT_DATE =
        DateTimeFormatter.ofPattern("d MMM yyyy", Locale.FRANCE);
    private static final DateTimeFormatter GOAL_DATE =
        DateTimeFormatter.ofPattern("MMM yyyy", Locale.FRANCE);

    private final List<CapHifzForecast.Quarter> quarters;
    private final GeometryRepository geometry;
    private final LocalDate start;
    private final boolean showSabqi;
    private final boolean showTail;
    private final boolean showMaintenance;
    private final boolean projectedMaintenance;
    private final VerseRef maintenanceVerse;
    private final int maintenanceJuz;
    private final IntConsumer exploreYear;
    private final BiConsumer<Boolean, CapHifzForecast.Milestone> openMilestone;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int selectedYear;
    private final VerseRef actualSabqiVerse;
    private final VerseRef actualTailVerse;
    private LocalDate selectedDate;
    private boolean selectedSabqi;
    private VerseRef selectedVerse;
    // Today is the CURRENT persisted QCF working position, not the end-of-year
    // scenario. No forecast milestone is falsely credited on this view.
    private boolean todayMode;
    private final String forecastAccessibilityDescription;

    CapHifzTimelineView(Context context, GeometryRepository geometry,
            List<CapHifzForecast.Quarter> quarters, LocalDate start, boolean showSabqi,
            boolean showTail, IntConsumer exploreYear,
            BiConsumer<Boolean, CapHifzForecast.Milestone> openMilestone, int selectedYear,
            VerseRef actualSabqiVerse, VerseRef actualTailVerse,
            boolean showMaintenance, VerseRef maintenanceVerse, int maintenanceJuz,
            boolean projectedMaintenance) {
        super(context);
        if (geometry == null || quarters == null || quarters.isEmpty() || start == null)
            throw new IllegalArgumentException("Real QCF and dated forecast required");
        CapHifzTimelineOrder.verify(quarters);
        this.geometry = geometry;
        this.quarters = quarters;
        this.start = start;
        this.showSabqi = showSabqi;
        this.showTail = showTail;
        this.showMaintenance = showMaintenance;
        this.projectedMaintenance = projectedMaintenance;
        this.maintenanceVerse = maintenanceVerse;
        this.maintenanceJuz = maintenanceJuz;
        this.exploreYear = exploreYear;
        this.openMilestone = openMilestone;
        this.selectedYear = selectedYear;
        this.actualSabqiVerse = actualSabqiVerse;
        this.actualTailVerse = actualTailVerse;
        setBackgroundColor(Ui.PAPER);
        StringBuilder spoken = new StringBuilder("Feuille de route prévisionnelle interactive ")
            .append(selectedYear).append(". Projection à 90 pour cent, pas des acquis. ");
        if (showSabqi) appendSpoken(spoken, true);
        if (showTail) appendSpoken(spoken, false);
        if (showMaintenance) {
            spoken.append("Entretien : cycle sans échéance après An-Nās, verset 6. ");
            if (projectedMaintenance)
                spoken.append("Phase prévue, sans séance de Juz anticipée. ");
            else if (maintenanceVerse != null)
                spoken.append("Séance de Juz ").append(maintenanceJuz)
                    .append(" ouverte à ").append(spokenVerse(maintenanceVerse)).append(". ");
            else spoken.append("Aucune nouvelle séance de Juz ouverte à cet instant. ");
        }
        spoken.append("Un point noir représente une fin de sourate dans son parcours. ")
            .append("Le cercle extérieur indique la fin sélectionnée; ")
            .append("touchez un point pour le sélectionner. ")
            .append("Ouvrez ensuite sa référence en dessous pour accéder au Mushaf. ")
            .append("Touchez un espace libre pour parcourir les jalons.");
        forecastAccessibilityDescription = spoken.toString();
        setContentDescription(forecastAccessibilityDescription);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        setClickable(true);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    private float dp(float n) { return n * getResources().getDisplayMetrics().density; }
    private float sp(float n) { return n * getResources().getDisplayMetrics().scaledDensity; }
    /** Large E-Ink screens need readable labels rather than phone-sized text. */
    private float labelScale() {
        // Patrick Hand remains the sole French UI font. At Android 150%
        // accessibility scaling, the previous 1.55x Canvas multiplier was
        // another zoom, crushing two actual BOOX rails into each other.
        float widthDp = getWidth() / getResources().getDisplayMetrics().density;
        return widthDp >= 720f ? 1.13f : 1f;
    }

    private boolean soloAcquisition() {
        return showSabqi && !showTail && !showMaintenance;
    }

    private float laneSeparation() {
        // Two complete, measured rows that NEVER overlap at 900x1200
        // and also use the extra vertical room on 1404x1872.
        float heightDp = getHeight() / getResources().getDisplayMetrics().density;
        return Math.min(169f, Math.max(0f, heightDp * .50f));
    }

    /** After An-Nās the missing Maîtrise lane releases real screen space.
     * Spread solo Acquisition vertically across that SAME graph viewport,
     * rather than centering a tiny 149dp strip with empty bands above/below.
     * This is layout-only and never affects forecast QCF positions.
     */
    private float soloSpan() {
        float h = getHeight() / getResources().getDisplayMetrics().density;
        return Math.max(100f, Math.min(295f, h - 28f));
    }

    private float titleBaseline() {
        return soloAcquisition() ? 22f : (labelScale() > 1f ? 22f : 15f);
    }

    private float globalTrackBaseline() {
        return soloAcquisition()
            ? 22f + (soloSpan() - 22f) * .27f
            : (labelScale() > 1f ? 51f : 31f);
    }

    private float annualTrackBaseline() {
        return soloAcquisition()
            ? 22f + (soloSpan() - 22f) * .72f
            : (labelScale() > 1f ? 96f : 58f);
    }

    private float captionBaseline() {
        return soloAcquisition()
            ? 22f + (soloSpan() - 22f) * .95f
            : (labelScale() > 1f ? 127f : 81f);
    }

    private float verticalOffset() {
        float heightDp = getHeight() / getResources().getDisplayMetrics().density;
        boolean dual = showSabqi && (showTail || showMaintenance);
        float contentHeight = (dual ? laneSeparation() : 0f) + captionBaseline() + 5f;
        return Math.max(0f, (heightDp - contentHeight) / 2f);
    }

    void setSelection(LocalDate date, boolean sabqi) {
        setSelection(date, sabqi, null);
    }

    void setSelection(LocalDate date, boolean sabqi, VerseRef verse) {
        selectedDate = date;
        selectedSabqi = sabqi;
        selectedVerse = verse;
        updateAccessibility();
        invalidate();
    }

    void setTodayMode(boolean enabled) {
        todayMode = enabled;
        updateAccessibility();
        invalidate();
    }

    private void updateAccessibility() {
        if (todayMode) {
            setContentDescription(forecastAccessibilityDescription
                + " Mode aujourd’hui : position de travail réelle, avant la prochaine "
                + "ligne de QCF. Acquisition " + spokenVerse(actualSabqiVerse)
                + (showTail ? ", Maîtrise " + spokenVerse(actualTailVerse) : "")
                + ". Les fins de sourates futures restent des prévisions, non des acquis.");
        } else if (selectedDate != null && selectedVerse != null) {
            setContentDescription(forecastAccessibilityDescription
                + " Jalon sélectionné sur la progression " + (selectedSabqi ? "Acquisition" : "Maîtrise")
                + " : " + spokenVerse(selectedVerse) + ", fin estimée le "
                + selectedDate.format(SHORT_DATE) + ".");
        } else {
            setContentDescription(forecastAccessibilityDescription);
        }
    }

    private LocalDate windowStart() {
        return selectedYear == start.getYear() ? start : LocalDate.of(selectedYear, 1, 1);
    }

    /** ALL physically completed sourates in the selected calendar year.
     * Every one is a dot on its own track, including crowded Itqān years. */
    private List<CapHifzForecast.Milestone> yearSurahFinishes(boolean sabqi) {
        ArrayList<CapHifzForecast.Milestone> result = new ArrayList<>();
        for (CapHifzForecast.Quarter q : quarters) {
            List<CapHifzForecast.Milestone> points =
                sabqi ? q.sabqiSurahFinishes : q.tailSurahFinishes;
            for (CapHifzForecast.Milestone point : points)
                if (point.date != null && point.date.getYear() == selectedYear)
                    result.add(point);
        }
        result.sort(Comparator.comparing((CapHifzForecast.Milestone p) -> p.date)
            .thenComparingInt(p -> GeometryRepository.ordinal(p.verse)));
        return result;
    }

    /** End-of-selected-year *predicted* reading position, never earned credit.
     * The same quarter projection feeds the estimated surah finish dates.
     * Do not use the immutable current cursor for future-year progress. */
    private VerseRef projectedPositionAtYear(boolean sabqi) {
        return CapHifzForecast.projectedPositionAtYear(quarters, selectedYear,
            sabqi, sabqi ? actualSabqiVerse : actualTailVerse);
    }

    /** Normalized physical QCF-line position along the COMPLETE route,
     * not the selected year's number of surah dots. */
    /** Physical route percentage of the LAST completed QCF line before
     * the persisted next-line cursor; never a phantom receipt. */
    private float actualProgress(boolean sabqi) {
        VerseRef next = sabqi ? actualSabqiVerse : actualTailVerse;
        if (next == null) return 0f;
        VerseRef begin = sabqi ? new VerseRef(2, 1) : ItqanRotationPolicy.TAIL_START;
        VerseRef finalVerse = sabqi ? SabqiRoute.ROUTE_END : ItqanRotationPolicy.TAIL_END;
        int first = geometry.firstLineIndex(begin);
        int finalLine = geometry.lastLineIndex(finalVerse);
        int lastCompleted = geometry.firstLineIndex(next) - 1;
        return Math.max(0f, Math.min(1f,
            (lastCompleted - first + 1f) / Math.max(1f, finalLine - first + 1f)));
    }

    private float selectedMilestoneProgress(boolean sabqi) {
        if (selectedDate == null || selectedVerse == null || selectedSabqi != sabqi)
            return Float.NaN;
        VerseRef begin = sabqi ? new VerseRef(2, 1) : ItqanRotationPolicy.TAIL_START;
        VerseRef finalVerse = sabqi ? SabqiRoute.ROUTE_END : ItqanRotationPolicy.TAIL_END;
        int first = geometry.firstLineIndex(begin);
        int finalLine = geometry.lastLineIndex(finalVerse);
        int line = geometry.lastLineIndex(selectedVerse);
        return Math.max(0f, Math.min(1f,
            (line - first + 1f) / Math.max(1f, finalLine - first + 1f)));
    }

    private float projectedProgress(boolean sabqi) {
        VerseRef projected = projectedPositionAtYear(sabqi);
        if (projected == null) return 0f;
        VerseRef from = sabqi ? new VerseRef(2, 1) : ItqanRotationPolicy.TAIL_START;
        VerseRef to = sabqi ? SabqiRoute.ROUTE_END : ItqanRotationPolicy.TAIL_END;
        int first = geometry.firstLineIndex(from);
        int last = geometry.lastLineIndex(to);
        int position = geometry.lastLineIndex(projected);
        return Math.max(0f, Math.min(1f,
            (position - first) / (float) Math.max(1, last - first)));
    }

    private static float pinX(int index, int total, float left, float width, float margin) {
        return left + margin + (width - 2f * margin) * (index + 1f) / (total + 1f);
    }

    private List<CapHifzForecast.Milestone> nextMilestones(boolean sabqi) {
        ArrayList<CapHifzForecast.Milestone> upcoming = new ArrayList<>();
        // Every item is a true SURAH FINISH, not a Juz/Hizb transition.
        VerseRef physicalCursor = sabqi ? actualSabqiVerse : actualTailVerse;
        int cursorOrdinal = physicalCursor == null ? -1
            : GeometryRepository.ordinal(physicalCursor);
        LocalDate visibleFrom = windowStart();
        if (selectedDate != null && selectedSabqi == sabqi
                && selectedDate.isAfter(visibleFrom)) visibleFrom = selectedDate;
        for (CapHifzForecast.Milestone point : yearSurahFinishes(sabqi)) {
            if (point.date != null && point.verse != null
                    && GeometryRepository.ordinal(point.verse) > cursorOrdinal
                    && !point.date.isBefore(visibleFrom)
                    && !point.date.isBefore(start)
                    && (selectedDate == null || selectedSabqi != sabqi
                        || !point.date.equals(selectedDate) || selectedVerse == null
                        || GeometryRepository.ordinal(point.verse)
                            >= GeometryRepository.ordinal(selectedVerse)))
                upcoming.add(point);
        }
        upcoming.sort(Comparator.comparing((CapHifzForecast.Milestone p) -> p.date)
            .thenComparingInt(p -> GeometryRepository.ordinal(p.verse)));
        // Two readable labels fit, but ALL endings are still distinct visible
        // pins on the year rail and addressable through the checkpoint arrows.
        return CapHifzForecast.visibleMilestones(upcoming);
    }

    private static String spokenVerse(VerseRef verse) {
        return verse == null ? "non déterminée"
            : QuranSurahNames.name(verse.getSurah()) + ", verset " + verse.getAyah();
    }

    private LocalDate finish(boolean sabqi) {
        for (CapHifzForecast.Quarter quarter : quarters) {
            LocalDate date = sabqi ? quarter.sabqiFinish : quarter.tailFinish;
            if (date != null) return date;
        }
        return null;
    }

    private void appendSpoken(StringBuilder spoken, boolean sabqi) {
        spoken.append(sabqi ? "Acquisition" : "Maîtrise")
            .append(", ").append(yearSurahFinishes(sabqi).size())
            .append(" fins de sourates projetées dans l'année. Position fin d'année estimée ")
            .append(spokenVerse(projectedPositionAtYear(sabqi)))
            .append(". Position réelle de travail ")
            .append(spokenVerse(sabqi ? actualSabqiVerse : actualTailVerse)).append(". ");
        List<CapHifzForecast.Milestone> upcoming = nextMilestones(sabqi);
        for (CapHifzForecast.Milestone point : upcoming)
            spoken.append("Jalon ").append(spokenVerse(point.verse)).append(" le ")
                .append(point.date.format(SHORT_DATE)).append(". ");
        spoken.append("Objectif final ").append(sabqi ? "Al-Fatḥ" : "An-Nās");
        LocalDate end = finish(sabqi);
        if (end != null) spoken.append(" vers ").append(end.format(GOAL_DATE));
        else spoken.append(", date indéterminée");
        spoken.append(". ");
    }

    private void stroke(int color, float width, boolean dashed) {
        paint.reset();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(color);
        paint.setStrokeWidth(dp(width));
        paint.setStrokeCap(Paint.Cap.ROUND);
        if (dashed) paint.setPathEffect(new DashPathEffect(
            new float[]{dp(3.5f), dp(3.5f)}, 0));
    }

    private void drawFitted(Canvas canvas, String value, float x, float y,
            float size, int color, Paint.Align align, boolean bold, float width) {
        paint.reset();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        paint.setTextSize(sp(size * labelScale()));
        paint.setTypeface(Ui.typeface(getContext(), bold ? Typeface.BOLD : Typeface.NORMAL));
        paint.setTextAlign(align);
        String shown = value;
        while (shown.length() > 2 && paint.measureText(shown) > width)
            shown = value.substring(0, Math.max(1, shown.length() - 2)) + "…";
        canvas.drawText(shown, x, y, paint);
    }

    private void filled(Canvas canvas, float x, float y, float radius) {
        paint.reset();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(INK);
        canvas.drawCircle(x, y, dp(radius), paint);
    }

    private void ring(Canvas canvas, float x, float y, float radius, float weight) {
        paint.reset();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Ui.PAPER);
        canvas.drawCircle(x, y, dp(radius + 1f), paint);
        stroke(INK, weight, false);
        canvas.drawCircle(x, y, dp(radius), paint);
    }

    private void goalDiamond(Canvas canvas, float x, float y) {
        float r = dp(labelScale() > 1f ? 10f : 6f);
        Path diamond = new Path();
        diamond.moveTo(x, y - r);
        diamond.lineTo(x + r, y);
        diamond.lineTo(x, y + r);
        diamond.lineTo(x - r, y);
        diamond.close();
        paint.reset();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(INK);
        canvas.drawPath(diamond, paint);
    }

    private float[] stations(float left, float width) {
        return new float[]{left + dp(6), left + width * .34f,
            left + width * .64f, left + width - dp(6)};
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float left = dp(16);
        float width = getWidth() - dp(32);
        if (width <= dp(140) || getHeight() < dp(78)) return;
        float offset = verticalOffset();
        if (showSabqi) drawLane(canvas, true, offset, left, width);
        if (showTail) drawLane(canvas, false,
            offset + (showSabqi ? laneSeparation() : 0f), left, width);
        if (showMaintenance)
            drawMaintenanceLane(canvas,
                offset + (showSabqi ? laneSeparation() : 0f), left, width);
    }

    private void drawLane(Canvas canvas, boolean sabqi, float row, float left, float width) {
        float top = dp(row);
        float[] x = stations(left, width);
        float globalY = top + dp(globalTrackBaseline());
        float annualY = top + dp(annualTrackBaseline());
        List<CapHifzForecast.Milestone> annual = yearSurahFinishes(sabqi);
        VerseRef yearEnd = projectedPositionAtYear(sabqi);
        VerseRef terminal = sabqi ? SabqiRoute.ROUTE_END : ItqanRotationPolicy.TAIL_END;
        LocalDate terminalDate = finish(sabqi);
        String track = sabqi ? "ACQUISITION" : "MAÎTRISE";
        String terminalText = QuranSurahNames.rawName(terminal.getSurah())
            + " · v." + terminal.getAyah();
        String yearEndText = yearEnd == null ? "—"
            : QuranSurahNames.rawName(yearEnd.getSurah()) + " · v." + yearEnd.getAyah();

        // Header: real annual projected position. Never mistake a prediction
        // for an acquired lesson, or show today's cursor as the year 2032.
        drawFitted(canvas, track, left, top + dp(titleBaseline()),
            soloAcquisition() ? 20f : 15f, INK, Paint.Align.LEFT, true, width * .43f);
        drawFitted(canvas, todayMode ? "Aujourd’hui · position de travail"
                : "Fin " + selectedYear + " : " + yearEndText,
            left + width, top + dp(titleBaseline()),
            soloAcquisition() ? 14.5f : 12f, MID, Paint.Align.RIGHT, false, width * .57f);

        // Helpful micro-labels in the extra post-Nās height, keeping the
        // graphic light enough for e-ink and distinguishing both scales.
        if (soloAcquisition()) {
            drawFitted(canvas, "Parcours global", left,
                top + dp(globalTrackBaseline() - 20f), 14f, MID,
                Paint.Align.LEFT, false, width * .44f);
            if (annualTrackBaseline() - globalTrackBaseline() >= 73f)
                drawFitted(canvas, "Fins de sourates en " + selectedYear,
                    left, top + dp(annualTrackBaseline() - 22f), 14f,
                    MID, Paint.Align.LEFT, false, width * .70f);
        }

        // Global position: continuous real QCF-line progress up to the
        // requested year, 2029 ... 2037. This is NOT the annual pin scale.
        float fraction = todayMode ? actualProgress(sabqi) : projectedProgress(sabqi);
        float projectedX = x[0] + (x[3] - x[0]) * fraction;
        stroke(FAINT, 1.15f, true);
        canvas.drawLine(x[0], globalY, x[3], globalY, paint);
        stroke(INK, soloAcquisition() ? 2.8f : 2.1f, false);
        canvas.drawLine(x[0], globalY, projectedX, globalY, paint);
        filled(canvas, projectedX, globalY, soloAcquisition() ? 4.8f : 3.7f);
        goalDiamond(canvas, x[3], globalY);
        // The triple concentric symbol is tied to the QCF line progression:
        // Today -> the authentic next-line cursor; forecast selection -> the
        // chosen surah's real final line (while fill still means year-end).
        float markerFraction = todayMode
            ? actualProgress(sabqi) : selectedMilestoneProgress(sabqi);
        if (!Float.isNaN(markerFraction)) {
            float markerX = x[0] + (x[3] - x[0]) * markerFraction;
            if (markerX < x[3] - dp(13f)) {
                ring(canvas, markerX, globalY, 10.5f, 1.4f);
                ring(canvas, markerX, globalY, 6.7f, 1.2f);
                filled(canvas, markerX, globalY, 3.8f);
            }
        }
        String goalDate = terminalDate == null ? "date —" : terminalDate.format(SHORT_DATE);
        // Leave a real gap below/left of the final diamond: the last
        // digit of a four-digit year must never sit underneath the symbol.
        drawFitted(canvas, "Objectif · " + terminalText + " · " + goalDate,
            x[3] - dp(16), globalY + dp(labelScale() > 1f ? 31f : 28f),
            soloAcquisition() ? 14f : 11f,
            MID, Paint.Align.RIGHT, false, width * .86f);

        // Annual zoom. Every dot is an actual surah FINISH in this year
        // (not arbitrary Juz/Hizb milestones). The three concentric forms
        // mark the automatically selected dated end of a specific surah.
        stroke(FAINT, .85f, false);
        canvas.drawLine(x[0] + dp(10), annualY, x[3] - dp(10), annualY, paint);
        int selectedIndex = -1;
        for (int i = 0; i < annual.size(); i++) {
            CapHifzForecast.Milestone p = annual.get(i);
            float px = pinX(i, annual.size(), left, width, dp(20));
            boolean selected = !todayMode && selectedDate != null && selectedSabqi == sabqi
                && selectedDate.equals(p.date) && selectedVerse != null
                && selectedVerse.equals(p.verse);
            if (selected) {
                selectedIndex = i;
                ring(canvas, px, annualY, 12f, 1.2f);
                ring(canvas, px, annualY, 7.4f, 1.2f);
                filled(canvas, px, annualY, 3.8f);
            } else {
                filled(canvas, px, annualY, annual.size() > 40 ? 1.7f : 3f);
            }
        }

        // Do NOT repeat the dated surah/ayah label: it is already shown in
        // the inline navigator under the frise. A one-line prompt links the
        // three-ring selected point to the sole date detail below.
        String annualCaption = todayMode
            ? "Aujourd’hui · repère réel · prévisions disponibles →"
            : annual.isEmpty()
                ? "Aucune fin de sourate prévue en " + selectedYear
                : selectedIndex >= 0
                    ? "Jalon " + (selectedIndex + 1) + "/" + annual.size()
                        + " sélectionné · détail sous la frise"
                    : annual.size() + " fins de sourates en " + selectedYear;
        drawFitted(canvas, annualCaption, left, top + dp(captionBaseline()),
            soloAcquisition() ? 15f : 12f, MID,
            Paint.Align.LEFT, false, width * .95f);
    }

    /** Post-Nās is a continuing activity, NOT a second forecast rail.
     * A compact Entretien status replaces the old four fictitious stations.
     * A session name is shown only when the genuine maintenance engine
     * has opened one; neither this view nor a tap creates a credit. */
    private void drawMaintenanceLane(Canvas canvas, float row, float left, float width) {
        float top = dp(row);
        drawFitted(canvas, "ENTRETIEN", left, top + dp(titleBaseline()),
            13f, INK, Paint.Align.LEFT, true, width * .46f);
        String description = projectedMaintenance ? "Après الناس · v.6 (prévision)"
            : maintenanceVerse == null ? "Cycle continu après الناس · v.6"
            : QuranSurahNames.rawName(maintenanceVerse.getSurah())
                + " · v." + maintenanceVerse.getAyah() + " · séance en cours";
        drawFitted(canvas, description, left, top + dp(captionBaseline()),
            14f, INK, Paint.Align.LEFT, false, width - dp(16));
        // No future finish date, false milestone, empty diamonds or
        // unnecessary duplicate Maîtrise progression after An-Nās.
    }

    private CapHifzForecast.Milestone touchedMilestone(boolean sabqi,
            float px, float py, float left, float width) {
        float row = verticalOffset() + (showSabqi && !sabqi ? laneSeparation() : 0f);
        float globalY = dp(row + globalTrackBaseline());
        float annualY = dp(row + annualTrackBaseline());
        float[] x = stations(left, width);

        LocalDate end = finish(sabqi);
        if (end != null && Math.abs(py - globalY) <= dp(15)
                && Math.abs(px - x[3]) <= dp(15)) {
            VerseRef verse = sabqi ? SabqiRoute.ROUTE_END : ItqanRotationPolicy.TAIL_END;
            return new CapHifzForecast.Milestone(end, verse, CapHifzForecast.hizb(verse));
        }
        if (Math.abs(py - annualY) > dp(17)) return null;
        List<CapHifzForecast.Milestone> annual = yearSurahFinishes(sabqi);
        float nearestDistance = dp(16);
        CapHifzForecast.Milestone nearest = null;
        for (int i = 0; i < annual.size(); i++) {
            float target = pinX(i, annual.size(), left, width, dp(20));
            float distance = Math.abs(px - target);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = annual.get(i);
            }
        }
        return nearest;
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) return true;
        if (event.getAction() == MotionEvent.ACTION_UP) {
            float left = dp(16), width = Math.max(1f, getWidth() - dp(32));
            if (openMilestone != null) {
                CapHifzForecast.Milestone hit = showSabqi
                    ? touchedMilestone(true, event.getX(), event.getY(), left, width) : null;
                if (hit != null) {
                    openMilestone.accept(true, hit);
                    super.performClick();
                    return true;
                }
                hit = showTail
                    ? touchedMilestone(false, event.getX(), event.getY(), left, width) : null;
                if (hit != null) {
                    openMilestone.accept(false, hit);
                    super.performClick();
                    return true;
                }
            }
            if (showMaintenance && maintenanceVerse != null && openMilestone != null) {
                // Entire compact status opens the authentic ongoing session's
                // verse; no fake checkpoint can be selected after An-Nās.
                float maintRow = verticalOffset() + (showSabqi ? laneSeparation() : 0f);
                if (event.getX() >= left && event.getX() <= left + width
                        && Math.abs(event.getY() - dp(maintRow + captionBaseline())) <= dp(26)) {
                    openMilestone.accept(false, new CapHifzForecast.Milestone(
                        start, maintenanceVerse, CapHifzForecast.hizb(maintenanceVerse)));
                    super.performClick();
                    return true;
                }
            }
            performClick();
            return true;
        }
        return super.onTouchEvent(event);
    }

    @Override public boolean performClick() {
        super.performClick();
        if (exploreYear != null) exploreYear.accept(selectedYear);
        return true;
    }
}
