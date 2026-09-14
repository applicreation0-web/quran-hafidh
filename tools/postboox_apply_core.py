from pathlib import Path


def replace_once(path: str, old: str, new: str) -> None:
    p = Path(path)
    text = p.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{path}: expected exactly one match, got {count}\n---OLD---\n{old[:500]}")
    p.write_text(text.replace(old, new, 1), encoding="utf-8")


activity = "hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java"
prefs = "hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java"

# Leçon neuve: total reveal count is the sole success authority.
replace_once(activity, '''        if (rep >= PreviewConfig.SABQI_TOTAL_REPS) {
            awaitingValidation = true;
            sessionCompleted = true;
            clock.pause();
            currentMask = 0;
            program.setText("Leçon neuve · " + sabqiBlock.verseLabel() + " · 5 lignes");
            progress.setText("37/37 · prêt à valider · révélations " + prefs.sabqiAssisted());
            showCurrent();
            addRoundAction("✓","Valider",v->validateSabqi());
            return;
        }
''', '''        if (rep >= PreviewConfig.SABQI_TOTAL_REPS) {
            boolean assistancePassed = StructuredSessionPolicy.assistancePasses(prefs.sabqiAssisted());
            awaitingValidation = assistancePassed;
            sessionCompleted = true;
            clock.pause();
            currentMask = 0;
            program.setText("Leçon neuve · " + sabqiBlock.verseLabel() + " · 5 lignes");
            progress.setText(assistancePassed
                ? "37/37 · prêt à valider · révélations " + prefs.sabqiAssisted()
                : "37/37 · à renforcer · révélations " + prefs.sabqiAssisted() + " · maximum 2");
            showCurrent();
            if (assistancePassed) addRoundAction("✓","Valider",v->validateSabqi());
            else addRoundAction("↻","Reprendre",v->restartSabqiAfterAssistance());
            return;
        }
''')

replace_once(activity, '''    private void validateSabqi() {
        if (sabqiBlock==null || prefs.sabqiRep()<PreviewConfig.SABQI_TOTAL_REPS) return;
        String label="Leçon neuve · "+sabqiBlock.verseLabel()+" · 37/37 · révélations "+prefs.sabqiAssisted();
''', '''    private void validateSabqi() {
        if (sabqiBlock==null || prefs.sabqiRep()<PreviewConfig.SABQI_TOTAL_REPS) return;
        if (!StructuredSessionPolicy.assistancePasses(prefs.sabqiAssisted())) {
            restartSabqiAfterAssistance();
            return;
        }
        String label="Leçon neuve · "+sabqiBlock.verseLabel()+" · 37/37 · révélations "+prefs.sabqiAssisted();
''')

replace_once(activity, '''        mushaf.cycleCompleted();
        renderMode();
    }

    /** Calendar/attendance promotion; display order never determines mastery order. */
''', '''        mushaf.cycleCompleted();
        renderMode();
    }

    private void restartSabqiAfterAssistance() {
        if (!prefs.setSabqiProgress(0, 0)) {
            onError("Impossible de relancer ce bloc de Leçon neuve.");
            return;
        }
        lastCheckpointBucket = -1L;
        clock.reset();
        prefs.setElapsedFor(mode, 0L);
        awaitingValidation = false;
        sessionCompleted = false;
        clock.resume();
        mushaf.cycleCompleted();
        renderMode();
    }

    /** Calendar/attendance promotion; display order never determines mastery order. */
''')

# 45 minutes is an Entretien target, not a hard stop. Keep the clock running beyond it.
replace_once(activity, '''    private void onTimedSessionLimit(long elapsed) {
        if (timedSessionLimitReached) return;
        timedSessionLimitReached = true;
        long saved = clock.pause();
        long effectiveElapsed = Math.max(elapsed, saved);
        prefs.setElapsedFor(mode, effectiveElapsed);
        String today = sessionDate.toString();
''', '''    private void onTimedSessionLimit(long elapsed) {
        if (timedSessionLimitReached) return;
        timedSessionLimitReached = true;
        if (MURAJAAH.equals(mode)) {
            prefs.setElapsedFor(mode, Math.max(elapsed, clock.elapsedMs()));
            updateMurajaahProgress();
            return;
        }
        long saved = clock.pause();
        long effectiveElapsed = Math.max(elapsed, saved);
        prefs.setElapsedFor(mode, effectiveElapsed);
        String today = sessionDate.toString();
''')

