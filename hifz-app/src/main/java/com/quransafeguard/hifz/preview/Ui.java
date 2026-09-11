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
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Petits constructeurs d'interface partagés.
 *
 * Reconstruit depuis l'APK 0.5-tablet-tafsir-test (commit ce7b1b67) — mêmes signatures publiques.
 * Corrections 2026-09-11 (audit Claude) :
 *  - boutons plats cernés d'encre, sans ombre ni gris Material : lisibles sur téléphone et sur e-ink ;
 *  - état appuyé en négatif (encre/papier), état désactivé clairement estompé ;
 *  - état « choisi » (setChosen) pour les sélecteurs : masque 0–100 %, édition de Tafsir ;
 *  - petits espacements entre boutons d'une même rangée (les bords ne se touchent plus).
 */
final class Ui {
    static final int INK = Color.rgb(18, 18, 17);
    static final int PAPER = Color.rgb(250, 248, 240);
    static final int MUTED = Color.rgb(82, 79, 73);
    static final int LINE = Color.rgb(196, 191, 180);

    private Ui() {}

    static Button button(Context context, String label, View.OnClickListener listener) {
        Button button = styled(new Button(context), context, 15f);
        button.setText(label);
        button.setOnClickListener(listener);
        button.setMinHeight(dp(context, 48));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(context, 4), 0, dp(context, 4));
        button.setLayoutParams(params);
        return button;
    }

    static Button smallButton(Context context, String label, View.OnClickListener listener) {
        Button button = styled(new Button(context), context, 14f);
        button.setText(label);
        button.setOnClickListener(listener);
        button.setMinWidth(dp(context, 48));
        button.setMinHeight(dp(context, 44));
        return button;
    }

    /** Sélecteur visuel : le bouton choisi est affiché en négatif. */
    static void setChosen(Button button, boolean chosen) {
        button.setSelected(chosen);
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
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
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
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, weight);
        if (view instanceof Button) {
            int gap = dp(view.getContext(), 3);
            params.setMargins(gap, gap, gap, gap);
        }
        view.setLayoutParams(params);
    }

    static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
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

    private static Button styled(Button button, Context context, float sizeSp) {
        button.setAllCaps(false);
        button.setTextSize(sizeSp);
        button.setStateListAnimator(null); // pas d'ombre : rendu net sur e-ink
        button.setElevation(0f);
        int radius = dp(context, 8);
        int stroke = Math.max(1, dp(context, 1));

        StateListDrawable background = new StateListDrawable();
        background.addState(new int[] {-android.R.attr.state_enabled}, shape(PAPER, LINE, radius, stroke));
        background.addState(new int[] {android.R.attr.state_pressed}, shape(INK, INK, radius, stroke));
        background.addState(new int[] {android.R.attr.state_selected}, shape(INK, INK, radius, stroke));
        background.addState(new int[] {}, shape(PAPER, INK, radius, stroke));
        button.setBackground(background);

        button.setTextColor(new ColorStateList(
            new int[][] {
                {-android.R.attr.state_enabled},
                {android.R.attr.state_pressed},
                {android.R.attr.state_selected},
                {}
            },
            new int[] {LINE, PAPER, PAPER, INK}
        ));
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
