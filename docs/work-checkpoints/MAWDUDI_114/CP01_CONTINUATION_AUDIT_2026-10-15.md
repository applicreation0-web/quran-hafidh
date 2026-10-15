# CP01 — Contrôle de reprise indépendant (15 octobre 2026)

Statut : **PASS NUMÉRIQUE / EDITORIAL NON APPROUVÉ**.

- EPUB personnel fourni : SHA-256 `8d0a4c5a34bdc8404900b8b61b8a93d633c7984f3497fe74a9d846adfa454bba`, octets récupérés et hash comparé.
- Les **12 archives privées NAV_DRAFT de Work**, déjà enregistrées dans le manifeste, ont été récupérées depuis le stockage persistant et relues. Aucun lot de collecte n'a été recommencé.
- Inventaire recalculé sur les vrais JSON privés : **114 sourates / 1 053 IDs distincts / 6 236 versets**, pas de trou, doublon ou chevauchement numérique.
- **1 044 notices Ayah Themes d'origine** et **9 plages éditoriales**, conservées distinctes des titres de Maududi. Une seule des neuf possède le statut intermédiaire de vérification verset/note : **aucun PASS éditorial global**.
- **114 premiers passages** présents et présélectionnés. **10 premiers titres** portent une vérification manuelle limitée ; les **104 autres** et la validation intégrale restent ouverts.
- **59 passages** comportent un terme candidat à vérification contextualisée (dont « Jews » / « Israelites »). Aucune substitution globale ni correction automatique n'a été faite.
- Rapport analytique privé : `MAWDUDI_CP01_CONTINUATION_AUDIT_PRIVATE_2026-10-15.json` ; 77 140 octets ; SHA-256 `785b28a0b580a760f8e56021244bfb431b16195447ae3e1d020398f13d563c83`. Stocké dans la bibliothèque privée, hors GitHub.
- Vérification ponctuelle de la source EPUB : le segment **2:97–100** reste une anomalie OCR/documentaire à résoudre ; aucune restauration inventée.
- Limites : aucune approbation de commentaire/note, aucun titre original des sections de commentaire présumé, aucune vérification de droits concluante, aucune validation visuelle physique BOOX.

## Suite impérative

Continuer la validation sémantique **au premier lot non approuvé** avec conservation des titres originaux et des variantes éditoriales. Examiner les 9 adaptations et les 59 candidats terminologiques contre versets et source avant tout PASS. CP02 doit contrôler les textes/notes section par section à partir de l'EPUB récupéré. CP03 peut réutiliser le couplage numérique QCF ci-dessous mais les miniatures doivent venir du vrai renderer QCF.

## Intégration Android — non activée comme produit final

`95e86bb2` : ajout de l'asset numérique public `mawdudi_public/passages_v1.csv`, `MawdudiPassageIndex`, `MawdudiMapModel` et tests JVM ; **aucun titre/commentaire protégé**. Ces composants sont conservés séparément de l'ancien fournisseur Ibn Kathir : **le routage Android final n'est PAS remplacé**, faute de corpus approuvé.

`db97b233` et `d7e7022f` : correction **ciblée** des deux scripts de gel Hifz pour admettre exactement les deux nouveaux adaptateurs documentaires, sans autoriser de modifications moteurs. Suite aux FAIL initiaux sur ces listes, les contrôles `Ibn Kathir Hifz + Amorces audit ONLY` et `CP08P` sont **PASS** sur `d7e7022f`. La régression Android instrumentée `38786649334` était encore en cours au contrôle ; **pas de PASS emprunté**.

Le pilote Al-Baqarah possède la continuité et le surlignage QCF à préserver. Le nouveau modèle 114 sourates est une fondation numérique non livrable, pas un APK Mawdudi privé. Aucun moteur Hifz, rendu QCF ou application officielle n'a été modifié.
