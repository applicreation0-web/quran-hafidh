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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** BOOX-oriented configuration. Rotation anchor is editable; live cursors are read-only. */
public final class SettingsActivity extends android.app.Activity {
    private static final int REQUEST_AUDIO_ZIP = 4103;
    private HifzPrefs prefs;
    private HifzSpeedStore speedStore;
    private J10ReviewPlanner j10Planner;
    private GeometryRepository geometry;
    private final ExecutorService j10Loader = Executors.newSingleThreadExecutor();
    private LinearLayout stabilizationRangesBox, acquiredRangesBox, sabqiStartRow, sabqiEndRow, rotationSetting, hardAnchoringSetting, audioSetting;
    private TextView sabqiStatus, itqanStatus, effectiveCorpusStatus, murajaahStatus, j10Status, audioStatus;

    private interface VerseChosen { void accept(VerseRef verse); }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new HifzPrefs(this);
        speedStore = new HifzSpeedStore(this);
        try {
            geometry = GeometryRepository.get(this);
            j10Planner = new J10ReviewPlanner(this);
        } catch (Throwable error) {
            Ui.showFatal(this, "La géométrie du Mushaf est indisponible. Fermez puis rouvrez l’application.");
            return;
        }

        ScrollView scroll = new ScrollView(this);scroll.setFillViewport(true);
        LinearLayout root = Ui.column(this);scroll.addView(root);
        LinearLayout top=Ui.row(this);top.setPadding(0,0,0,Ui.dp(this,2));
        top.addView(Ui.iconButton(this,"","Retour",v->finish()));
        TextView title=Ui.bookText(this,"Paramètres Hifz",18,true);Ui.weight(title,1);title.setGravity(Gravity.CENTER);top.addView(title);
        TextView balance=Ui.text(this,"",1,false);top.addView(balance,new LinearLayout.LayoutParams(Ui.dp(this,44),Ui.dp(this,44)));
        root.addView(top);

        section(root,"Parcours");
        TextView protocol=Ui.text(this,"Lun/Mer/Ven · Apprentissage   ·   Mar/Jeu · Stabilisation   ·   Sam/Dim · Révision",11f,false);
        protocol.setTextColor(Ui.MUTED);protocol.setPadding(Ui.dp(this,4),0,Ui.dp(this,4),Ui.dp(this,4));root.addView(protocol);

        sabqiStartRow=Ui.settingRow(this,"Début de la plage à mémoriser",prefs.sabqiStart().toString(),v->chooseVerse("Début de la plage à mémoriser",prefs.sabqiStart(),verse->setSabqiBound(true,verse)));
        root.addView(sabqiStartRow);root.addView(Ui.divider(this));
        sabqiEndRow=Ui.settingRow(this,"Fin de la plage à mémoriser",prefs.sabqiEnd().toString(),v->chooseVerse("Fin de la plage à mémoriser",prefs.sabqiEnd(),verse->setSabqiBound(false,verse)));
        root.addView(sabqiEndRow);
        sabqiStatus=Ui.text(this,"",11f,false);sabqiStatus.setTextColor(Ui.MUTED);sabqiStatus.setPadding(Ui.dp(this,4),0,0,Ui.dp(this,4));root.addView(sabqiStatus);

        section(root,"Plages à stabiliser");
        stabilizationRangesBox=Ui.column(this);stabilizationRangesBox.setPadding(0,0,0,0);root.addView(stabilizationRangesBox);
        root.addView(Ui.divider(this));
        root.addView(Ui.settingRow(this,"Ajouter","Nouvelle plage",v->chooseStabilizationRange(null,-1)));

        section(root,"Plages acquises");
        acquiredRangesBox=Ui.column(this);acquiredRangesBox.setPadding(0,0,0,0);root.addView(acquiredRangesBox);
        root.addView(Ui.divider(this));
        root.addView(Ui.settingRow(this,"Ajouter","Nouvelle plage",v->chooseAcquiredRange(null,-1)));

