# Reprise exécutable — Mawdudi 114

1. Lire STATUS_CURRENT.md, CP00_REPORT.md, CP00_INITIAL_INVENTORY.json, PROGRESS_MATRIX.csv et PRIVATE_ASSETS_MANIFEST.md.
2. Vérifier branche unique, HEAD distant, git status et changements locaux. Le dernier checkpoint initial vérifié est `9354af4397f248a64add0484a0ed898cef54187b` ; récupérer ensuite le commit du présent rapport. Ne rien écraser.
3. Lever le blocage de stockage privé ; aucune suppression de fichiers utilisateur n’est autorisée par cette procédure.
4. Retrouver l’EPUB et le prompt joints via les références PRIVÉES conservées dans le reçu de remise. SHA EPUB : `8d0a4c5a34bdc8404900b8b61b8a93d633c7984f3497fe74a9d846adfa454bba`. Le ZIP temporaire n’a pas été sauvegardé.
5. Depuis la racine du dépôt, la vérification reproductible est :

```bash
python -B scripts/test_audit_mawdudi_epub.py
python -B scripts/audit_mawdudi_epub.py --epub "$MAWDUDI_SOURCE_EPUB" --output "$MAWDUDI_AUDIT_OUTPUT"
```

Définir ces deux variables avec les chemins réels de la source privée et d’une sortie locale. Comparer la sortie à EPUB_STRUCTURAL_AUDIT.json. Les chiffres 114 / 6127 / 109 sont des résultats structurels historiques, pas des nouveaux PASS.

6. Sauvegarder le paquet privé sur un stockage durable et vérifier les octets récupérés ; simulation de reprise complète NON EXÉCUTÉE à cause du quota. Les extraits privés intermédiaires sont régénérables depuis la source brute et le script.
7. Clôturer CP-00 après persistance vérifiée. Reprendre CP-01 lot sourates 001–010, jamais 114 d’un bloc ; sauvegarde publique expurgée et privée avant le lot suivant.
8. Pour 109 labels non reconnus, conserver la preuve/source et ne jamais compléter par IA ou une substitution globale. Les crochets de la sourate 9 sont déjà gérés ; ne pas les réauditer comme des omissions.
9. SOURCE_PRECHECK_FINDINGS.json contient les anomalies d’index 2/19/54/55. Aucune correction de bornes ou de titres n’est approuvée.
10. Préserver les moteurs Hifz, QCF et l’officielle. Aucun texte protégé, identifiant privé, APK ou secret dans GitHub/CI publics. CP-02 à CP-07 restent NON EXÉCUTÉS.

## Actualisation : stockage disponible

CP00_CLOSURE.md remplace les étapes de levée de quota ci-dessus : archive privée sauvegardée, récupérée et contrôlée. Reprendre CP-01 lot 001–010 au premier élément non terminé, sans refaire l’inventaire acquis. Les identifiants du paquet sont dans le reçu privé uniquement.

## Reprise CP-01

Lire CP01_LOT_001_010.md. Récupérer son archive privée par le nom du manifeste et l’identifiant du reçu privé ; vérifier son SHA. Les CSV publics fournissent les IDs et bornes, jamais les titres. Aucun titre ne doit être recréé à partir d’un ID. Contrôler les éléments encore PENDING ; ne pas compter la collecte comme une validation finale des dix sourates.

Lot source 011–020 collecté et sauvegardé : lire CP01_LOT_011_020.md, contrôler son SHA dans le manifeste et reprendre selon SOURCE_INTAKE_PROGRESS.csv. Révisions éditoriales encore en cours ; aucune collecte ne vaut validation de titre.

Lot source 021–030 collecté et sauvegardé : lire CP01_LOT_021_030.md, contrôler son SHA dans le manifeste et reprendre selon SOURCE_INTAKE_PROGRESS.csv. Révisions éditoriales encore en cours ; aucune collecte ne vaut validation de titre.

Lot source 031–040 collecté et sauvegardé : lire CP01_LOT_031_040.md, contrôler son SHA dans le manifeste et reprendre selon SOURCE_INTAKE_PROGRESS.csv. Révisions éditoriales encore en cours ; aucune collecte ne vaut validation de titre.

Lot source 041–050 collecté et sauvegardé : lire CP01_LOT_041_050.md, contrôler son SHA dans le manifeste et reprendre selon SOURCE_INTAKE_PROGRESS.csv. Révisions éditoriales encore en cours ; aucune collecte ne vaut validation de titre.

Lot source 051–060 collecté et sauvegardé : lire CP01_LOT_051_060.md, contrôler son SHA dans le manifeste et reprendre selon SOURCE_INTAKE_PROGRESS.csv. Révisions éditoriales encore en cours ; aucune collecte ne vaut validation de titre.

