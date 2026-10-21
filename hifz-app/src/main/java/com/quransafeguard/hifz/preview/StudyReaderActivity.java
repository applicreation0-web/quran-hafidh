package com.quransafeguard.hifz.preview;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.RelativeSizeSpan;
import android.text.style.AbsoluteSizeSpan;
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
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.quransafeguard.hifz.core.VerseRef;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Lecture/Etude. Tafsir stays outside all memorisation modes; audio belongs to Hifz modes only. */
public final class StudyReaderActivity extends android.app.Activity implements MushafView.Listener {
    private static final float TAFSIR_MIN_SP = 16f;
    private static final float TAFSIR_MAX_SP = 26f;
    private static final float TAFSIR_DEFAULT_SP = 18f;
    private static final String TAFSIR_PREFS = "hifz_tafsir_reading";
    private static final String TAFSIR_FONT_KEY = "commentary_font_size_sp";
    private static final String TAFSIR_EDITION_KEY = "edition";
    /** Jump straight to a page (int extra) and, optionally, highlight one verse on it (string extra, e.g. "2:255"). */
    public static final String EXTRA_JUMP_PAGE = "jumpPage";
    public static final String EXTRA_JUMP_VERSE = "jumpVerse";
    public static final String EXTRA_MAP_PREVIEW = "ibnKathirMapPreview";

    private MushafView mushaf;
    private AnnotationOverlayView annotationOverlay;
    private AnnotationStore annotationStore;
    private Button annotationButton;
    private Button annotationUndoButton;
    private Button annotationClearButton;
    // The pen starts closed; its undo/erase tools only appear while it is open.
    private boolean annotationEnabled = false;
    private int page = 1;
    private VerseRef selected;
    private VerseRef pendingJumpVerse;
    private Button tafsirButton;
    private Button semanticButton;
    /** Lecture-only, opt-in Révision active gomme: no timer, validation or Hifz writes. */
    private Button freeActiveMaskButton;
    private boolean freeActiveMask;
    private boolean freeActiveMaskPageReady;
    private AyahThemesCueIndex ayahThemesCues;
    private boolean semanticCuesEnabled;
    private android.app.Dialog semanticTitleDialog;
    /** Phone Tafsir: a non-modal bottom panel inside the reader (Mushaf stays tappable above it). */
    private FrameLayout bottomTafsir;
    private LinearLayout topControls, readerActions, pageRail, rootRow, sideTafsir, correspondenceHost;
    private FrameLayout readerPane;
    private TextView surahPicker;
    private TextView rubPicker;
    private boolean controlsVisible = true;
    private boolean mapPreview;
    private MawdudiMapData mapPreviewUnits;
    private MapJalalaynDialog mapPreviewJalalayn;
    // Atlas-only navigation state; never changes ordinary reading or Hifz settings.
    private boolean mapUnitPreview;
    private TextView atlasCueFooter;
    private Button atlasPassageUp,atlasPassageDown;
    private java.util.List<MawdudiPassageIndex.Passage> atlasPassages=java.util.Collections.emptyList();
    private int atlasPassagePosition=-1;
    private boolean keepAtlasPassageOnNextPage;
    private boolean restoreAtlasTafsir;
    private boolean largeScreen;
    private HifzPrefs hifzPrefs;
    private GeometryRepository geometry;
    private final EinkController eink = new EinkController();
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Runnable autoHide = this::hideControls;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        mapPreview = getIntent().getBooleanExtra(EXTRA_MAP_PREVIEW, false);
        mapUnitPreview = mapPreview && getIntent().getBooleanExtra("map114UnitMode", false);
        if(mapPreview) {
            try {
                mapPreviewUnits=MawdudiMapData.load(this,MawdudiPassageIndex.load(this));
            }catch(Exception missingPrivateData){
                // Fail closed: no fabricated/mislocated unit boundary annotations.
                mapPreviewUnits=null;
            }
        }
        if(mapUnitPreview) {
            // Read-only unit passage list. Never re-segment an Ayah Themes passage.
            try {
                int s=getIntent().getIntExtra("map114Surah",0);
                int lo=getIntent().getIntExtra("map114FocusStart",0);
                int hi=getIntent().getIntExtra("map114FocusEnd",0);
                java.util.ArrayList<MawdudiPassageIndex.Passage> inside=new java.util.ArrayList<>();
                for(MawdudiPassageIndex.Passage candidate:MawdudiPassageIndex.load(this).forSurah(s))
                    if(candidate.lastAyah>=lo&&candidate.firstAyah<=hi)inside.add(candidate);
                atlasPassages=inside;
                if(state!=null){
                    String savedId=state.getString("map114ReaderPassageId");
                    for(int k=0;k<inside.size();k++)
                        if(inside.get(k).id.equals(savedId)){atlasPassagePosition=k;break;}
                    // Compatibility with already-saved activities on the same catalog.
                    if(savedId==null)atlasPassagePosition=state.getInt("map114PassageIndex",-1);
                } else {
                    // Direct passage selection from Atlas must survive opening Lecture
                    // even when multiple Ayah Themes passages start on one QCF page.
                    String requestedId=getIntent().getStringExtra("map114PassageId");
                    int requestedStart=getIntent().getIntExtra("map114SelectedStart",-1);
                    int requestedEnd=getIntent().getIntExtra("map114SelectedEnd",-1);
                    for(int k=0;k<inside.size();k++){
                        MawdudiPassageIndex.Passage candidate=inside.get(k);
                        if(requestedId!=null&&requestedId.equals(candidate.id)
                            &&requestedStart==candidate.firstAyah
                            &&requestedEnd==candidate.lastAyah){
                            atlasPassagePosition=k;break;
                        }
                    }
                }
                if(atlasPassagePosition<0||atlasPassagePosition>=inside.size())atlasPassagePosition=-1;
            }catch(RuntimeException missingIndex){atlasPassages=java.util.Collections.emptyList();}
        }
        int jumpPage = state==null ? getIntent().getIntExtra(EXTRA_JUMP_PAGE, 0) : 0;
        page = jumpPage >= 1 && jumpPage <= 604
            ? jumpPage : getSharedPreferences("hifz_study", MODE_PRIVATE).getInt("page", 1);
        if(state!=null)page=state.getInt("map114ReaderPage",page);
        // A saved reader cursor wins over the original one-shot launch jump.
        String jumpVerse = state!=null ? state.getString("map114SelectedVerse")
            : getIntent().getStringExtra(EXTRA_JUMP_VERSE);
        if (jumpVerse != null) {
            try { pendingJumpVerse = GeometryRepository.parseVerse(jumpVerse); } catch (RuntimeException malformed) { /* ignore */ }
        }
        if (!mapPreview) getSharedPreferences("hifz_study", MODE_PRIVATE).edit().putInt("page", page).apply();
        hifzPrefs = new HifzPrefs(this);
        largeScreen = getResources().getConfiguration().smallestScreenWidthDp >= 600;
        // CP02 Lecture-only: 1053 Ayah Themes starts with exact QCF word-1 geometry.
        // No private pack means no markers, NEVER a fallback to old documentary headings.
        ayahThemesCues=AyahThemesCueIndex.load(this);
        semanticCuesEnabled=ayahThemesCues!=null
            && (mapPreview||getSharedPreferences("hifz_study",MODE_PRIVATE)
                .getBoolean("ayah_themes_start_markers_visible",true));

        if(state!=null){
            freeActiveMask=ayahThemesCues!=null&&state.getBoolean("readerFreeActiveMask",false);
            semanticCuesEnabled=ayahThemesCues!=null&&state.getBoolean("map114SemanticCues",semanticCuesEnabled);
            controlsVisible=state.getBoolean("map114ControlsVisible",true);
            restoreAtlasTafsir=state.getBoolean("map114TafsirOpen",false);
        }

