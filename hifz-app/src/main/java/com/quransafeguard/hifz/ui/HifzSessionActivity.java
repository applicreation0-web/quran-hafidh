package com.quransafeguard.hifz.ui;

import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.quransafeguard.hifz.core.EligibleCorpus;
import com.quransafeguard.hifz.core.QuranCanon;
import com.quransafeguard.hifz.core.VerseRange;
import com.quransafeguard.hifz.core.VerseRef;
import com.quransafeguard.hifz.data.GeometryRepository;
import com.quransafeguard.hifz.data.LineGeometryRepository;
import com.quransafeguard.hifz.reader.ReaderSurface;
import com.quransafeguard.hifz.storage.HifzProgressStore;
import com.quransafeguard.hifz.storage.HifzScheduleStore;
import com.quransafeguard.hifz.storage.RepetitionTimingStore;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Structured Hifz session. No Tafsir and no audio entry point exists in this Activity. */
public final class HifzSessionActivity extends android.app.Activity implements ReaderSurface.Listener {
    public static final String EXTRA_MODE = "mode";
    public static final String EXTRA_SCHEDULE_DATE = "schedule_date";

    private String mode;
    private LocalDate scheduledDate;
    private HifzProgressStore progressStore;
    private HifzScheduleStore scheduleStore;
    private RepetitionTimingStore timingStore;
    private LineGeometryRepository lineGeometry;
    private ReaderSurface surface;
    private TextView heading, program, progressText, timerText;
    private LinearLayout actions;
    private SessionClock clock;
    private long lastRepElapsed;
    private final ExecutorService loader = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "quran-hifz-session-loader");
        t.setDaemon(true);
        return t;
    });

    private LineGeometryRepository.FiveLineBlock sabqiBlock;
    private LineGeometryRepository.VerseUnit itqanUnit;
    private LineGeometryRepository.EligibleLinePlan murajaahPlan;
    private GeometryRepository.AyahRegion focus;
    private VerseRef murajaahActualEnd;
    private int currentPage = 1;
    private int currentMask;
    private boolean murajaahReviewingRecent;
    private List<LineGeometryRepository.PageLine> activeLines = Collections.emptyList();

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        mode = getIntent().getStringExtra(EXTRA_MODE);
        if (!HifzScheduleStore.SABQI.equals(mode)
            && !HifzScheduleStore.ITQAN.equals(mode)
            && !HifzScheduleStore.MURAJAAH.equals(mode)) {
            throw new IllegalArgumentException("Unknown Hifz mode");
        }
        String date = getIntent().getStringExtra(EXTRA_SCHEDULE_DATE);
        scheduledDate = date == null ? LocalDate.now() : LocalDate.parse(date);
        progressStore = new HifzProgressStore(this);
        scheduleStore = new HifzScheduleStore(this);
        timingStore = new RepetitionTimingStore(this);
        lineGeometry = LineGeometryRepository.get(this);
        if (!progressStore.isConfigured()) {
            finish();
            return;
        }
        buildUi();
        clock = new SessionClock(progressStore.elapsedMs(mode), elapsed -> {
            if (timerText != null) timerText.setText("Temps actif : " + SessionClock.format(elapsed));
            progressStore.setElapsedMs(mode, elapsed);
        });
        lastRepElapsed = sumTimings(timingStore.values(mode));
        prepareMode();
    }

    private void buildUi() {
        LinearLayout root = Ui.column(this);
        root.setPadding(0, 0, 0, 0);

        // BOOX already exposes a system Back action. Avoid duplicating it inside the reader.
        heading = Ui.text(this, HifzProgramActivity.displayMode(mode), 17, true);
        heading.setGravity(Gravity.CENTER);
        heading.setPadding(Ui.dp(this, 8), Ui.dp(this, 3), Ui.dp(this, 8), Ui.dp(this, 3));
        root.addView(heading);

        program = Ui.text(this, "Préparation de la géométrie…", 14, true);
        program.setPadding(Ui.dp(this, 12), 1, Ui.dp(this, 12), 1);
        root.addView(program);

        timerText = Ui.text(this, "Temps actif : 00:00", 13, false);
        timerText.setPadding(Ui.dp(this, 12), 1, Ui.dp(this, 12), 1);
        root.addView(timerText);

        progressText = Ui.text(this, "", 14, false);
        progressText.setPadding(Ui.dp(this, 12), 1, Ui.dp(this, 12), 2);
        root.addView(progressText);

        surface = new ReaderSurface(this);
        surface.setListener(this);
        root.addView(surface, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            0,
            1f
        ));

        // Only Hifz-specific actions stay visible. Page navigation remains swipe/PageUp/PageDown.
        actions = Ui.row(this);
        actions.setPadding(Ui.dp(this, 8), 2, Ui.dp(this, 8), Ui.dp(this, 4));
        root.addView(actions);
        setContentView(root);
    }

    private void addRevealAction() {
        Button reveal = Ui.smallButton(this, "Afficher", null);
        reveal.setOnTouchListener((v, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                surface.revealTemporarily(true);
            }
            if (event.getActionMasked() == MotionEvent.ACTION_UP
                || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                surface.revealTemporarily(false);
            }
            return true;
        });
        Ui.weight(reveal, 1f);
        actions.addView(reveal);
    }

    private void prepareMode() {
        actions.removeAllViews();
        program.setText("Préparation de la géométrie…");
        loader.execute(() -> {
            try {
                if (HifzScheduleStore.SABQI.equals(mode)) prepareSabqi();
                else if (HifzScheduleStore.ITQAN.equals(mode)) prepareItqan();
                else prepareMurajaah();
            } catch (Throwable error) {
                runOnUiThread(() -> onError(error));
            }
        });
    }

    private void prepareSabqi() throws Exception {
        VerseRef cursor = progressStore.sabqiCursor();
        int startLine = lineGeometry.firstLineIndex(cursor);
        LineGeometryRepository.FiveLineBlock block = lineGeometry.fiveLineBlock(startLine);
        if (ordinal(block.endVerse) > ordinal(progressStore.sabqiEnd())) {
            startLine = lineGeometry.firstLineIndex(progressStore.sabqiStart());
            block = lineGeometry.fiveLineBlock(startLine);
        }
        sabqiBlock = block;
        activeLines = block.lines;
        currentPage = block.lines.get(0).page;
        currentMask = sabqiMask(progressStore.sabqiRep());
        runOnUiThread(this::renderSabqi);
    }

    private void renderSabqi() {
        int rep = progressStore.sabqiRep();
        program.setText("5 lignes · " + sabqiBlock.startVerse + " → " + sabqiBlock.endVerse + " · 37 répétitions");
        progressText.setText("Répétition suivante : " + Math.min(37, rep + 1)
            + " / 37 · masque " + currentMask + "% · aides "
            + progressStore.sabqiAssisted() + timingSuffix(mode));
        showCurrent();
        Button done = Ui.smallButton(this, "Répétition faite", v -> completeSabqi(false));
        Button assisted = Ui.smallButton(this, "Faite avec aide", v -> completeSabqi(true));
        Ui.weight(done, 1);
        Ui.weight(assisted, 1);
        actions.addView(done);
        actions.addView(assisted);
        addRevealAction();
    }

    private void completeSabqi(boolean assisted) {
        int rep = progressStore.sabqiRep();
        if (rep >= 37) return;
        recordRepetitionTiming();
        rep++;
        int aids = progressStore.sabqiAssisted() + (assisted ? 1 : 0);
        if (rep >= 37) {
            progressStore.addRecentSabqi(sabqiBlock.startGlobalIndex, sabqiBlock.endGlobalIndex);
            try {
                List<LineGeometryRepository.PageLine> all = lineGeometry.allLines();
                VerseRef next = progressStore.sabqiStart();
                if (sabqiBlock.endGlobalIndex + 1 < all.size()) {
                    List<VerseRef> verses = all.get(sabqiBlock.endGlobalIndex + 1).verses;
                    if (!verses.isEmpty()
                        && ordinal(verses.get(0)) <= ordinal(progressStore.sabqiEnd())) {
                        next = verses.get(0);
                    }
                }
                progressStore.setSabqiCursor(next);
            } catch (Throwable ignored) {
                progressStore.setSabqiCursor(progressStore.sabqiStart());
            }
            progressStore.setSabqiProgress(0, 0);
            completeScheduled("37 répétitions · " + sabqiBlock.startVerse + " → " + sabqiBlock.endVerse);
            return;
        }
        progressStore.setSabqiProgress(rep, aids);
        currentMask = sabqiMask(rep);
        applyOverlay();
        renderProgressOnly();
    }

    private void prepareItqan() throws Exception {
        EligibleCorpus corpus = exactItqanCorpus();
        itqanUnit = lineGeometry.eligiblePageUnit(progressStore.itqanCursor(), corpus);
        activeLines = itqanUnit.lines;
        currentPage = itqanUnit.page;
        currentMask = itqanMask(progressStore.itqanRep());
        runOnUiThread(this::renderItqan);
    }

    private void renderItqan() {
        int rep = progressStore.itqanRep();
        program.setText("Page " + itqanUnit.page + " · " + itqanUnit.start + " → " + itqanUnit.end + " · ×30");
        progressText.setText("Répétition suivante : " + Math.min(30, rep + 1)
            + " / 30 · masque " + currentMask + "% · aides "
            + progressStore.itqanAssisted() + timingSuffix(mode));
        showCurrent();
        Button done = Ui.smallButton(this, "Répétition faite", v -> completeItqan(false));
        Button assisted = Ui.smallButton(this, "Faite avec aide", v -> completeItqan(true));
        Ui.weight(done, 1);
        Ui.weight(assisted, 1);
        actions.addView(done);
        actions.addView(assisted);
        addRevealAction();
    }

    private void completeItqan(boolean assisted) {
        int rep = progressStore.itqanRep();
        if (rep >= 30) return;
        recordRepetitionTiming();
        rep++;
        int aids = progressStore.itqanAssisted() + (assisted ? 1 : 0);
        if (rep >= 30) {
            VerseRef next = ordinal(itqanUnit.end) >= ordinal(progressStore.itqanEnd())
                ? progressStore.itqanStart()
                : QuranCanon.INSTANCE.next(itqanUnit.end);
            if (next == null || ordinal(next) > ordinal(progressStore.itqanEnd())) {
                next = progressStore.itqanStart();
            }
            progressStore.setItqanCursor(next);
            progressStore.setItqanProgress(0, 0);
            completeScheduled("×30 · " + itqanUnit.start + " → " + itqanUnit.end + timingSuffix(mode));
            return;
        }
        progressStore.setItqanProgress(rep, aids);
        currentMask = itqanMask(rep);
        applyOverlay();
        renderProgressOnly();
    }

    private void prepareMurajaah() throws Exception {
        List<HifzProgressStore.RecentSabqi> recent = progressStore.recentSabqi();
        if (!recent.isEmpty()) {
            murajaahReviewingRecent = true;
            HifzProgressStore.RecentSabqi item = recent.get(0);
            List<LineGeometryRepository.PageLine> all = lineGeometry.allLines();
            int end = Math.min(all.size() - 1, item.endGlobalLine);
            activeLines = new ArrayList<>(all.subList(item.startGlobalLine, end + 1));
            currentPage = activeLines.get(0).page;
            currentMask = 0;
            runOnUiThread(this::renderMurajaahRecent);
            return;
        }
        prepareMurajaahItqan(false);
    }

    private void renderMurajaahRecent() {
        program.setText("Murājaʿah · Sabqi récent");
        progressText.setText("Bloc récent : " + activeLines.size()
            + " ligne(s). Aucune répétition imposée.");
        showCurrent();
        Button done = Ui.smallButton(this, "Bloc revu", v -> {
            progressStore.removeFirstRecentSabqi();
            loader.execute(() -> {
                try {
                    prepareMurajaahItqan(true);
                } catch (Throwable e) {
                    runOnUiThread(() -> onError(e));
                }
            });
        });
        Ui.weight(done, 1);
        actions.addView(done);
        addRevealAction();
    }

    private void prepareMurajaahItqan(boolean recentWasReviewed) throws Exception {
        murajaahReviewingRecent = false;
        int minutes = recentWasReviewed ? 30 : 45;
        int requestedLines = Math.max(1,
            (int) Math.floor(minutes * 60.0 / progressStore.murajaahSecondsPerLine()));
        murajaahPlan = lineGeometry.planEligibleLines(
            progressStore.murajaahCursor(), requestedLines, exactItqanCorpus());
        murajaahActualEnd = null;
        activeLines = Collections.emptyList();
        currentPage = lineGeometry.pageForVerse(murajaahPlan.start);
        currentMask = 0;
        final int finalMinutes = minutes;
        runOnUiThread(() -> renderMurajaahItqan(finalMinutes));
    }

    private void renderMurajaahItqan(int minutes) {
        actions.removeAllViews();
        program.setText("Murājaʿah · cycle Itqān · " + minutes + " min");
        progressText.setText("Prévision : " + murajaahPlan.start + " → "
            + murajaahPlan.predictedEnd + " · " + murajaahPlan.requestedLines
            + " lignes\nTouchez le dernier verset réellement terminé.");
        showCurrent();
        Button finish = Ui.smallButton(this, "Terminer au verset choisi", v -> finishMurajaah());
        Ui.weight(finish, 1);
        actions.addView(finish);
        addRevealAction();
    }

    private void finishMurajaah() {
        if (murajaahActualEnd == null) {
            Toast.makeText(this,
                "Touchez le dernier verset réellement révisé.",
                Toast.LENGTH_LONG).show();
            return;
        }
        VerseRef next = progressStore.nextMurajaah(murajaahActualEnd);
        progressStore.setMurajaahCursor(next);
        completeScheduled("Réel : " + murajaahPlan.start + " → " + murajaahActualEnd);
    }

    private void completeScheduled(String label) {
        try {
            scheduleStore.markCompleted(scheduledDate, mode);
        } catch (Throwable mismatch) {
            // Session remains valid; scheduling metadata is secondary.
        }
        progressStore.markCompletedToday(mode, label);
        progressStore.setElapsedMs(mode, 0L);
        timingStore.reset(mode);
        Toast.makeText(this, "Séance terminée.", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void showCurrent() {
        surface.showPage(currentPage);
    }

    private void applyOverlay() {
        List<LineGeometryRepository.PageLine> visible = new ArrayList<>();
        for (LineGeometryRepository.PageLine line : activeLines) {
            if (line.page == currentPage) visible.add(line);
        }
        surface.setMemorizationState(visible, focus, currentMask);
    }

    private void renderProgressOnly() {
        if (HifzScheduleStore.SABQI.equals(mode)) {
            int rep = progressStore.sabqiRep();
            progressText.setText("Répétition suivante : " + (rep + 1)
                + " / 37 · masque " + currentMask + "% · aides "
                + progressStore.sabqiAssisted() + timingSuffix(mode));
        } else if (HifzScheduleStore.ITQAN.equals(mode)) {
            int rep = progressStore.itqanRep();
            progressText.setText("Répétition suivante : " + (rep + 1)
                + " / 30 · masque " + currentMask + "% · aides "
                + progressStore.itqanAssisted() + timingSuffix(mode));
        }
    }

    private String timingSuffix(String m) {
        long avg = timingStore.average(m);
        long med = timingStore.median(m);
        if (avg <= 0L) return "";
        return " · moy. " + SessionClock.format(avg) + " · méd. " + SessionClock.format(med);
    }

    private void recordRepetitionTiming() {
        long now = clock.elapsedMs();
        long delta = Math.max(0L, now - lastRepElapsed);
        timingStore.add(mode, delta);
        lastRepElapsed = now;
    }

    @Override public void onPageChanged(int page) {
        currentPage = page;
        applyOverlay();
    }

    @Override public void onVerseTapped(GeometryRepository.AyahRegion region) {
        focus = region;
        VerseRef ref = new VerseRef(region.surah, region.ayah);
        if (HifzScheduleStore.MURAJAAH.equals(mode)
            && !murajaahReviewingRecent
            && murajaahPlan != null
            && murajaahPlan.traversalVerses.contains(ref)) {
            murajaahActualEnd = ref;
            progressText.setText("Fin réelle sélectionnée : " + ref
                + "\nLa borne réelle, pas la prévision, sera sauvegardée.");
        }
        applyOverlay();
    }

    @Override public void onPageSwipe(int delta) {
        goPage(delta);
    }

    @Override public void onError(Throwable error) {
        Toast.makeText(this, "Hifz : " + error.getMessage(), Toast.LENGTH_LONG).show();
    }

    private void goPage(int delta) {
        int next = Math.max(1, Math.min(604, currentPage + delta));
        if (next != currentPage) {
            currentPage = next;
            surface.showPage(next);
        }
    }

    @Override public boolean onKeyDown(int code, KeyEvent event) {
        if (code == KeyEvent.KEYCODE_PAGE_UP) {
            goPage(-1);
            return true;
        }
        if (code == KeyEvent.KEYCODE_PAGE_DOWN) {
            goPage(1);
            return true;
        }
        return super.onKeyDown(code, event);
    }

    @Override protected void onResume() {
        super.onResume();
        if (clock != null) clock.resume();
    }

    @Override protected void onPause() {
        if (clock != null) progressStore.setElapsedMs(mode, clock.pause());
        super.onPause();
    }

    @Override protected void onDestroy() {
        loader.shutdownNow();
        if (clock != null) clock.dispose();
        if (surface != null) surface.close();
        super.onDestroy();
    }

    private EligibleCorpus exactItqanCorpus() {
        return EligibleCorpus.Companion.of(
            new VerseRange(progressStore.itqanStart(), progressStore.itqanEnd()));
    }

    private static int ordinal(VerseRef ref) {
        return QuranCanon.INSTANCE.ordinal(ref);
    }

    private static long sumTimings(List<Long> values) {
        long s = 0L;
        for (Long v : values) s += v;
        return s;
    }

    private static int sabqiMask(int completed) {
        int next = completed + 1;
        if (next <= 15) return 0;
        if (next <= 20) return 25;
        if (next <= 25) return 50;
        if (next <= 30) return 75;
        return 100;
    }

    private static int itqanMask(int completed) {
        int next = completed + 1;
        if (next <= 10) return 0;
        if (next <= 15) return 25;
        if (next <= 20) return 50;
        if (next <= 25) return 75;
        return 100;
    }
}
