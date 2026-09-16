from pathlib import Path
import re


def replace_exact(path, old, new, expected=1):
    p = Path(path)
    text = p.read_text(encoding="utf-8")
    count = text.count(old)
    if count == 0 and new in text:
        return False
    if count != expected:
        raise SystemExit(f"{path}: expected {expected} occurrences, found {count}: {old[:80]!r}")
    p.write_text(text.replace(old, new), encoding="utf-8")
    return True


def replace_regex(path, pattern, replacement, expected=1):
    p = Path(path)
    text = p.read_text(encoding="utf-8")
    updated, count = re.subn(pattern, replacement, text, flags=re.S)
    if count == 0 and replacement in text:
        return False
    if count != expected:
        raise SystemExit(f"{path}: expected {expected} regex matches, found {count}: {pattern[:100]!r}")
    p.write_text(updated, encoding="utf-8")
    return True


SETTINGS = "hifz-app/src/main/java/com/quransafeguard/hifz/preview/SettingsActivity.java"
MAIN = "hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java"
SESSION = "hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java"
UI = "hifz-app/src/main/java/com/quransafeguard/hifz/preview/Ui.java"

# ---- Settings: canonical cadence, two range views, canonical vocabulary ----
replace_exact(
    SETTINGS,
    "private LinearLayout rangesBox, sabqiStartRow, sabqiEndRow, rotationSetting, hardAnchoringSetting, audioSetting;",
    "private LinearLayout stabilizationRangesBox, acquiredRangesBox, sabqiStartRow, sabqiEndRow, rotationSetting, hardAnchoringSetting, audioSetting;",
)
replace_exact(
    SETTINGS,
    'TextView protocol=Ui.text(this,"Lun/Mer/Ven · Leçon neuve + Reprise 30 min   ·   Mar/Jeu/Sam · Ancrage + Entretien 45 min   ·   Dim · Ancrage puis Consolidation + Entretien 45 min",11f,false);',
    'TextView protocol=Ui.text(this,"Lun/Mer/Ven · Apprentissage   ·   Mar/Jeu · Stabilisation   ·   Sam/Dim · Révision",11f,false);',
)
old_ranges_block = '''        section(root,"Ancrage · plages");
        rangesBox=Ui.column(this);rangesBox.setPadding(0,0,0,0);root.addView(rangesBox);
        root.addView(Ui.divider(this));
        root.addView(Ui.settingRow(this,"Ajouter","Nouvelle plage",v->chooseRange(null,-1)));
        itqanStatus=Ui.text(this,"",11f,false);itqanStatus.setTextColor(Ui.MUTED);itqanStatus.setPadding(Ui.dp(this,4),0,0,Ui.dp(this,2));root.addView(itqanStatus);
        rotationSetting=Ui.settingRow(this,"Début de rotation d’ancrage",prefs.itqanRotationStart().toString(),
            v->chooseVerse("Début de rotation d’ancrage",prefs.itqanRotationStart(),this::setRotationStart));
        root.addView(rotationSetting);
        root.addView(Ui.divider(this));
        hardAnchoringSetting=Ui.settingRow(this,"Sourates difficiles à ancrer","0 sourate",v->showHardAnchoringSelector());
        root.addView(hardAnchoringSetting);

        section(root,"Ancrage · corpus");
'''
new_ranges_block = '''        section(root,"Plages à stabiliser");
        stabilizationRangesBox=Ui.column(this);stabilizationRangesBox.setPadding(0,0,0,0);root.addView(stabilizationRangesBox);

        section(root,"Plages acquises");
        acquiredRangesBox=Ui.column(this);acquiredRangesBox.setPadding(0,0,0,0);root.addView(acquiredRangesBox);

        itqanStatus=Ui.text(this,"",11f,false);itqanStatus.setTextColor(Ui.MUTED);itqanStatus.setPadding(Ui.dp(this,4),0,0,Ui.dp(this,2));root.addView(itqanStatus);
        rotationSetting=Ui.settingRow(this,"Début de rotation de stabilisation",prefs.itqanRotationStart().toString(),
            v->chooseVerse("Début de rotation de stabilisation",prefs.itqanRotationStart(),this::setRotationStart));
        root.addView(rotationSetting);
        root.addView(Ui.divider(this));
        hardAnchoringSetting=Ui.settingRow(this,"Sourates difficiles à stabiliser","0 sourate",v->showHardAnchoringSelector());
        root.addView(hardAnchoringSetting);

        section(root,"État du corpus");
'''
replace_exact(SETTINGS, old_ranges_block, new_ranges_block)
replace_exact(SETTINGS, 'section(root,"Entretien");', 'section(root,"Révision");')
replace_exact(SETTINGS, 'root.addView(Ui.settingRow(this,"Entretien",speedStore.maintenanceSummary(),null));', 'root.addView(Ui.settingRow(this,"Révision",speedStore.maintenanceSummary(),null));')
old_reperes = '''        addRepere(root,"Leçon neuve","Cinq lignes jamais vues, mémorisées le matin avec dévoilement progressif du texte.");
        addRepere(root,"Reprise du soir","Les mêmes cinq lignes, répétées le soir pendant trente minutes.");
        addRepere(root,"Consolidation","Les leçons récentes, revues à tour de rôle avant leur passage vers l’Ancrage.");
        addRepere(root,"Ancrage","Une page entière travaillée en profondeur, jusqu’à pouvoir la réciter sans le texte.");
        addRepere(root,"Ancrage fractionné","Une page difficile répartie en sous-blocs successifs. Chaque sous-bloc est travaillé à ×35 ; la page n’est acquise qu’après le dernier bloc.");
        addRepere(root,"Entretien","Le parcours régulier de tout ce qui est acquis, texte caché, révélé seulement en cas de blocage.");
        addRepere(root,"J10","Garantie de fraîcheur : toute matière acquise doit être récitée à nouveau au plus tard tous les dix jours. J10 utilise les créneaux existants, il n’ajoute pas une séance parallèle.");
        addRepere(root,"En attente","Une page promue mais pas encore ancrée. Elle ne fait pas encore partie de l’entretien.");
        addRepere(root,"Acquis","Une page ancrée, qui circule dans l’entretien et entre dans la garantie J10.");
'''
new_reperes = '''        addRepere(root,"Apprentissage","Action de mémorisation d’un nouveau bloc de 5 lignes. La reprise du même bloc reste dans cette étape.");
        addRepere(root,"Appris","État obtenu après l’Apprentissage. L’étape suivante est la Stabilisation.");
        addRepere(root,"Stabilisation","Action de renforcement de la matière Apprise selon le protocole de répétition et de masquage.");
        addRepere(root,"Stabilisé","État obtenu après la Stabilisation. L’étape suivante est la Consolidation.");
        addRepere(root,"Consolidation","Action groupée sur 1 à 3 unités stabilisées. Elle dépend de la progression et n’est pas attachée à un jour fixe.");
        addRepere(root,"Acquis","État obtenu après la Consolidation. La matière entre dans la Révision et dans la garantie J10.");
        addRepere(root,"Révision","Rotation régulière du corpus Acquis. Une absence entraîne un report souple, sans double quota automatique.");
        addRepere(root,"J10","Garantie de fraîcheur : la matière Acquise doit être revue au plus tard tous les dix jours. J10 utilise les créneaux existants et n’ajoute pas de séance parallèle.");
'''
replace_exact(SETTINGS, old_reperes, new_reperes)
old_refresh_ranges = '''    private void refreshRanges(){
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
'''
new_refresh_ranges = '''    private void refreshRanges(){
        renderRangeList(stabilizationRangesBox,prefs.unconsolidatedPromotedRanges());
        renderRangeList(acquiredRangesBox,prefs.murajaahCorpus().getRanges());
    }

    private void renderRangeList(LinearLayout box,List<VerseRange> ranges){
        box.removeAllViews();
        if(ranges==null||ranges.isEmpty()){
            TextView empty=Ui.text(this,"Aucune plage",11.5f,false);empty.setTextColor(Ui.MUTED);empty.setPadding(Ui.dp(this,4),Ui.dp(this,4),Ui.dp(this,4),Ui.dp(this,4));box.addView(empty);return;
        }
        for(int i=0;i<ranges.size();i++){
            VerseRange range=ranges.get(i);LinearLayout row=Ui.row(this);row.setMinimumHeight(Ui.dp(this,42));
            TextView label=Ui.text(this,"Plage "+(i+1),12f,true);label.setPadding(Ui.dp(this,4),0,Ui.dp(this,8),0);row.addView(label);
            TextView value=Ui.text(this,range.getStart()+" → "+range.getEndInclusive(),11.5f,false);value.setTextColor(Ui.MUTED);Ui.weight(value,1);row.addView(value);box.addView(row);
            if(i+1<ranges.size())box.addView(Ui.divider(this));
        }
    }
'''
replace_exact(SETTINGS, old_refresh_ranges, new_refresh_ranges)
for old, new in [
    ('Position de la leçon hors de la plage', 'Position de l’Apprentissage hors de la plage'),
    ('Repositionner la Leçon neuve au début ', 'Repositionner l’Apprentissage au début '),
    ('Au moins une plage d’ancrage doit rester définie.', 'Au moins une plage acquise de départ doit rester définie.'),
    ('Impossible d’enregistrer les plages d’ancrage.', 'Impossible d’enregistrer les plages acquises de départ.'),
    ('Position Ancrage · ', 'Position Stabilisation · '),
    ('   ·   Entretien · ', '   ·   Révision · '),
    ('Terminez la page d’Ancrage en cours avant de modifier ce réglage.', 'Terminez l’unité de Stabilisation en cours avant de modifier ce réglage.'),
    ('Aucune sourate dans le corpus d’Ancrage.', 'Aucune sourate dans le corpus de Stabilisation.'),
    ('Sourates difficiles à ancrer', 'Sourates difficiles à stabiliser'),
    ('Corpus d’ancrage · ', 'Corpus de Stabilisation · '),
    ('En attente d’ancrage · ', 'À stabiliser · '),
    ('Ce verset n’appartient à aucune plage d’ancrage.', 'Ce verset n’appartient à aucune plage de Stabilisation.'),
    ('Mar/Jeu/Sam/Dim · 45 min · position ', 'Sam/Dim · 45 min · position '),
    ('\\nLeçon neuve : ', '\\nApprentissage : '),
    ('\\nPlages d’ancrage : ', '\\nPlages acquises de départ : '),
    ('\\nCorpus d’ancrage : ', '\\nCorpus de Stabilisation : '),
    ('\\nEn attente d’ancrage : ', '\\nÀ stabiliser : '),
    ('\\nPosition Ancrage : ', '\\nPosition Stabilisation : '),
    ('\\nPosition Entretien : ', '\\nPosition Révision : '),
    ('\\nVitesse Entretien : ', '\\nVitesse Révision : '),
    ('(leçons, ancrage, entretien, positions et chronos)', '(Apprentissage, Stabilisation, Révision, positions et chronos)'),
]:
    replace_exact(SETTINGS, old, new)