        rootRow = new LinearLayout(this);
        rootRow.setOrientation(LinearLayout.HORIZONTAL);
        rootRow.setBackgroundColor(Ui.PAPER);
        readerPane = new FrameLayout(this);
        readerPane.setBackgroundColor(Ui.PAPER);
        if (largeScreen) {
            rootRow.addView(readerPane, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 58f));
            sideTafsir = Ui.column(this);
            sideTafsir.setVisibility(View.GONE);
            rootRow.addView(sideTafsir, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 42f));
            setContentView(rootRow);
            Ui.respectSystemBars(this, rootRow, 0, 0, 0, 0);
        } else {
            setContentView(readerPane);
            Ui.respectSystemBars(this, readerPane, 0, 0, 0, 0);
        }

        LinearLayout readerStack = Ui.column(this);
        readerStack.setPadding(0, 0, 0, 0);
        readerPane.addView(readerStack, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        topControls = Ui.row(this);
        topControls.setGravity(Gravity.CENTER_VERTICAL);
        topControls.setBackgroundColor(Ui.PAPER);
        topControls.setPadding(Ui.dp(this, 4), 0, Ui.dp(this, 4), 0);
        Button back = Ui.iconButton(this, "‹", "Retour", v -> finish());
        LinearLayout titleRow = Ui.row(this);
        Ui.weight(titleRow, 1f);
        titleRow.setGravity(Gravity.CENTER);
        // The rub'/hizb boundary is shown once, by the reader's right-gutter écusson; the header
        // no longer repeats it.
        TextView balance = Ui.text(this, "", 1f, false);
        balance.setMinWidth(Ui.dp(this, 44));
        topControls.addView(back);
        topControls.addView(titleRow);
        topControls.addView(balance, new LinearLayout.LayoutParams(Ui.dp(this, 44), Ui.dp(this, 44)));
        readerStack.addView(topControls, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        mushaf = new MushafView(this);
        mushaf.setListener(this);
        annotationStore = new AnnotationStore(this);
        annotationOverlay = new AnnotationOverlayView(this);
        annotationOverlay.setStore(annotationStore);
        annotationOverlay.setDrawingEnabled(annotationEnabled);
        FrameLayout mushafContainer = new FrameLayout(this);
        mushafContainer.addView(mushaf, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mushafContainer.addView(annotationOverlay, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        readerStack.addView(mushafContainer, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        readerActions = Ui.row(this);
        readerActions.setGravity(Gravity.CENTER);
        readerActions.setPadding(Ui.dp(this, 6), 0, Ui.dp(this, 6), 0);
        readerActions.setMinimumHeight(Ui.dp(this, 48));
        tafsirButton = tafsirReaderAction();
        readerActions.addView(tafsirButton);
        semanticButton = Ui.iconButton(this, "", "Afficher les ancres", v -> toggleSemanticCues());
        semanticButton.setVisibility(ayahThemesCues!=null?View.VISIBLE:View.GONE);
        readerActions.addView(semanticButton);
        // One discreet access point, using the existing Révision pictogram.
        // The 100% mask only affects the current Lecture surface.
        freeActiveMaskButton=Ui.iconButton(this,"","Révision active",
            v -> toggleFreeActiveMask());
        Ui.setButtonIcon(freeActiveMaskButton,R.drawable.ic_ui_revision);
        freeActiveMaskButton.setVisibility(ayahThemesCues!=null?View.VISIBLE:View.GONE);
        readerActions.addView(freeActiveMaskButton);
        updateFreeActiveMaskButton();
        if(mapUnitPreview){
            atlasPassageUp=Ui.iconButton(this,"","Passage vers le haut",v->moveAtlasPassage(-1));
            atlasPassageDown=Ui.iconButton(this,"","Passage vers le bas",v->moveAtlasPassage(1));
            Ui.setButtonIcon(atlasPassageUp,R.drawable.ic_ui_previous);
            atlasPassageUp.setRotation(90f);
            Ui.setButtonIcon(atlasPassageDown,R.drawable.ic_ui_previous);
            atlasPassageDown.setRotation(270f);
            readerActions.addView(atlasPassageUp);
            readerActions.addView(atlasPassageDown);
            updateAtlasPassageButtons();
        }
        updateSemanticButton();
        annotationButton = Ui.iconButton(this, "", "Annoter", v -> toggleAnnotationMode());
        annotationButton.setSelected(annotationEnabled);
        readerActions.addView(annotationButton);
        annotationUndoButton = Ui.iconButton(this, "", "Annuler la note",
            v -> annotationOverlay.undoLastStroke());
        annotationClearButton = Ui.iconButton(this, "", "Effacer les notes",
            v -> annotationOverlay.clearCurrentPage());
        annotationUndoButton.setVisibility(View.GONE);
        annotationClearButton.setVisibility(View.GONE);
        readerActions.addView(annotationUndoButton);
        readerActions.addView(annotationClearButton);
        readerStack.addView(readerActions, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        correspondenceHost=Ui.row(this);
        correspondenceHost.setGravity(Gravity.CENTER);
        correspondenceHost.setVisibility(View.GONE);
        readerStack.addView(correspondenceHost,new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));

        pageRail = Ui.row(this);
        pageRail.setPadding(Ui.dp(this, 10), 0, Ui.dp(this, 10), Ui.dp(this, 1));
        // Must stay the exact same size as rubPicker below: pageRail centers each child by its
        // own bounding box (padding + line height), so any size difference between the two shifts
        // their text baselines off the shared row line even though the row itself looks centered.
        // Arabic script does read visually lighter than rubPicker's plain Latin digits/word at an
        // identical size, but that's a much smaller defect than a broken baseline, so it stays.
        surahPicker = Ui.bookText(this, "Sourate", 13f, true);
        surahPicker.setGravity(Gravity.CENTER);
        surahPicker.setPadding(0, Ui.dp(this, 4), 0, Ui.dp(this, 4));
        surahPicker.setClickable(true);
        surahPicker.setFocusable(true);
        surahPicker.setEnabled(false);
        surahPicker.setContentDescription("Aller à une sourate");
        surahPicker.setOnClickListener(v -> showSurahPicker());
        pageRail.addView(surahPicker, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        rubPicker = Ui.bookText(this, "", 13f, true);
        rubPicker.setGravity(Gravity.CENTER);
        rubPicker.setPadding(0, Ui.dp(this, 4), 0, Ui.dp(this, 4));
        rubPicker.setClickable(true);
        rubPicker.setFocusable(true);
        rubPicker.setContentDescription("Aller à un Hizb");
        rubPicker.setOnClickListener(v -> showRubPicker());
        pageRail.addView(rubPicker, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        updateRubPickerLabel();
        LinearLayout.LayoutParams railParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        railParams.topMargin = 0;
        readerStack.addView(pageRail, railParams);
        // Caption is beneath the real 15-line QCF page, never over the text.
        // Refresh it on every physical page change, not only when a word is tapped.
        if(mapUnitPreview){
            atlasCueFooter=Ui.text(this,"",12f,false);
            atlasCueFooter.setMinHeight(Ui.dp(this,56));
            atlasCueFooter.setGravity(Gravity.CENTER);
            atlasCueFooter.setMaxLines(5);
            atlasCueFooter.setEllipsize(null);
            atlasCueFooter.setOnClickListener(v->showFullAtlasCue());
            atlasCueFooter.setPadding(Ui.dp(this,5),Ui.dp(this,2),Ui.dp(this,5),Ui.dp(this,3));
            atlasCueFooter.setContentDescription("Passage Ayah Themes actif sur cette page");
            readerStack.addView(atlasCueFooter,new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        if (!largeScreen) {
            bottomTafsir = new FrameLayout(this);
            bottomTafsir.setBackgroundColor(Ui.PAPER);
            bottomTafsir.setVisibility(View.GONE);
            bottomTafsir.setClickable(true);
            bottomTafsir.setFocusable(true);
            int panelHeight = mapPreview
                ? Math.max(Ui.dp(this, 150),Math.min(Ui.dp(this, 320),
                    Math.round(getResources().getDisplayMetrics().heightPixels * .35f)))
                : Math.max(Ui.dp(this, 220),
                    Math.round(getResources().getDisplayMetrics().heightPixels * .42f));
            if(mapPreview) {
                // Atlas: a real sibling, never an overlay hiding part of the Mushaf.
                // The weighted QCF pane shrinks, and recovers its full height on close.
                readerStack.addView(bottomTafsir,new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,panelHeight));
            } else {
                // Keep the established ordinary reading behavior unchanged.
                readerPane.addView(bottomTafsir,new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,panelHeight,Gravity.BOTTOM));
            }
        }
        scheduleAutoHide();
        loadGeometryForSurahPicker();
    }

    private void loadGeometryForSurahPicker() {
        io.execute(() -> {
            try {
                GeometryRepository loaded = GeometryRepository.get(getApplicationContext());
                runOnUiThread(() -> {
                    geometry = loaded;
                    if (surahPicker != null) { surahPicker.setEnabled(true); updateSurahPickerLabel(); }
                });
            } catch (Throwable ignored) {
                // Surah picker stays disabled; page swipe/back navigation still works without it.
            }
        });
    }

    private void updateSurahPickerLabel() {
        if (geometry == null || surahPicker == null) return;
        surahPicker.setText(QuranSurahNames.labelFor(geometry.firstSurahOnPage(page)) + " ▾");
    }

    private void showSurahPicker() {
        if (geometry == null) return;
        showControls();
        QuranSurahNames.showPicker(this, geometry, this::setPage);
    }

    private void updateRubPickerLabel() {
        if (rubPicker == null) return;
        rubPicker.setText(QuranRubNames.compactLabel(page) + " ▾");
    }

    private void showRubPicker() {
        showControls();
        QuranRubNames.showPicker(this, this::setPage);
    }

    /** Same icon grammar as Amorces/Annoter; the hint stays in the description/tooltip. */
    private Button tafsirReaderAction() {
        Button button = Ui.iconButton(this, "", "Tafsir", v -> openTafsir());
        button.setContentDescription("Tafsir · touchez un verset puis ouvrez le commentaire");
        return button;
    }

    private void setPage(int requested) {
        int next = Math.max(1, Math.min(604, requested));
        if(mapPreview) {
            // In the map-linked reader, turning a page cannot escape the
            // selected Ayah Themes passage. The ordinary Hifz reader is unchanged.
            try {
                int surah=getIntent().getIntExtra("map114Surah",0);
                int first=getIntent().getIntExtra("map114FocusStart",0);
                int last=getIntent().getIntExtra("map114FocusEnd",0);
                GeometryRepository qcf=GeometryRepository.get(this);
                if(surah>=1&&surah<=114&&first>=1&&last>=first) {
                    next=Math.max(qcf.pageForVerse(new VerseRef(surah,first)),
                        Math.min(MawdudiMapModel.lastPageForVerse(new VerseRef(surah,last),qcf),next));
                }
            } catch(RuntimeException invalidPreview) {return;}
        }
        if (next == page) { keepAtlasPassageOnNextPage=false;showControls(); return; }
        if(mapUnitPreview&&!keepAtlasPassageOnNextPage)selectAtlasPassageOnPage(next);
        keepAtlasPassageOnNextPage=false;
        closeSemanticTitle();
        closeTafsirPanels();
        mushaf.clearReveal();
        page = next;
        selected = null;
        tafsirButton.setContentDescription("Tafsir · touchez un verset puis ouvrez le commentaire");
        if (!mapPreview) getSharedPreferences("hifz_study", MODE_PRIVATE).edit().putInt("page", page).apply();
        updateSurahPickerLabel();
        updateRubPickerLabel();
        if(freeActiveMask){
            // Never carry old-page anchor geometry into the next QCF folio.
            mushaf.clearPageLandmarkBoxes();
            mushaf.clearSemanticCues();
        }
        if(!showMap114Focus())mushaf.show(page, Collections.emptyList(), Collections.emptyList(), 0);
        showControls();
    }

    private void go(int delta) { setPage(page + delta); }

    /** With the Tafsir open, tapping another verse selects it and refreshes the same panel in
     *  place (no close/reopen, same A−/A+ size and edition from the reading prefs). */
    @Override public void onVerseTap(VerseRef verse) {
        boolean tafsirWasOpen = isTafsirOpen();
        closeSemanticTitle();
        if(mapUnitPreview){
            int s=getIntent().getIntExtra("map114Surah",0);
            int first=getIntent().getIntExtra("map114FocusStart",0);
            int last=getIntent().getIntExtra("map114FocusEnd",0);
            if(verse.getSurah()!=s||verse.getAyah()<first||verse.getAyah()>last)return;
        }
        if(mapUnitPreview){
            for(int k=0;k<atlasPassages.size();k++){
                MawdudiPassageIndex.Passage candidate=atlasPassages.get(k);
                if(candidate.surah==verse.getSurah()&&candidate.contains(verse.getAyah())){
                    atlasPassagePosition=k;if(!freeActiveMask)showMap114Focus();updateAtlasCueFooter();
                    updateAtlasPassageButtons();break;
                }
            }
        }
        selected = verse;
        tafsirButton.setContentDescription("Tafsir " + verse.getSurah() + ":" + verse.getAyah());
        if(!mapUnitPreview&&!freeActiveMask)mushaf.setSelection(Collections.singletonList(verse), Collections.emptyList());
        updateCorrespondences();
        refreshOpenTafsir(verse);
        if (tafsirWasOpen && !largeScreen) hideControls(); else showControls();
    }

    @Override public void onPageSwipe(int delta) { go(delta); }
    @Override public void onSurfaceTap() {
        if (isTafsirOpen()) return;
        if (controlsVisible) hideControls(); else showControls();
    }
    @Override public void onSemanticCueTap(String passageId) {
        if((!semanticCuesEnabled&&!freeActiveMask)||ayahThemesCues==null){closeSemanticTitle();return;}
        // A title is shown only for a real first-word anchor on this QCF page.
        String label=ayahThemesCues.titleForVisibleStart(page,passageId);
        if(label==null){closeSemanticTitle();return;}
        showControls();
        if(mapUnitPreview){
            updateAtlasCueFooter();
            return;
        }
        semanticTitleDialog=SemanticTitlePopup.showEnglish(this,label,semanticTitleDialog);
        SemanticTitlePopup.positionAboveIcons(this,semanticTitleDialog,readerActions);
        readerActions.post(() ->
            SemanticTitlePopup.positionAboveIcons(this,semanticTitleDialog,readerActions));
    }

    /** A visible caption is valid only for the current, titled word cue and current page. */
    private void closeSemanticTitle() {
        if (semanticTitleDialog != null && semanticTitleDialog.isShowing())
            semanticTitleDialog.dismiss();
        semanticTitleDialog = null;
    }

    /** Lecture-only Ayah Themes visual cues, never Hifz mask/cursor changes. */
    private void toggleAnnotationMode() {
        annotationEnabled = !annotationEnabled;
        annotationOverlay.setDrawingEnabled(annotationEnabled);
        annotationButton.setSelected(annotationEnabled);
        Ui.setIconDescription(annotationButton,
            annotationEnabled ? "Désactiver le crayon" : "Activer le crayon");
        annotationUndoButton.setVisibility(annotationEnabled ? View.VISIBLE : View.GONE);
        annotationClearButton.setVisibility(annotationEnabled ? View.VISIBLE : View.GONE);
    }

    private void toggleSemanticCues() {
        if(ayahThemesCues==null)return;
        semanticCuesEnabled=!semanticCuesEnabled;
        if(!semanticCuesEnabled)closeSemanticTitle();
        if(!mapPreview)getSharedPreferences("hifz_study",MODE_PRIVATE).edit()
            .putBoolean("ayah_themes_start_markers_visible",semanticCuesEnabled).apply();
        updateSemanticButton();
        applySemanticCues();
        updateAtlasCueFooter();
        showControls();
    }

    private void updateSemanticButton() {
        if (semanticButton == null) return;
        semanticButton.setSelected(semanticCuesEnabled);
        String description=semanticCuesEnabled?"Masquer les ancres"
            :"Afficher les ancres";
        Ui.setIconDescription(semanticButton,description);
        int icon=Ui.iconFor(semanticCuesEnabled?"Masquer les ancres":"Afficher les ancres","");
        if (icon != 0) Ui.setButtonIcon(semanticButton, icon);
    }

    private void applySemanticCues() {
        if (mushaf == null) return;
        if(freeActiveMask){applyFreeActiveMask();return;}
        if(!semanticCuesEnabled||ayahThemesCues==null){
            mushaf.clearSemanticCues();
            return;
        }
        if(mapUnitPreview){
            org.json.JSONArray all=ayahThemesCues.forUnitPage(page,
                getIntent().getIntExtra("map114Surah",0),
                getIntent().getIntExtra("map114FocusStart",0),
                getIntent().getIntExtra("map114FocusEnd",0));
            org.json.JSONArray active=new org.json.JSONArray();
            if(atlasPassagePosition>=0&&atlasPassagePosition<atlasPassages.size()){
                String activeId=atlasPassages.get(atlasPassagePosition).id;
                for(int i=0;i<all.length();i++){
                    org.json.JSONObject cue=all.optJSONObject(i);
                    if(cue!=null&&activeId.equals(cue.optString("id")))active.put(cue);
                }
            }
            mushaf.setSemanticCues(active,false,true);
        }
        else mushaf.setSemanticCues(ayahThemesCues.forPage(page),false,true);
    }

    /** Read-only reproduction of the *existing* Révision active word-hole mask.
     * No session is created, no counters/cursors are advanced and no prefs are changed.
     * All masking is disabled if any authentic word anchor is unverifiable.
     */
    private void applyFreeActiveMask() {
        freeActiveMaskPageReady=false;
        org.json.JSONArray landmarks=new org.json.JSONArray();
        org.json.JSONArray anchors=new org.json.JSONArray();
        try {
            if(ayahThemesCues!=null) {
                landmarks=WordGeometryRepository.shared(this).pageLandmarkBoxes(page);
                anchors=ayahThemesCues.forPage(page);
                if(landmarks.length()==6) {
                    freeActiveMaskPageReady=true;
                    for(int i=0;i<anchors.length();i++) {
                        org.json.JSONObject cue=anchors.getJSONObject(i);
                        if(cue.getInt("anchorWordCount")!=1
                           ||cue.getJSONArray("boxes").length()!=1
                           ||cue.getString("id").isEmpty()) {
                            freeActiveMaskPageReady=false;break;
                        }
                    }
                }
            }
        } catch(Exception unavailable) {
            freeActiveMaskPageReady=false;
        }
        mushaf.setMaskFollowsSelection(false);
        mushaf.setLandmarkLines(null,null);
        if(freeActiveMaskPageReady) {
            mushaf.setPageLandmarkBoxes(landmarks);
            mushaf.setSemanticCues(anchors,true,false);
            mushaf.setMask(100);
        } else {
            // Identical fail-open rule to scheduled Révision active: do not cover
            // the Quran when source-first-word geometry is incomplete.
            mushaf.setMask(0);
            mushaf.clearPageLandmarkBoxes();
            mushaf.clearSemanticCues();
        }
        updateFreeActiveMaskButton();
    }

    private void toggleFreeActiveMask() {
        if(ayahThemesCues==null)return;
        freeActiveMask=!freeActiveMask;
        freeActiveMaskPageReady=false;
        closeSemanticTitle();
        mushaf.clearReveal();
        mushaf.setMask(0);
        mushaf.clearPageLandmarkBoxes();
        mushaf.clearSemanticCues();
        mushaf.setMaskFollowsSelection(!freeActiveMask);
        mushaf.setMaskEntropy(hifzPrefs.maskEntropyFor(freeActiveMask
            ?HifzSessionActivity.MURAJAAH_ACTIVE:"reader_default"));
        updateFreeActiveMaskButton();
        // Repaint the same original QCF folio without Atlas focus overlays.
        // On-page-shown installs exact holes *before* enabling the 100% mask.
        if(freeActiveMask||!showMap114Focus())
            mushaf.show(page,Collections.emptyList(),Collections.emptyList(),0);
        showControls();
    }

    private void updateFreeActiveMaskButton() {
        if(freeActiveMaskButton==null)return;
        freeActiveMaskButton.setSelected(freeActiveMask);
        Ui.setIconDescription(freeActiveMaskButton,freeActiveMask
            ?(freeActiveMaskPageReady
                ?"Désactiver le masque libre · gommer sauf amorces et repères"
                :"Désactiver le masque libre · repères indisponibles sur cette page")
            :"Masque libre · Révision active, gommer sauf amorces");
    }

    private void selectAtlasPassageOnPage(int nextPage){
        if(!mapUnitPreview||atlasPassages.isEmpty())return;
        int s=getIntent().getIntExtra("map114Surah",0);
        int first=getIntent().getIntExtra("map114FocusStart",0);
        int last=getIntent().getIntExtra("map114FocusEnd",0);
        try{
            GeometryRepository qcf=geometry!=null?geometry:GeometryRepository.get(this);
            if(atlasPassagePosition>=0&&atlasPassagePosition<atlasPassages.size()){
                MawdudiPassageIndex.Passage active=atlasPassages.get(atlasPassagePosition);
                int lo=Math.max(first,active.firstAyah),hi=Math.min(last,active.lastAyah);
                if(nextPage>=qcf.pageForVerse(new VerseRef(s,lo))
                    &&nextPage<=MawdudiMapModel.lastPageForVerse(new VerseRef(s,hi),qcf))return;
            }
            for(VerseRef verse:qcf.versesOnLines(qcf.lineIdsOnPage(nextPage))){
                if(verse.getSurah()!=s||verse.getAyah()<first||verse.getAyah()>last)continue;
                for(int k=0;k<atlasPassages.size();k++)
                    if(atlasPassages.get(k).contains(verse.getAyah())) {
                        atlasPassagePosition=k;updateAtlasPassageButtons();return;
                    }
            }
        }catch(RuntimeException noGeometry){/* Preserve focus if QCF geometry is unavailable. */}
    }

    private void updateAtlasPassageButtons(){
        if(atlasPassageUp==null||atlasPassageDown==null)return;
        boolean previous=atlasPassagePosition>0;
        boolean following=atlasPassagePosition>=0&&atlasPassagePosition<atlasPassages.size()-1;
        atlasPassageUp.setEnabled(previous);atlasPassageUp.setAlpha(previous?1f:0.32f);
        atlasPassageDown.setEnabled(following);atlasPassageDown.setAlpha(following?1f:0.32f);
    }

    private void moveAtlasPassage(int direction){
        if(!mapUnitPreview||atlasPassages.isEmpty()||(direction!=1&&direction!=-1))return;
        if(atlasPassagePosition<0)selectAtlasPassageOnPage(page);
        int next=atlasPassagePosition+direction;
        if(next<0||next>=atlasPassages.size())return;
        atlasPassagePosition=next;
        MawdudiPassageIndex.Passage active=atlasPassages.get(next);
        int first=Math.max(active.firstAyah,getIntent().getIntExtra("map114FocusStart",1));
        int target=GeometryRepository.get(this).pageForVerse(new VerseRef(active.surah,first));
        if(page!=target){keepAtlasPassageOnNextPage=true;setPage(target);}
        else{
            if(!showMap114Focus())mushaf.show(page,Collections.emptyList(),Collections.emptyList(),0);
            applySemanticCues();
            updateAtlasCueFooter();
        }
        updateAtlasPassageButtons();
        updateCorrespondences();
    }

    private void showFullAtlasCue(){
        if(!mapUnitPreview||atlasPassagePosition<0||atlasPassagePosition>=atlasPassages.size())return;
        MawdudiPassageIndex.Passage active=atlasPassages.get(atlasPassagePosition);
        String label=ayahThemesCues==null?null:ayahThemesCues.titleForPassage(active.id);
        if(label==null)return;
        semanticTitleDialog=SemanticTitlePopup.showEnglish(this,AtlasDisplayTitleFormatter.display(MawdudiMapScreen.cleanPassageCaption(label)),semanticTitleDialog);
        SemanticTitlePopup.positionAboveIcons(this,semanticTitleDialog,readerActions);
    }

    private void updateAtlasCueFooter(){
        if(!mapUnitPreview||atlasCueFooter==null)return;
        if(ayahThemesCues==null)return;
        if(atlasPassagePosition<0||atlasPassagePosition>=atlasPassages.size())
            selectAtlasPassageOnPage(page);
        if(atlasPassagePosition<0||atlasPassagePosition>=atlasPassages.size()){
            atlasCueFooter.setText("Aucune ancre sur cette page");return;
        }
        MawdudiPassageIndex.Passage active=atlasPassages.get(atlasPassagePosition);
        String sourceTitle=ayahThemesCues.titleForPassage(active.id);
        String title=sourceTitle;
        String unitTitle="";
        if(mapPreviewUnits!=null){
            int first=getIntent().getIntExtra("map114FocusStart",active.firstAyah);
            MawdudiMapData.Macro unit=mapPreviewUnits.at(active.surah,first);
            unitTitle=mapPreviewUnits.displayUnitTitle(unit.id);
            title=mapPreviewUnits.displayContextTitle(active.id,unit.id,sourceTitle);
        }
        if(title==null||title.isEmpty()){
            atlasCueFooter.setText("Aucune ancre sur cette page");return;
        }
        SpannableStringBuilder caption=new SpannableStringBuilder();
        if(!unitTitle.isEmpty()){
            caption.append(unitTitle);
            caption.setSpan(new StyleSpan(Typeface.BOLD),0,caption.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            caption.setSpan(new AbsoluteSizeSpan(Math.round(16f*getResources().getDisplayMetrics().scaledDensity)),0,caption.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            caption.append("\n");
        }
        int passageStart=caption.length();
        caption.append(MawdudiMapScreen.cleanPassageCaption(title));
        caption.setSpan(new StyleSpan(Typeface.ITALIC),passageStart,caption.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        atlasCueFooter.setText(caption);
    }

    private void updateCorrespondences(){
        if(correspondenceHost==null)return;
        if(!controlsVisible){correspondenceHost.setVisibility(View.GONE);return;}
        int s=selected==null?0:selected.getSurah(),a=selected==null?0:selected.getAyah();
        String unitId=null,passageId=null;
        int first=a,last=a;
        if(mapUnitPreview&&atlasPassagePosition>=0&&atlasPassagePosition<atlasPassages.size()){
            MawdudiPassageIndex.Passage active=atlasPassages.get(atlasPassagePosition);
            s=active.surah;
            first=Math.max(active.firstAyah,getIntent().getIntExtra("map114FocusStart",active.firstAyah));
            last=Math.min(active.lastAyah,getIntent().getIntExtra("map114FocusEnd",active.lastAyah));
            if(a<first||a>last)a=first;
            passageId=active.id;
            if(mapPreviewUnits!=null)unitId=mapPreviewUnits.at(s,first).id;
        }
        if(s==0||a==0){correspondenceHost.removeAllViews();correspondenceHost.setVisibility(View.GONE);return;}
        CorrespondenceEntryPoints.populate(this,correspondenceHost,
            CorrespondenceEntryPoints.origin(this,CorrespondenceAccess.Surface.LECTURE,
                mapUnitPreview?"atlas-unit":"lecture",s,a,page,unitId,passageId,
                "cues:"+semanticCuesEnabled+";focus:"+first+"-"+last,s+":"+a,first,last));
    }

    private void showControls() {
        controlsVisible = true;
        topControls.setVisibility(View.VISIBLE);
        readerActions.setVisibility(View.VISIBLE);
        pageRail.setVisibility(View.VISIBLE);
        updateCorrespondences();
        scheduleAutoHide();
    }

    private void hideControls() {
        if (topControls == null || readerActions == null || pageRail == null) return;
        controlsVisible = false;
        if(correspondenceHost!=null)correspondenceHost.setVisibility(View.GONE);
        topControls.setVisibility(View.GONE);
        readerActions.setVisibility(View.GONE);
        pageRail.setVisibility(View.GONE);
        if (mushaf != null) mushaf.removeCallbacks(autoHide);
    }

    private void scheduleAutoHide() {
        if (mushaf == null) return;
        mushaf.removeCallbacks(autoHide);
        if (hifzPrefs != null && eink.isEink(hifzPrefs)) return;
        mushaf.postDelayed(autoHide, 3600L);
    }

    private void openTafsir() {
        final VerseRef verse = selected;
        if (verse == null) {
            Toast.makeText(this, "Touchez d’abord un verset pour ouvrir le Tafsir.", Toast.LENGTH_LONG).show();
            showControls();
            return;
        }
        if(mapPreview) {
            // Atlas/Jalalayn is now non-modal: BOOX side panel, phone bottom panel.
            // Both preserve the genuine QCF page, selected verse and Hifz data.
            if(mapPreviewJalalayn!=null)mapPreviewJalalayn.close();
            int first=getIntent().getIntExtra("map114FocusStart",verse.getAyah());
            int last=getIntent().getIntExtra("map114FocusEnd",verse.getAyah());
            if(mapUnitPreview&&atlasPassagePosition>=0&&atlasPassagePosition<atlasPassages.size()){
                MawdudiPassageIndex.Passage focus=atlasPassages.get(atlasPassagePosition);
                first=Math.max(first,focus.firstAyah);
                last=Math.min(last,focus.lastAyah);
            }
            final ViewGroup panelHost=largeScreen?sideTafsir:bottomTafsir;
            if(panelHost==null)return;
            mapPreviewJalalayn=new MapJalalaynDialog(this,verse.getSurah(),first,last,
                verse.getAyah(),panelHost,()->{
                    panelHost.setVisibility(View.GONE);
                    mushaf.clearReveal();
                    eink.local(panelHost,hifzPrefs);
                    showControls();
                });
            mapPreviewJalalayn.show();
            showControls();
            eink.local(panelHost,hifzPrefs);
            return;
        }
        if (largeScreen) { openSideTafsir(verse); return; }
        openBottomTafsir(verse);
    }

    private boolean isTafsirOpen() {
        return (mapPreviewJalalayn!=null&&mapPreviewJalalayn.isShowing())
            || (largeScreen && sideTafsir != null && sideTafsir.getVisibility() == View.VISIBLE)
            || (!largeScreen && bottomTafsir != null && bottomTafsir.getVisibility() == View.VISIBLE);
    }

    private void refreshOpenTafsir(VerseRef verse) {
        if (verse == null) return;
        if(mapPreview&&mapPreviewJalalayn!=null&&mapPreviewJalalayn.isShowing()) {
            if(mapPreviewJalalayn.accepts(verse))mapPreviewJalalayn.selectVerse(verse);
            else {
                mapPreviewJalalayn.close();
                openTafsir();
            }
            return;
        }
        if (largeScreen && sideTafsir != null && sideTafsir.getVisibility() == View.VISIBLE) {
            openSideTafsir(verse);
            return;
        }
        if (!largeScreen && bottomTafsir != null && bottomTafsir.getVisibility() == View.VISIBLE) {
            bottomTafsir.removeAllViews();
            bottomTafsir.addView(buildTafsirPanel(verse, this::closeBottomTafsir), new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            mushaf.revealSelectionAboveBottomPanel();
            eink.local(bottomTafsir, hifzPrefs);
        }
    }

    private void openBottomTafsir(VerseRef verse) {
        if (bottomTafsir == null) return;
        hideControls();
        bottomTafsir.removeAllViews();
        bottomTafsir.addView(buildTafsirPanel(verse, this::closeBottomTafsir), new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        bottomTafsir.setVisibility(View.VISIBLE);
        mushaf.revealSelectionAboveBottomPanel();
        eink.local(bottomTafsir, hifzPrefs);
    }

    private void closeBottomTafsir() {
        if (bottomTafsir == null) return;
        bottomTafsir.removeAllViews();
        bottomTafsir.setVisibility(View.GONE);
        activeTafsirTarget = null;
        mushaf.clearReveal();
        eink.local(bottomTafsir, hifzPrefs);
        showControls();
    }

    private void closeTafsirPanels() {
        if(mapPreviewJalalayn!=null){mapPreviewJalalayn.close();mapPreviewJalalayn=null;}
        closeSideTafsir();
        if (bottomTafsir != null && bottomTafsir.getVisibility() == View.VISIBLE) closeBottomTafsir();
    }

    private void openSideTafsir(VerseRef verse) {
        sideTafsir.removeAllViews();
        LinearLayout shell = buildTafsirPanel(verse, this::closeSideTafsir);
        sideTafsir.addView(shell, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        sideTafsir.setVisibility(View.VISIBLE);
        showControls();
    }

    private void closeSideTafsir() {
        if (sideTafsir != null) {
            sideTafsir.removeAllViews();
            sideTafsir.setVisibility(View.GONE);
        }
    }

    private LinearLayout buildTafsirPanel(VerseRef verse, Runnable closeAction) {
        SharedPreferences readingPrefs = getSharedPreferences(TAFSIR_PREFS, MODE_PRIVATE);
        final float[] fontSize = {
            Math.max(TAFSIR_MIN_SP, Math.min(TAFSIR_MAX_SP,
                readingPrefs.getFloat(TAFSIR_FONT_KEY, TAFSIR_DEFAULT_SP)))
        };
        final TafsirRepository.Entry[] loaded = { null };
        final MultiTafsirRepository.Edition[] currentEdition = {
            MultiTafsirRepository.Edition.fromStorage(readingPrefs.getString(TAFSIR_EDITION_KEY, "jalalayn"))
        };

        LinearLayout shell = Ui.column(this);
        shell.setPadding(Ui.dp(this, 8), Ui.dp(this, 3), Ui.dp(this, 8), Ui.dp(this, 6));

        LinearLayout titleRow = Ui.row(this);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = Ui.bookText(this, "Tafsir · " + verse.getSurah() + ":" + verse.getAyah(), 13.5f, true);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        title.setGravity(Gravity.CENTER_VERTICAL);
        title.setPadding(Ui.dp(this, 3), 0, Ui.dp(this, 4), 0);
        Ui.weight(title, 1);
        titleRow.addView(title);

        TextView source = Ui.text(this, "", 10.5f, false);
        source.setTextColor(Ui.MUTED);
        source.setPadding(Ui.dp(this, 2), 0, Ui.dp(this, 2), Ui.dp(this, 3));
        source.setVisibility(View.GONE);

        Button minus = tafsirTextControl("A−", "Réduire le texte", v -> {
            fontSize[0] = Math.max(TAFSIR_MIN_SP, fontSize[0] - 2f);
            readingPrefs.edit().putFloat(TAFSIR_FONT_KEY, fontSize[0]).apply();
            if (loaded[0] != null) renderTafsirText(loaded[0], fontSize[0]);
        });
        Button plus = tafsirTextControl("A+", "Agrandir le texte", v -> {
            fontSize[0] = Math.min(TAFSIR_MAX_SP, fontSize[0] + 2f);
            readingPrefs.edit().putFloat(TAFSIR_FONT_KEY, fontSize[0]).apply();
            if (loaded[0] != null) renderTafsirText(loaded[0], fontSize[0]);
        });
        Button info = Ui.iconButton(this, "i", "Référence", v ->
            source.setVisibility(source.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE));
        Button close = Ui.iconButton(this, "×", "Fermer le Tafsir", v -> closeAction.run());
        titleRow.addView(minus);
        titleRow.addView(plus);
        titleRow.addView(info);
        titleRow.addView(close);
        shell.addView(titleRow, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 48)));
        shell.addView(source);

        LinearLayout editionRow = Ui.row(this);
        editionRow.setGravity(Gravity.CENTER_VERTICAL);
        editionRow.setPadding(0, Ui.dp(this, 1), 0, Ui.dp(this, 2));
        editionRow.setVisibility(View.GONE);
        shell.addView(editionRow);

        final LinearLayout textColumn = Ui.column(this);
        textColumn.setPadding(0, 0, 0, 0);
        this.activeTafsirTarget = textColumn;
        ScrollView scroll = new ScrollView(this);
        scroll.addView(textColumn);
        shell.addView(scroll, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        io.execute(() -> {
            try {
                Map<MultiTafsirRepository.Edition, TafsirRepository.Entry> available =
                    new MultiTafsirRepository(this).loadAvailable(verse);
                runOnUiThread(() -> {
                    textColumn.removeAllViews();
                    editionRow.removeAllViews();
                    MultiTafsirRepository.Edition resolved = currentEdition[0];
                    if (!available.isEmpty() && !available.containsKey(resolved)) {
                        resolved = available.containsKey(MultiTafsirRepository.Edition.JALALAYN)
                            ? MultiTafsirRepository.Edition.JALALAYN
                            : available.keySet().iterator().next();
                    }
                    currentEdition[0] = resolved;

                    if (available.isEmpty()) {
                        editionRow.setVisibility(View.GONE);
                        title.setText("Tafsir · " + verse.getSurah() + ":" + verse.getAyah());
                        source.setText("");
                        textColumn.addView(tafsirText("Indisponible pour ce verset.", fontSize[0], false));
                        return;
                    }

                    TafsirRepository.Entry entry = available.get(currentEdition[0]);
                    loaded[0] = entry;
                    readingPrefs.edit().putString(TAFSIR_EDITION_KEY, currentEdition[0].storageValue).apply();
                    applyTafsirIdentity(title, source, verse, entry);
                    renderTafsir(textColumn, entry, fontSize[0]);

                    if (available.size() <= 1) {
                        editionRow.setVisibility(View.GONE);
                    } else {
                        editionRow.setVisibility(View.VISIBLE);
                        Button selector = tafsirEditionSelector(currentEdition[0].displayName + " ▾");
                        selector.setOnClickListener(v -> showTafsirEditionMenu(
                            selector, available, currentEdition, readingPrefs, verse,
                            loaded, title, source, textColumn, fontSize));
                        editionRow.addView(selector, new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 40)));
                    }
                });
            } catch (Throwable error) {
                runOnUiThread(() -> {
                    editionRow.setVisibility(View.GONE);
                    textColumn.removeAllViews();
                    textColumn.addView(tafsirText("Tafsir indisponible.", fontSize[0], false));
                });
            }
        });
        return shell;
    }

    private Button tafsirTextControl(String label, String description, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setAllCaps(false);
        button.setText(label);
        button.setTextSize(13f);
        button.setTextColor(Ui.INK);
        button.setGravity(Gravity.CENTER);
        button.setContentDescription(description);
        button.setOnClickListener(listener);
        button.setStateListAnimator(null);
        button.setElevation(0f);
        button.setBackgroundColor(Color.TRANSPARENT);
        button.setMinimumWidth(0);
        button.setMinimumHeight(0);
        button.setPadding(Ui.dp(this, 3), 0, Ui.dp(this, 3), 0);
        button.setLayoutParams(new LinearLayout.LayoutParams(Ui.dp(this, 44), Ui.dp(this, 44)));
        return button;
    }

    private Button tafsirEditionSelector(String label) {
        Button button = new Button(this);
        button.setAllCaps(false);
        button.setText(label);
        button.setTextSize(12f);
        button.setTextColor(Ui.INK);
        button.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
        button.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        button.setStateListAnimator(null);
        button.setElevation(0f);
        button.setBackgroundColor(Color.TRANSPARENT);
        button.setMinimumWidth(0);
        button.setMinimumHeight(0);
        button.setPadding(Ui.dp(this, 4), 0, Ui.dp(this, 8), 0);
        button.setSingleLine(true);
        button.setEllipsize(TextUtils.TruncateAt.END);
        return button;
    }

    private void showTafsirEditionMenu(
            Button anchor,
            Map<MultiTafsirRepository.Edition, TafsirRepository.Entry> available,
            MultiTafsirRepository.Edition[] currentEdition,
            SharedPreferences readingPrefs,
            VerseRef verse,
            TafsirRepository.Entry[] loaded,
            TextView title,
            TextView source,
            LinearLayout textColumn,
            float[] fontSize) {
        PopupMenu menu = new PopupMenu(this, anchor);
        for (MultiTafsirRepository.Edition edition : MultiTafsirRepository.Edition.values()) {
            if (!available.containsKey(edition)) continue;
            menu.getMenu().add(edition.displayName).setOnMenuItemClickListener(item -> {
                currentEdition[0] = edition;
                readingPrefs.edit().putString(TAFSIR_EDITION_KEY, edition.storageValue).apply();
                anchor.setText(edition.displayName + " ▾");
                TafsirRepository.Entry entry = available.get(edition);
                loaded[0] = entry;
                applyTafsirIdentity(title, source, verse, entry);
                renderTafsir(textColumn, entry, fontSize[0]);
                return true;
            });
        }
        menu.show();
    }

    private LinearLayout activeTafsirTarget;
    private void renderTafsirText(TafsirRepository.Entry entry, float fontSp) {
        if (activeTafsirTarget != null) renderTafsir(activeTafsirTarget, entry, fontSp);
    }

    private void applyTafsirIdentity(TextView title, TextView source, VerseRef verse,
                                     TafsirRepository.Entry entry) {
        title.setText(entry.editionName + " · " + verse.getSurah() + ":" + verse.getAyah());
        String metadata = entry.metadataLine();
        source.setText(metadata.isEmpty() ? entry.editionName : metadata);
    }

    private void renderTafsir(LinearLayout target, TafsirRepository.Entry entry, float fontSp) {
        target.removeAllViews();
        addRunBlocks(target, entry.commentaryRuns, fontSp, false);
        if (!entry.notes.isEmpty()) {
            // "Notes (n) ›" as a plain foldable line, not a boxed button (spec §8).
            String closedLabel = "Notes (" + entry.notes.size() + ") ›";
            String openLabel = "Notes (" + entry.notes.size() + ") ⌄";
            TextView notesToggle = Ui.bookText(this, closedLabel, 13f, true);
            notesToggle.setMinHeight(Ui.dp(this, 44));
            notesToggle.setGravity(Gravity.CENTER_VERTICAL);
            notesToggle.setClickable(true);
            notesToggle.setFocusable(true);
            LinearLayout notes = Ui.column(this);
            notes.setPadding(0, 0, 0, 0);
            notes.setVisibility(View.GONE);
            notesToggle.setOnClickListener(v -> {
                boolean open = notes.getVisibility() != View.VISIBLE;
                notes.setVisibility(open ? View.VISIBLE : View.GONE);
                notesToggle.setText(open ? openLabel : closedLabel);
            });
            target.addView(notesToggle);
            for (TafsirRepository.Note note : entry.notes) {
                TextView label = tafsirText(Integer.toString(note.number) + ".", fontSp, true);
                label.setPadding(0, Ui.dp(this, 6), 0, 0);
                notes.addView(label);
                addRunBlocks(notes, note.runs, Math.max(TAFSIR_MIN_SP, fontSp - 1f), true);
            }
            target.addView(notes);
        }
    }

    private void addRunBlocks(LinearLayout target, List<TafsirRepository.Run> runs, float fontSp, boolean note) {
        ArrayList<TafsirRepository.Run> block = new ArrayList<>();
        Boolean poetry = null;
        for (TafsirRepository.Run run : runs) {
            boolean isPoetry = run.style == TafsirRepository.RunStyle.POETRY;
            if (poetry != null && poetry != isPoetry) {
                addRunBlock(target, block, fontSp, note, poetry);
                block.clear();
            }
            poetry = isPoetry;
            block.add(run);
        }
        if (!block.isEmpty()) addRunBlock(target, block, fontSp, note, Boolean.TRUE.equals(poetry));
    }

    private void addRunBlock(LinearLayout target, List<TafsirRepository.Run> runs,
                             float fontSp, boolean note, boolean poetry) {
        SpannableStringBuilder text = new SpannableStringBuilder();
        for (TafsirRepository.Run run : runs) {
            String value = poetry ? run.text : run.text.replaceAll("\\n{2,}", "\n");
            int start = text.length();
            text.append(value);
            int end = text.length();
            if (end <= start) continue;
            switch (run.style) {
                case ITALIC:
                case TRANSLITERATION:
                case POETRY:
                    text.setSpan(new StyleSpan(Typeface.ITALIC), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    break;
                case BOLD:
                case TECHNICAL_TERM:
                    text.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    break;
                case BOLD_ITALIC:
                    text.setSpan(new StyleSpan(Typeface.BOLD_ITALIC), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    break;
                case NOTE_REF:
                    text.setSpan(new SuperscriptSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    text.setSpan(new RelativeSizeSpan(.78f), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    break;
                default:
                    break;
            }
        }
        TextView view = tafsirText(text, fontSp, false);
        view.setLineSpacing(0f, poetry ? 1.35f : (note ? 1.30f : 1.25f));
        if (poetry) {
            view.setGravity(Gravity.START);
            view.setPadding(Ui.dp(this, 12), 0, Ui.dp(this, 4), Ui.dp(this, 4));
        }
        target.addView(view);
    }

    private TextView tafsirText(CharSequence value, float sp, boolean bold) {
        TextView text = Ui.text(this, value.toString(), sp, bold);
        text.setText(value);
        text.setTextIsSelectable(false);
        return text;
    }

    /** Atlas uses the existing Sabqi/Itqan context veil, scoped to the active genuine passage. */
    private boolean showMap114Focus() {
        if(freeActiveMask)return false; // no Atlas fade-away behind the free recall mask
        if(mapUnitPreview) {
            int surah=getIntent().getIntExtra("map114Surah",0);
            int unitFirst=getIntent().getIntExtra("map114FocusStart",0);
            int unitLast=getIntent().getIntExtra("map114FocusEnd",0);
            if(surah<1||surah>114||unitFirst<1||unitLast<unitFirst)return false;
            try {
                GeometryRepository qcf=GeometryRepository.get(this);
                if(atlasPassagePosition<0||atlasPassagePosition>=atlasPassages.size())
                    selectAtlasPassageOnPage(page);
                if(atlasPassagePosition<0||atlasPassagePosition>=atlasPassages.size())
                    return false;
                MawdudiPassageIndex.Passage active=atlasPassages.get(atlasPassagePosition);
                int first=Math.max(unitFirst,active.firstAyah);
                int last=Math.min(unitLast,active.lastAyah);
                java.util.List<VerseRef> focus=new java.util.ArrayList<>();
                for(VerseRef v:qcf.versesOnLines(qcf.lineIdsOnPage(page)))
                    if(v.getSurah()==surah&&v.getAyah()>=first&&v.getAyah()<=last&&!focus.contains(v))
                        focus.add(v);
                if(focus.isEmpty())return false;
                if(selected==null||!focus.contains(selected))selected=focus.get(0);
                tafsirButton.setContentDescription("Tafsir "+selected.getSurah()+":"+selected.getAyah());
                // The focus veil is the only selection: no unrelated weak-spot outlines.
                mushaf.setHighlightVerses(java.util.Collections.emptyList());
                mushaf.show(page,focus,
                    qcf.lineIdsForVerseRange(new VerseRef(surah,first),new VerseRef(surah,last)),
                    0,true);
                updateAtlasPassageButtons();
                return true;
            }catch(RuntimeException badGeometry){return false;}
        }
        String id=getIntent().getStringExtra("map114PassageId");
        if(!mapPreview||id==null)return false;
        try {
            MawdudiPassageIndex.Passage passage=null;
            for(MawdudiPassageIndex.Passage p:MawdudiPassageIndex.load(this).all())if(p.id.equals(id)){passage=p;break;}
            if(passage==null)return false;
            int first=getIntent().getIntExtra("map114FocusStart",passage.firstAyah);
            int last=getIntent().getIntExtra("map114FocusEnd",passage.lastAyah);
            if(!passage.contains(first)||!passage.contains(last)||first>last)return false;
            GeometryRepository qcf=GeometryRepository.get(this);
            java.util.List<VerseRef> focus=new java.util.ArrayList<>();
            for(VerseRef verse:MawdudiMapModel.highlightedVerses(passage,page,qcf))
                if(verse.getAyah()>=first&&verse.getAyah()<=last)focus.add(verse);
            selected=focus.isEmpty()?null:focus.get(0);
            tafsirButton.setContentDescription(selected==null?"Tafsir · touchez un verset puis ouvrez le commentaire"
                :"Tafsir "+selected.getSurah()+":"+selected.getAyah());
            mushaf.setHighlightVerses(focus);
            mushaf.show(page,focus,focus.isEmpty()?Collections.emptyList():qcf.lineIdsForVerseRange(
                new VerseRef(passage.surah,first),new VerseRef(passage.surah,last)),0,!focus.isEmpty());
            return true;
        } catch(RuntimeException invalidMapFocus) {return false;}
    }
    @Override protected void onSaveInstanceState(Bundle out) {
        // All Lecture entry points consume their Intent jump only once.
        out.putInt("map114ReaderPage",page);
        out.putString("map114SelectedVerse",selected==null?null:selected.getSurah()+":"+selected.getAyah());
        out.putBoolean("map114SemanticCues",semanticCuesEnabled);
        out.putBoolean("readerFreeActiveMask",freeActiveMask);
        out.putBoolean("map114ControlsVisible",controlsVisible);
        out.putBoolean("map114TafsirOpen",isTafsirOpen());
        if(atlasPassagePosition>=0&&atlasPassagePosition<atlasPassages.size())
            out.putString("map114ReaderPassageId",atlasPassages.get(atlasPassagePosition).id);
        if(mapUnitPreview)out.putInt("map114PassageIndex",atlasPassagePosition);
        super.onSaveInstanceState(out);
    }

    @Override public void onReady() {
        if(freeActiveMask){
            mushaf.setMaskFollowsSelection(false);
            mushaf.setMaskEntropy(hifzPrefs.maskEntropyFor(HifzSessionActivity.MURAJAAH_ACTIVE));
        }
        if (pendingJumpVerse != null) {
            selected = pendingJumpVerse;
            tafsirButton.setContentDescription("Tafsir " + pendingJumpVerse.getSurah() + ":" + pendingJumpVerse.getAyah());
            if(!showMap114Focus()) {
                if(freeActiveMask)mushaf.show(page, Collections.emptyList(), Collections.emptyList(), 0);
                else mushaf.show(page, Collections.singletonList(pendingJumpVerse), Collections.emptyList(), 0);
            }
            pendingJumpVerse = null;
        } else {
            if(!showMap114Focus())mushaf.show(page, Collections.emptyList(), Collections.emptyList(), 0);
        }
    }
    @Override public void onError(String message) { Toast.makeText(this, message, Toast.LENGTH_LONG).show(); }
    @Override public void onPageShown(int shown) {
        if(shown!=page)closeSemanticTitle();
        page = shown;
        annotationOverlay.setPage(shown);
        updateSurahPickerLabel();
        updateRubPickerLabel();
        applySemanticCues();
        updateAtlasCueFooter();
        updateAtlasPassageButtons();
        updateCorrespondences();
        boolean keepControlsHidden=!controlsVisible;
        if(restoreAtlasTafsir){restoreAtlasTafsir=false;if(selected!=null)openTafsir();}
        if(keepControlsHidden)hideControls();
        if(mapUnitPreview){
            // Lecture Atlas has no study/debug side-beads or ornamental parallel bars.
            // Rubʿ/Hizb badge and every original QCF glyph remain untouched.
            mushaf.evaluateJavascript("(function(){['centermark','sidemarks'].forEach(function(id){"
                +"var v=document.getElementById(id);if(v)v.style.setProperty('display','none','important');"
                +"});})();",null);
        }
        if(mapPreview&&mapPreviewUnits!=null){
            int surah=getIntent().getIntExtra("map114Surah",0);
            if(surah>=1&&surah<=114){
                GeometryRepository originalQcf=GeometryRepository.get(this);
                if(mapUnitPreview)
                    MapUnitMarkers.showScoped(mushaf,shown,surah,
                        mapPreviewUnits.forSurah(surah),originalQcf);
                else
                    MapUnitMarkers.show(mushaf,shown,surah,
                        mapPreviewUnits.forSurah(surah),originalQcf);
            }
        }
    }
    @Override public void finish() {
        if(mapUnitPreview){
            Intent back=new Intent().putExtra(EXTRA_JUMP_PAGE,page)
                .putExtra("map114Surah",getIntent().getIntExtra("map114Surah",0))
                .putExtra("map114UnitOrdinal",getIntent().getIntExtra("map114UnitOrdinal",0));
            if(atlasPassagePosition>=0&&atlasPassagePosition<atlasPassages.size()){
                MawdudiPassageIndex.Passage active=atlasPassages.get(atlasPassagePosition);
                back.putExtra("map114ReturnPassageId",active.id)
                    .putExtra("map114ReturnPassageStart",active.firstAyah)
                    .putExtra("map114ReturnPassageEnd",active.lastAyah);
            }
            setResult(RESULT_OK,back);
        }
        super.finish();
    }

    @Override public void onBackPressed() {
        if (isTafsirOpen()) { closeTafsirPanels(); return; }
        super.onBackPressed();
    }
    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_PAGE_UP) { go(-1); return true; }
        if (keyCode == KeyEvent.KEYCODE_PAGE_DOWN) { go(1); return true; }
        return super.onKeyDown(keyCode, event);
    }
    @Override protected void onPause() {
        if(mushaf!=null)mushaf.removeCallbacks(autoHide);
        super.onPause();
    }

    @Override protected void onResume() {
        super.onResume();
        if(controlsVisible)scheduleAutoHide();
    }

    @Override protected void onDestroy() {
        if(mapPreviewJalalayn!=null){mapPreviewJalalayn.close();mapPreviewJalalayn=null;}
        io.shutdownNow();
        if (semanticTitleDialog != null && semanticTitleDialog.isShowing()) semanticTitleDialog.dismiss();
        if (mushaf != null) {
            mushaf.removeCallbacks(autoHide);
            mushaf.destroySafely();
        }
        super.onDestroy();
    }
}
