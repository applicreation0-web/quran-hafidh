package com.quransafeguard.hifz.preview;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Point;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.quransafeguard.hifz.core.VerseRef;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Private 114-surah semantic map: existing OFFLINE Jalalayn, never Mawdudi/OCR
 * commentary. An overlay does not move or rescale the QCF Mushaf below it.
 * It neither reads nor writes Hifz progress, masks or scheduling.
 */
final class MapJalalaynDialog {
    private final Activity activity;
    private final int surah;
    private final int first;
    private final int last;
    private final Dialog dialog;
    private final ViewGroup embeddedHost;
    private final Runnable onEmbeddedClose;
    private final LinearLayout shell;
    private final TafsirRepository repository;
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    private final TextView heading, body;
    private final android.widget.Button previous, next;
    private final ScrollView scroll;
    private int ayah, request;
    private boolean closed;

    static int boundedAyah(int first,int last,int ayah) {
        if(first<1||last<first)throw new IllegalArgumentException("Invalid passage bounds");
        return Math.max(first,Math.min(last,ayah));
    }
    static int stepAyah(int first,int last,int ayah,int direction) {
        if(direction!=-1&&direction!=1)
            throw new IllegalArgumentException("Tafsir step must be +/-1");
        return boundedAyah(first,last,boundedAyah(first,last,ayah)+direction);
    }

    MapJalalaynDialog(Activity activity,int surah,int first,int last,int requestedAyah) {
        this(activity,surah,first,last,requestedAyah,null,null);
    }

    /** In Lecture, embed the same immutable offline Jalalayn text beside/below the Mushaf. */
    MapJalalaynDialog(Activity activity,int surah,int first,int last,int requestedAyah,
                      ViewGroup embeddedHost,Runnable onEmbeddedClose) {
        if(surah<1||surah>114)throw new IllegalArgumentException("Invalid surah");
        this.activity=activity;
        this.surah=surah;
        this.first=first;
        this.last=last;
        ayah=boundedAyah(first,last,requestedAyah);
        repository=new TafsirRepository(activity);
        this.embeddedHost=embeddedHost;
        this.onEmbeddedClose=onEmbeddedClose;
        dialog=embeddedHost==null?new Dialog(activity):null;
        if(dialog!=null)dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        shell=Ui.column(activity);
        shell.setBackgroundColor(Ui.PAPER);
        shell.setPadding(Ui.dp(activity,8),Ui.dp(activity,5),Ui.dp(activity,8),Ui.dp(activity,7));

        LinearLayout controls=Ui.row(activity);
        controls.setGravity(Gravity.CENTER_VERTICAL);
        controls.addView(Ui.iconButton(activity,"","Retour",v->close()),
            new LinearLayout.LayoutParams(Ui.dp(activity,44),Ui.dp(activity,44)));
        previous=Ui.iconButton(activity,"","Verset précédent",v->move(-1));
        controls.addView(previous,new LinearLayout.LayoutParams(Ui.dp(activity,44),Ui.dp(activity,44)));
        heading=Ui.text(activity,"",14f,true);
        heading.setGravity(Gravity.CENTER);
        controls.addView(heading,new LinearLayout.LayoutParams(0,Ui.dp(activity,44),1));
        next=Ui.iconButton(activity,"","Verset suivant",v->move(1));
        controls.addView(next,new LinearLayout.LayoutParams(Ui.dp(activity,44),Ui.dp(activity,44)));
        shell.addView(controls);

        scroll=new ScrollView(activity);
        scroll.setVerticalScrollBarEnabled(true);
        body=Ui.bookText(activity,"",17f,false);
        body.setTextIsSelectable(true);
        body.setPadding(Ui.dp(activity,6),Ui.dp(activity,5),Ui.dp(activity,6),Ui.dp(activity,5));
        scroll.addView(body,new ScrollView.LayoutParams(-1,-2));
        shell.addView(scroll,new LinearLayout.LayoutParams(-1,0,1f));
        if(dialog!=null) {
            dialog.setContentView(shell);
            dialog.setCanceledOnTouchOutside(true);
            dialog.setOnDismissListener(ignored->close());
        }
    }

    void show() {
        if(closed)return;
        if(embeddedHost!=null) {
            if(shell.getParent()!=embeddedHost) {
                if(shell.getParent() instanceof ViewGroup)
                    ((ViewGroup)shell.getParent()).removeView(shell);
                embeddedHost.removeAllViews();
                embeddedHost.addView(shell,new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));
            }
            embeddedHost.setVisibility(View.VISIBLE);
            render();
            return;
        }
        dialog.show();
        Window window=dialog.getWindow();
        if(window!=null) {
            // BOOX portrait: a real full-screen reading sheet, no partial overlay
            // covering the QCF or forcing the user to zoom. Underlying map is
            // kept in memory, untouched, and restored precisely on dismiss.
            window.setGravity(Gravity.CENTER);
            window.setWindowAnimations(0);
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT);
        }
        render();
    }

    private void move(int direction) {
        if(closed)return;
        int target=stepAyah(first,last,ayah,direction);
        if(target==ayah)return;
        ayah=target;
        scroll.scrollTo(0,0);
        render();
    }

    private void render() {
        if(closed)return;
        final int current=ayah;
        final int token=++request;
        heading.setText("Jalalayn · "+surah+":"+current);
        previous.setEnabled(current>first);
        previous.setAlpha(current>first?1f:.28f);
        next.setEnabled(current<last);
        next.setAlpha(current<last?1f:.28f);
        body.setText("Loading existing offline Tafsir al-Jalalayn…");
        io.execute(()->{
            TafsirRepository.Entry entry=null;
            try{entry=repository.load(new VerseRef(surah,current));}
            catch(Exception unavailable){/* Fail closed; never synthesize commentary. */}
            final TafsirRepository.Entry result=entry;
            activity.runOnUiThread(()->{
                if(closed||activity.isFinishing()||request!=token||current!=ayah)return;
                body.setText(result==null
                    ?"Jalalayn unavailable for this verse in this build."
                    :BaqaraPilotScreen.renderExistingJalalayn(result));
            });
        });
    }

    /** Changing the focused verse refreshes the existing panel without moving the Mushaf. */
    boolean accepts(VerseRef verse) {
        return verse!=null&&verse.getSurah()==surah
            &&verse.getAyah()>=first&&verse.getAyah()<=last;
    }
    void selectVerse(VerseRef verse) {
        if(closed||!accepts(verse)||ayah==verse.getAyah())return;
        ayah=verse.getAyah();
        scroll.scrollTo(0,0);
        render();
    }
    boolean isShowing(){
        return !closed&&(dialog!=null?dialog.isShowing()
            :embeddedHost!=null&&shell.getParent()==embeddedHost
                &&embeddedHost.getVisibility()==View.VISIBLE);
    }
    void close() {
        if(closed)return;
        closed=true;
        request++;
        io.shutdownNow();
        if(dialog!=null){
            if(dialog.isShowing())dialog.dismiss();
        }else{
            if(shell.getParent()==embeddedHost)embeddedHost.removeView(shell);
            if(onEmbeddedClose!=null)onEmbeddedClose.run();
        }
    }
}