        itqanStatus=Ui.text(this,"",11f,false);itqanStatus.setTextColor(Ui.MUTED);itqanStatus.setPadding(Ui.dp(this,4),Ui.dp(this,4),0,Ui.dp(this,2));root.addView(itqanStatus);
        rotationSetting=Ui.settingRow(this,"Début de rotation de stabilisation",prefs.itqanRotationStart().toString(),
            v->chooseVerse("Début de rotation de stabilisation",prefs.itqanRotationStart(),this::setRotationStart));
        root.addView(rotationSetting);
        root.addView(Ui.divider(this));
        hardAnchoringSetting=Ui.settingRow(this,"Sourates difficiles à stabiliser","0 sourate",v->showHardAnchoringSelector());
        root.addView(hardAnchoringSetting);

        section(root,"Corpus effectif");
        effectiveCorpusStatus=Ui.text(this,"",11f,false);
        effectiveCorpusStatus.setTextColor(Ui.MUTED);
        effectiveCorpusStatus.setPadding(Ui.dp(this,4),0,Ui.dp(this,4),Ui.dp(this,2));
        root.addView(effectiveCorpusStatus);

        section(root,"Révision");
        murajaahStatus=Ui.text(this,"",12f,false);murajaahStatus.setPadding(Ui.dp(this,4),0,0,Ui.dp(this,2));root.addView(murajaahStatus);

        section(root,"Vitesses");
        root.addView(Ui.settingRow(this,"Révision",speedStore.maintenanceSummary(),null));
        root.addView(Ui.divider(this));
        root.addView(Ui.settingRow(this,"Consolidation",speedStore.consolidationSummary(),null));

        section(root,"J10");
        j10Status=Ui.text(this,"",11.5f,false);
        j10Status.setTextColor(Ui.MUTED);
        j10Status.setPadding(Ui.dp(this,4),0,Ui.dp(this,4),Ui.dp(this,2));
        root.addView(j10Status);

        section(root,"Audio");
        audioSetting=Ui.settingRow(this,"Al-Husary Muʿallim","Choisir le pack",v->selectAudioZip());
        audioStatus=Ui.settingValue(audioSetting);root.addView(audioSetting);

        section(root,"Affichage");
        LinearLayout einkRow=Ui.row(this);einkRow.setPadding(Ui.dp(this,2),Ui.dp(this,3),Ui.dp(this,2),Ui.dp(this,3));einkRow.setMinimumHeight(Ui.dp(this,48));
        TextView einkLabel=Ui.text(this,"Optimisation E‑Ink / BOOX",13f,false);Ui.weight(einkLabel,1);einkRow.addView(einkLabel);
        Switch eink=new Switch(this);eink.setChecked(prefs.forceEink());eink.setContentDescription("Optimisation E‑Ink / BOOX");eink.setOnCheckedChangeListener((button,checked)->prefs.setForceEink(checked));einkRow.addView(eink);root.addView(einkRow);

        section(root,"Avancé");
        root.addView(Ui.settingRow(this,"Diagnostic","État Hifz",v->showDiagnostic()));root.addView(Ui.divider(this));
        root.addView(Ui.settingRow(this,"Réinitialiser","Progression Hifz",v->confirmReset()));

        section(root,"Repères");
        addRepere(root,"Apprentissage","Action de mémorisation d’un nouveau passage. Après validation, le passage devient Appris.");
        addRepere(root,"Appris","État d’un passage mémorisé dont la prochaine action structurée est la Stabilisation.");
        addRepere(root,"Stabilisation","Action de renforcement de la matière Apprise sur les unités physiques du Mushaf. Après validation, elle devient Stabilisée.");
        addRepere(root,"Stabilisé","État d’un passage prêt pour la Consolidation.");
        addRepere(root,"Consolidation","Action de regroupement progressif de une à trois unités. Une Consolidation validée fait passer la matière vers Acquis.");
        addRepere(root,"Acquis","État d’un passage qui entre dans la Révision et dans la garantie J10.");
        addRepere(root,"Révision","Rotation régulière de la matière Acquise, avec révélation seulement en cas de besoin.");
        addRepere(root,"J10","Garantie de fraîcheur : toute matière Acquise doit être revue au plus tard tous les dix jours. J10 utilise les créneaux existants et n’ajoute pas de séance parallèle.");
        addRepere(root,"Report souple","Une séance manquée reste due au prochain créneau sans échec, sans double quota automatique et sans déplacement silencieux du curseur.");

