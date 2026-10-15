# Mawdudi 114 — état courant

CP-00 terminé et reprise isolée vérifiée. CP-01 en cours : les 114 index source sont capturés ; 1 044 notices originales et neuf adaptations de plages manquantes forment un parcours de travail de 1 053 passages, couvrant 6 236 versets sans doublon. Contrôle numérique uniquement : aucune approbation éditoriale globale.

Les douze archives privées du parcours de travail ont été sauvegardées, récupérées et vérifiées par SHA-256. La révision des titres, des introductions et des ouvertures est répartie par sourates ; les cas OCR demeurent explicitement partiels. CP-02 demeure ouvert (texte, notes, édition, droits). Préparation numérique QCF en cours ; miniatures et validation visuelle non exécutées. Aucun nouvel APK.

HEAD précédent confirmé : `7212a149d6d293c495e2857e3a1bdd1a382e4973`. Le commit de ce checkpoint suit sur la même branche. Reprendre la révision éditoriale et l’audit des commentaires, sans recommencer les captures source validées.


## Reprise auditée 2026-10-15 — source complète du statut : CP01_CONTINUATION_AUDIT_2026-10-15.md

SHA avant nouveau checkpoint documentaire : `d7e7022f88d4f2029a6ca158600334e1f0e26679` (vérifié distant). EPUB privé + 12 ZIPs NAV_DRAFT récupérés indépendamment depuis la bibliothèque persistante, SHA originaux contrôlés, 1053 IDs/6236 ayat/114 sourates sans trou ni doublon. L'original anglais reste privé. Rapport individuel privé persistant `MAWDUDI_CP01_CONTINUATION_AUDIT_PRIVATE_2026-10-15.json` (77 140 octets, SHA 785b28a0b580a760f8e56021244bfb431b16195447ae3e1d020398f13d563c83). 10/114 premières ouvertures seulement avec revue manuelle partielle, zéro PASS sémantique final, 59 dossiers de vocabulaire en attente. CP02 commentaires et droits ouverts. Intégration NUMÉRIQUE Android séparée réalisée, non branchée comme interface finale. Les CI de gel/audit et staging du commit d7e7022f sont vertes ; tests Android instrumentés encore en cours. Aucun nouvel APK complet ni essais BOOX.

NE PAS reproduire la collecte source ni recommencer CP00. Reprendre au premier lot NON APPROUVÉ de CP01, puis CP02. Ne pas confondre asset numérique de navigation et produit final. Vérifier la régression 38786649334 avant tout nouveau statut vert.

## Continuation CP01–CP03 — independent source and QCF checks (15 Oct 2026)

CP01 original 12 NAV_DRAFT archives independently parsed: 114 surahs, 1,053 passages, 6,236 ayat; all stable IDs, ordinal/previous/next links, coverage, origins, source references checked. 1,044 literal Ayah Themes headings (PRIVATE), 9 separately tagged editorial adaptations. All 114 openings screened for preface-keyword contamination (0 automated flags; **NOT** semantic approval). 59 candidate terminology passages logged. Zero titles in draft empty under correct origin rules. **0/1,053 final semantic approvals**; 114 opening meaning gates OPEN.

CP02: source EPUB SHA-256 rechecked; the retained evidence for historical sections 1–25 independently reproduced **25/25 raw SHA-256** values, with missing labels at 2:97–100 and a footnote/OCR defect in 1:1–7. **0 newly approved bodies or notes**; 531 historical sections not yet cross-checked in this new independent pass. Original edition/translation and offline embedding authorization remain unverified.

CP03: canonical 1,053-range Android model already present; physical QCF bridge now fixes a bug when the last ayah of a passage continues onto a later page (commit `872fac3479af70bac3581b0568f4261b33c66927`). Added real geometry whole-index regression; **CI pending** at this checkpoint. Real miniatures, highlight positioning and physical BOOX still NOT EXECUTED. Do not route the user-facing final product to unapproved content.

