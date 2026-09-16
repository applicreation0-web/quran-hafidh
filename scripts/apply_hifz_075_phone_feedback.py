#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def load(rel):
    path = ROOT / rel
    return path, path.read_text(encoding="utf-8")


def save(path, text):
    path.write_text(text, encoding="utf-8")


def replace_once(text, old, new, label):
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected exactly one match, got {count}")
    return text.replace(old, new, 1)


# ---------------------------------------------------------------------------
# Settings: canonical cadence, two independent range lists, canonical Repères.
# ---------------------------------------------------------------------------
path, s = load("hifz-app/src/main/java/com/quransafeguard/hifz/preview/SettingsActivity.java")
s = replace_once(s,
    "private LinearLayout rangesBox, sabqiStartRow, sabqiEndRow, rotationSetting, hardAnchoringSetting, audioSetting;",
    "private LinearLayout stabilizationRangesBox, acquiredRangesBox, sabqiStartRow, sabqiEndRow, rotationSetting, hardAnchoringSetting, audioSetting;",
    "Settings range boxes")

old_block = '''        section(root,"Parcours");
        TextView protocol=Ui.text(this,"Lun/Mer/Ven · Leçon neuve + Reprise 30 min   ·   Mar/Jeu/Sam · Ancrage + Entretien 45 min   ·   Dim · Ancrage puis Consolidation + Entretien 45 min",11f,false);
        protocol.setTextColor(Ui.MUTED);protocol.setPadding(Ui.dp(this,4),0,Ui.dp(this,4),Ui.dp(this,4));root.addView(protocol);

        sabqiStartRow=Ui.settingRow(this,"Début de la plage à mémoriser",prefs.sabqiStart().toString(),v->chooseVerse("Début de la plage à mémoriser",prefs.sabqiStart(),verse->setSabqiBound(true,verse)));
        root.addView(sabqiStartRow);root.addView(Ui.divider(this));
        sabqiEndRow=Ui.settingRow(this,"Fin de la plage à mémoriser",prefs.sabqiEnd().toString(),v->chooseVerse("Fin de la plage à mémoriser",prefs.sabqiEnd(),verse->setSabqiBound(false,verse)));
        root.addView(sabqiEndRow);
        sabqiStatus=Ui.text(this,"",11f,false);sabqiStatus.setTextColor(Ui.MUTED);sabqiStatus.setPadding(Ui.dp(this,4),0,0,Ui.dp(this,4));root.addView(sabqiStatus);

        section(root,"Ancrage · plages");
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
        effectiveCorpusStatus=Ui.text(this,"",11f,false);
        effectiveCorpusStatus.setTextColor(Ui.MUTED);
        effectiveCorpusStatus.setPadding(Ui.dp(this,4),0,Ui.dp(this,4),Ui.dp(this,2));
        root.addView(effectiveCorpusStatus);

        section(root,"Entretien");
        murajaahStatus=Ui.text(this,"",12f,false);murajaahStatus.setPadding(Ui.dp(this,4),0,0,Ui.dp(this,2));root.addView(murajaahStatus);

        section(root,"Vitesses");
        root.addView(Ui.settingRow(this,"Entretien",speedStore.maintenanceSummary(),null));
        root.addView(Ui.divider(this));
        root.addView(Ui.settingRow(this,"Consolidation",speedStore.consolidationSummary(),null));
'''
new_block = '''        section(root,"Parcours");
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
'''
s = replace_once(s, old_block, new_block, "Settings main corpus block")