        setContentView(scroll);int inset=Ui.dp(this,12);Ui.respectSystemBars(this,root,inset,inset,inset,inset);
        refreshAll();
    }

    private void section(LinearLayout root,String title){
        TextView view=Ui.bookText(this,title,15,true);view.setPadding(0,Ui.dp(this,12),0,Ui.dp(this,3));root.addView(view);
    }

    private void addRepere(LinearLayout root,String title,String definition){
        TextView heading=Ui.text(this,title,12.5f,true);heading.setPadding(Ui.dp(this,4),Ui.dp(this,5),Ui.dp(this,4),0);root.addView(heading);
        TextView body=Ui.text(this,definition,11f,false);body.setTextColor(Ui.MUTED);body.setPadding(Ui.dp(this,4),0,Ui.dp(this,4),Ui.dp(this,5));root.addView(body);
        root.addView(Ui.divider(this));
    }

    private void refreshAll(){refreshRangeLists();refreshSabqi();refreshItqan();refreshHardAnchoring();refreshEffectiveItqanCorpus();refreshMurajaah();refreshJ10();refreshAudio();}

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
        if(GeometryRepository.ordinal(newStart)>GeometryRepository.ordinal(newEnd)){Toast.makeText(this,"Le début doit précéder la fin de la plage à mémoriser.",Toast.LENGTH_LONG).show();return;}
        if(isStart)prefs.setSabqiStart(verse);else prefs.setSabqiEnd(verse);
        int low=geometry.firstLineIndex(newStart),high=geometry.lastLineIndex(newEnd),cursor=prefs.sabqiLineCursor();
        if(cursor>=0&&(cursor<low||cursor>high)){
            new AlertDialog.Builder(this).setTitle("Position de la leçon hors de la plage")
                .setMessage("Repositionner l’Apprentissage au début "+newStart+" ?")
                .setNegativeButton("Garder",(d,w)->refreshSabqi())
                .setPositiveButton("Repositionner",(d,w)->{prefs.setSabqiLineCursor(low);prefs.setSabqiProgress(0,0);prefs.setElapsedFor(HifzSessionActivity.SABQI,0L);refreshSabqi();}).show();
        }else refreshSabqi();
    }

    private void refreshRangeLists(){
        refreshStabilizationRanges();
        refreshAcquiredRanges();
    }

    private void refreshStabilizationRanges(){
        renderRanges(stabilizationRangesBox,prefs.unconsolidatedPromotedRanges(),true);
    }

    private void refreshAcquiredRanges(){
        renderRanges(acquiredRangesBox,prefs.itqanRanges(),false);
    }

    private void renderRanges(LinearLayout box,List<VerseRange> ranges,boolean stabilization){
        box.removeAllViews();
        if(ranges.isEmpty()){
            TextView empty=Ui.text(this,"Aucune plage",11.5f,false);empty.setTextColor(Ui.MUTED);empty.setPadding(Ui.dp(this,4),Ui.dp(this,4),0,Ui.dp(this,4));box.addView(empty);return;
        }
        for(int i=0;i<ranges.size();i++){
            final int index=i;VerseRange range=ranges.get(i);LinearLayout row=Ui.row(this);row.setMinimumHeight(Ui.dp(this,46));
            TextView label=Ui.text(this,"Plage "+(i+1),12.5f,true);label.setPadding(Ui.dp(this,4),0,Ui.dp(this,6),0);row.addView(label);
            TextView value=Ui.text(this,range.getStart()+" → "+range.getEndInclusive(),11.5f,false);value.setTextColor(Ui.MUTED);Ui.weight(value,1);row.addView(value);
            row.addView(Ui.iconButton(this,"","Modifier la plage",v->{if(stabilization)chooseStabilizationRange(range,index);else chooseAcquiredRange(range,index);}));
            row.addView(Ui.iconButton(this,"","Supprimer la plage",v->{if(stabilization)removeStabilizationRange(index);else removeAcquiredRange(index);}));box.addView(row);
            if(i+1<ranges.size())box.addView(Ui.divider(this));
        }
    }

    private void chooseStabilizationRange(VerseRange existing,int index){chooseRange(existing,index,true);}
    private void chooseAcquiredRange(VerseRange existing,int index){chooseRange(existing,index,false);}

    private void chooseRange(VerseRange existing,int index,boolean stabilization){
        List<VerseRange> current=stabilization?prefs.unconsolidatedPromotedRanges():prefs.itqanRanges();
        VerseRef fallback=stabilization?prefs.itqanRotationStart():prefs.itqanRanges().get(0).getStart();
        VerseRef initialStart=existing==null?fallback:existing.getStart();
        chooseVerse(existing==null?"Début de la nouvelle plage":"Début de la plage",initialStart,start->
            chooseVerse("Fin de la plage",existing==null?start:existing.getEndInclusive(),end->{
                if(GeometryRepository.ordinal(start)>GeometryRepository.ordinal(end)){Toast.makeText(this,"Le début de plage doit précéder sa fin.",Toast.LENGTH_LONG).show();return;}
                ArrayList<VerseRange> ranges=new ArrayList<>(current);VerseRange replacement=new VerseRange(start,end);
                if(index<0)ranges.add(replacement);else ranges.set(index,replacement);
                saveV6Ranges(ranges,stabilization);
            }));
    }

    private void removeStabilizationRange(int index){
        ArrayList<VerseRange> ranges=new ArrayList<>(prefs.unconsolidatedPromotedRanges());
        VerseRange target=ranges.get(index);
        new AlertDialog.Builder(this).setTitle("Supprimer cette plage à stabiliser ?").setMessage(target.getStart()+" → "+target.getEndInclusive())
            .setNegativeButton("Annuler",null).setPositiveButton("Supprimer",(d,w)->{ranges.remove(index);saveV6Ranges(ranges,true);}).show();
    }

    private void removeAcquiredRange(int index){
        ArrayList<VerseRange> ranges=new ArrayList<>(prefs.itqanRanges());
        if(ranges.size()<=1){Toast.makeText(this,"Au moins une plage Acquise doit rester définie.",Toast.LENGTH_LONG).show();return;}
        VerseRange target=ranges.get(index);
        new AlertDialog.Builder(this).setTitle("Supprimer cette plage Acquise ?").setMessage(target.getStart()+" → "+target.getEndInclusive())
            .setNegativeButton("Annuler",null).setPositiveButton("Supprimer",(d,w)->{ranges.remove(index);saveV6Ranges(ranges,false);}).show();
    }

    private void saveV6Ranges(List<VerseRange> ranges,boolean stabilization){
        try{
            boolean saved=stabilization
                ?prefs.setV6StabilizationRanges(ranges,geometry)
                :prefs.setV6AcquiredRanges(ranges,geometry);
            if(!saved){Toast.makeText(this,"Impossible d’enregistrer les plages.",Toast.LENGTH_LONG).show();return;}
        }catch(IllegalArgumentException|IllegalStateException error){
            Toast.makeText(this,error.getMessage()==null?"Plages incompatibles.":error.getMessage(),Toast.LENGTH_LONG).show();return;
        }
        refreshRangeLists();refreshItqan();refreshEffectiveItqanCorpus();
        if(!prefs.isItqanCursorValid()||!prefs.isRotationStartValid()||!prefs.isMurajaahCursorValid()){
            new AlertDialog.Builder(this).setTitle("Position à vérifier")
                .setMessage("Une borne est hors du nouveau corpus. Aucun déplacement automatique n’a été effectué.")
                .setPositiveButton("OK",null).show();
        }
    }

    private void refreshItqan(){
        TextView rotationValue=Ui.settingValue(rotationSetting);if(rotationValue!=null)rotationValue.setText(prefs.itqanRotationStart().toString());
        boolean invalid=!prefs.isItqanCursorValid()||!prefs.isMurajaahCursorValid();
        itqanStatus.setText((invalid?"⚠ ":"")+"Position Stabilisation · "+prefs.itqanCursor()+"   ·   Révision · "+prefs.murajaahCursor());
        itqanStatus.setTextColor(Ui.MUTED);
        itqanStatus.setVisibility(View.VISIBLE);
    }

    private void refreshHardAnchoring(){
        int count=prefs.hardAnchoringSurahs().size();
        TextView value=Ui.settingValue(hardAnchoringSetting);
        if(value!=null)value.setText(count+" "+(count==1?"sourate":"sourates"));
    }

    private List<Integer> availableHardAnchoringSurahs(){
        LinkedHashSet<Integer> result=new LinkedHashSet<>();
        for(VerseRange range:prefs.effectiveItqanRanges()){
            for(int surah=range.getStart().getSurah();surah<=range.getEndInclusive().getSurah();surah++)result.add(surah);
        }
        return new ArrayList<>(result);
    }

    private void showHardAnchoringSelector(){
        if(prefs.itqanRep()>0 || prefs.itqanBlockIndex()>0){
            Toast.makeText(this,"Terminez l’unité de Stabilisation en cours avant de modifier ce réglage.",Toast.LENGTH_LONG).show();
            return;
        }
        List<Integer> candidates=availableHardAnchoringSurahs();
        if(candidates.isEmpty()){Toast.makeText(this,"Aucune sourate dans le corpus de Stabilisation.",Toast.LENGTH_LONG).show();return;}
        List<Integer> selected=prefs.hardAnchoringSurahs();
        String[] labels=new String[candidates.size()];
        boolean[] checked=new boolean[candidates.size()];
        for(int i=0;i<candidates.size();i++){
            int surah=candidates.get(i);
            labels[i]="Sourate "+surah;
            checked[i]=selected.contains(surah);
        }
        new AlertDialog.Builder(this)
            .setTitle("Sourates difficiles à stabiliser")
            .setMultiChoiceItems(labels,checked,(dialog,which,isChecked)->checked[which]=isChecked)
            .setNegativeButton("Annuler",null)
            .setPositiveButton("Enregistrer",(dialog,which)->{
                ArrayList<Integer> chosen=new ArrayList<>();
                for(int i=0;i<candidates.size();i++)if(checked[i])chosen.add(candidates.get(i));
                if(!prefs.setHardAnchoringSurahs(chosen)){Toast.makeText(this,"Impossible d’enregistrer ce réglage.",Toast.LENGTH_LONG).show();return;}
                refreshHardAnchoring();
            }).show();
    }

    private void refreshEffectiveItqanCorpus(){
        List<VerseRange> effective=prefs.effectiveItqanRanges();
        List<VerseRange> pending=prefs.unconsolidatedPromotedRanges();
        String corpus=effective.isEmpty()?"aucune plage":rangeSummary(effective);
        String waiting=pending.isEmpty()?"aucune":rangeSummary(pending);
        effectiveCorpusStatus.setText("Corpus de travail · "+corpus+"\nÀ stabiliser · "+waiting);
    }

    private static String rangeSummary(List<VerseRange> ranges){
        StringBuilder out=new StringBuilder();
        for(int i=0;i<ranges.size();i++){
            if(i>0)out.append("  ·  ");
            VerseRange r=ranges.get(i);
            out.append(r.getStart()).append(" → ").append(r.getEndInclusive());
        }
        return out.toString();
    }

    private void setRotationStart(VerseRef verse){
        if(!prefs.itqanWorkCorpus().contains(verse)){
            Toast.makeText(this,"Ce verset n’appartient à aucune plage de Stabilisation.",Toast.LENGTH_LONG).show();
            return;
        }
        prefs.setItqanRotationStart(verse);
        refreshItqan();
        Toast.makeText(this,"Début de rotation enregistré. Les positions actuelles restent inchangées.",Toast.LENGTH_LONG).show();
    }

    private void refreshMurajaah(){
        murajaahStatus.setText("Sam/Dim · Révision · 45 min · position "+prefs.murajaahCursor());
        murajaahStatus.setTextColor(Ui.MUTED);
    }

    private void refreshJ10(){
        if(j10Status==null)return;
        j10Status.setText("J10 · calcul…");
        final LocalDate today = HifzClock.today();
        j10Loader.execute(() -> {
            try{
                J10ReviewPlanner.PriorityGroup group=j10Planner.priorityGroup(today);
                J10ReviewPolicy.Forecast forecast=group.forecast;
                String state;
                if(forecast.sustainability==J10ReviewPolicy.Sustainability.NON_TENABLE){
                    state="Plan non tenable · déficit "+forecast.deficitMinutes+" min / 10 jours";
                }else if(forecast.sustainability==J10ReviewPolicy.Sustainability.TENSION){
                    state="Plan sous tension";
                }else{
                    state="Plan tenable";
                }
                String priority=group.isEmpty()?"Aucune priorité immédiate"
                    :"Priorité actuelle · J"+group.maxAgeDays+" · "+group.lineIds.size()+" ligne(s)";
                String text="Intervalle maximal · 10 jours\n"+state+"\n"+priority;
                runOnUiThread(() -> { if (!isFinishing() && j10Status != null) j10Status.setText(text); });
            }catch(RuntimeException error){
                runOnUiThread(() -> { if (!isFinishing() && j10Status != null) j10Status.setText("J10 · état à vérifier"); });
            }
        });
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
            +"\nApprentissage : "+prefs.sabqiStart()+" → "+prefs.sabqiEnd()+" · ligne "+prefs.sabqiLineCursor()
            +"\nPlages Acquises : "+prefs.itqanRanges().size()+"\nCorpus de travail : "+prefs.effectiveItqanRanges().size()+" plage(s)"
            +"\nPages promues : "+prefs.promotedRanges().size()+"\nÀ stabiliser : "+prefs.unconsolidatedPromotedRanges().size()
            +"\nDébut rotation : "+prefs.itqanRotationStart()+"\nPosition Stabilisation : "+prefs.itqanCursor()
            +"\nPosition Révision : "+prefs.murajaahCursor()
            +"\nFile de Consolidation : "+prefs.recentSabqi().size()
            +"\nVitesse Révision : "+speedStore.maintenanceSummary()
            +"\nVitesse Consolidation : "+speedStore.consolidationSummary();
        new AlertDialog.Builder(this).setTitle("Diagnostic Hifz").setMessage(state).setPositiveButton("Fermer",null).show();
    }

    private void confirmReset(){
        new AlertDialog.Builder(this).setTitle("Réinitialiser la progression Hifz ?")
            .setMessage("Efface la progression Hifz locale (Apprentissage, Stabilisation, Consolidation, Révision, positions et chronos). Le Mushaf, les Tafsir et l’audio installé ne sont pas modifiés.")
            .setNegativeButton("Annuler",null).setPositiveButton("Réinitialiser",(d,w)->{prefs.resetPreviewState();getSharedPreferences("hifz_preview_session_gates",MODE_PRIVATE).edit().clear().apply();refreshAll();}).show();
    }

    @Override protected void onDestroy() {
        j10Loader.shutdownNow();
        super.onDestroy();
    }

}
