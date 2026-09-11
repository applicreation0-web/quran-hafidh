package com.quransafeguard.hifz.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

final class Ui {
    private Ui() { }

    // Reuse the established 0.10.x visual contract: warm cream + near-black only.
    static final int INK = Color.rgb(23, 23, 21);      // #171715
    static final int PAPER = Color.rgb(247, 242, 232); // #F7F2E8
    static final int SECONDARY = Color.rgb(85, 85, 80);

    static int dp(Context c, int value) { return Math.round(value * c.getResources().getDisplayMetrics().density); }

    static TextView text(Context c, String value, float sp, boolean bold) {
        TextView v = new TextView(c);
        v.setText(value);
        v.setTextSize(sp);
        v.setTextColor(INK);
        v.setLineSpacing(0f, 1.12f);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return v;
    }

    static Button button(Context c, String label, View.OnClickListener listener) {
        Button b = new Button(c);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(16f);
        b.setTextColor(INK);
        b.setOnClickListener(listener);
        b.setStateListAnimator(null);
        b.setElevation(0f);
        b.setMinHeight(dp(c, 50));
        b.setPadding(dp(c, 16), dp(c, 8), dp(c, 16), dp(c, 8));
        b.setBackground(buttonBackground(c, 18));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, dp(c, 5), 0, dp(c, 5));
        b.setLayoutParams(p);
        return b;
    }

    static Button smallButton(Context c, String label, View.OnClickListener listener) {
        Button b = new Button(c);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(14f);
        b.setTextColor(INK);
        b.setOnClickListener(listener);
        b.setStateListAnimator(null);
        b.setElevation(0f);
        b.setMinWidth(dp(c, 48));
        b.setMinHeight(dp(c, 44));
        b.setPadding(dp(c, 9), dp(c, 5), dp(c, 9), dp(c, 5));
        b.setBackground(buttonBackground(c, 14));
        return b;
    }

    private static GradientDrawable buttonBackground(Context c, int radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(PAPER);
        d.setStroke(Math.max(1, dp(c, 1)), SECONDARY);
        d.setCornerRadius(dp(c, radiusDp));
        return d;
    }

    static LinearLayout column(Context c) {
        LinearLayout root = new LinearLayout(c);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(PAPER);
        int horizontal = c.getResources().getConfiguration().smallestScreenWidthDp >= 600 ? 22 : 12;
        root.setPadding(dp(c, horizontal), dp(c, 10), dp(c, horizontal), dp(c, 10));
        return root;
    }

    static LinearLayout row(Context c) {
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        return row;
    }

    static void weight(View view, float weight) {
        view.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weight));
    }
}