old_reperes = '''        section(root,"Repères");
        addRepere(root,"Leçon neuve","Cinq lignes jamais vues, mémorisées le matin avec dévoilement progressif du texte.");
        addRepere(root,"Reprise du soir","Les mêmes cinq lignes, répétées le soir pendant trente minutes.");
        addRepere(root,"Consolidation","Les leçons récentes, revues à tour de rôle avant leur passage vers l’Ancrage.");
        addRepere(root,"Ancrage","Une page entière travaillée en profondeur, jusqu’à pouvoir la réciter sans le texte.");
        addRepere(root,"Ancrage fractionné","Une page difficile répartie en sous-blocs successifs. Chaque sous-bloc est travaillé à ×35 ; la page n’est acquise qu’après le dernier bloc.");
        addRepere(root,"Entretien","Le parcours régulier de tout ce qui est acquis, texte caché, révélé seulement en cas de blocage.");
        addRepere(root,"J10","Garantie de fraîcheur : toute matière acquise doit être récitée à nouveau au plus tard tous les dix jours. J10 utilise les créneaux existants, il n’ajoute pas une séance parallèle.");
        addRepere(root,"En attente","Une page promue mais pas encore ancrée. Elle ne fait pas encore partie de l’entretien.");
        addRepere(root,"Acquis","Une page ancrée, qui circule dans l’entretien et entre dans la garantie J10.");
'''
new_reperes = '''        section(root,"Repères");
        addRepere(root,"Apprentissage","Action de mémorisation d’un nouveau passage. Après validation, le passage devient Appris.");
        addRepere(root,"Appris","État d’un passage mémorisé dont la prochaine action structurée est la Stabilisation.");
        addRepere(root,"Stabilisation","Action de renforcement de la matière Apprise sur les unités physiques du Mushaf. Après validation, elle devient Stabilisée.");
        addRepere(root,"Stabilisé","État d’un passage prêt pour la Consolidation.");
        addRepere(root,"Consolidation","Action de regroupement progressif de une à trois unités. Une Consolidation validée fait passer la matière vers Acquis.");
        addRepere(root,"Acquis","État d’un passage qui entre dans la Révision et dans la garantie J10.");
        addRepere(root,"Révision","Rotation régulière de la matière Acquise, avec révélation seulement en cas de besoin.");
        addRepere(root,"J10","Garantie de fraîcheur : toute matière Acquise doit être revue au plus tard tous les dix jours. J10 utilise les créneaux existants et n’ajoute pas de séance parallèle.");
        addRepere(root,"Report souple","Une séance manquée reste due au prochain créneau sans échec, sans double quota automatique et sans déplacement silencieux du curseur.");
'''
s = replace_once(s, old_reperes, new_reperes, "Settings repères")
s = replace_once(s,
    "private void refreshAll(){refreshSabqi();refreshRanges();refreshItqan();refreshHardAnchoring();refreshEffectiveItqanCorpus();refreshMurajaah();refreshJ10();refreshAudio();}",
    "private void refreshAll(){refreshRangeLists();refreshSabqi();refreshItqan();refreshHardAnchoring();refreshEffectiveItqanCorpus();refreshMurajaah();refreshJ10();refreshAudio();}",
    "Settings refreshAll")

start = s.index("    private void refreshRanges(){")
end = s.index("    private void refreshItqan(){", start)
range_methods = '''    private void refreshRangeLists(){
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

'''
s = s[:start] + range_methods + s[end:]

# Canonical visible vocabulary in Settings only. Internal identifiers remain untouched.
replacements = {
    "Position Ancrage": "Position Stabilisation",
    "Entretien · ": "Révision · ",
    "page d’Ancrage": "unité de Stabilisation",
    "corpus d’Ancrage": "corpus de Stabilisation",
    "Sourates difficiles à ancrer": "Sourates difficiles à stabiliser",
    "Corpus d’ancrage · ": "Corpus de travail · ",
    "En attente d’ancrage · ": "À stabiliser · ",
    "plage d’ancrage": "plage de Stabilisation",
    "Mar/Jeu/Sam/Dim · 45 min · position ": "Sam/Dim · Révision · 45 min · position ",
    "Leçon neuve : ": "Apprentissage : ",
    "Plages d’ancrage : ": "Plages Acquises : ",
    "Corpus d’ancrage : ": "Corpus de travail : ",
    "En attente d’ancrage : ": "À stabiliser : ",
    "Position Entretien : ": "Position Révision : ",
    "Vitesse Entretien : ": "Vitesse Révision : ",
    "leçons, ancrage, entretien, positions et chronos": "Apprentissage, Stabilisation, Consolidation, Révision, positions et chronos",
    "Repositionner la Leçon neuve au début ": "Repositionner l’Apprentissage au début ",
}
for old, new in replacements.items():
    s = s.replace(old, new)
