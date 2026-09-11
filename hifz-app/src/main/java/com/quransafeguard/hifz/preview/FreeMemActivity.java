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
    private android.content.SharedPreferences state;

    @Override protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        geometry = GeometryRepository.get(this);
        state = getSharedPreferences("free_mem_preview", MODE_PRIVATE);
        page = state.getInt("page", 1); count = state.getInt("count", 0); mask = state.getInt("mask", 0);
        start = parseOptional(state.getString("start", ""));
        end = parseOptional(state.getString("end", ""));

        LinearLayout root = Ui.column(this); root.setPadding(0,0,0,0);
        LinearLayout top = Ui.row(this); top.setPadding(Ui.dp(this,8),Ui.dp(this,4),Ui.dp(this,8),Ui.dp(this,4));
        top.addView(Ui.smallButton(this,"‹",v->finish()));
        title = Ui.text(this,"Mémorisation libre · "+page+" / 604",15,true); Ui.weight(title,1f); title.setGravity(Gravity.CENTER); top.addView(title); root.addView(top);

        selection = Ui.text(this,"",14,false); selection.setPadding(Ui.dp(this,12),4,Ui.dp(this,12),4); root.addView(selection);
        mushaf = new MushafView(this); mushaf.setListener(this); root.addView(mushaf,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1f));

        counter = Ui.text(this,"Répétitions manuelles : "+count,15,true); counter.setGravity(Gravity.CENTER); root.addView(counter);
        LinearLayout reps=Ui.row(this);
        Button minus=Ui.smallButton(this,"−1",v->{if(count>0)count--;save();counter.setText("Répétitions manuelles : "+count);mushaf.localCounterChanged();});
        Button plus=Ui.smallButton(this,"+1",v->{count++;save();counter.setText("Répétitions manuelles : "+count);mushaf.localCounterChanged();});
        Button reset=Ui.smallButton(this,"Remise à 0",v->{count=0;save();counter.setText("Répétitions manuelles : 0");mushaf.localCounterChanged();});
        Ui.weight(minus,1);Ui.weight(plus,1);Ui.weight(reset,1);reps.addView(minus);reps.addView(plus);reps.addView(reset);root.addView(reps);

        LinearLayout masks=Ui.row(this);
        for(int value:new int[]{0,25,50,75,100}){Button b=Ui.smallButton(this,value+"%",v->{mask=value;save();mushaf.setMask(mask);updateSelectionLabel();});Ui.weight(b,1);masks.addView(b);}root.addView(masks);
        LinearLayout nav=Ui.row(this);
        Button prev=Ui.smallButton(this,"‹ Page",v->go(-1));Button audio=Ui.smallButton(this,"Audio",v->Toast.makeText(this,"Al-Husary Muʿallim : corpus local à installer séparément. Aucun compteur Hifz n’est modifié.",Toast.LENGTH_LONG).show());Button next=Ui.smallButton(this,"Page ›",v->go(1));
        Ui.weight(prev,1);Ui.weight(audio,1);Ui.weight(next,1);nav.addView(prev);nav.addView(audio);nav.addView(next);root.addView(nav);
        setContentView(root);
        Ui.respectSystemBars(this, root, 0, 0, 0, 0);
        updateSelectionLabel();
    }

    private VerseRef parseOptional(String value){
        try { return value == null || value.isEmpty() ? null : GeometryRepository.parseVerse(value); }
        catch (RuntimeException ignored) { return null; }
    }

    private void go(int d){
        page=Math.max(1,Math.min(604,page+d));
        start=end=null; count=0; mask=0;
        save();
        counter.setText("Répétitions manuelles : 0");
        updateSelectionLabel();
        mushaf.show(page,Collections.emptyList(),Collections.emptyList(),0);
        title.setText("Mémorisation libre · "+page+" / 604");
    }

    private void save(){
        state.edit()
            .putInt("page",page)
            .putInt("count",count)
            .putInt("mask",mask)
            .putString("start",start==null?"":start.toString())
            .putString("end",end==null?"":end.toString())
            .apply();
    }

    private void updateSelectionLabel(){
        if(start==null||end==null) selection.setText("Touchez un verset pour commencer la sélection.");
        else selection.setText("Passage libre : "+start+" → "+end+" · masque "+mask+"%\nAucun curseur Sabqi / Itqān / Murājaʿah n’est modifié.");
    }

    @Override public void onVerseTap(VerseRef verse){
        if(start==null){start=end=verse;}
        else if(start.equals(end)){if(GeometryRepository.ordinal(verse)<GeometryRepository.ordinal(start)){end=start;start=verse;}else end=verse;}
        else {start=end=verse;count=0;counter.setText("Répétitions manuelles : 0");}
        List<VerseRef> refs=geometry.versesForRange(start,end);List<String> lines=geometry.lineIdsForVerseRange(start,end);
        mushaf.setSelection(refs,lines);mushaf.setMask(mask);
        updateSelectionLabel();save();
    }
    @Override public void onReady(){
        if(start!=null&&end!=null){
            List<VerseRef> refs=geometry.versesForRange(start,end);
            List<String> lines=geometry.lineIdsForVerseRange(start,end);
            mushaf.show(page,refs,lines,mask);
        } else mushaf.show(page,Collections.emptyList(),Collections.emptyList(),0);
    }
    @Override public void onError(String message){Toast.makeText(this,message,Toast.LENGTH_LONG).show();}
    @Override public void onPageShown(int shown){page=shown;title.setText("Mémorisation libre · "+shown+" / 604");}
    @Override public boolean onKeyDown(int code,KeyEvent e){if(code==KeyEvent.KEYCODE_PAGE_UP){go(-1);return true;}if(code==KeyEvent.KEYCODE_PAGE_DOWN){go(1);return true;}return super.onKeyDown(code,e);}
    @Override protected void onDestroy(){if(mushaf!=null)mushaf.destroySafely();super.onDestroy();}
}
