package com.quransafeguard.hifz.preview;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.quransafeguard.hifz.core.HifzSchedule;
import com.quransafeguard.hifz.core.SessionKind;
import com.quransafeguard.hifz.core.VerseRef;

import java.time.LocalDate;

/** Quran Hifz full-scope preview home. No Safeguard/blocking API is linked here. */
public final class MainActivity extends android.app.Activity {
    private HifzPrefs prefs;
    private GeometryRepository geometry;
    private TextView today;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = new HifzPrefs(this);
        geometry = GeometryRepository.get(this);
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = Ui.column(this);
        root.setPadding(Ui.dp(this,20),Ui.dp(this,20),Ui.dp(this,20),Ui.dp(this,24));
        scroll.addView(root);

        root.addView(Ui.text(this,"Quran Hifz",28,true));
        TextView subtitle=Ui.text(this,"Preview fonctionnelle · BOOX / E‑Ink · aucun blocage d’applications",14,false);
        subtitle.setPadding(0,Ui.dp(this,4),0,Ui.dp(this,16));root.addView(subtitle);

        today=Ui.text(this,"",18,true);today.setPadding(0,0,0,Ui.dp(this,16));root.addView(today);refreshToday();

        root.addView(Ui.button(this,"Lecture / Étude · Tafsir",v->startActivity(new Intent(this,StudyReaderActivity.class))));
        root.addView(Ui.button(this,"Mémorisation libre",v->startActivity(new Intent(this,FreeMemActivity.class))));
        root.addView(Ui.button(this,"Sabqi · 5 lignes · 37 répétitions",v->openSession(HifzSessionActivity.SABQI)));
        root.addView(Ui.button(this,"Itqān · cycle sans fin · ×30",v->openSession(HifzSessionActivity.ITQAN)));
        root.addView(Ui.button(this,"Murājaʿah · révision en blocs de versets",v->openSession(HifzSessionActivity.MURAJAAH)));
        root.addView(Ui.button(this,"État / paramètres de test",v->startActivity(new Intent(this,SettingsActivity.class))));

        TextView limits=Ui.text(this,"Audio Hifz : logique isolée prévue, mais récitation Al‑Husary non embarquée dans cette preview tant que l’hébergement/redistribution n’est pas validé.\n\nDurées 90/60/45 min et split de masque Itqān 10/5/5/5/5 : paramètres de travail, pas décisions irréversibles.",13,false);
        limits.setPadding(0,Ui.dp(this,18),0,0);root.addView(limits);
        setContentView(scroll);
    }

    @Override protected void onResume(){super.onResume();if(today!=null)refreshToday();}

    private void openSession(String mode){startActivity(new Intent(this,HifzSessionActivity.class).putExtra(HifzSessionActivity.EXTRA_MODE,mode));}

    private void refreshToday(){
        LocalDate date=LocalDate.now();SessionKind kind=HifzSchedule.INSTANCE.scheduledKind(date,prefs.programStartDate());
        if(kind==null){today.setText("Aujourd’hui · parcours non démarré");return;}
        String line;
        switch(kind){
            case SABQI:{int cursor=prefs.sabqiLineCursor();if(cursor<0)cursor=geometry.firstLineIndex(new VerseRef(2,75));GeometryRepository.FiveLineBlock b=geometry.fiveLineBlock(cursor);line="Aujourd’hui — Sabqi\nSourate "+b.startVerse.getSurah()+" · "+b.verseLabel()+"\n5 lignes · 37 répétitions";break;}
            case ITQAN:{GeometryRepository.VerseUnit u=geometry.eligiblePageUnit(prefs.itqanCursor(),prefs.corpus());line="Aujourd’hui — Itqān\nSourate "+u.start.getSurah()+" · "+u.start+" → "+u.end+"\n×30 · corpus cyclique";break;}
            case MURAJAAH:{int planned=(int)Math.floor(PreviewConfig.MURAJAAH_ITQAN_MINUTES_WORKING*60.0/prefs.murajaahSecondsPerLine());GeometryRepository.EligibleLinePlan p=geometry.planEligibleLines(prefs.murajaahCursor(),Math.max(1,planned),prefs.corpus());line="Aujourd’hui — Murājaʿah\nBloc A : Sabqi récent ("+prefs.recentSabqi().size()+" en attente)\nBloc B : "+p.start+" → "+p.actualPlannedEnd+" (prévision)";break;}
            default: line="Aujourd’hui — Mémorisation libre";
        }
        today.setText(line);
    }
}
