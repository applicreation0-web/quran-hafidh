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
    private IbnKathirRecitationStarts recitationStarts;
    private boolean semanticCuesEnabled;
    private android.app.Dialog semanticTitleDialog;
    /** Phone Tafsir: a non-modal bottom panel inside the reader (Mushaf stays tappable above it). */
    private FrameLayout bottomTafsir;
    private LinearLayout topControls, readerActions, pageRail, rootRow, sideTafsir;
    private FrameLayout readerPane;
    private TextView surahPicker;
    private TextView rubPicker;
    private boolean controlsVisible = true;
    private boolean mapPreview;
    private boolean largeScreen;
    private HifzPrefs hifzPrefs;
    private GeometryRepository geometry;
    private final EinkController eink = new EinkController();
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Runnable autoHide = this::hideControls;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        mapPreview = getIntent().getBooleanExtra(EXTRA_MAP_PREVIEW, false);
        int jumpPage = getIntent().getIntExtra(EXTRA_JUMP_PAGE, 0);
        page = jumpPage >= 1 && jumpPage <= 604
            ? jumpPage : getSharedPreferences("hifz_study", MODE_PRIVATE).getInt("page", 1);
        String jumpVerse = getIntent().getStringExtra(EXTRA_JUMP_VERSE);
        if (jumpVerse != null) {
            try { pendingJumpVerse = GeometryRepository.parseVerse(jumpVerse); } catch (RuntimeException malformed) { /* ignore */ }
        }
        if (!mapPreview) getSharedPreferences("hifz_study", MODE_PRIVATE).edit().putInt("page", page).apply();
        hifzPrefs = new HifzPrefs(this);
        largeScreen = getResources().getConfiguration().smallestScreenWidthDp >= 600;
        // Lecture only: verified Ibn Kathir passage starts, separate from Hifz masks.
        try {
            recitationStarts=new IbnKathirRecitationStarts(
                IbnKathirGroupIndex.shared(),WordGeometryRepository.shared(this));
        } catch(RuntimeException unavailable){recitationStarts=null;}
        semanticCuesEnabled=recitationStarts!=null&&recitationStarts.isAvailable()
            && (mapPreview||getSharedPreferences("hifz_study",MODE_PRIVATE)
                .getBoolean("ibn_kathir_start_markers_visible",false));

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
        readerActions.addView(Ui.iconButton(this, "", "Carte", v -> openIbnKathirMap()));
        semanticButton = Ui.iconButton(this, "", "Afficher les amorces", v -> toggleSemanticCues());
        semanticButton.setVisibility(recitationStarts!=null&&recitationStarts.isAvailable()?View.VISIBLE:View.GONE);
        readerActions.addView(semanticButton);
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

        if (!largeScreen) {
            bottomTafsir = new FrameLayout(this);
            bottomTafsir.setBackgroundColor(Ui.PAPER);
            bottomTafsir.setVisibility(View.GONE);
            bottomTafsir.setClickable(true);
            bottomTafsir.setFocusable(true);
            int panelHeight = Math.max(Ui.dp(this, 220),
                Math.round(getResources().getDisplayMetrics().heightPixels * .42f));
            readerPane.addView(bottomTafsir, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, panelHeight, Gravity.BOTTOM));
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

    private void openIbnKathirMap() {
        // A temporary Mushaf consultation returns to its existing Carte, never nests another one.
        if (mapPreview) { finish(); return; }
        int currentSurah = selected != null ? selected.getSurah()
            : (geometry == null ? 1 : geometry.firstSurahOnPage(page));
        int currentAyah = selected == null ? 1 : selected.getAyah();
        Intent map = new Intent(this, IbnKathirMapActivity.class);
        map.putExtra(IbnKathirMapActivity.EXTRA_SURAH, currentSurah);
        map.putExtra(IbnKathirMapActivity.EXTRA_AYAH, currentAyah);
        startActivity(map);
    }

    private void setPage(int requested) {
        int next = Math.max(1, Math.min(604, requested));
        if (next == page) { showControls(); return; }
        closeTafsirPanels();
        mushaf.clearReveal();
        page = next;
        selected = null;
        tafsirButton.setContentDescription("Tafsir · touchez un verset puis ouvrez le commentaire");
        if (!mapPreview) getSharedPreferences("hifz_study", MODE_PRIVATE).edit().putInt("page", page).apply();
        updateSurahPickerLabel();
        updateRubPickerLabel();
        mushaf.show(page, Collections.emptyList(), Collections.emptyList(), 0);
        showControls();
    }

    private void go(int delta) { setPage(page + delta); }

    /** With the Tafsir open, tapping another verse selects it and refreshes the same panel in
     *  place (no close/reopen, same A−/A+ size and edition from the reading prefs). */
    @Override public void onVerseTap(VerseRef verse) {
        boolean tafsirWasOpen = isTafsirOpen();
        selected = verse;
        tafsirButton.setContentDescription("Tafsir " + verse.getSurah() + ":" + verse.getAyah());
        mushaf.setSelection(Collections.singletonList(verse), Collections.emptyList());
        refreshOpenTafsir(verse);
        if (tafsirWasOpen && !largeScreen) hideControls(); else showControls();
    }

    @Override public void onPageSwipe(int delta) { go(delta); }
    @Override public void onSurfaceTap() {
        if (isTafsirOpen()) return;
        if (controlsVisible) hideControls(); else showControls();
    }
    @Override public void onSemanticCueTap(String passageId) {
        if(!semanticCuesEnabled||recitationStarts==null)return;
        IbnKathirGroupIndex.Group group=recitationStarts.groupForId(passageId);
        if(group==null)return;
        if(semanticTitleDialog!=null)semanticTitleDialog.dismiss();
        semanticTitleDialog=new android.app.AlertDialog.Builder(this)
            .setTitle("Départ Ibn Kathīr · "+group.navigationRange())
            .setMessage("Premier mot exact du groupe documentaire. "
                +"Ce repère de début n'est pas encore une amorce de récitation validée.")
            .setPositiveButton("Lire Ibn Kathīr",(dialog,which)->{
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW,
                        android.net.Uri.parse(IbnKathirQuranComSource.urlForGroup(group))));
                } catch(android.content.ActivityNotFoundException missing) {
                    Toast.makeText(this,"Navigateur indisponible.",Toast.LENGTH_LONG).show();
                }
            })
            .setNeutralButton("Carte",(dialog,which)->{
                Intent map=new Intent(this,IbnKathirMapActivity.class);
                map.putExtra(IbnKathirMapActivity.EXTRA_SURAH,group.surah);
                map.putExtra(IbnKathirMapActivity.EXTRA_AYAH,group.startAyah);
                startActivity(map);
            })
            .setNegativeButton("Fermer",(dialog,which)->dialog.dismiss())
            .create();
        semanticTitleDialog.show();
    }

    /** Lecture-only Ibn Kathir source starts: NOT editorially approved mnemonic keys. */
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
        if(recitationStarts==null||!recitationStarts.isAvailable())return;
        semanticCuesEnabled=!semanticCuesEnabled;
        if(!mapPreview)getSharedPreferences("hifz_study",MODE_PRIVATE).edit()
            .putBoolean("ibn_kathir_start_markers_visible",semanticCuesEnabled).apply();
        updateSemanticButton();
        applySemanticCues();
        showControls();
    }

    private void updateSemanticButton() {
        if (semanticButton == null) return;
        semanticButton.setSelected(semanticCuesEnabled);
        String description=semanticCuesEnabled?"Masquer les départs Ibn Kathīr"
            :"Afficher les départs Ibn Kathīr";
        Ui.setIconDescription(semanticButton,description);
        int icon=Ui.iconFor(semanticCuesEnabled?"Masquer les amorces":"Afficher les amorces","");
        if (icon != 0) Ui.setButtonIcon(semanticButton, icon);
    }

    private void applySemanticCues() {
        if (mushaf == null) return;
        if(!semanticCuesEnabled||recitationStarts==null||!recitationStarts.isAvailable()){
            mushaf.clearSemanticCues();
            return;
        }
        mushaf.setSemanticCues(recitationStarts.forPage(page),false,true);
    }

    private void showControls() {
        controlsVisible = true;
        topControls.setVisibility(View.VISIBLE);
        readerActions.setVisibility(View.VISIBLE);
        pageRail.setVisibility(View.VISIBLE);
        scheduleAutoHide();
    }

    private void hideControls() {
        if (topControls == null || readerActions == null || pageRail == null) return;
        controlsVisible = false;
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
        if (largeScreen) { openSideTafsir(verse); return; }
        openBottomTafsir(verse);
    }

    private boolean isTafsirOpen() {
        return (largeScreen && sideTafsir != null && sideTafsir.getVisibility() == View.VISIBLE)
            || (!largeScreen && bottomTafsir != null && bottomTafsir.getVisibility() == View.VISIBLE);
    }

    private void refreshOpenTafsir(VerseRef verse) {
        if (verse == null) return;
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

    @Override public void onReady() {
        if (pendingJumpVerse != null) {
            selected = pendingJumpVerse;
            tafsirButton.setContentDescription("Tafsir " + pendingJumpVerse.getSurah() + ":" + pendingJumpVerse.getAyah());
            mushaf.show(page, Collections.singletonList(pendingJumpVerse), Collections.emptyList(), 0);
            pendingJumpVerse = null;
        } else {
            mushaf.show(page, Collections.emptyList(), Collections.emptyList(), 0);
        }
    }
    @Override public void onError(String message) { Toast.makeText(this, message, Toast.LENGTH_LONG).show(); }
    @Override public void onPageShown(int shown) {
        page = shown;
        annotationOverlay.setPage(shown);
        updateSurahPickerLabel();
        updateRubPickerLabel();
        applySemanticCues();
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
    @Override protected void onDestroy() {
        io.shutdownNow();
        if (semanticTitleDialog != null && semanticTitleDialog.isShowing()) semanticTitleDialog.dismiss();
        if (mushaf != null) {
            mushaf.removeCallbacks(autoHide);
            mushaf.destroySafely();
        }
        super.onDestroy();
    }
}
