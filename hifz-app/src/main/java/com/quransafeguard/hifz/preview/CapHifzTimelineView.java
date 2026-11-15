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
 * Concise BOOX journey: authentic current cursor, TWO dated projected Juz/Hizb
 * crossings, then the canonical terminal objective, for each active discipline.
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
    private static final float SECOND_ROW = 84f;
    // Keep circular nodes clear of the caption beneath them at BOOX 150%.
    private static final float TRACK_Y = 34f;
    private static final DateTimeFormatter SHORT_DATE =
        DateTimeFormatter.ofPattern("d MMM yy", Locale.FRANCE);
    private static final DateTimeFormatter GOAL_DATE =
        DateTimeFormatter.ofPattern("MMM yyyy", Locale.FRANCE);

    private final List<CapHifzForecast.Quarter> quarters;
    private final LocalDate start;
    private final boolean showSabqi;
    private final boolean showTail;
    private final IntConsumer exploreYear;
    private final BiConsumer<Boolean, CapHifzForecast.Milestone> openMilestone;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int selectedYear;
    private final VerseRef actualSabqiVerse;
    private final VerseRef actualTailVerse;
    private LocalDate selectedDate;
    private boolean selectedSabqi;

    CapHifzTimelineView(Context context, GeometryRepository geometry,
            List<CapHifzForecast.Quarter> quarters, LocalDate start, boolean showSabqi,
            boolean showTail, IntConsumer exploreYear,
            BiConsumer<Boolean, CapHifzForecast.Milestone> openMilestone, int selectedYear,
            VerseRef actualSabqiVerse, VerseRef actualTailVerse) {
        super(context);
        if (geometry == null || quarters == null || quarters.isEmpty() || start == null)
            throw new IllegalArgumentException("Real QCF and dated forecast required");
        CapHifzTimelineOrder.verify(quarters);
        this.quarters = quarters;
        this.start = start;
        this.showSabqi = showSabqi;
        this.showTail = showTail;
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
        spoken.append("Touchez un jalon pour ouvrir son verset dans le Mushaf. ")
            .append("Touchez un espace libre pour voir les autres jalons.");
        setContentDescription(spoken.toString());
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        setClickable(true);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    private float dp(float n) { return n * getResources().getDisplayMetrics().density; }
    private float sp(float n) { return n * getResources().getDisplayMetrics().scaledDensity; }
    /** Large E-Ink screens need readable labels rather than phone-sized text. */
    private float labelScale() { return getWidth() >= dp(720) ? 1.55f : 1f; }
    private float laneSeparation() { return labelScale() > 1f ? 160f : SECOND_ROW; }
    private float trackCenter() { return labelScale() > 1f ? 60f : TRACK_Y; }
    private float titleBaseline() { return labelScale() > 1f ? 28f : 18f; }
    private float numberBaseline() { return labelScale() > 1f ? 110f : 64f; }
    private float dateBaseline() { return labelScale() > 1f ? 143f : 83f; }

    void setSelection(LocalDate date, boolean sabqi) {
        selectedDate = date;
        selectedSabqi = sabqi;
        invalidate();
    }

    private LocalDate windowStart() {
        return selectedYear == start.getYear() ? start : LocalDate.of(selectedYear, 1, 1);
    }

    private List<CapHifzForecast.Milestone> nextMilestones(boolean sabqi) {
        ArrayList<CapHifzForecast.Milestone> upcoming = new ArrayList<>();
        for (CapHifzForecast.Quarter q : quarters) {
            List<CapHifzForecast.Milestone> candidates =
                sabqi ? q.sabqiMilestones : q.tailMilestones;
            for (CapHifzForecast.Milestone point : candidates) {
                if (point.date != null && !point.date.isBefore(windowStart())
                        && !point.date.isBefore(start)) upcoming.add(point);
            }
        }
        upcoming.sort(Comparator.comparing((CapHifzForecast.Milestone p) -> p.date)
            .thenComparingInt(p -> p.hizb));
        return upcoming.subList(0, Math.min(2, upcoming.size()));
    }

    /** Spoken names are complete; visible stages show an icon and number only. */
    private static boolean isJuz(CapHifzForecast.Milestone point) {
        return point.hizb % 2 == 1;
    }

    private static String milestoneNumber(CapHifzForecast.Milestone point) {
        return Integer.toString(isJuz(point) ? (point.hizb + 1) / 2 : point.hizb);
    }

    private static String spokenMilestone(CapHifzForecast.Milestone point) {
        return (isJuz(point) ? "Juz " : "Hizb ") + milestoneNumber(point);
    }

    private static String spokenVerse(VerseRef verse) {
        return verse == null ? "non déterminée"
            : QuranSurahNames.name(verse.getSurah()) + ", verset " + verse.getAyah();
    }

    /** One offline vector pictogram per type, reused in the inline navigator. */
    private void symbol(Canvas canvas, CapHifzForecast.Milestone point, float x, float y) {
        int resource = isJuz(point) ? R.drawable.ic_cap_juz : R.drawable.ic_cap_hizb;
        android.graphics.drawable.Drawable icon = getContext().getDrawable(resource);
        if (icon == null) return;
        int extent = Math.round(dp(labelScale() > 1f ? 18f : 11.5f));
        icon.setBounds(Math.round(x) - extent, Math.round(y) - extent,
            Math.round(x) + extent, Math.round(y) + extent);
        icon.draw(canvas);
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
            .append(", position de travail ")
            .append(spokenVerse(sabqi ? actualSabqiVerse : actualTailVerse)).append(". ");
        List<CapHifzForecast.Milestone> upcoming = nextMilestones(sabqi);
        for (CapHifzForecast.Milestone point : upcoming)
            spoken.append("Jalon ").append(spokenMilestone(point)).append(" le ")
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
        paint.setTypeface(Ui.typeface(bold ? Typeface.BOLD : Typeface.NORMAL));
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
        if (showSabqi) drawLane(canvas, true, 0f, left, width);
        if (showTail) drawLane(canvas, false, showSabqi ? laneSeparation() : 0f, left, width);
    }

    private void drawLane(Canvas canvas, boolean sabqi, float row, float left, float width) {
        float top = dp(row);
        float[] x = stations(left, width);
        float y = top + dp(trackCenter());
        VerseRef current = sabqi ? actualSabqiVerse : actualTailVerse;
        List<CapHifzForecast.Milestone> coming = nextMilestones(sabqi);
        LocalDate targetDate = finish(sabqi);
        String target = sabqi ? "AL-FATḤ 29" : "AN-NĀS 6";

        drawFitted(canvas, sabqi ? "ACQUISITION" : "MAÎTRISE",
            left, top + dp(titleBaseline()), 11.5f, INK, Paint.Align.LEFT, true, width * .46f);

        // A schematic sequence rather than an invented time or completion scale.
        stroke(FAINT, 1.2f, true);
        canvas.drawLine(x[0], y, x[3], y, paint);
        paint.setPathEffect(null);
        filled(canvas, x[0], y, 3.6f);
        // Selection halo stays OUTSIDE the icon: no white eraser over the glyph.
        if (selectedDate != null && selectedSabqi == sabqi) {
            for (int i = 0; i < coming.size(); i++) {
                if (selectedDate.equals(coming.get(i).date)) {
                    stroke(INK, 1.2f, false);
                    canvas.drawCircle(x[i + 1], y,
                        dp(labelScale() > 1f ? 21.5f : 13.5f), paint);
                }
            }
            if (targetDate != null && selectedDate.equals(targetDate)) {
                stroke(INK, 1.2f, false);
                canvas.drawCircle(x[3], y,
                    dp(labelScale() > 1f ? 16f : 13.5f), paint);
            }
        }
        for (int i = 0; i < coming.size(); i++)
            symbol(canvas, coming.get(i), x[i + 1], y);
        for (int i = coming.size(); i < 2; i++) ring(canvas, x[i + 1], y, 3.5f, .8f);
        goalDiamond(canvas, x[3], y);

        float w = Math.min(x[1] - x[0], x[2] - x[1]) * .85f;
        // The authentic current position is a surah and ayah, never a page.
        drawFitted(canvas, current == null ? "Sourate —"
                : QuranSurahNames.rawName(current.getSurah()),
            x[0], top + dp(numberBaseline()), 10.5f, INK, Paint.Align.LEFT, true, w);
        drawFitted(canvas, current == null ? "verset —" : "verset " + current.getAyah(),
            x[0], top + dp(dateBaseline()), 9.5f, MID, Paint.Align.LEFT, false, w);

        for (int i = 0; i < 2; i++) {
            float xx = x[i + 1];
            String name = i < coming.size() ? milestoneNumber(coming.get(i)) : "—";
            String date = i < coming.size()
                ? coming.get(i).date.format(SHORT_DATE) : "à déterminer";
            drawFitted(canvas, name, xx, top + dp(numberBaseline()), 12f,
                i == 0 ? INK : MID, Paint.Align.CENTER, true, w);
            drawFitted(canvas, date, xx, top + dp(dateBaseline()), 9.5f, MID,
                Paint.Align.CENTER, false, w);
        }

        float lastW = (x[3] - x[2]) * .94f;
        drawFitted(canvas, target, x[3], top + dp(numberBaseline()), 11f, INK,
            Paint.Align.RIGHT, true, lastW);
        drawFitted(canvas, targetDate != null ? targetDate.format(GOAL_DATE) : "date —",
            x[3], top + dp(dateBaseline()), 9.5f, MID, Paint.Align.RIGHT, false, lastW);
    }

    private CapHifzForecast.Milestone touchedMilestone(boolean sabqi,
            float px, float py, float left, float width) {
        float row = showSabqi && !sabqi ? laneSeparation() : 0f;
        if (Math.abs(py - dp(row + trackCenter())) > dp(38)) return null;
        float[] x = stations(left, width);
        List<CapHifzForecast.Milestone> coming = nextMilestones(sabqi);
        for (int i = 0; i < coming.size(); i++) {
            if (Math.abs(px - x[i + 1]) <= dp(24)) return coming.get(i);
        }
        LocalDate end = finish(sabqi);
        if (end != null && Math.abs(px - x[3]) <= dp(24)) {
            VerseRef verse = sabqi ? SabqiRoute.ROUTE_END : ItqanRotationPolicy.TAIL_END;
            return new CapHifzForecast.Milestone(end, verse, CapHifzForecast.hizb(verse));
        }
        return null;
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
