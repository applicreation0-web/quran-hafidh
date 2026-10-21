package com.quransafeguard.hifz.preview;

import org.junit.Test;
import static org.junit.Assert.*;

/** Private BOOX Atlas: only rendering changes, not 1419 canonical editorial titles. */
public final class AtlasDisplayTitleFormatterTest {
 @Test public void convertsEnglishAndFormerAyahThemesNamesWithoutMutatingSource(){
  String original="Musa (Moses), Isa (Jesus), Maryem (Mary) and Ibrahim";
  assertEquals("Mūsā (موسى), ʿĪsā (عيسى), Maryam (مريم) and Ibrāhīm (إبراهيم)",
      AtlasDisplayTitleFormatter.display(original));
  assertEquals("Musa (Moses), Isa (Jesus), Maryem (Mary) and Ibrahim",original);
 }
 @Test public void preservesExistingArabicNamesWithoutDuplicatingParentheses(){
  assertEquals("Ādam (آدم) and Ibrāhīm (إبراهيم)",
    AtlasDisplayTitleFormatter.display("Ādam (آدم) and Ibrāhīm (إبراهيم)"));
  assertEquals("Who was ʿĪsā (عيسى) son of Maryam (مريم)?",
    AtlasDisplayTitleFormatter.display("Who was Jesus son of Mary?"));
 }
 @Test public void retainsUnchangedNonPersonalTitlesAndNoVerseNumbersInserted(){
  assertEquals("The Israelites and the Covenant",
      AtlasDisplayTitleFormatter.display("The Israelites and the Covenant"));
  assertEquals("The Creator and His Revelation",
      AtlasDisplayTitleFormatter.display("The Creator and His Revelation"));
 }
 @Test public void handlesPersonalNamesInLongAyahThemeHeadings(){
  assertEquals("Prophet Yūsuf (يوسف) and Yaḥyā (يحيى), Zakariyyā (زكريا)",
    AtlasDisplayTitleFormatter.display("Prophet Yusuf and Yahya (John), Zakariya"));
  assertEquals("Firʿawn (فرعون) and Mūsā (موسى)",
    AtlasDisplayTitleFormatter.display("Pharaoh and Moses"));
 }
}
