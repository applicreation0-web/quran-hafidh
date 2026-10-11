package com.quransafeguard.hifz.preview;
import org.junit.Test;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;
/** No browsing through 114-surah or numbered passage lists: Mushaf in-place only. */
public final class NoSemanticListsAndOfflineAmorcesContractTest {
  static String read(String p) throws Exception {
    Path f=Paths.get(p);if(!Files.exists(f))f=Paths.get("..",p);
    return new String(Files.readAllBytes(f),StandardCharsets.UTF_8);
  }
  @Test public void readingHasOnlyInPlaceCueAndNoMapShortcut() throws Exception {
    String s=read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/StudyReaderActivity.java");
    assertFalse(s.contains("openIbnKathirMap"));
    assertFalse(s.contains("new Intent(this,IbnKathirMapActivity.class)"));
    assertFalse(s.contains("Ui.iconButton(this, \"\", \"Carte\""));
    assertTrue(s.contains("setTitle(\"Amorce · Ibn Kathīr\")"));
    assertTrue(s.contains("verifiedQafHeadings(group)"));
    assertFalse(s.contains("IbnKathirQuranComSource.urlForGroup(group)"));
  }
}