save(path, s)


# ---------------------------------------------------------------------------
# HifzPrefs: atomic manual-range reconciliation, no cursor reset, no fake J10 dates.
# ---------------------------------------------------------------------------
path, s = load("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java")
anchor = '''    public boolean setItqanRanges(List<VerseRange> ranges) {
        if (ranges == null || ranges.isEmpty()) return false;
        List<VerseRange> normalized = normalizeRanges(ranges);
        return p.edit().putString("itqanRanges", rangesJson(normalized)).commit();
    }
'''
insert = anchor + '''
    /** Schema-6 manual configuration: replace only the configured pending Stabilisation ranges. */
    public boolean setV6StabilizationRanges(List<VerseRange> ranges, GeometryRepository geometry) {
        return setV6ManualRanges(itqanRanges(), ranges, geometry);
    }

    /** Schema-6 manual configuration: replace only the configured Acquired base ranges. */
    public boolean setV6AcquiredRanges(List<VerseRange> ranges, GeometryRepository geometry) {
        return setV6ManualRanges(ranges, unconsolidatedPromotedRanges(), geometry);
    }

    private boolean setV6ManualRanges(List<VerseRange> acquiredRanges,
                                      List<VerseRange> stabilizationRanges,
                                      GeometryRepository geometry) {
        if (geometry == null) throw new IllegalArgumentException("Géométrie Mushaf requise.");
        validateV6ManualRanges(acquiredRanges, stabilizationRanges);
        List<VerseRange> acquiredNormalized = normalizeRanges(acquiredRanges);
        List<VerseRange> stabilizationNormalized = normalizeRanges(stabilizationRanges);

        ArrayList<GeometryRepository.LineMeta> allLines = new ArrayList<>();
        for (int i = 0; i < geometry.lineCount(); i++) allLines.add(geometry.line(i));
        LinkedHashSet<String> oldManualAcquired = new LinkedHashSet<>(
            CorpusLinePolicy.ownedLineIds(itqanRanges(), allLines));
        LinkedHashSet<String> oldManualStabilization = new LinkedHashSet<>(
            CorpusLinePolicy.ownedLineIds(unconsolidatedPromotedRanges(), allLines));
        LinkedHashSet<String> newManualAcquired = new LinkedHashSet<>(
            CorpusLinePolicy.ownedLineIds(acquiredNormalized, allLines));
        LinkedHashSet<String> newManualStabilization = new LinkedHashSet<>(
            CorpusLinePolicy.ownedLineIds(stabilizationNormalized, allLines));

        synchronized (V6_STATE_LOCK) {
            requireSchema6ProgressionState();
            LinkedHashSet<String> quarantine = v6LineIdSet("v6QuarantineLineIds");
            LinkedHashSet<String> legacyPartial = v6LineIdSet("v6LegacyPartialAcquiredLineIds");
            for (String lineId : newManualAcquired) {
                if (quarantine.contains(lineId) || legacyPartial.contains(lineId))
                    throw new IllegalStateException("Cette plage contient une ligne en quarantaine.");
            }
            for (String lineId : newManualStabilization) {
                if (quarantine.contains(lineId) || legacyPartial.contains(lineId))
                    throw new IllegalStateException("Cette plage contient une ligne en quarantaine.");
            }

            if ((itqanRep() > 0 || itqanBlockIndex() > 0) && itqanUnitStart() != null && itqanUnitEnd() != null) {
                if (!rangeCoveredBy(stabilizationNormalized, itqanUnitStart(), itqanUnitEnd())) {
                    throw new IllegalStateException("Terminez la Stabilisation en cours avant de retirer sa plage.");
                }
            }

            LinkedHashSet<String> learned = v6LineIdSet("v6LearnedLineIds");
            LinkedHashSet<String> stabilized = v6LineIdSet("v6StabilizedLineIds");
            LinkedHashSet<String> acquired = v6LineIdSet("v6AcquiredCreditLineIds");
            learned.removeAll(oldManualStabilization);
            acquired.removeAll(oldManualAcquired);
            learned.addAll(newManualStabilization);
            acquired.addAll(newManualAcquired);
            for (String lineId : newManualStabilization) {
                acquired.remove(lineId);
                stabilized.remove(lineId);
            }
            for (String lineId : newManualAcquired) {
                learned.remove(lineId);
                stabilized.remove(lineId);
            }

            LinkedHashMap<String, Long> activeJ10 = v6EpochDayMap("v6ActiveJ10LastReviewed");
            LinkedHashSet<String> unknownDue = v6LineIdSet("v6UnknownDueLineIds");
            activeJ10.entrySet().removeIf(entry -> !acquired.contains(entry.getKey()));
            unknownDue.retainAll(acquired);
            for (String lineId : acquired) {
                if (!activeJ10.containsKey(lineId)) unknownDue.add(lineId);
            }
            for (String lineId : newManualStabilization) {
                activeJ10.remove(lineId);
                unknownDue.remove(lineId);
            }

            List<VerseRange> consolidatedPromotions = new ArrayList<>(promotedRanges());
            for (VerseRange oldPending : unconsolidatedPromotedRanges()) {
                consolidatedPromotions = subtractCoverage(
                    consolidatedPromotions, oldPending.getStart(), oldPending.getEndInclusive());
            }
            ArrayList<VerseRange> promotedNext = new ArrayList<>(consolidatedPromotions);
            promotedNext.addAll(stabilizationNormalized);

            SharedPreferences.Editor editor = p.edit()
                .putString("itqanRanges", rangesJson(acquiredNormalized))
                .putString("promotedRanges", rangesJson(normalizeRanges(promotedNext)))
                .putString("unconsolidatedPromotedRanges", rangesJson(stabilizationNormalized))
                .putBoolean("anchoringQueueInitialized", false)
                .putString("v6LearnedLineIds", lineIdsJson(learned))
                .putString("v6StabilizedLineIds", lineIdsJson(stabilized))
                .putString("v6AcquiredCreditLineIds", lineIdsJson(acquired))
                .putString("v6ActiveJ10LastReviewed", epochDayMapJson(activeJ10))
                .putString("v6UnknownDueLineIds", lineIdsJson(unknownDue));
            return editor.commit();
        }
    }

    private void validateV6ManualRanges(List<VerseRange> acquiredRanges,
                                        List<VerseRange> stabilizationRanges) {
        if (acquiredRanges == null || acquiredRanges.isEmpty())
            throw new IllegalArgumentException("Au moins une plage Acquise est requise.");
        if (stabilizationRanges == null)
            throw new IllegalArgumentException("La liste À stabiliser est requise.");
        validateNoRangeOverlap(acquiredRanges, "Plages Acquises");
        validateNoRangeOverlap(stabilizationRanges, "Plages à stabiliser");
        for (VerseRange acquired : acquiredRanges) {
            for (VerseRange stabilization : stabilizationRanges) {
                if (rangesOverlap(acquired, stabilization))
                    throw new IllegalArgumentException("Une plage ne peut pas être à la fois Acquise et À stabiliser.");
            }
        }
    }

    private static void validateNoRangeOverlap(List<VerseRange> ranges, String label) {
        for (int i = 0; i < ranges.size(); i++) {
            if (ranges.get(i) == null) throw new IllegalArgumentException(label + " : plage absente.");
            for (int j = i + 1; j < ranges.size(); j++) {
                if (ranges.get(j) == null || rangesOverlap(ranges.get(i), ranges.get(j)))
                    throw new IllegalArgumentException(label + " : chevauchement interdit.");
            }
        }
    }

    private static boolean rangesOverlap(VerseRange left, VerseRange right) {
        int leftStart = GeometryRepository.ordinal(left.getStart());
        int leftEnd = GeometryRepository.ordinal(left.getEndInclusive());
        int rightStart = GeometryRepository.ordinal(right.getStart());
        int rightEnd = GeometryRepository.ordinal(right.getEndInclusive());
        return leftStart <= rightEnd && rightStart <= leftEnd;
    }

    private static boolean rangeCoveredBy(List<VerseRange> ranges, VerseRef start, VerseRef endInclusive) {
        for (VerseRange range : ranges) {
            if (GeometryRepository.ordinal(range.getStart()) <= GeometryRepository.ordinal(start)
                    && GeometryRepository.ordinal(range.getEndInclusive()) >= GeometryRepository.ordinal(endInclusive)) return true;
        }
        return false;
    }
'''
s = replace_once(s, anchor, insert, "HifzPrefs range editor insertion")
save(path, s)


