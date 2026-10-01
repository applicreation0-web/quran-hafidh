package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/**
 * Rub' al-hizb navigation and header badge: wired into both screens that already offer
 * a surah picker (Lecture, Mémorisation libre), alongside it — never replacing it — and
 * a small header badge (the same self-authored 8-point star as this reader's other plain,
 * non-licensed marks) shown only on the ~240 pages that actually start a rub'.
 */
public final class QuranRubNavigationSourceContractTest {
    private static String read(String repoPath) throws Exception {
        Path direct = Paths.get(repoPath);
        if (Files.exists(direct)) return new String(Files.readAllBytes(direct), StandardCharsets.UTF_8);
        Path parent = Paths.get("..", repoPath);
        if (Files.exists(parent)) return new String(Files.readAllBytes(parent), StandardCharsets.UTF_8);
        throw new IllegalStateException("Missing repository file: " + repoPath);
    }

    @Test public void studyReaderShowsAHizbPickerAndBadgeAlongsideTheExistingSurahPicker() throws Exception {
        String study = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/StudyReaderActivity.java");
        assertTrue("the rub' picker must sit beside the surah picker, not replace it",
            study.contains("private TextView surahPicker;") && study.contains("private TextView rubPicker;"));
        assertTrue("the header badge must be backed by the plain, self-authored star drawable",
            study.contains("rubBadge.setBackgroundResource(R.drawable.ic_ui_hizb);"));
        assertTrue("the badge must hide on the ~364 pages that start no rub', never guess",
            study.contains("int[] row = QuranRubBoundaries.boundaryOnPage(page);\n"
                + "        if (row == null) { rubBadge.setVisibility(View.GONE); return; }"));
        assertTrue("the picker's own status label must reflect the real current rub', not a static caption",
            study.contains("rubPicker.setText(QuranRubNames.currentLabel(page) + \" ▾\");"));
        assertTrue("every page-changing path (initial load, jump, swipe) must refresh both the "
                + "picker label and the badge",
            countOccurrences(study, "updateRubPickerLabel();") >= 3
                && countOccurrences(study, "updateRubBadge();") >= 3);
    }

    @Test public void freeMemorizationOffersTheSameHizbPickerNextToItsOwnSurahPicker() throws Exception {
        String freeMem = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/FreeMemActivity.java");
        assertTrue("must sit right beside the existing surah nav button",
            freeMem.contains("nav.addView(Ui.iconButton(this,\"\",\"Sourate\",v->showSurahPicker()));\n"
                + "        nav.addView(Ui.iconButton(this,\"\",\"Hizb\",v->showRubPicker()));"));
        assertTrue("must reuse the same shared picker helper as Lecture, not a second implementation",
            freeMem.contains("private void showRubPicker(){ QuranRubNames.showPicker(this,this::setPage); }"));
    }

    @Test public void hizbIconIsResolvedFromPlainGeometryLikeEveryOtherReaderMark() throws Exception {
        String ui = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/Ui.java");
        assertTrue("must be resolvable through the same icon lookup every other nav button uses",
            ui.contains("if (s.contains(\"hizb\")) return R.drawable.ic_ui_hizb;"));
        String icon = read("hifz-app/src/main/res/drawable/ic_ui_hizb.xml");
        assertTrue("the rub'-al-hizb 8-point star must stay two overlaid squares — the same generic "
                + "construction as the reader's other star, never a path lifted from licensed Mushaf art",
            icon.contains("android:pathData=\"M12,3 L21,12 L12,21 L3,12 Z\"")
                && icon.contains("android:pathData=\"M18.36,5.64 L5.64,5.64 L5.64,18.36 L18.36,18.36 Z\""));
    }

    private static int countOccurrences(String haystack, String needle) {
        int count = 0, from = 0;
        while (true) {
            int at = haystack.indexOf(needle, from);
            if (at < 0) return count;
            count++;
            from = at + needle.length();
        }
    }
}
