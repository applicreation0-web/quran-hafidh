package com.quransafeguard.hifz.preview;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.graphics.drawable.GradientDrawable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.quransafeguard.hifz.core.VerseRef;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Al-Baqarah PRIVATE phone pilot: one continuous map, two scales, no large cards.
 * Macro ranges: Mawdudi's Topics and their Interconnection, short editorial labels.
 * Fine titles: private Ayah Themes index (never presented as Mawdudi's own titles).
 *
 * This screen never reads nor mutates Hifz sessions/progress/review engines.
 */
final class BaqaraPilotScreen implements MushafView.Listener {
    private static final int INK = 0xff292825;
    private static final int MUTED = 0xff6b6a65;
    private static final int LINE = 0xffaba89f;

    private final IbnKathirMapActivity host;
    private final Activity activity;
    private final GeometryRepository geometry;
    private final List<BaqaraPilotSpec.Theme> themes;
    private List<BaqaraAyahThemeTitles.Passage> passages;
    private final int initialAyah;

    private int theme = -1; // -1: global Maududi overview; >=0: Ayah Themes in one macro range.
    private int selectedIndex;
    private int previewPage;
    private int scrollY;
    private int priorScroll;
    private int commentaryScrollY;
    private int commentaryAyah;
    private int commentaryRequest;
    private boolean commentaryOpen;
    private boolean destroyed;
    private final ExecutorService commentaryIo = Executors.newSingleThreadExecutor();
    private final TafsirRepository jalalayn;
    private boolean ready;
    private boolean mushafExpanded;

    private ScrollView mapScroll, commentaryScroll;
    private FrameLayout mapHost;
    private LinearLayout mapColumn, root;
    private TextView pageIndicator, caption, mapHeading, commentaryHeading, commentaryBody;
    private Button previousPage, nextPage, previousCommentaryVerse, nextCommentaryVerse;
    private LinearLayout pageControlsRow;
    private MushafView mushaf;
    private LinearLayout.LayoutParams mapSizing, previewSizing;

    BaqaraPilotScreen(IbnKathirMapActivity activity, GeometryRepository geometry, Bundle saved,
                      int requestedAyah) {
        this.host = activity;
        this.activity = activity;
        this.geometry = geometry;
        jalalayn = new TafsirRepository(activity);
        initialAyah = Math.max(1, Math.min(286, requestedAyah));
        themes = BaqaraPilotSpec.themes();
        passages = BaqaraAyahThemeTitles.load(activity);
        selectedIndex = passageIndexForAyah(initialAyah);
        if (saved != null) {
            theme = Math.max(-1, Math.min(9, saved.getInt("bp_theme", -1)));
            selectedIndex = Math.max(0, Math.min(passages.size() - 1,
                saved.getInt("bp_selected", selectedIndex)));
            previewPage = saved.getInt("bp_page", 0);
            scrollY = saved.getInt("bp_scroll", 0);
            priorScroll = saved.getInt("bp_prior", 0);
            commentaryOpen = saved.getBoolean("bp_commentary", false);
            commentaryScrollY = saved.getInt("bp_commentary_scroll", 0);
            commentaryAyah = saved.getInt("bp_commentary_ayah", passages.get(selectedIndex).first);
            mushafExpanded = saved.getBoolean("bp_expanded", false);
        }
        if (commentaryAyah == 0) commentaryAyah = passages.get(selectedIndex).first;
    }

    void display() {
        root = Ui.column(host);
        root.setBackgroundColor(Ui.PAPER);
        root.setPadding(Ui.dp(host, 6), 0, Ui.dp(host, 6), Ui.dp(host, 4));

        LinearLayout top = Ui.row(host);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.addView(Ui.iconButton(activity, "", "Retour", v -> {
            if (!handleBack()) host.finish();
        }));
        TextView title = Ui.bookText(host, "Al-Baqarah  ·  1–286", 17f, true);
        title.setGravity(Gravity.CENTER);
        top.addView(title, new LinearLayout.LayoutParams(0, Ui.dp(host, 45), 1f));
        top.addView(Ui.iconButton(activity, "", "Choisir une sourate", v ->
            QuranSurahNames.showSurahPicker(host, chosen -> {
                Intent intent = new Intent(host, IbnKathirMapActivity.class);
                intent.putExtra(IbnKathirMapActivity.EXTRA_SURAH, chosen);
                intent.putExtra(IbnKathirMapActivity.EXTRA_AYAH, 1);
                host.startActivity(intent);
                host.finish();
            })));
        root.addView(top);

        LinearLayout mapHeader = Ui.row(host);
        mapHeader.setGravity(Gravity.CENTER_VERTICAL);
        Button overview = Ui.iconButton(activity, "", "Accueil", v -> showOverview());
        overview.setContentDescription("Vue générale de la sourate");
        mapHeader.addView(overview, new LinearLayout.LayoutParams(Ui.dp(host, 48), Ui.dp(host, 48)));
        mapHeading = Ui.bookText(host, "", 13.5f, true);
        mapHeading.setGravity(Gravity.CENTER_VERTICAL);
        mapHeading.setSingleLine(true);
        mapHeading.setEllipsize(TextUtils.TruncateAt.END);
        mapHeader.addView(mapHeading, new LinearLayout.LayoutParams(0, Ui.dp(host, 37), 1f));
        Button scale = Ui.iconButton(activity, "", "Focus", v -> {
            mushafExpanded = !mushafExpanded; updateSplit();
        });
        scale.setContentDescription("Agrandir ou réduire le Muṣḥaf");
        mapHeader.addView(scale, new LinearLayout.LayoutParams(Ui.dp(host, 48), Ui.dp(host, 48)));
        root.addView(mapHeader);
        root.addView(Ui.divider(host));

        mapScroll = new ScrollView(host);
        mapScroll.setVerticalScrollBarEnabled(false);
        mapColumn = Ui.column(host);
        mapColumn.setPadding(Ui.dp(host, 5), Ui.dp(host, 1), Ui.dp(host, 5), Ui.dp(host, 3));
        mapScroll.addView(mapColumn, new ScrollView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        commentaryScroll = new ScrollView(host);
        commentaryScroll.setVerticalScrollBarEnabled(true);
        commentaryScroll.setVisibility(View.GONE);
        LinearLayout commentaryColumn = Ui.column(host);
        commentaryColumn.setPadding(Ui.dp(host, 12), Ui.dp(host, 6),
            Ui.dp(host, 12), Ui.dp(host, 12));
        Button returnToMap = Ui.iconButton(activity, "", "Retour", v -> closeCommentary());
        returnToMap.setContentDescription("Retour à la carte");
        LinearLayout tafsirControls = Ui.row(host);
        tafsirControls.setGravity(Gravity.CENTER_VERTICAL);
        tafsirControls.addView(returnToMap, new LinearLayout.LayoutParams(
            Ui.dp(host, 44), Ui.dp(host, 40)));
        previousCommentaryVerse = Ui.iconButton(activity, "", "Verset précédent",
            v -> moveCommentaryVerse(-1));
        nextCommentaryVerse = Ui.iconButton(activity, "", "Verset suivant",
            v -> moveCommentaryVerse(1));
        tafsirControls.addView(previousCommentaryVerse, new LinearLayout.LayoutParams(
            Ui.dp(host, 44), Ui.dp(host, 40)));
        tafsirControls.addView(nextCommentaryVerse, new LinearLayout.LayoutParams(
            Ui.dp(host, 44), Ui.dp(host, 40)));
        commentaryColumn.addView(tafsirControls);
        commentaryHeading = Ui.bookText(host, "", 15f, true);
        commentaryColumn.addView(commentaryHeading);
        commentaryBody = Ui.bookText(host, "", 13.5f, false);
        commentaryBody.setPadding(0, Ui.dp(host, 7), 0, Ui.dp(host, 10));
        commentaryBody.setTextIsSelectable(true);
        commentaryColumn.addView(commentaryBody);
        commentaryScroll.addView(commentaryColumn, new ScrollView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        mapHost = new FrameLayout(host);
        mapHost.addView(mapScroll, new FrameLayout.LayoutParams(-1, -1));
        mapHost.addView(commentaryScroll, new FrameLayout.LayoutParams(-1, -1));
        mapSizing = new LinearLayout.LayoutParams(-1, 0, .18f);
        root.addView(mapHost, mapSizing);
        root.addView(Ui.divider(host));

        LinearLayout preview = Ui.column(host);
        caption = Ui.bookText(host, "", 13.5f, true);
        caption.setMaxLines(1);
        caption.setEllipsize(TextUtils.TruncateAt.END);
        caption.setGravity(Gravity.CENTER);
        caption.setPadding(Ui.dp(host, 8), Ui.dp(host, 4),
            Ui.dp(host, 8), Ui.dp(host, 2));
        caption.setContentDescription("Ouvrir le commentaire du passage");
        caption.setClickable(true);
        caption.setFocusable(true);
        caption.setOnClickListener(v -> openCommentary());
        LinearLayout cueNavigation = Ui.row(host);
        cueNavigation.setGravity(Gravity.CENTER_VERTICAL);
        Button previousCue = Ui.iconButton(activity, "", "Passage précédent du corpus",
            v -> movePassage(-1));
        cueNavigation.addView(previousCue,
            new LinearLayout.LayoutParams(Ui.dp(host, 48), Ui.dp(host, 48)));
        cueNavigation.addView(caption, new LinearLayout.LayoutParams(0, -2, 1f));
        Button nextCue = Ui.iconButton(activity, "", "Passage suivant du corpus",
            v -> movePassage(1));
        cueNavigation.addView(nextCue,
            new LinearLayout.LayoutParams(Ui.dp(host, 48), Ui.dp(host, 48)));
        preview.addView(cueNavigation);

        pageControlsRow = Ui.row(host);
        pageControlsRow.setGravity(Gravity.CENTER_VERTICAL);
        previousPage = Ui.iconButton(activity, "", "Page précédente", v -> turnPage(-1));
        nextPage = Ui.iconButton(activity, "", "Page suivante", v -> turnPage(1));
        pageIndicator = Ui.text(host, "", 12f, true);
        pageIndicator.setGravity(Gravity.CENTER);
        pageControlsRow.addView(previousPage);
        pageControlsRow.addView(pageIndicator, new LinearLayout.LayoutParams(0, -2, 1f));
        pageControlsRow.addView(nextPage);
        // A one-page passage needs no second navigation row: keep the Mushaf larger.
        preview.addView(pageControlsRow);

        mushaf = new MushafView(host);
        host.attachPilotPreview(mushaf);
        mushaf.setContentDescription("Muṣḥaf QCF, toucher pour ouvrir Lecture plein écran");
        mushaf.setListener(this);
        preview.addView(mushaf, new LinearLayout.LayoutParams(-1, 0, 1f));
        previewSizing = new LinearLayout.LayoutParams(-1, 0, .82f);
        root.addView(preview, previewSizing);

        host.setContentView(root);
        Ui.respectSystemBars(host, root, 0, 0, 0, 0);
        updateSplit();
        drawMap(false);
        if (commentaryOpen) displayCommentary();
        renderPreview();
    }

    private void updateSplit() {
        if (mapSizing == null || previewSizing == null) return;
        mapSizing.weight = mushafExpanded ? .09f : .18f;
        previewSizing.weight = mushafExpanded ? .91f : .82f;
        mapHost.setLayoutParams(mapSizing);
        if (previewSizing != null && root.getChildCount() > 0)
            root.getChildAt(root.getChildCount() - 1).setLayoutParams(previewSizing);
    }

    private int passageIndexForAyah(int ayah) {
        for (int i = 0; i < passages.size(); i++) if (passages.get(i).contains(ayah)) return i;
        return 0;
    }

    private BaqaraAyahThemeTitles.Passage chosen() { return passages.get(selectedIndex); }

    private void showOverview() {
        if (commentaryOpen) closeCommentary();
        if (theme != -1) { theme = -1; scrollY = priorScroll; drawMap(false); }
    }

    private void openTheme(int index) {
        priorScroll = mapScroll.getScrollY();
        theme = index;
        int target = themes.get(index).first;
        // Selection moves within the chosen theme, without changing memorization state.
        selectedIndex = passageIndexForAyah(target);
        previewPage = 0;
        scrollY = 0;
        drawMap(true);
        renderPreview();
    }

    private void drawMap(boolean resetScroll) {
        mapColumn.removeAllViews();
        mapHeading.setText(theme == -1 ? "10 themes  ·  whole surah" :
            themes.get(theme).displayTitle);
        if (theme == -1) drawOverviewNodes();
        else drawPassageNodes();
        int to = resetScroll ? 0 : scrollY;
        mapScroll.post(() -> mapScroll.scrollTo(0, Math.max(0, to)));
    }

    private void drawOverviewNodes() {
        for (int i = 0; i < themes.size(); i++) {
            final int destination = i;
            BaqaraPilotSpec.Theme block = themes.get(i);
            LinearLayout row = Ui.row(host);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, Ui.dp(host, 2), 0, Ui.dp(host, 2));
            LinearLayout spine = marker(false);
            row.addView(spine, new LinearLayout.LayoutParams(
                Ui.dp(host, 27), Ui.dp(host, 46)));
            LinearLayout text = Ui.column(host);
            TextView name = Ui.bookText(host, block.displayTitle, 14f, true);
            name.setMaxLines(2);
            text.addView(name);
            TextView verse = Ui.text(host, block.range(), 11.5f, false);
            verse.setTextColor(MUTED);
            text.addView(verse);
            row.addView(text, new LinearLayout.LayoutParams(0, -2, 1f));
            android.widget.ImageView go = new android.widget.ImageView(host);
            go.setImageResource(R.drawable.ic_ui_next);
            go.setColorFilter(MUTED);
            row.addView(go, new LinearLayout.LayoutParams(
                Ui.dp(host, 20), Ui.dp(host, 20)));
            row.setClickable(true);
            row.setFocusable(true);
            row.setContentDescription(block.displayTitle + ", " + block.range());
            row.setOnClickListener(v -> openTheme(destination));
            mapColumn.addView(row);
        }
    }

    private void drawPassageNodes() {
        BaqaraPilotSpec.Theme area = themes.get(theme);
        for (int i = 0; i < passages.size(); i++) {
            BaqaraAyahThemeTitles.Passage passage = passages.get(i);
            // A passage belongs to exactly one macro stage, by its first ayah.
            // Crossing a macro border NEVER truncates that actual ayah passage.
            if (passage.last < area.first || passage.first > area.last) continue;
            final int position = i;
            boolean active = position == selectedIndex;
            LinearLayout row = Ui.row(host);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, Ui.dp(host, 2), 0, Ui.dp(host, 2));
            row.addView(marker(active), new LinearLayout.LayoutParams(
                Ui.dp(host, 27), Ui.dp(host, 46)));
            LinearLayout text = Ui.column(host);
            TextView name = Ui.bookText(host, passage.title, 13.5f, true);
            name.setMaxLines(2);
            if (!passage.verified) name.setTextColor(MUTED);
            text.addView(name);
            TextView range = Ui.text(host, passage.range(), 11f, false);
            range.setTextColor(MUTED);
            text.addView(range);
            row.addView(text, new LinearLayout.LayoutParams(0, -2, 1f));
            row.setClickable(true);
            row.setFocusable(true);
            row.setContentDescription(passage.title + ", " + passage.range());
            row.setOnClickListener(v -> select(position));
            mapColumn.addView(row);
        }
    }

    private LinearLayout marker(boolean active) {
        LinearLayout holder = Ui.column(host);
        holder.setGravity(Gravity.CENTER_HORIZONTAL);
        View upper = new View(host);
        upper.setBackgroundColor(LINE);
        holder.addView(upper, new LinearLayout.LayoutParams(Ui.dp(host, 1), 0, 1f));
        View dot = new View(host);
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(active ? INK : Ui.PAPER);
        circle.setStroke(Ui.dp(host, 1), INK);
        dot.setBackground(circle);
        holder.addView(dot, new LinearLayout.LayoutParams(
            Ui.dp(host, active ? 12 : 8), Ui.dp(host, active ? 12 : 8)));
        View lower = new View(host);
        lower.setBackgroundColor(LINE);
        holder.addView(lower, new LinearLayout.LayoutParams(Ui.dp(host, 1), 0, 1f));
        return holder;
    }

    private void select(int position) {
        scrollY = mapScroll.getScrollY();
        selectedIndex = position;
        previewPage = 0;
        commentaryAyah = chosen().first;
        drawMap(false);
        renderPreview();
    }

    private int pageStart() {
        return geometry.pageForVerse(new VerseRef(2, chosen().first));
    }
    private int pageEnd() {
        // Share the real QCF final-line resolver with the 114-surah map model.
        // pageForVerse() returns only the FIRST page of a verse.
        return MawdudiMapModel.lastPageForVerse(new VerseRef(2, chosen().last), geometry);
    }

    private void renderPreview() {
        if (!ready || mushaf == null) return;
        int first = pageStart(), last = pageEnd();
        if (previewPage < first || previewPage > last) previewPage = first;
        LinkedHashSet<VerseRef> exact = new LinkedHashSet<>();
        for (VerseRef verse : geometry.versesOnLines(geometry.lineIdsOnPage(previewPage))) {
            if (verse.getSurah() == 2 && verse.getAyah() >= chosen().first
                    && verse.getAyah() <= chosen().last)
                exact.add(verse);
        }
        List<VerseRef> selected = new ArrayList<>(exact);
        // In commentary focus, highlight the exact verse already linked to Jalalayn.
        if (commentaryOpen && commentaryAyah >= chosen().first && commentaryAyah <= chosen().last) {
            VerseRef focus = new VerseRef(2, commentaryAyah);
            if (exact.contains(focus)) selected = Collections.singletonList(focus);
        }
        caption.setText(chosen().title + "   ·   " + chosen().range());
        pageIndicator.setText("Page " + previewPage +
            (first == last ? "" : "  ·  " + first + "–" + last));
        // Suppress redundant page arrows on single-page passages, not the gestures.
        pageControlsRow.setVisibility(first == last ? View.GONE : View.VISIBLE);
        previousPage.setEnabled(previewPage > first);
        nextPage.setEnabled(previewPage < last);
        previousPage.setAlpha(previewPage > first ? 1f : .28f);
        nextPage.setAlpha(previewPage < last ? 1f : .28f);
        mushaf.setHighlightVerses(selected);
        mushaf.show(previewPage, selected, Collections.emptyList(), 0);
    }

    private void turnPage(int change) {
        int next = Math.max(pageStart(), Math.min(pageEnd(), previewPage + change));
        if (next != previewPage) { previewPage = next; renderPreview(); }
    }

    void movePassage(int delta) {
        int next = Math.max(0, Math.min(passages.size() - 1, selectedIndex + delta));
        if (next == selectedIndex) return;
        selectedIndex = next;
        previewPage = 0;
        commentaryAyah = chosen().first;
        int newTheme = BaqaraPilotSpec.themeIndexForAyah(chosen().first);
        if (newTheme != theme) { theme = newTheme; scrollY = 0; }
        drawMap(false);
        renderPreview();
    }

    private void openCommentary() {
        if (commentaryOpen) return;
        scrollY = mapScroll.getScrollY();
        commentaryScrollY = 0;
        commentaryAyah = chosen().first;
        commentaryOpen = true;
        displayCommentary();
    }

    /** Uses the EXISTING audited offline Jalalayn database. Macro theme labels
     *  alone come from Mawdudi; source commentary and notes never do. */
    private void displayCommentary() {
        if (!commentaryOpen) return;
        mapScroll.setVisibility(View.GONE);
        commentaryScroll.setVisibility(View.VISIBLE);
        showCommentaryVerse();
        commentaryScroll.post(() -> commentaryScroll.scrollTo(0, Math.max(0, commentaryScrollY)));
    }

    static int boundedTafsirAyah(int first, int last, int ayah) {
        if (first < 1 || last < first || last > 286)
            throw new IllegalArgumentException("Invalid Al-Baqarah passage");
        return Math.max(first, Math.min(last, ayah));
    }

    static int stepTafsirAyah(int first, int last, int ayah, int direction) {
        if (direction != -1 && direction != 1)
            throw new IllegalArgumentException("Tafsir step must be +/-1");
        return boundedTafsirAyah(first, last,
            boundedTafsirAyah(first, last, ayah) + direction);
    }

    private void moveCommentaryVerse(int direction) {
        if (!commentaryOpen) return;
        int next = stepTafsirAyah(chosen().first, chosen().last, commentaryAyah, direction);
        if (next == commentaryAyah) return;
        commentaryAyah = next;
        commentaryScrollY = 0;
        showCommentaryVerse();
        commentaryScroll.post(() -> commentaryScroll.scrollTo(0, 0));
    }

    private void showCommentaryVerse() {
        if (!commentaryOpen || destroyed) return;
        BaqaraAyahThemeTitles.Passage passage = chosen();
        commentaryAyah = boundedTafsirAyah(passage.first, passage.last, commentaryAyah);
        final int ayah = commentaryAyah;
        final int passageIndex = selectedIndex;
        final int request = ++commentaryRequest;
        final VerseRef verse = new VerseRef(2, ayah);
        previousCommentaryVerse.setEnabled(ayah > passage.first);
        nextCommentaryVerse.setEnabled(ayah < passage.last);
        previousCommentaryVerse.setAlpha(ayah > passage.first ? 1f : .28f);
        nextCommentaryVerse.setAlpha(ayah < passage.last ? 1f : .28f);
        commentaryHeading.setText("Jalalayn · 2:" + ayah + "\n" + passage.range());
        commentaryBody.setText("Loading existing offline Tafsir al-Jalalayn…");
        if (ready) {
            int focusPage = geometry.pageForVerse(verse);
            if (previewPage != focusPage) previewPage = focusPage;
            renderPreview();
        }
        commentaryIo.execute(() -> {
            TafsirRepository.Entry result = null;
            try { result = jalalayn.load(verse); }
            catch (Exception unavailable) { /* Existing offline repository fails closed. */ }
            final TafsirRepository.Entry entry = result;
            host.runOnUiThread(() -> {
                if (destroyed || !commentaryOpen || commentaryRequest != request ||
                    passageIndex != selectedIndex || ayah != commentaryAyah) return;
                if (entry == null) {
                    commentaryBody.setText("Jalalayn unavailable for this verse in this build.");
                    return;
                }
                commentaryBody.setText(renderExistingJalalayn(entry));
            });
        });
    }

    private static void appendJalalaynRuns(SpannableStringBuilder builder,
                                           List<TafsirRepository.Run> runs) {
        for (TafsirRepository.Run run : runs) {
            int from = builder.length();
            builder.append(run.text);
            int style = 0;
            switch (run.style) {
                case BOLD: case TECHNICAL_TERM: style = Typeface.BOLD; break;
                case BOLD_ITALIC: style = Typeface.BOLD_ITALIC; break;
                case ITALIC: case TRANSLITERATION: case POETRY: style = Typeface.ITALIC; break;
                default: break;
            }
            if (style != 0 && builder.length() > from)
                builder.setSpan(new StyleSpan(style), from, builder.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private static CharSequence renderExistingJalalayn(TafsirRepository.Entry entry) {
        SpannableStringBuilder output = new SpannableStringBuilder();
        String attribution = entry.metadataLine();
        if (!attribution.isEmpty()) output.append(attribution).append("\n\n");
        appendJalalaynRuns(output, entry.commentaryRuns);
        for (TafsirRepository.Note note : entry.notes) {
            output.append("\n\nNote ").append(Integer.toString(note.number)).append("\n");
            appendJalalaynRuns(output, note.runs);
        }
        return output;
    }

    private void closeCommentary() {
        if (!commentaryOpen) return;
        commentaryScrollY = commentaryScroll.getScrollY();
        commentaryRequest++;
        commentaryOpen = false;
        commentaryScroll.setVisibility(View.GONE);
        mapScroll.setVisibility(View.VISIBLE);
        mapScroll.post(() -> mapScroll.scrollTo(0, Math.max(0, scrollY)));
    }

    boolean handleBack() {
        if (commentaryOpen) { closeCommentary(); return true; }
        if (theme != -1) { showOverview(); return true; }
        return false;
    }

    void save(Bundle out) {
        out.putInt("bp_theme", theme);
        out.putInt("bp_selected", selectedIndex);
        out.putInt("bp_page", previewPage);
        out.putInt("bp_scroll", mapScroll == null ? scrollY :
            (commentaryOpen ? scrollY : mapScroll.getScrollY()));
        out.putInt("bp_prior", priorScroll);
        out.putBoolean("bp_commentary", commentaryOpen);
        out.putInt("bp_commentary_scroll", commentaryOpen
            ? commentaryScroll.getScrollY() : commentaryScrollY);
        out.putBoolean("bp_expanded", mushafExpanded);
        out.putInt("bp_commentary_ayah", commentaryAyah);
    }

    /** Expose the same real QCF view for existing runtime integrity tests. */
    MushafView mushafForIntegrityCheck() { return mushaf; }

    void destroy() {
        destroyed = true;
        commentaryRequest++;
        commentaryIo.shutdownNow();
        if (mushaf != null) mushaf.destroySafely();
    }

    private void openReader() {
        if (!ready) return;
        List<VerseRef> exact = new ArrayList<>();
        for (VerseRef verse : geometry.versesOnLines(geometry.lineIdsOnPage(previewPage)))
            if (verse.getSurah() == 2 && verse.getAyah() >= chosen().first
                && verse.getAyah() <= chosen().last) exact.add(verse);
        if (exact.isEmpty()) return;
        Intent intent = new Intent(host, StudyReaderActivity.class);
        intent.putExtra(StudyReaderActivity.EXTRA_JUMP_PAGE, previewPage);
        intent.putExtra(StudyReaderActivity.EXTRA_JUMP_VERSE, exact.get(0).toString());
        intent.putExtra(StudyReaderActivity.EXTRA_MAP_PREVIEW, true);
        host.startActivity(intent);
    }

    @Override public void onReady() { ready = true; renderPreview(); }
    @Override public void onError(String message) { caption.setText("Muṣḥaf unavailable"); }
    @Override public void onPageShown(int page) {}
    @Override public void onVerseTap(VerseRef verse) {
        if (commentaryOpen && verse.getSurah() == 2
            && verse.getAyah() >= chosen().first && verse.getAyah() <= chosen().last) {
            commentaryAyah = verse.getAyah();
            commentaryScrollY = 0;
            showCommentaryVerse();
            return;
        }
        openReader();
    }
    @Override public void onSurfaceTap() { openReader(); }
    @Override public void onPageSwipe(int delta) { turnPage(delta); }
}
