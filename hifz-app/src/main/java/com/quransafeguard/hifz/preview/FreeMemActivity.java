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
    private TextView selection, counter;
    private LinearLayout audioHost;
    private HifzAudioDialog audioPlayer;
    private android.content.SharedPreferences state;
    private final List<Button> maskButtons = new ArrayList<>();
    private Button resetButton;

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
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.addView(Ui.iconButton(this,"‹","Retour",v->finish()));
        // Two-line header (spec 23.8): title, then "Passage … · masque X %".
        LinearLayout titles=Ui.column(this);titles.setPadding(0,0,0,0);titles.setGravity(Gravity.CENTER);Ui.weight(titles,1f);
        TextView title=Ui.bookText(this,"Mémorisation libre",15f,true);title.setGravity(Gravity.CENTER);titles.addView(title);
        selection = Ui.text(this,"",11.5f,false); selection.setTextColor(Ui.MUTED); selection.setGravity(Gravity.CENTER); selection.setSingleLine(true); titles.addView(selection);
        top.addView(titles);
        top.addView(new View(this),new LinearLayout.LayoutParams(Ui.dp(this,48),Ui.dp(this,48)));
        root.addView(top);

        audioHost = Ui.column(this); audioHost.setPadding(0,0,0,0); audioHost.setVisibility(View.GONE);
        root.addView(audioHost,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));

        mushaf = new MushafView(this); mushaf.setListener(this); root.addView(mushaf,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1f));

        // "−  12  +" with reset only once there is something to reset (spec 12).
        LinearLayout reps=Ui.row(this);reps.setGravity(Gravity.CENTER);
        Button decrement=Ui.iconButton(this,"−","Retirer une répétition",v->{if(count==0)return;count--;save();updateCounter();mushaf.localCounterChanged();});
        Ui.setButtonIcon(decrement,R.drawable.ic_ui_remove);reps.addView(decrement);
        counter = Ui.bookText(this,"0",17,true); counter.setGravity(Gravity.CENTER); counter.setContentDescription("Répétitions");
        reps.addView(counter,new LinearLayout.LayoutParams(Ui.dp(this,56),Ui.dp(this,48)));
        Button increment=Ui.iconButton(this,"+","Ajouter une répétition",v->{count++;save();updateCounter();mushaf.localCounterChanged();});
        Ui.setButtonIcon(increment,R.drawable.ic_ui_add);reps.addView(increment);
        resetButton=Ui.iconButton(this,"↺","Remettre à zéro",v->{count=0;save();updateCounter();mushaf.localCounterChanged();});
        reps.addView(resetButton);
        root.addView(reps);

        // Compact segmented mask selector "0 | 25 | 50 | 75 | 100": plain figures, no boxes.
        LinearLayout masks=Ui.row(this);masks.setGravity(Gravity.CENTER);
        int[] levels={0,25,50,75,100};
        for(int k=0;k<levels.length;k++){
            int value=levels[k];
            if(k>0){TextView bar=Ui.text(this,"|",14,false);bar.setTextColor(Ui.LINE);masks.addView(bar);}
            Button b=new Button(this);b.setAllCaps(false);b.setText(String.valueOf(value));b.setTextSize(14f);
            b.setBackgroundColor(android.graphics.Color.TRANSPARENT);b.setStateListAnimator(null);b.setElevation(0f);
            b.setMinWidth(Ui.dp(this,44));b.setMinimumWidth(Ui.dp(this,44));b.setMinHeight(Ui.dp(this,40));b.setMinimumHeight(Ui.dp(this,40));
            b.setPadding(Ui.dp(this,6),0,Ui.dp(this,6),0);b.setContentDescription("Masque "+value+" %");
            b.setOnClickListener(v->{mask=value;save();mushaf.setMask(mask);updateSelectionLabel();updateMaskButtons();});
            b.setTag(value);maskButtons.add(b);masks.addView(b,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,Ui.dp(this,40)));
        }
        root.addView(masks);

        // Explicit RTL navigation grammar, matching the main reader.
        LinearLayout nav=Ui.row(this);nav.setGravity(Gravity.CENTER);nav.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        nav.addView(Ui.iconButton(this,"›","Page suivante",v->go(1)));
        if (new HifzAudioGate(this).installed()) nav.addView(Ui.iconButton(this,"♪","Audio",v->openAudio()));
        nav.addView(Ui.iconButton(this,"‹","Page précédente",v->go(-1)));
        nav.addView(Ui.iconButton(this,"","Sourate",v->showSurahPicker()));
        nav.addView(Ui.iconButton(this,"","Hizb",v->showRubPicker()));
        root.addView(nav);
        setContentView(root);
        Ui.respectSystemBars(this, root, 0, 0, 0, 0);
        updateSelectionLabel();
        updateMaskButtons();
        updateCounter();
    }

    private void updateCounter(){
        counter.setText(String.valueOf(count));
        resetButton.setVisibility(count>0?View.VISIBLE:View.INVISIBLE);
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
        updateCounter();
        updateSelectionLabel();updateMaskButtons();
        mushaf.show(page,Collections.emptyList(),Collections.emptyList(),0);
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
        else selection.setText("Passage "+QuranSurahNames.range(start,end)+" · masque "+mask+" %");
    }

    private void updateMaskButtons() {
        boolean hasSelection = start != null && end != null;
        for (Button button : maskButtons) {
            int value = (Integer) button.getTag();
            boolean chosen = hasSelection && value == mask;
            button.setEnabled(hasSelection);
            button.setTextColor(!hasSelection ? Ui.LINE : chosen ? Ui.INK : Ui.MUTED);
            button.setTypeface(android.graphics.Typeface.SERIF, chosen ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
            button.setPaintFlags(chosen ? (button.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG)
                : (button.getPaintFlags() & ~android.graphics.Paint.UNDERLINE_TEXT_FLAG));
        }
    }

    @Override public void onVerseTap(VerseRef verse){
        closeAudio();
        if(start==null){start=end=verse;}
        else if(start.equals(end)){if(GeometryRepository.ordinal(verse)<GeometryRepository.ordinal(start)){end=start;start=verse;}else end=verse;}
        else {start=end=verse;count=0;updateCounter();}
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
    @Override public void onPageShown(int shown){page=shown;}
    @Override public boolean onKeyDown(int code,KeyEvent e){if(mushaf==null)return super.onKeyDown(code,e);if(code==KeyEvent.KEYCODE_PAGE_UP){go(-1);return true;}if(code==KeyEvent.KEYCODE_PAGE_DOWN){go(1);return true;}return super.onKeyDown(code,e);}
    @Override protected void onDestroy(){closeAudio();if(mushaf!=null)mushaf.destroySafely();super.onDestroy();}
}
