# Quran Haafidh — Consolidation privée 20 octobre (sans APK et sans atteinte aux acquis)

## Source exacte et compatibilité Git

Branche unique : `work/haafidh-1171-ibn-kathir-integration-independent`.
HEAD revu : `075a5d98ad863cb8e59411bd640fdfbfb136a64b`.
Dernier SHA du **code applicatif** : `26f7f619c11c855424786e0380463fc33850db5e`.
Vérification GitHub compare des deux SHA : **deux ajouts de documents uniquement**, zéro modification de l'application, des moteurs Hifz, d'Atlas ou de Correspondances.

CI au SHA applicatif : Hifz `39379618174` SUCCESS ; compilation/test variante privée `39379618165` SUCCESS ; Android instrumenté `39379618169` SUCCESS, 108/108 tests Android, arrêt forcé et 32 captures sur émulateur. Les tests utilisent les surfaces publiques/synthétiques et ne valident pas un APK personnel signé.

## Ressources privées reprises des dernières versions (hors GitHub)

Archive utilisateur: `QURAN_HAAFIDH_BOOX_LIVRAISON_PRIVEE_VERIFIEE_2026-10-20.zip` ; SHA-256 `a4fbdfacca9e9070d12fb2baa71960f57d341fb139cb1ab49b64210796edd904`.

- 7 ressources Android privées, empreintes vérifiées, staging exclusif `hifz-app/src/private114/assets/` (chemin ignoré Git).
- Catalogue des 1 419 titres : 366 Unités + 1 053 Passages ; correspondance littérale 1 419/1 419 avec la clôture 19 octobre ; 114 sourates, 89 identifiants communs. SHA-256 du catalogue `065ec0dd68c488efacbd9e77635bf9d531b4cbb2b1bc82db7853ad4da0c02c9c`.
- Rejet de l'ancienne version du fichier `atlas_approved_unit_titles_366_v1.json` provenant d'un APK précédent ; conservation de la version corrigée et épinglée du 18 octobre.
- PDF original, index privé Atlas et trois actifs historiques Ayah Themes vérifiés par hashes ; ancien APK lu uniquement pour les trois actifs et comme référence statique.
- 110 Correspondances et l'addendum récent conservés **comme références éditoriales non activées**, quatre certifications éditoriales historiquement acquises, **zéro approbation finale de production**. Aucune fabrication de droits, références, ancres ou validations QA.
- Sans fichier de keystore ni mot de passe dans l'archive ; PKCS#12 conservé séparément dans la bibliothèque, non exposé.
- Contrôle autonome de SHA et de cardinalités PASS ; vérifications négatives du périmètre Git/écarts de code ; validateur lecture seule du futur APK signé (v2/v3, certificat, package, version, 7 assets et QCF) et validateur lecture seule du binaire installé sur BOOX.
- Le script de staging refuse **tout changement du code depuis le SHA testé** (hors documents de livraison), branche incorrecte, fichier Git suivi, SHA divergent, symlink ou overwrite ; précontrôle tous les fichiers avant copie, rollback des seules créations en cas d'erreur.

## Limite d'exécution et ordre des opérations

Dans le conteneur de cette conversation, pas de SDK Android/Gradle ni d'accès réseau Git ; pas de connexion à la BOOX physique. **Aucun nouvel APK signé, aucune installation et aucun test BOOX sur ce binaire** n'ont donc été effectués ici.

Étapes restant à faire sur machine sécurisée : vérifier le HEAD sans reprise de source ancienne ; installer les assets déjà vérifiés via le script du paquet ; construire debug sous `AYAH_THEMES_PRIVATE_114=1`, signer avec la clé compatible préservée séparément ; contrôler le binaire signé et comparer en lecture seule au binaire effectivement installé sur BOOX ; sauvegarder, installer seulement si compatible, tester physiquement Hifz/Atlas/Lecture/QCF/Jalalayn/Correspondances ; documenter et corriger les défauts ; audit contradictoire de fin ; **release seulement après accord explicite utilisateur**. Aucun refus de build debug.

NO-GO release n'interdit jamais d'avancer sur les travaux réalisables ; ne pas déclarer terminé ce qui demande des signatures, des droits ou le matériel physique.
