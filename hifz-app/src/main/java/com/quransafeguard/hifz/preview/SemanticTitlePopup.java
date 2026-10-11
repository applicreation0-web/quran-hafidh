package com.quransafeguard.hifz.preview;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Point;
import android.view.View;
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

    /** Match Al-Munir: the ENTIRE heading sheet ends above the real
     * reader icon row, including on BOOX with non-standard system insets.
     * Measuring the toolbar's screen Y is safer than guessing its height. */
    static void positionAboveIcons(Activity activity, Dialog dialog, View iconRow) {
        if(activity==null||dialog==null||!dialog.isShowing()||iconRow==null)return;
        Window window=dialog.getWindow();
        if(window==null)return;
        if(!iconRow.isShown()||iconRow.getHeight()<=0) {
            iconRow.post(() -> positionAboveIcons(activity,dialog,iconRow));
            return;
        }
        int[] iconTop=new int[2];
        iconRow.getLocationOnScreen(iconTop);
        Point realScreen=new Point();
        activity.getWindowManager().getDefaultDisplay().getRealSize(realScreen);
        // The window bottom is never below the physical screen bottom; using
        // that upper bound cannot let the popup overlap the icon row.
        int gap=Ui.dp(activity, 10);
        WindowManager.LayoutParams attrs=window.getAttributes();
        attrs.y=Math.max(Ui.dp(activity, 8),realScreen.y-iconTop[1]+gap);
        window.setAttributes(attrs);
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
            // Safe initial position until the actual reader toolbar is measured.
            // The final placement is calculated from that toolbar, not a 68dp guess.
            WindowManager.LayoutParams attrs=window.getAttributes();
            attrs.y=Ui.dp(activity, 140);
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
