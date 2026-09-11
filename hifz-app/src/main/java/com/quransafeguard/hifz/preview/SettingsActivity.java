package com.quransafeguard.hifz.preview;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

public final class SettingsActivity extends android.app.Activity {
    private HifzPrefs prefs;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new HifzPrefs(this);
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = Ui.column(this);
        scroll.addView(root);

        LinearLayout top = Ui.row(this);
        top.addView(Ui.smallButton(this,"‹",v->finish()));
        TextView title=Ui.text(this,"Paramètres",22,true);Ui.weight(title,1);top.addView(title);root.addView(top);

        Switch eink = new Switch(this);
        eink.setText("Optimisation E‑Ink / BOOX");
        eink.setChecked(prefs.forceEink());
        eink.setOnCheckedChangeListener((button,checked)->prefs.setForceEink(checked));
        root.addView(eink);

        TextView programTitle=Ui.text(this,"Parcours Hifz",17,true);
        programTitle.setPadding(0,Ui.dp(this,18),0,Ui.dp(this,6));root.addView(programTitle);
        root.addView(Ui.text(this,
            "Lun / Mer / Ven · Sabqi le matin, révision courte le soir\n"+
            "Mar / Jeu · Itqān ×30\n"+
            "Sam / Dim · Murājaʿah matin et soir",
            14,false));

        TextView audio=Ui.text(this,"Audio Al‑Husary Muʿallim\nCorpus local séparé à installer. L’audio ne modifie ni les répétitions ni les curseurs.",13,false);
        audio.setPadding(0,Ui.dp(this,18),0,0);root.addView(audio);

        TextView testTitle=Ui.text(this,"Données de test",17,true);
        testTitle.setPadding(0,Ui.dp(this,20),0,Ui.dp(this,6));root.addView(testTitle);
        root.addView(Ui.button(this,"Voir le diagnostic Hifz",v->showDiagnostic()));
        root.addView(Ui.button(this,"Réinitialiser l’état de test",v->confirmReset()));

        setContentView(scroll);
        int insetBase = Ui.dp(this,16);
        Ui.respectSystemBars(this, root, insetBase, insetBase, insetBase, insetBase);
    }

    private void showDiagnostic() {
        String state =
            "Schéma : "+prefs.schema()+"\n"+
            "Début programme : "+prefs.programStartDate()+"\n"+
            "Curseur Itqān : "+prefs.itqanCursor()+"\n"+
            "Curseur Murājaʿah : "+prefs.murajaahCursor()+"\n"+
            "Frontière promue : "+prefs.promotedFrontier()+"\n"+
            "Ligne Sabqi : "+prefs.sabqiLineCursor()+"\n"+
            "File Sabqi récent : "+prefs.recentSabqi().size();
        new AlertDialog.Builder(this)
            .setTitle("Diagnostic Hifz")
            .setMessage(state)
            .setPositiveButton("Fermer",null)
            .show();
    }

    private void confirmReset() {
        new AlertDialog.Builder(this)
            .setTitle("Réinitialiser l’état de test ?")
            .setMessage("Remet les curseurs du scénario de référence : Sabqi 2:75, Itqān 49:1, corpus 2:1→2:74 ∪ 49:1→114:6.")
            .setNegativeButton("Annuler",null)
            .setPositiveButton("Réinitialiser",(d,w)->{
                prefs.resetPreviewState();
                getSharedPreferences("hifz_preview_session_gates",MODE_PRIVATE).edit().clear().apply();
            })
            .show();
    }
}
