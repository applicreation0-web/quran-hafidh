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

import com.quransafeguard.hifz.core.CadenceAction;
import com.quransafeguard.hifz.core.HifzSchedule;
import com.quransafeguard.hifz.core.QuranCanon;
import com.quransafeguard.hifz.core.VerseRange;
import com.quransafeguard.hifz.core.VerseRef;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/** BOOX-oriented configuration. Rotation anchor is editable; live cursors are read-only. */
public final class SettingsActivity extends android.app.Activity {
    private static final int REQUEST_AUDIO_ZIP = 4103;
    private static final int REQUEST_BACKUP_EXPORT = 4104;
    private static final int REQUEST_BACKUP_IMPORT = 4105;
    private HifzPrefs prefs;
    private HifzSpeedStore speedStore;
    private GeometryRepository geometry;
    private LinearLayout stabilizationRangesBox, acquiredRangesBox, sabqiStartRow, sabqiEndRow, rotationSetting, hardAnchoringSetting, audioSetting, learningDaysSetting, weakVersesSetting;
    private TextView sabqiStatus, itqanStatus, effectiveCorpusStatus, murajaahStatus, audioStatus, protocol, consolidationSchemaNote, renforcementSchemaNote;
    private static final String[] WEEKDAY_ABBREVIATIONS = {"Lun", "Mar", "Mer", "Jeu", "Ven", "Sam"};
    private static final DayOfWeek[] WEEKDAYS = {
        DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY
    };

    private interface VerseChosen { void accept(VerseRef verse); }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new HifzPrefs(this);
        speedStore = new HifzSpeedStore(this);
        try {
            geometry = GeometryRepository.get(this);
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
        protocol=Ui.text(this,weeklyCadenceSummary(),11f,false);
        protocol.setTextColor(Ui.MUTED);protocol.setPadding(Ui.dp(this,4),0,Ui.dp(this,4),Ui.dp(this,4));root.addView(protocol);
        learningDaysSetting=Ui.settingRow(this,"Séances d’Apprentissage par semaine",learningDaysSummary(),v->chooseLearningDaysPerWeek());
        root.addView(learningDaysSetting);root.addView(Ui.divider(this));

        sabqiStartRow=Ui.settingRow(this,"Début de la plage d’Apprentissage",prefs.sabqiStart().toString(),v->chooseVerse("Début de la plage d’Apprentissage",prefs.sabqiStart(),verse->setSabqiBound(true,verse)));
        root.addView(sabqiStartRow);root.addView(Ui.divider(this));
        sabqiEndRow=Ui.settingRow(this,"Fin de la plage d’Apprentissage",prefs.sabqiEnd().toString(),v->chooseVerse("Fin de la plage d’Apprentissage",prefs.sabqiEnd(),verse->setSabqiBound(false,verse)));
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

        section(root,"Audio");
        audioSetting=Ui.settingRow(this,"Al-Husary Muʿallim","Choisir le pack",v->selectAudioZip());
        audioStatus=Ui.settingValue(audioSetting);root.addView(audioSetting);

        section(root,"Affichage");
        LinearLayout einkRow=Ui.row(this);einkRow.setPadding(Ui.dp(this,2),Ui.dp(this,3),Ui.dp(this,2),Ui.dp(this,3));einkRow.setMinimumHeight(Ui.dp(this,48));
        TextView einkLabel=Ui.text(this,"Optimisation E‑Ink / BOOX",13f,false);Ui.weight(einkLabel,1);einkRow.addView(einkLabel);
        Switch eink=new Switch(this);eink.setChecked(prefs.forceEink());eink.setContentDescription("Optimisation E‑Ink / BOOX");eink.setOnCheckedChangeListener((button,checked)->prefs.setForceEink(checked));einkRow.addView(eink);root.addView(einkRow);

        section(root,"Sauvegarde");
        root.addView(Ui.settingRow(this,"Exporter","Fichier à conserver hors de l’appareil",v->exportBackup()));root.addView(Ui.divider(this));
        root.addView(Ui.settingRow(this,"Importer","Restaurer depuis un fichier exporté",v->confirmImportBackup()));
        TextView backupNote=Ui.text(this,"Sans compte ni serveur, la progression vit uniquement dans l’appli : désinstaller l’appli ou perdre l’appareil l’efface. Exportez régulièrement.",11f,false);
        backupNote.setTextColor(Ui.MUTED);backupNote.setPadding(Ui.dp(this,4),Ui.dp(this,3),Ui.dp(this,4),0);root.addView(backupNote);

        section(root,"Avancé");
        root.addView(Ui.settingRow(this,"Diagnostic","État Hifz",v->showDiagnostic()));root.addView(Ui.divider(this));
        weakVersesSetting=Ui.settingRow(this,"Repères faibles","",v->startActivity(new Intent(this,WeakVersesActivity.class)));
        root.addView(weakVersesSetting);root.addView(Ui.divider(this));
        root.addView(Ui.settingRow(this,"Réinitialiser","Progression Hifz",v->confirmReset()));

        section(root,"Schéma");
        TextView schema=Ui.bookText(this,"Apprentissage → Appris → Stabilisation → Stabilisé → Consolidation → Acquis → Révision",12.5f,true);
        schema.setPadding(Ui.dp(this,4),Ui.dp(this,4),Ui.dp(this,4),Ui.dp(this,5));root.addView(schema);
        consolidationSchemaNote=Ui.text(this,"",11f,false);
        consolidationSchemaNote.setTextColor(Ui.MUTED);consolidationSchemaNote.setPadding(Ui.dp(this,4),0,Ui.dp(this,4),Ui.dp(this,3));root.addView(consolidationSchemaNote);
        renforcementSchemaNote=Ui.text(this,"",11f,false);
        renforcementSchemaNote.setTextColor(Ui.MUTED);renforcementSchemaNote.setPadding(Ui.dp(this,4),0,Ui.dp(this,4),Ui.dp(this,3));root.addView(renforcementSchemaNote);
        TextView carryoverSchemaNote=Ui.text(this,"Report souple · une séance manquée reste due au prochain créneau du même type — aucun jour n’est perdu.",11f,false);
        carryoverSchemaNote.setTextColor(Ui.MUTED);carryoverSchemaNote.setPadding(Ui.dp(this,4),0,Ui.dp(this,4),Ui.dp(this,5));root.addView(carryoverSchemaNote);

        setContentView(scroll);int inset=Ui.dp(this,12);Ui.respectSystemBars(this,root,inset,inset,inset,inset);
        refreshAll();
    }

