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

/** Shared BOOX-first UI primitives: flat, high contrast, no animation. */
final class Ui {
    static final int INK = Color.rgb(18, 18, 17);
    static final int PAPER = Color.rgb(250, 248, 240);
    static final int MUTED = Color.rgb(82, 79, 73);
    static final int LINE = Color.rgb(196, 191, 180);

    private Ui() {}

    static Button button(Context context, String label, View.OnClickListener listener) {
        Button button = styled(new Button(context), context, 15f, dp(context, 18));
        button.setText(label);
        button.setOnClickListener(listener);
        button.setMinHeight(dp(context, 48));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(context, 4), 0, dp(context, 4));
        button.setLayoutParams(params);
        return button;
    }

    /** Text action: compact rounded capsule, used only when a word is clearer than a symbol. */
    static Button smallButton(Context context, String label, View.OnClickListener listener) {
        Button button = styled(new Button(context), context, 14f, dp(context, 16));
        button.setText(label);
        button.setOnClickListener(listener);
        button.setMinWidth(dp(context, 48));
        button.setMinHeight(dp(context, 44));
        return button;
    }

    /** Primary BOOX navigation/action control. Fixed circle; contentDescription carries the text label. */
    static Button roundButton(Context context, String symbol, String description, View.OnClickListener listener) {
        Button button = new Button(context);
        button.setAllCaps(false);
        button.setText(symbol);
        button.setTextSize(20f);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setGravity(Gravity.CENTER);
        button.setContentDescription(description);
        button.setOnClickListener(listener);
        button.setStateListAnimator(null);
        button.setElevation(0f);
        int size = dp(context, 52);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
        int gap = dp(context, 5);
        params.setMargins(gap, gap, gap, gap);
        button.setLayoutParams(params);
        button.setMinWidth(0);button.setMinHeight(0);
        button.setPadding(0,0,0,0);
        int stroke=Math.max(1,dp(context,1));
        StateListDrawable bg=new StateListDrawable();
        bg.addState(new int[]{-android.R.attr.state_enabled},shape(PAPER,LINE,size/2,stroke));
        bg.addState(new int[]{android.R.attr.state_pressed},shape(INK,INK,size/2,stroke));
        bg.addState(new int[]{android.R.attr.state_selected},shape(INK,INK,size/2,stroke));
        bg.addState(new int[]{},shape(PAPER,INK,size/2,stroke));
        button.setBackground(bg);
        button.setTextColor(new ColorStateList(
            new int[][]{{-android.R.attr.state_enabled},{android.R.attr.state_pressed},{android.R.attr.state_selected},{}},
            new int[]{LINE,PAPER,PAPER,INK}));
        return button;
    }

    static LinearLayout roundAction(Context context, String symbol, String label, View.OnClickListener listener) {
        LinearLayout box = column(context);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        box.setPadding(dp(context,4),dp(context,2),dp(context,4),dp(context,2));
        Button b=roundButton(context,symbol,label,listener);box.addView(b);
        TextView caption=text(context,label,11,false);caption.setGravity(Gravity.CENTER);box.addView(caption,
            new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        return box;
    }

    /** Visual selector: chosen control is shown in negative. */
    static void setChosen(Button button, boolean chosen) { button.setSelected(chosen); }

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
        view.setLineSpacing(0f, 1.15f);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    static void weight(View view, float weight) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, weight);
        if (view instanceof Button) {
            int gap = dp(view.getContext(), 3);
            params.setMargins(gap, gap, gap, gap);
        }
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
        button.setTextColor(new ColorStateList(
            new int[][]{{-android.R.attr.state_enabled},{android.R.attr.state_pressed},{android.R.attr.state_selected},{}},
            new int[]{LINE,PAPER,PAPER,INK}));
        int h = dp(context, 10);
        button.setPadding(h, 0, h, 0);
        return button;
    }

    private static GradientDrawable shape(int fill, int strokeColor, int radius, int strokeWidth) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(fill);
        shape.setCornerRadius(radius);
        shape.setStroke(strokeWidth, strokeColor);
        return shape;
    }
}
