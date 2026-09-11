package com.quransafeguard.hifz.test;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.quransafeguard.hifz.core.EligibleCorpus;
import com.quransafeguard.hifz.core.HifzState;
import com.quransafeguard.hifz.core.SabqiRepetitionPlan;
import com.quransafeguard.hifz.core.VerseRef;

public final class MainActivity extends Activity {
    private HifzState state;
    private TextView status;
    private TextView log;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        state = new HifzState(
            new VerseRef(2, 1),
            new VerseRef(2, 74),
            new VerseRef(49, 1),
            new VerseRef(49, 1),
            new VerseRef(49, 1)
        );

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(24), dp(24), dp(24));
        root.setBackgroundColor(Color.WHITE);
        scroll.addView(root);

        TextView title = text("Quran Hifz — moteur parallèle TEST", 24, true);
        root.addView(title);

        TextView warning = text(
            "Harness technique : ce n’est pas encore l’application Quran Hifz finale. " +
            "Il sert à vérifier le moteur verset-par-verset avant intégration du Mushaf, du Tafsir et du rendu BOOX.",
            16,
            false
        );
        warning.setPadding(0, dp(12), 0, dp(16));
        root.addView(warning);

        status = text("", 18, false);
        root.addView(status);

        root.addView(button("Avancer Itqān d’un verset", new View.OnClickListener() {
            @Override public void onClick(View v) {
                VerseRef before = state.getItqanCursor();
                state = state.advanceItqanVerse();
                append("Itqān : " + before + " → " + state.getItqanCursor());
                refresh();
            }
        }));

        root.addView(button("Avancer Murājaʿah d’un verset", new View.OnClickListener() {
            @Override public void onClick(View v) {
                VerseRef before = state.getMurajaahItqanCursor();
                state = state.advanceMurajaahVerse();
                append("Murājaʿah : " + before + " → " + state.getMurajaahItqanCursor());
                refresh();
            }
        }));

        root.addView(button("Promouvoir Al-Baqarah jusqu’à 2:95", new View.OnClickListener() {
            @Override public void onClick(View v) {
                VerseRef oldCursor = state.getItqanCursor();
                state = state.promoteTo(new VerseRef(2, 95));
                append("Promotion corpus : frontier 2:95 ; curseur Itqān reste " + oldCursor);
                refresh();
            }
        }));

        root.addView(button("Tester wrap 114:6 → 2:1", new View.OnClickListener() {
            @Override public void onClick(View v) {
                EligibleCorpus corpus = state.corpus();
                VerseRef wrapped = corpus.next(new VerseRef(114, 6));
                append("Wrap test : 114:6 → " + wrapped + (wrapped.equals(new VerseRef(2, 1)) ? "  ✓" : "  ✗"));
            }
        }));

        root.addView(button("Tester fin du premier intervalle", new View.OnClickListener() {
            @Override public void onClick(View v) {
                VerseRef frontier = state.getPromotedFrontier();
                VerseRef next = state.corpus().next(frontier);
                append("Fin intervalle : " + frontier + " → " + next + " (gap sauté)");
            }
        }));

        TextView sabqi = text(
            "Sabqi contract\n5 lignes réelles par bloc\n" +
            "15 visible + 5 masque 25% + 5 masque 50% + 5 masque 75% + 7 masque 100% = " +
            SabqiRepetitionPlan.TOTAL_REPS + " répétitions",
            17,
            false
        );
        sabqi.setPadding(0, dp(18), 0, dp(12));
        root.addView(sabqi);

        log = text("Journal\n", 15, false);
        root.addView(log);

        setContentView(scroll);
        refresh();
    }

    private void refresh() {
        status.setText(
            "Corpus éligible : " + state.corpus().getRanges() + "\n" +
            "Curseur Itqān : " + state.getItqanCursor() + "\n" +
            "Curseur Murājaʿah : " + state.getMurajaahItqanCursor() + "\n" +
            "Frontier promu : " + state.getPromotedFrontier() + "\n"
        );
    }

    private void append(String message) {
        log.append("• " + message + "\n");
    }

    private Button button(String label, View.OnClickListener listener) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(16f);
        b.setOnClickListener(listener);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        p.setMargins(0, dp(6), 0, dp(6));
        b.setLayoutParams(p);
        return b;
    }

    private TextView text(String value, int sp, boolean bold) {
        TextView tv = new TextView(this);
        tv.setText(value);
        tv.setTextColor(Color.BLACK);
        tv.setTextSize((float) sp);
        if (bold) tv.setTypeface(tv.getTypeface(), android.graphics.Typeface.BOLD);
        return tv;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