# ---------------------------------------------------------------------------
# Weekly dashboard: canonical weekday actions; no fixed Sunday Consolidation.
# ---------------------------------------------------------------------------
path, s = load("hifz-app/src/main/java/com/quransafeguard/hifz/preview/WeeklyDashboardPlanner.java")
if "import com.quransafeguard.hifz.core.CadenceAction;" not in s:
    s = s.replace("import com.quransafeguard.hifz.core.DailyPlan;\n", "import com.quransafeguard.hifz.core.CadenceAction;\nimport com.quransafeguard.hifz.core.DailyPlan;\n")
start = s.index("    List<Row> week(LocalDate today){")
end = s.index("    private Projection projectMurajaah", start)
week_method = '''    List<Row> week(LocalDate today){
        ArrayList<Row> out=new ArrayList<>();
        int sabqiCursor=currentSabqiCursor();
        VerseRef murajaahCursor=prefs.murajaahCursor();
        EligibleCorpus murajaahCorpus=prefs.murajaahCorpus();

        prefs.currentAnchoringEntry(geometry);
        List<AnchoringQueue.Entry> projectedAnchoring=AnchoringQueue.visitOrder(
            prefs.anchoringQueue(),prefs.anchoringQueueIndex());
        int projectedAnchoringIndex=0;
        int projectedItqanBlockIndex=prefs.itqanBlockIndex();

        for(LocalDate date:window(today)){
            if(date.isBefore(prefs.programStartDate())){
                out.add(new Row(date,day(date,today),"—","—","Parcours non démarré"));
                continue;
            }
            CadenceAction action=HifzSchedule.INSTANCE.actionFor(date.getDayOfWeek());
            String morning="—",evening="—",state="À faire";
            switch(action){
                case LEARNING:{
                    DashboardLedger.Record morningActual=ledger.find(date,HifzSessionActivity.SABQI);
                    DashboardLedger.Record eveningActual=ledger.find(date,HifzSessionActivity.SABQI_TODAY_REVIEW);
                    boolean morningJ10=morningActual==null&&hostBudgetStore!=null&&hostBudgetStore.isSlotConsumed(HifzSessionActivity.SABQI,date);
                    boolean eveningJ10=eveningActual==null&&hostBudgetStore!=null&&hostBudgetStore.isSlotConsumed(HifzSessionActivity.SABQI_TODAY_REVIEW,date);
                    GeometryRepository.FiveLineBlock block=safeSabqi(sabqiCursor);
                    if(morningJ10)morning="J10 · créneau utilisé";
                    else if(morningActual!=null)morning="✓ "+compact(morningActual.label);
                    else if(block==null)morning="Apprentissage · plage à vérifier";
                    else{
                        morning="Apprentissage · "+range(block.startVerse,block.endVerse)+" · 5 lignes";
                        sabqiCursor=block.endLineIndex+1;
                    }
                    if(eveningJ10)evening="J10 · créneau utilisé";
                    else if(eveningActual!=null)evening="✓ "+compact(eveningActual.label);
                    else evening="Apprentissage · reprise · "+HifzSchedule.EVENING_REVIEW_MINUTES+" min";
                    boolean m=morningActual!=null||morningJ10,e=eveningActual!=null||eveningJ10;
                    state=m&&e?"Validé":m?"Reprise à faire":"À faire";
                    break;
                }
                case STABILIZATION:{
                    DashboardLedger.Record actual=ledger.find(date,HifzSessionActivity.ITQAN);
                    boolean j10=actual==null&&hostBudgetStore!=null&&hostBudgetStore.isSlotConsumed(HifzSessionActivity.ITQAN,date);
                    if(j10)morning="J10 · créneau utilisé";
                    else if(actual!=null)morning="✓ "+compact(actual.label);
                    else if(date.equals(today)&&prefs.anchoringDeferredToday())morning="Stabilisation · unité reportée";
                    else if(projectedAnchoringIndex>=projectedAnchoring.size())morning="Stabilisation · aucune unité à stabiliser";
                    else{
                        AnchoringQueue.Entry entry=projectedAnchoring.get(projectedAnchoringIndex);
                        VerseRef start=GeometryRepository.parseVerse(entry.start),end=GeometryRepository.parseVerse(entry.end);
                        List<VerseRef> verses=geometry.versesForRange(start,end);
                        boolean fractionated=prefs.isFractionatedUnit(verses);
                        int reps=PreviewConfig.itqanTotalReps(entry.protocol);
                        if(fractionated){
                            int[] segments=geometry.surahSegmentLineCounts(start,end);
                            int blocks=Math.max(1,PreviewConfig.fractionatedBlockCount(segments));
                            int block=Math.max(0,Math.min(projectedItqanBlockIndex,blocks-1));
                            morning="Stabilisation · "+range(start,end)+" · bloc "+(block+1)+"/"+blocks+" · ×"+reps;
                            block++;if(block>=blocks){projectedItqanBlockIndex=0;projectedAnchoringIndex++;}else projectedItqanBlockIndex=block;
                        }else{
                            morning="Stabilisation · "+range(start,end)+" · ×"+reps;
                            projectedItqanBlockIndex=0;projectedAnchoringIndex++;
                        }
                    }
                    state=(actual!=null||j10)?"Validé":"À faire";
                    break;
                }
                case REVISION:{
                    DashboardLedger.Record actual=ledger.find(date,HifzSessionActivity.MURAJAAH);
                    boolean j10=actual==null&&hostBudgetStore!=null&&hostBudgetStore.isSlotConsumed(HifzSessionActivity.MURAJAAH,date);
                    if(j10)evening="J10 · créneau utilisé";
                    else if(actual!=null)evening="✓ "+compact(actual.label);
                    else{
                        Projection projected=projectMurajaah(murajaahCursor,murajaahCorpus,HifzSchedule.MAINTENANCE_MINUTES);
                        evening="Révision · "+projected.label;
                        murajaahCursor=projected.next;
                    }
                    state=(actual!=null||j10)?"Validé":"À faire";
                    break;
                }
                default:throw new IllegalStateException("Action de cadence inconnue");
            }
            out.add(new Row(date,day(date,today),morning,evening,state));
        }
        return out;
    }

'''
s = s[:start] + week_method + s[end:]
save(path, s)