replace_once(activity, '''        } else if (MURAJAAH.equals(mode)) {
            progress.setText(murajaahActualEnd == null
                ? "Durée atteinte · touchez le dernier verset."
                : "Fin réelle · " + murajaahActualEnd);
            eink.local(progress, prefs);
            if (murajaahFinishButton != null) murajaahFinishButton.setEnabled(murajaahActualEnd != null);
        }
''', '''        }
''')

# Ancrage: total reveals, including fractionated blocks, are checked before any credit/advance.
replace_once(activity, '''        if(rep>=itqanTargetReps){
            awaitingValidation=true;sessionCompleted=true;clock.pause();currentMask=0;
            program.setText(itqanProgramLabel());
            progress.setText(itqanTargetReps+"/"+itqanTargetReps+" · prêt à valider · révélations finales "+prefs.itqanFinalReveals());
            showCurrent();addRoundAction("✓","Valider",v->validateItqan());return;
        }
''', '''        if(rep>=itqanTargetReps){
            boolean assistancePassed = StructuredSessionPolicy.assistancePasses(prefs.itqanAssisted());
            awaitingValidation=assistancePassed;sessionCompleted=true;clock.pause();currentMask=0;
            program.setText(itqanProgramLabel());
            progress.setText(assistancePassed
                ? itqanTargetReps+"/"+itqanTargetReps+" · prêt à valider · révélations "+prefs.itqanAssisted()
                : itqanTargetReps+"/"+itqanTargetReps+" · à renforcer · révélations "+prefs.itqanAssisted()+" · maximum 2");
            showCurrent();
            if (assistancePassed) addRoundAction("✓","Valider",v->validateItqan());
            else addRoundAction("↻","Reprendre",v->restartItqanAfterAssistance());
            return;
        }
''')

replace_once(activity, '''    private void validateItqan(){
        if(itqanUnit==null||anchoringEntry==null||prefs.itqanRep()<itqanTargetReps)return;
        String metrics = anchoringInstrumentation();
''', '''    private void validateItqan(){
        if(itqanUnit==null||anchoringEntry==null||prefs.itqanRep()<itqanTargetReps)return;
        if (!StructuredSessionPolicy.assistancePasses(prefs.itqanAssisted())) {
            restartItqanAfterAssistance();
            return;
        }
        String metrics = anchoringInstrumentation();
''')

replace_once(activity, '''        } else if (!PreviewConfig.itqanValidationPassed(prefs.itqanFinalReveals())) {
            metricsStore.recordAnchoring("échec · "+itqanUnit.start+" → "+itqanUnit.end+" · "+metrics);
            if (!prefs.failAndDeferAnchoring(itqanUnit.start, itqanUnit.end)) {
                onError("Impossible d’enregistrer le report de cette page d’Ancrage.");
                return;
            }
            awaitingValidation=false;
            sessionCompleted=false;
            restartAnchoringClockAfterDeferral();
            mushaf.cycleCompleted();
            renderMode();
            return;
        }
''', '''        }
''')

replace_once(activity, '''        String label=(fractionatedItqan ? "Ancrage fractionné" : "Ancrage")+" · "+itqanUnit.start+" → "+itqanUnit.end+" · ×"+itqanTargetReps
            +" · révélations finales "+prefs.itqanFinalReveals()+" · "+metrics;
''', '''        String label=(fractionatedItqan ? "Ancrage fractionné" : "Ancrage")+" · "+itqanUnit.start+" → "+itqanUnit.end+" · ×"+itqanTargetReps
            +" · révélations "+prefs.itqanAssisted()+" · "+metrics;
''')

