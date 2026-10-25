# QURAN HAAFIDH 1.17.5 — AUDIT PRÉ-RELEASE APPROFONDI (FOND & FORME)
Date : 25 octobre 2026
Dépôt : applicreation0-web/quran-hafidh
Branche : work/haafidh-1175-boox-ux-hifz-atlas
HEAD audité pour les corrections : 2f1bf36783671355bba0656b0b1e8bc59fc05ea8
Version candidate : 1.17.5, versionCode 43 — applicationId release com.quransafeguard.hifz
**Verdict : NO-GO release officielle. NO-GO APK installable par-dessus 1.17.4 tant que la signature et le corpus privé ne sont pas validés.**

## 1. Périmètre et méthode

Audit différentiel depuis 4135df3863e17da72adbd06457f46f2966ddcb6a, et non reprise de tous les audits historiques.
Examen du moteur Hifz, progression schema 6, format de sauvegarde v3, répétitions,
coexistence Sabqi/Itqan/soir/finale, masques physiques QCF, sélecteurs Ayah Themes,
routage des quatre accès d'accueil et contrats Atlas figés.
Inspection des **six photographies originales de BOOX** dans
HAAFIDH_BOOX_REPRISE_6_PHOTOS_ET_PROMPT_20261025.zip : empreintes du manifeste
vérifiées 6/6. Ces images représentent un **debug antérieur**. Les captures
Android des workflows sont **émulateur Pixel 2, et non BOOX**.

## 2. Défauts métier démontrés et corrections

| Gravité | Observation | Correctif et preuve |
| --- | --- | --- |
| P0 | La finale et la séance ordinaire partageaient la même clé de persistance, causant conflits de restauration. | Commit 27928f76 : stockage séparé par famille, migration conservant le payload. |
| P0 | Une finale démarrée antérieurement pouvait ne plus être trouvée après passage de date : le lecteur d'OPEN consultait encore le slot ordinaire. | Commit b3ff49f9 : recherche par type de cycle, migration préalable et date d'origine maintenue. |
| P0 | Le nouvel assemblage Sabqi en pages n'avait plus les mêmes limites que les 5 lignes de recentSabqi, empêchant le nettoyage des anciens blocs Acquis. | Commit b3ff49f9 : soustraction des **indices physiques effectivement crédités**, découpage résiduel sans perte de date ni de reviewStreak. Trois tests JVM complémentaires. |
| P1 | 29 sous-blocs historiques peuvent déborder le plafond moteur de 30 pages finales. | Commit b3ff49f9 : trente pages exactes maximum, surplus encore non Acquis conservé dans l'historique. |
| P1 | Une finale partielle pouvait resservir des lignes déjà Acquises lorsque les unités anciennes chevauchaient des pages. | Commit b3ff49f9 : exclusion des lignes déjà Acquises de la planification des nouvelles finales. |
| P0 | Une répétition masquée ne doit pas être comptée tant que le rendu exact n'est pas prêt. | Commits 0017c4d0 et 3242583f : mêmes amorces **Ayah Themes que Lecture**, 100 % masque atomique préparé, attente du callback QCF pageShown, échec de validation si géométrie indisponible. |
| P1 | Les tests source conservaient des attentes de l'ancienne finale sans masque et de la fenêtre de 8 semaines. | Commits 8cbf98aa et 05a7168b : attentes conformes au nouveau protocole et aux 4 semaines sans retirer les invariants historiques. |

Le commit 2f1bf367 ajoute des **tests Android instrumentés spécifiques** :
coexistence des séances ordinaires/finales Sabqi et Itqan, restauration datée,
migration d'une finale SNOWBALL_EXTENDED déjà ouverte et conservation de ses répétitions.

## 3. Matrice Hifz

| Parcours | Format normal protégé | Finale/contrôle | Verdict |
| --- | --- | --- | --- |
| Sabqi | 5 lignes par séance ; ×37 progressif | Dimanche : portion physique réellement apprise ×3, 1 visible + 2 masquées ; crédit Appris → Acquis seulement sur justificatifs | Correctif source intégré ; Android physique à vérifier |
| Itqan avant An-Nas | ×40 sur sous-blocs réels, volumes configurables, sans hypothèse fixe de quatre pages | Dimanche : seules lignes Stabilisé avec justificatif de Consolidation ×7/×10, 1 visible + 2 masquées | Correctif source intégré ; test partiel réel encore requis |
| Itqan après An-Nas | 1 hizb par séance, ×3 (2 lectures visibles + 1 rappel masqué ancré) | Pas de deuxième crédit d'acquisition sur les lignes déjà Acquises | Contrat PASS, sans modifier les séances historiques OPEN ×4 |
| Révision active/passive | Corps acquis, curseurs indépendants et durée accordée | Aucune nouvelle révision passive dimanche | Contrats de non-régression PASS |
| Sessions antérieures | SNOWBALL / SNOWBALL_EXTENDED sauvegardés | Les quotas figés sont restaurés, jamais réécrits par le protocole SUNDAY_PAGE | PASS source, instrumentation renforcée |

