package com.quransafeguard.hifz.preview;
import org.junit.Test;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;
/** Lecture stays in-place; the separate Carte must never revive source headings or web links. */
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
    assertTrue(s.contains("SemanticTitlePopup.showEnglish(this,label,semanticTitleDialog)"));
    assertTrue("Reader must load ONLY M7-B approved private titles",
      s.contains("IbnKathirQuranComPrivateTitles.approvedTitle(this,group)"));
    assertFalse("Old spa5k source headings must not be displayed",
      s.contains("IbnKathirTitlePack.sourceHeadings(this,group)"));
    assertFalse("Old hard-coded source checkpoints must not be displayed",
      s.contains("IbnKathirOfflineTitles.verifiedHeadings(group)"));
    assertFalse(s.contains("IbnKathirQuranComSource.urlForGroup(group)"));
    assertFalse(s.contains("Intent.ACTION_VIEW"));
  }
}
