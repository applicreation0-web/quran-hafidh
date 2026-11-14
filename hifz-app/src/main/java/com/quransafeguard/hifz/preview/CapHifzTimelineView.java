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
import java.util.function.IntConsumer;

/**
 * A two-track ink-on-paper Hifz road map, not a Cartesian line chart.
 *
 * The horizontal axis is strictly chronological. Checkpoints are placed at
 * the first projected quarter reaching each canonical Juz, so none can be
 * reordered, mirrored or spaced as though all Juz took equal time.
 * The two tracks are NEVER joined. The positions are FORECASTS, not credits.
 */
final class CapHifzTimelineView extends View {
    private static final int INK = Color.rgb(22, 22, 22);
    private static final int MID = Color.rgb(79, 79, 79);
    private static final int FAINT = Color.rgb(182, 179, 169);
    private static final int[] SABQI_JUZ = {2, 5, 10, 15, 20, 25, 26};
    private static final int[] ITQAN_JUZ = {27, 28, 29, 30};

    private final List<CapHifzForecast.Quarter> quarters;
    private final boolean showSabqi;
    private final boolean showTail;
    private final IntConsumer selectYear;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int selectedYear;

    CapHifzTimelineView(Context context, GeometryRepository geometry,
            List<CapHifzForecast.Quarter> quarters, boolean showSabqi,
            boolean showTail, IntConsumer selectYear, int selectedYear) {
        super(context);
        if (geometry == null || quarters == null || quarters.isEmpty())
            throw new IllegalArgumentException("Real QCF and dated forecast required");
        CapHifzTimelineOrder.verify(quarters);
        this.quarters = quarters;
        this.showSabqi = showSabqi;
        this.showTail = showTail;
        this.selectYear = selectYear;
        this.selectedYear = selectedYear;
        setBackgroundColor(Ui.PAPER);
        setContentDescription("Feuille de route prévisionnelle, de gauche à droite par année. "
            + "Deux chemins séparés : Sabqi vers Al-Fatḥ et Itqān vers An-Nās. "
            + "Les petits jalons indiquent les Juz atteints selon les fins de trimestre. "
            + "Touchez un point du temps pour ouvrir les trimestres de cette année. "
            + "Cette simulation n'atteste aucune mémorisation.");
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        setClickable(selectYear != null);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    private float dp(float n) { return n * getResources().getDisplayMetrics().density; }
    private float sp(float n) { return n * getResources().getDisplayMetrics().scaledDensity; }

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

    private float x(LocalDate date, float left, float width) {
        long from = quarters.get(0).date.toEpochDay();
        long to = quarters.get(quarters.size() - 1).date.toEpochDay();
        return to <= from ? left
            : left + width * (date.toEpochDay() - from) / (float) (to - from);
    }

    private LocalDate firstReached(int juz, boolean sabqi) {
        return CapHifzTimelineOrder.firstProjectedJuz(quarters, juz, sabqi);
    }

    private LocalDate finish(boolean sabqi) {
        for (CapHifzForecast.Quarter quarter : quarters) {
            if (sabqi ? quarter.sabqiComplete : quarter.firstTailComplete) return quarter.date;
        }
        return null;
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        final float left = dp(16), right = getWidth() - dp(16);
        final float width = right - left;
        if (width <= dp(72) || getHeight() <= dp(95)) return;

        // Separate lanes with generous whitespace; no Cartesian axes, bars,
        // grid or fake step lengths. E-Ink-friendly strokes under 2dp.
        if (showSabqi) drawLane(canvas, true, left, right, dp(61));
        if (showTail) drawLane(canvas, false, left, right, showSabqi ? dp(132) : dp(61));

        float yearY = getHeight() - dp(17);
        stroke(FAINT, .7f);
        canvas.drawLine(left, yearY - dp(12), right, yearY - dp(12), paint);
        int begin = quarters.get(0).date.getYear();
        int end = quarters.get(quarters.size() - 1).date.getYear();
        int interval = end - begin <= 4 ? 1 : end - begin <= 12 ? 3 : 5;
        float lastTextRight = -10000f;
        for (int yr = begin; yr <= end; yr++) {
            if ((yr - begin) % interval != 0 && yr != end) continue;
            LocalDate at = yr == begin ? quarters.get(0).date : LocalDate.of(yr, 1, 1);
            float xx = x(at, left, width);
            // Avoid the common Excel-style pile-up of year labels.
            if (xx - lastTextRight < dp(38) && yr != end) continue;
            stroke(FAINT, .7f);
            canvas.drawLine(xx, yearY - dp(14), xx, yearY - dp(9), paint);
            text(canvas, Integer.toString(yr), xx, yearY, 10.5f, MID,
                yr == begin ? Paint.Align.LEFT : yr == end ? Paint.Align.RIGHT : Paint.Align.CENTER, false);
            lastTextRight = xx + dp(19);
        }
        // The selected year is a fine ink marker, not an intrusive hover box.
        if (selectedYear >= begin && selectedYear <= end) {
            LocalDate chosen = selectedYear == begin ? quarters.get(0).date
                : LocalDate.of(selectedYear, 1, 1);
            float xx = x(chosen, left, width);
            stroke(INK, 1.35f);
            canvas.drawCircle(xx, yearY - dp(12), dp(2.25f), paint);
        }
    }

    private void drawLane(Canvas canvas, boolean sabqi,
            float left, float right, float centerY) {
        float width = right - left;
        float headerY = centerY - dp(34);
        text(canvas, sabqi ? "SABQI" : "ITQĀN", left, headerY, 12f, INK,
            Paint.Align.LEFT, true);
        text(canvas, sabqi ? "Vers Al-Fatḥ" : "Vers An-Nās", right, headerY,
            11.5f, MID, Paint.Align.RIGHT, false);

        stroke(FAINT, 1.0f);
        canvas.drawLine(left, centerY, right, centerY, paint);

        LocalDate end = finish(sabqi);
        LocalDate last = end == null ? quarters.get(quarters.size() - 1).date : end;
        float endX = x(last, left, width);
        stroke(sabqi ? INK : MID, 1.7f);
        if (!sabqi)
            paint.setPathEffect(new DashPathEffect(new float[]{dp(4.2f), dp(3.3f)}, 0));
        canvas.drawLine(left, centerY, endX, centerY, paint);
        paint.setPathEffect(null);

        // Track endpoints are fixed in Quranic order. They are NOT the same
        // as claiming all 30 Juz have been acquired.
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(sabqi ? INK : MID);
        canvas.drawCircle(left, centerY, dp(2.6f), paint);
        if (end != null) {
            stroke(sabqi ? INK : MID, 1.45f);
            canvas.drawCircle(endX, centerY, dp(5.0f), paint);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(endX, centerY, dp(2.2f), paint);
        } else {
            stroke(MID, 1.0f);
            canvas.drawCircle(endX, centerY, dp(3.4f), paint);
        }

        // Sparse, meaningful, canonical Juz checkpoints rather than arbitrary
        // equally-spaced points. Labels are suppressed when they would collide.
        int[] milestones = sabqi ? SABQI_JUZ : ITQAN_JUZ;
        float previousLabelX = left - dp(26);
        for (int milestone : milestones) {
            LocalDate at = firstReached(milestone, sabqi);
            if (at == null) continue;
            float xx = x(at, left, width);
            boolean finalJuz = milestone == (sabqi ? 26 : 30);
            if (xx - previousLabelX < dp(44) || finalJuz && end != null
                    && Math.abs(xx - endX) < dp(12)) continue;
            stroke(sabqi ? INK : MID, 1.1f);
            canvas.drawCircle(xx, centerY, dp(3.1f), paint);
            text(canvas, "J" + milestone, xx, centerY - dp(12), 10.5f,
                MID, Paint.Align.CENTER, false);
            previousLabelX = xx;
        }
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        if (selectYear == null) return super.onTouchEvent(event);
        if (event.getAction() == MotionEvent.ACTION_DOWN) return true;
        if (event.getAction() == MotionEvent.ACTION_UP) {
            float left = dp(16), width = Math.max(1, getWidth() - dp(32));
            float fraction = Math.max(0, Math.min(1, (event.getX() - left) / width));
            long from = quarters.get(0).date.toEpochDay();
            long to = quarters.get(quarters.size() - 1).date.toEpochDay();
            int tappedYear = LocalDate.ofEpochDay(from + Math.round(fraction * (to - from))).getYear();
            selectYear.accept(tappedYear);
            performClick();
            return true;
        }
        return super.onTouchEvent(event);
    }

    @Override public boolean performClick() {
        super.performClick();
        return true;
    }
}
