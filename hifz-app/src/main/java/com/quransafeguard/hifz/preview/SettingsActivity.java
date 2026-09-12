package com.quransafeguard.hifz.preview;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
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

/** BOOX-oriented configuration. Rotation anchor is editable; live cursors are read-only. */
public final class SettingsActivity extends android.app.Activity {
    private static final int REQUEST_AUDIO_ZIP = 4103;
    private HifzPrefs prefs;
    private GeometryRepository geometry;
    private LinearLayout rangesBox, sabqiStartRow, sabqiEndRow, rotationSetting, audioSetting;
    private TextView sabqiStatus, itqanStatus, murajaahStatus, audioStatus;

    private interface VerseChosen { void accept(VerseRef verse); }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new HifzPrefs(this);
        geometry = GeometryRepository.get(this);

        ScrollView scroll = new ScrollView(this);scroll.setFillViewport(true);
        LinearLayout root = Ui.column(this);scroll.addView(root);
        LinearLayout top=Ui.row(this);top.setPadding(0,0,0,Ui.dp(this,2));
        top.addView(Ui.iconButton(this,"","Retour",v->finish()));
        TextView title=Ui.bookText(this,"Paramètres Hifz",18,true);Ui.weight(title,1);title.setGravity(Gravity.CENTER);top.addView(title);
        TextView balance=Ui.text(this,"",1,false);top.addView(balance,new LinearLayout.LayoutParams(Ui.dp(this,44),Ui.dp(this,44)));
        root.addView(top);

        section(root,"Parcours");
        TextView protocol=Ui.text(this,"Lun/Mer/Ven · Sabqi 5 lignes + 30 min   ·   Mar/Jeu · Itqān ×"+PreviewConfig.ITQAN_TOTAL_REPS+" + Murājaʿah 60 min   ·   Week-end · 30 + 30 min",11f,false);
        protocol.setTextColor(Ui.MUTED);protocol.setPadding(Ui.dp(this,4),0,Ui.dp(this,4),Ui.dp(this,4));root.addView(protocol);

        sabqiStartRow=Ui.settingRow(this,"Début Sabqi",prefs.sabqiStart().toString(),v->chooseVerse("Début Sabqi",prefs.sabqiStart(),verse->setSabqiBound(true,verse)));
        root.addView(sabqiStartRow);root.addView(Ui.divider(this));
        sabqiEndRow=Ui.settingRow(this,"Fin Sabqi",prefs.sabqiEnd().toString(),v->chooseVerse("Fin Sabqi",prefs.sabqiEnd(),verse->setSabqiBound(false,verse)));
        root.addView(sabqiEndRow);
        sabqiStatus=Ui.text(this,"",11f,false);sabqiStatus.setTextColor(Ui.MUTED);sabqiStatus.setPadding(Ui.dp(this,4),0,0,Ui.dp(this,4));root.addView(sabqiStatus);

        section(root,"Itqān · plages");
        rangesBox=Ui.column(this);rangesBox.setPadding(0,0,0,0);root.addView(rangesBox);
        root.addView(Ui.divider(this));
        root.addView(Ui.settingRow(this,"Ajouter","Nouvelle plage",v->chooseRange(null,-1)));
        itqanStatus=Ui.text(this,"",11f,false);itqanStatus.setTextColor(Ui.MUTED);itqanStatus.setPadding(Ui.dp(this,4),0,0,Ui.dp(this,2));root.addView(itqanStatus);
        rotationSetting=Ui.settingRow(this,"Début de rotation Itqān",prefs.itqanRotationStart().toString(),
            v->chooseVerse("Début de rotation Itqān",prefs.itqanRotationStart(),this::setRotationStart));
        root.addView(rotationSetting);

        section(root,"Murājaʿah");
        murajaahStatus=Ui.text(this,"",12f,false);murajaahStatus.setPadding(Ui.dp(this,4),0,0,Ui.dp(this,2));root.addView(murajaahStatus);

        section(root,"Audio");
        audioSetting=Ui.settingRow(this,"Al-Husary Muʿallim","Choisir le pack",v->selectAudioZip());
        audioStatus=Ui.settingValue(audioSetting);root.addView(audioSetting);

