package com.quransafeguard.hifz.preview;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

final class Ui {
    private Ui() {}
    static final int INK = Color.rgb(18, 18, 17);
    static final int PAPER = Color.rgb(250, 248, 240);
    static final int MUTED = Color.rgb(82, 79, 73);

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

    /**
     * Android 14 and lower already keep a normal non-edge-to-edge Activity inside
     * system bars. Android 15+ enforces edge-to-edge for this targetSdk, so only
     * those devices need manual system-bar insets. This avoids double-padding older
     * phones/BOOX devices while protecting controls on API 35+.
     */
    static void respectSystemBars(Activity activity, View root, int left, int top, int right, int bottom) {
        Window window = activity.getWindow();
        window.setStatusBarColor(PAPER);
        window.setNavigationBarColor(PAPER);
        int flags = window.getDecorView().getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        if (Build.VERSION.SDK_INT >= 26) flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        window.getDecorView().setSystemUiVisibility(flags);

        if (Build.VERSION.SDK_INT < 35) {
            root.setPadding(left, top, right, bottom);
            return;
        }
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
            view.setPadding(
                left + bars.left,
                top + bars.top,
                right + bars.right,
                bottom + bars.bottom
            );
            return insets;
        });
        root.requestApplyInsets();
    }
}
