"""Source wiring gates; runtime gestures are in CorrespondenceNavigationInstrumentedTest."""
import unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/'hifz-app/src/main/java/com/quransafeguard/hifz/preview'
class TafsirNavigationContract(unittest.TestCase):
 def test_no_page_arrows_and_navigation_lives_in_tafsir_headers(self):
  s=(JAVA/'CorrespondenceComparisonActivity.java').read_text()
  self.assertNotIn('"Page précédente ',s)
  self.assertNotIn('"Page suivante ',s)
  self.assertIn('"Verset précédent "+',s)
  self.assertIn('"Verset suivant "+',s)
  self.assertIn('stepVerse(pane,-1)',s)
 def test_selection_drives_reference_and_dynamic_polygon_highlight(self):
  s=(JAVA/'CorrespondenceComparisonActivity.java').read_text()
  self.assertIn('pages[side].setSelectedVerse',s)
  self.assertIn('geometry.versesOnLines(geometry.lineIdsOnPage',s)
  self.assertIn('navigationSurahs[side]',s)
  t=(JAVA/'MapQcfThumbnails.java').read_text()
  self.assertIn('void setSelectedVerse(',t)
  self.assertIn('applyVerseSelection()',t)
  self.assertIn('haafidh-active-verse',t)
 def test_each_tafsir_header_retains_the_existing_reading_route(self):
  s=(JAVA/'CorrespondenceComparisonActivity.java').read_text()
  self.assertIn('label.setOnClickListener(v->openReading(pane))',s)
 def test_sources_dialog_has_editorial_note_without_permanent_panel(self):
  s=(JAVA/'CorrespondenceComparisonActivity.java').read_text()
  self.assertIn('"En bref"',s[s.index('private void showSources'):])
  self.assertIn('"Sources"',s[s.index('private void showSources'):])
  self.assertIn('"Limites"',s[s.index('private void showSources'):])
if __name__=='__main__': unittest.main()
