package com.quransafeguard.hifz.preview;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.quransafeguard.hifz.core.HifzSchedule;
import com.quransafeguard.hifz.core.ScheduledSession;
import com.quransafeguard.hifz.core.SessionType;
import com.quransafeguard.hifz.core.VerseRef;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Quran Hifz personal home. No Safeguard/blocking API is linked here. */
public final class MainActivity extends android.app.Activity {
    private HifzPrefs prefs;
    private volatile GeometryRepository geometry;
    private TextView today;
    private Button todayButton, freeMemButton;
    private final ExecutorService localLoader = Executors.newSingleThreadExecutor();

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = new HifzPrefs(this);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        FrameLayout holder = new FrameLayout(this);
        holder.setBackgroundColor(Ui.PAPER);
        scroll.addView(holder, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout root = Ui.column(this);
        int side = Ui.dp(this,20);
        int bottom = Ui.dp(this,24);
        root.setPadding(side,side,side,bottom);
        int screen = getResources().getDisplayMetrics().widthPixels;
        int contentWidth = Math.max(Ui.dp(this,280), Math.min(screen - Ui.dp(this,24), Ui.dp(this,760)));
        FrameLayout.LayoutParams rootLp = new FrameLayout.LayoutParams(contentWidth, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        holder.addView(root, rootLp);

        root.addView(Ui.text(this,"Quran Hifz",28,true));
        TextView subtitle=Ui.text(this,"Personnel · téléphone + tablette BOOX · données locales",14,false);
        subtitle.setPadding(0,Ui.dp(this,4),0,Ui.dp(this,16));root.addView(subtitle);

        today=Ui.text(this,"Initialisation locale du parcours…",17,true);
        today.setPadding(0,0,0,Ui.dp(this,12));root.addView(today);

        todayButton=Ui.button(this,"Ouvrir la séance structurée prévue",v->openToday());
        todayButton.setEnabled(false); root.addView(todayButton);
        root.addView(Ui.button(this,"Lecture / Étude · Tafsir",v->startActivity(new Intent(this,StudyReaderActivity.class))));
        freeMemButton=Ui.button(this,"Mémorisation libre",v->startActivity(new Intent(this,FreeMemActivity.class)));
        freeMemButton.setEnabled(false); root.addView(freeMemButton);

        TextView directTitle=Ui.text(this,"Accès direct",16,true);
        directTitle.setPadding(0,Ui.dp(this,18),0,Ui.dp(this,4));root.addView(directTitle);
        TextView directHelp=Ui.text(this,"Pour travailler ou tester un mode sans changer le calendrier par la seule ouverture.",13,false);
        directHelp.setPadding(0,0,0,Ui.dp(this,6));root.addView(directHelp);
        LinearLayout direct=Ui.row(this);
        Button sabqi=Ui.smallButton(this,"Sabqi",v->openMode(HifzSessionActivity.SABQI));
        Button itqan=Ui.smallButton(this,"Itqān",v->openMode(HifzSessionActivity.ITQAN));
        Button murajaah=Ui.smallButton(this,"Murājaʿah",v->openMode(HifzSessionActivity.MURAJAAH));
        Ui.weight(sabqi,1);Ui.weight(itqan,1);Ui.weight(murajaah,1);
        direct.addView(sabqi);direct.addView(itqan);direct.addView(murajaah);root.addView(direct);

        root.addView(Ui.button(this,"Paramètres",v->startActivity(new Intent(this,SettingsActivity.class))));

        setContentView(scroll);
        Ui.respectSystemBars(this, holder, 0, 0, 0, 0);
        localLoader.execute(() -> {
            try {
                GeometryRepository loaded = GeometryRepository.get(getApplicationContext());
                geometry = loaded;
                runOnUiThread(() -> {
                    todayButton.setEnabled(true);
                    freeMemButton.setEnabled(true);
                    refreshToday();
                });
            } catch (Throwable error) {
                runOnUiThread(() -> {
                    today.setText("Géométrie locale indisponible");
                    todayButton.setEnabled(false);
                    freeMemButton.setEnabled(false);
                });
            }
        });
    }

    @Override protected void onResume(){super.onResume();if(today!=null && geometry!=null)refreshToday();}

    private void openMode(String mode) {
        startActivity(new Intent(this,HifzSessionActivity.class).putExtra(HifzSessionActivity.EXTRA_MODE,mode));
    }

    private void openToday(){
        LocalDate date=LocalDate.now();
        ScheduledSession scheduled=HifzSchedule.INSTANCE.scheduled(date,prefs.programStartDate(),date);
        if(scheduled==null) return;
        switch(scheduled.getType()){
            case SABQI: openMode(HifzSessionActivity.SABQI); break;
            case ITQAN: openMode(HifzSessionActivity.ITQAN); break;
            case MURAJAAH: openMode(HifzSessionActivity.MURAJAAH); break;
            default: throw new IllegalStateException("Unsupported Hifz session type: "+scheduled.getType());
        }
    }

    private String personalDayPlan(DayOfWeek day) {
        switch (day) {
            case MONDAY:
                return "Matin · Sabqi Al-Baqarah · 5 lignes · 90 min\nSoir · Révision Sabqi · 15–20 min";
            case TUESDAY:
                return "Itqān Hujurāt → An-Nās · 1 page ×30";
            case WEDNESDAY:
                return "Matin · Sabqi Al-Baqarah · 5 lignes · 90 min\nSoir · Révision Sabqi cumulative · 15–20 min";
            case THURSDAY:
                return "Itqān Hujurāt → An-Nās · page suivante ×30";
            case FRIDAY:
                return "Matin · Sabqi Al-Baqarah · 5 lignes · 90 min\nSoir · Bilan Sabqi · 15–20 min";
            case SATURDAY:
                return "Matin · Murājaʿah Hujurāt → An-Nās · 60 min\nSoir · Murājaʿah Al-Baqarah · 60 min";
            case SUNDAY:
                return "Matin · suite Murājaʿah Hujurāt → An-Nās · 60 min\nSoir · suite Murājaʿah Al-Baqarah · 60 min";
            default:
                throw new IllegalStateException("Unsupported day: "+day);
        }
    }

    private void refreshToday(){
        GeometryRepository localGeometry = geometry;
        if (localGeometry == null) {
            today.setText("Initialisation locale du parcours…");
            return;
        }
        LocalDate date=LocalDate.now();
        ScheduledSession scheduled=HifzSchedule.INSTANCE.scheduled(date,prefs.programStartDate(),date);
        if(scheduled==null){today.setText("Aujourd’hui\n"+personalDayPlan(date.getDayOfWeek())+"\n\nParcours structuré non démarré.");todayButton.setEnabled(false);return;}
        SessionType kind=scheduled.getType();
        String detail;
        switch(kind){
            case SABQI:{int cursor=prefs.sabqiLineCursor();if(cursor<0)cursor=localGeometry.firstLineIndex(new VerseRef(2,75));GeometryRepository.FiveLineBlock b=localGeometry.fiveLineBlock(cursor);detail="Séance structurée actuelle · Sabqi · "+b.verseLabel()+" · 37 répétitions";break;}
            case ITQAN:{
                int rep=prefs.itqanRep();
                VerseRef start=prefs.itqanUnitStart(), end=prefs.itqanUnitEnd();
                if(rep>0 && start!=null && end!=null) detail="Séance structurée actuelle · Itqān · "+start+" → "+end+" · reprise "+(rep+1)+"/30";
                else {GeometryRepository.VerseUnit u=localGeometry.eligiblePageUnit(prefs.itqanCursor(),prefs.corpus());detail="Séance structurée actuelle · Itqān · "+u.start+" → "+u.end+" · ×30";}
                break;
            }
            case MURAJAAH:{detail="Séance structurée actuelle · Murājaʿah";break;}
            default: throw new IllegalStateException("Unsupported Hifz session type: "+kind);
        }
        today.setText("Aujourd’hui\n"+personalDayPlan(date.getDayOfWeek())+"\n\n"+detail);
        todayButton.setEnabled(true);
    }

    @Override protected void onDestroy() {
        localLoader.shutdownNow();
        super.onDestroy();
    }
}
