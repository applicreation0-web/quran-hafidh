# Private114 — reprise sans Work, preuves du 20 octobre 2026

## Base et résultats réellement vérifiés

HEAD de référence `26f7f619c11c855424786e0380463fc33850db5e` sur la branche d'intégration. Trois workflows de CE SHA :

- 39379618174 — Frozen Hifz engines — **SUCCESS**.
- 39379618165 — compilation et tests `AYAH_THEMES_PRIVATE_114=1` — **SUCCESS**, sans génération d'APK.
- 39379618169 — instrumentation Android **SUCCESS**, `108/108 PASS`, force-stop réel et 32 captures sur émulateur. **Données publiques/synthétiques uniquement**.

La voie d'affichage des titres approuvés est conditionnée par `BuildConfig.ATLAS_PRIVATE_EDITORIAL_PREVIEW`, vraie seulement dans la variante debug privée. Les quatre indicateurs bibliographiques/de droits restent tels qu'ils sont ; leur activation n'est ni inventée ni requise pour montrer les titres approuvés en mode privé. Les builds ordinaires et les règles de release ne sont pas contournés.

## Préparation privée hors dépôt

Paquet utilisateur autonome : `QURAN_HAAFIDH_PRIVATE114_PREPARATION_SECURISEE_2026-10-20.zip`, SHA-256 `b8bacda81ae429b3f59412a620a3ec708ab562df0098556666970ccab214987a`; **ne pas uploader ce ZIP sur GitHub**, il contient les 1 419 titres. Le paquet fournit :
- Les 1 419 titres avec SHA-256 exact attendu `065ec0dd68c488efacbd9e77635bf9d531b4cbb2b1bc82db7853ad4da0c02c9c` ;
- Un programme de staging exclusivement dans `hifz-app/src/private114/assets/atlas/`, chemin ignoré par Git ;
- Un vérificateur **lecture seule** d'APK signé (certificat, package, versionCode, 604 SVG QCF et 1 419 titres réellement approuvés, sans fausse certification documentaire) ;
- Un contrôle **lecture seule** de l'application déjà installée sur la BOOX, exigeant le même applicationId/certificat ;
- Neuf tests locaux PASS (pas de SDK ni keystore nécessaire).

## Ressources encore nécessaires pour construire localement

Assets privés historiques `atlas_private/atlas_366_comments_offline_reference_v1.json` (SHA `8f514ffa...`), `atlas_private/atlas_approved_unit_titles_366_v1.json` (SHA `421277c5...`), `atlas_private/maududi_original.pdf` (SHA `1d207bb3...`), et ressources associées éventuellement requises par les builds précédents. Clé/certificat BOOX conservés hors dépôt. Cet environnement **n'a ni ces ressources complètes, ni le SDK Android et la clé** : aucun APK compatible signé n'a été produit.

Le certificat de référence Private114 SHA-256 est `85cd816f5213ae81f450146789c13ec476df54d7be8cc5f806decc9ed3f7061e`. `applicationId` `com.quransafeguard.hifz.ayahthemes114private`, versionCode connu 39. La vraie installation BOOX doit être lue avant toute tentative de mise à jour. Ne jamais désinstaller ni supposer la progression migrée d'un autre package.

## Correspondances

110 dossiers historisés ; 77 retenus documentés, 27 en attente de preuve et 6 écartés. **0 activée en production.** Ne pas transformer les 77 en relations certifiées. Autoriser des essais d'interface synthétiques, puis intégrer et tester uniquement le sous-ensemble avec sources, droits, ancres QCF et QA documentés. Cela avance en parallèle du debug Atlas.

## Interdictions maintenues

Aucune clé, PDF, fichier personnel, APK signé privé ni titres dans GitHub/CI publics. Aucun faux `source_verified`, `independent_verified`, `redistributable`. Aucun APK final public ni release sans nouvelle validation explicite de l'utilisateur. NO-GO release n'arrête pas les corrections restantes.

## Priorité suivante

1. Compléter hors dépôt les assets privés avec SHA exact, compiler `assembleDebug`, signer localement le debug avec le certificat connu.
2. Vérifier statiquement le binaire **signé**, puis inspecter en lecture seule le certificat/package du binaire installé sur BOOX.
3. Sauvegarder les données, tester la mise à jour compatible et la navigation QCF/Atlas/Hifz sur BOOX.
4. En parallèle, terminer les validations individuelles Correspondances admissibles ; garder les autres désactivées.
5. Audit contradictoire et corrections finales, puis release uniquement après accord.
