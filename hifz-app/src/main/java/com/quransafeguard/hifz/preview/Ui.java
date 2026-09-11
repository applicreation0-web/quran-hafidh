package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

final class Ui {
    private Ui() {}
    static final int INK = Color.rgb(18, 18, 17);
    static final int PAPER = Color.WHITE;

    static int dp(Context c, int value) { return Math.round(value * c.getResources().getDisplayMetrics().density); }

    static TextView text(Context c, String value, float sp, boolean bold) {
        TextView v = new TextView(c);
        v.setText(value); v.setTextSize(sp); v.setTextColor(INK);
        v.setLineSpacing(0f, 1.15f);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return v;
    }

    static Button button(Context c, String label, View.OnClickListener listener) {
        Button b = new Button(c);
        b.setText(label); b.setAllCaps(false); b.setTextSize(15f); b.setTextColor(INK);
        b.setOnClickListener(listener);
        b.setMinHeight(dp(c, 48));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, dp(c, 4), 0, dp(c, 4)); b.setLayoutParams(p);
        return b;
    }

    static Button smallButton(Context c, String label, View.OnClickListener listener) {
        Button b = new Button(c); b.setText(label); b.setAllCaps(false); b.setTextSize(14f); b.setTextColor(INK); b.setOnClickListener(listener);
        b.setMinWidth(dp(c, 48)); b.setMinHeight(dp(c, 44));
        return b;
    }

    static LinearLayout column(Context c) {
        LinearLayout root = new LinearLayout(c); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(PAPER);
        root.setPadding(dp(c, 16), dp(c, 16), dp(c, 16), dp(c, 16));
        return root;
    }

    static LinearLayout row(Context c) {
        LinearLayout row = new LinearLayout(c); row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(Gravity.CENTER_VERTICAL); return row;
    }

    static void weight(View view, float weight) {
        view.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weight));
    }
}
