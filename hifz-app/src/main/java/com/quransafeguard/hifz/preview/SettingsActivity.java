package com.quransafeguard.hifz.preview;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.quransafeguard.hifz.core.QuranCanon;
import com.quransafeguard.hifz.core.VerseRange;
import com.quransafeguard.hifz.core.VerseRef;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** BOOX-oriented configuration. Cursor changes are always explicit. */
public final class SettingsActivity extends android.app.Activity {
    private static final int REQUEST_AUDIO_ZIP = 4103;
    private HifzPrefs prefs;
    private GeometryRepository geometry;
    private LinearLayout rangesBox;
    private TextView sabqiStatus, itqanStatus, murajaahStatus, audioStatus;
    private Button sabqiStartButton, sabqiEndButton, rotationButton;

    private interface VerseChosen { void accept(VerseRef verse); }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new HifzPrefs(this);
        geometry = GeometryRepository.get(this);

        ScrollView scroll = new ScrollView(this);scroll.setFillViewport(true);
        LinearLayout root = Ui.column(this);scroll.addView(root);
        LinearLayout top=Ui.row(this);top.addView(Ui.roundButton(this,"","Retour",v->finish()));
        TextView title=Ui.bookText(this,"Paramètres Hifz",20,true);Ui.weight(title,1);title.setGravity(Gravity.CENTER);top.addView(title);root.addView(top);

        section(root,"Parcours");
        root.addView(Ui.text(this,"Sabqi · 5 lignes · 37   |   Itqān · ×"+PreviewConfig.ITQAN_TOTAL_REPS+"\nMurājaʿah · 30 min récent + 30 min Itqān",12.5f,false));