# ---- Main screen: canonical quick actions + real soft carry-over resolver ----
replace_exact(MAIN, 'import com.quransafeguard.hifz.core.DailyPlan;\n', 'import com.quransafeguard.hifz.core.CadenceAction;\nimport com.quransafeguard.hifz.core.DailyPlan;\nimport com.quransafeguard.hifz.core.ScheduledCadence;\n')
replace_exact(MAIN, 'import java.util.ArrayList;\nimport java.util.List;\n', 'import java.util.ArrayList;\nimport java.util.LinkedHashSet;\nimport java.util.List;\nimport java.util.Set;\n')
replace_exact(MAIN, 'LinearLayout sabqi = Ui.modeCard(this, "", "Leçon neuve", v -> openMode(HifzSessionActivity.SABQI));', 'LinearLayout sabqi = Ui.modeCard(this, "", "Apprentissage", v -> openMode(HifzSessionActivity.SABQI));')
replace_exact(MAIN, 'LinearLayout itqan = Ui.modeCard(this, "", "Ancrage", v -> openMode(HifzSessionActivity.ITQAN));', 'LinearLayout itqan = Ui.modeCard(this, "", "Stabilisation", v -> openMode(HifzSessionActivity.ITQAN));')
replace_exact(MAIN, 'LinearLayout murajaah = Ui.modeCard(this, "", "Entretien", v -> openMode(HifzSessionActivity.MURAJAAH));', 'LinearLayout murajaah = Ui.modeCard(this, "", "Révision", v -> openMode(HifzSessionActivity.MURAJAAH));')
old_open = '''    private void openMode(String mode) {
        startActivity(new Intent(this, HifzSessionActivity.class).putExtra(HifzSessionActivity.EXTRA_MODE, mode));
    }

    private void openToday() {
        String mode = firstIncompleteMode(HifzClock.today());
        if (mode != null) openMode(mode);
    }
'''
new_open = '''    private void openMode(String mode) { openMode(mode, null); }

    private void openMode(String mode, LocalDate scheduledDate) {
        Intent intent=new Intent(this,HifzSessionActivity.class).putExtra(HifzSessionActivity.EXTRA_MODE,mode);
        if(scheduledDate!=null)intent.putExtra(HifzSessionActivity.EXTRA_SCHEDULED_DATE,scheduledDate.toString());
        startActivity(intent);
    }

    private ScheduledCadence nextDue(LocalDate todayDate) {
        Set<LocalDate> completed=completedCadenceDates(todayDate);
        return HifzSchedule.INSTANCE.nextDue(prefs.programStartDate(),todayDate,completed);
    }

    private Set<LocalDate> completedCadenceDates(LocalDate through) {
        LinkedHashSet<LocalDate> out=new LinkedHashSet<>();
        LocalDate date=prefs.programStartDate();
        while(!date.isAfter(through)){
            if(isCadenceComplete(date,HifzSchedule.INSTANCE.actionFor(date.getDayOfWeek())))out.add(date);
            date=date.plusDays(1);
        }
        return out;
    }

    private boolean isCadenceComplete(LocalDate date,CadenceAction action) {
        switch(action){
            case LEARNING:return sessionComplete(date,HifzSessionActivity.SABQI)&&sessionComplete(date,HifzSessionActivity.SABQI_TODAY_REVIEW);
            case STABILIZATION:return sessionComplete(date,HifzSessionActivity.ITQAN);
            case REVISION:return sessionComplete(date,HifzSessionActivity.MURAJAAH);
            default:return false;
        }
    }

    private boolean sessionComplete(LocalDate date,String mode) {
        return ledger.find(date,mode)!=null||(hostBudgetStore!=null&&hostBudgetStore.isSlotConsumed(mode,date));
    }

    private String modeForCadence(ScheduledCadence due) {
        if(due.getAction()==CadenceAction.LEARNING){
            return sessionComplete(due.getScheduledDate(),HifzSessionActivity.SABQI)
                ?HifzSessionActivity.SABQI_TODAY_REVIEW:HifzSessionActivity.SABQI;
        }
        if(due.getAction()==CadenceAction.STABILIZATION)return HifzSessionActivity.ITQAN;
        return HifzSessionActivity.MURAJAAH;
    }

    private void openToday() {
        ScheduledCadence due=nextDue(HifzClock.today());
        if(due!=null)openMode(modeForCadence(due),due.getScheduledDate());
    }
'''
replace_exact(MAIN, old_open, new_open)
replace_regex(
    MAIN,
    r'''    private void refreshToday\(\) \{.*?\n    \}\n\n    static String anchoringTodayDetail''',
    '''    private void refreshToday() {
        GeometryRepository g=geometry;
        if(g==null){today.setText("…");return;}
        LocalDate now=HifzClock.today();
        if(now.isBefore(prefs.programStartDate())){
            today.setText("Parcours non démarré");todayAction.setEnabled(false);return;
        }
        ScheduledCadence due=nextDue(now);
        if(due==null){today.setText("Programme à jour");todayAction.setEnabled(false);return;}
        String prefix=due.getOverdue()?"Report "+due.getScheduledDate()+" · ":"";
        String detail;
        try{
            if(due.getAction()==CadenceAction.LEARNING){
                if(!sessionComplete(due.getScheduledDate(),HifzSessionActivity.SABQI)){
                    int cursor=prefs.sabqiLineCursor();if(cursor<0)cursor=g.firstLineIndex(prefs.sabqiStart());
                    GeometryRepository.FiveLineBlock b=g.fiveLineBlock(cursor);
                    detail="Apprentissage · "+shortRange(b.startVerse,b.endVerse)+" · 5 lignes";
                }else detail="Apprentissage · reprise · "+HifzSchedule.EVENING_REVIEW_MINUTES+" min";
            }else if(due.getAction()==CadenceAction.STABILIZATION){
                detail=anchoringTodayDetail(prefs,g).replace("Matin · ","");
            }else detail="Révision · "+HifzSchedule.MAINTENANCE_MINUTES+" min";
        }catch(RuntimeException error){detail="Parcours à vérifier";}
        today.setText(prefix+detail);todayAction.setEnabled(true);
    }

    static String anchoringTodayDetail'''
)
replace_exact(MAIN, 'return "Matin · Ancrage · aucune page en attente";', 'return "Matin · Stabilisation · aucune unité en attente";')
replace_exact(MAIN, 'return "Matin · Ancrage · " + shortRange(start, end) + " · ×" + reps;', 'return "Matin · Stabilisation · " + shortRange(start, end) + " · ×" + reps;')
replace_exact(MAIN, 'return "Matin · Ancrage fractionné · " + shortRange(start, end)', 'return "Matin · Stabilisation · " + shortRange(start, end)')
replace_regex(
    MAIN,
    r'''    private void refreshRecentSabqiAdvisory\(\) \{.*?\n    \}\n\n    private void refreshDashboard''',
    '''    private void refreshRecentSabqiAdvisory() {
        if(recentSabqiAdvisory==null)return;
        recentSabqiAdvisory.setText("");
        recentSabqiAdvisory.setVisibility(View.GONE);
    }

    private void refreshDashboard'''
)

