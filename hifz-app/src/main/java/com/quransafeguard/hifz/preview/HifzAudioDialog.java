package com.quransafeguard.hifz.preview;

import android.app.Activity;
import android.app.Dialog;
import android.media.MediaPlayer;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.quransafeguard.hifz.core.VerseRef;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Audio controls deliberately isolated from HifzPrefs/repetition/cursor state. */
final class HifzAudioDialog {
    private final Activity activity;
    private final MushafView mushaf;
    private final HifzAudioPack pack;
    private final ArrayList<VerseRef> queue;
    private Dialog dialog;
    private MediaPlayer player;
    private TextView title;
    private Button playPause;
    private int index;
    private boolean paused;

    HifzAudioDialog(Activity activity, MushafView mushaf, List<VerseRef> verses) {
        this.activity = activity;
        this.mushaf = mushaf;
        this.pack = new HifzAudioPack(activity);
        this.queue = new ArrayList<>(verses == null ? java.util.Collections.emptyList() : verses);
    }

    void show() {
        if (!pack.installed()) {
            Toast.makeText(activity, "Installez d’abord le pack audio local dans Paramètres.", Toast.LENGTH_LONG).show();
            return;
        }
        if (queue.isEmpty()) {
            Toast.makeText(activity, "Sélectionnez d’abord un verset ou un passage.", Toast.LENGTH_LONG).show();
            return;
        }
        dialog = new Dialog(activity);
        LinearLayout shell = Ui.column(activity);
        shell.setPadding(Ui.dp(activity,16),Ui.dp(activity,12),Ui.dp(activity,16),Ui.dp(activity,12));
        title = Ui.text(activity,"Audio Al-Husary Muʿallim",17,true);
        title.setGravity(Gravity.CENTER_HORIZONTAL);shell.addView(title);

        LinearLayout row = Ui.row(activity);
        Button previous = Ui.smallButton(activity,"← Verset",v->previous());
        playPause = Ui.smallButton(activity,"Lire",v->toggle());
        Button next = Ui.smallButton(activity,"Verset →",v->next());
        Ui.weight(previous,1);Ui.weight(playPause,1);Ui.weight(next,1);
        row.addView(previous);row.addView(playPause);row.addView(next);shell.addView(row);

        LinearLayout second = Ui.row(activity);
        Button repeat = Ui.smallButton(activity,"Répéter",v->playCurrent(false));
        Button close = Ui.smallButton(activity,"Fermer",v->dialog.dismiss());
        Ui.weight(repeat,1);Ui.weight(close,1);second.addView(repeat);second.addView(close);shell.addView(second);

        dialog.setContentView(shell);
        dialog.setOnDismissListener(d->{release();mushaf.setAudioVerse(null);});
        dialog.show();
        Window w=dialog.getWindow();
        if(w!=null){w.setLayout(Math.min(activity.getResources().getDisplayMetrics().widthPixels-Ui.dp(activity,24),Ui.dp(activity,620)),ViewGroup.LayoutParams.WRAP_CONTENT);w.setWindowAnimations(0);}
        updateIdentity();
    }

    private void toggle() {
        if (player != null && player.isPlaying()) {
            player.pause();paused=true;playPause.setText("Reprendre");return;
        }
        if (player != null && paused) {
            player.start();paused=false;playPause.setText("Pause");return;
        }
        playCurrent(true);
    }

    private void previous(){if(index>0){index--;playCurrent(true);}}
    private void next(){if(index+1<queue.size()){index++;playCurrent(true);}}

    private void playCurrent(boolean allowAutoNext) {
        releasePlayerOnly();
        paused=false;
        VerseRef verse=queue.get(index);
        File file=pack.fileFor(verse);
        if(!file.isFile()){
            Toast.makeText(activity,"Audio manquant pour "+verse,Toast.LENGTH_LONG).show();return;
        }
        try {
            player=new MediaPlayer();
            player.setDataSource(file.getAbsolutePath());
            player.setOnCompletionListener(done->{
                releasePlayerOnly();
                playPause.setText("Lire");
                if(allowAutoNext && index+1<queue.size()){index++;playCurrent(true);}
            });
            player.prepare();
            mushaf.setAudioVerse(verse);
            updateIdentity();
            player.start();
            playPause.setText("Pause");
        } catch (Throwable error) {
            releasePlayerOnly();
            String message=error.getMessage();
            Toast.makeText(activity,"Lecture audio impossible : "+(message==null?error.getClass().getSimpleName():message),Toast.LENGTH_LONG).show();
        }
    }

    private void updateIdentity(){
        if(title==null||queue.isEmpty())return;
        VerseRef verse=queue.get(index);
        title.setText("Al-Husary Muʿallim · "+verse+" · "+(index+1)+"/"+queue.size());
        mushaf.setAudioVerse(verse);
    }

    private void releasePlayerOnly(){
        if(player!=null){try{player.stop();}catch(Throwable ignored){}player.release();player=null;}
        paused=false;
    }

    private void release(){releasePlayerOnly();}
}
