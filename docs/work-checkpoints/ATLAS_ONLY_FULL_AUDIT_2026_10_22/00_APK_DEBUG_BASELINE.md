# Quran Haafidh — baseline du dernier debug signé (22 octobre 2026)

## Identification vérifiée sur GitHub
- Dépôt : `applicreation0-web/quran-hafidh` (PUBLIC).
- Branche applicative : `work/haafidh-1171-atlas-only-pre20-mask`.
- Commit source de référence : `040af230d35f148ac323155a8ae6868e89094aef` (HEAD vérifié le 22/10/2026 avant l'ajout de cette documentation).
- Workflow du dépôt : `.github/workflows/haafidh-boox-private-debug-staging.yml`.
- Le workflow exporte `UNFINALIZED-PRIVATE-ASSETS-MISSING-DO-NOT-INSTALL.apk` : il manque les actifs privés, il n'est PAS le debug signé utilisable.

## Identification déclarée dans le manifeste privé retrouvé
- Fichier annoncé : `QURAN_HAAFIDH_BOOX_ATLAS_FRISE_PASSAGES_GOMMAGE_CORRIGE_DEBUG_20261022.apk`
- Taille : 91 887 695 octets.
- SHA-256 déclaré : `a14e9892afa36b8a996246024332562cb903e5af634f65e2c03d933edc1e01cd`.
- Certificat SHA-256 déclaré : `85cd816f5213ae81f450146789c13ec476df54d7be8cc5f806decc9ed3f7061e`.
- Identifiant Android attendu : `com.quransafeguard.hifz.ayahthemes114private`.
- Attendus : 604 folios QCF, 7 ressources Atlas privées ; **Correspondances absentes volontairement**.
- Statut déclaré : APK signé contrôlé ; test BOOX physique **non effectué**.

## Limites de preuve
- Ce manifeste public est un inventaire de métadonnées, NON une vérification cryptographique indépendante de l'APK : le fichier binaire signé n'était pas accessible dans la session d'audit documentaire.
- Un fichier de rapport JSON préexistant indique le SHA ci-dessus ; une vérification indépendante exige de recalculer le SHA sur les octets réels, puis inspecter le manifest, les DEX, assets, la signature et la provenance.
- Ne JAMAIS substituer l'APK de staging au debug signé.
- Ne PAS envoyer sur ce dépôt PUBLIC l'APK intégrant les actifs privés, la clé de signature, les mots de passe ou les corpus.
- Aucun prétendu PASS de rendu BOOX physique sans capture ou contrôle sur l'appareil.
- La documentation ajoutée après le commit applicatif ne change pas sa provenance.

## Relais de contrôle
Voir `01_PLAN_AUDIT_INTEGRAL.md` pour l'ordre des tests, les critères bloquants, l'audit contradictoire et la recette BOOX.
