# Quran Haafidh 1.17.1 — audit contradictoire / Amorces locales / sept jours
Date : 2026-10-11. **DEBUG uniquement : aucune release et aucune validation BOOX physique annoncée.**

## Provenance et limites
- Référence immuable : `7fd45bbe3d2e67a2e21c6f8ca1d5ae935d1f0d21`.
- Branche unique : `work/haafidh-1171-ibn-kathir-integration-independent`.
- Le SHA final est celui du fichier BUILD_PROVENANCE.txt dans l'archive GitHub ; tous les tests doivent porter ce même SHA.
- Deux CI indépendantes sont exigées avant création : `Ibn Kathir Hifz + Amorces audit ONLY - no APK` et `Hifz + Amorces + sept jours Android regression ONLY - no APK`. Vérifier la conclusion GitHub, pas la présence d'un script.
- L'appareil BOOX physique, les sauvegardes réelles et le ressenti e-ink ne sont **pas** certifiés par les CI.

## Changements validables
- Retrait du Quiz : aucune activité, aucun corpus, aucun historique ni permission micro Quiz dans l'APK. Les données Hifz acquises ne sont pas réécrites par cette suppression. L'icône historique demeure uniquement dans le contrat des 23 vecteurs, non accessible depuis l'application.
- Retrait du navigateur de passages, de la Carte à listes et des liens externes en Lecture. Les Amorces sont des repères QCF sur les mots du Mushaf. Un toucher ouvre une fenêtre anglaise locale, sans appel réseau.
- Le corpus documentaire de 1 903 groupes couvre les 114 sourates. **Seuls 16 groupes** (8 Qāf, 8 An-Najm) ont des intertitres anglais originaux localement attestés. Pour les 1 887 autres, aucun titre n'est inventé. Un début documentaire mécanique ne vaut **pas** une amorce mnémotechnique éditorialement approuvée : approbations = 0. La reprise du corpus complet requiert revue documentaire et vérification du droit de reproduction.
- Programme à sept jours : Recommandé 3 Sabqi + 4 Itqān, dont dimanche Itqān et finales sans révision active/passive. Le réglage Manuel sept jours répartit n Sabqi et 7−n Itqān et conserve le dimanche Itqān. L'ancien Classique n'est pas proposé mais reste interprétable pour les dates historiques. Pour une installation existante encore Classique, la migration est datée au prochain lundi ; aucun crédit V6 n'est supprimé.
- Affichage Itqān : voile sur les pages hors bloc sans changer le moteur de masque, de répétitions ou de validation. Reader QCF identique aux octets officiels sauf cette transformation visuelle reconstruite et vérifiée.
- Prévision : objectif Itqān Recommandé = 30 lignes/semaine, Manuel = estimation conservatrice d'après ses jours Itqān ; plage Sabqi zéro jour protège contre division par zéro. Ce ne sont pas des crédits Hifz ni des engagements sur les frontières de pages/sourates.

## Audit critique des moteurs, sauvegarde et révisions
- `hifz-core`, rendu `MushafView.java`, géométrie QCF 604 pages, 77 432 mots, gestion des lignes et migrations V6 restent sous contrôle de comparaison SHA ou de deltas strictement bornés.
- `WordGeometryRepository.java` ne diffère de 1.17.1 que par le retrait de l'interface Quiz et de son annotation @Override. Son comptage de mots et les boîtes QCF restent inchangés (script de reconstruction byte-exact).
- Les invariants Sabqi, Itqān ×40 et étapes 20+5+5+5+5, supplément +1/+2 limité aux frontières physiques et sans fusion de sourates, Consolidation nouveau bloc ×7 et précédent continu ×1, Renforcement ×10, finales dimanche ×3, snapshot OPEN daté et non-revalidation cumulée sont couverts par les JVM et contrats contradictoires existants.
- La révision passive utilise seulement des versets entièrement crédités Appris/Acquis, la révision active reste indépendante, aucun curseur révision ne doit avancer le dimanche. La progression V6 ne peut ni sauter d'étape ni créditer deux fois une séance identique.
- Les contrôles couvrent également les icônes et les ouvertures de huit modes Hifz. Les tests de fonctionnement sur vraie progression BOOX restent à vérifier après installation de ce debug distinct.

## Portes d'acceptation
1. Test JVM de `hifz-core` + tests unitaires application + sourceContractTest complets verts.
2. Six tests Android instrumentés verts et au moins 14 captures non vides ; écrans de Lecture hors listes, Amorce Qāf locale, accueil sans Quiz, calendrier sept jours, huit modes Hifz.
3. CI de compilation final sur **le même SHA que ces deux CI**, contrôles signature, version, package indépendant et SHA256 de l'APK.
4. Si l'une de ces portes échoue : **pas de nouvel APK publiable**, et aucun recours à l'ancien AUDIT-CRITIQUE comme preuve de la nouvelle version.