# ---------------------------------------------------------------------------
# Main home: canonical labels + oldest due cadence drives the launcher.
# ---------------------------------------------------------------------------
path, s = load("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java")
if "import com.quransafeguard.hifz.core.CadenceAction;" not in s:
    s = s.replace("import com.quransafeguard.hifz.core.DailyPlan;\n", "import com.quransafeguard.hifz.core.CadenceAction;\nimport com.quransafeguard.hifz.core.DailyPlan;\n")
if "import com.quransafeguard.hifz.core.ScheduledCadence;" not in s:
    s = s.replace("import com.quransafeguard.hifz.core.SessionKind;\n", "import com.quransafeguard.hifz.core.SessionKind;\nimport com.quransafeguard.hifz.core.ScheduledCadence;\n")
if "import java.util.LinkedHashSet;" not in s:
    s = s.replace("import java.util.ArrayList;\n", "import java.util.ArrayList;\nimport java.util.LinkedHashSet;\n")
s = s.replace('Ui.modeCard(this, "", "Leçon neuve"', 'Ui.modeCard(this, "", "Apprentissage"')
s = s.replace('Ui.modeCard(this, "", "Ancrage"', 'Ui.modeCard(this, "", "Stabilisation"')
s = s.replace('Ui.modeCard(this, "", "Entretien"', 'Ui.modeCard(this, "", "Révision"')

