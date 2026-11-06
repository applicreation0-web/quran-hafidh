"""Admission contract: frozen exclusions, reproducible anchors, explicit conditional cases."""
import hashlib
import json
import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

class CatalogueTest(unittest.TestCase):
    def test_runtime_uses_admitted_catalogue(self):
        source = (ROOT / 'hifz-app/src/main/java/com/quransafeguard/hifz/preview/CorrespondenceCases.java').read_text()
        self.assertIn('for(Entry candidate:CorrespondenceCatalogue.ADMITTED)', source)
        self.assertIn('for(Entry entry:CorrespondenceCatalogue.ADMITTED)', source)

    def test_exact_refrains_against_existing_arabic_corpus(self):
        geometry = json.loads((ROOT / 'app/src/main/assets/reader109/geometry.json').read_text())
        refs = sorted(geometry['verses'], key=lambda r: tuple(map(int, r.split(':'))))
        lines = (ROOT / 'scripts/data/quran-uthmani-tanzil.txt').read_text().splitlines()
        self.assertEqual(6236, len(lines))
        text = dict(zip(refs, lines))
        dossiers = json.loads((ROOT / 'docs/correspondances/catalogue-62.json').read_text())['dossiers']
        for id in ('R019', 'R030', 'R031', 'R047', 'R048', 'R059'):
            d = next(d for d in dossiers if d['id'] == id)
            passages = [text[r] for r in d['reperes']]
            self.assertEqual(1, len(set(passages)), id)
            if id in ('R047', 'R048', 'R059'):
                surah = d['reperes'][0].split(':')[0]
                self.assertEqual(set(d['reperes']), {r for r, t in text.items() if r.startswith(surah+':') and t == passages[0]}, id)

    def test_structured_comparisons_never_pair_two_examples_of_the_same_side(self):
        dossiers = json.loads((ROOT / 'docs/correspondances/catalogue-62.json').read_text())['dossiers']
        by_id = {d['id']: d for d in dossiers}
        self.assertIn('comparaisons', by_id['R054'], 'each source anchor requires explicit partners')
        self.assertEqual(['66:10'], by_id['R054']['comparaisons']['66:11'])
        self.assertEqual(['66:10'], by_id['R054']['comparaisons']['66:12'])
        self.assertEqual(['93:6'], by_id['R070']['comparaisons']['93:9'])
        self.assertEqual(['56:91'], by_id['R049']['comparaisons']['56:8'])
        for d in dossiers:
            if d['statut'] != 'ADMIS':
                continue
            for ref, partners in d['comparaisons'].items():
                self.assertTrue(partners, (d['id'], ref))
                self.assertNotIn(ref, partners)
                for partner in partners:
                    self.assertIn(ref, d['comparaisons'][partner], (d['id'], ref, partner))

    def test_catalogue_is_individual_and_preserves_exclusions(self):
        path = ROOT / 'docs/correspondances/catalogue-62.json'
        self.assertTrue(path.exists(), '62 individual dossiers must exist')
        data = json.loads(path.read_text())
        old = json.loads((ROOT / 'docs/correspondances/registre-documentaire-110.json').read_text())
        excluded = {d['id'] for d in old['dossiers'] if d['decisionDocumentaire'] == 'EXCLURE'}
        self.assertEqual(48, len(excluded))
        self.assertEqual(excluded, set(data['exclusionsPreservees']))
        dossiers = data['dossiers']
        self.assertEqual(62, len(dossiers))
        self.assertEqual(62, len({d['id'] for d in dossiers}))
        self.assertFalse(excluded & {d['id'] for d in dossiers})
        for d in dossiers:
            for field in ('categorie', 'passages', 'correspondance', 'explication', 'sources', 'limites', 'ancresQcf', 'statut'):
                self.assertTrue(d[field], (d['id'], field))
            self.assertTrue(all(s['url'].startswith('https://') for s in d['sources']))
            if d['statut'] == 'CONDITIONNEL':
                self.assertTrue(d['conditionManquante'])
        admitted = [d for d in dossiers if d['statut'] == 'ADMIS']
        self.assertEqual(60, len(admitted))
        self.assertEqual({'R032', 'R033'}, {d['id'] for d in dossiers if d['statut'] == 'CONDITIONNEL'})
        source = (ROOT / 'hifz-app/src/main/java/com/quransafeguard/hifz/preview/CorrespondenceCatalogue.java').read_text()
        addenda = json.loads((ROOT / 'docs/correspondances/additions-11712.json').read_text())['dossiers']
        self.assertEqual(1, len(addenda))
        self.assertEqual('R111', addenda[0]['id'])
        self.assertEqual('ADMIS', addenda[0]['statut'])
        self.assertEqual(['2:113', '2:118'], addenda[0]['reperes'])
        self.assertEqual({'2:113': ['2:118'], '2:118': ['2:113']}, addenda[0]['comparaisons'])
        self.assertEqual(18, addenda[0]['folioQcfAttendu'])
        self.assertEqual({d['id'] for d in admitted} | {d['id'] for d in addenda},
                         set(re.findall(r'new CorrespondenceCases.Entry\("(R\d+)"', source)))
        r065 = next(d for d in dossiers if d['id'] == 'R065')
        self.assertEqual(['85:10', '85:11'], r065['reperes'])
        self.assertEqual(hashlib.sha256((ROOT / 'docs/correspondances/registre-documentaire-110.json').read_bytes()).hexdigest(), data['registreHistoriqueSha256'])

if __name__ == '__main__':
    unittest.main()
