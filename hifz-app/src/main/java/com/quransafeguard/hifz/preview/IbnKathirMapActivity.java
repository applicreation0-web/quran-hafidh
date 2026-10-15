package com.quransafeguard.hifz.preview;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
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
import java.util.List;

/**
 * Graphical documentary map for the 1,903 verified Ibn Kathir groups.
 * It is read-only: no Hifz cursor, credit, review corpus or memorisation state is touched.
 */
public final class IbnKathirMapActivity extends Activity implements MushafView.Listener {
    static final String EXTRA_SURAH = "mapSurah";
    static final String EXTRA_AYAH = "mapAyah";

    private IbnKathirGroupIndex index;
    private GeometryRepository geometry;
    private int surah = 1;
    private int selectedGroupIndex;
    private int previewPage;
    private int scrollY;
    // -1: Ibn Ashur thematic overview, -2: full documentary sequence; >=0 theme.
    private int themeChoice = -1;
    private int overviewScrollY;
    private TextView themesTab;
    private TextView allPassagesTab;
    private boolean previewReady;
    private boolean commentaryOpen;
    private int commentaryScrollY;
    private FrameLayout mapHost;
    private ScrollView commentaryScroll;
    private TextView commentaryTitle;
    private TextView commentaryBody;

    private TextView header;
    private TextView mapSummary;
    private TextView previewCaption;
    private TextView pageIndicator;
    private Button previousPage;
    private Button nextPage;
    private LinearLayout themeTabs;
    private ScrollView mapScroll;
    private LinearLayout mapColumn;
    private MushafView pagePreview;
    private final ArrayList<View> graphicalNodes = new ArrayList<>();

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        try {
            index = IbnKathirGroupIndex.shared();
            geometry = GeometryRepository.get(this);
        } catch (RuntimeException unavailable) {
            Ui.showFatal(this, "La carte Ibn Kathīr ou le Muṣḥaf vérifié est indisponible.");
            return;
        }