Lot source 061–070 collecté et sauvegardé : lire CP01_LOT_061_070.md, contrôler son SHA dans le manifeste et reprendre selon SOURCE_INTAKE_PROGRESS.csv. Révisions éditoriales encore en cours ; aucune collecte ne vaut validation de titre.

Lot source 071–080 collecté et sauvegardé : lire CP01_LOT_071_080.md, contrôler son SHA dans le manifeste et reprendre selon SOURCE_INTAKE_PROGRESS.csv. Révisions éditoriales encore en cours ; aucune collecte ne vaut validation de titre.

Lot source 081–090 collecté et sauvegardé : lire CP01_LOT_081_090.md, contrôler son SHA dans le manifeste et reprendre selon SOURCE_INTAKE_PROGRESS.csv. Révisions éditoriales encore en cours ; aucune collecte ne vaut validation de titre.

Lot source 091–100 collecté et sauvegardé : lire CP01_LOT_091_100.md, contrôler son SHA dans le manifeste et reprendre selon SOURCE_INTAKE_PROGRESS.csv. Révisions éditoriales encore en cours ; aucune collecte ne vaut validation de titre.

Lot source 101–110 collecté et sauvegardé : lire CP01_LOT_101_110.md, contrôler son SHA dans le manifeste et reprendre selon SOURCE_INTAKE_PROGRESS.csv. Révisions éditoriales encore en cours ; aucune collecte ne vaut validation de titre.

Lot source 111–114 collecté et sauvegardé : lire CP01_LOT_111_114.md, contrôler son SHA dans le manifeste et reprendre selon SOURCE_INTAKE_PROGRESS.csv. Révisions éditoriales encore en cours ; aucune collecte ne vaut validation de titre.

Parcours canonique de travail : lire CP01_CANONICAL_DRAFT_AUDIT.json et PASSAGE_INDEX_PUBLIC.csv. 1053 IDs couvrent 6236 ayat ; ce résultat numérique ne vaut pas validation éditoriale. Les douze archives NAV_DRAFT restaurées constituent les données privées correspondantes.

Lots privés partiels supplémentaires récupérés et vérifiés : voir CP01_CP03_PARTIAL_BATCHES.json. Scripts QCF numériques autonomes disponibles, sans changement de rendu ni du moteur. Révisions éditoriales partielles 11–20 et36–84 ; aucune approbation du texte OCR. Reprendre les lots éditoriaux suivants.

Révisions privées 001–035 stables et récupérées : 796 propositions, aucun PASS sémantique final. Lire CP01_REVIEW_001_035_PARTIAL.json. Source brute OCR, originaux conservés ; ne pas approuver automatiquement les propositions.


## Reprise après suspension Work, sans perte (15 octobre 2026)

1. Vérifier le vrai HEAD postérieur à `d7e7022f88d4f2029a6ca158600334e1f0e26679`, git status local/les éventuels patches Work et latest CI ; ne rien écraser, ne pas reset.
2. Lire `CP01_CONTINUATION_AUDIT_2026-10-15.md` et son JSON public. Rapport privé détaillé déjà conservé en bibliothèque sous le nom `MAWDUDI_CP01_CONTINUATION_AUDIT_PRIVATE_2026-10-15.json` avec SHA `785b28a0b580a760f8e56021244bfb431b16195447ae3e1d020398f13d563c83`. Source EPUB et 12 NAV_DRAFT ZIP déjà vérifiés.
3. L'asset Android numérique `hifz-app/src/main/assets/mawdudi_public/passages_v1.csv` et les classes `MawdudiPassageIndex` / `MawdudiMapModel` sont des fondations read-only, sans corpus privé. CI Audit et CP08P verts sur `d7e7022f`; vérifier le test instrumenté, ne pas le compter PASS avant publication de sa preuve.
4. Aucun titre n'est approuvé automatiquement ; les 9 adaptations, 59 candidats de terminologie, 104 ouvertures sans vérification manuelle initiale et 556 sections de commentaire restent à certifier selon les exigences du prompt final.
5. Poursuivre CP01/CP02 sur les lots privés déjà persistés, pas de nouvelle branche/dépôt/APK de staging présenté comme final. Recette BOOX/114 sourates seulement après les gates éditoriaux et le rendu QCF réel.

### Independent continuation (after SHA `872fac3479af70bac3581b0568f4261b33c66927`)

Read STATUS_CURRENT.md and the two public JSON records `CP01_SOURCE_INTEGRITY_PUBLIC_V2.json`, `CP02_REVERIFY_FIRST25_PUBLIC.json` before doing any extra extraction. Recover the two source audit JSON documents AND their scripts from persistent Library folder `/MAWDUDI_WORK_CONTINUATION_2026-10-15`; verify recorded SHA-256 and ensure no private text enters GitHub/CI. Original EPUB and 12 NAV_DRAFT archives remain persistently saved separately; reverify their SHA before use, never re-extract as new work.

