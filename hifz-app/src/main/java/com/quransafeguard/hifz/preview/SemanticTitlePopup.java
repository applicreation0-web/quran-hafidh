package com.quransafeguard.hifz.preview;

import android.app.Activity;
import android.app.Dialog;
import android.text.TextUtils;
import android.graphics.Point;
import android.view.View;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
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

    /**
     * The linear 114-surah map keeps its on-axis labels compact, but a tap must
     * never lose any words from the genuine Ayah Themes title. This separate,
     * scrollable caption has no card/border and does not change Lecture or Hifz.
     * Unlike short retrieval cues, it remains open until the reader dismisses it.
     */
    static Dialog showEnglishFull(Activity activity, String title, Dialog previous) {
        if (previous != null && previous.isShowing()) previous.dismiss();
        if (activity == null || activity.isFinishing() || title == null || title.trim().isEmpty())
            return null;
        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        ScrollView scroll = new ScrollView(activity);
        scroll.setFillViewport(false);
        scroll.setVerticalScrollBarEnabled(true);
        scroll.setPadding(Ui.dp(activity, 10), Ui.dp(activity, 8),
            Ui.dp(activity, 10), Ui.dp(activity, 8));
        scroll.setBackgroundColor(Ui.PAPER);
        TextView label = Ui.text(activity, title.trim(), 14f, false);
        label.setTextColor(Ui.INK);
        label.setTextDirection(View.TEXT_DIRECTION_LTR);
        label.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        label.setGravity(Gravity.CENTER);
        label.setIncludeFontPadding(false);
        label.setLineSpacing(0f, 1.08f);
        label.setContentDescription(title.trim());
        scroll.addView(label);
        dialog.setContentView(scroll);
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            window.setWindowAnimations(0);
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
            window.setBackgroundDrawableResource(android.R.color.transparent);
            int width = Math.min(activity.getResources().getDisplayMetrics().widthPixels
                - Ui.dp(activity, 28), Ui.dp(activity, 390));
            int height = Math.min(Ui.dp(activity, 210),
                activity.getResources().getDisplayMetrics().heightPixels / 3);
            WindowManager.LayoutParams attrs = window.getAttributes();
            attrs.alpha = 0f;
            attrs.y = 0;
            window.setAttributes(attrs);
            window.setLayout(Math.max(Ui.dp(activity, 160), width),
                Math.max(Ui.dp(activity, 72), height));
        }
        return dialog;
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
        // A title becomes visible ONLY when its final position is anchored
        // to the measured icon row; avoid a flash over BOOX toolbar controls.
        attrs.alpha=1f;
        window.setAttributes(attrs);
    }

    private static Dialog create(Activity activity,String title,Dialog previous,boolean arabic) {
        if (previous != null && previous.isShowing()) previous.dismiss();
        if (activity == null || activity.isFinishing() || title == null || title.trim().isEmpty()) return null;

        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout shell = Ui.column(activity);
        // Bare Al-Munir-style caption: no card, border, shadow or black button.
        // Readable E-Ink caption (14sp), still lighter than the Mushaf.
        shell.setPadding(Ui.dp(activity, 7), Ui.dp(activity, 3),
            Ui.dp(activity, 7), Ui.dp(activity, 3));
        TextView label = arabic
            ?Ui.bookText(activity, title.trim(), 14f, true)
            :Ui.text(activity, title.trim(), 14f, false);
        label.setIncludeFontPadding(false);
        label.setMaxLines(2);
        label.setEllipsize(TextUtils.TruncateAt.END);
        label.setContentDescription(title.trim());
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
                - Ui.dp(activity, 28), Ui.dp(activity, 390));
            // Initially invisible, NOT at a guessed position. The caller
            // must measure readerActions and invoke positionAboveIcons(...).
            WindowManager.LayoutParams attrs=window.getAttributes();
            attrs.alpha=0f;
            attrs.y=0;
            window.setAttributes(attrs);
            window.setLayout(Math.max(Ui.dp(activity, 160), max),
                ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        // Review sessions already call show(...) from their Mushaf cue handler.
        // Anchor there to their actual last control row without changing a
        // frozen Hifz engine, adding another button or guessing screen insets.
        // Lecture uses showEnglish(...) with its explicitly measured icon row.
        if (arabic) {
            View content = activity.findViewById(android.R.id.content);
            if (content instanceof ViewGroup) {
                ViewGroup holder = (ViewGroup) content;
                if (holder.getChildCount() > 0
                    && holder.getChildAt(holder.getChildCount()-1) instanceof ViewGroup) {
                    ViewGroup screen = (ViewGroup) holder.getChildAt(holder.getChildCount()-1);
                    if (screen.getChildCount() > 0) {
                        View toolbar = screen.getChildAt(screen.getChildCount()-1);
                        toolbar.post(() -> positionAboveIcons(activity, dialog, toolbar));
                    }
                }
            }
        }
        // A semantic title is a retrieval cue, not persistent commentary.
        shell.postDelayed(() -> {
            if (dialog.isShowing() && !activity.isFinishing()) dialog.dismiss();
        }, arabic ? 3200L : 8000L);
        return dialog;
    }
}