PRIVATE persisted in Library: `/MAWDUDI_WORK_CONTINUATION_2026-10-15/CP01_114_SOURATES_INTEGRITY_PRIVATE_V2.json` (103,371 bytes, SHA-256 `10d0568f290625d8e6cc1b81a602867ddf8a4feedb88ad1debd3b4ba784edb4a`), `CP02_REVERIFY_FIRST25_PRIVATE.json` (8,143 bytes, SHA-256 `82a33b096179c614a074a096df96210236273f98f2e18d2c4d4547baebdaabed`). Both scripts independently saved there. Public counts only in CP01_SOURCE_INTEGRITY_PUBLIC_V2.json / CP02_REVERIFY_FIRST25_PUBLIC.json. No third-party title/body text in GitHub.

**Gates:** CP01 numeric PASS / semantic BLOCKED_PENDING_REVIEW; CP02 structural source first25 REVERIFIED / 556 body+notes NOT APPROVED; CP03 page-end bug FIXED / visual NOT TESTED; CP04–CP07 NOT COMPLETE. The final APK is not ready. Resume without collecting the source archives again.

## CP02 — BookReader OCR / EPUB source audit 2026-10-15

New independent source-safe Python tool `scripts/mawdudi_ocr_page_reconcile.py` and six synthetic tests. Inputs already verified on a private copy: 109 cases, 44 exact hOCR words, 65 other candidate locations, 107 references within 5-page scan windows, 2 broad review windows (24:3: 32 pages; 66:5: 7 pages). All are CANDIDATES, NOT corrections. Five original BookReader scan markers inspected: page n50, 2:97 and 2:100 visible, 2:98 and 2:99 NOT CONFIRMED; page n110, 2:217 NOT CONFIRMED. Browser non-detection is not proof of absence; mark 3 as discrepancy pending zoom/image validation. 0 newly editorial-approved texts, notes or sections, 0 automated repairs; rights/edition unresolved. The detailed 109-case metadata-only ledger, preliminary 5-observation evidence and both scripts were saved in the persistent private Library folder `/MAWDUDI_WORK_CONTINUATION_2026-10-15`. EPUB and hOCR originals immutable; no Maududi protected text in GitHub/CI. New CI required before PASS claim. CP01 numeric pass; CP02 incomplete; CP03 QCF compile regressions passed on af238389; CP04–07 not complete.

## CP02 — 1 210 pages hOCR ↔ EPUB, audit de conservation du texte

Nouvel audit métadonnées par page (script `scripts/mawdudi_hocr_epub_retention.py`) sur sources réelles conservées : 1 210 pages hOCR comparées aux 1 210 pages EPUB correspondantes, **1 189 sans différence OCR majeure**, **14 pertes critiques candidates**, **5 pertes fortes candidates**, 1 différence à réexaminer et 1 page insuffisamment probante. Total **19 pages fortement dégradées** = [50,110,215,228,288,344,362,482,549,582,960,961,993,1009,1032,1073,1106,1124,1143]. Les **41 repères hOCR absents de la page EPUB correspondante** sont tous parmi ces 19 pages; les **3 autres repères hOCR** figurent déjà dans l'EPUB (10:14, 19:84, 20:78). Cette corrélation identifie une perte de texte OCR localisée dans le corpus de 1 210 pages, mais **ne prouve pas la complétude ni la justesse de l'édition** et ne couvre pas les pages 1 210–3 116.

Paquet privé durable `CP02_PRIVATE_OCR_RECOVERY_CANDIDATES_19_PAGES.zip` = 134 875 octets, SHA-256 `012d19ee1d16e2175fe62121bd7f9e496ae41ad4b343c14b264faec9a598fb23`, contient 19 brouillons de pages OCR avec ordre des mots, confiance/coordonnées, manifestes source/EPUB et hashes. **0 pages insérées dans l'application, 0 commentaires approuvés**; les pages candidates ne sont pas une réparation validée. Conservé dans la bibliothèque privée `/MAWDUDI_WORK_CONTINUATION_2026-10-15/`, aucun texte source dans le dépôt GitHub. Les sources d'origine ne sont pas modifiées.

CP03 : commit `de07de56aec7d6e1526ae6b832c513270345331a` aligne `BaqaraPilotScreen.pageEnd()` sur `MawdudiMapModel.lastPageForVerse()` (dernier verset pouvant continuer sur une autre page). Test QCF réel sur 1053 intervalles adapté ; **CI NOUVELLE EN COURS**, pas de PASS anticipé ni BOOX physique. CP04–CP07 et APK privé final NON TERMINÉS. Edition exacte, correction des notes et droits d'intégration toujours ouverts.
