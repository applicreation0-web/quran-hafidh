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

    // One or two keywords per refrain: only adjacent verses, never invented Tafsir.
    private static final String[] RAHMAN_BEFORE = {
        "Grains", "Djinns", "Orients", "Mers", "Perles", "Navires",
        "Permanence", "Dépendance", "Deux communautés", "Limites", "Flammes",
        "Ciel", "Fautes", "Criminels", "Géhenne", "Deux jardins", "Branches",
        "Sources", "Fruits", "Couches", "Regards", "Rubis", "Bienfaisance",
        "Deux jardins", "Verdure", "Sources", "Fruits", "Compagnes", "Pavillons",
        "Compagnes", "Tapis"
    };
    private static final String[] RAHMAN_AFTER = {
        "Création humaine", "Orients", "Deux mers", "Perles", "Navires",
        "Finitude", "Dépendance", "Deux communautés", "Limites", "Flammes",
        "Ciel déchiré", "Fautes", "Criminels", "Géhenne", "Deux jardins",
        "Branches", "Sources", "Fruits", "Couches", "Regards", "Rubis",
        "Bienfaisance", "Deux jardins", "Verdure", "Sources", "Fruits",
        "Compagnes", "Pavillons", "Compagnes", "Tapis", "Majesté"
    };
    private static final String[] QAMAR_BEFORE = {"Nūḥ", "ʿĀd", "Thamūd", "Lūṭ"};
    private static final String[] QAMAR_AFTER = {"ʿĀd", "Thamūd", "Lūṭ", "Pharaon"};

    private static String shortWords(String raw) {
        String text = raw.replace(" : ", " ").replace("’", "'");
        String[] words = text.split("\\s+");
        StringBuilder out = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty() || word.matches("(?i)(le|la|les|de|des|du|et|en|au|aux|à|un|une|qui|sur|dans|pour|avec|leurs|leur|ses|son|sa|ce|ces|d'|l')")) continue;
            if (out.length() > 0) out.append(" ");
            out.append(word);
            if (out.toString().split(" ").length >= 2) break;
        }
        return out.length() > 0 ? out.toString() : raw;
    }

    private static String keyword(String id, String[] node, boolean after) {
        int rank = Integer.parseInt(node[3]) - 1;
        if ("R048".equals(id)) return (after ? RAHMAN_AFTER : RAHMAN_BEFORE)[rank];
        if ("R047".equals(id)) return (after ? QAMAR_AFTER : QAMAR_BEFORE)[rank];
        return shortWords(node[after ? 12 : 9]);
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
            detail.setText("Avant " + keyword(id, selected, false) +
                "  ·  " + selected[5] + "  ·  Après " + keyword(id, selected, true));
            detail.setContentDescription("Lire " + selected[5] + " dans le Mushaf. Avant " +
                keyword(id, selected, false) + ". Après " + keyword(id, selected, true));
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
            TextView card = Ui.text(a, n[3] + "\n" + keyword(id, n, false),
                screenDp < 540 ? 11 : 13, false);
            card.setGravity(Gravity.CENTER);
            card.setMaxLines(3);
            card.setMinHeight(Ui.dp(a, 58));
            card.setPadding(Ui.dp(a, 1), 0, Ui.dp(a, 1), 0);
            card.setContentDescription("Refrain " + n[3] + " sur " + n[4] +
                ", " + n[5] + " : " + keyword(id, n, false));
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
