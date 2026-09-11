package com.quransafeguard.hifz.preview;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.SuperscriptSpan;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.quransafeguard.hifz.core.VerseRef;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Lecture/Etude. This is the only Hifz screen that exposes Tafsir. */
public final class StudyReaderActivity extends android.app.Activity implements MushafView.Listener {
    private static final float TAFSIR_MIN_SP = 16f;
    private static final float TAFSIR_MAX_SP = 26f;
    private static final float TAFSIR_DEFAULT_SP = 18f;
    private static final String TAFSIR_PREFS = "hifz_tafsir_reading";
    private static final String TAFSIR_FONT_KEY = "commentary_font_size_sp";
    private static final String TAFSIR_EDITION_KEY = "edition";

    private MushafView mushaf;
    private int page = 1;
    private VerseRef selected;
    private TextView pageLabel;
    private Button tafsirButton, audioButton;
    private LinearLayout topControls, bottomControls, rootRow, sideTafsir;
    private FrameLayout readerPane;
    private SeekBar pageSeek;
    private boolean controlsVisible = true;
    private boolean largeScreen;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Runnable autoHide = this::hideControls;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        page = getSharedPreferences("hifz_study", MODE_PRIVATE).getInt("page", 1);
        largeScreen = getResources().getConfiguration().smallestScreenWidthDp >= 600;

