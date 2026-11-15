package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;

import java.time.LocalDate;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.IntConsumer;

/**
 * BOOX one-year ATLAS-style zoom of the real, read-only Hifdh forecast.
 *
 * The whole twenty-year forecast must NEVER be squeezed into 300 pixels.
 * The arrows beneath this view navigate actual calendar years. Juz entrance
 * rings and Hizb markers retain their exact projected crossing dates. A tap
 * on a marker opens its canonical QCF Mushaf verse, while tapping elsewhere
 * opens the accessible detailed milestone explorer. No credits are written.
 */
final class CapHifzTimelineView extends View {
    private static final int INK = Color.rgb(22, 22, 22);
    private static final int MID = Color.rgb(79, 79, 79);
    private static final int FAINT = Color.rgb(182, 179, 169);

    private final List<CapHifzForecast.Quarter> quarters;
    private final LocalDate start;
    private final boolean showSabqi;
    private final boolean showTail;
    private final IntConsumer exploreYear;
    private final BiConsumer<Boolean, CapHifzForecast.Milestone> openMilestone;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int selectedYear;

    CapHifzTimelineView(Context context, GeometryRepository geometry,
            List<CapHifzForecast.Quarter> quarters, LocalDate start, boolean showSabqi,
            boolean showTail, IntConsumer exploreYear,
            BiConsumer<Boolean, CapHifzForecast.Milestone> openMilestone, int selectedYear) {
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
        setBackgroundColor(Ui.PAPER);
        setContentDescription("Feuille de route prévisionnelle interactive " + selectedYear
            + ". Les cercles signalent les entrées dans les Juz, les petits points les Hizb. "
            + "Toucher un jalon ouvre sa page exacte du Mushaf. "
            + "Toucher ailleurs ouvre la liste des étapes de cette année. "
            + "Les échéances simulées ne sont pas des acquis.");
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        setClickable(true);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    private float dp(float n) { return n * getResources().getDisplayMetrics().density; }
    private float sp(float n) { return n * getResources().getDisplayMetrics().scaledDensity; }

    private LocalDate windowStart() {
        return selectedYear == start.getYear() ? start : LocalDate.of(selectedYear, 1, 1);
    }

    private LocalDate windowEnd() {
        return LocalDate.of(selectedYear, 12, 31);
    }

    private float x(LocalDate date, float left, float width) {
        long from = windowStart().toEpochDay();
        long to = windowEnd().toEpochDay();
        float portion = to <= from ? 0f
            : (date.toEpochDay() - from) / (float)(to - from);
        return left + width * Math.max(0f, Math.min(1f, portion));
    }

    private void stroke(int color, float width) {
        paint.reset();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(color);
        paint.setStrokeWidth(dp(width));
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
    }

    private void text(Canvas canvas, String value, float x, float y,
            float size, int color, Paint.Align align, boolean bold) {
        paint.reset();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        paint.setTextSize(sp(size));
        paint.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
        paint.setTextAlign(align);
        canvas.drawText(value, x, y, paint);
    }

    private void rings(Canvas canvas, float x, float y, int count) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Ui.PAPER);
        canvas.drawCircle(x, y, dp(2.7f * count), paint);
        stroke(INK, 1f);
        for (int i = 1; i <= count; i++) canvas.drawCircle(x, y, dp(2.7f * i), paint);
    }

    private LocalDate finish(boolean sabqi) {
        for (CapHifzForecast.Quarter quarter : quarters) {
            LocalDate date = sabqi ? quarter.sabqiFinish : quarter.tailFinish;
            if (date != null) return date;
        }
        return null;
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        final float left = dp(16), right = getWidth() - dp(16);
        final float width = right - left;
        if (width <= dp(72) || getHeight() <= dp(78)) return;

        if (showSabqi) drawLane(canvas, true, left, right, dp(30));
        if (showTail) drawLane(canvas, false, left, right,
            showSabqi ? dp(67) : dp(30));

        float yearY = getHeight() - dp(8);
        stroke(FAINT, .7f);
        canvas.drawLine(left, yearY - dp(12), right, yearY - dp(12), paint);
        for (int month : new int[]{3, 6, 9, 12}) {
            LocalDate quarter = LocalDate.of(selectedYear, month, 1).plusMonths(1).minusDays(1);
            if (quarter.isBefore(windowStart())) continue;
            float xx = x(quarter, left, width);
            stroke(FAINT, .7f);
            canvas.drawLine(xx, yearY - dp(14), xx, yearY - dp(9), paint);
        }
        text(canvas, windowStart().getMonthValue() + "/" + selectedYear,
            left, yearY, 10f, MID, Paint.Align.LEFT, false);
        text(canvas, "DÉC", right, yearY, 10f, MID, Paint.Align.RIGHT, false);
    }

    private void drawLane(Canvas canvas, boolean sabqi,
            float left, float right, float centerY) {
        float width = right - left;
        text(canvas, sabqi ? "NOUVEAUX" : "MAÎTRISE", left,
            centerY - dp(15), 10.5f, INK, Paint.Align.LEFT, true);
        text(canvas, sabqi ? "Vers Al-Fatḥ" : "Vers An-Nās", right,
            centerY - dp(15), 10.5f, MID, Paint.Align.RIGHT, false);

        stroke(FAINT, 1.0f);
        canvas.drawLine(left, centerY, right, centerY, paint);

        LocalDate end = finish(sabqi);
        if (end == null || !end.isBefore(windowStart())) {
            float endX = end == null || end.isAfter(windowEnd())
                ? right : x(end, left, width);
            stroke(sabqi ? INK : MID, 1.7f);
            if (!sabqi)
                paint.setPathEffect(new DashPathEffect(
                    new float[]{dp(4.2f), dp(3.3f)}, 0));
            canvas.drawLine(left, centerY, endX, centerY, paint);
            paint.setPathEffect(null);
            if (end != null && end.getYear() == selectedYear)
                rings(canvas, endX, centerY, 3);
        }

        // Show all usable crossings at year scale; Juz entrances are two
        // concentric rings, other Hizb are tiny outlined dots. A dense day's
        // markers can overlap visually but remain all accessible in the list.
        float lastLabelX = left - dp(32);
        for (CapHifzForecast.Quarter quarter : quarters) {
            List<CapHifzForecast.Milestone> points =
                sabqi ? quarter.sabqiMilestones : quarter.tailMilestones;
            for (CapHifzForecast.Milestone point : points) {
                if (point.date == null || point.date.getYear() != selectedYear
                    || point.date.isBefore(windowStart())) continue;
                float xx = x(point.date, left, width);
                boolean juzStart = point.hizb % 2 == 1;
                if (juzStart) rings(canvas, xx, centerY, 2);
                else {
                    paint.setStyle(Paint.Style.FILL);
                    paint.setColor(Ui.PAPER);
                    canvas.drawCircle(xx, centerY, dp(3f), paint);
                    stroke(MID, 1f);
                    canvas.drawCircle(xx, centerY, dp(3f), paint);
                }
                if (juzStart && xx - lastLabelX >= dp(34)
                        && right - xx >= dp(14)) {
                    text(canvas, "J" + ((point.hizb + 1) / 2), xx,
                        centerY + dp(14), 9.5f, MID, Paint.Align.CENTER, false);
                    lastLabelX = xx;
                }
            }
        }
    }

    private CapHifzForecast.Milestone touchedMilestone(boolean sabqi,
            float tapX, float tapY, float left, float width) {
        float centerY = sabqi ? dp(30) : showSabqi ? dp(67) : dp(30);
        if (Math.abs(tapY - centerY) > dp(17)) return null;
        // The large concentric destination is a real touch target as well:
        // Al-Fatḥ / An-Nās open at the exact last verse of the route.
        LocalDate goal = finish(sabqi);
        if (goal != null && goal.getYear() == selectedYear
                && Math.abs(x(goal, left, width) - tapX) <= dp(19)) {
            com.quransafeguard.hifz.core.VerseRef verse = sabqi
                ? SabqiRoute.ROUTE_END : ItqanRotationPolicy.TAIL_END;
            return new CapHifzForecast.Milestone(goal, verse, CapHifzForecast.hizb(verse));
        }
        CapHifzForecast.Milestone nearest = null;
        float distance = dp(17);
        for (CapHifzForecast.Quarter q : quarters) {
            List<CapHifzForecast.Milestone> points =
                sabqi ? q.sabqiMilestones : q.tailMilestones;
            for (CapHifzForecast.Milestone p : points) {
                if (p.date == null || p.date.getYear() != selectedYear
                    || p.date.isBefore(windowStart())) continue;
                float dx = Math.abs(x(p.date, left, width) - tapX);
                if (dx <= distance) {
                    distance = dx;
                    nearest = p;
                }
            }
        }
        return nearest;
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) return true;
        if (event.getAction() == MotionEvent.ACTION_UP) {
            float left = dp(16), width = Math.max(1, getWidth() - dp(32));
            if (openMilestone != null) {
                CapHifzForecast.Milestone hit = showSabqi
                    ? touchedMilestone(true, event.getX(), event.getY(), left, width)
                    : null;
                if (hit != null) {
                    openMilestone.accept(true, hit);
                    super.performClick();
                    return true;
                }
                hit = showTail
                    ? touchedMilestone(false, event.getX(), event.getY(), left, width)
                    : null;
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
