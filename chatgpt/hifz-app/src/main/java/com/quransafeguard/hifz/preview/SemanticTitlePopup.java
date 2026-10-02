package com.quransafeguard.hifz.preview;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Compact, non-dimming semantic title panel; intentionally lighter than the Tafsir sheet. */
final class SemanticTitlePopup {
    private SemanticTitlePopup() {}

    static Dialog show(Activity activity, String title, Dialog previous) {
        if (previous != null && previous.isShowing()) previous.dismiss();
        if (activity == null || activity.isFinishing() || title == null || title.trim().isEmpty()) return null;

        Dialog dialog = new Dialog(activity);
        LinearLayout shell = Ui.column(activity);
        shell.setPadding(Ui.dp(activity, 14), Ui.dp(activity, 9), Ui.dp(activity, 14), Ui.dp(activity, 9));
        TextView label = Ui.bookText(activity, title.trim(), 14f, true);
        label.setGravity(Gravity.CENTER);
        label.setTextColor(Ui.INK);
        shell.addView(label, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        dialog.setContentView(shell);
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();

        Window window = dialog.getWindow();
        if (window != null) {
            window.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            window.setWindowAnimations(0);
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setBackgroundDrawableResource(android.R.color.transparent);
            int max = Math.min(activity.getResources().getDisplayMetrics().widthPixels - Ui.dp(activity, 24), Ui.dp(activity, 620));
            window.setLayout(Math.max(Ui.dp(activity, 220), max), ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        // A semantic title is a retrieval cue, not persistent commentary.
        shell.postDelayed(() -> {
            if (dialog.isShowing() && !activity.isFinishing()) dialog.dismiss();
        }, 3200L);
        return dialog;
    }
}
