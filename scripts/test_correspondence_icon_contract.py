import hashlib
import re
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path
from correspondence_icon_vectors import convert_svg

ROOT=Path(__file__).resolve().parents[1]
SOURCE=ROOT/'docs/correspondences/icons_contractual_8'
A='{http://schemas.android.com/apk/res/android}'

class ContractIconsTest(unittest.TestCase):
    def test_all_eight_sources_match_user_manifest(self):
        rows=re.findall(r'(\S+\.svg)\s+SHA256\s+([0-9a-f]{64})',(SOURCE/'MANIFEST_SHA256.txt').read_text())
        self.assertEqual(8,len(rows))
        for name,digest in rows:self.assertEqual(digest,hashlib.sha256((SOURCE/name).read_bytes()).hexdigest(),name)

    def test_square_viewport_centers_the_original_geometry_without_stretch(self):
        for source in SOURCE.glob('*.svg'):
            vector=ET.fromstring(convert_svg(source.read_text()))
            self.assertEqual('24dp',vector.get(A+'width'))
            self.assertEqual('24dp',vector.get(A+'height'))
            self.assertEqual('72',vector.get(A+'viewportWidth'))
            self.assertEqual('72',vector.get(A+'viewportHeight'))
            self.assertEqual('12',vector.find('group').get(A+'translateY'))

    def test_original_path_data_is_preserved_verbatim(self):
        for source in SOURCE.glob('*.svg'):
            original=ET.fromstring(source.read_text())
            vector=ET.fromstring(convert_svg(source.read_text()))
            paths=[e.get(A+'pathData') for e in vector.iter('path')]
            for element in original.iter():
                if element.tag.endswith('}path'):self.assertIn(element.get('d'),paths,source.name)

    def test_centre_is_exact_clipped_square_and_four_horizontal_strokes(self):
        source=SOURCE/'07_centre.svg';vector=ET.fromstring(convert_svg(source.read_text()))
        paths=[p.get(A+'pathData') for p in vector.iter('path')]
        self.assertIn('M5 17H23 M5 31H23 M49 17H67 M49 31H67',paths)
        self.assertEqual(2,paths.count('M26 14H46V34H26Z'))
        clips=[p.get(A+'pathData') for p in vector.iter('clip-path')]
        self.assertEqual(['M26 14H46V34H26Z'],clips)
        self.assertFalse(any(re.search(r'[Aa]',p) for p in paths))

    def test_progression_has_exactly_four_strictly_growing_circles(self):
        source=ET.fromstring((SOURCE/'04_progression.svg').read_text())
        radii=[float(e.get('r')) for e in source.iter() if e.tag.endswith('}circle')]
        self.assertEqual([2.7,4.3,6.4,8.5],radii)
        vector=ET.fromstring(convert_svg((SOURCE/'04_progression.svg').read_text()))
        paths=list(vector.iter('path'));self.assertEqual(4,len(paths))
        for radius,path in zip(radii,paths):self.assertIn('A'+str(radius)+' '+str(radius),path.get(A+'pathData'))

if __name__=='__main__':unittest.main()
