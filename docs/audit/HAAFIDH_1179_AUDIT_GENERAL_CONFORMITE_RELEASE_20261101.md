# Quran Haafidh 1.17.9 — audit général des demandes et portes de release privée
**Date : 1 novembre 2026. Statut : AUDIT EN COURS / NO GO pour APK privé.**
**Périmètre :** source de la branche `work/haafidh-1179-final-compliance-20261101`, dérivée de la base fonctionnelle certifiée `d08a97cbe307219628125431af701e6cbc40e795`. Cette note est séparée de la branche de travail pour ne pas perturber les tests. Vérifier le HEAD réel et les résultats Actions avant de figer la candidate.

## 1. Principe de vérité
- **Validé auparavant :** [12/12 jobs Android/JVM/Hifz catalogue](https://github.com/applicreation0-web/quran-hafidh/actions/runs/41212783170), [8/8 partitions 279 paires](https://github.com/applicreation0-web/quran-hafidh/actions/runs/41210439759), [certificat officiel historique](https://github.com/applicreation0-web/quran-hafidh/actions/runs/41209120644).
- **Endurance sur l'ancien HEAD de production :** [300 retours de navigation par profil](https://github.com/applicreation0-web/quran-hafidh/actions/runs/41246852263) : jobs navigation PASS, *run global FAILURE* dû aux anciens jobs Tafsīr ; [200 consultations Tafsīr, 100 rotations et 400 sélections WebView par profil](https://github.com/applicreation0-web/quran-hafidh/actions/runs/41249576279) : deux jobs PASS ; [recueil des preuves](https://github.com/applicreation0-web/quran-hafidh/actions/runs/41252645644) : PASS collecte, non nouveau test.
- **Après corrections supplémentaires du 1er novembre :** la validation complète sur le nouvel HEAD doit réussir séparément. Ne pas transférer artificiellement les anciennes réussites sur le nouveau code.
- Aucun résultat d'émulateur ne remplace le tactile système ou le BOOX E-Ink physique.

## 2. Matrice des recommandations utilisateur
| Recommandation | État du code | Justificatif / réserve |
|---|---|---|
| 279 paires admises ; neuf hors catalogue, six exclues ; aucun arbitrage automatique | **OK base certifiée** | `AtlasDiscriminantCases.java`, `research/alabbad_catalogue_approved.json`, vérification catalogue |
| Cas particulier 13:18 ↔ 39:47 (ID 62), 2:63 ↔ 2:93, intra/inter-sourate | **OK base certifiée** | indices canon/QCF, tests dédiés |
| Deux véritables pages Mushaf QCF Médine 345×550 ; pas de reflow | **OK sur Android émulé** | `MapQcfThumbnails.java`, 604 pages locales SVG, app teste proportions/contours |
| Extraits côte à côte, réduire aux différences utiles sans faux mot coranique | **CORRIGÉ sur branche conformité, CI à valider** | `QcfAtlasVerseStrips.java` + `AtlasDiscriminantOmissions.java`, sixieme source? cinq cas définis précisément ci-dessous |
| Soulignements et mots pivots QCF agrandis, lignes sans coupure | **PARTIEL** | vrai SVG QCF et recadrages vecteurs, tailles/ghosting à contrôler physiquement ; pas de mutation des glyphes originaux |
| Rapport explicite du type de relation et des mots discriminants sans pavé ni texte artificiel | **CORRIGÉ sur branche conformité, CI à valider** | dialogue : type, références, indices et source. 279 descriptions religieuses sont vides par choix de ne pas inventer de commentaire |
| Ajout/omission : ne pas souligner un mot commun comme substitut ; « absent » côté manquant | **CORRIGÉ par projection d'affichage, CI à valider** | cas 142,145,149,161,185 ; ancre de recadrage ≠ mot souligné. Source éditoriale d'origine conservée |
| Pivot double / groupes complets / actif-passif / inversion : revue de 12 cas | **PARTIEL** | 5 cas d'omission projetés ; 197/198/293 à conserver, 120/124/147/199 encore proposés dans V2, non promus sans certification éditoriale/QCF |
| Traduction française **courte relue** de chaque pivot utile | **NON TERMINÉ** | aucun corpus complet de glosses françaises des 279 cas n'est certifié dans l'application ; ne pas déduire d'une traduction automatique ni du Tafsīr anglais |
| Tafsīr sur clic de chacun des versets QCF, gauche/droite, sans fausse référence | **OK base + correction** | polygones QCF natifs, contrôle génération et référence, chargement Jalālayn original de chaque côté |
| Lecture liée à la comparaison et au verset QCF réellement choisi | **CORRIGÉ sur branche conformité** | bouton Lecture suit désormais le verset touché, plus seulement l'ancre initiale |
| Icône « deux maillons » hors Mushaf, signalement Lecture et Révision | **OK sur émulateurs** | `AtlasDiscriminantMarkers.java`, pas de second bouton/rail massif |
| Masquer les portions QCF non pertinentes dans les comparaisons | **PARTIEL** | extraits limités avec SVG/QCF authentiques, mais atténuation uniforme du reste d'une page entière non garantie contractuellement |
| Révision active : aucun indice en restitution masquée, comparaison seulement après vraie validation ; reprise visible/masquée | **OK sur tests existants** | `RevisionAtlasAccess`, masques/replay, tests d'états; moteurs non modifiés |
| Révision passive : timer suspendu en comparaison et Lecture, reprise exacte | **OK sur tests existants** | 300 navigations par profil et test reprise du chronomètre |
| Finales Sabqi/Itqān et historique OPEN ; jamais de finale Itqān nouvelle après An-Nās | **PRÉSERVÉ / tests historiques** | 52 tests de données historiques, aucun changement moteur Hifz |
| Sauvegardes v3 schéma6 sourceSnapshot, aucun crédit artificiel, migrations | **PRÉSERVÉ / tests historiques** | garde source comparé à 1.17.8, backups et engines non modifiés |
| Accueil BOOX : grosses icônes, libellés redondants sous icône supprimés, pas de décoration superflue | **CORRIGÉ sur branche conformité, CI à valider** | `MainActivity.java`, noms d'accessibilité et interactions conservés |
| Affichage BOOX réel : ghosting, taille, rémanence après masquage/dévoilement | **NON VALIDÉ physiquement** | nécessite BOOX et modes de rafraîchissement E-Ink |
| Robustesse après 300 navigations, 200 Tafsīr, 100 rotations, surveillance PSS/ANR | **VALIDÉ sur d08; à corréler au nouvel HEAD** | aucune détection d'ANR/crash applicatif lors de ces passages ; injection système rejetée par Taskbar d'émulateur |
| Aucune publication publique, upgrade en place et signature originale | **GARDES PRÉSENTS / LIVRAISON BLOQUÉE** | dépôt GitHub PUBLIC ; APK et ressources/clé privées ne doivent JAMAIS être dans Releases/Actions publics |

## 3. Cinq corrections éditoriales à afficher sans modifier le corpus
La revue V2 des 294 cas conserve cinq dossiers admis nécessitant de distinguer les vrais mots présents des ancrages communs.
| ID | Passages | Mot(s) soulignés sur le premier QCF | Mot(s) soulignés sur le second QCF | Relation |
|---|---|---|---|---|
| 142 | 11:60 / 11:99 | 4 (ٱلدُّنۡیَا) | aucun | absent second côté |
| 145 | 39:72 / 40:76 | 1 (قِیلَ) | aucun | absent second côté |
| 149 | 22:62 / 31:30 | 11 (هُوَ) | aucun | absent second côté |
| 161 | 2:174 / 3:77 | aucun | 18,19,20 (وَلَا یَنظُرُ إِلَیۡهِمۡ) | ajouté second côté |
| 185 | 7:54 / 25:59 | aucun | 5,6 (وَمَا بَیۡنَهُمَا) | ajouté second côté |

Ces vecteurs d'indices viennent de `research/mutash_pivot_review_v2_20261031.json` et doivent être confirmés dans les tests QCF sur le **nouveau HEAD** ; les 279 décisions restent gelées. Le côté sans mot distinct utilise l'ancre historique **uniquement pour recadrer** les vraies lignes QCF ; il ne doit plus dessiner de soulignement trompeur.

## 4. Comparaison de non-régression et périmètre
Par rapport à `d08a97c`, les modifications de production ciblées concernent uniquement les adaptateurs Atlas, le recadrage QCF, la projection des cinq omissions et `MainActivity` (libellés BOOX). Aucun fichier `hifz-core/src`, `HifzPrefs`, `HifzBackup`, ordonnanceur ou moteur des finales n'a été modifié. Un contrôle Python de delta exact préserve ces frontières, avec hash Git-blobs figés pour les seuls nouveaux changements d'UI hors allowlist initiale.

**Limitation éditoriale majeure :** les 279 entrées n'ont aucune glose française finalisée, et certains contrastes curatés restent une revue **non applicable automatiquement**. La release ne doit pas être déclarée conforme à 100 % de ces demandes avant certification.

## 5. Signature, confidentialité et candidat privé
- Le dépôt GitHub est **public**. Aucune clé historique, aucune des sept ressources protégées, aucune progression utilisateur et **aucun APK privé signé** ne doivent y être déposés.
- Vérifications sources : appId `com.quransafeguard.hifz`, version `1.17.9`, code 47, certificat historique SHA256 `66adc4f0dcf70d716a8050fd4b0195266307576aa2f9bd54c9cbf2851a330e14`.
- Une attestation de certificat existante ne fournit **pas** l'accès à la clé de signature. Les sept ressources avaient été vérifiées dans un environnement Work devenu indisponible et ne sont pas accessibles ici.
- Outil public sûr pour un ordinateur local de confiance : `scripts/build_1179_private_offline.py` + `scripts/test_build_1179_private_offline.py`, sans publication ni installation automatique. Il exige : checkout Git exact et propre, 604 SVG QCF, sept ressources SHA originales, vrai keystore historique et certificat, APK historique officiel de référence et Android SDK build tools 36.0.0. Produit seulement une candidate privée hors du dépôt et un rapport avec hash, lorsque tous les intrants existent.
- **Aucun APK privé signé 1.17.9 n'a encore été généré ou installé.**
- Avant GO : APK complet signé, contrôle de l'APK historique vraiment installé, sauvegarde externe vérifiée, `adb install -r`/mise à jour sur place sans désinstallation, comparaisons des acquis/credits/finales et recette écran E-Ink physique incluant toucher des deux Mushaf et ghosting.

## 6. Décision
**NO GO pour la release privée à ce stade.** Ne jamais confondre un pipeline CI vert avec une installation physique signée. Cette note décrit les dernières corrections et les limites jusqu'à la production d'un résultat final de tests sur le HEAD de conformité. Les recommandations non complètement réalisées restent visibles au lieu d'être artificiellement marquées comme satisfaites.

Aucun changement automatique des 279 décisions, aucun nouvel arbitrage demandé, aucune modification des moteurs Hifz, aucune publication publique.
