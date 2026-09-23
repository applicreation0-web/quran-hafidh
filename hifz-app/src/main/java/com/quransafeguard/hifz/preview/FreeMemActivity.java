package com.quransafeguard.hifz.preview;

import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.quransafeguard.hifz.core.VerseRef;

import java.util.ArrayList;
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
    private LinearLayout audioHost;
    private HifzAudioDialog audioPlayer;
    private android.content.SharedPreferences state;
    private final List<Button> maskButtons = new ArrayList<>();

    @Override protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        try {
            geometry = GeometryRepository.get(this);
        } catch (Throwable error) {
            Ui.showFatal(this, "La géométrie du Mushaf est indisponible. Fermez puis rouvrez l’application.");
            return;
        }
        state = getSharedPreferences("free_mem_preview", MODE_PRIVATE);
        page = state.getInt("page", 1); count = state.getInt("count", 0); mask = state.getInt("mask", 0);
        start = parseOptional(state.getString("start", ""));
        end = parseOptional(state.getString("end", ""));

        LinearLayout root = Ui.column(this); root.setPadding(0,0,0,0);
        LinearLayout top = Ui.row(this); top.setPadding(Ui.dp(this,4),0,Ui.dp(this,4),0);
        top.addView(Ui.iconButton(this,"‹","Retour",v->finish()));
        title = Ui.text(this,"Mémorisation libre · "+page+" / 604",13,true); Ui.weight(title,1f); title.setGravity(Gravity.CENTER); top.addView(title);
        TextView balance=Ui.text(this,"",1,false);balance.setMinWidth(Ui.dp(this,44));top.addView(balance,new LinearLayout.LayoutParams(Ui.dp(this,44),Ui.dp(this,44)));
        root.addView(top);

        audioHost = Ui.column(this); audioHost.setPadding(0,0,0,0); audioHost.setVisibility(View.GONE);
        root.addView(audioHost,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));

        selection = Ui.text(this,"",11.5f,false); selection.setTextColor(Ui.MUTED); selection.setPadding(Ui.dp(this,10),0,Ui.dp(this,10),Ui.dp(this,2)); root.addView(selection);
        mushaf = new MushafView(this); mushaf.setListener(this); root.addView(mushaf,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1f));

        counter = Ui.text(this,"Répétitions · "+count,12,true); counter.setGravity(Gravity.CENTER); root.addView(counter);
        LinearLayout reps=Ui.row(this);reps.setGravity(Gravity.CENTER);
        Button decrement=Ui.iconButton(this,"−","Retirer une répétition",v->{if(count==0)return;count--;save();counter.setText("Répétitions · "+count);mushaf.localCounterChanged();});
        Ui.setButtonIcon(decrement,R.drawable.ic_ui_remove);reps.addView(decrement);
        Button increment=Ui.iconButton(this,"+","Ajouter une répétition",v->{count++;save();counter.setText("Répétitions · "+count);mushaf.localCounterChanged();});
        Ui.setButtonIcon(increment,R.drawable.ic_ui_add);reps.addView(increment);
        reps.addView(Ui.iconButton(this,"↺","Remettre à zéro",v->{count=0;save();counter.setText("Répétitions · 0");mushaf.localCounterChanged();}));
        root.addView(reps);

        LinearLayout masks=Ui.row(this);masks.setGravity(Gravity.CENTER);
        for(int value:new int[]{0,25,50,75,100}){
            Button b=Ui.smallButton(this,value+"%",v->{mask=value;save();mushaf.setMask(mask);updateSelectionLabel();updateMaskButtons();});
            b.setTag(value);maskButtons.add(b);Ui.weight(b,1);masks.addView(b);
        }
        root.addView(masks);

        // Explicit RTL navigation grammar, matching the main reader.
        LinearLayout nav=Ui.row(this);nav.setGravity(Gravity.CENTER);nav.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        nav.addView(Ui.iconButton(this,"›","Page suivante",v->go(1)));
        if (new HifzAudioGate(this).available()) nav.addView(Ui.iconButton(this,"♪","Audio",v->openAudio()));
        nav.addView(Ui.iconButton(this,"‹","Page précédente",v->go(-1)));
        nav.addView(Ui.iconButton(this,"","Sourate",v->showSurahPicker()));
        nav.addView(Ui.iconButton(this,"","Hizb",v->showRubPicker()));
        root.addView(nav);
        setContentView(root);
        Ui.respectSystemBars(this, root, 0, 0, 0, 0);
        updateSelectionLabel();
        updateMaskButtons();
    }

    private VerseRef parseOptional(String value){
        try { return value == null || value.isEmpty() ? null : GeometryRepository.parseVerse(value); }
        catch (RuntimeException ignored) { return null; }
    }

    private void go(int d){ setPage(Math.max(1,Math.min(604,page+d))); }

    private void showSurahPicker(){ QuranSurahNames.showPicker(this,geometry,this::setPage); }

    private void showRubPicker(){ QuranRubNames.showPicker(this,this::setPage); }

    private void setPage(int requested){
        closeAudio();
        page=requested;
        start=end=null; count=0; mask=0;
        save();
        counter.setText("Répétitions · 0");
        updateSelectionLabel();updateMaskButtons();
        mushaf.show(page,Collections.emptyList(),Collections.emptyList(),0);
        title.setText("Mémorisation libre · "+page+" / 604");
    }

    private void openAudio(){
        List<VerseRef> refs=(start!=null&&end!=null)?geometry.versesForRange(start,end):Collections.emptyList();
        closeAudio();
        audioPlayer=new HifzAudioDialog(this,mushaf,refs);
        audioPlayer.attachInline(audioHost);
    }

    private void closeAudio(){
        HifzAudioDialog current=audioPlayer;audioPlayer=null;if(current!=null)current.detachInline();
    }

    private void save(){
        state.edit().putInt("page",page).putInt("count",count).putInt("mask",mask)
            .putString("start",start==null?"":start.toString()).putString("end",end==null?"":end.toString()).apply();
    }

    private void updateSelectionLabel(){
        if(start==null||end==null) selection.setText("Touchez un verset pour choisir le passage.");
        else selection.setText("Passage · "+start+" → "+end+" · masque "+mask+"% · indépendant du Parcours Hifz");
    }

    private void updateMaskButtons() {
        boolean hasSelection = start != null && end != null;
        for (Button button : maskButtons) {
            int value = (Integer) button.getTag();
            button.setEnabled(hasSelection);Ui.setChosen(button, hasSelection && value == mask);
        }
    }

    @Override public void onVerseTap(VerseRef verse){
        closeAudio();
        if(start==null){start=end=verse;}
        else if(start.equals(end)){if(GeometryRepository.ordinal(verse)<GeometryRepository.ordinal(start)){end=start;start=verse;}else end=verse;}
        else {start=end=verse;count=0;counter.setText("Répétitions · 0");}
        List<VerseRef> refs=geometry.versesForRange(start,end);List<String> lines=geometry.lineIdsForVerseRange(start,end);
        mushaf.setSelection(refs,lines);mushaf.setMask(mask);updateSelectionLabel();updateMaskButtons();save();
    }
    @Override public void onPageSwipe(int delta){go(delta);}
    @Override public void onReady(){
        if(start!=null&&end!=null){
            List<VerseRef> refs=geometry.versesForRange(start,end);List<String> lines=geometry.lineIdsForVerseRange(start,end);
            mushaf.show(page,refs,lines,mask);
        } else mushaf.show(page,Collections.emptyList(),Collections.emptyList(),0);
    }
    @Override public void onError(String message){Toast.makeText(this,message,Toast.LENGTH_LONG).show();}
    @Override public void onPageShown(int shown){page=shown;title.setText("Mémorisation libre · "+shown+" / 604");}
    @Override public boolean onKeyDown(int code,KeyEvent e){if(mushaf==null)return super.onKeyDown(code,e);if(code==KeyEvent.KEYCODE_PAGE_UP){go(-1);return true;}if(code==KeyEvent.KEYCODE_PAGE_DOWN){go(1);return true;}return super.onKeyDown(code,e);}
    @Override protected void onDestroy(){closeAudio();if(mushaf!=null)mushaf.destroySafely();super.onDestroy();}
}