Specific next unresolved tasks: independently review meaning/source of all 1,053 fine titles, macro theme source notes and all 114 first blocks; contextual review of 59 terminology candidates and nine editorial adaptations; compare all 556 historical English commentary sections/notes to an identified licensed edition and resolve OCR; finish genuine QCF thumbnails and two-way map/mushaf navigation for 114 surahs; finish Android Hifz/BOOX regression, then only a validated licensed private signed APK. The Android QCF page-end fix was committed and has a new JVM regression, not yet asserted PASS on its own SHA here.

Do not conflate the 25 raw SHA reproductions with text correctness. Never call a staging APK final; do not route other surahs back into Ibn Kathir in the final private package.

### CP02 continuation — 2026-10-15 source-safe scan locator

DO NOT restart CP00/CP01 source intake. Use scripts/mawdudi_ocr_page_reconcile.py on the original EPUB, private `CP02_HOCR_109_CASES_PRIVATE_V2.json`, canonical EPUB_UNRESOLVED_109.csv and the private 5-observation JSON (all available in persistent Library as previous checkpoints). 109 cases tracked, 107 candidate windows <=5 pages, 2 broad. Five browser observations have 2 visible and 3 not confirmed. No verse/text/footnote editorial certification, no rights proof. Public report `CP02_BROWSER_OCR_RECONCILIATION_PUBLIC.json` documents counts only. Do NOT alter OCR source, Hifz, or ship commentary based on these matches. Carry out original-scan plus independent-edition proof for remaining cases; keep all private text off GitHub. Check latest HEAD and CI before next commit.

### Point de reprise après de07de56 — OCR retention and QCF preview consistency

Reprendre EXACTEMENT l'audit source CP02 à partir du nouveau tableau `CP02_HOCR_EPUB_RETENTION_PUBLIC.json`, de la matrice de 109 références, et de l'archive **privée** `CP02_PRIVATE_OCR_RECOVERY_CANDIDATES_19_PAGES.zip` (SHA 012d19ee1d16e2175fe62121bd7f9e496ae41ad4b343c14b264faec9a598fb23), stockée dans Library `/MAWDUDI_WORK_CONTINUATION_2026-10-15`. 19 pages sont de forts candidats de pertes OCR; 41 références en attente de réparation sont concentrées là. Vérifier les scans originaux, commentaires et notes; ne promouvoir aucune page candidate sans alignement visuel et source indépendante. Le reste des 1907 pages EPUB hors hOCR n'a PAS été comparé. 6 tests source-safe + 5 tests rétention synthétiques PASS locaux; le commit de07de56 doit avoir ses CI propres. Pilot Baqarah pageEnd réutilise le resolver QCF général. Ne jamais reprendre l'ancienne liste Ibn Kathir dans le produit Maududi; ne pas toucher aux moteurs Hifz ni aux sources originales.

### Reprise CP03–CP04 navigation read-only (after 8cc1f02a)

Read `MawdudiMapModel.Navigation` and `MawdudiMapModelRealQcfTest` before modifying Android. New stable-id verified snapshot includes passage, QCF page, map/commentary scroll and panel state; navigation crosses surah boundaries without Hifz credit. Source public numeric index 1053/6236; no authorized original title/commentary or macro source is attached to this component. New CI audit + instrumented Android regression for 8cc1f02a must be checked. Continue with semantic map UI and real authentic QCF linking, but gate any source labels/commentaries from private storage and never re-enable old Ibn Kathir as final. CP02 19-page private OCR recovery archive intact in Library and titles still unapproved.

### 15 Oct 2026 — 11 screenshots / BookReader offset correction

Newest functional head `d5c3bfd2e9e8336f3d93cb9616c7b8dab002827c` on the single authorized branch. **Don't reset code or Work's prior private archives.** Former 19 high-risk links used internal EPUB file indices as if they were visible BookReader indexes. True verified mapping: `BookReader nK -> EPUB/page_(K+1).html`. Dedicated code and regressions correct this without any change to private corpus or Hifz engines. Audit of screenshots from user: 7:202 at n581, 16:59–60 at n960, 18:55–56 at n1072 are printed but corresponding EPUB verse labels absent; n1072/EPUB 1073 has nearly zero OCR text. The other 8 screenshots are context, do not mark entire commentary certified. Read `CP02_BOOKREADER_ALIGNMENT_PUBLIC_V2.json` for corrected 19-page list. Private evidence and reproducing script: library `/MAWDUDI_WORK_CONTINUATION_2026-10-15/CP02_USER_SCREENSHOTS_AUDIT_PRIVATE_2026-10-15.json`. Do not upload user screenshots to public GitHub, and do not treat missing labels as automatic commentary restoration. New CI must be inspected before PASS assertions.
