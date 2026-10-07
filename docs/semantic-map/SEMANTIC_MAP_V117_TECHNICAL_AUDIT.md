# Audit technique — Carte sémantique / Quran Haafidh 1.17

Audit du 2026-10-07. Base vérifiée localement et à distance :
`82658bde0ef80be5d521c5838d19139aa63c80e4`.
Branche exclusive : `work/chatgpt-semantic-map-v1-17`.
Le HEAD initial est exactement la base ; arbre initial propre. Aucune autre branche
n'est checkoutée ou modifiée. `hifz-app/build.gradle.kts` confirme versionCode 38,
versionName 1.17. Le répertoire historique `chatgpt/` et le produit Safeguard `app/`
ne sont pas le point d'intégration de Haafidh.

## Architecture constatée

| Fonction | Fichiers / comportement |
|---|---|
| Accueil | `hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java` : Activity Android native ; lignes Lecture, Mémorisation libre, Quiz, Parcours Hifz, Progression ; parcours en sous-vue |
| Lecture | `StudyReaderActivity.java` ; extras `jumpPage` et `jumpVerse` ; écrit le curseur `hifz_study`, initialise HifzPrefs et le corpus Amorces |
| Mushaf | `MushafView.java` ; WebView hors ligne, pages KFQC SVG Brotli embarquées ; `hifzreader/index.html` et `reader.js` ; aucun accès réseau |
| Verset | `hifz-core/.../VerseRef` ; références sourate/verset, géométrie par ligne dans `GeometryRepository` |
| Mapping | `GeometryRepository.pageForVerse` utilise la première ligne ; `lastLineIndex` est indispensable pour la FIN d'un verset qui déborde sur une page suivante |
| Sources embarquées | `prepareHifzAssets` copie Mushaf, geometry.json, waqf, boîtes mot-à-mot, Tafsir et corpus sémantique Al-Munir existant |
| Téléphone / tablette | Lecture : seuil smallestScreenWidthDp >= 600, Tafsir latéral 58/42 sur grand écran, panneau inférieur sur téléphone |
| Paysage | Activités du lecteur déclarent configChanges orientation/screenSize ; vérifier le redimensionnement sans réinitialiser la section |
| E-Ink | `EinkController` : détection Onyx/Boox ou préférence forcée, rafraîchissement partiel puis nettoyage périodique, fallback invalidate sans flash artificiel |
| Tests | 103 fichiers sous hifz-app/src/test au départ ; JVM comportementaux séparés des SourceContractTest ; hifz-core:test ; instrumentation existante |
| Build de référence | CI : Java 17, Gradle 9.5.0, SDK 37, build-tools 36.0.0. Ni Gradle ni SDK Android trouvés localement lors du premier contrôle |

## Point critique d'isolation

Le constructeur de MushafView crée HifzPrefs, lance ses migrations et appelle
maskEntropyFor("reader_default"), qui peut écrire. Une dépendance de rendu n'est
donc pas intrinsèquement une dépendance sans effet de bord. Ouvrir StudyReader
tel quel ajoute aussi des changements de curseur et des Amorces à la carte.

Option préférée à vérifier : Activity dédiée à la carte, réemploi de MushafView
avec contexte isolant TOUTES ses préférences dans un espace `semantic_map_*`.
Ne pas injecter une copie vivante de la progression. Vérifier les autres accès
au contexte avant de retenir cette option. Si l'isolation ne peut être prouvée,
ne pas activer le prototype et documenter le blocage. Aucun changement profond
du moteur n'est autorisé.

## Surface de modification envisagée

- MainActivity.java : une seule ligne de navigation Carte sémantique.
- hifz-app/src/main/AndroidManifest.xml : déclaration d'une Activity privée.
- Nouveaux fichiers préfixés SemanticMap pour modèle, dépôt, vue et contexte.
- Nouveau dossier d'assets `semantic-map/`, distinct de `semantic/`.
- Nouveaux scripts de validation et tests dédiés ; documentation dans ce dossier.
- Éventuel workflow propre à cette branche, lecture seule des contenus GitHub,
  sans publication de release ni accès aux clés de signature.

## Fichiers protégés

Ne pas modifier hifz-core, HifzPrefs, HifzSessionActivity, SessionClock, les moteurs
Sabqi/Itqan/Consolidation/Renforcement/Révision, DashboardLedger, Quiz*,
SemanticPassageRepository, les assets semantic-source et hifzreader, StudyReader,
MushafView, les scripts matérialisant Al-Munir, les workflows de release,
la version de l'application ou les clés de signature. Aucun fichier sous
chatgpt/ ou app/ ne doit être changé pour cette mission.

## Risques et vérification

1. Effets de bord du constructeur de rendu : test d'isolation des préférences.
2. Fin de verset sur deux pages : contrôler première ET dernière page géométrique.
3. OCR arabe et numérotation de volumes : contrôle visuel et références imprimées.
4. Rotation / retour Android : conserver sourate, section, page et retour carte.
5. Corpus invalide : refuser l'ouverture, ne pas substituer Al-Munir.
6. Régression accueil : diff minimal ; exécuter tests JVM, contrats et build.
7. BOOX réel : non attestable par inspection source ou simulateur LCD ; documenter
   explicitement les essais matériels non effectués.

## Retrait complet

Supprimer la ligne d'accueil, la déclaration d'Activity et tous les nouveaux
fichiers SemanticMap/assets/tests/scripts dédiés. Aucun schéma Hifz à rétrograder,
aucune migration à annuler, aucun corpus existant à restaurer. Des préférences
exclusivement préfixées semantic_map peuvent être supprimées sans toucher Hifz.

## Statut de l'audit

Inspection statique réalisée ; intégration proposée, non encore implémentée.
Aucun test dynamique ou build n'est déclaré réussi à ce stade.