**Attention aux écarts non prouvés** : la proposition de rappels S−1 et S−3
espacés en semaine n'est pas certifiée comme un ordonnanceur dédié ; la fenêtre
historique de 4 semaines ne prouve pas, à elle seule, ces deux rendez-vous.
Le plafond discussionnel de 30–40 minutes n'est pas un déclencheur de
validation automatique des finales : `isTimedMode()` ne les inclut pas.
Ne pas annoncer ces fonctionnalités comme livrées sans preuves supplémentaires.

## 4. Audit comparatif des six écrans — photos anciennes / code courant

| Photo | Défaut observé sur BOOX photographiée | Correction déjà présente dans le code | Reste ouvert |
| --- | --- | --- | --- |
| 01 Paramètres | Petits intitulés, sous-lignes absentes | Titres 21sp et sous-lignes serif 18sp sur grande tablette | Lisibilité/touch BOOX physique |
| 02 Plages et corpus | Références coraniques trop fines/petites, champs peu distincts | Taille des bornes agrandie, zones 48dp, pas de troncature intentionnelle | RTL et densité BOOX |
| 03 Atlas Lecture | Frises/titres petits, grand espace autour du Mushaf | Retour volontaire au ratio QCF éprouvé ; sélection Ayah Themes, Jalalayn et ancres figés | **P1 visuel :** lecture effective des frises sur BOOX 10 pouces, sans déformer le Mushaf |
| 04 Progression | Grille 604 pages sans légende visible | Légende des 4 états remontée au-dessus du quadrillage | Perception des contrastes/hamiltons E-Ink sur appareil |
| 05 Parcours Hifz | Titres faibles en regard des six grandes icônes | En-têtes SABQI/ITQĀN/RÉVISION agrandis, planning en dessous, six icônes conservées | Vérifier déroulement sur BOOX |
| 06 Accueil | Petit objectif et zone d'accès rapide déséquilibrée | Grille 2×2 **Parcours/Progression ; Lecture/Atlas**, sans séparateur interne ; grands libellés et icônes figées | Vérifier portrait BOOX sans collision/défilement anormal |

Le visuel d'**émulateur** du run 40189766240, artefact 12597795361,
montre les quatre accès en 2×2 sans trait interne et les six icônes du parcours.
Il ne représente pas le rendu d'un écran E-Ink réel.

## 5. Tests et provenance

- Contrats source/JVM antérieurs : PASS run 40189787550 (HEAD 8c5eb464).
- Instrumentation Android antérieure : PASS **29/29, 0 échec, 0 ignoré**, run 40189766240
  (HEAD 3242583f), avec captures d'émulateur et XML Android.
- Nouveau correctif de progression b3ff49f9 : **PASS source/JVM**
  run 40191493617 ; **PASS staging hors ligne**
  run 40191493631 (artefact 12597962877, uniquement
  `UNFINALIZED-PRIVATE-ASSETS-MISSING-DO-NOT-INSTALL.apk`).
- HEAD 2f1bf367 : **PASS source/JVM** run 40191876023 ; tests Android instrumentés
  étendus lancés dans run 40191876038 (vérifier sa conclusion finale avant
  de considérer la persistance instrumentée comme validée).
- Les captures BOOX initiales ont des SHA-256 conformes au manifeste 6/6,
  mais sont antérieures au code candidat.

## 6. Sûreté, packaging et obligations de release

- Le manifest est hors ligne, `android:usesCleartextTraffic=false`,
  `android:allowBackup=false` et les activités non racines ne sont pas exportées.
- `applicationId` de release = `com.quransafeguard.hifz`, versionCode 43.
- Le debug privé `boox1175debug` a un autre applicationId : **ne remplace pas**
  la 1.17.4 (42) installée.
- Le staging CI n'intègre ni les ressources privées complètes ni le certificat
  historique. La configuration Gradle exige un keystore hors ligne valide et
  les empreintes exactes des ressources privées pour un upgrade officiel.
- Aucune preuve d'installation *in place*, sans suppression ni perte des acquis,
  sur BOOX physique à ce stade.
- Aucune permission de publication officielle n'est accordée.

## 7. Décision

**NO-GO RELEASE OFFICIELLE** — tant que la reprise réelle des sessions
et l'expérience de masquage n'ont pas été testées sur la BOOX et que les
ressources privées et la signature release n'ont pas été validées.

**NO-GO DEBUG PRIVÉ INSTALLABLE IN-PLACE** — seul le staging
hors ligne non finalisé est produit à ce stade.

Étapes de levée :
1. vérifier l'instrumentation Android du HEAD complet, dont les nouveaux
   tests finale/séance ordinaire ;
2. sur BOOX réel, tester finale avec 0 / 1 / 2,5 / 4 pages réellement
   terminées, reprise après redémarrage, masquage exact et absence de ghosting ;
3. vérifier l'Accueil, les frises Atlas, six écrans et conservation des icônes ;
4. assembler **hors ligne** l'APK avec assets privés et signature historiquement
   compatible, puis prouver l'upgrade sans effacement via sauvegarde préalable ;
5. revenir à l'utilisateur pour accord explicite avant release officielle.
