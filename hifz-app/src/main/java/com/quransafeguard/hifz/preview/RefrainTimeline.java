package com.quransafeguard.hifz.preview;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/** Compact, swipeable refrain rail. Quran/Atlas anchors and Tafsir sources are unchanged. */
final class RefrainTimeline {
    private RefrainTimeline() {}
    interface OnSelect { void selected(String verse); }

    static final class State {
        int selected = -1;
        // Retain old saved keys for upgrade and restoration compatibility.
        boolean open;
        final Set<String> stations = new HashSet<>(), nodes = new HashSet<>();
        State(Bundle saved) {
            if (saved == null) return;
            selected = saved.getInt("friseSelected", -1);
            open = saved.getBoolean("friseOpen", false);
            ArrayList<String> s = saved.getStringArrayList("friseStations");
            ArrayList<String> n = saved.getStringArrayList("friseNodes");
            if (s != null) stations.addAll(s);
            if (n != null) nodes.addAll(n);
        }
        void save(Bundle out) {
            out.putInt("friseSelected", selected);
            out.putBoolean("friseOpen", open);
            out.putStringArrayList("friseStations", new ArrayList<>(stations));
            out.putStringArrayList("friseNodes", new ArrayList<>(nodes));
        }
    }

    private static String cue(RefrainAtlasThemes atlas,String id,String[] node){
        if(atlas==null)return node[5];
        try{return atlas.keyword(id,node);}catch(RuntimeException invalid){return node[5];}
    }
    private static String context(RefrainAtlasThemes atlas,String id,String[] node,boolean after){
        if(atlas==null)return node[after?11:8];
        try{return after?atlas.after(id,node):atlas.before(id,node);}
        catch(RuntimeException invalid){return node[after?11:8];}
    }

