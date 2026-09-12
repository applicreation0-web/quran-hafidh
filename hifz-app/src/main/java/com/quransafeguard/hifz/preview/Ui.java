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

import java.util.Locale;

/** BOOX-first UI: quiet, flat, high-contrast, animation-free and semantically uniform. */
final class Ui {
    static final int INK = Color.rgb(18, 18, 17);
    static final int PAPER = Color.rgb(250, 248, 240);
    static final int SURFACE = Color.rgb(253, 251, 246);
    static final int MUTED = Color.rgb(82, 79, 73);
    static final int LINE = Color.rgb(206, 201, 190);

    private Ui() {}

    static Button button(Context context, String label, View.OnClickListener listener) {
        Button button = styled(new Button(context), context, 14.5f, dp(context, 14));
        button.setText(label);
        button.setOnClickListener(listener);
        button.setMinHeight(dp(context, 46));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(context, 3), 0, dp(context, 3));
        button.setLayoutParams(params);
        return button;
    }

    /** Compact text action. */
    static Button smallButton(Context context, String label, View.OnClickListener listener) {
        Button button = styled(new Button(context), context, 13.5f, dp(context, 13));
        button.setText(label);
        button.setOnClickListener(listener);
        button.setMinWidth(dp(context, 44));
        button.setMinHeight(dp(context, 40));
        return button;
    }

    /** Small utility control. Pictograms are selected by semantic label, never by arbitrary glyph style. */
    static Button roundButton(Context context, String symbol, String description, View.OnClickListener listener) {
        Button button = new Button(context);
        button.setAllCaps(false);
        int iconRes = iconFor(description, symbol);
        if (iconRes != 0) {
            button.setText("");
            button.setCompoundDrawablesWithIntrinsicBounds(iconRes, 0, 0, 0);
            button.setCompoundDrawableTintList(iconTint());
        } else {
            button.setText(symbol);
            button.setTextSize(17f);
            button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }
        button.setGravity(Gravity.CENTER);
        button.setContentDescription(description);
        button.setOnClickListener(listener);
        button.setStateListAnimator(null);
        button.setElevation(0f);
        int size = dp(context, 44);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
        int gap = dp(context, 4);
        params.setMargins(gap, gap, gap, gap);
        button.setLayoutParams(params);
        button.setMinWidth(0); button.setMinHeight(0);
        button.setPadding(dp(context, 10), dp(context, 10), dp(context, 10), dp(context, 10));
        int stroke = Math.max(1, dp(context, 1));
        StateListDrawable bg = new StateListDrawable();
        bg.addState(new int[]{-android.R.attr.state_enabled}, shape(PAPER, LINE, size / 2, stroke));
        bg.addState(new int[]{android.R.attr.state_pressed}, shape(INK, INK, size / 2, stroke));
        bg.addState(new int[]{android.R.attr.state_selected}, shape(INK, INK, size / 2, stroke));
        bg.addState(new int[]{}, shape(PAPER, INK, size / 2, stroke));
        button.setBackground(bg);
        button.setTextColor(iconTint());
        return button;
    }

    /** Symbol + caption action with one visual grammar across all screens. */
    static LinearLayout roundAction(Context context, String symbol, String label, View.OnClickListener listener) {
        LinearLayout box = column(context);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        box.setPadding(dp(context,3),dp(context,1),dp(context,3),dp(context,1));
        Button b = roundButton(context, symbol, label, listener);
        box.addView(b);
        TextView caption = text(context, label, 10.5f, false);
        caption.setGravity(Gravity.CENTER);
        caption.setSingleLine(true);
        box.addView(caption, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return box;
    }

    /** Ebook-style home action. */
    static LinearLayout cardAction(Context context, String symbol, String label, View.OnClickListener listener) {
        LinearLayout card = column(context);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(context,8),dp(context,9),dp(context,8),dp(context,8));
        card.setBackground(shape(SURFACE, LINE, dp(context,14), Math.max(1, dp(context,1))));
        card.setClickable(true);
        card.setFocusable(true);
        card.setContentDescription(label);
        card.setOnClickListener(listener);

        TextView icon = text(context, "", 19f, true);
        int iconRes = iconFor(label, symbol);
        if (iconRes != 0) {
            icon.setCompoundDrawablesWithIntrinsicBounds(iconRes, 0, 0, 0);
            icon.setCompoundDrawableTintList(ColorStateList.valueOf(INK));
            icon.setMinHeight(dp(context, 26));
        } else {
            icon.setText(symbol);
        }
        icon.setGravity(Gravity.CENTER);
        card.addView(icon, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView caption = text(context, label, 10.5f, false);
        caption.setGravity(Gravity.CENTER);
        caption.setSingleLine(true);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cp.topMargin = dp(context,3);
        card.addView(caption, cp);
        return card;
    }

    /** Hifz mode card: stable pictogram + name + one short protocol cue. */
    static LinearLayout modeCard(Context context, String symbol, String label, View.OnClickListener listener) {
        LinearLayout card = cardAction(context, symbol, label, listener);
        if (card.getChildCount() > 1 && card.getChildAt(1) instanceof TextView) {
            TextView title = (TextView) card.getChildAt(1);
            title.setTextSize(13.5f);
            title.setTypeface(Typeface.SERIF, Typeface.BOLD);
        }
        String lower = label.toLowerCase(Locale.ROOT);
        String cue = lower.contains("sabqi") ? "5 lignes" : lower.contains("itq") ? "×40" : lower.contains("mur") ? "Révision" : "";
        if (!cue.isEmpty()) {
            TextView subtitle = text(context, cue, 9.5f, false);
            subtitle.setTextColor(MUTED);
            subtitle.setGravity(Gravity.CENTER);
            subtitle.setSingleLine(true);
            card.addView(subtitle, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        card.setPadding(dp(context,8),dp(context,9),dp(context,8),dp(context,8));
        return card;
    }

    static void setChosen(Button button, boolean chosen) { button.setSelected(chosen); }

    static void panel(View view) {
        view.setBackground(shape(SURFACE, LINE, dp(view.getContext(), 14), Math.max(1, dp(view.getContext(), 1))));
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
        view.setLineSpacing(0f, 1.10f);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    static TextView bookText(Context context, String value, float sizeSp, boolean bold) {
        TextView view = text(context, value, sizeSp, false);
        view.setTypeface(Typeface.SERIF, bold ? Typeface.BOLD : Typeface.NORMAL);
        return view;
    }

    static void weight(View view, float weight) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, weight);
        int gap = dp(view.getContext(), 3);
        params.setMargins(gap, gap, gap, gap);
        view.setLayoutParams(params);
    }

    static int dp(Context context, int value) { return Math.round(value * context.getResources().getDisplayMetrics().density); }

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
            new int[]{LINE,PAPER,PAPER,INK});
    }

    private static int iconFor(String semantic, String fallbackSymbol) {
        String s = semantic == null ? "" : semantic.toLowerCase(Locale.ROOT);
        if (s.contains("retour")) return R.drawable.ic_ui_back;
        if (s.contains("fermer")) return R.drawable.ic_ui_close;
        if (s.contains("référence") || s.contains("diagnostic")) return R.drawable.ic_ui_info;
        if (s.equals("lecture")) return R.drawable.ic_ui_reading;
        if (s.contains("mémor")) return R.drawable.ic_ui_memorize;
        if (s.contains("param")) return R.drawable.ic_ui_settings;
        if (s.equals("séance")) return R.drawable.ic_ui_session;
        if (s.contains("audio") || s.contains("écouter")) return R.drawable.ic_ui_audio;
        if (s.contains("répétition") || s.contains("rotation") || s.contains("réinitial")) return R.drawable.ic_ui_repeat;
        if (s.contains("révéler")) return R.drawable.ic_ui_reveal;
        if (s.contains("valider") || s.equals("revu") || s.contains("termin")) return R.drawable.ic_ui_validate;
        if (s.contains("ajouter")) return R.drawable.ic_ui_add;
        if (s.contains("modifier")) return R.drawable.ic_ui_edit;
        if (s.contains("supprimer")) return R.drawable.ic_ui_delete;
        if (s.contains("choisir le pack") || s.contains("import")) return R.drawable.ic_ui_import;
        if (s.contains("sabqi")) return R.drawable.ic_ui_sabqi;
        if (s.contains("itq")) return R.drawable.ic_ui_itqan;
        if (s.contains("murāja") || s.contains("muraja")) return R.drawable.ic_ui_murajaah;
        return 0;
    }

    private static GradientDrawable shape(int fill, int strokeColor, int radius, int strokeWidth) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(fill);
        shape.setCornerRadius(radius);
        shape.setStroke(strokeWidth, strokeColor);
        return shape;
    }
}