replace_once(activity, '''        awaitingValidation=false;closeClockForCompletedSession();mushaf.cycleCompleted();renderMode();
    }

    private void renderMurajaah(){
''', '''        awaitingValidation=false;closeClockForCompletedSession();mushaf.cycleCompleted();renderMode();
    }

    private void restartItqanAfterAssistance() {
        if (itqanUnit == null || !prefs.setItqanProgress(0, 0, 0, itqanUnit.start, itqanUnit.end)) {
            onError("Impossible de relancer ce bloc d’Ancrage.");
            return;
        }
        lastCheckpointBucket = -1L;
        clock.reset();
        prefs.setElapsedFor(mode, 0L);
        awaitingValidation = false;
        sessionCompleted = false;
        clock.resume();
        mushaf.cycleCompleted();
        renderMode();
    }

    private void renderMurajaah(){
''')

# Entretien: explicit endpoint, early validation, free page turning, persisted page, no planned-end clamp.
old_murajaah = '''        sessionCompleted = false;
        timedSessionLimitReached = PreviewConfig.timedSessionComplete(clock.elapsedMs(), targetMinutes());
        EligibleCorpus corpus = prefs.murajaahCorpus();
        int lines = HifzCadence.targetLines(targetMinutes(), speedStore.maintenanceSecondsPerLine());
        murajaahPlan = geometry.planEligibleLines(prefs.murajaahCursor(), lines, corpus);
        murajaahActualEnd = prefs.murajaahActualEnd();
        currentPage = geometry.pageForVerse(murajaahPlan.start);
        currentSelection = murajaahPlan.traversalVerses;
        currentLineIds = Collections.emptyList();
        currentMask = 0;
        program.setText("Entretien · " + murajaahPlan.start + " → " + murajaahPlan.actualPlannedEnd);
        progress.setText(timedSessionLimitReached
            ? (murajaahActualEnd == null ? "Durée atteinte · touchez le dernier verset." : "Fin réelle · " + murajaahActualEnd)
            : "Corpus acquis · " + targetMinutes() + " min");
        showCurrent();
        if (murajaahActualEnd != null && murajaahPlan.traversalVerses.contains(murajaahActualEnd)) {
            mushaf.setSelection(Collections.singletonList(murajaahActualEnd),
                geometry.lineIdsForVerseRange(murajaahActualEnd, murajaahActualEnd));
        } else if (murajaahActualEnd != null) {
            murajaahActualEnd = null;
            prefs.setMurajaahActualEnd(null);
        }
        LinearLayout validateAction = Ui.roundAction(this, "", "Valider", v -> finishMurajaah());
        murajaahFinishButton = (Button) validateAction.getChildAt(0);
        murajaahFinishButton.setEnabled(timedSessionLimitReached && murajaahActualEnd != null);
        actions.addView(validateAction);
'''
new_murajaah = '''        sessionCompleted = false;
        timedSessionLimitReached = PreviewConfig.timedSessionComplete(clock.elapsedMs(), targetMinutes());
        EligibleCorpus corpus = prefs.murajaahCorpus();
        int lines = HifzCadence.targetLines(targetMinutes(), speedStore.maintenanceSecondsPerLine());
        murajaahPlan = geometry.planEligibleLines(prefs.murajaahCursor(), lines, corpus);
        murajaahActualEnd = prefs.murajaahActualEnd();
        if (murajaahActualEnd != null && !corpus.contains(murajaahActualEnd)) {
            murajaahActualEnd = null;
            prefs.setMurajaahActualEnd(null);
        }
        int savedPage = prefs.murajaahPage();
        currentPage = savedPage >= 1 && savedPage <= 604
            ? savedPage : geometry.pageForVerse(murajaahPlan.start);
        currentSelection = Collections.emptyList();
        currentLineIds = Collections.emptyList();
        currentMask = 0;
        program.setText("Entretien · objectif " + murajaahPlan.start + " → " + murajaahPlan.actualPlannedEnd);
        updateMurajaahProgress();
        showCurrent();
        restoreMurajaahEndpointSelectionOnCurrentPage();
        LinearLayout validateAction = Ui.roundAction(this, "", "Valider jusqu’ici", v -> finishMurajaah());
        murajaahFinishButton = (Button) validateAction.getChildAt(0);
        murajaahFinishButton.setEnabled(true);
        actions.addView(validateAction);
'''
replace_once(activity, old_murajaah, new_murajaah)