    private void section(LinearLayout root,String title){
        TextView view=Ui.bookText(this,title,15,true);view.setPadding(0,Ui.dp(this,12),0,Ui.dp(this,3));root.addView(view);
    }

    private void refreshAll(){refreshWeeklyCadence();refreshRangeLists();refreshSabqi();refreshItqan();refreshHardAnchoring();refreshEffectiveItqanCorpus();refreshMurajaah();refreshAudio();}

    /** Weekday abbreviations (Mon..Sat) currently assigned to the given cadence action, e.g. "Lun/Mer/Ven". */
    private String daysFor(CadenceAction action){
        int days=prefs.learningDaysPerWeek();
        StringBuilder out=new StringBuilder();
        for(int i=0;i<WEEKDAYS.length;i++){
            if(HifzSchedule.INSTANCE.actionFor(WEEKDAYS[i],days)!=action)continue;
            if(out.length()>0)out.append("/");
            out.append(WEEKDAY_ABBREVIATIONS[i]);
        }
        return out.toString();
    }

    /**
     * "Lun/Mer/Ven · Apprentissage · Mar/Jeu/Sam · Stabilisation · Dim · Révision", built from the
     * current split. At the 0/6 extremes one family has no day at all that week — its clause is
     * dropped entirely rather than showing an empty day list before "· Apprentissage/Stabilisation".
     */
    private String weeklyCadenceSummary(){
        String learningDays=daysFor(CadenceAction.LEARNING),stabilizationDays=daysFor(CadenceAction.STABILIZATION);
        StringBuilder out=new StringBuilder();
        if(!learningDays.isEmpty())out.append(learningDays).append(" · Apprentissage   ·   ");
        if(!stabilizationDays.isEmpty())out.append(stabilizationDays).append(" · Stabilisation   ·   ");
        return out.append("Dim · Révision").toString();
    }

    private String learningDaysSummary(){
        int days=prefs.learningDaysPerWeek();
        return days+"/6 jours   ·   Stabilisation "+(6-days)+"/6";
    }

