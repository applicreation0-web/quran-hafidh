# Consolidation privée des ressources — Atlas / Correspondances — 2026-10-20

## Base
Branche unique : `work/haafidh-1171-ibn-kathir-integration-independent`. Code de référence : `f61d067cb42aa37ded73db72191e0898a94e36a2`.
Ne **jamais** remplacer cette base par le code extrait d'un ancien APK.

## Ressources privées retrouvées et validées *hors GitHub*

Sept actifs privés ont été assemblés dans un overlay autonome, à installer uniquement sous `hifz-app/src/private114/assets/`, chemin déjà ignoré par Git. **Aucun contenu privé, texte Atlas, PDF, fichier de clé, mot de passe ni APK privé n'est envoyé sur GitHub ou CI publique**.

- Catalogue Atlas privé : 1 419 titres (366 Unités, 1 053 Passages), digest `065ec0dd68c488efacbd9e77635bf9d531b4cbb2b1bc82db7853ad4da0c02c9c`. Comparaison exacte avec `ATLAS_CLOTURE_EDITORIALE_2026-10-19.zip` : **zéro divergence**. Toutes les décisions éditoriales préservées ; indicateurs bibliographiques/de droits inchangés.
- Index Atlas 366 : `8f514ffa367d67a49198799be279691ba0555363fac0b030f09fbf39fe3c3484`.
- Titres Atlas hérités corrigés : `421277c59519f2b904de23bbc1e0eee6faeb569e33ed6b425b4b9235cf82ff79`, repris de l'archive plus récente du 18 octobre. **Attention** : un ancien APK contient un fichier du même nom avec SHA différent ; ne pas réinjecter cette ancienne version.
- PDF original privé Maududi : `1d207bb3dd58d45e7a77b0b5942f88a5c216b3a4273ea5a4474fc9e140b0d62e`.
- Ayah Themes titre/macro/special historiques épinglés par le code : `a42914ae968c77bc7224f5126a41cc3e92099fbc797a91b01d74405d242eb209`, `3589af556f82e55e1619fc0f3ae16b7f21b4609f156a8ad808a3a0bbe6d3c09a`, `d3fb2a7cc23f84851f83caa14ab23a549b8f410529930131dd2c2560d7450404`.
- Ancien binaire BOOX privé uniquement comme référence de compatibilité : APK `fe8a5169`, contient 604 pages QCF compressées et certificat de signature conforme à `85cd816f5213ae81f450146789c13ec476df54d7be8cc5f806decc9ed3f7061e`.
- Archives historiques PKCS#12 récupérées dans la Bibliothèque ; identité signataire PKCS#12 **non vérifiée à nouveau** sans mot de passe. Ce fait ne vaut pas signature d'un APK actuel.

## Vérifications de non-régression et périmètre

11 contrôles locaux d'intégrité/structure du pack réussis ; 7/7 SHA corrects, 1 419 titres appariés par niveau+ID, 0 texte divergent, flags de certification et de droits inchangés, aucune clé incluse. Ces tests **ne sont ni une compilation ni une recette physique BOOX**.

Les trois CI du commit `26f7f619` avaient terminé SUCCESS, dont 108/108 tests Android sur émulateur public/synthétique. Un nouveau binaire privé est encore à construire/signifier hors service public et devra être testé distinctement.

Correspondances : conserver les 110 dossiers, leurs verdicts et leurs preuves. **Aucune activation automatique des 77 candidats** ; les 27 en attente de preuve et les 6 écartés restent préservés. Pin catalogue de production vide tant que les contrôles individuels requis ne sont pas établis. Pas de réécriture des moteurs Hifz, du corpus QCF/Jalalayn ni des titres définitifs.

## Étapes restant à effectuer

1. Récupérer ce **nouvel overlay privé** sans l'uploader au dépôt public.
2. Dans environnement local avec Git/Android SDK/JDK17/Gradle, copier sous `src/private114/assets/` avec vérification SHA et Git ignore ; ajouter le manifeste Android privé approprié.
3. Compiler l'APK debug du HEAD correspondant, signer hors dépôt avec la clé compatible puis vérifier certificat, package, versionCode, zipalign, v2/v3, 604 pages et catalogue 1 419 exact.
4. Inspecter d'abord en lecture seule le certificat, le package et la version réellement installés sur BOOX, puis sauvegarde et recette physique de la mise à jour, sans désinstaller ni altérer les acquis.
5. Terminer les certifications Correspondances, corriger anomalies et audits. Aucune release finale sans accord.

Le NO-GO release n'empêche pas les corrections et tests encore possibles.
