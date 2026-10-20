# Quran Haafidh — frise des jalons, état au 20 octobre 2026

**Contrat utilisateur : aucune remise d'APK tant que toutes les fonctionnalités Atlas, Hifz, Correspondances ne sont pas terminées et testées ensemble.** Aucun APK partiel, aucune publication publique sans accord.

## Frise de livraison à reprendre au prochain jalon

`[J0 ATLAS/HIFZ ✓]──[J1 110 DOSSIERS ✓]──[J2 LECTURE 110 ✓ / CONTRE-AVIS ◐]──[J3 TRIAGE 110 ✓ / CERTIFICATIONS ◐]──[J4 CATALOGUE/QCF ○]──[J5 TESTS/LIVRAISON ○]`

| Jalon | État vérifié | Preuve ou blocage | Action suivante |
|---|---|---|---|
| J0 Socle Atlas/Hifz | **ACQUIS** | Atlas 366+1 053 titres verrouillés, 1 419 correspondances approuvées ; code applicatif 26f7f619, 3 CI réussies dont 108 tests Android synthétiques. BOOX physique non testé. | Ne pas modifier les acquis ; tests globaux en J5. |
| J1 Inventaire documentaire | **ACQUIS** | 110/110 IDs ; 80 retenues/documentées, 24 en attente, 6 écartées, références originales préservées. | Distinguer admission et certification. |
| J2 Lectures et audit contradictoire | **COUVERTURE DOCUMENTAIRE 110/110 ; CONTRE-AVIS INDÉPENDANT EN COURS** | 15 revues ciblées historiques + 39 approfondies + 56 courtes ; chaque dossier a une objection. Jalalayn 734 locateurs recollés ; 1 157 occurrences de versets et 9 693 positions de mots recomptées sur les quatre lots de l'ancien APK, hashes conformes ; aucune divergence statique. | Ne pas confondre recollation et vérification indépendante ; obtenir des preuves spécifiques et contradicteurs externes avant certification. |
| J3 Certification par dossier | **EN COURS — TRIAGE 110/110, CERTIFICATION INDIVIDUELLE OUVERTE** | 110/110 dossiers ont reçu un contre-test documentaire/lexical gradué (46 deux témoins arabes, 13 progressions, 24 ressemblances, 27 structures ou types ouverts). **Source supplémentaire vérifiée dans l’original arabe : al-Suyūṭī, *Marāṣid al-Maṭāliʿ*, p. 51/54/69/70/73/75/83 : sept attestations ciblées R038/R049/R051/R057/R079/R085/R089 ; contre-épreuve de bornes R084 (64:4 vs 64:1).** 4 certifications historiques, 0 nouvelle ; 0 production. | Valider individuellement la catégorie exacte et les frontières ; indépendance savante/droits/QCF/QA avant activation. |
| J4 Catalogue utilisable et QCF | **À TERMINER** | Architecture Android existante ; 0 relation autorisée en production, ancrages focaux et QA native non certifiés. | Catalogue approuvé, 8 types/icônes, vrais foyers QCF, fade-away, restauration sans régression. |
| J5 QA globale puis APK complet | **À TERMINER** | 108 tests Android sur corpus public/synthétique ; aucune recette BOOX de l'application complète. | Test intégré privé, BOOX, audits et corrections ; un seul APK complet à soumettre après validation. |

**Règle de mesure : jamais de pourcentage estimé ; utiliser les états fondés sur preuves et les vrais compteurs.** Mise à jour de ce fichier et de la frise interactive HTML après chaque jalon terminé, non en arrière-plan. Le ZIP privé contenant le détail des 110 dossiers ne doit pas être versé dans GitHub public.

## Contre-expertise ciblée : état des preuves (non-certificateur)