    private static GradientDrawable tile(Activity a, boolean active) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(active ? Ui.INK : Ui.SURFACE);
        bg.setCornerRadius(Ui.dp(a, 6));
        bg.setStroke(Math.max(1, Ui.dp(a, 1)), Ui.INK);
        return bg;
    }

    private static void reading(Activity a, String ref, String page) {
        a.startActivity(new Intent(a, StudyReaderActivity.class)
            .putExtra(StudyReaderActivity.EXTRA_JUMP_PAGE, Integer.parseInt(page))
            .putExtra(StudyReaderActivity.EXTRA_JUMP_VERSE, ref));
    }

    /** Six visible points for Ar-Rahman, four for Al-Qamar; swipe for the other occurrences. */
    static void append(Activity a, LinearLayout host, String id, String initialRef, State state, OnSelect onSelect) {
        ArrayList<String[]> nodes = new ArrayList<>();
        for (String[] n : RefrainTimelineData.NODES)
            if (id.equals(n[0])) nodes.add(n);
        if (nodes.isEmpty()) return;
        final RefrainAtlasThemes atlas=RefrainAtlasThemes.load(a);

        final int visible = Math.min(6, nodes.size());
        int screenDp = a.getResources().getConfiguration().screenWidthDp;
        final int slotDp = Math.max(46, (screenDp - 108) / visible);
        final int slotPx = Ui.dp(a, slotDp + 2);
        final int[] current = {state.selected};
        if (current[0] < 0 || current[0] >= nodes.size()) {
            current[0] = 0;
            for (int i = 0; i < nodes.size(); i++)
                if (initialRef.equals(nodes.get(i)[5])) {current[0] = i; break;}
        }
        state.selected = current[0];

        LinearLayout panel = Ui.column(a);
        panel.setPadding(Ui.dp(a, 4), Ui.dp(a, 2), Ui.dp(a, 4), Ui.dp(a, 4));
        LinearLayout line = Ui.row(a);
        line.setGravity(Gravity.CENTER_VERTICAL);
        HorizontalScrollView scroll = new HorizontalScrollView(a);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.setFillViewport(false);
        LinearLayout chips = Ui.row(a);
        ArrayList<TextView> cards = new ArrayList<>();
        TextView detail = Ui.text(a, "", 14, true);
        detail.setTextColor(Ui.INK);
        detail.setGravity(Gravity.CENTER);
        detail.setMinHeight(Ui.dp(a, 36));
        detail.setContentDescription("Lire le refrain dans le Mushaf");
        detail.setOnClickListener(v -> {
            String[] n = nodes.get(current[0]);
            reading(a, n[5], n[6]);
        });

        Runnable[] redraw = new Runnable[1];
        redraw[0] = () -> {
            state.selected = current[0];
            String[] selected = nodes.get(current[0]);
            detail.setText("Before " + context(atlas,id,selected,false) +
                "  ·  " + selected[5] + "  ·  After " + context(atlas,id,selected,true));
            String original=atlas==null?"Atlas source unavailable":atlas.passageTitle(selected[5]);
            detail.setContentDescription("Lire " + selected[5] + " dans le Mushaf. Atlas: " +
                original + ". Before " + context(atlas,id,selected,false) +
                ". After " + context(atlas,id,selected,true));
            for (int i = 0; i < cards.size(); i++) {
                TextView card = cards.get(i);
                boolean active = i == current[0];
                card.setBackground(tile(a, active));
                card.setTextColor(active ? Ui.PAPER : Ui.INK);
                card.setSelected(active);
            }
            // Keep the selected point within the six visible positions, with no E-Ink animation.
            int left = Math.max(0, (current[0] - visible + 1) * slotPx);
            int right = (current[0] + 1) * slotPx;
            int x = scroll.getScrollX();
            int viewport = scroll.getWidth() > 0 ? scroll.getWidth() : visible * slotPx;
            if (x > left) scroll.scrollTo(left, 0);
            else if (x + viewport < right) scroll.scrollTo(Math.max(0, right - viewport), 0);
        };

        Runnable choose = () -> {
            redraw[0].run();
            if (onSelect != null) onSelect.selected(nodes.get(current[0])[5]);
        };
        for (int i = 0; i < nodes.size(); i++) {
            String[] n = nodes.get(i);
            TextView card = Ui.text(a, n[3] + "\n" + cue(atlas,id,n),
                screenDp < 540 ? 11 : 13, false);
            card.setGravity(Gravity.CENTER);
            card.setMaxLines(3);
            card.setMinHeight(Ui.dp(a, 58));
            card.setPadding(Ui.dp(a, 1), 0, Ui.dp(a, 1), 0);
            card.setContentDescription("Refrain " + n[3] + " sur " + n[4] +
                ", " + n[5] + " : " + cue(atlas,id,n));
            final int index = i;
            card.setOnClickListener(v -> {current[0] = index; choose.run();});
            LinearLayout.LayoutParams lp =
                new LinearLayout.LayoutParams(Ui.dp(a, slotDp), Ui.dp(a, 58));
            lp.setMargins(Ui.dp(a, 1), 0, Ui.dp(a, 1), 0);
            chips.addView(card, lp);
            cards.add(card);
        }
        scroll.addView(chips, new HorizontalScrollView.LayoutParams(-2, -2));
        Button previous = Ui.iconButton(a, "‹", "Occurrence précédente", v -> {
            current[0] = (current[0] + nodes.size() - 1) % nodes.size();
            choose.run();
        });
        Button next = Ui.iconButton(a, "›", "Occurrence suivante", v -> {
            current[0] = (current[0] + 1) % nodes.size();
            choose.run();
        });
        line.addView(previous, new LinearLayout.LayoutParams(Ui.dp(a, 48), Ui.dp(a, 60)));
        line.addView(scroll, new LinearLayout.LayoutParams(0, Ui.dp(a, 60), 1f));
        line.addView(next, new LinearLayout.LayoutParams(Ui.dp(a, 48), Ui.dp(a, 60)));
        panel.addView(line, new LinearLayout.LayoutParams(-1, Ui.dp(a, 60)));
        panel.addView(detail, new LinearLayout.LayoutParams(-1, Ui.dp(a, 36)));
        host.addView(panel, new LinearLayout.LayoutParams(-1, Ui.dp(a, 100)));
        scroll.post(redraw[0]);
    }
}
