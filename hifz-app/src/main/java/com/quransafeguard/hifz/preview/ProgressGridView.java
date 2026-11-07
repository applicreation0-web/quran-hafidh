package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

/**
 * A 604-page overview drawn as one Canvas grid (not 604 child Views — cheap to lay out and to
 * redraw). Every status is a distinct fill PATTERN, never a color: hue carries no meaning on an
 * e-ink BOOX screen and color changes ghost, so "Acquis" vs "à stabiliser" vs "en apprentissage"
 * vs "pas commencé" must stay legible in pure black/white.
 */
final class ProgressGridView extends View {
    static final int VIDE = 0;
    static final int APPRENTISSAGE = 1;
    static final int STABILISER = 2;
    static final int ACQUIS = 3;

    private static final int COLUMNS = 22;
    private static final int TOTAL_PAGES = 604;

    interface OnPageTapped { void tapped(int page); }

    private final int[] status = new int[TOTAL_PAGES + 1]; // 1-indexed by page number
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF cellRect = new RectF();
    private OnPageTapped listener;
    private float cellSize;
    private float gap;
    private int firstPage=1, lastPage=TOTAL_PAGES;
    private final Paint pageNumber = new Paint(Paint.ANTI_ALIAS_FLAG);

    ProgressGridView(Context context) {
        super(context);
        fill.setStyle(Paint.Style.FILL);
        fill.setColor(Ui.INK);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setColor(Ui.LINE);
        gap = Ui.dp(context, 1);
        pageNumber.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        pageNumber.setTextAlign(Paint.Align.CENTER);
    }

    void setStatus(int page, int value) {
        if (page >= 1 && page <= TOTAL_PAGES) status[page] = value;
    }

    void setOnPageTapped(OnPageTapped listener) { this.listener = listener; }

    /** Read-only Juz zoom; the 604-page status array and the page-to-QCF route never change. */
    void setPageWindow(int first,int last) {
        if(first<1 || last>604 || first>last)throw new IllegalArgumentException("Invalid Juz page window");
        firstPage=first;lastPage=last;requestLayout();invalidate();
    }
    int firstVisiblePage(){return firstPage;}
    int lastVisiblePage(){return lastPage;}
    private boolean zoomed(){return firstPage!=1 || lastPage!=TOTAL_PAGES;}
    private int columns(){return zoomed() ?
        (getResources().getConfiguration().smallestScreenWidthDp>=600?7:5) : COLUMNS;}
    private int visibleCount(){return lastPage-firstPage+1;}

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        int width = MeasureSpec.getSize(widthSpec);
        cellSize = width / (float) columns();
        int rows = (visibleCount() + columns() - 1) / columns();
        setMeasuredDimension(width, Math.round(rows * cellSize));
    }

    @Override protected void onDraw(Canvas canvas) {
        int cols=columns();
        int rows = (visibleCount() + cols - 1) / cols;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int page = firstPage + row * cols + col;
                if (page > lastPage) break;
                float left = col * cellSize, top = row * cellSize;
                cellRect.set(left + gap, top + gap, left + cellSize - gap, top + cellSize - gap);
                drawCell(canvas, cellRect, status[page], fill, stroke);
                if(zoomed()){
                    pageNumber.setColor(status[page]==ACQUIS?Ui.PAPER:Ui.INK);
                    pageNumber.setTextSize(Math.max(Ui.dp(getContext(),12),cellSize*.21f));
                    canvas.drawText(Integer.toString(page),cellRect.centerX(),
                        cellRect.centerY()-(pageNumber.ascent()+pageNumber.descent())/2,pageNumber);
                }
            }
        }
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        if (listener == null || cellSize <= 0) return false;
        // A View that returns false for ACTION_DOWN never receives the ACTION_UP that follows it
        // (Android stops delivering that gesture to it entirely) — so ACTION_DOWN must be claimed.
        if (event.getAction() == MotionEvent.ACTION_DOWN) return true;
        if (event.getAction() != MotionEvent.ACTION_UP) return false;
        int col = (int) (event.getX() / cellSize), row = (int) (event.getY() / cellSize);
        if (col < 0 || col >= columns() || row < 0) return false;
        int page = firstPage + row * columns() + col;
        if (page >= firstPage && page <= lastPage) {
            performClick();
            listener.tapped(page);
        }
        return true;
    }

    @Override public boolean performClick() {super.performClick();return true;}

    /** Shared by the grid cells and the legend swatches, so both always draw the exact same pattern. */
    static void drawCell(Canvas canvas, RectF rect, int status, Paint fill, Paint stroke) {
        float radius = Math.max(1f, rect.width() * 0.12f);
        canvas.drawRoundRect(rect, radius, radius, stroke);
        if (status == ACQUIS) {
            canvas.drawRoundRect(rect, radius, radius, fill);
            return;
        }
        canvas.save();
        canvas.clipRect(rect);
        if (status == STABILISER) {
            float step = Math.max(2f, rect.width() * 0.22f);
            for (float x = rect.left - rect.height(); x < rect.right; x += step) {
                canvas.drawLine(x, rect.bottom, x + rect.height(), rect.top, fill);
            }
        } else if (status == APPRENTISSAGE) {
            float step = Math.max(3f, rect.width() * 0.34f);
            float dotRadius = Math.max(0.6f, rect.width() * 0.07f);
            for (float y = rect.top + step / 2; y < rect.bottom; y += step) {
                for (float x = rect.left + step / 2; x < rect.right; x += step) {
                    canvas.drawCircle(x, y, dotRadius, fill);
                }
            }
        }
        canvas.restore();
    }
}