replace_once(activity, '''    private void finishMurajaah(){
        if (!timedSessionLimitReached) {
            Toast.makeText(this, "La durée prévue n’est pas encore atteinte.", Toast.LENGTH_LONG).show();
            return;
        }
        if (murajaahActualEnd == null) {
''', '''    private void finishMurajaah(){
        if (!StructuredSessionPolicy.murajaahCanValidate(murajaahActualEnd != null)) {
''')

replace_once(activity, '''    private int countMurajaahLinesThrough(VerseRef through){
        if (murajaahPlan == null || through == null) return 0;
        if (MurajaahTraversalPolicy.endpointAmbiguous(murajaahPlan.traversalVerses, through)) return 0;
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        for (VerseRef verse : murajaahPlan.traversalVerses) {
            ids.addAll(geometry.lineIdsForVerseRange(verse, verse));
            if (verse.equals(through)) return ids.size();
        }
        throw new IllegalArgumentException("Fin d’Entretien hors du parcours planifié : " + through);
    }

    private void checkpointMurajaah(long elapsed){
        if (!MURAJAAH.equals(mode) || sessionCompleted) return;
        prefs.setMurajaahActualEnd(murajaahActualEnd);
    }

    @Override public void onVerseTap(VerseRef verse){
        if(MURAJAAH.equals(mode)&&murajaahPlan!=null&&murajaahPlan.traversalVerses.contains(verse)){
            murajaahActualEnd=verse;
            progress.setText("Fin réelle · "+verse);
            eink.local(progress, prefs);
            mushaf.setSelection(Collections.singletonList(verse),geometry.lineIdsForVerseRange(verse,verse));
            checkpointMurajaah(clock.elapsedMs());
            if (murajaahFinishButton != null) murajaahFinishButton.setEnabled(timedSessionLimitReached);
        }
    }
''', '''    private int countMurajaahLinesThrough(VerseRef through){
        if (murajaahPlan == null || through == null) return 0;
        EligibleCorpus corpus = prefs.murajaahCorpus();
        if (!corpus.contains(through)) throw new IllegalArgumentException("Fin d’Entretien hors du corpus acquis : " + through);
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        VerseRef cursor = murajaahPlan.start;
        for (int visited = 0; visited < 6236; visited++) {
            ids.addAll(geometry.lineIdsForVerseRange(cursor, cursor));
            if (cursor.equals(through)) return ids.size();
            cursor = corpus.next(cursor);
            if (cursor.equals(murajaahPlan.start)) break;
        }
        throw new IllegalArgumentException("Fin d’Entretien inaccessible depuis le curseur courant : " + through);
    }

    private void updateMurajaahProgress() {
        if (!MURAJAAH.equals(mode) || progress == null || murajaahPlan == null) return;
        String target = "objectif " + targetMinutes() + " min";
        if (murajaahActualEnd == null) {
            progress.setText((timedSessionLimitReached ? target + " atteint" : target)
                + " · touchez le dernier verset réellement révisé");
        } else {
            progress.setText("Fin réelle · " + murajaahActualEnd
                + (timedSessionLimitReached ? " · " + target + " atteint" : " · validation possible à tout moment"));
        }
        eink.local(progress, prefs);
    }

    private void restoreMurajaahEndpointSelectionOnCurrentPage() {
        if (!MURAJAAH.equals(mode) || murajaahActualEnd == null || mushaf == null) return;
        if (geometry.pageForVerse(murajaahActualEnd) != currentPage) return;
        mushaf.setSelection(Collections.singletonList(murajaahActualEnd),
            geometry.lineIdsForVerseRange(murajaahActualEnd, murajaahActualEnd));
    }

    private void checkpointMurajaah(long elapsed){
        if (!MURAJAAH.equals(mode) || sessionCompleted) return;
        prefs.setMurajaahActualEnd(murajaahActualEnd);
        prefs.setMurajaahPage(currentPage);
    }

    @Override public void onVerseTap(VerseRef verse){
        if (!MURAJAAH.equals(mode) || murajaahPlan == null) return;
        EligibleCorpus corpus = prefs.murajaahCorpus();
        if (!corpus.contains(verse)) {
            Toast.makeText(this, "Ce verset n’appartient pas encore au corpus acquis.", Toast.LENGTH_SHORT).show();
            return;
        }
        murajaahActualEnd=verse;
        updateMurajaahProgress();
        mushaf.setSelection(Collections.singletonList(verse),geometry.lineIdsForVerseRange(verse,verse));
        checkpointMurajaah(clock.elapsedMs());
    }
''')