    private void refreshWeeklyCadence(){
        protocol.setText(weeklyCadenceSummary());
        TextView value=Ui.settingValue(learningDaysSetting);
        if(value!=null)value.setText(learningDaysSummary());
        String stabilizationDays=daysFor(CadenceAction.STABILIZATION),learningDays=daysFor(CadenceAction.LEARNING);
        consolidationSchemaNote.setText("Consolidation · soir "+stabilizationDays);
        renforcementSchemaNote.setText("Renforcement (soir "+learningDays+") et Consolidation (soir "+stabilizationDays+") — l’effet boule de neige :"
            +"\n1. Chaque soir : ×10 sur CHAQUE bloc Appris ou Stabilisé accumulé depuis le début de la semaine (pas seulement celui du jour), plus ×10 sur l’ensemble de ces blocs lus d’une traite dès qu’il y en a plus d’un."
            +"\n2. Dimanche matin : la même chose une dernière fois (chaque bloc ×10 puis l’ensemble ×10), puis ils passent en Acquis."
            +"\n3. Chaque soir ajoute aussi "+HifzSchedule.ACTIVE_REVIEW_MINUTES+" min de Révision active puis "
            +HifzSchedule.MAINTENANCE_MINUTES+" min d’Entretien de l’Acquis, dimanche soir compris.");
    }

    private void chooseLearningDaysPerWeek(){
        int min=HifzSchedule.MIN_LEARNING_DAYS_PER_WEEK,max=HifzSchedule.MAX_LEARNING_DAYS_PER_WEEK;
        int current=prefs.learningDaysPerWeek();
        String[] labels=new String[max-min+1];
        int checkedIndex=0;
        for(int i=0;i<labels.length;i++){
            int days=min+i;
            labels[i]=days+" Apprentissage  ·  "+(6-days)+" Stabilisation";
            if(days==current)checkedIndex=i;
        }
        int[] selection={checkedIndex};
        new AlertDialog.Builder(this).setTitle("Séances d’Apprentissage par semaine")
            .setSingleChoiceItems(labels,checkedIndex,(dialog,which)->selection[0]=which)
            .setNegativeButton("Annuler",null)
            .setPositiveButton("Enregistrer",(dialog,which)->{
                int days=min+selection[0];
                if(days==current){applyLearningDaysPerWeek(days);return;}
                if(HifzClock.today().getDayOfWeek()==DayOfWeek.SUNDAY){applyLearningDaysPerWeek(days);return;}
                warnBeforeMidWeekCadenceChange(days);
            }).show();
    }

    /**
     * The weekly cadence (and its boule de neige accumulator) resets every Monday, with Sunday as
     * the reserved boundary day — changing the split mid-week leaves days already passed classified
     * under the old split while the rest of this same week would follow the new one, so a day still
     * due can be reclassified differently once actually caught up. This can't corrupt anything, so
     * it's a warning the learner can override, not a hard block until Sunday.
     */
    private void warnBeforeMidWeekCadenceChange(int days){
        new AlertDialog.Builder(this).setTitle("Changement en cours de semaine")
            .setMessage("Cette semaine a déjà commencé avec l’ancienne répartition. Changer maintenant peut "
                + "faire que le rattrapage d’un jour resté dû cette semaine ne corresponde plus au type de "
                + "séance prévu à l’origine. Continuer quand même ?")
            .setNegativeButton("Annuler",null)
            .setPositiveButton("Continuer",(dialog,which)->applyLearningDaysPerWeek(days))
            .show();
    }