        rootRow = new LinearLayout(this);
        rootRow.setOrientation(LinearLayout.HORIZONTAL);
        rootRow.setBackgroundColor(Ui.PAPER);
        readerPane = new FrameLayout(this);
        readerPane.setBackgroundColor(Ui.PAPER);
        if (largeScreen) {
            rootRow.addView(readerPane,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.MATCH_PARENT,58f));
            sideTafsir = Ui.column(this);
            sideTafsir.setVisibility(View.GONE);
            rootRow.addView(sideTafsir,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.MATCH_PARENT,42f));
            setContentView(rootRow);
            Ui.respectSystemBars(this, rootRow, 0, 0, 0, 0);
        } else {
            setContentView(readerPane);
            Ui.respectSystemBars(this, readerPane, 0, 0, 0, 0);
        }

        mushaf = new MushafView(this); mushaf.setListener(this);
        readerPane.addView(mushaf, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        topControls = Ui.row(this);
        topControls.setBackgroundColor(Ui.PAPER);
        topControls.setPadding(Ui.dp(this,8),Ui.dp(this,4),Ui.dp(this,8),Ui.dp(this,4));
        Button back=Ui.smallButton(this,"‹ Retour",v->finish());
        pageLabel=Ui.text(this,"Lecture / Étude · "+page+" / 604",15,true);
        Ui.weight(pageLabel,1f);pageLabel.setGravity(Gravity.CENTER);
        topControls.addView(back);topControls.addView(pageLabel);
        FrameLayout.LayoutParams topLp=new FrameLayout.LayoutParams(overlayWidth(720),ViewGroup.LayoutParams.WRAP_CONTENT,Gravity.TOP|Gravity.CENTER_HORIZONTAL);
        topLp.setMargins(Ui.dp(this,8),Ui.dp(this,6),Ui.dp(this,8),0);readerPane.addView(topControls,topLp);

        bottomControls=Ui.column(this);
        bottomControls.setBackgroundColor(Ui.PAPER);
        bottomControls.setPadding(Ui.dp(this,8),Ui.dp(this,4),Ui.dp(this,8),Ui.dp(this,6));
        LinearLayout buttons=Ui.row(this);
        // Arabic-book direction: next canonical page is on the LEFT, previous on the RIGHT.
        Button next=Ui.smallButton(this,"Page suivante ›",v->go(1));
        tafsirButton=Ui.smallButton(this,"Tafsir",v->openTafsir());tafsirButton.setEnabled(false);
        Ui.weight(next,1);Ui.weight(tafsirButton,1);buttons.addView(next);buttons.addView(tafsirButton);
        if(new HifzAudioGate(this).available()){
            audioButton=Ui.smallButton(this,"Audio",v->openAudio());audioButton.setEnabled(false);Ui.weight(audioButton,1);buttons.addView(audioButton);
        }
        Button prev=Ui.smallButton(this,"‹ Page précédente",v->go(-1));Ui.weight(prev,1);buttons.addView(prev);
        bottomControls.addView(buttons);

        pageSeek=new SeekBar(this);pageSeek.setMax(603);pageSeek.setProgress(page-1);
        pageSeek.setLayoutDirection(View.LAYOUT_DIRECTION_RTL); // page 1 visually right, page 604 left
        pageSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            @Override public void onProgressChanged(SeekBar seekBar,int progress,boolean fromUser){if(fromUser)pageLabel.setText("Lecture / Étude · "+(progress+1)+" / 604");}
            @Override public void onStartTrackingTouch(SeekBar seekBar){showControls();}
            @Override public void onStopTrackingTouch(SeekBar seekBar){setPage(seekBar.getProgress()+1);}
        });
        bottomControls.addView(pageSeek,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        FrameLayout.LayoutParams bottomLp=new FrameLayout.LayoutParams(overlayWidth(760),ViewGroup.LayoutParams.WRAP_CONTENT,Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);
        bottomLp.setMargins(Ui.dp(this,8),0,Ui.dp(this,8),Ui.dp(this,6));readerPane.addView(bottomControls,bottomLp);
        scheduleAutoHide();
    }

    private int overlayWidth(int maxDp){
        int screen=getResources().getDisplayMetrics().widthPixels;
        int usable=largeScreen?Math.round(screen*.58f):screen;
        return Math.max(Ui.dp(this,280),Math.min(usable-Ui.dp(this,16),Ui.dp(this,maxDp)));
    }

    private void setPage(int requested){
        int next=Math.max(1,Math.min(604,requested));if(next==page){showControls();return;}
        closeSideTafsir();
        page=next;selected=null;tafsirButton.setEnabled(false);tafsirButton.setText("Tafsir");if(audioButton!=null)audioButton.setEnabled(false);
        getSharedPreferences("hifz_study",MODE_PRIVATE).edit().putInt("page",page).apply();
        pageLabel.setText("Lecture / Étude · "+page+" / 604");pageSeek.setProgress(page-1);
        mushaf.show(page,Collections.emptyList(),Collections.emptyList(),0);showControls();
    }
    private void go(int delta){setPage(page+delta);}

    @Override public void onVerseTap(VerseRef verse){
        selected=verse;tafsirButton.setEnabled(true);tafsirButton.setText("Tafsir "+verse.getSurah()+":"+verse.getAyah());
        if(audioButton!=null)audioButton.setEnabled(true);
        mushaf.setSelection(Collections.singletonList(verse),Collections.emptyList());showControls();
    }
    @Override public void onPageSwipe(int delta){go(delta);}
    @Override public void onSurfaceTap(){if(controlsVisible)hideControls();else showControls();}

    private void showControls(){controlsVisible=true;topControls.setVisibility(View.VISIBLE);bottomControls.setVisibility(View.VISIBLE);scheduleAutoHide();}
    private void hideControls(){
        if(topControls==null||bottomControls==null)return;controlsVisible=false;topControls.setVisibility(View.GONE);bottomControls.setVisibility(View.GONE);
        if(mushaf!=null)mushaf.removeCallbacks(autoHide);
    }
    private void scheduleAutoHide(){if(mushaf==null)return;mushaf.removeCallbacks(autoHide);if(selected!=null)return;mushaf.postDelayed(autoHide,3200L);}

    private void openAudio(){
        List<VerseRef> verses=selected==null?Collections.emptyList():Collections.singletonList(selected);
        new HifzAudioDialog(this,mushaf,verses).show();
    }

    private void openTafsir(){
        final VerseRef verse=selected;if(verse==null)return;
        if(largeScreen){openSideTafsir(verse);return;}
        hideControls();mushaf.revealSelectionAboveBottomPanel();
        final Dialog dialog=new Dialog(this);
        LinearLayout shell=buildTafsirPanel(verse,dialog::dismiss);
        dialog.setContentView(shell);dialog.setCanceledOnTouchOutside(true);
        dialog.setOnDismissListener(closed->{mushaf.clearReveal();showControls();});
        dialog.show();Window w=dialog.getWindow();
        if(w!=null){
            w.setGravity(Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);w.setWindowAnimations(0);w.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            View content=findViewById(android.R.id.content);int availableHeight=content==null?getResources().getDisplayMetrics().heightPixels:content.getHeight();
            w.setLayout(Math.min(getResources().getDisplayMetrics().widthPixels-Ui.dp(this,16),Ui.dp(this,760)),Math.max(Ui.dp(this,220),Math.round(availableHeight*.50f)));
        }
    }

    private void openSideTafsir(VerseRef verse){
        sideTafsir.removeAllViews();
        LinearLayout shell=buildTafsirPanel(verse,this::closeSideTafsir);
        sideTafsir.addView(shell,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));
        sideTafsir.setVisibility(View.VISIBLE);showControls();
    }
    private void closeSideTafsir(){if(sideTafsir!=null){sideTafsir.removeAllViews();sideTafsir.setVisibility(View.GONE);}}

    private LinearLayout buildTafsirPanel(VerseRef verse,Runnable closeAction){
        LinearLayout shell=Ui.column(this);shell.setPadding(Ui.dp(this,14),Ui.dp(this,8),Ui.dp(this,14),Ui.dp(this,10));
        TextView title=Ui.text(this,"Tafsir · "+verse.getSurah()+":"+verse.getAyah(),17,true);shell.addView(title);
        TextView source=Ui.text(this,"Chargement des éditions vérifiées…",12,false);source.setTextColor(Ui.MUTED);source.setPadding(0,0,0,Ui.dp(this,4));shell.addView(source);
        LinearLayout editionRow=Ui.row(this);editionRow.setVisibility(View.GONE);shell.addView(editionRow);

        SharedPreferences readingPrefs=getSharedPreferences(TAFSIR_PREFS,MODE_PRIVATE);
        final float[] fontSize={Math.max(TAFSIR_MIN_SP,Math.min(TAFSIR_MAX_SP,readingPrefs.getFloat(TAFSIR_FONT_KEY,TAFSIR_DEFAULT_SP)))};
        final TafsirRepository.Entry[] loaded={null};
        final MultiTafsirRepository.Edition[] currentEdition={MultiTafsirRepository.Edition.fromStorage(readingPrefs.getString(TAFSIR_EDITION_KEY,"jalalayn"))};
        final LinearLayout textColumn=Ui.column(this);textColumn.setPadding(0,0,0,0);

        LinearLayout controls=Ui.row(this);
        Button minus=Ui.smallButton(this,"A−",v->{fontSize[0]=Math.max(TAFSIR_MIN_SP,fontSize[0]-2f);readingPrefs.edit().putFloat(TAFSIR_FONT_KEY,fontSize[0]).apply();if(loaded[0]!=null)renderTafsir(textColumn,loaded[0],fontSize[0]);});
        Button plus=Ui.smallButton(this,"A+",v->{fontSize[0]=Math.min(TAFSIR_MAX_SP,fontSize[0]+2f);readingPrefs.edit().putFloat(TAFSIR_FONT_KEY,fontSize[0]).apply();if(loaded[0]!=null)renderTafsir(textColumn,loaded[0],fontSize[0]);});
        Button close=Ui.smallButton(this,"Fermer",v->closeAction.run());Ui.weight(minus,1);Ui.weight(plus,1);Ui.weight(close,1);controls.addView(minus);controls.addView(plus);controls.addView(close);shell.addView(controls);
        ScrollView scroll=new ScrollView(this);textColumn.addView(tafsirText("Chargement…",fontSize[0],false));scroll.addView(textColumn);shell.addView(scroll,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1f));

        io.execute(()->{
            try{
                Map<MultiTafsirRepository.Edition,TafsirRepository.Entry> available=new MultiTafsirRepository(this).loadAvailable(verse);
                runOnUiThread(()->{
                    textColumn.removeAllViews();editionRow.removeAllViews();
                    if(available.isEmpty()){
                        source.setText("Aucune édition disponible pour ce verset.");
                        textColumn.addView(tafsirText("Aucun commentaire vérifié n’est disponible pour ce verset.",fontSize[0],false));return;
                    }
                    MultiTafsirRepository.Edition resolved=currentEdition[0];
                    if(!available.containsKey(resolved))resolved=available.containsKey(MultiTafsirRepository.Edition.JALALAYN)?MultiTafsirRepository.Edition.JALALAYN:available.keySet().iterator().next();
                    currentEdition[0]=resolved;
                    if(available.size()>1){
                        editionRow.setVisibility(View.VISIBLE);
                        for(MultiTafsirRepository.Edition edition:available.keySet()){
                            Button editionButton=Ui.smallButton(this,edition.displayName,v->{
                                ViewGroup row=(ViewGroup)v.getParent();for(int i=0;i<row.getChildCount();i++){View child=row.getChildAt(i);if(child instanceof Button)Ui.setChosen((Button)child,child==v);}
                                currentEdition[0]=edition;readingPrefs.edit().putString(TAFSIR_EDITION_KEY,edition.storageValue).apply();
                                TafsirRepository.Entry entry=available.get(edition);loaded[0]=entry;applyTafsirIdentity(title,source,verse,entry);renderTafsir(textColumn,entry,fontSize[0]);
                            });
                            Ui.setChosen(editionButton,edition==currentEdition[0]);Ui.weight(editionButton,1);editionRow.addView(editionButton);
                        }
                    }else editionRow.setVisibility(View.GONE);
                    TafsirRepository.Entry entry=available.get(currentEdition[0]);loaded[0]=entry;readingPrefs.edit().putString(TAFSIR_EDITION_KEY,currentEdition[0].storageValue).apply();
                    applyTafsirIdentity(title,source,verse,entry);renderTafsir(textColumn,entry,fontSize[0]);
                });
            }catch(Throwable error){runOnUiThread(()->{source.setText("Tafsir local");textColumn.removeAllViews();textColumn.addView(tafsirText("Tafsir indisponible : "+safeMessage(error),fontSize[0],false));});}
        });
        return shell;
    }

    private void applyTafsirIdentity(TextView title,TextView source,VerseRef verse,TafsirRepository.Entry entry){
        title.setText(entry.editionName+" · "+verse.getSurah()+":"+verse.getAyah());String metadata=entry.metadataLine();source.setText(metadata.isEmpty()?entry.editionName:metadata);
    }
    private static String safeMessage(Throwable error){String message=error.getMessage();return message==null||message.trim().isEmpty()?error.getClass().getSimpleName():message;}

    private void renderTafsir(LinearLayout target,TafsirRepository.Entry entry,float fontSp){
        target.removeAllViews();addRunBlocks(target,entry.commentaryRuns,fontSp,false);
        if(!entry.notes.isEmpty()){
            Button notesToggle=Ui.smallButton(this,"Notes ("+entry.notes.size()+")",null);LinearLayout notes=Ui.column(this);notes.setPadding(0,0,0,0);notes.setVisibility(View.GONE);
            notesToggle.setOnClickListener(v->notes.setVisibility(notes.getVisibility()==View.VISIBLE?View.GONE:View.VISIBLE));target.addView(notesToggle);
            for(TafsirRepository.Note note:entry.notes){TextView label=tafsirText(Integer.toString(note.number)+".",fontSp,true);label.setPadding(0,Ui.dp(this,6),0,0);notes.addView(label);addRunBlocks(notes,note.runs,Math.max(TAFSIR_MIN_SP,fontSp-1f),true);}target.addView(notes);
        }
    }
    private void addRunBlocks(LinearLayout target,List<TafsirRepository.Run> runs,float fontSp,boolean note){
        ArrayList<TafsirRepository.Run> block=new ArrayList<>();Boolean poetry=null;
        for(TafsirRepository.Run run:runs){boolean isPoetry=run.style==TafsirRepository.RunStyle.POETRY;if(poetry!=null&&poetry!=isPoetry){addRunBlock(target,block,fontSp,note,poetry);block.clear();}poetry=isPoetry;block.add(run);}
        if(!block.isEmpty())addRunBlock(target,block,fontSp,note,Boolean.TRUE.equals(poetry));
    }
    private void addRunBlock(LinearLayout target,List<TafsirRepository.Run> runs,float fontSp,boolean note,boolean poetry){
        SpannableStringBuilder text=new SpannableStringBuilder();
        for(TafsirRepository.Run run:runs){
            String value=poetry?run.text:run.text.replaceAll("\\n{2,}","\n");int start=text.length();text.append(value);int end=text.length();if(end<=start)continue;
            switch(run.style){
                case ITALIC:case TRANSLITERATION:case POETRY:text.setSpan(new StyleSpan(Typeface.ITALIC),start,end,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);break;
                case BOLD:case TECHNICAL_TERM:text.setSpan(new StyleSpan(Typeface.BOLD),start,end,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);break;
                case BOLD_ITALIC:text.setSpan(new StyleSpan(Typeface.BOLD_ITALIC),start,end,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);break;
                case NOTE_REF:text.setSpan(new SuperscriptSpan(),start,end,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);text.setSpan(new RelativeSizeSpan(.78f),start,end,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);break;
                default:break;
            }
        }
        TextView view=tafsirText(text,fontSp,false);view.setLineSpacing(0f,poetry?1.35f:(note?1.30f:1.25f));
        if(poetry){view.setGravity(Gravity.START);view.setPadding(Ui.dp(this,12),0,Ui.dp(this,4),Ui.dp(this,4));}
        target.addView(view);
    }
    private TextView tafsirText(CharSequence value,float sp,boolean bold){TextView text=Ui.text(this,value.toString(),sp,bold);text.setText(value);text.setTextIsSelectable(false);return text;}

    @Override public void onReady(){mushaf.show(page,Collections.emptyList(),Collections.emptyList(),0);}
    @Override public void onError(String message){Toast.makeText(this,message,Toast.LENGTH_LONG).show();}
    @Override public void onPageShown(int shown){page=shown;pageLabel.setText("Lecture / Étude · "+shown+" / 604");pageSeek.setProgress(shown-1);}
    @Override public boolean onKeyDown(int keyCode,KeyEvent event){if(keyCode==KeyEvent.KEYCODE_PAGE_UP){go(-1);return true;}if(keyCode==KeyEvent.KEYCODE_PAGE_DOWN){go(1);return true;}return super.onKeyDown(keyCode,event);}
    @Override protected void onDestroy(){io.shutdownNow();if(mushaf!=null){mushaf.removeCallbacks(autoHide);mushaf.destroySafely();}super.onDestroy();}
}
