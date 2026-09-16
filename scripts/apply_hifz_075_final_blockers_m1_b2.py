from pathlib import Path


def replace_once(path: str, old: str, new: str) -> None:
    p = Path(path)
    text = p.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{path}: expected exactly one match, found {count}")
    p.write_text(text.replace(old, new, 1), encoding="utf-8")

# M1: a corrupt persisted Consolidation must be visible and route to recovery instead of disappearing.
main = "hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java"
replace_once(main,
    "    private volatile GeometryRepository geometry;\n    private TextView today;",
    "    private volatile GeometryRepository geometry;\n    private boolean consolidationNeedsAttention;\n    private TextView today;")
replace_once(main,
'''    private boolean progressionConsolidationDue(GeometryRepository g){
        if(g==null)return false;
        if(HifzClock.today().toString().equals(prefs.lastRecentSabqiReviewDate()))return false;
        try{
            ConsolidationCycleEngine engine=new ConsolidationCycleEngine();
            ConsolidationCycleEngine.Session open=prefs.restoreConsolidationSession(
                engine,ConsolidationCycleEngine.Family.STABILIZATION);
            return open!=null || !prefs.stabilizedConsolidationUnits(g,3).isEmpty();
        }catch(RuntimeException error){
            android.util.Log.e("QuranHifz","Unable to evaluate progression Consolidation",error);
            return false;
        }
    }''',
'''    private boolean progressionConsolidationDue(GeometryRepository g){
        consolidationNeedsAttention=false;
        if(g==null)return false;
        if(HifzClock.today().toString().equals(prefs.lastRecentSabqiReviewDate()))return false;
        try{
            ConsolidationCycleEngine engine=new ConsolidationCycleEngine();
            ConsolidationCycleEngine.Session open=prefs.restoreConsolidationSession(
                engine,ConsolidationCycleEngine.Family.STABILIZATION);
            return open!=null || !prefs.stabilizedConsolidationUnits(g,3).isEmpty();
        }catch(RuntimeException error){
            consolidationNeedsAttention=true;
            android.util.Log.e("QuranHifz","Unable to evaluate progression Consolidation",error);
            return false;
        }
    }''')
replace_once(main,
'''        ScheduledCadence due=nextDueCadence(current);
        String mode=nextMode(due);
        if(mode==null)return;
        LocalDate scheduled=HifzSessionActivity.RECENT_SABQI_REVIEW.equals(mode)''',
'''        ScheduledCadence due=nextDueCadence(current);
        String mode=nextMode(due);
        if(consolidationNeedsAttention){
            startActivity(new Intent(this,SettingsActivity.class));
            return;
        }
        if(mode==null)return;
        LocalDate scheduled=HifzSessionActivity.RECENT_SABQI_REVIEW.equals(mode)''')
replace_once(main,
'''        ScheduledCadence due=nextDueCadence(current);
        String mode=nextMode(due);
        if(mode==null){today.setText("Programme à jour");todayAction.setEnabled(false);return;}''',
'''        ScheduledCadence due=nextDueCadence(current);
        String mode=nextMode(due);
        if(consolidationNeedsAttention){
            today.setText("Consolidation · état à vérifier");
            todayAction.setEnabled(true);
            return;
        }
        if(mode==null){today.setText("Programme à jour");todayAction.setEnabled(false);return;}''')

# B2: expose quarantine IDs as a defensive immutable snapshot; existing resolver remains authoritative.
prefs = "hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java"
replace_once(prefs,
'''    void resolveV6Quarantine(
            String lineId,''',
'''    List<String> v6QuarantineLineIds() {
        synchronized (V6_STATE_LOCK) {
            requireSchema6ProgressionState();
            return Collections.unmodifiableList(new ArrayList<>(v6LineIdSet("v6QuarantineLineIds")));
        }
    }

    void resolveV6Quarantine(
            String lineId,''')

# B2 UI: conservative, retry-safe recovery. Never invent ACQUIRED credit: quarantined lines return to Stabilization.
settings = "hifz-app/src/main/java/com/quransafeguard/hifz/preview/SettingsActivity.java"
replace_once(settings,
'''    private void showDiagnostic(){
        String state="Schéma : "+prefs.schema()+"\\nDébut programme : "+prefs.programStartDate()
            +"\\nApprentissage : "+prefs.sabqiStart()+" → "+prefs.sabqiEnd()+" · ligne "+prefs.sabqiLineCursor()
            +"\\nPlages Acquises : "+prefs.itqanRanges().size()+"\\nCorpus de travail : "+prefs.effectiveItqanRanges().size()+" plage(s)"
            +"\\nPages promues : "+prefs.promotedRanges().size()+"\\nÀ stabiliser : "+prefs.unconsolidatedPromotedRanges().size()
            +"\\nDébut rotation : "+prefs.itqanRotationStart()+"\\nPosition Stabilisation : "+prefs.itqanCursor()
            +"\\nPosition Révision : "+prefs.murajaahCursor()
            +"\\nFile de Consolidation : "+prefs.recentSabqi().size()
            +"\\nVitesse Révision : "+speedStore.maintenanceSummary()
            +"\\nVitesse Consolidation : "+speedStore.consolidationSummary();
        new AlertDialog.Builder(this).setTitle("Diagnostic Hifz").setMessage(state).setPositiveButton("Fermer",null).show();
    }''',
'''    private void showDiagnostic(){
        List<String> quarantine=prefs.v6QuarantineLineIds();
        String state="Schéma : "+prefs.schema()+"\\nDébut programme : "+prefs.programStartDate()
            +"\\nApprentissage : "+prefs.sabqiStart()+" → "+prefs.sabqiEnd()+" · ligne "+prefs.sabqiLineCursor()
            +"\\nPlages Acquises : "+prefs.itqanRanges().size()+"\\nCorpus de travail : "+prefs.effectiveItqanRanges().size()+" plage(s)"
            +"\\nPages promues : "+prefs.promotedRanges().size()+"\\nÀ stabiliser : "+prefs.unconsolidatedPromotedRanges().size()
            +"\\nDébut rotation : "+prefs.itqanRotationStart()+"\\nPosition Stabilisation : "+prefs.itqanCursor()
            +"\\nPosition Révision : "+prefs.murajaahCursor()
            +"\\nFile de Consolidation : "+prefs.recentSabqi().size()
            +"\\nQuarantaine : "+quarantine.size()+" ligne(s)"
            +"\\nVitesse Révision : "+speedStore.maintenanceSummary()
            +"\\nVitesse Consolidation : "+speedStore.consolidationSummary();
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
    }''')

print("Applied final blockers M1/B2: visible Consolidation recovery + conservative quarantine exit.")
