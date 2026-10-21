# Quran Haafidh — Debug BOOX privé et procédure durable

**Date :** 21 octobre 2026. **Branche code :** `work/haafidh-1171-atlas-correspondances`. La procédure automatisée est conservée dans `scripts/finalize_private_boox_debug.py` sur GitHub. **Compilation CI validée :** `ba13de8e337be82f582480ed9a5d334c4928ad45` (run 39554668546). **Dernier checkpoint documentaire :** `ced63b88c2036f74ddcbe9b02d66e457241b33fd`.

## 1. Debug livré

- APK : `QURAN_HAAFIDH_BOOX_ATLAS_CORRESPONDANCES_DEBUG_PRIVE_2026-10-21.apk`
- SHA-256 : `e94add5e54f740e79215212817de87319d7d65d3cfd6ab28023272a37b602e5b`
- Taille : 91 941 567 octets.
- Paquet Android : `com.quransafeguard.hifz.ayahthemes114private`.
- Version code : 39 (`1.17.1-AyahThemes114-PRIVATE`).
- Empreinte SHA-256 certificat : `85cd816f5213ae81f450146789c13ec476df54d7be8cc5f806decc9ed3f7061e`.
- Signature Android v2 et v3 valides, ZIP aligné 16 KiB.
- 604 SVG KFQC, 366 unités + 1 053 passages Atlas et 110 dossiers historiques ; quatre relations certifiées/actives R030, R037, R047 et R048.
- 9/9 empreintes SHA ressources privées exactes, 704 composants de l'APK provenant de la compilation GitHub inchangés.

**Limite :** Ces preuves garantissent l'intégrité et la signature du fichier produit, **pas** le résultat d'une installation/test physique BOOX. La présence des 110 dossiers n'équivaut pas à 110 relations activées.

## 2. Pour essayer sur la BOOX en conservant les données

1. **Ne pas désinstaller** Quran Haafidh et ne pas effacer ses données.
2. Vérifier quel paquet est actuellement installé. Si c'est `com.quransafeguard.hifz.ayahthemes114private` avec le certificat ci-dessus, le debug peut faire l'objet d'une **mise à jour sur place** (code 39 sans downgrade). Si le paquet installé est `com.quransafeguard.hifz` ou porte un autre certificat, **STOP**, ce n'est pas la même application ; ne pas supposer que les données seront reprises.
3. Effectuer une sauvegarde des données Hifz depuis l'application actuelle, puis contrôler que cette sauvegarde est lisible et stockée hors de la BOOX.
4. Mettre à jour **sans désinstaller**. Avec ADB et seulement après comparaison du certificat et du paquet : `adb install -r <chemin-vers-apk>`.
5. Vérifier : accueil → Atlas (carte Unités complète, feuilletage QCF, titres approuvés), Ancre (nouvelle icône), Amorces (key-round ancien), jalons du Hifz, Lecture et Jalalayn, Correspondances (110 dossiers historiques clairement distincts des quatre certifiés), sélection QCF, retour et restauration après arrêt forcé ; lisibilité et scintillement E-Ink.
6. En cas de différence de progression, fermeture brutale ou perte du Mushaf, **arrêter et conserver le fichier APK actuel et la sauvegarde**. Ne pas essayer de « réparer » par une désinstallation.

## 3. Procédure durable, sans GitHub privé ni secrets

Pour **chaque nouvelle version du code** :

1. Vérifier le HEAD exact de GitHub et les tests de non-régression Atlas/Hifz/Correspondances. Utiliser l'artefact GitHub `HAAFIDH-BOOX-OFFLINE-UNFINALIZED-<sha>` produit depuis ce HEAD ; ne jamais le transmettre ou installer comme version finale.
2. Conserver localement le pack `QURAN_HAAFIDH_BOOX_PACK_PRIVE_9_RESSOURCES_2026-10-21.zip` (**aucune clé ni mot de passe dedans**). Son SHA-256 attendu est `ec21168411f95d66ef336a5091050c86e482ccb62a51cb44436a9f6f6d5bf870`. Il réunit les sept actifs privés Atlas et les deux catalogues privés Correspondances dans des chemins Android canoniques.
3. Exécuter en local `finalize_private_boox_debug.py` avec Python 3, Java, `keytool`, un PKCS#12 personnel et son fichier de mot de passe (hors dépôt), l'artefact CI ZIP et le SHA source intégral. Le script refuse tout SHA source ancien, toute mauvaise signature, tout pack modifié, tout changement de package ou tout downgrade.
4. Vérifier le rapport JSON produit et le certificat de l'app effectivement installée ; passer ensuite aux tests Android instrumentés, au contrôle BOOX physique, à la sauvegarde et au retour arrière.
5. Toute révision éditoriale ou ajout de relations doit générer **une nouvelle version auditée** du pack et un changement explicite des empreintes attendues dans le script, avec provenance vérifiable. Ne pas bricoler les drapeaux de certification, ni ajouter de titres non approuvés.

### Exemple de commande pour le prochain build

```bash
python3 finalize_private_boox_debug.py \
  --staging-zip 'HAAFIDH-BOOX-OFFLINE-UNFINALIZED-<sha>.zip' \
  --private-pack QURAN_HAAFIDH_BOOX_PACK_PRIVE_9_RESSOURCES_2026-10-21.zip \
  --keystore /chemin/prive/Haafidh-Private114-Debug-Signing-Key.p12 \
  --password-file /chemin/prive/PRIVATE_SIGNING_KEY_PASSWORD.txt \
  --previous-apk /chemin/prive/ancien-debug-compatible.apk \
  --expected-code-sha <sha-complet-de-la-CI> \
  --output Quran-Haafidh-BOOX-private-next.apk
```

Le mot de passe ne doit pas apparaître dans la ligne de commande, les journaux ou un commit. Le mot de passe et le PKCS#12 **ne sont pas inclus** dans ce livrable durable.

## 4. Ce qui reste à certifier pour la release

Le mode Atlas de prévisualisation privée montre les titres éditorialement approuvés sans prétendre que tous les droits et contre-contrôles sont acquis. Correspondances active seulement les quatre relations certifiées : les 106 autres dossiers historiques ne peuvent être promus sans preuve complète des bornes, classifications, droits et contrôles d'affichage. Les tests physiques BOOX et une validation utilisateur restent indispensables avant une release finale. Aucun déploiement public n'est autorisé.