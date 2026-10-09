package com.quransafeguard.hifz.preview;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.quransafeguard.hifz.core.VerseRef;
import java.util.ArrayList;

/**
 * P1 long-form Iṣlāḥī reader. No internet, WebView, engine state or copyrighted
 * full commentary is loaded here. Returns to the same scroll position when a
 * temporary Mushaf page Activity is dismissed (normal Android back stack).
 */
public final class IslahiTafsirActivity extends Activity {
    private static final String EXTRA_SURAH = "islahiReaderSurah";
    private static final String EXTRA_FIRST = "islahiReaderFirst";
    private static final String EXTRA_LAST = "islahiReaderLast";
    private final ArrayList<TextView> adjustable = new ArrayList<>();
    private int surah, first, last;
    private ScrollView scroll;
    private float fontSp = 17f;
    private int restoreY;

    public static Intent forBlock(Context context, int surah, int first, int last) {
        Intent intent = new Intent(context, IslahiTafsirActivity.class);
        intent.putExtra(EXTRA_SURAH, surah);
        intent.putExtra(EXTRA_FIRST, first);
        intent.putExtra(EXTRA_LAST, last);
        return intent;
    }

    @Override protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        surah = getIntent().getIntExtra(EXTRA_SURAH, 0);
        first = getIntent().getIntExtra(EXTRA_FIRST, 0);
        last = getIntent().getIntExtra(EXTRA_LAST, 0);
        if (!IslahiPilotContent.hasEnglishReadingNotes(surah, first, last)
                && !IslahiPilotContent.isMultiPageNavigationPilot(surah, first, last)) {
            finish();
            return;
        }
        if (saved != null) {
            restoreY = saved.getInt("readingScrollY", 0);
            fontSp = saved.getFloat("readingFontSp", 17f);
        }
        LinearLayout root = Ui.column(this);
        root.setBackgroundColor(Ui.PAPER);
        LinearLayout header = Ui.row(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(Ui.iconButton(this, "‹", "Retour au bloc", v -> finish()));
        TextView title = Ui.bookText(this, "Iṣlāḥī  " + surah + ":" + first + "–" + last, 18f, true);
        title.setGravity(Gravity.CENTER);
        header.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        header.addView(Ui.iconButton(this, "−", "Smaller tafsir text", v -> resize(-1f)));
        header.addView(Ui.iconButton(this, "+", "Larger tafsir text", v -> resize(1f)));
        root.addView(header);
        root.addView(Ui.divider(this));

        scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout content = Ui.column(this);
        int pad = Ui.dp(this, 13);
        content.setPadding(pad, Ui.dp(this, 8), pad, Ui.dp(this, 28));
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        setContentView(root);
        Ui.respectSystemBars(this, root, 0, 0, 0, 0);

        final boolean checked = IslahiPilotContent.hasEnglishReadingNotes(surah, first, last);
        paragraph(content, checked
            ? "OFFLINE · SOURCE-CHECKED ENGLISH EDITORIAL READING NOTES"
            : "OFFLINE · PAGE NAVIGATION TEST · COMMENTARY NOT YET VERIFIED", 12f, true);
        paragraph(content, checked ? IslahiPilotContent.NAS_TITLE :
            "Mushaf ↔ Iṣlāḥī passage navigation", 20f, true);

        paragraph(content, "MUSHAF PAGES", 12f, true);
        addPageLinks(content);

        if (checked) {
            content.addView(Ui.divider(this));
            paragraph(content, "COMMENTARY NOTES", 14f, true);
            paragraph(content, "These short English explanations are editorial summaries "
                + "checked against the published English translation. They are not "
                + "the full original tafsir or a verified Urdu translation.", 14f, false);
            for (IslahiPilotContent.Section section : IslahiPilotContent.nasSections()) {
                content.addView(Ui.divider(this));
                paragraph(content, section.reference() + "  ·  " + section.title, 18f, true);
                resizable(content, section.readingNote);
                paragraph(content, section.printedPages + " · Source: Tadabbur-i Qur'an, vol. 9", 12f, false);
                Button open = Ui.smallButton(this, "Open Mushaf · " + section.reference(),
                    v -> openMushaf(firstPage(section.firstAyah), section.firstAyah, section.lastAyah));
                open.setContentDescription("Open the Mushaf on verses " + section.reference());
                content.addView(open);
            }
            content.addView(Ui.divider(this));
            paragraph(content, IslahiPilotContent.NAS_SOURCE, 12f, false);
            paragraph(content, "Full English commentary is not included in this test APK: "
                + "offline reproduction permission has not been established. "
                + "No internet connection is needed for these editorial notes.", 13f, false);
        } else {
            content.addView(Ui.divider(this));
            paragraph(content, "The boundaries of this block are documented, but the "
                + "corresponding English commentary and its internal verse-to-paragraph "
                + "mapping have not yet been certified. No tafsir text has been invented.", 15f, false);
            paragraph(content, "Use the Mushaf page references above to test navigation. "
                + "Back returns to this exact reading position.", 13f, false);
        }
        scroll.post(() -> scroll.scrollTo(0, restoreY));
    }

