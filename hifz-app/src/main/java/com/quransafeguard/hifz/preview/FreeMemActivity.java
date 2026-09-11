package com.quransafeguard.hifz.preview;

import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.quransafeguard.hifz.core.VerseRef;

import java.util.Collections;
import java.util.List;

/** Free, punctual memorization. It never touches structured Hifz cursors. */
public final class FreeMemActivity extends android.app.Activity implements MushafView.Listener {
    private MushafView mushaf;
    private GeometryRepository geometry;
    private VerseRef start, end;
    private int page = 1;
    private int count = 0;
    private int mask = 0;
    private TextView title, selection, counter;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        geometry = GeometryRepository.get(this);
        android.content.SharedPreferences p = getSharedPreferences("free_mem_preview", MODE_PRIVATE);
        page = p.getInt("page", 1); count = p.getInt("count", 0); mask = p.getInt("mask", 0);

        LinearLayout root = Ui.column(this); root.setPadding(0,0,0,0);
        LinearLayout top = Ui.row(this); top.setPadding(Ui.dp(this,8),Ui.dp(this,4),Ui.dp(this,8),Ui.dp(this,4));
        top.addView(Ui.smallButton(this,"‹",v->finish()));
        title = Ui.text(this,"Mémorisation libre · "+page+" / 604",15,true); Ui.weight(title,1f); title.setGravity(Gravity.CENTER); top.addView(title); root.addView(top);

        selection = Ui.text(this,"Touchez un verset pour commencer la sélection.",14,false); selection.setPadding(Ui.dp(this,12),4,Ui.dp(this,12),4); root.addView(selection);
        mushaf = new MushafView(this); mushaf.setListener(this); root.addView(mushaf,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1f));

        counter = Ui.text(this,"Répétitions manuelles : "+count,15,true); counter.setGravity(Gravity.CENTER); root.addView(counter);
        LinearLayout reps=Ui.row(this);
        Button minus=Ui.smallButton(this,"−1",v->{if(count>0)count--;save();counter.setText("Répétitions manuelles : "+count);});
        Button plus=Ui.smallButton(this,"+1",v->{count++;save();counter.setText("Répétitions manuelles : "+count);mushaf.localCounterChanged();});
        Button reset=Ui.smallButton(this,"Remise à 0",v->{count=0;save();counter.setText("Répétitions manuelles : 0");});
        Ui.weight(minus,1);Ui.weight(plus,1);Ui.weight(reset,1);reps.addView(minus);reps.addView(plus);reps.addView(reset);root.addView(reps);

        LinearLayout masks=Ui.row(this);
        for(int value:new int[]{0,25,50,75,100}){Button b=Ui.smallButton(this,value+"%",v->{mask=value;save();mushaf.setMask(mask);});Ui.weight(b,1);masks.addView(b);}root.addView(masks);
        LinearLayout nav=Ui.row(this);
        Button prev=Ui.smallButton(this,"‹ Page",v->go(-1));Button audio=Ui.smallButton(this,"Audio",v->Toast.makeText(this,"Al-Husary Muʿallim : moteur prêt, corpus audio non activé tant que l’hébergement/redistribution n’est pas validé.",Toast.LENGTH_LONG).show());Button next=Ui.smallButton(this,"Page ›",v->go(1));
        Ui.weight(prev,1);Ui.weight(audio,1);Ui.weight(next,1);nav.addView(prev);nav.addView(audio);nav.addView(next);root.addView(nav);
        setContentView(root);
    }

    private void go(int d){page=Math.max(1,Math.min(604,page+d));save();start=end=null;selection.setText("Touchez un verset pour commencer la sélection.");mushaf.show(page,Collections.emptyList(),Collections.emptyList(),0);title.setText("Mémorisation libre · "+page+" / 604");}
    private void save(){getSharedPreferences("free_mem_preview",MODE_PRIVATE).edit().putInt("page",page).putInt("count",count).putInt("mask",mask).apply();}

    @Override public void onVerseTap(VerseRef verse){
        if(start==null){start=end=verse;}
        else if(start.equals(end)){if(GeometryRepository.ordinal(verse)<GeometryRepository.ordinal(start)){end=start;start=verse;}else end=verse;}
        else {start=end=verse;count=0;counter.setText("Répétitions manuelles : 0");}
        List<VerseRef> refs=geometry.versesForRange(start,end);List<String> lines=geometry.lineIdsForVerseRange(start,end);
        mushaf.show(page,refs,lines,mask);selection.setText("Passage libre : "+start+" → "+end+" · masque "+mask+"%\nAucun curseur Sabqi / Itqān / Murājaʿah n’est modifié.");save();
    }
    @Override public void onReady(){mushaf.show(page,Collections.emptyList(),Collections.emptyList(),0);}
    @Override public void onError(String message){Toast.makeText(this,message,Toast.LENGTH_LONG).show();}
    @Override public void onPageShown(int shown){title.setText("Mémorisation libre · "+shown+" / 604");}
    @Override public boolean onKeyDown(int code,KeyEvent e){if(code==KeyEvent.KEYCODE_PAGE_UP){go(-1);return true;}if(code==KeyEvent.KEYCODE_PAGE_DOWN){go(1);return true;}return super.onKeyDown(code,e);}
    @Override protected void onDestroy(){if(mushaf!=null)mushaf.destroySafely();super.onDestroy();}
}
