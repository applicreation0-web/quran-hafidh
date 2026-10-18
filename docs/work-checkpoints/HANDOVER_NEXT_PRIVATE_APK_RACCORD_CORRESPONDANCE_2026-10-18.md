# Quran Haafidh — préparation APK privée et transmission avant fusion

Date : 2026-10-18. Cette note **n'autorise aucune compilation, signature, fusion ni publication d'APK**.

## Intention utilisateur

Préparer **toute l'application Quran Haafidh** dans son état cible, avec les commits nécessaires, les tests et les preuves de non-régression. L'utilisateur donnera **séparément** l'autorisation de produire la prochaine APK.

En parallèle, l'utilisateur développe une **autre fonctionnalité distincte, « Raccord Correspondance »**, destinée à présenter des **similitudes entre versets** et des **liens entre différentes sourates**. L'utilisateur ouvrira un chat dédié pour **fusionner ensuite** les deux chantiers. **Ne pas intégrer, modifier, présumer achevée ou inventer une implémentation de Raccord Correspondance ici.**

## Sources et restrictions impératives

- Dépôt : `applicreation0-web/quran-hafidh` ; **seule branche de préparation autorisée** : `work/haafidh-1171-ibn-kathir-integration-independent`. Vérifier le HEAD réel avant tout nouveau commit.
- Conserver l'application officielle **1.17.1**, package/certificat installés, données et progression Hifz, moteur Sabqi/Itqān/Consolidation/Révision, état des séances et sauvegardes.
- Conserver le Muṣḥaf QCF Hafs, les sources originales **1 053 Ayah Themes**, **366 unités Atlas**, les **89 passages chevauchants**, Jalalayn intact. Aucun titre privé ou source sous licence ni clé/certificat privé dans ce dépôt **public**, ses logs ou ses artefacts CI.
- Les titres Atlas sont des **synthèses éditoriales**, pas des citations inventées de Maududi/Jalalayn. Les originaux Ayah Themes restent immuables ; toute correction éventuelle est un alias d'affichage sourcé.
- **Pas de faux vert :** « documenté », « candidat », « approuvé », « intégré », « testé » et « vérifié sur BOOX » sont six statuts différents.

## Chantiers internes à livrer AVANT la future fusion

1. **Interface et pictogrammes** — nom `Atlas` (alias legacy `Carte` possible), `Unité Atlas` (gras), `Passage Ayah Themes` (italique), `Ancres` = pictogramme ancre, `Amorces` = `key-round` ; icônes sans libellés sous le bouton. Contrat de référence : `docs/ATLAS_APP_APPELLATIONS_ICONS_2026-10-18.md` et `docs/FROZEN_UI_ICONS_13_PLUS_10.tsv`.
2. **Audit éditorial privé exhaustif** — justifier les 366 titres d'unités et 1 053 passages par versets précis, Ayah Themes et commentaires identifiés. Ne pas confondre indexation automatique et validation. Les 246 propositions n'équivalent pas à des titres approuvés. Titre global court ; nomenclature coranique (Yūsuf, Ibrāhīm, Mūsā, etc.).
3. **Passages partagés** — conserver les 1 053 IDs et limites canoniques ; traiter chaque intersection unité/passage comme une vue, jamais comme un nouveau passage. Les 1 143 rattachements doivent rester complets.
4. **Intégration privée** — produire un asset privé des **titres effectivement validés seulement**, comparer les références d'unités, vérifier les SHA-256 et adapter les tests/chargeurs verrouillés à 120 titres si et seulement si la validation a réellement permis un autre total.
5. **Contrôles applicatifs** — CI au dernier HEAD, contrat des icônes, navigation Atlas / Muṣḥaf / Jalalayn, QCF plein écran/parité/glissement, non-régression des sauvegardes, tests moteurs Hifz et simulations longitudinales 180/365/548 jours avec rapports distinguant PASS/FAIL/NON EXÉCUTÉ.
6. **Transmission** — à la fin, produire la liste complète des commits effectués avec SHA, le dernier HEAD, les décisions éditoriales et références, les pièces privées conservées hors GitHub, les tests avec URLs et les blocages résiduels ; ouvrir un dossier de fusion **sans toucher à Raccord Correspondance**.

## Contrat minimal de future fusion « Raccord Correspondance »

Le besoin fonctionnel communiqué est seulement : **similitudes entre différents versets** et **liens entre sourates**. Aucun corpus, interface, algorithme, source exégétique, dénomination de bouton, schéma ni emplacement n'est encore approuvé dans ce chantier.

Lors du **chat de fusion dédié** :
- inspecter l'implémentation effectivement livrée de Raccord Correspondance et sa provenance ;
- définir l'interface entre les deux fonctions à partir d'identifiants **sourate:verset** fiables, sans refondre ou recouper arbitrairement les titres Ayah Themes ni les frontières Atlas ;
- vérifier les interactions lecture/Atlas/navigation/retour et les ressources mémoire et écran BOOX ;
- conserver les moteurs Hifz et données acquis ;
- faire un audit de l'ensemble **après** la fusion. Ne pas considérer les CI séparées suffisantes pour certifier le résultat fusionné.

## Gate « READY FOR NEXT APK »

Le document de vérité est `docs/work-checkpoints/NEXT_APK_READINESS_GATE_2026-10-18.md`.
Le statut reste **NO-GO** tant que les titres ne sont pas validés individuellement, les assets privés non intégrés et les suites de régression non prouvées.
L'APK, son certificat, sa continuité d'installation et l'acceptation réelle BOOX nécessiteront des contrôles **après construction**. Aucun test d'ancien commit ne prouve une nouvelle application fusionnée.

## Transmission demandée par l'utilisateur

Lorsque la préparation sera réellement terminée : livrer un récapitulatif prêt à copier dans le futur chat de fusion, comprenant
- HEAD et liste des commits de cette branche ; plan sans nouveau dépôt ou branche ;
- inventaire exact des titres intégrés et des titres refusés/non certifiés ;
- paquet privé / empreintes et disponibilité hors GitHub, **sans les publier** ;
- résultats de CI et tests Hifz ; statut de chaque vérification BOOX ;
- instructions d'intégration de Raccord Correspondance dans le chat prévu ;
- procédure de génération d'APK **uniquement après autorisation explicite de l'utilisateur**.
