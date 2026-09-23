package com.quransafeguard.hifz.preview;

import android.app.Dialog;
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

    private MushafView mushaf;
    private int page = 1;
    private VerseRef selected;
    private VerseRef pendingJumpVerse;
    private TextView pageLabel;
    private Button tafsirButton;
    private LinearLayout topControls, readerActions, pageRail, rootRow, sideTafsir;
    private FrameLayout readerPane;
    private TextView surahPicker;
    private TextView rubPicker;
    private TextView rubBadge;
    private boolean controlsVisible = true;
    private boolean largeScreen;
    private HifzPrefs hifzPrefs;
    private GeometryRepository geometry;
    private final EinkController eink = new EinkController();
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Runnable autoHide = this::hideControls;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        int jumpPage = getIntent().getIntExtra(EXTRA_JUMP_PAGE, 0);
        page = jumpPage >= 1 && jumpPage <= 604
            ? jumpPage : getSharedPreferences("hifz_study", MODE_PRIVATE).getInt("page", 1);
        String jumpVerse = getIntent().getStringExtra(EXTRA_JUMP_VERSE);
        if (jumpVerse != null) {
            try { pendingJumpVerse = GeometryRepository.parseVerse(jumpVerse); } catch (RuntimeException malformed) { /* ignore */ }
        }
        getSharedPreferences("hifz_study", MODE_PRIVATE).edit().putInt("page", page).apply();
        hifzPrefs = new HifzPrefs(this);
        largeScreen = getResources().getConfiguration().smallestScreenWidthDp >= 600;

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
        rubBadge = Ui.bookText(this, "", 11f, true);
        rubBadge.setGravity(Gravity.CENTER);
        rubBadge.setBackgroundResource(R.drawable.ic_ui_hizb);
        rubBadge.setVisibility(View.GONE);
        int badgeSize = Ui.dp(this, 22);
        LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(badgeSize, badgeSize);
        badgeParams.setMargins(0, 0, Ui.dp(this, 6), 0);
        titleRow.addView(rubBadge, badgeParams);
        pageLabel = Ui.bookText(this, "Lecture · " + page + " / 604", 13f, true);
        titleRow.addView(pageLabel);
        TextView balance = Ui.text(this, "", 1f, false);
        balance.setMinWidth(Ui.dp(this, 44));
        topControls.addView(back);
        topControls.addView(titleRow);
        topControls.addView(balance, new LinearLayout.LayoutParams(Ui.dp(this, 44), Ui.dp(this, 44)));
        readerStack.addView(topControls, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        mushaf = new MushafView(this);
        mushaf.setListener(this);
        readerStack.addView(mushaf, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        readerActions = Ui.row(this);
        readerActions.setGravity(Gravity.CENTER);
        readerActions.setPadding(Ui.dp(this, 6), 0, Ui.dp(this, 6), 0);
        readerActions.setMinimumHeight(Ui.dp(this, 60));
        tafsirButton = tafsirReaderAction();
        readerActions.addView(tafsirButton);
        readerStack.addView(readerActions, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        pageRail = Ui.row(this);
        pageRail.setPadding(Ui.dp(this, 10), 0, Ui.dp(this, 10), Ui.dp(this, 1));
        surahPicker = Ui.bookText(this, "Sourate", 13f, true);
        surahPicker.setGravity(Gravity.CENTER);
        surahPicker.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 8));
        surahPicker.setClickable(true);
        surahPicker.setFocusable(true);
        surahPicker.setEnabled(false);
        surahPicker.setContentDescription("Aller à une sourate");
        surahPicker.setOnClickListener(v -> showSurahPicker());
        pageRail.addView(surahPicker, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        rubPicker = Ui.bookText(this, "", 13f, true);
        rubPicker.setGravity(Gravity.CENTER);
        rubPicker.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 8));
        rubPicker.setClickable(true);
        rubPicker.setFocusable(true);
        rubPicker.setContentDescription("Aller à un Hizb");
        rubPicker.setOnClickListener(v -> showRubPicker());
        pageRail.addView(rubPicker, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        updateRubPickerLabel();
        updateRubBadge();
        LinearLayout.LayoutParams railParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        railParams.topMargin = Ui.dp(this, 20);
        readerStack.addView(pageRail, railParams);
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
        rubPicker.setText(QuranRubNames.currentLabel(page) + " ▾");
    }

    private static final String[] RUB_BADGE_LABEL = {"H", "¼", "½", "¾"};

    private void updateRubBadge() {
        if (rubBadge == null) return;
        int[] row = QuranRubBoundaries.boundaryOnPage(page);
        if (row == null) { rubBadge.setVisibility(View.GONE); return; }
        rubBadge.setText(RUB_BADGE_LABEL[row[5]]);
        rubBadge.setVisibility(View.VISIBLE);
    }

    private void showRubPicker() {
        showControls();
        QuranRubNames.showPicker(this, this::setPage);
    }

    private Button tafsirReaderAction() {
        Button button = new Button(this);
        button.setAllCaps(false);
        button.setText("Tafsir");
        button.setTextSize(14.5f);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(Ui.INK);
        button.setGravity(Gravity.CENTER);
        button.setContentDescription("Tafsir · touchez un verset puis ouvrez le commentaire");
        button.setOnClickListener(v -> openTafsir());
        button.setStateListAnimator(null);
        button.setElevation(0f);
        button.setBackgroundColor(Color.TRANSPARENT);
        button.setMinimumHeight(Ui.dp(this, 60));
        button.setMinimumWidth(Ui.dp(this, 190));
        button.setPadding(Ui.dp(this, 16), 0, Ui.dp(this, 16), 0);
        return button;
    }

    private void setPage(int requested) {
        int next = Math.max(1, Math.min(604, requested));
        if (next == page) { showControls(); return; }
        closeSideTafsir();
        mushaf.clearReveal();
        page = next;
        selected = null;
        tafsirButton.setContentDescription("Tafsir · touchez un verset puis ouvrez le commentaire");
        getSharedPreferences("hifz_study", MODE_PRIVATE).edit().putInt("page", page).apply();
        pageLabel.setText("Lecture · " + page + " / 604");
        updateSurahPickerLabel();
        updateRubPickerLabel();
        updateRubBadge();
        mushaf.show(page, Collections.emptyList(), Collections.emptyList(), 0);
        showControls();
    }

    private void go(int delta) { setPage(page + delta); }

    @Override public void onVerseTap(VerseRef verse) {
        selected = verse;
        tafsirButton.setContentDescription("Tafsir " + verse.getSurah() + ":" + verse.getAyah());
        mushaf.setSelection(Collections.singletonList(verse), Collections.emptyList());
        showControls();
    }

    @Override public void onPageSwipe(int delta) { go(delta); }
    @Override public void onSurfaceTap() { if (controlsVisible) hideControls(); else showControls(); }

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
        hideControls();
        mushaf.revealSelectionAboveBottomPanel();
        final Dialog dialog = new Dialog(this);
        LinearLayout shell = buildTafsirPanel(verse, dialog::dismiss);
        dialog.setContentView(shell);
        dialog.setCanceledOnTouchOutside(true);
        dialog.setOnDismissListener(closed -> { mushaf.clearReveal(); showControls(); });
        dialog.show();
        Window w = dialog.getWindow();
        if (w != null) {
            w.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            w.setWindowAnimations(0);
            w.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            View content = findViewById(android.R.id.content);
            int availableHeight = content == null ? getResources().getDisplayMetrics().heightPixels : content.getHeight();
            w.setLayout(
                Math.min(getResources().getDisplayMetrics().widthPixels - Ui.dp(this, 16), Ui.dp(this, 760)),
                Math.max(Ui.dp(this, 220), Math.round(availableHeight * .42f)));
        }
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
            Button notesToggle = Ui.smallButton(this, "Notes (" + entry.notes.size() + ")", null);
            LinearLayout notes = Ui.column(this);
            notes.setPadding(0, 0, 0, 0);
            notes.setVisibility(View.GONE);
            notesToggle.setOnClickListener(v ->
                notes.setVisibility(notes.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE));
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
        pageLabel.setText("Lecture · " + shown + " / 604");
        updateSurahPickerLabel();
        updateRubPickerLabel();
        updateRubBadge();
    }
    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_PAGE_UP) { go(-1); return true; }
        if (keyCode == KeyEvent.KEYCODE_PAGE_DOWN) { go(1); return true; }
        return super.onKeyDown(keyCode, event);
    }
    @Override protected void onDestroy() {
        io.shutdownNow();
        if (mushaf != null) {
            mushaf.removeCallbacks(autoHide);
            mushaf.destroySafely();
        }
        super.onDestroy();
    }
}
