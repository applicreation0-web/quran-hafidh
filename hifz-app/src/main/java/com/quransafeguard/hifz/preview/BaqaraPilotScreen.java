package com.quransafeguard.hifz.preview;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

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
    private boolean commentaryOpen;
    private boolean ready;
    private boolean mushafExpanded;

    private ScrollView mapScroll, commentaryScroll;
    private FrameLayout mapHost;
    private LinearLayout mapColumn, root;
    private TextView pageIndicator, caption, mapHeading, commentaryHeading, commentaryBody;
    private Button previousPage, nextPage;
    private MushafView mushaf;
    private LinearLayout.LayoutParams mapSizing, previewSizing;

    BaqaraPilotScreen(IbnKathirMapActivity activity, GeometryRepository geometry, Bundle saved,
                      int requestedAyah) {
        this.host = activity;
        this.activity = activity;
        this.geometry = geometry;
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
            mushafExpanded = saved.getBoolean("bp_expanded", false);
        }
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
        mapHeading.setMaxLines(2);
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
        mapColumn.setPadding(Ui.dp(host, 5), Ui.dp(host, 2), Ui.dp(host, 5), Ui.dp(host, 9));
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
        commentaryColumn.addView(returnToMap, new LinearLayout.LayoutParams(
            Ui.dp(host, 44), Ui.dp(host, 35)));
        commentaryHeading = Ui.bookText(host, "", 15f, true);
        commentaryColumn.addView(commentaryHeading);
        commentaryBody = Ui.bookText(host, "", 13.5f, false);
        commentaryBody.setPadding(0, Ui.dp(host, 7), 0, Ui.dp(host, 10));
        commentaryBody.setTextIsSelectable(true);
        commentaryColumn.addView(commentaryBody);
        TextView sourceLink = Ui.bookText(host, "↗  Open English commentary source", 12f, true);
        sourceLink.setPadding(0, Ui.dp(host, 6), 0, Ui.dp(host, 6));
        sourceLink.setOnClickListener(v -> openSourceInBrowser());
        commentaryColumn.addView(sourceLink);
        commentaryScroll.addView(commentaryColumn, new ScrollView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        mapHost = new FrameLayout(host);
        mapHost.addView(mapScroll, new FrameLayout.LayoutParams(-1, -1));
        mapHost.addView(commentaryScroll, new FrameLayout.LayoutParams(-1, -1));
        mapSizing = new LinearLayout.LayoutParams(-1, 0, .34f);
        root.addView(mapHost, mapSizing);
        root.addView(Ui.divider(host));

        LinearLayout preview = Ui.column(host);
        caption = Ui.bookText(host, "", 13.5f, true);
        caption.setMaxLines(2);
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

        LinearLayout controls = Ui.row(host);
        controls.setGravity(Gravity.CENTER_VERTICAL);
        previousPage = Ui.iconButton(activity, "", "Page précédente", v -> turnPage(-1));
        nextPage = Ui.iconButton(activity, "", "Page suivante", v -> turnPage(1));
        pageIndicator = Ui.text(host, "", 12f, true);
        pageIndicator.setGravity(Gravity.CENTER);
        controls.addView(previousPage);
        controls.addView(pageIndicator, new LinearLayout.LayoutParams(0, -2, 1f));
        controls.addView(nextPage);
        preview.addView(controls);

        mushaf = new MushafView(host);
        host.attachPilotPreview(mushaf);
        mushaf.setContentDescription("Muṣḥaf QCF, toucher pour ouvrir Lecture plein écran");
        mushaf.setListener(this);
        preview.addView(mushaf, new LinearLayout.LayoutParams(-1, 0, 1f));
        previewSizing = new LinearLayout.LayoutParams(-1, 0, .66f);
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
        mapSizing.weight = mushafExpanded ? .15f : .34f;
        previewSizing.weight = mushafExpanded ? .85f : .66f;
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
                Ui.dp(host, 27), Ui.dp(host, 51)));
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
                Ui.dp(host, 27), Ui.dp(host, 49)));
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
        drawMap(false);
        renderPreview();
    }

    private int pageStart() {
        return geometry.pageForVerse(new VerseRef(2, chosen().first));
    }
    private int pageEnd() {
        return geometry.pageForVerse(new VerseRef(2, chosen().last));
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
        caption.setText(chosen().title + "   ·   " + chosen().range());
        pageIndicator.setText("Page " + previewPage +
            (first == last ? "" : "  ·  " + first + "–" + last));
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
        int newTheme = BaqaraPilotSpec.themeIndexForAyah(chosen().first);
        if (newTheme != theme) { theme = newTheme; scrollY = 0; }
        drawMap(false);
        renderPreview();
    }

    private void openCommentary() {
        if (commentaryOpen) return;
        scrollY = mapScroll.getScrollY();
        commentaryScrollY = 0;
        commentaryOpen = true;
        displayCommentary();
    }

    private void displayCommentary() {
        BaqaraAyahThemeTitles.Passage passage = chosen();
        commentaryHeading.setText(passage.title + "\n" + passage.range());
        int firstSection = BaqaraPilotSpec.sectionIndexForAyah(passage.first);
        int lastSection = BaqaraPilotSpec.sectionIndexForAyah(passage.last);
        StringBuilder text = new StringBuilder();
        for (int i = firstSection; i <= lastSection && i >= 0; i++) {
            int[] range = BaqaraPilotSpec.sectionRange(i);
            text.append("Mawdudi · Section ").append(i + 1)
                .append(" · 2:").append(range[0]).append("–").append(range[1]).append("\n\n");
            String privateText = privateCommentary(i + 1);
            text.append(privateText == null
                ? "Complete English commentary not installed in this private build. " +
                  "Open the source to consult this section. No invented commentary is shown."
                : privateText);
            text.append("\n\n");
        }
        commentaryBody.setText(text.toString().trim());
        mapScroll.setVisibility(View.GONE);
        commentaryScroll.setVisibility(View.VISIBLE);
        commentaryScroll.post(() -> commentaryScroll.scrollTo(0, Math.max(0, commentaryScrollY)));
    }

    private String privateCommentary(int section) {
        String path = String.format(Locale.ROOT,
            "mawdudi_baqara_private/commentary_section_%02d.txt", section);
        try (InputStream in = host.getAssets().open(path);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read, size = 0;
            while ((read = in.read(buffer)) != -1) {
                size += read;
                if (size > 160000) return null;
                out.write(buffer, 0, read);
            }
            String s = out.toString(StandardCharsets.UTF_8.name()).trim();
            return s.length() < 100 ? null : s;
        } catch (Exception unavailable) { return null; }
    }

    private void openSourceInBrowser() {
        int i = Math.max(0, BaqaraPilotSpec.sectionIndexForAyah(chosen().first));
        int[] range = BaqaraPilotSpec.sectionRange(i);
        Uri url = Uri.parse("https://www.islamicstudies.info/quran/towards.php?sura=2"
            + "&verse=" + range[0] + "&to=" + range[1]);
        try { host.startActivity(new Intent(Intent.ACTION_VIEW, url)); }
        catch (Exception ignored) {
            Toast.makeText(host, "Unable to open the external source", Toast.LENGTH_SHORT).show();
        }
    }

    private void closeCommentary() {
        if (!commentaryOpen) return;
        commentaryScrollY = commentaryScroll.getScrollY();
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
    }

    /** Expose the same real QCF view for existing runtime integrity tests. */
    MushafView mushafForIntegrityCheck() { return mushaf; }

    void destroy() {
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
    @Override public void onVerseTap(VerseRef verse) { openReader(); }
    @Override public void onSurfaceTap() { openReader(); }
    @Override public void onPageSwipe(int delta) { turnPage(delta); }
}