    private void addPageLinks(LinearLayout content) {
        int begin, end;
        try {
            begin = firstPage(first);
            end = lastPage(last);
        } catch (RuntimeException missing) {
            paragraph(content, "Mushaf page geometry unavailable.", 14f, false);
            return;
        }
        if (begin < 1 || end > 604 || end < begin || end - begin > 25) return;
        HorizontalScrollView strip = new HorizontalScrollView(this);
        strip.setHorizontalScrollBarEnabled(false);
        LinearLayout row = Ui.row(this);
        for (int number = begin; number <= end; number++) {
            final int page = number;
            Button b = Ui.smallButton(this, "Mushaf " + number, v -> openMushaf(page, first, last));
            b.setContentDescription("Open Mushaf page " + number + " with Iṣlāḥī passage highlighted");
            row.addView(b);
        }
        strip.addView(row);
        content.addView(strip);
    }

    private int firstPage(int ayah) {
        GeometryRepository g = GeometryRepository.get(this);
        return g.line(g.firstLineIndex(new VerseRef(surah, ayah))).page;
    }
    private int lastPage(int ayah) {
        GeometryRepository g = GeometryRepository.get(this);
        return g.line(g.lastLineIndex(new VerseRef(surah, ayah))).page;
    }

    private void openMushaf(int page, int verseFrom, int verseTo) {
        // A new temporary StudyReader is launched, leaving this reader (and scrollY)
        // on the activity back stack. No progress / state in HifzPrefs is written.
        Intent intent = new Intent(this, StudyReaderActivity.class);
        intent.putExtra(StudyReaderActivity.EXTRA_JUMP_PAGE, page);
        intent.putExtra(StudyReaderActivity.EXTRA_ISLAHI_SURAH, surah);
        intent.putExtra(StudyReaderActivity.EXTRA_ISLAHI_START, verseFrom);
        intent.putExtra(StudyReaderActivity.EXTRA_ISLAHI_END, verseTo);
        startActivity(intent);
    }

    private void paragraph(LinearLayout host, String message, float size, boolean strong) {
        TextView text = Ui.bookText(this, message, size, strong);
        text.setTextColor(strong ? Ui.INK : Ui.MUTED);
        text.setPadding(0, Ui.dp(this, 9), 0, Ui.dp(this, 10));
        host.addView(text);
    }

    private void resizable(LinearLayout host, String message) {
        TextView text = Ui.bookText(this, message, fontSp, false);
        text.setTextColor(Ui.INK);
        text.setLineSpacing(Ui.dp(this, 3), 1.12f);
        text.setPadding(0, Ui.dp(this, 12), 0, Ui.dp(this, 12));
        adjustable.add(text);
        host.addView(text);
    }

    private void resize(float delta) {
        fontSp = Math.max(15f, Math.min(27f, fontSp + delta));
        for (TextView t : adjustable) t.setTextSize(fontSp);
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        state.putInt("readingScrollY", scroll == null ? restoreY : scroll.getScrollY());
        state.putFloat("readingFontSp", fontSp);
        super.onSaveInstanceState(state);
    }
}
