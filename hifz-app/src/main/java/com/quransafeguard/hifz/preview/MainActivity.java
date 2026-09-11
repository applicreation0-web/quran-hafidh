package com.quransafeguard.hifz.preview;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.quransafeguard.hifz.core.HifzSchedule;
import com.quransafeguard.hifz.core.ScheduledSession;
import com.quransafeguard.hifz.core.SessionType;
import com.quransafeguard.hifz.core.VerseRef;

import java.time.LocalDate;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Quran Hifz test home. No Safeguard/blocking API is linked here. */
public final class MainActivity extends android.app.Activity {
    private HifzPrefs prefs;
    private volatile GeometryRepository geometry;
    private TextView today;
    private final ExecutorService localLoader = Executors.newSingleThreadExecutor();

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = new HifzPrefs(this);

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = Ui.column(this);
        root.setPadding(Ui.dp(this,20),Ui.dp(this,20),Ui.dp(this,20),Ui.dp(this,24));
        scroll.addView(root);

        root.addView(Ui.text(this,"Quran Hifz TEST",28,true));
        TextView subtitle=Ui.text(this,"BOOX / E‑Ink · aucun blocage d’applications",14,false);
        subtitle.setPadding(0,Ui.dp(this,4),0,Ui.dp(this,16));root.addView(subtitle);

        today=Ui.text(this,"Initialisation locale du parcours…",18,true);
        today.setPadding(0,0,0,Ui.dp(this,16));root.addView(today);

        root.addView(Ui.button(this,"Lecture / Étude · Tafsir",v->startActivity(new Intent(this,StudyReaderActivity.class))));
        root.addView(Ui.button(this,"Mémorisation libre",v->startActivity(new Intent(this,FreeMemActivity.class))));
        root.addView(Ui.button(this,"Sabqi · 5 lignes · 37 répétitions",v->openSession(HifzSessionActivity.SABQI)));
        root.addView(Ui.button(this,"Itqān · cycle sans fin · ×30",v->openSession(HifzSessionActivity.ITQAN)));
        root.addView(Ui.button(this,"Murājaʿah · révision en blocs de versets",v->openSession(HifzSessionActivity.MURAJAAH)));
        root.addView(Ui.button(this,"État / paramètres de test",v->startActivity(new Intent(this,SettingsActivity.class))));

        TextView limits=Ui.text(this,"Audio Hifz : logique isolée, sans incrément des répétitions ni déplacement des curseurs. La récitation Al‑Husary n’est pas embarquée tant que sa redistribution n’est pas validée.\n\nDurées 90/60/45 min et split de masque Itqān 10/5/5/5/5 : paramètres de travail, pas décisions irréversibles.",13,false);
        limits.setPadding(0,Ui.dp(this,18),0,0);root.addView(limits);

        // First frame must never wait for the full 604-page geometry parse.
        setContentView(scroll);
        localLoader.execute(() -> {
            try {
                GeometryRepository loaded = GeometryRepository.get(getApplicationContext());
                geometry = loaded;
                runOnUiThread(this::refreshToday);
            } catch (Throwable error) {
                runOnUiThread(() -> today.setText("Géométrie locale indisponible"));
            }
        });
    }

    @Override protected void onResume(){super.onResume();if(today!=null && geometry!=null)refreshToday();}

    private void openSession(String mode){startActivity(new Intent(this,HifzSessionActivity.class).putExtra(HifzSessionActivity.EXTRA_MODE,mode));}

    private void refreshToday(){
        GeometryRepository localGeometry = geometry;
        if (localGeometry == null) {
            today.setText("Initialisation locale du parcours…");
            return;
        }
        LocalDate date=LocalDate.now();
        ScheduledSession scheduled=HifzSchedule.INSTANCE.scheduled(date,prefs.programStartDate(),date);
        if(scheduled==null){today.setText("Aujourd’hui · parcours non démarré");return;}
        SessionType kind=scheduled.getType();
        String line;
        switch(kind){
            case SABQI:{int cursor=prefs.sabqiLineCursor();if(cursor<0)cursor=localGeometry.firstLineIndex(new VerseRef(2,75));GeometryRepository.FiveLineBlock b=localGeometry.fiveLineBlock(cursor);line="Aujourd’hui — Sabqi\nSourate "+b.startVerse.getSurah()+" · "+b.verseLabel()+"\n5 lignes · 37 répétitions";break;}
            case ITQAN:{GeometryRepository.VerseUnit u=localGeometry.eligiblePageUnit(prefs.itqanCursor(),prefs.corpus());line="Aujourd’hui — Itqān\nSourate "+u.start.getSurah()+" · "+u.start+" → "+u.end+"\n×30 · corpus cyclique";break;}
            case MURAJAAH:{int planned=(int)Math.floor(PreviewConfig.MURAJAAH_ITQAN_MINUTES_WORKING*60.0/prefs.murajaahSecondsPerLine());GeometryRepository.EligibleLinePlan p=localGeometry.planEligibleLines(prefs.murajaahCursor(),Math.max(1,planned),prefs.corpus());line="Aujourd’hui — Murājaʿah\nBloc A : Sabqi récent ("+prefs.recentSabqi().size()+" en attente)\nBloc B : "+p.start+" → "+p.actualPlannedEnd+" (prévision)";break;}
            default: throw new IllegalStateException("Unsupported Hifz session type: "+kind);
        }
        today.setText(line);
    }

    @Override protected void onDestroy() {
        localLoader.shutdownNow();
        super.onDestroy();
    }
}
