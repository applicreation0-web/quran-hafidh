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

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Quran Hifz personal home. No Safeguard/blocking API is linked here. */
public final class MainActivity extends android.app.Activity {
    private HifzPrefs prefs;
    private DashboardLedger ledger;
    private volatile GeometryRepository geometry;
    private TextView today;
    private Button todayButton;
    private LinearLayout dashboard;
    private final ExecutorService localLoader = Executors.newSingleThreadExecutor();

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);prefs=new HifzPrefs(this);ledger=new DashboardLedger(this);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);
        FrameLayout holder=new FrameLayout(this);holder.setBackgroundColor(Ui.PAPER);scroll.addView(holder,new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));
        LinearLayout root=Ui.column(this);int side=Ui.dp(this,18),bottom=Ui.dp(this,24);root.setPadding(side,side,side,bottom);
        int screen=getResources().getDisplayMetrics().widthPixels;int contentWidth=Math.max(Ui.dp(this,300),Math.min(screen-Ui.dp(this,18),Ui.dp(this,900)));
        holder.addView(root,new FrameLayout.LayoutParams(contentWidth,ViewGroup.LayoutParams.WRAP_CONTENT,Gravity.TOP|Gravity.CENTER_HORIZONTAL));

        TextView title=Ui.text(this,"Quran Hifz",28,true);title.setGravity(Gravity.CENTER_HORIZONTAL);root.addView(title);
        TextView subtitle=Ui.text(this,"BOOX Go 10.3 Gen II · offline · données locales",13,false);subtitle.setGravity(Gravity.CENTER_HORIZONTAL);subtitle.setPadding(0,Ui.dp(this,2),0,Ui.dp(this,12));root.addView(subtitle);
        today=Ui.text(this,"Initialisation locale du parcours…",16,true);today.setGravity(Gravity.CENTER_HORIZONTAL);today.setPadding(0,0,0,Ui.dp(this,8));root.addView(today);

        LinearLayout primary=Ui.row(this);primary.setGravity(Gravity.CENTER);
        LinearLayout todayAction=Ui.roundAction(this,"▶","Séance",v->openToday());todayButton=(Button)todayAction.getChildAt(0);todayButton.setEnabled(false);
        LinearLayout study=Ui.roundAction(this,"◫","Lecture",v->startActivity(new Intent(this,StudyReaderActivity.class)));
        LinearLayout free=Ui.roundAction(this,"M","Mémoriser",v->startActivity(new Intent(this,FreeMemActivity.class)));
        LinearLayout settings=Ui.roundAction(this,"⚙","Paramètres",v->startActivity(new Intent(this,SettingsActivity.class)));
        primary.addView(todayAction);primary.addView(study);primary.addView(free);primary.addView(settings);root.addView(primary);

        TextView dashTitle=Ui.text(this,"Semaine",18,true);dashTitle.setPadding(0,Ui.dp(this,16),0,Ui.dp(this,5));root.addView(dashTitle);
        dashboard=Ui.column(this);dashboard.setPadding(0,0,0,0);root.addView(dashboard);

        TextView directTitle=Ui.text(this,"Accès direct",15,true);directTitle.setPadding(0,Ui.dp(this,16),0,Ui.dp(this,3));root.addView(directTitle);
        LinearLayout direct=Ui.row(this);direct.setGravity(Gravity.CENTER);
        direct.addView(Ui.roundAction(this,"S","Sabqi",v->openMode(HifzSessionActivity.SABQI)));
        direct.addView(Ui.roundAction(this,"I","Itqān",v->openMode(HifzSessionActivity.ITQAN)));
        direct.addView(Ui.roundAction(this,"R","Murājaʿah",v->openMode(HifzSessionActivity.MURAJAAH)));
        root.addView(direct);

        setContentView(scroll);Ui.respectSystemBars(this,holder,0,0,0,0);
        localLoader.execute(()->{
            try{GeometryRepository loaded=GeometryRepository.get(getApplicationContext());geometry=loaded;runOnUiThread(()->{todayButton.setEnabled(true);ledger.capture(prefs);refreshAll();});}
            catch(Throwable error){runOnUiThread(()->{today.setText("Géométrie locale indisponible");todayButton.setEnabled(false);});}
        });
    }

    @Override protected void onResume(){
        super.onResume();prefs=new HifzPrefs(this);if(ledger==null)ledger=new DashboardLedger(this);ledger.capture(prefs);if(today!=null&&geometry!=null)refreshAll();
    }

    private void refreshAll(){refreshToday();refreshDashboard();}

    private void openMode(String mode){startActivity(new Intent(this,HifzSessionActivity.class).putExtra(HifzSessionActivity.EXTRA_MODE,mode));}
    private void openToday(){
        LocalDate date=LocalDate.now();ScheduledSession scheduled=HifzSchedule.INSTANCE.scheduled(date,prefs.programStartDate(),date);if(scheduled==null)return;
        switch(scheduled.getType()){case SABQI:openMode(HifzSessionActivity.SABQI);break;case ITQAN:openMode(HifzSessionActivity.ITQAN);break;case MURAJAAH:openMode(HifzSessionActivity.MURAJAAH);break;default:throw new IllegalStateException("Unsupported Hifz session type: "+scheduled.getType());}
    }

    private void refreshToday(){
        GeometryRepository g=geometry;if(g==null){today.setText("Initialisation locale du parcours…");return;}
        LocalDate date=LocalDate.now();ScheduledSession scheduled=HifzSchedule.INSTANCE.scheduled(date,prefs.programStartDate(),date);
        if(scheduled==null){today.setText("Aujourd’hui · parcours structuré non démarré");todayButton.setEnabled(false);return;}
        String detail;
        try{
            SessionType kind=scheduled.getType();
            switch(kind){
                case SABQI:{
                    int cursor=prefs.sabqiLineCursor();if(cursor<0)cursor=g.firstLineIndex(prefs.sabqiStart());
                    if(cursor<g.firstLineIndex(prefs.sabqiStart())||cursor>g.lastLineIndex(prefs.sabqiEnd()))detail="Sabqi · curseur à repositionner";
                    else{GeometryRepository.FiveLineBlock b=g.fiveLineBlock(cursor);int rep=prefs.sabqiRep();String state=rep>=PreviewConfig.SABQI_TOTAL_REPS?"✓ prêt à valider":rep>0?"reprise "+(rep+1)+"/37":"37 répétitions";detail="Sabqi · "+range(b.startVerse,b.endVerse)+" · "+state;}break;
                }
                case ITQAN:{
                    if(!prefs.isItqanCursorValid()){detail="Itqān · curseur hors des plages";break;}
                    int rep=prefs.itqanRep();VerseRef start=prefs.itqanUnitStart(),end=prefs.itqanUnitEnd();
                    if(rep>0&&start!=null&&end!=null){String state=rep>=PreviewConfig.ITQAN_TOTAL_REPS?"✓ prêt à valider":"reprise "+(rep+1)+"/30";detail="Itqān · "+range(start,end)+" · "+state;}
                    else{GeometryRepository.VerseUnit u=g.eligiblePageUnit(prefs.itqanCursor(),prefs.corpus());detail="Itqān · "+range(u.start,u.end)+" · ×30";}break;
                }
                case MURAJAAH:{detail="Murājaʿah · Bloc "+prefs.murajaahPhase()+" · "+prefs.recentSabqi().size()+" bloc(s) Sabqi récent";break;}
                default:throw new IllegalStateException("Unsupported Hifz session type: "+kind);
            }
        }catch(RuntimeException error){detail="Parcours à vérifier · "+(error.getMessage()==null?error.getClass().getSimpleName():error.getMessage());}
        today.setText("Aujourd’hui · "+detail);todayButton.setEnabled(true);
    }

    private void refreshDashboard(){
        if(dashboard==null||geometry==null)return;
        dashboard.removeAllViews();
        LinearLayout header=Ui.row(this);header.setPadding(0,Ui.dp(this,2),0,Ui.dp(this,4));
        addCell(header,"Jour",0.45f,true);addCell(header,"Matin",2.2f,true);addCell(header,"Soir",2.2f,true);addCell(header,"État",0.9f,true);dashboard.addView(header);
        List<WeeklyDashboardPlanner.Row> rows=new WeeklyDashboardPlanner(prefs,geometry,ledger).week(LocalDate.now());
        for(WeeklyDashboardPlanner.Row item:rows){
            LinearLayout row=Ui.row(this);row.setPadding(0,Ui.dp(this,4),0,Ui.dp(this,4));
            addCell(row,item.day,0.45f,true);addCell(row,item.morning,2.2f,false);addCell(row,item.evening,2.2f,false);addCell(row,item.state,0.9f,false);dashboard.addView(row);
        }
        TextView note=Ui.text(this,"Passages futurs = projection locale depuis les curseurs réels, sans déplacement. ✓ n’apparaît qu’après validation enregistrée.",11,false);note.setPadding(0,Ui.dp(this,5),0,0);dashboard.addView(note);
    }

    private void addCell(LinearLayout row,String value,float weight,boolean bold){
        TextView cell=Ui.text(this,value,11.5f,bold);cell.setPadding(Ui.dp(this,4),Ui.dp(this,2),Ui.dp(this,4),Ui.dp(this,2));
        cell.setLayoutParams(new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,weight));row.addView(cell);
    }

    private static String range(VerseRef a,VerseRef b){
        if(a.getSurah()==b.getSurah())return "Sourate "+a.getSurah()+" · v."+a.getAyah()+"–"+b.getAyah();
        return "Sourate "+a.getSurah()+" v."+a.getAyah()+" → "+b.getSurah()+":"+b.getAyah();
    }

    @Override protected void onDestroy(){localLoader.shutdownNow();super.onDestroy();}
}