replace_once(activity, '''    @Override public void onReady(){if(!hasShown)showCurrent();}
    @Override public void onError(String message){Toast.makeText(this,message,Toast.LENGTH_LONG).show();}
    @Override public void onPageShown(int page){currentPage=page;}
    @Override protected void onResume(){
        super.onResume();
        if(clock==null)return;
        clock.syncPersistedElapsed(prefs.elapsedFor(mode));
        if(!sessionCompleted&&!awaitingValidation&&!timedSessionLimitReached)clock.resume();
    }
''', '''    @Override public void onReady(){
        if(StructuredSessionPolicy.shouldInitialReaderShow(hasShown, sessionCompleted))showCurrent();
    }
    @Override public void onError(String message){Toast.makeText(this,message,Toast.LENGTH_LONG).show();}
    @Override public void onPageShown(int page){
        currentPage=page;
        if(MURAJAAH.equals(mode)&&!sessionCompleted){
            prefs.setMurajaahPage(page);
            restoreMurajaahEndpointSelectionOnCurrentPage();
        }
    }
    @Override protected void onResume(){
        super.onResume();
        if(clock==null)return;
        clock.syncPersistedElapsed(prefs.elapsedFor(mode));
        if(!sessionCompleted&&!awaitingValidation&&(!timedSessionLimitReached||MURAJAAH.equals(mode)))clock.resume();
    }
''')

# Persist the current Entretien page without a schema bump; clear it on successful completion.
replace_once(prefs, '''    public void setMurajaahActualEnd(VerseRef value) {
        p.edit().putString("murajaahActualEnd", value == null ? "" : value.toString()).apply();
    }

    public boolean completeMurajaah(VerseRef nextCursor, String date, String label) {
''', '''    public void setMurajaahActualEnd(VerseRef value) {
        p.edit().putString("murajaahActualEnd", value == null ? "" : value.toString()).apply();
    }
    public int murajaahPage() { return p.getInt("murajaahPage", 0); }
    public void setMurajaahPage(int value) {
        if (value >= 1 && value <= 604) p.edit().putInt("murajaahPage", value).apply();
        else p.edit().remove("murajaahPage").apply();
    }

    public boolean completeMurajaah(VerseRef nextCursor, String date, String label) {
''')

replace_once(prefs, '''            .putLong("murajaahElapsedMs", 0L)
            .putString("murajaahActualEnd", "")
            .putString("lastMurajaahDate", date)
''', '''            .putLong("murajaahElapsedMs", 0L)
            .putString("murajaahActualEnd", "")
            .remove("murajaahPage")
            .putString("lastMurajaahDate", date)
''')

print("POSTBOOX_CORE_PATCH_OK")