start = s.index("    private DailyPlan planFor(LocalDate date) {")
end = s.index("    private void refreshRecentSabqiAdvisory()", start)
main_runtime = '''    private void openMode(String mode) { openMode(mode,HifzClock.today()); }

    private void openMode(String mode,LocalDate scheduledDate) {
        Intent intent=new Intent(this,HifzSessionActivity.class).putExtra(HifzSessionActivity.EXTRA_MODE,mode);
        if(scheduledDate!=null)intent.putExtra(HifzSessionActivity.EXTRA_SCHEDULED_DATE,scheduledDate.toString());
        startActivity(intent);
    }

    private boolean modeComplete(LocalDate date,String mode){
        if(ledger.find(date,mode)!=null)return true;
        return hostBudgetStore!=null&&hostBudgetStore.isSlotConsumed(mode,date);
    }

    private boolean cadenceComplete(LocalDate date){
        CadenceAction action=HifzSchedule.INSTANCE.actionFor(date.getDayOfWeek());
        switch(action){
            case LEARNING:return modeComplete(date,HifzSessionActivity.SABQI)
                &&modeComplete(date,HifzSessionActivity.SABQI_TODAY_REVIEW);
            case STABILIZATION:return modeComplete(date,HifzSessionActivity.ITQAN);
            case REVISION:return modeComplete(date,HifzSessionActivity.MURAJAAH);
            default:return false;
        }
    }

    private ScheduledCadence nextDueCadence(LocalDate todayDate){
        LinkedHashSet<LocalDate> completed=new LinkedHashSet<>();
        LocalDate cursor=prefs.programStartDate();
        while(!cursor.isAfter(todayDate)){
            if(cadenceComplete(cursor))completed.add(cursor);
            cursor=cursor.plusDays(1);
        }
        return HifzSchedule.INSTANCE.nextDue(prefs.programStartDate(),todayDate,completed);
    }

    private String nextMode(ScheduledCadence due){
        if(due==null)return null;
        LocalDate date=due.getScheduledDate();
        switch(due.getAction()){
            case LEARNING:
                if(!modeComplete(date,HifzSessionActivity.SABQI))return HifzSessionActivity.SABQI;
                return !modeComplete(date,HifzSessionActivity.SABQI_TODAY_REVIEW)?HifzSessionActivity.SABQI_TODAY_REVIEW:null;
            case STABILIZATION:return !modeComplete(date,HifzSessionActivity.ITQAN)?HifzSessionActivity.ITQAN:null;
            case REVISION:return !modeComplete(date,HifzSessionActivity.MURAJAAH)?HifzSessionActivity.MURAJAAH:null;
            default:return null;
        }
    }

    private void openToday() {
        ScheduledCadence due=nextDueCadence(HifzClock.today());
        String mode=nextMode(due);
        if(mode!=null)openMode(mode,due.getScheduledDate());
    }

    private void refreshToday() {
        GeometryRepository g=geometry;
        if(g==null){today.setText("…");return;}
        LocalDate current=HifzClock.today();
        if(current.isBefore(prefs.programStartDate())){
            today.setText("Parcours non démarré");todayAction.setEnabled(false);return;
        }
        ScheduledCadence due=nextDueCadence(current);
        if(due==null){today.setText("Programme à jour");todayAction.setEnabled(false);return;}
        String mode=nextMode(due);
        if(mode==null){today.setText("Programme à jour");todayAction.setEnabled(false);return;}
        String prefix=due.getOverdue()?"Report "+due.getScheduledDate()+" · ":"";
        String detail;
        try{
            if(HifzSessionActivity.SABQI.equals(mode)){
                int cursor=prefs.sabqiLineCursor();if(cursor<0)cursor=g.firstLineIndex(prefs.sabqiStart());
                GeometryRepository.FiveLineBlock b=g.fiveLineBlock(cursor);
                detail="Apprentissage · "+shortRange(b.startVerse,b.endVerse)+" · 5 lignes";
            }else if(HifzSessionActivity.SABQI_TODAY_REVIEW.equals(mode)){
                detail="Apprentissage · reprise · "+HifzSchedule.EVENING_REVIEW_MINUTES+" min";
            }else if(HifzSessionActivity.ITQAN.equals(mode)){
                detail=anchoringTodayDetail(prefs,g);
            }else if(HifzSessionActivity.MURAJAAH.equals(mode)){
                detail="Révision · "+HifzSchedule.MAINTENANCE_MINUTES+" min";
            }else detail="Parcours à vérifier";
        }catch(RuntimeException error){detail="Parcours à vérifier";}
        today.setText(prefix+detail);todayAction.setEnabled(true);
    }

    static String anchoringTodayDetail(HifzPrefs prefs,GeometryRepository geometry){
        AnchoringQueue.Entry entry=prefs.inProgressAnchoringEntry();
        if(entry==null)entry=prefs.currentAnchoringEntry(geometry);
        if(entry==null)return "Stabilisation · aucune unité à stabiliser";
        VerseRef start=GeometryRepository.parseVerse(entry.start),end=GeometryRepository.parseVerse(entry.end);
        boolean fractionated=prefs.isFractionatedUnit(geometry.versesForRange(start,end));
        int reps=PreviewConfig.itqanTotalReps(entry.protocol);
        if(!fractionated)return "Stabilisation · "+shortRange(start,end)+" · ×"+reps;
        int[] segments=geometry.surahSegmentLineCounts(start,end);
        int blocks=Math.max(1,PreviewConfig.fractionatedBlockCount(segments));
        int block=Math.max(0,Math.min(prefs.itqanBlockIndex(),blocks-1));
        return "Stabilisation · "+shortRange(start,end)+" · bloc "+(block+1)+"/"+blocks+" · ×"+reps;
    }

'''
s = s[:start] + main_runtime + s[end:]
save(path, s)


# ---------------------------------------------------------------------------
# Structured sessions: canonical names only; internal mode constants unchanged.
# ---------------------------------------------------------------------------
path, s = load("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java")
# User-visible French labels; English/internal symbols are intentionally untouched.
for old,new in [
    ("Leçon neuve","Apprentissage"),
    ("Reprise du soir","Apprentissage"),
    ("Ancrage fractionné","Stabilisation"),
    ("Ancrage","Stabilisation"),
    ("ancrage","stabilisation"),
    ("Entretien","Révision"),
    ("entretien","révision"),
]:
    s=s.replace(old,new)
save(path,s)

print("PHONE_FEEDBACK_PATCH_OK")
