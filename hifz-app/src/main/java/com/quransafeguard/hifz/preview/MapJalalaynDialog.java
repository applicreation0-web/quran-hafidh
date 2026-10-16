package com.quransafeguard.hifz.preview;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Point;
import android.view.Gravity;
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

    MapJalalaynDialog(Activity host,int surah,int first,int last,int requestedAyah) {
        if(surah<1||surah>114)throw new IllegalArgumentException("Invalid surah");
        this.activity=host;
        this.surah=surah;
        this.first=first;
        this.last=last;
        ayah=boundedAyah(first,last,requestedAyah);
        repository=new TafsirRepository(host);
        dialog=new Dialog(host);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout shell=Ui.column(host);
        shell.setBackgroundColor(Ui.PAPER);
        shell.setPadding(Ui.dp(host,8),Ui.dp(host,5),Ui.dp(host,8),Ui.dp(host,7));

        LinearLayout controls=Ui.row(host);
        controls.setGravity(Gravity.CENTER_VERTICAL);
        controls.addView(Ui.iconButton(host,"","Retour à la carte",v->close()),
            new LinearLayout.LayoutParams(Ui.dp(host,44),Ui.dp(host,44)));
        previous=Ui.iconButton(host,"","Verset précédent",v->move(-1));
        controls.addView(previous,new LinearLayout.LayoutParams(Ui.dp(host,44),Ui.dp(host,44)));
        heading=Ui.text(host,"",14f,true);
        heading.setGravity(Gravity.CENTER);
        controls.addView(heading,new LinearLayout.LayoutParams(0,Ui.dp(host,44),1));
        next=Ui.iconButton(host,"","Verset suivant",v->move(1));
        controls.addView(next,new LinearLayout.LayoutParams(Ui.dp(host,44),Ui.dp(host,44)));
        shell.addView(controls);

        scroll=new ScrollView(host);
        scroll.setVerticalScrollBarEnabled(true);
        body=Ui.bookText(host,"",13.5f,false);
        body.setTextIsSelectable(true);
        body.setPadding(Ui.dp(host,6),Ui.dp(host,5),Ui.dp(host,6),Ui.dp(host,5));
        scroll.addView(body,new ScrollView.LayoutParams(-1,-2));
        shell.addView(scroll,new LinearLayout.LayoutParams(-1,0,1f));
        dialog.setContentView(shell);
        dialog.setCanceledOnTouchOutside(true);
        dialog.setOnDismissListener(ignored->close());
    }

    void show() {
        if(closed)return;
        dialog.show();
        Window window=dialog.getWindow();
        if(window!=null) {
            window.setGravity(Gravity.CENTER);
            window.setWindowAnimations(0);
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setBackgroundDrawableResource(android.R.color.transparent);
            Point screen=new Point();
            activity.getWindowManager().getDefaultDisplay().getRealSize(screen);
            window.setLayout(Math.max(Ui.dp(activity,200),screen.x-Ui.dp(activity,24)),
                Math.max(Ui.dp(activity,220),screen.y*3/5));
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

    boolean isShowing(){return !closed&&dialog.isShowing();}
    void close() {
        if(closed)return;
        closed=true;
        request++;
        io.shutdownNow();
        if(dialog.isShowing())dialog.dismiss();
    }
}