# ---- Structured session title: canonical labels, internal mode constants untouched ----
old_display = '''    private String displayModeName(){
        if (SABQI.equals(mode)) return "Leçon neuve";
        if (SABQI_TODAY_REVIEW.equals(mode)) return "Reprise du soir";
        if (ITQAN.equals(mode)) return "Ancrage";
        if (RECENT_SABQI_REVIEW.equals(mode)) return "Consolidation";
        return "Entretien";
    }
'''
new_display = '''    private String displayModeName(){
        if (SABQI.equals(mode)) return "Apprentissage";
        if (SABQI_TODAY_REVIEW.equals(mode)) return "Apprentissage";
        if (ITQAN.equals(mode)) return "Stabilisation";
        if (RECENT_SABQI_REVIEW.equals(mode)) return "Consolidation";
        return "Révision";
    }
'''
replace_exact(SESSION, old_display, new_display)

# ---- UI protocol cues understand canonical labels; legacy internal aliases remain accepted ----
old_cue = '''        String cue = lower.contains("leçon") || lower.contains("lecon") || lower.contains("sabqi") ? "5 lignes"
            : lower.contains("reprise") ? "30 min"
            : lower.contains("consolidation") ? "30 min"
            : lower.contains("ancrage") || lower.contains("itq") ? "Répétitions"
            : lower.contains("entretien") || lower.contains("mur") ? "45 min" : "";
'''
new_cue = '''        String cue = lower.contains("apprentissage") || lower.contains("leçon") || lower.contains("lecon") || lower.contains("sabqi") ? "5 lignes"
            : lower.contains("reprise") ? "30 min"
            : lower.contains("consolidation") ? "Cycle 1–3"
            : lower.contains("stabilisation") || lower.contains("ancrage") || lower.contains("itq") ? "Répétitions"
            : lower.contains("révision") || lower.contains("revision") || lower.contains("entretien") || lower.contains("mur") ? "45 min" : "";
'''
replace_exact(UI, old_cue, new_cue)

print("PHONE_FEEDBACK_PATCH_APPLIED")
