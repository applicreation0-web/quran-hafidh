package com.quransafeguard.hifz.preview;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

public final class SettingsActivity extends android.app.Activity {
    private HifzPrefs prefs;
    private TextView state;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new HifzPrefs(this);
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = Ui.column(this); scroll.addView(root);
        LinearLayout top = Ui.row(this); top.addView(Ui.smallButton(this,"‹",v->finish()));
        TextView title=Ui.text(this,"Paramètres",22,true);Ui.weight(title,1);top.addView(title);root.addView(top);

        Switch eink = new Switch(this); eink.setText("Forcer le rendu E‑Ink"); eink.setChecked(prefs.forceEink());
        eink.setOnCheckedChangeListener((button,checked)->prefs.setForceEink(checked)); root.addView(eink);

        root.addView(Ui.button(this,"Réinitialiser l’état Hifz",v->new AlertDialog.Builder(this)
            .setTitle("Réinitialiser ?").setMessage("Remet les curseurs du scénario de référence : Sabqi 2:75, Itqān 49:1, corpus 2:1→2:74 ∪ 49:1→114:6.")
            .setNegativeButton("Annuler",null).setPositiveButton("Réinitialiser",(d,w)->{prefs.resetPreviewState();getSharedPreferences("hifz_preview_session_gates",MODE_PRIVATE).edit().clear().apply();refresh();}).show()));

        state=Ui.text(this,"",14,false);state.setPadding(0,Ui.dp(this,16),0,0);root.addView(state);refresh();
        TextView audio=Ui.text(this,"Audio Al‑Husary Muʿallim\nCorpus local séparé à installer. L’audio ne modifie ni les répétitions ni les curseurs.",13,false);audio.setPadding(0,Ui.dp(this,18),0,0);root.addView(audio);
        TextView working=Ui.text(this,"Réglages actuels (modifiables)\n• Sabqi 90 min\n• Itqān 60 min\n• Murājaʿah 45 min = 15 min Sabqi récent + 30 min cycle Itqān\n• Itqān masques : 10/5/5/5/5\n• vitesse initiale Murājaʿah : 9 s/ligne\n• nettoyage E‑Ink : à chaque changement de page",13,false);working.setPadding(0,Ui.dp(this,18),0,0);root.addView(working);
        setContentView(scroll);
        int insetBase = Ui.dp(this,16);
        Ui.respectSystemBars(this, root, insetBase, insetBase, insetBase, insetBase);
    }

    private void refresh(){
        state.setText("Schéma : "+prefs.schema()+"\nDébut programme : "+prefs.programStartDate()+"\nCorpus Itqān : "+prefs.corpus().getRanges()+"\nCurseur Itqān : "+prefs.itqanCursor()+"\nCurseur Murājaʿah : "+prefs.murajaahCursor()+"\nFrontière promue : "+prefs.promotedFrontier()+"\nLigne Sabqi : "+prefs.sabqiLineCursor()+"\nFile Sabqi récent : "+prefs.recentSabqi().size());
    }
}
