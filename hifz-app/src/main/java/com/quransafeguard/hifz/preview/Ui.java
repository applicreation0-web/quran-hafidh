package com.quransafeguard.hifz.preview;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;


/** BOOX-first UI: quiet, flat, high-contrast, animation-free and semantically uniform. */
final class Ui {
    static final int INK = 0xff121211;
    static final int PAPER = 0xfffaf8f0;
    static final int SURFACE = 0xfffdfbf6;
    static final int MUTED = 0xff524f49;
    static final int LINE = 0xff88847d; // visible on greyscale E-Ink, no subtle web hairline

    /** Patrick Hand (SIL OFL 1.1), bundled offline; QCF Mushaf glyphs are untouched.
     * Context-lazy initialization avoids JVM source-contract regressions. */
    private static volatile Typeface interfaceFont;
    static Typeface typeface(Context context, int style) {
        Typeface base = interfaceFont;
        if (base == null) {
            synchronized (Ui.class) {
                base = interfaceFont;
                if (base == null) {
                    base = context.getResources().getFont(R.font.patrick_hand);
                    interfaceFont = base;
                }
            }
        }
        return style == Typeface.NORMAL ? base : Typeface.create(base, style);
    }

    private Ui() {}

    static Button button(Context context, String label, View.OnClickListener listener) {
        Button button = styled(new Button(context), context, 17f, dp(context, 14));
        button.setText(label);
        button.setOnClickListener(listener);
        button.setMinHeight(dp(context, 46));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(context, 3), 0, dp(context, 3));
        button.setLayoutParams(params);
        return button;
    }

    /** Compact text action, reserved for tabs and explicit text choices. */
    static Button smallButton(Context context, int iconRes, String label, View.OnClickListener listener) {
        Button button = styled(new Button(context), context, 16f, dp(context, 10));
        button.setText(label);
        if (iconRes != 0) {
            button.setCompoundDrawablesWithIntrinsicBounds(iconRes, 0, 0, 0);
            button.setCompoundDrawableTintList(iconTint());
            button.setCompoundDrawablePadding(dp(context, 5));
        }
        button.setContentDescription(label);
        button.setOnClickListener(listener);
        button.setMinWidth(dp(context, 48));
        button.setMinHeight(dp(context, 48));
        return button;
    }

    /** 48dp hit target with a quiet 24dp pictogram and no permanent visible circle. */
    static Button iconButton(Context context, int iconRes, String description, View.OnClickListener listener) {
        if (iconRes == 0) {
            throw new IllegalArgumentException("Unmapped compact icon role: " + description);
        }
        Button button = new Button(context);
        button.setAllCaps(false);
        button.setGravity(Gravity.CENTER);
        setIconDescription(button, description);
        button.setOnClickListener(listener);
        button.setStateListAnimator(null);
        button.setElevation(0f);
        StateListDrawable pressBackground = new StateListDrawable();
        GradientDrawable unavailable = shape(Color.TRANSPARENT, LINE, dp(context, 6), 0);
        unavailable.setStroke(dp(context, 1), LINE, dp(context, 2), dp(context, 2));
        pressBackground.addState(new int[] {-android.R.attr.state_enabled}, unavailable);
        pressBackground.addState(new int[] {android.R.attr.state_pressed},
            shape(INK, INK, dp(context, 8), 0));
        // An active icon stays quiet on E-Ink: no large solid black tile.
        pressBackground.addState(new int[] {android.R.attr.state_selected},
            shape(SURFACE, LINE, dp(context, 6), Math.max(1, dp(context, 1))));
        pressBackground.addState(new int[] {},
            shape(Color.TRANSPARENT, Color.TRANSPARENT, dp(context, 8), 0));
        button.setBackground(pressBackground);
        button.setMinWidth(0);
        button.setMinHeight(0);
        button.setPadding(dp(context, 12), dp(context, 12), dp(context, 12), dp(context, 12));
        int size = dp(context, 48);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
        params.setMargins(dp(context, 1), 0, dp(context, 1), 0);
        button.setLayoutParams(params);
        setButtonIcon(button, iconRes);
        return button;
    }

    /** Icon-only buttons: spoken label plus the long-press tooltip where the platform has one. */
    static void setIconDescription(Button button, String description) {
        if (button == null) return;
        String value = description == null ? "" : description;
        button.setContentDescription(value);
        if (Build.VERSION.SDK_INT >= 26) button.setTooltipText(value);
    }

    static void setButtonIcon(Button button, int iconRes) {
        if (button == null) return;
        button.setText("");
        button.setCompoundDrawablesWithIntrinsicBounds(iconRes, 0, 0, 0);
        button.setCompoundDrawableTintList(iconTintFlat());
    }

    /**
     * Action of the session/reader bars: the icon alone, exactly like the other icon buttons. The
     * label is never printed under it (it crowded the bar on device); it stays available as the
     * spoken description and the long-press tooltip.
     */
    static LinearLayout roundAction(Context context, int iconRes, String label, View.OnClickListener listener) {
        LinearLayout box = column(context);
        box.setGravity(Gravity.CENTER);
        box.setPadding(0, 0, 0, 0);
        box.addView(iconButton(context, iconRes, label, listener));
        return box;
    }


    /** Stabilisation's cue: the deep first pass works in weekly units; after the first An-Nās
     *  arrival every session is one hizb of maintenance. */
    static String stabilizationCue(boolean postNasMaintenance) {
        return postNasMaintenance
            ? "Stabilisation · 1 hizb"
            : PreviewConfig.STABILIZATION_WEEKLY_LINES + " lignes/semaine";
    }

    /** Shared BOOX menu hierarchy: one family for headings, values and actions.
     * Values remain live TextViews (settings and progress summaries still update in-place). */
    static TextView menuSubline(Context context, String value) {
        TextView secondary = text(context, value == null ? "" : value, 15f, false);
        // 10-inch BOOX typography must remain readable at arm's length.
        if (context.getResources().getConfiguration().smallestScreenWidthDp >= 600)
            secondary.setTextSize(18f);
        secondary.setTypeface(typeface(context, Typeface.NORMAL));
        secondary.setTextColor(MUTED);
        secondary.setLineSpacing(0f, 1.0f);
        secondary.setMaxLines(2);
        secondary.setEllipsize(android.text.TextUtils.TruncateAt.END);
        return secondary;
    }

    /** Two-level menu row. A genuine second line replaces the squeezed right column.
     * Keep the clickable row at least 56dp and its arrow separate from text. */
    static LinearLayout settingRow(Context context, String label, String value, View.OnClickListener listener) {
        LinearLayout row = row(context);
        row.setPadding(dp(context, 2), dp(context, 6), dp(context, 2), dp(context, 6));
        row.setMinimumHeight(dp(context, 56));
        LinearLayout copy = column(context);
        copy.setPadding(dp(context, 4), dp(context, 2), dp(context, 4), dp(context, 2));
        TextView name = bookText(context, label, 17f, true);
        if (context.getResources().getConfiguration().smallestScreenWidthDp >= 600)
            name.setTextSize(21f);
        name.setMaxLines(2);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        copy.addView(name);
        TextView current = menuSubline(context, value);
        // Read-only values have no destination: never hide their full programme/speed.
        if (listener == null) {
            current.setMaxLines(Integer.MAX_VALUE);
            current.setEllipsize(null);
        }
        current.setPadding(0, dp(context, 2), 0, 0);
        copy.addView(current);
        row.addView(copy, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        if (listener != null) {
            android.widget.ImageView chevron = new android.widget.ImageView(context);
            chevron.setImageResource(R.drawable.ic_ui_next);
            chevron.setColorFilter(INK);
            chevron.setPadding(dp(context, 3), dp(context, 12), dp(context, 3), dp(context, 12));
            chevron.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            row.addView(chevron, new LinearLayout.LayoutParams(dp(context, 24), dp(context, 44)));
            row.setClickable(true);
            row.setFocusable(true);
            row.setOnClickListener(listener);
        }
        return row;
    }

    /** Supports compact Hifz legacy rows as well as two-level Settings menu rows. */
    static TextView settingValue(LinearLayout row) {
        if (row == null) return null;
        if (row.getChildCount() > 1 && row.getChildAt(1) instanceof TextView)
            return (TextView) row.getChildAt(1);
        if (row.getChildCount() > 0 && row.getChildAt(0) instanceof LinearLayout) {
            LinearLayout copy = (LinearLayout) row.getChildAt(0);
            if (copy.getChildCount() > 1 && copy.getChildAt(1) instanceof TextView)
                return (TextView) copy.getChildAt(1);
        }
        return null;
    }

    static View divider(Context context) {
        View line = new View(context);
        line.setBackgroundColor(LINE);
        line.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, dp(context, 1))));
        return line;
    }

    static void panel(View view) {
        view.setBackground(shape(SURFACE, LINE, dp(view.getContext(), 16), Math.max(1, dp(view.getContext(), 1))));
    }

    static LinearLayout column(Context context) {
        LinearLayout column = new LinearLayout(context);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setBackgroundColor(PAPER);
        int pad = dp(context, 16);
        column.setPadding(pad, pad, pad, pad);
        return column;
    }

    static LinearLayout row(Context context) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        return row;
    }

    static TextView text(Context context, String value, float sizeSp, boolean bold) {
        TextView view = new TextView(context);
        view.setText(value);
        view.setTextSize(sizeSp);
        view.setTextColor(INK);
        view.setLineSpacing(0f, 1.08f);
        view.setTypeface(typeface(context, bold ? Typeface.BOLD : Typeface.NORMAL));
        return view;
    }

    static TextView bookText(Context context, String value, float sizeSp, boolean bold) {
        return text(context, value, sizeSp, bold);
    }

    /** Headings share the exact interface family; hierarchy comes from weight only. */
    static TextView notebookTitle(Context context, String value, float sizeSp) {
        TextView view = text(context, value, sizeSp, true);
        view.setLetterSpacing(0.01f);
        return view;
    }

    static void weight(View view, float weight) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, weight);
        int gap = dp(view.getContext(), 3);
        params.setMargins(gap, gap, gap, gap);
        view.setLayoutParams(params);
    }

    static int dp(Context context, int value) { return Math.round(value * context.getResources().getDisplayMetrics().density); }

    static void showFatal(Activity activity, String message) {
        LinearLayout root = column(activity);
        int pad = dp(activity, 24);
        root.setGravity(Gravity.CENTER);
        root.setPadding(pad, pad, pad, pad);
        TextView title = bookText(activity, "Parcours indisponible", 18f, true);
        title.setGravity(Gravity.CENTER);
        root.addView(title);
        TextView body = text(activity, message == null ? "La géométrie du Mushaf ne peut pas être chargée." : message, 13f, false);
        body.setTextColor(MUTED);
        body.setGravity(Gravity.CENTER);
        body.setPadding(0, dp(activity, 10), 0, dp(activity, 12));
        root.addView(body);
        root.addView(button(activity, "Retour", v -> activity.finish()));
        activity.setContentView(root);
        respectSystemBars(activity, root, pad, pad, pad, pad);
    }

    static void respectSystemBars(Activity activity, View root, int left, int top, int right, int bottom) {
        Window window = activity.getWindow();
        window.setStatusBarColor(PAPER);
        window.setNavigationBarColor(PAPER);
        window.getDecorView().setBackgroundColor(PAPER);
        int flags = window.getDecorView().getSystemUiVisibility()
            | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        window.getDecorView().setSystemUiVisibility(flags);
        if (Build.VERSION.SDK_INT < 35) {
            root.setPadding(left, top, right, bottom);
            return;
        }
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
            view.setPadding(bars.left + left, bars.top + top, bars.right + right, bars.bottom + bottom);
            return insets;
        });
        root.requestApplyInsets();
    }

    private static Button styled(Button button, Context context, float sizeSp, int radius) {
        button.setAllCaps(false);
        button.setTextSize(sizeSp);
        button.setTypeface(typeface(context, Typeface.NORMAL));
        button.setStateListAnimator(null);
        button.setElevation(0f);
        int stroke = Math.max(1, dp(context, 1));
        StateListDrawable background = new StateListDrawable();
        background.addState(new int[] {-android.R.attr.state_enabled}, shape(PAPER, LINE, radius, stroke));
        background.addState(new int[] {android.R.attr.state_pressed}, shape(INK, INK, radius, stroke));
        background.addState(new int[] {android.R.attr.state_selected}, shape(INK, INK, radius, stroke));
        background.addState(new int[] {}, shape(PAPER, INK, radius, stroke));
        button.setBackground(background);
        button.setTextColor(iconTint());
        int h = dp(context, 10);
        button.setPadding(h, 0, h, 0);
        return button;
    }

    private static ColorStateList iconTint() {
        return new ColorStateList(
            new int[][]{{-android.R.attr.state_enabled},{android.R.attr.state_pressed},{android.R.attr.state_selected},{}},
            new int[]{MUTED,PAPER,PAPER,INK});
    }

    private static ColorStateList iconTintFlat() {
        // Selected glyph sits on a light E-Ink outline, NOT on solid black.
        // PAPER-on-PAPER made the Amorces key invisible in the user's screenshot.
        return new ColorStateList(
            new int[][]{{-android.R.attr.state_enabled},{android.R.attr.state_pressed},{android.R.attr.state_selected},{}},
            new int[]{MUTED,PAPER,INK,INK});
    }

    private static GradientDrawable shape(int fill, int strokeColor, int radius, int strokeWidth) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(fill);
        shape.setCornerRadius(radius);
        shape.setStroke(strokeWidth, strokeColor);
        return shape;
    }
}