        sabqiStatus=Ui.text(this,"",12.5f,false);sabqiStatus.setPadding(0,Ui.dp(this,7),0,Ui.dp(this,3));root.addView(sabqiStatus);
        LinearLayout sabqiButtons=Ui.row(this);sabqiButtons.setGravity(Gravity.CENTER);
        sabqiStartButton=Ui.smallButton(this,"Début",v->chooseVerse("Début Sabqi",prefs.sabqiStart(),verse->setSabqiBound(true,verse)));
        sabqiEndButton=Ui.smallButton(this,"Fin",v->chooseVerse("Fin Sabqi",prefs.sabqiEnd(),verse->setSabqiBound(false,verse)));
        LinearLayout.LayoutParams compact=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT);
        compact.setMargins(Ui.dp(this,4),Ui.dp(this,2),Ui.dp(this,4),Ui.dp(this,2));
        sabqiButtons.addView(sabqiStartButton,new LinearLayout.LayoutParams(compact));
        sabqiButtons.addView(sabqiEndButton,new LinearLayout.LayoutParams(compact));
        root.addView(sabqiButtons);

        section(root,"Itqān · plages");
        rangesBox=Ui.column(this);rangesBox.setPadding(0,Ui.dp(this,2),0,0);root.addView(rangesBox);
        LinearLayout addRow=Ui.row(this);addRow.setGravity(Gravity.CENTER);addRow.addView(Ui.smallButton(this,"Ajouter",v->chooseRange(null,-1)));root.addView(addRow);
        itqanStatus=Ui.text(this,"",11.5f,false);itqanStatus.setPadding(0,Ui.dp(this,4),0,Ui.dp(this,2));root.addView(itqanStatus);
        LinearLayout rotationRow=Ui.row(this);rotationRow.setGravity(Gravity.CENTER);
        rotationButton=Ui.smallButton(this,"Rotation",v->chooseVerse("Début de rotation Itqān",prefs.itqanRotationStart(),this::setRotationStart));
        rotationRow.addView(rotationButton);
        rotationRow.addView(Ui.smallButton(this,"Repositionner Murājaʿah",v->confirmMurajaahReposition()));root.addView(rotationRow);

        section(root,"Murājaʿah");
        murajaahStatus=Ui.text(this,"",12.5f,false);root.addView(murajaahStatus);

        section(root,"Audio Al-Husary Muʿallim");
        audioStatus=Ui.text(this,"",12.5f,false);root.addView(audioStatus);
        LinearLayout audioActions=Ui.row(this);audioActions.setGravity(Gravity.CENTER);
        audioActions.addView(Ui.smallButton(this,"Choisir le pack",v->selectAudioZip()));root.addView(audioActions);

        section(root,"Affichage");
        Switch eink=new Switch(this);eink.setText("Optimisation E‑Ink / BOOX");eink.setChecked(prefs.forceEink());eink.setOnCheckedChangeListener((button,checked)->prefs.setForceEink(checked));root.addView(eink);

        section(root,"Avancé");
        LinearLayout diagnostics=Ui.row(this);diagnostics.setGravity(Gravity.CENTER);
        diagnostics.addView(Ui.smallButton(this,"Diagnostic",v->showDiagnostic()));
        diagnostics.addView(Ui.smallButton(this,"Réinitialiser",v->confirmReset()));root.addView(diagnostics);

        setContentView(scroll);int inset=Ui.dp(this,12);Ui.respectSystemBars(this,root,inset,inset,inset,inset);
        refreshAll();
    }

    private void section(LinearLayout root,String title){
        TextView view=Ui.bookText(this,title,16,true);view.setPadding(0,Ui.dp(this,14),0,Ui.dp(this,4));root.addView(view);
    }

    private void refreshAll(){refreshSabqi();refreshRanges();refreshItqan();refreshMurajaah();refreshAudio();}

    private void refreshSabqi(){
        int first=geometry.firstLineIndex(prefs.sabqiStart()),last=geometry.lastLineIndex(prefs.sabqiEnd());
        int total=last-first+1;
        boolean aligned=total>0&&total%PreviewConfig.SABQI_LINES==0;
        sabqiStatus.setText("Sabqi · "+total+" lignes"+(aligned?"":" · ⚠ fin à ajuster"));
        sabqiStartButton.setText("Début · "+prefs.sabqiStart());
        sabqiEndButton.setText("Fin · "+prefs.sabqiEnd());
    }

    private void setSabqiBound(boolean isStart,VerseRef verse){
        VerseRef newStart=isStart?verse:prefs.sabqiStart(),newEnd=isStart?prefs.sabqiEnd():verse;
        if(GeometryRepository.ordinal(newStart)>GeometryRepository.ordinal(newEnd)){Toast.makeText(this,"Le début doit précéder la fin Sabqi.",Toast.LENGTH_LONG).show();return;}
        if(isStart)prefs.setSabqiStart(verse);else prefs.setSabqiEnd(verse);
        int low=geometry.firstLineIndex(newStart),high=geometry.lastLineIndex(newEnd),cursor=prefs.sabqiLineCursor();
        if(cursor>=0&&(cursor<low||cursor>high)){
            new AlertDialog.Builder(this).setTitle("Curseur Sabqi hors de la plage")
                .setMessage("Repositionner le Sabqi au début "+newStart+" ?")
                .setNegativeButton("Garder",(d,w)->refreshSabqi())
                .setPositiveButton("Repositionner",(d,w)->{prefs.setSabqiLineCursor(low);prefs.setSabqiProgress(0,0);prefs.setElapsedFor(HifzSessionActivity.SABQI,0L);refreshSabqi();}).show();
        }else refreshSabqi();
    }

    private void refreshRanges(){
        rangesBox.removeAllViews();List<VerseRange> ranges=prefs.itqanRanges();
        for(int i=0;i<ranges.size();i++){
            final int index=i;VerseRange range=ranges.get(i);LinearLayout row=Ui.row(this);
            TextView label=Ui.text(this,"Plage "+(i+1)+" · "+range.getStart()+" → "+range.getEndInclusive(),12.5f,false);Ui.weight(label,1);row.addView(label);
            row.addView(Ui.roundButton(this,"","Modifier la plage",v->chooseRange(range,index)));
            row.addView(Ui.roundButton(this,"","Supprimer la plage",v->removeRange(index)));rangesBox.addView(row);
        }
    }

    private void chooseRange(VerseRange existing,int index){
        VerseRef initialStart=existing==null?prefs.itqanRotationStart():existing.getStart();
        chooseVerse(existing==null?"Début de la nouvelle plage":"Début de la plage",initialStart,start->
            chooseVerse("Fin de la plage",existing==null?start:existing.getEndInclusive(),end->{
                if(GeometryRepository.ordinal(start)>GeometryRepository.ordinal(end)){Toast.makeText(this,"Le début de plage doit précéder sa fin.",Toast.LENGTH_LONG).show();return;}
                ArrayList<VerseRange> ranges=new ArrayList<>(prefs.itqanRanges());VerseRange replacement=new VerseRange(start,end);
                if(index<0)ranges.add(replacement);else ranges.set(index,replacement);
                saveRanges(ranges);
            }));
    }

    private void removeRange(int index){
        ArrayList<VerseRange> ranges=new ArrayList<>(prefs.itqanRanges());
        if(ranges.size()<=1){Toast.makeText(this,"Au moins une plage Itqān doit rester définie.",Toast.LENGTH_LONG).show();return;}
        VerseRange target=ranges.get(index);
        new AlertDialog.Builder(this).setTitle("Supprimer cette plage ?").setMessage(target.getStart()+" → "+target.getEndInclusive())
            .setNegativeButton("Annuler",null).setPositiveButton("Supprimer",(d,w)->{ranges.remove(index);saveRanges(ranges);}).show();
    }

    private void saveRanges(List<VerseRange> ranges){
        if(!prefs.setItqanRanges(ranges)){Toast.makeText(this,"Impossible d’enregistrer les plages Itqān.",Toast.LENGTH_LONG).show();return;}
        refreshRanges();refreshItqan();
        if(!prefs.isItqanCursorValid()||!prefs.isRotationStartValid()||!prefs.isMurajaahCursorValid()){
            new AlertDialog.Builder(this).setTitle("Curseur à vérifier")
                .setMessage("Une borne est hors du nouveau corpus. Aucun déplacement automatique n’a été effectué.")
                .setPositiveButton("OK",null).show();
        }
    }

    private void refreshItqan(){
        rotationButton.setText("Rotation · "+prefs.itqanRotationStart());
        boolean invalid=!prefs.isItqanCursorValid()||!prefs.isMurajaahCursorValid();
        itqanStatus.setText(invalid?"⚠ Curseur à repositionner":"");
        itqanStatus.setVisibility(invalid?View.VISIBLE:View.GONE);
    }

    private void setRotationStart(VerseRef verse){
        if(!prefs.corpus().contains(verse)){Toast.makeText(this,"Ce verset n’appartient à aucune plage Itqān.",Toast.LENGTH_LONG).show();return;}
        prefs.setItqanRotationStart(verse);refreshItqan();
        new AlertDialog.Builder(this).setTitle("Rotation enregistrée")
            .setMessage("Repositionner le curseur Itqān sur "+verse+" ? L’unité ×"+PreviewConfig.ITQAN_TOTAL_REPS+" en cours sera remise à zéro.")
            .setNegativeButton("Garder",null)
            .setPositiveButton("Repositionner",(d,w)->{prefs.setItqanCursor(verse);prefs.setItqanProgress(0,0,null,null);prefs.setElapsedFor(HifzSessionActivity.ITQAN,0L);refreshItqan();}).show();
    }

    private void confirmMurajaahReposition(){
        VerseRef target=prefs.itqanRotationStart();
        if(!prefs.corpus().contains(target)){Toast.makeText(this,"Choisissez d’abord une rotation valide.",Toast.LENGTH_LONG).show();return;}
        new AlertDialog.Builder(this).setTitle("Repositionner Murājaʿah ?")
            .setMessage("Repartir de "+target+" au Bloc A ?")
            .setNegativeButton("Annuler",null)
            .setPositiveButton("Repositionner",(d,w)->{prefs.setMurajaahCursor(target);prefs.setElapsedFor(HifzSessionActivity.MURAJAAH,0L);prefs.setMurajaahRuntime("A",null,0,0L,0L);refreshItqan();refreshMurajaah();}).show();
    }

    private void refreshMurajaah(){
        murajaahStatus.setText("30 min Sabqi récent + 30 min Itqān · Bloc "+prefs.murajaahPhase());
    }

    private void refreshAudio(){
        HifzAudioPack pack=new HifzAudioPack(this);
        if(pack.installed()) audioStatus.setText("✓ Audio hors ligne prêt · 6 236 versets");
        else audioStatus.setText("Pack audio non installé · choisissez le ZIP Husary Muʿallim");
    }

    private void selectAudioZip(){
        Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/zip");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
            Uri downloads=Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADownload");
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI,downloads);
        }
        startActivityForResult(Intent.createChooser(intent,"Choisir "+HifzAudioPack.PACK_FILE_NAME),REQUEST_AUDIO_ZIP);
    }

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode!=REQUEST_AUDIO_ZIP||resultCode!=RESULT_OK||data==null)return;
        Uri uri=data.getData();if(uri==null)return;
        try{
            int flags=data.getFlags()&Intent.FLAG_GRANT_READ_URI_PERMISSION;
            if(flags!=0)getContentResolver().takePersistableUriPermission(uri,flags);
        }catch(SecurityException ignored){}
        audioStatus.setText("Import et vérification… gardez Quran Hifz ouvert.");
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        new Thread(()->{
            HifzAudioPack.ImportResult result=new HifzAudioPack(getApplicationContext()).importZip(uri);
            runOnUiThread(()->{
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                if(result.ok){refreshAudio();Toast.makeText(this,"Audio installé.",Toast.LENGTH_LONG).show();}
                else{audioStatus.setText(result.message);Toast.makeText(this,result.message,Toast.LENGTH_LONG).show();}
            });
        },"hifz-audio-import").start();
    }

    private void chooseVerse(String title,VerseRef current,VerseChosen chosen){
        LinearLayout box=Ui.row(this);box.setPadding(Ui.dp(this,12),Ui.dp(this,8),Ui.dp(this,12),Ui.dp(this,8));
        NumberPicker surah=new NumberPicker(this);surah.setMinValue(1);surah.setMaxValue(114);surah.setValue(current.getSurah());surah.setWrapSelectorWheel(false);
        NumberPicker ayah=new NumberPicker(this);ayah.setMinValue(1);ayah.setMaxValue(QuranCanon.INSTANCE.ayahCount(current.getSurah()));ayah.setValue(current.getAyah());ayah.setWrapSelectorWheel(false);
        surah.setOnValueChangedListener((picker,oldValue,newValue)->{int max=QuranCanon.INSTANCE.ayahCount(newValue);ayah.setMaxValue(max);if(ayah.getValue()>max)ayah.setValue(max);});
        TextView colon=Ui.text(this," : ",18,true);box.addView(surah);box.addView(colon);box.addView(ayah);
        new AlertDialog.Builder(this).setTitle(title).setView(box).setNegativeButton("Annuler",null)
            .setPositiveButton("Choisir",(d,w)->chosen.accept(new VerseRef(surah.getValue(),ayah.getValue()))).show();
    }

    private void showDiagnostic(){
        String state="Schéma : "+prefs.schema()+"\nDébut programme : "+prefs.programStartDate()
            +"\nSabqi : "+prefs.sabqiStart()+" → "+prefs.sabqiEnd()+" · ligne "+prefs.sabqiLineCursor()
            +"\nPlages Itqān : "+prefs.itqanRanges().size()+"\nPlages promues : "+prefs.promotedRanges().size()
            +"\nDébut rotation : "+prefs.itqanRotationStart()+"\nCurseur Itqān : "+prefs.itqanCursor()
            +"\nCurseur Murājaʿah : "+prefs.murajaahCursor()+"\nPhase Murājaʿah : "+prefs.murajaahPhase()
            +"\nFile Sabqi récent : "+prefs.recentSabqi().size()+"\nVitesse récent : "+String.format(Locale.ROOT,"%.2f",prefs.recentSecondsPerLine())+" s/ligne"
            +"\nVitesse ancien : "+String.format(Locale.ROOT,"%.2f",prefs.murajaahSecondsPerLine())+" s/ligne";
        new AlertDialog.Builder(this).setTitle("Diagnostic Hifz").setMessage(state).setPositiveButton("Fermer",null).show();
    }

    private void confirmReset(){
        new AlertDialog.Builder(this).setTitle("Réinitialiser l’état de test ?")
            .setMessage("Remet le scénario de référence. Le Mushaf, les Tafsir et l’audio installé ne sont pas modifiés.")
            .setNegativeButton("Annuler",null).setPositiveButton("Réinitialiser",(d,w)->{prefs.resetPreviewState();getSharedPreferences("hifz_preview_session_gates",MODE_PRIVATE).edit().clear().apply();refreshAll();}).show();
    }
}