        if (state != null) {
            surah = state.getInt("surah", 1);
            selectedGroupIndex = state.getInt("selectedGroupIndex", 0);
            previewPage = state.getInt("previewPage", 0);
            scrollY = state.getInt("scrollY", 0);
            themeChoice = state.getInt("themeChoice", -1);
            overviewScrollY = state.getInt("overviewScrollY", 0);
            commentaryOpen = state.getBoolean("commentaryOpen", false);
            commentaryScrollY = state.getInt("commentaryScrollY", 0);
        } else {
            surah = getIntent().getIntExtra(EXTRA_SURAH, 1);
            int ayah = getIntent().getIntExtra(EXTRA_AYAH, 1);
            surah = Math.max(1, Math.min(114, surah));
            selectedGroupIndex = IbnKathirMapModel.groupIndexForAyah(surah, ayah, index);
        }
        surah = Math.max(1, Math.min(114, surah));
        buildScreen();
        showSurah(false);
    }

    private void buildScreen() {
        LinearLayout root = Ui.column(this);
        root.setBackgroundColor(Ui.PAPER);
        root.setPadding(Ui.dp(this, 8), Ui.dp(this, 2), Ui.dp(this, 8), Ui.dp(this, 6));

        LinearLayout top = Ui.row(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.addView(Ui.iconButton(this, "", "Retour", v -> finish()));
        header = Ui.bookText(this, "", 18f, true);
        header.setGravity(Gravity.CENTER);
        header.setSingleLine(true);
        top.addView(header, new LinearLayout.LayoutParams(0, Ui.dp(this, 48), 1f));
        top.addView(Ui.iconButton(this, "", "Choisir une sourate", v ->
            QuranSurahNames.showSurahPicker(this, this::chooseSurah)));
        root.addView(top);
        root.addView(Ui.divider(this));

        mapSummary = Ui.text(this, "", 12.5f, false);
        mapSummary.setTextColor(Ui.MUTED);
        mapSummary.setGravity(Gravity.CENTER);
        mapSummary.setPadding(Ui.dp(this, 4), Ui.dp(this, 3), Ui.dp(this, 4), Ui.dp(this, 4));
        root.addView(mapSummary);

        LinearLayout tabs = Ui.row(this);
        tabs.setPadding(0, Ui.dp(this, 2), 0, Ui.dp(this, 3));
        themesTab = Ui.bookText(this, "Themes", 14f, true);
        allPassagesTab = Ui.bookText(this, "All Passages", 14f, true);
        for (TextView tab : new TextView[]{themesTab, allPassagesTab}) {
            tab.setGravity(Gravity.CENTER);
            tab.setClickable(true);
            tab.setFocusable(true);
            tabs.addView(tab, new LinearLayout.LayoutParams(0, Ui.dp(this, 37), 1f));
        }
        themesTab.setOnClickListener(v -> openThemeOverview());
        allPassagesTab.setOnClickListener(v -> openAllPassages());
        root.addView(tabs);
        themeTabs = tabs;

        mapScroll = new ScrollView(this);
        mapScroll.setFillViewport(false);
        mapScroll.setVerticalScrollBarEnabled(true);
        mapColumn = Ui.column(this);
        mapColumn.setPadding(Ui.dp(this, 3), Ui.dp(this, 3), Ui.dp(this, 3), Ui.dp(this, 7));
        mapScroll.addView(mapColumn, new ScrollView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        // The same map ScrollView and the same QCF MushafView survive every switch.
        mapHost = new FrameLayout(this);
        mapHost.addView(mapScroll, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        commentaryScroll = new ScrollView(this);
        commentaryScroll.setVerticalScrollBarEnabled(true);
        commentaryScroll.setVisibility(View.GONE);
        LinearLayout commentaryColumn = Ui.column(this);
        commentaryColumn.setPadding(Ui.dp(this, 6), Ui.dp(this, 7),
            Ui.dp(this, 6), Ui.dp(this, 10));
        TextView backToMap = Ui.bookText(this, "← Back to thematic map", 13f, true);
        backToMap.setClickable(true);
        backToMap.setFocusable(true);
        backToMap.setOnClickListener(v -> closeCommentary());
        commentaryColumn.addView(backToMap);
        commentaryTitle = Ui.bookText(this, "", 16f, true);
        commentaryTitle.setPadding(0, Ui.dp(this, 12), 0, Ui.dp(this, 5));
        commentaryColumn.addView(commentaryTitle);
        commentaryBody = Ui.bookText(this, "", 14f, false);
        commentaryBody.setTextIsSelectable(true);
        commentaryColumn.addView(commentaryBody);
        commentaryScroll.addView(commentaryColumn, new ScrollView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        mapHost.addView(commentaryScroll, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.addView(mapHost, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, .46f));
        root.addView(Ui.divider(this));

        LinearLayout preview = Ui.column(this);
        preview.setPadding(0, Ui.dp(this, 3), 0, 0);
        previewCaption = Ui.bookText(this, "", 14f, true);
        previewCaption.setGravity(Gravity.CENTER);
        previewCaption.setMaxLines(3);
        previewCaption.setPadding(Ui.dp(this, 5), Ui.dp(this, 2), Ui.dp(this, 5), Ui.dp(this, 2));
        preview.addView(previewCaption);
        // Only this cue header opens the commentary. QCF taps still open Lecture.
        previewCaption.setClickable(true);
        previewCaption.setFocusable(true);
        previewCaption.setContentDescription("Open Ibn Kathir commentary for selected passage");
        previewCaption.setOnClickListener(v -> openCommentary());

        LinearLayout pageControls = Ui.row(this);
        pageControls.setGravity(Gravity.CENTER);
        previousPage = Ui.iconButton(this, "", "Page précédente", v -> turnPreviewPage(-1));
        nextPage = Ui.iconButton(this, "", "Page suivante", v -> turnPreviewPage(1));
        pageIndicator = Ui.text(this, "", 12.5f, true);
        pageIndicator.setGravity(Gravity.CENTER);
        pageControls.addView(previousPage);
        pageControls.addView(pageIndicator, new LinearLayout.LayoutParams(0,
            ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        pageControls.addView(nextPage);
        preview.addView(pageControls);

        pagePreview = new MushafView(this);
        pagePreview.setContentDescription(
            "Miniature authentique du Muṣḥaf · toucher pour ouvrir Lecture");
        pagePreview.setListener(this);
        preview.addView(pagePreview, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        root.addView(preview, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, .54f));

        setContentView(root);
        Ui.respectSystemBars(this, root, 0, 0, 0, 0);
    }

    private void chooseSurah(int chosen) {
        surah = Math.max(1, Math.min(114, chosen));
        selectedGroupIndex = 0;
        previewPage = 0;
        scrollY = 0;
        overviewScrollY = 0;
        themeChoice = surah == 2 ? -1 : -2;
        commentaryOpen = false;
        commentaryScrollY = 0;
        mapScroll.setVisibility(View.VISIBLE);
        commentaryScroll.setVisibility(View.GONE);
        showSurah(true);
    }

    private void showSurah(boolean resetScroll) {
        List<IbnKathirGroupIndex.Group> groups = index.groupsForSurah(surah);
        selectedGroupIndex = Math.max(0, Math.min(groups.size() - 1, selectedGroupIndex));
        IbnKathirGroupIndex.Group selected = groups.get(selectedGroupIndex);
        previewPage = IbnKathirMapModel.clampPage(selected, previewPage, geometry);
        themeTabs.setVisibility(surah == 2 ? View.VISIBLE : View.GONE);
        if (surah != 2) themeChoice = -2;
        header.setText("Map · " + QuranSurahNames.labelFor(surah));
        if (surah == 2 && themeChoice == -1) {
            mapSummary.setText("13 sourced themes · Ibn Ashur · overlapping verse references");
        } else if (surah == 2 && themeChoice >= 0) {
            IbnAshurBaqaraThemes.Theme topic = IbnAshurBaqaraThemes.all().get(themeChoice);
            mapSummary.setText(topic.references() + " · Ibn Ashur theme · Ibn Kathir passages");
        } else {
            mapSummary.setText(groups.size() + " Ibn Kathir passages · tap a block then the Mushaf");
        }
        themesTab.setBackground(nodeBackground(surah == 2 && themeChoice != -2));
        allPassagesTab.setBackground(nodeBackground(surah == 2 && themeChoice == -2));
        renderMap(groups);
        renderPreview();
        if (commentaryOpen) showCommentaryPanel();
        int target = resetScroll ? 0 : Math.max(0, scrollY);
        mapScroll.post(() -> mapScroll.scrollTo(0, target));
    }

    private void renderMap(List<IbnKathirGroupIndex.Group> groups) {
        if (surah == 2 && themeChoice == -1) buildThemeOverview(groups);
        else buildGraphicalMap(groups);
    }

    /** Source-backed theme nodes. Intersections are links, not new verse borders. */
    private void buildThemeOverview(List<IbnKathirGroupIndex.Group> groups) {
        mapColumn.removeAllViews();
        graphicalNodes.clear();
        // One hub and thirteen siblings, never a chained numbered passage list.
        TextView hub=Ui.bookText(this, "AL-BAQARAH\\n13 THEMES · IBN ʿĀSHŪR", 15f, true);
        hub.setGravity(Gravity.CENTER);
        hub.setPadding(Ui.dp(this, 9),Ui.dp(this, 11),Ui.dp(this, 9),Ui.dp(this, 11));
        hub.setBackground(nodeBackground(false));
        LinearLayout.LayoutParams hubParams=new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hubParams.leftMargin=Ui.dp(this, 45);
        hubParams.rightMargin=Ui.dp(this, 45);
        mapColumn.addView(hub,hubParams);

        // Trunk is connected to the central hub. Each horizontal spoke links
        // a theme independently; drawing no fabricated hierarchy among themes.
        FrameLayout lead=new FrameLayout(this);
        View leadLine=new View(this);
        leadLine.setBackgroundColor(0xff8d8b84);
        lead.addView(leadLine,new FrameLayout.LayoutParams(
            Ui.dp(this, 2), ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER));
        mapColumn.addView(lead,new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,Ui.dp(this, 15)));

        List<IbnAshurBaqaraThemes.Theme> topics=IbnAshurBaqaraThemes.all();
        for(int i=0;i<topics.size();i+=2) {
            LinearLayout row=Ui.row(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.addView(buildThemeNode(i,topics.get(i),groups),
                new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.MATCH_PARENT,1f));
            FrameLayout spine=new FrameLayout(this);
            View vertical=new View(this);
            vertical.setBackgroundColor(0xff8d8b84);
            spine.addView(vertical,new FrameLayout.LayoutParams(
                Ui.dp(this,2),ViewGroup.LayoutParams.MATCH_PARENT,Gravity.CENTER));
            View horizontal=new View(this);
            horizontal.setBackgroundColor(0xff8d8b84);
            spine.addView(horizontal,new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,Ui.dp(this,2),Gravity.CENTER));
            row.addView(spine,new LinearLayout.LayoutParams(
                Ui.dp(this,18),ViewGroup.LayoutParams.MATCH_PARENT));
            if(i+1<topics.size()) {
                row.addView(buildThemeNode(i+1,topics.get(i+1),groups),
                    new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.MATCH_PARENT,1f));
            } else {
                row.addView(new View(this),new LinearLayout.LayoutParams(
                    0,ViewGroup.LayoutParams.MATCH_PARENT,1f));
            }
            LinearLayout.LayoutParams rowParams=new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,Ui.dp(this,145));
            mapColumn.addView(row,rowParams);
        }
        TextView source=Ui.text(this,
            "Thematic index adapted from Ibn Ashur via Quran.com.\\n"
            + "Themes may overlap; All Passages preserves all 173 canonical blocks.",
            11.5f,false);
        source.setGravity(Gravity.CENTER);
        source.setTextColor(Ui.MUTED);
        source.setPadding(Ui.dp(this,5),Ui.dp(this,10),
            Ui.dp(this,5),Ui.dp(this,10));
        mapColumn.addView(source);
    }

    private View buildThemeNode(int position,IbnAshurBaqaraThemes.Theme topic,
            List<IbnKathirGroupIndex.Group> groups) {
        LinearLayout node=Ui.column(this);
        node.setGravity(Gravity.CENTER_VERTICAL);
        node.setPadding(Ui.dp(this,7),Ui.dp(this,7),Ui.dp(this,7),Ui.dp(this,7));
        node.setBackground(nodeBackground(false));
        node.setClickable(true);
        node.setFocusable(true);
        node.setOnClickListener(v -> openTheme(position));

        TextView number=Ui.text(this,String.format(java.util.Locale.ROOT,
            "%02d",position+1),11f,true);
        number.setTextColor(Ui.MUTED);
        TextView title=Ui.bookText(this,topic.titleEn,12.5f,true);
        title.setMaxLines(5);
        TextView range=Ui.text(this,topic.references(),11f,false);
        range.setTextColor(Ui.MUTED);
        TextView count=Ui.text(this,
            IbnAshurBaqaraThemes.matchingIndices(topic,groups).size()
                + " Ibn Kathir passages",10.5f,false);
        count.setTextColor(Ui.MUTED);
        node.addView(number);
        node.addView(title);
        node.addView(range);
        node.addView(count);
        node.setContentDescription(topic.titleEn + " · " + topic.references()
            + " · open Ibn Kathir passages");
        graphicalNodes.add(node);
        return node;
    }

    private void openCommentary() {
        if (commentaryOpen || !previewReady) return;
        scrollY = mapScroll.getScrollY();
        commentaryScrollY = 0;
        commentaryOpen = true;
        showCommentaryPanel();
    }

    private void showCommentaryPanel() {
        IbnKathirGroupIndex.Group group = currentGroup();
        String title = IbnKathirQuranComPrivateTitles.approvedTitle(this, group);
        commentaryTitle.setText((title == null ? "Untitled passage" : title) +
            "\\n" + group.navigationRange());
        IbnKathirFullCommentaryPrivate.Entry entry =
            IbnKathirFullCommentaryPrivate.load(this, group);
        if (entry == null) {
            commentaryBody.setText(
                "Full Ibn Kathir commentary is not yet verified for this passage.\\n\\n" +
                "No generated text or commentary from a different group will be substituted.");
        } else {
            commentaryBody.setText("Ibn Kathir — Darussalam, volume " +
                entry.volume + ", page " + entry.page + "\\n\\n" + entry.text);
        }
        mapScroll.setVisibility(View.GONE);
        commentaryScroll.setVisibility(View.VISIBLE);
        int target = Math.max(0, commentaryScrollY);
        commentaryScroll.post(() -> commentaryScroll.scrollTo(0, target));
    }

    private void closeCommentary() {
        if (!commentaryOpen) return;
        commentaryScrollY = commentaryScroll.getScrollY();
        commentaryOpen = false;
        commentaryScroll.setVisibility(View.GONE);
        mapScroll.setVisibility(View.VISIBLE);
        int original = scrollY;
        mapScroll.post(() -> mapScroll.scrollTo(0, original));
    }

    private void openTheme(int topicIndex) {
        if (surah != 2) return;
        overviewScrollY = mapScroll.getScrollY();
        List<Integer> related = IbnAshurBaqaraThemes.matchingIndices(
            IbnAshurBaqaraThemes.all().get(topicIndex), index.groupsForSurah(2));
        if (related.isEmpty()) return;
        themeChoice = topicIndex;
        selectedGroupIndex = related.get(0);
        previewPage = IbnKathirMapModel.pageSpan(currentGroup(), geometry).first;
        scrollY = 0;
        showSurah(true);
    }

    private void openThemeOverview() {
        if (commentaryOpen) closeCommentary();
        if (surah != 2 || themeChoice == -1) return;
        themeChoice = -1;
        scrollY = overviewScrollY;
        showSurah(false);
    }

    private void openAllPassages() {
        if (commentaryOpen) closeCommentary();
        if (surah != 2 || themeChoice == -2) return;
        if (themeChoice == -1) overviewScrollY = mapScroll.getScrollY();
        themeChoice = -2;
        scrollY = 0;
        showSurah(true);
    }

    /** A vertical chain of alternating nodes: deliberately not the retired numbered list. */
    private void buildGraphicalMap(List<IbnKathirGroupIndex.Group> groups) {
        mapColumn.removeAllViews();
        graphicalNodes.clear();
        int visible = 0;
        for (int i = 0; i < groups.size(); i++) {
            if (surah == 2 && themeChoice >= 0
                    && !IbnAshurBaqaraThemes.all().get(themeChoice).intersects(groups.get(i)))
                continue;
            if (visible > 0) {
                FrameLayout connectorHost = new FrameLayout(this);
                View connector = new View(this);
                connector.setBackgroundColor(0xff8d8b84);
                connectorHost.addView(connector, new FrameLayout.LayoutParams(
                    Math.max(1, Ui.dp(this, 2)), Ui.dp(this, 11), Gravity.CENTER));
                mapColumn.addView(connectorHost, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 11)));
            }
            final int position = i;
            IbnKathirGroupIndex.Group group = groups.get(i);
            LinearLayout node = Ui.row(this);
            node.setGravity(Gravity.CENTER_VERTICAL);
            node.setMinimumHeight(Ui.dp(this, 62));
            node.setPadding(Ui.dp(this, 8), Ui.dp(this, 6), Ui.dp(this, 8), Ui.dp(this, 6));
            node.setClickable(true);
            node.setFocusable(true);
            node.setOnClickListener(v -> selectGroup(position));

            TextView marker = Ui.text(this, Integer.toString(i + 1), 12f, true);
            marker.setGravity(Gravity.CENTER);
            marker.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD);
            marker.setTextColor(i == selectedGroupIndex ? Ui.PAPER : Ui.INK);
            marker.setBackground(nodeMarker(i == selectedGroupIndex));
            node.addView(marker, new LinearLayout.LayoutParams(Ui.dp(this, 38), Ui.dp(this, 38)));

            LinearLayout copy = Ui.column(this);
            copy.setPadding(Ui.dp(this, 9), 0, Ui.dp(this, 4), 0);
            String approved = IbnKathirQuranComPrivateTitles.approvedTitle(this, group);
            String stableTitle = approved == null ? "Sans titre" : approved;
            TextView title = Ui.bookText(this, stableTitle, 14.5f, true);
            title.setMaxLines(2);
            if (approved == null) title.setTextColor(Ui.MUTED);
            TextView range = Ui.text(this, QuranSurahNames.range(
                new VerseRef(group.surah, group.startAyah),
                new VerseRef(group.surah, group.endAyah)), 12.5f, false);
            range.setTextColor(Ui.MUTED);
            copy.addView(title);
            copy.addView(range);
            node.addView(copy, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            node.setContentDescription("Bloc " + (i + 1) + " sur " + groups.size()
                + " · " + stableTitle + " · " + group.navigationRange());
            node.setBackground(nodeBackground(i == selectedGroupIndex));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            int offset = Ui.dp(this, 20);
            if ((visible & 1) == 0) params.rightMargin = offset;
            else params.leftMargin = offset;
            mapColumn.addView(node, params);
            graphicalNodes.add(node);
            visible++;
        }
    }

    private GradientDrawable nodeBackground(boolean selected) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(selected ? 0xffdedbd2 : Ui.PAPER);
        shape.setCornerRadius(Ui.dp(this, 7));
        shape.setStroke(Math.max(1, Ui.dp(this, selected ? 2 : 1)),
            selected ? Ui.INK : 0xff8d8b84);
        return shape;
    }

    private GradientDrawable nodeMarker(boolean selected) {
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.OVAL);
        shape.setColor(selected ? Ui.INK : 0xffe8e5dc);
        shape.setStroke(Math.max(1, Ui.dp(this, 1)), Ui.INK);
        return shape;
    }

    private void selectGroup(int position) {
        scrollY = mapScroll.getScrollY();
        selectedGroupIndex = position;
        IbnKathirGroupIndex.Group group = currentGroup();
        previewPage = IbnKathirMapModel.pageSpan(group, geometry).first;
        renderMap(index.groupsForSurah(surah));
        mapScroll.post(() -> mapScroll.scrollTo(0, scrollY));
        renderPreview();
    }

    private IbnKathirGroupIndex.Group currentGroup() {
        return IbnKathirMapModel.groupAt(surah, selectedGroupIndex, index);
    }

    private void renderPreview() {
        if (!previewReady || pagePreview == null) return;
        IbnKathirGroupIndex.Group group = currentGroup();
        previewPage = IbnKathirMapModel.clampPage(group, previewPage, geometry);
        IbnKathirMapModel.PageSpan span = IbnKathirMapModel.pageSpan(group, geometry);
        List<VerseRef> exact = IbnKathirMapModel.highlightedVerses(group, previewPage, geometry);
        String approved = IbnKathirQuranComPrivateTitles.approvedTitle(this, group);
        previewCaption.setText((approved == null ? "Sans titre" : approved) + "\n"
            + QuranSurahNames.range(new VerseRef(group.surah, group.startAyah),
                new VerseRef(group.surah, group.endAyah)));
        pageIndicator.setText(span.first == span.last
            ? "Page " + previewPage
            : "Page " + previewPage + " · " + span.first + "–" + span.last);
        setPageControl(previousPage, previewPage > span.first);
        setPageControl(nextPage, previewPage < span.last);
        pagePreview.setHighlightVerses(exact);
        pagePreview.show(previewPage, exact, Collections.emptyList(), 0);
    }

    private static void setPageControl(Button button, boolean enabled) {
        button.setEnabled(enabled);
        button.setAlpha(enabled ? 1f : .3f);
    }

    private void turnPreviewPage(int delta) {
        IbnKathirGroupIndex.Group group = currentGroup();
        int next = IbnKathirMapModel.clampPage(group, previewPage + delta, geometry);
        if (next == previewPage) return;
        previewPage = next;
        renderPreview();
    }

    private void openPreviewInReader() {
        IbnKathirGroupIndex.Group group = currentGroup();
        List<VerseRef> exact = IbnKathirMapModel.highlightedVerses(group, previewPage, geometry);
        if (exact.isEmpty()) {
            Toast.makeText(this, "Le passage est absent de cette page du Muṣḥaf.", Toast.LENGTH_LONG).show();
            return;
        }
        Intent intent = new Intent(this, StudyReaderActivity.class);
        intent.putExtra(StudyReaderActivity.EXTRA_JUMP_PAGE, previewPage);
        intent.putExtra(StudyReaderActivity.EXTRA_JUMP_VERSE, exact.get(0).toString());
        intent.putExtra(StudyReaderActivity.EXTRA_MAP_PREVIEW, true);
        startActivity(intent);
    }

    @Override public void onReady() {
        previewReady = true;
        renderPreview();
    }

    @Override public void onError(String message) {
        previewCaption.setText("Miniature indisponible · " + currentGroup().navigationRange());
    }

    @Override public void onPageShown(int page) {}
    @Override public void onVerseTap(VerseRef verse) { openPreviewInReader(); }
    @Override public void onSurfaceTap() { openPreviewInReader(); }
    @Override public void onPageSwipe(int delta) { turnPreviewPage(delta); }

    @Override protected void onSaveInstanceState(Bundle out) {
        scrollY = mapScroll == null ? scrollY : mapScroll.getScrollY();
        IbnKathirMapModel.SavedState saved = new IbnKathirMapModel.SavedState(
            surah, selectedGroupIndex, previewPage, scrollY);
        out.putInt("surah", saved.surah);
        out.putInt("selectedGroupIndex", saved.groupIndex);
        out.putInt("previewPage", saved.previewPage);
        out.putInt("scrollY", saved.scrollY);
        out.putInt("themeChoice", themeChoice);
        out.putInt("overviewScrollY", overviewScrollY);
        out.putBoolean("commentaryOpen", commentaryOpen);
        out.putInt("commentaryScrollY",
            commentaryOpen && commentaryScroll != null
                ? commentaryScroll.getScrollY() : commentaryScrollY);
        super.onSaveInstanceState(out);
    }

    @Override public void onBackPressed() {
        if (commentaryOpen) {
            closeCommentary();
            return;
        }
        if (surah == 2 && themeChoice >= 0) {
            openThemeOverview();
            return;
        }
        super.onBackPressed();
    }

    @Override protected void onDestroy() {
        if (pagePreview != null) pagePreview.destroySafely();
        super.onDestroy();
    }
}
