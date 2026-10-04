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
    static final int INK = 0xff121211;
    static final int PAPER = 0xfffaf8f0;
    static final int SURFACE = 0xfffdfbf6;
    static final int MUTED = 0xff524f49;
    static final int LINE = 0xffcec9be;

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

    /** Compact text action, reserved for tabs and explicit text choices. */
    static Button smallButton(Context context, String label, View.OnClickListener listener) {
        Button button = styled(new Button(context), context, 13.5f, dp(context, 10));
        String shown = label;
        int iconRes = iconFor(label, label);
        if (label != null && label.toLowerCase(Locale.ROOT).contains("audio")) shown = "Écouter";
        button.setText(shown);
        if (iconRes != 0) {
            button.setCompoundDrawablesWithIntrinsicBounds(iconRes, 0, 0, 0);
            button.setCompoundDrawableTintList(iconTint());
            button.setCompoundDrawablePadding(dp(context, 5));
        }
        button.setContentDescription(shown);
        button.setOnClickListener(listener);
        button.setMinWidth(dp(context, 48));
        button.setMinHeight(dp(context, 48));
        return button;
    }

    /** 48dp hit target with a quiet 24dp pictogram and no permanent visible circle. */
    static Button iconButton(Context context, String symbol, String description, View.OnClickListener listener) {
        Button button = new Button(context);
        button.setAllCaps(false);
        button.setGravity(Gravity.CENTER);
        setIconDescription(button, description);
        button.setOnClickListener(listener);
        button.setStateListAnimator(null);
        button.setElevation(0f);
        StateListDrawable pressBackground = new StateListDrawable();
        pressBackground.addState(new int[] {android.R.attr.state_pressed},
            shape(INK, INK, dp(context, 8), 0));
        pressBackground.addState(new int[] {android.R.attr.state_selected},
            shape(INK, INK, dp(context, 8), 0));
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
        int iconRes = iconFor(description, symbol);
        if (iconRes != 0) {
            button.setText("");
            setButtonIcon(button, iconRes);
        } else {
            button.setText(symbol);
            button.setTextSize(17f);
            button.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
            button.setTextColor(iconTintFlat());
        }
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
    static LinearLayout roundAction(Context context, String symbol, String label, View.OnClickListener listener) {
        LinearLayout box = column(context);
        box.setGravity(Gravity.CENTER);
        box.setPadding(0, 0, 0, 0);
        box.addView(iconButton(context, symbol, label, listener));
        return box;
    }


    /** Stabilisation's cue: the deep first pass works in weekly units; after the first An-Nās
     *  arrival every session is one hizb of maintenance. */
    static String stabilizationCue(boolean postNasMaintenance) {
        return postNasMaintenance
            ? "1 hizb · entretien"
            : PreviewConfig.STABILIZATION_WEEKLY_LINES + " lignes/semaine";
    }

    /** Liseuse-style setting: label on the left, current value/action on the right. */
    static LinearLayout settingRow(Context context, String label, String value, View.OnClickListener listener) {
        LinearLayout row = row(context);
        row.setPadding(dp(context, 2), dp(context, 5), dp(context, 2), dp(context, 5));
        row.setMinimumHeight(dp(context, 48));
        TextView name = text(context, label, 13f, false);
        Ui.weight(name, 1f);
        name.setPadding(dp(context, 4), 0, dp(context, 6), 0);
        row.addView(name);
        TextView current = text(context, value == null ? "" : value, 12f, false);
        current.setTextColor(MUTED);
        current.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        current.setMaxLines(2);
        row.addView(current, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        if (listener != null) {
            TextView chevron = text(context, "›", 20f, false);
            chevron.setTextColor(MUTED);
            chevron.setGravity(Gravity.CENTER);
            chevron.setPadding(dp(context, 7), 0, 0, 0);
            row.addView(chevron, new LinearLayout.LayoutParams(dp(context, 24), dp(context, 44)));
            row.setClickable(true);
            row.setFocusable(true);
            row.setOnClickListener(listener);
        }
        return row;
    }

    static TextView settingValue(LinearLayout row) {
        if (row == null || row.getChildCount() < 2 || !(row.getChildAt(1) instanceof TextView)) return null;
        return (TextView) row.getChildAt(1);
    }

    static View divider(Context context) {
        View line = new View(context);
        line.setBackgroundColor(LINE);
        line.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, dp(context, 1))));
        return line;
    }

    static void panel(View view) {
        view.setBackground(shape(SURFACE, LINE, dp(view.getContext(), 12), Math.max(1, dp(view.getContext(), 1))));
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
        return new ColorStateList(
            new int[][]{{-android.R.attr.state_enabled},{android.R.attr.state_pressed},{android.R.attr.state_selected},{}},
            new int[]{MUTED,PAPER,PAPER,INK});
    }

    static int iconFor(String semantic, String fallbackSymbol) {
        String s = semantic == null ? "" : semantic.toLowerCase(Locale.ROOT);
        if (s.contains("quiz")) return R.drawable.ic_ui_quiz;
        if (s.contains("annoter") || s.contains("crayon")) return R.drawable.ic_ui_edit;
        if (s.contains("annuler la note")) return R.drawable.ic_ui_undo;
        if (s.contains("effacer les notes")) return R.drawable.ic_ui_annotation_erase;
        if (s.contains("retour")) return R.drawable.ic_ui_back;
        if (s.contains("fermer") || s.contains("plus tard")) return R.drawable.ic_ui_close;
        if (s.contains("tafsir")) return R.drawable.ic_ui_tafsir;
        if (s.contains("passage précédent du corpus")) return R.drawable.ic_ui_jump_prev;
        if (s.contains("passage suivant du corpus")) return R.drawable.ic_ui_jump_next;
        if (s.contains("précédent") || s.contains("precedent")) return R.drawable.ic_ui_previous;
        if (s.contains("suivant")) return R.drawable.ic_ui_next;
        if (s.contains("lire") || s.contains("pause")) return R.drawable.ic_ui_play;
        if (s.contains("référence") || s.contains("diagnostic")) return R.drawable.ic_ui_info;
        if (s.equals("lecture")) return R.drawable.ic_ui_reading;
        if (s.contains("mémor")) return R.drawable.ic_ui_memorize;
        if (s.contains("progression")) return R.drawable.ic_ui_progress_map;
        if (s.contains("param")) return R.drawable.ic_ui_settings;
        if (s.equals("séance")) return R.drawable.ic_ui_session;
        if (s.contains("audio") || s.contains("écouter")) return R.drawable.ic_ui_audio;
        if (s.contains("sourate")) return R.drawable.ic_ui_surah_list;
        if (s.contains("hizb")) return R.drawable.ic_ui_hizb;
        if (s.contains("réinitial") || s.contains("remettre à zéro")) return R.drawable.ic_ui_reset;
        if (s.contains("rotation")) return R.drawable.ic_ui_rotation;
        if (s.startsWith("retirer")) return R.drawable.ic_ui_delete;
        if (s.startsWith("ajouter")) return R.drawable.ic_ui_add;
        if (s.equals("commencer")) return R.drawable.ic_ui_play;
        if (s.equals("correct")) return R.drawable.ic_ui_validate;
        if (s.contains("hésitation") || s.contains("hesitation")) return R.drawable.ic_ui_info;
        if (s.contains("à revoir") || s.contains("a revoir")) return R.drawable.ic_ui_repeat;
        if (s.contains("répétition") || s.contains("répéter") || s.contains("réessayer")) return R.drawable.ic_ui_repeat;
        if (s.contains("enregistrer")) return R.drawable.ic_ui_record;
        if (s.contains("arrêter") || s.contains("arreter")) return R.drawable.ic_ui_stop;
        if (s.contains("vérifier") || s.contains("verifier")) return R.drawable.ic_ui_validate;
        if (s.contains("révéler")) return R.drawable.ic_ui_reveal;
        if (s.contains("amorce")) return R.drawable.ic_ui_semantic_anchor;
        if (s.contains("à renforcer") || s.contains("a renforcer")) return R.drawable.ic_hifz_strengthen;
        if (s.contains("en attente")) return R.drawable.ic_hifz_waiting;
        if (s.equals("acquis") || s.contains("page acquise")) return R.drawable.ic_hifz_acquired;
        if (s.contains("apprentissage") || s.contains("leçon neuve") || s.contains("lecon neuve")) return R.drawable.ic_hifz_new_lesson;
        if (s.contains("reprise")) return R.drawable.ic_hifz_reprise;
        if (s.contains("finale")) return R.drawable.ic_hifz_final_review;
        if (s.contains("renforcement") || s.contains("consolidation")) return R.drawable.ic_hifz_consolidation;
        if (s.contains("stabilisation") || s.contains("ancrage")) return R.drawable.ic_hifz_anchor;
        if (s.contains("révision") || s.contains("revision") || s.contains("entretien")) return R.drawable.ic_hifz_maintenance;
        if (s.contains("valider") || s.equals("revu") || s.contains("termin")) return R.drawable.ic_ui_validate;
        if (s.startsWith("début")) return R.drawable.ic_ui_start;
        if (s.startsWith("fin")) return R.drawable.ic_ui_end;
        if (s.contains("ajouter")) return R.drawable.ic_ui_add;
        if (s.contains("modifier")) return R.drawable.ic_ui_edit;
        if (s.contains("supprimer")) return R.drawable.ic_ui_delete;
        if (s.contains("choisir le pack") || s.contains("import")) return R.drawable.ic_ui_import;
        if (s.contains("sabqi")) return R.drawable.ic_hifz_new_lesson;
        if (s.contains("itq")) return R.drawable.ic_hifz_anchor;
        if (s.contains("murāja") || s.contains("muraja")) return R.drawable.ic_hifz_maintenance;
        if (s.contains("annuler")) return R.drawable.ic_ui_reset;
        if (s.contains("effacer")) return R.drawable.ic_ui_delete;
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
