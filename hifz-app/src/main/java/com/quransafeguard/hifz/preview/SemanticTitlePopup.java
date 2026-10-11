package com.quransafeguard.hifz.preview;

import android.app.Activity;
import android.app.Dialog;
import android.os.Build;
import android.view.WindowInsets;
import android.view.View;
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
        return create(activity,title,previous,true);
    }

    /** Same bottom-anchored, non-dimming Al-Munir title sheet, English typography. */
    static Dialog showEnglish(Activity activity,String title,Dialog previous) {
        return create(activity,title,previous,false);
    }

    private static Dialog create(Activity activity,String title,Dialog previous,boolean arabic) {
        if (previous != null && previous.isShowing()) previous.dismiss();
        if (activity == null || activity.isFinishing() || title == null || title.trim().isEmpty()) return null;

        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout shell = Ui.column(activity);
        // Bare Al-Munir-style caption: no card, border, shadow or black button.
        // The English heading is deliberately less prominent than the Mushaf.
        shell.setPadding(Ui.dp(activity, 9), Ui.dp(activity, 5),
            Ui.dp(activity, 9), Ui.dp(activity, 5));
        TextView label = arabic
            ?Ui.bookText(activity, title.trim(), 14f, true)
            :Ui.text(activity, title.trim(), 11.5f, false);
        label.setIncludeFontPadding(false);
        label.setLineSpacing(0f, 1.02f);
        label.setGravity(Gravity.CENTER);
        label.setTextDirection(arabic ? android.view.View.TEXT_DIRECTION_RTL : android.view.View.TEXT_DIRECTION_LTR);
        label.setTextAlignment(android.view.View.TEXT_ALIGNMENT_CENTER);
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
            window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
            window.setBackgroundDrawableResource(android.R.color.transparent);
            int max = Math.min(activity.getResources().getDisplayMetrics().widthPixels
                - Ui.dp(activity, 28), Ui.dp(activity, 480));
            // Gravity.BOTTOM alone places the sheet behind navigation controls
            // on some BOOX/Android configurations. Lift it above the reader's
            // two compact control rows AND the actual system navigation inset.
            int bottomInset=0;
            View decor=activity.getWindow().getDecorView();
            WindowInsets insets=decor.getRootWindowInsets();
            if(insets!=null) {
                if(Build.VERSION.SDK_INT>=30)
                    bottomInset=insets.getInsets(WindowInsets.Type.navigationBars()).bottom;
                else bottomInset=insets.getStableInsetBottom();
            }
            WindowManager.LayoutParams attrs=window.getAttributes();
            attrs.y=bottomInset+Ui.dp(activity, 68);
            window.setAttributes(attrs);
            window.setLayout(Math.max(Ui.dp(activity, 160), max),
                ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        // A semantic title is a retrieval cue, not persistent commentary.
        shell.postDelayed(() -> {
            if (dialog.isShowing() && !activity.isFinishing()) dialog.dismiss();
        }, 3200L);
        return dialog;
    }
}