        section(root,"Affichage");
        LinearLayout einkRow=Ui.row(this);einkRow.setPadding(Ui.dp(this,2),Ui.dp(this,3),Ui.dp(this,2),Ui.dp(this,3));einkRow.setMinimumHeight(Ui.dp(this,48));
        TextView einkLabel=Ui.text(this,"Optimisation E‑Ink / BOOX",13f,false);Ui.weight(einkLabel,1);einkRow.addView(einkLabel);
        Switch eink=new Switch(this);eink.setChecked(prefs.forceEink());eink.setContentDescription("Optimisation E‑Ink / BOOX");eink.setOnCheckedChangeListener((button,checked)->prefs.setForceEink(checked));einkRow.addView(eink);root.addView(einkRow);

        section(root,"Avancé");
        root.addView(Ui.settingRow(this,"Diagnostic","État Hifz",v->showDiagnostic()));root.addView(Ui.divider(this));
        root.addView(Ui.settingRow(this,"Réinitialiser","État de test",v->confirmReset()));

        setContentView(scroll);int inset=Ui.dp(this,12);Ui.respectSystemBars(this,root,inset,inset,inset,inset);
        refreshAll();
    }

    private void section(LinearLayout root,String title){
        TextView view=Ui.bookText(this,title,15,true);view.setPadding(0,Ui.dp(this,12),0,Ui.dp(this,3));root.addView(view);
    }

    private void refreshAll(){refreshSabqi();refreshRanges();refreshItqan();refreshMurajaah();refreshAudio();}

    private void refreshSabqi(){
        int first=geometry.firstLineIndex(prefs.sabqiStart()),last=geometry.lastLineIndex(prefs.sabqiEnd());
        int total=last-first+1;
        boolean aligned=total>0&&total%PreviewConfig.SABQI_LINES==0;
        sabqiStatus.setText(total+" lignes"+(aligned?"":" · ⚠ fin à ajuster"));
        TextView startValue=Ui.settingValue(sabqiStartRow),endValue=Ui.settingValue(sabqiEndRow);
        if(startValue!=null)startValue.setText(prefs.sabqiStart().toString());
        if(endValue!=null)endValue.setText(prefs.sabqiEnd().toString());
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
            final int index=i;VerseRange range=ranges.get(i);LinearLayout row=Ui.row(this);row.setMinimumHeight(Ui.dp(this,46));
            TextView label=Ui.text(this,"Plage "+(i+1),12.5f,true);label.setPadding(Ui.dp(this,4),0,Ui.dp(this,6),0);row.addView(label);
            TextView value=Ui.text(this,range.getStart()+" → "+range.getEndInclusive(),11.5f,false);value.setTextColor(Ui.MUTED);Ui.weight(value,1);row.addView(value);
            row.addView(Ui.iconButton(this,"","Modifier la plage",v->chooseRange(range,index)));
            row.addView(Ui.iconButton(this,"","Supprimer la plage",v->removeRange(index)));rangesBox.addView(row);
            if(i+1<ranges.size())rangesBox.addView(Ui.divider(this));
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
        TextView rotationValue=Ui.settingValue(rotationSetting);if(rotationValue!=null)rotationValue.setText(prefs.itqanRotationStart().toString());
        boolean invalid=!prefs.isItqanCursorValid()||!prefs.isMurajaahCursorValid();
        itqanStatus.setText((invalid?"⚠ ":"")+"Curseur Itqān · "+prefs.itqanCursor()+"   ·   Murājaʿah · "+prefs.murajaahCursor());
        itqanStatus.setTextColor(Ui.MUTED);
        itqanStatus.setVisibility(View.VISIBLE);
    }

    private void setRotationStart(VerseRef verse){
        if(!prefs.itqanWorkCorpus().contains(verse)){
            Toast.makeText(this,"Ce verset n’appartient à aucune plage Itqān.",Toast.LENGTH_LONG).show();
            return;
        }
        prefs.setItqanRotationStart(verse);
        refreshItqan();
        Toast.makeText(this,"Début de rotation enregistré. Les curseurs actuels restent inchangés.",Toast.LENGTH_LONG).show();
    }

    private void refreshMurajaah(){
        murajaahStatus.setText("Mar/Jeu · 60 min   ·   Sam/Dim · 30 min   ·   curseur "+prefs.murajaahCursor());
        murajaahStatus.setTextColor(Ui.MUTED);
    }

    private void refreshAudio(){
        HifzAudioPack pack=new HifzAudioPack(this);
        if(pack.installed()) audioStatus.setText("✓ prêt · 6 236 versets");
        else audioStatus.setText("Choisir le pack");
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
        audioStatus.setText("Import et vérification…");
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
            +"\nCurseur Murājaʿah : "+prefs.murajaahCursor()
            +"\nPromotions à consolider : "+prefs.unconsolidatedPromotedRanges().size()
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