    private void applyLearningDaysPerWeek(int days){
        if(!prefs.setLearningDaysPerWeek(days)){Toast.makeText(this,"Impossible d’enregistrer ce réglage.",Toast.LENGTH_LONG).show();return;}
        refreshWeeklyCadence();
        Toast.makeText(this,"Cadence mise à jour : "+days+" Apprentissage / "+(6-days)+" Stabilisation par semaine.",Toast.LENGTH_LONG).show();
    }

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
        if(GeometryRepository.ordinal(newStart)>GeometryRepository.ordinal(newEnd)){Toast.makeText(this,"Le début doit précéder la fin de la plage d’Apprentissage.",Toast.LENGTH_LONG).show();return;}
        if(isStart)prefs.setSabqiStart(verse);else prefs.setSabqiEnd(verse);
        int low=geometry.firstLineIndex(newStart),high=geometry.lastLineIndex(newEnd),cursor=prefs.sabqiLineCursor();
        if(cursor>=0&&(cursor<low||cursor>high)){
            new AlertDialog.Builder(this).setTitle("Position de l’Apprentissage hors de la plage")
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
        boolean rotationInvalid=!prefs.isRotationStartValid();
        boolean itqanInvalid=!prefs.isItqanCursorValid();
        boolean murajaahInvalid=!prefs.isMurajaahCursorValid();
        if(rotationInvalid||itqanInvalid||murajaahInvalid){
            new AlertDialog.Builder(this).setTitle("Position à vérifier")
                .setMessage("Une borne est hors du nouveau corpus. Aucun déplacement automatique n’a été effectué : Répétition et Révision resteront bloquées tant que la position n’est pas corrigée.")
                .setNegativeButton("Garder",null)
                .setPositiveButton("Repositionner au début",(d,w)->{
                    if(rotationInvalid){
                        VerseRef start=prefs.itqanWorkCorpus().getRanges().get(0).getStart();
                        prefs.setItqanRotationStart(start);
                    }
                    if(itqanInvalid){
                        VerseRef start=prefs.itqanWorkCorpus().getRanges().get(0).getStart();
                        prefs.setItqanCursor(start);
                    }
                    if(murajaahInvalid){
                        VerseRef start=prefs.murajaahCorpus().getRanges().get(0).getStart();
                        prefs.setMurajaahCursor(start);
                    }
                    refreshItqan();
                    Toast.makeText(this,"Positions repositionnées au début du nouveau corpus.",Toast.LENGTH_LONG).show();
                }).show();
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
        murajaahStatus.setText("Révision active "+HifzSchedule.ACTIVE_REVIEW_MINUTES+" min puis Entretien "
            +HifzSchedule.MAINTENANCE_MINUTES+" min · chaque soir · position "+prefs.murajaahCursor());
        murajaahStatus.setTextColor(Ui.MUTED);
        int weakCount=prefs.murajaahWeakVerses().size();
        TextView weakValue=Ui.settingValue(weakVersesSetting);
        if(weakValue!=null)weakValue.setText(weakCount==0?"Aucun":weakCount+" verset(s)");
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
        if(requestCode==REQUEST_BACKUP_EXPORT){
            if(resultCode!=RESULT_OK||data==null||data.getData()==null)return;
            writeBackupTo(data.getData());
            return;
        }
        if(requestCode==REQUEST_BACKUP_IMPORT){
            if(resultCode!=RESULT_OK||data==null||data.getData()==null)return;
            readBackupFrom(data.getData());
            return;
        }
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

    private void exportBackup(){
        Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE,"quran-hifz-sauvegarde-"+HifzClock.today()+".json");
        startActivityForResult(intent,REQUEST_BACKUP_EXPORT);
    }

    private void writeBackupTo(Uri uri){
        try(java.io.OutputStream out=getContentResolver().openOutputStream(uri)){
            if(out==null)throw new java.io.IOException("Impossible d’ouvrir le fichier.");
            out.write(HifzBackup.export(this).toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            Toast.makeText(this,"Sauvegarde exportée.",Toast.LENGTH_LONG).show();
        }catch(Exception error){
            Toast.makeText(this,"Échec de l’export : "+error.getMessage(),Toast.LENGTH_LONG).show();
        }
    }

    private void confirmImportBackup(){
        new AlertDialog.Builder(this).setTitle("Importer une sauvegarde")
            .setMessage("La progression actuelle sera entièrement remplacée par le contenu du fichier choisi. Continuer ?")
            .setNegativeButton("Annuler",null)
            .setPositiveButton("Choisir le fichier",(d,w)->{
                Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/json");
                startActivityForResult(Intent.createChooser(intent,"Choisir la sauvegarde"),REQUEST_BACKUP_IMPORT);
            }).show();
    }

    private void readBackupFrom(Uri uri){
        try(java.io.InputStream in=getContentResolver().openInputStream(uri)){
            if(in==null)throw new java.io.IOException("Impossible d’ouvrir le fichier.");
            java.io.ByteArrayOutputStream buffer=new java.io.ByteArrayOutputStream();
            byte[] chunk=new byte[8192];int n;
            while((n=in.read(chunk))>=0)buffer.write(chunk,0,n);
            org.json.JSONObject root=new org.json.JSONObject(buffer.toString(java.nio.charset.StandardCharsets.UTF_8.name()));
            int count=HifzBackup.restore(this,root);
            Toast.makeText(this,"Sauvegarde restaurée ("+count+" entrées). Relancez l’appli.",Toast.LENGTH_LONG).show();
            recreate();
        }catch(Exception error){
            Toast.makeText(this,"Échec de l’import : "+error.getMessage(),Toast.LENGTH_LONG).show();
        }
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
        List<String> quarantine=prefs.v6QuarantineLineIds();
        String state="Version : "+BuildConfig.VERSION_NAME+" ("+BuildConfig.VERSION_CODE+")"
            +"\nSchéma : "+prefs.schema()+"\nDébut programme : "+prefs.programStartDate()
            +"\nApprentissage : "+prefs.sabqiStart()+" → "+prefs.sabqiEnd()+" · ligne "+prefs.sabqiLineCursor()
            +"\nEstimation fin Apprentissage : "+apprentissageEtaSummary()
            +"\nPlages Acquises : "+prefs.itqanRanges().size()+"\nCorpus de travail : "+prefs.effectiveItqanRanges().size()+" plage(s)"
            +"\nPages promues : "+prefs.promotedRanges().size()+"\nÀ stabiliser : "+prefs.unconsolidatedPromotedRanges().size()
            +"\nDébut rotation : "+prefs.itqanRotationStart()+"\nPosition Stabilisation : "+prefs.itqanCursor()
            +"\nPosition Révision : "+prefs.murajaahCursor()
            +"\nPosition Révision active : "+prefs.activeMurajaahCursor()
            +"\nFile de Consolidation : "+prefs.recentSabqi().size()
            +"\nQuarantaine : "+quarantine.size()+" ligne(s)"
            +"\nEstimation fin Stabilisation : "+stabilizationEtaSummary()
            +"\nVitesse Révision : "+speedStore.maintenanceSummary()
            +"\nVitesse Consolidation : "+speedStore.consolidationSummary();
        AlertDialog.Builder dialog=new AlertDialog.Builder(this)
            .setTitle("Diagnostic Hifz").setMessage(state).setPositiveButton("Fermer",null);
        if(!quarantine.isEmpty()){
            dialog.setNeutralButton("Reprendre en Stabilisation",(d,w)->{
                int resolved=0;
                for(String lineId:new ArrayList<>(quarantine)){
                    try{
                        prefs.resolveV6Quarantine(lineId,HifzV6Migration.QuarantineResolution.RETURN_TO_STABILIZATION);
                        resolved++;
                    }catch(RuntimeException error){
                        Toast.makeText(this,"Résolution interrompue · réessayez depuis Diagnostic.",Toast.LENGTH_LONG).show();
                        break;
                    }
                }
                refreshAll();
                if(resolved>0)Toast.makeText(this,"Quarantaine résolue · "+resolved+" ligne(s) à reprendre en Stabilisation.",Toast.LENGTH_LONG).show();
            });
        }
        dialog.show();
    }

    /**
     * Stabilisation runs a fixed PreviewConfig.STABILIZATION_WEEKLY_LINES lines/week (3 sessions,
     * Tue/Thu/Sat, 8+7+7) regardless of learningDaysPerWeek, so remaining physical lines divided
     * by that pace, projected from today, is an honest — if optimistic, since a weekly unit can
     * end early at a surah boundary — estimate of when "à stabiliser" clears entirely.
     */
    private String stabilizationEtaSummary(){
        int remaining=prefs.stabilizationLinesRemaining(geometry);
        if(remaining==0)return "à jour";
        int weeks=(remaining+PreviewConfig.STABILIZATION_WEEKLY_LINES-1)/PreviewConfig.STABILIZATION_WEEKLY_LINES;
        return remaining+" ligne(s) restante(s) · ~"+weeks+" semaine(s) · ~"+HifzClock.today().plusWeeks(weeks);
    }

    /**
     * Apprentissage's own weekly pace — SABQI_LINES per session, learningDaysPerWeek sessions —
     * unlike Stabilisation's fixed cadence. Remaining lines run from the live sabqiLineCursor to
     * sabqiEnd and are bounded below by sabqiStart, so anything printed before it (Al-Fatiha,
     * under the default 2:75 start) is never counted: it was never part of this walk.
     */
    private String apprentissageEtaSummary(){
        int remaining=prefs.sabqiLinesRemaining(geometry);
        if(remaining==0)return "à jour";
        int weeklyPace=PreviewConfig.SABQI_LINES*prefs.learningDaysPerWeek();
        int weeks=(remaining+weeklyPace-1)/weeklyPace;
        return remaining+" ligne(s) restante(s) · ~"+weeks+" semaine(s) · ~"+HifzClock.today().plusWeeks(weeks);
    }

    private void confirmReset(){
        new AlertDialog.Builder(this).setTitle("Réinitialiser la progression Hifz ?")
            .setMessage("Efface la progression Hifz locale (Apprentissage, Stabilisation, Consolidation, Révision, positions et chronos). Le Mushaf, les Tafsir et l’audio installé ne sont pas modifiés.")
            .setNegativeButton("Annuler",null).setPositiveButton("Réinitialiser",(d,w)->{prefs.resetPreviewState();getSharedPreferences("hifz_preview_session_gates",MODE_PRIVATE).edit().clear().apply();refreshAll();}).show();
    }

}
