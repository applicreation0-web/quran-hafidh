# Quran Safeguard 0.10.9 — handoff de re-audit Astra

## Candidat vérifié

- Branche : `release/0.10.9-work`
- HEAD applicatif candidat : `df0be3ce9521e0bf0ecb6a62bbb4e393f1261a4b`
- Parent audité NO-GO : `9846de48794e34eefc66d947e7f18991ad1c3342`
- Workflow vert : [run 34312527559](https://github.com/applicreation0-web/quran-unlock-android/actions/runs/34312527559)
- Job build : PASS
- Runtime instrumenté Light : 5/5 PASS, 0 échec, 0 erreur
- Runtime instrumenté Plus : 5/5 PASS, 0 échec, 0 erreur

Ce fichier est le seul ajout documentaire postérieur au HEAD applicatif candidat ci-dessus. Aucun code, test, gate ou artefact applicatif n'est modifié par ce handoff.

## Corrections bloquantes depuis le NO-GO

1. **Nettoyage E-Ink différé après Afficher brièvement**
   - `EInkRefreshPolicy` conserve un nettoyage complet pending lorsque le délai minimal interdit le refresh immédiat.
   - Le nettoyage est exécuté au premier instant admissible, y compris si un refresh est survenu pendant la révélation.
   - Les timers obsolètes sont annulés lors d'un refresh complet ou d'un changement de page.
   - Le refresh reste purement visuel et ne modifie aucun compteur, ligne, jalon, win, masque ou état pédagogique.

2. **Fallback E-Ink générique réellement visible**
   - Le fallback est déclenché dans la WebView au-dessus de la page HTML opaque par `window.einkFullRefreshFallback()`.
   - La zone de lecture reçoit une transition noir/crème synchronisée avec le masque ; si le hook WebView est absent, un overlay natif prend le relais.
   - Aucun SVG coranique n'est modifié, aucune API BOOX n'est obligatoire et STANDARD reste strictement no-op.

3. **Validation impossible avec un indice actif**
   - Toute aide remet immédiatement la série locale de réussites à zéro.
   - Une réussite sans aide ne peut pas être enregistrée tant que l'aide reste active.
   - `Refaire` efface l'aide de la tentative courante sans effacer les jalons antérieurs.
   - `Afficher brièvement` invalide également la tentative et la série locale.

4. **Reprise exacte après kill/restart**
   - Le mode Lecture/Mémorisation et la sélection sont persistés avec la session complète.
   - L'activité lit le mode persistant avant la création de la WebView.
   - Sous-bloc, ligne, étape, masque déterministe, compteurs, wins, jalons et aides sont restaurés.
   - Le Mushaf reste caché pendant le premier rendu jusqu'à l'application du masque restauré, empêchant le flash de texte non masqué.

5. **Preuves runtime et APK**
   - Les manifestes, permissions, identifiants, versions, frontières Light/Plus, 604 pages et état fermé de la gate audio sont extraits des APK produits.
   - L'émulateur API 35 exécute réellement les mêmes tests instrumentés sur Light et Plus.
   - La CI prépare explicitement la capacité disque et KVM sans remplacer les tests runtime par des recherches statiques.

## Tests de non-régression

- Politique E-Ink : page à t=0, révélation à t=100 ms, retour au masque à t=1000 ms, rattrapage différé ; refresh pendant révélation ; coalescence des révélations.
- Protocole : `2 réussites → Premier mot → tentative correcte` ne donne jamais `wins=3` et ne permet jamais la validation ; même invariant pour Afficher brièvement ; Refaire efface l'aide.
- Cycle de vie Android : Lecture → Mémorisation → progression → destruction Activity/WebView → relance depuis une intention Lecture → restauration exacte et masque présent sans frame initiale non masquée.
- Fallback E-Ink runtime : overlay visible sur WebView opaque en EINK, changement matériel de page compris ; strict no-op en STANDARD.
- Safeguard runtime : OFF → ON → OFF observable et absence d'état de cible hors service ; migration versionCode 27 → 28 conservant applications protégées et total de lecture.
- Protocole complet : 786 transitions PASS.
- Gates modernisées 10.8, vérificateurs 10.9, unitaires Light/Plus, builds debug/release Light/Plus et isolation des éditions : PASS.

## Artefacts issus du HEAD candidat

- Artifact build : `quran-safeguard-0.10.9-astra-re-audit`, ID `10089043591`
- Artifact runtime : `quran-safeguard-0.10.9-runtime-evidence`, ID `10089112961`
- Light : `app-light-release-unsigned.apk`
  - SHA-256 : `385c52b0c338845e43e90226db955b4eac07c1d3cc45597250690abd3cbe9782`
  - applicationId : `com.applicreation0.quransafeguard`
  - versionCode/versionName : `28` / `0.10.9`
- Plus : `app-plus-release-unsigned.apk`
  - SHA-256 : `7144c577db68102a271bdb99d26ad231ea72ea63b7142f4f4b0c5a65b7a9ef82`
  - applicationId : `com.applicreation0.quransafeguard.plus`
  - versionCode/versionName : `28` / `0.10.9-plus.1`

Les deux APK proviennent du même run et du même HEAD. Ils sont non signés ; aucune signature ni publication n'a été effectuée.

## Invariants préservés

- Mushaf exact de 604 pages, sans recomposition.
- Protocole 10/5/5/5/7 et aucun Tafsir en Mémorisation.
- Safeguard 60 secondes, budgets et exclusions sensibles conservés.
- STANDARD no-op pour E-Ink.
- Un seul APK Plus contenant STANDARD et EINK.
- Gate audio fermée (`redistributionApproved=false`) et aucune requête audio réseau.
- Aucun contournement, suppression ou assouplissement de gate n'a été introduit.

## Limites restant matérielles

Le chemin logiciel générique et les invariants pédagogiques sont couverts sur émulateur Android. L'efficacité physique du full refresh, le niveau résiduel réel de ghosting et les éventuelles API propriétaires doivent encore être vérifiés sur dalles E-Ink 7,8 pouces et 10,3 pouces/BOOX réelles. Ces vérifications matérielles ne sont pas déclarées PASS dans ce handoff.