- **R011 :** 17:111 / 18:1, répétition partielle de formule de louange ; ni exclusivité ni grande structure entre sourates démontrée.
- **R021 :** al-Suyūṭī compare 55/56 sur une structure plus large que la plage 55:37–78 retenue initialement : attribution savante attestée mais focus et limites à distinguer.
- **R026 :** inversion jour/nuit locale 91:3–4 / 92:1–2 ; 91:1–2 = contexte, pas un chiasme global.
- **R032 :** Farrin décrit sept unités concentriques dans toute al-Baqarah, 2:142–152 = centre D ; pas symétrie interne à ces onze versets.
- **R033 :** Farrin propose neuf membres dans 2:255 et un centre lié à la connaissance divine ; géométrie mot QCF à démontrer avant toute hachure.
- **R038 :** 18:1 ↔ 18:109–110, écho thématique possible ; *inclusio* lexicale insuffisamment prouvée : ne pas certifier sous ce type.
- **R078 :** 109:2–5, alternance textuelle je/vous, 109:3 et 109:5 identiques ; nuance de 109:2/109:4.

### Références directes, non épuisement du corpus

- Farrin (2014), entretien d'auteur : https://en.alukah.net/library/2/4842
- Farrin (2010), `Surat al-Baqara: A Structural Analysis` : https://onlinelibrary.wiley.com/doi/10.1111/j.1478-1913.2009.01299.x
- al-Suyūṭī, `Asrār Tartīb al-Qurʾān`, passage traduit à propos de la sourate 56 : https://www.imamghazali.org/blog/tafsir-of-surah-al-waqiah-verses-1-26
- Texte coranique 109 : https://quran.com/al-kafirun ; 18 : https://quran.com/al-kahf

**Aucune attestation bibliographique générale n'est convertie en preuve par ID automatiquement.** La grille documentaire et les corrections restent hors de l'application jusqu'à certification individuelle. Les ressources privées demeurent hors GitHub.

## Mise à jour probante — 20 octobre, J2/J3

- Sources de contrôle : *registres privés conservés hors GitHub*, base Jalalayn de l'ancien APK signé, quatre JSON `assets/reader109/word-boxes` dont SHA-256 revérifiés, et deux lots de corroborations externes.
- 110/110 dossiers de J2 couverts, dont **56 lectures courtes** ; clôture de la **couverture locale**, **pas** d'un audit contradictoire humain ou intermodèle réellement indépendant.
- **14 dossiers** font l'objet d'un recoupement externe ciblé : R028, R031, R040, R048, R057, R059, R071, R058, R068, R069, R073, R075, R078, R083. Aucun passage automatique de « documentée » à « certifiée ».
- **R071** : 94:5 diffère de 94:6 par `فَ` initial ; seule la proposition commune constitue une répétition exacte.
- **R078** : 109:3 et 109:5 sont répétés ; 109:2 et 109:4 diffèrent. L'alternance des positions énonciatives reste défendable, pas une répétition intégrale des 4 versets.
- **R083** : 62:10 permet la dispersion après la prière ; 62:11 réprouve la dispersion de l'assemblée avant sa clôture. Éviter une fausse opposition entre dispersion en soi et prière.
- **R033** : les rectangles des 50 mots présents en page 42 sont confirmés par les références statiques, avec segmentation 9 membres ; cela ne prouve ni la structure par expertise scientifique indépendante ni la réussite du surlignage hachuré sur Android/BOOX.
- Tout contrôle du contenu privé reste **hors dépôt**, dépôt uniquement pour les compteurs, corrections de méthode et sources publiques.
- ZIP privé produit pour transmission : `QURAN_HAAFIDH_CORRESPONDANCES_J3_14_ET_FRISE_2026-10-20.zip`; SHA-256 `7531014070e1e6059c318e13987dd826d8f657df93694246963f2864be56087d`. Frise interactive locale J3_14.html, table 110 IDs, sources et manifest.
- Règle absolue : **0 relation en production, 0 certification nouvelle, 0 APK transmis**. Atlas et Hifz inchangés.
