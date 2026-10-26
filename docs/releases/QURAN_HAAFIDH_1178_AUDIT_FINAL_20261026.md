# Quran Haafidh 1.17.8 — procès-verbal d'audit et porte de publication (26 octobre 2026)

**Nature :** archivage durable des résultats disponibles, reconstitué à partir du bilan Work transmis par capture et vérifications GitHub. **Ce fichier n'est pas présenté comme une copie octet pour octet du rapport Work temporaire**, inaccessible après l'échec d'enregistrement Library.

## Identité et preuves

- Dépôt : `applicreation0-web/quran-hafidh`.
- Branche auditée : `fix/haafidh-1178-family-postfinal-20261026`.
- Commit de code effectivement testé : `1fe5d470d1ad4dacee5fbeddce3edc8669b99ddd`.
- CI : [run 40395410484](https://github.com/applicreation0-web/quran-hafidh/actions/runs/40395410484), workflow `haafidh-1178-family-credit-audit.yml`, résultat `completed/success` (2026-10-26 20:32:34 UTC).
- Artefact technique : `haafidh-boox-1178-family-android-audit` (id 12685333794), conservation annoncée jusqu'au 2026-11-09 20:32:27 UTC ; le résultat du run et le présent rapport perdurent après expiration.
- L'artefact d'audit n'est PAS une APK privée, complète et signée.

## Résultats annoncés par le bilan Work

- 917 tests JVM / contrats source et 52 tests Android : tous annoncés PASS.
- Quatre phases de mort du processus : annoncées PASS.
- Sauvegarde utilisateur réelle rejouée **sur copie isolée** : acquis et finales `OPEN` annoncés préservés.
- Portrait Android et captures contrôlés en émulateur, **pas sur BOOX physique**.
- Deux corrections : refus de crédit familial avec preuve partielle ; tri physique des identifiants appris lors de la lecture de révision passive sans réécrire la progression (voir `HAAFIDH_1178_PASSIVE_READ_EXCEPTION_20261026.md`).
- Ces chiffres proviennent du bilan Work, la réussite globale du job étant vérifiée séparément sur GitHub. Ils ne constituent pas une nouvelle exécution de 969 tests par l'auteur de cette note.

## Limites et porte de publication

**GO technique pour le SHA audité ; NO-GO release officielle tant que ces portes ne sont pas franchies :**

1. Version candidate `1.17.8`, `versionCode > 45` : source, APK et manifeste cohérents, nouveau SHA CI audité.
2. Intégration des sept ressources privées Atlas exactes **hors dépôt et CI publics** ; vérification SHA-256 avant et après création du binaire.
3. Signature APK v2 avec certificat historique `66adc4f0dcf70d716a8050fd4b0195266307576aa2f9bd54c9cbf2851a330e14` et contrôle `apksigner`, package `com.quransafeguard.hifz`.
4. Sauvegarde préalable et installation sur place par-dessus le même paquet/signataire existant, **sans désinstallation**.
5. Test sur BOOX physique : finale Sabqi / Itqān OPEN, révision passive, calendrier, lecture QCF 604 pages, masquage, Atlas et rendu E-Ink.
6. Comparaison acquis, séances et crédits avant / après installation et relance du processus, aucune validation/rattrapage inventé.

**Ne jamais publier un artefact « private-assets-missing »/unsigned.** Il sert exclusivement de base d'assemblage hors ligne ; pas d'APK officiel sur les GitHub Releases publiques.

## Décision au 26 octobre 2026

CI source : PASS. Signature de la future 1.17.8 : NON VÉRIFIÉE. BOOX physique : NON EXÉCUTÉ. Publication : BLOQUÉE par ces deux contrôles, pas par le quota Library.

La note permet une reprise déterministe sans réaudit historique et sans divulgation de données privées.

## Candidate privée signée — preuves finales

- **Commit applicatif exact de la compilation Release** : `45232281bffbea66a54489162fde11d2c28e6997`.
- **Audit JVM et Android du même code applicatif** : [run 40405581372](https://github.com/applicreation0-web/quran-hafidh/actions/runs/40405581372), `SUCCESS`; la comparaison Git avec le commit de compilation ne relève que `.github/workflows/haafidh-1178-private-unsigned-staging.yml`, sans changement de source.
- **Staging Release code-only** : [run 40405791411](https://github.com/applicreation0-web/quran-hafidh/actions/runs/40405791411), `SUCCESS`, version `1.17.8`, code `46`, paquet `com.quransafeguard.hifz`.
- **Sept fichiers Atlas privés** : injectés hors dépôt public, 7 empreintes SHA-256 conformes au pack historique ; 723 entrées de l'APK public inchangées octet pour octet, sept ajouts privés, zéro suppression.
- **Mushaf QCF** : 604 pages incluses ; CRC du ZIP et alignement : PASS.
- **Signature** : [run 40407121809](https://github.com/applicreation0-web/quran-hafidh/actions/runs/40407121809) `SUCCESS` pour les seules données `signedData` de 1 449 octets, contraintes par SHA-256 `d4e0b680f5020deda33f495f42010057f86ed8a48a6b656641e84d205f48ed31`. Aucun APK, PDF, corpus privé ni sauvegarde personnelle n'a transité par GitHub.
- **Signature vérifiée indépendamment sur APK final** : `apksigner verify --verbose --print-certs --min-sdk-version 26` = PASS, schéma Android v2 true, certificat SHA-256 `66adc4f0dcf70d716a8050fd4b0195266307576aa2f9bd54c9cbf2851a330e14`, un seul signataire RSA 4096 bits.
- **Candidate privée** : `QURAN_HAAFIDH_BOOX_1.17.8_CANDIDATE_PRIVEE_SIGNEE.apk`, **93 172 823 octets**, SHA-256 `47858ac41a769d3e698610d87205719f86c2e91319555f23c9c40ed10428ebba`.
- **Diffusion** : candidat uniquement via environnement privé de conversation ; ne pas publier cet APK dans le dépôt GitHub public.

### Feu vert

La candidate est techniquement signée et vérifiée **mais n'a pas été installée sur BOOX**. Sa compatibilité physique, la conservation effective des acquis après update-in-place, la lisibilité E-Ink, les finales OPEN et la reprise après arrêt forcé sur le BOOX doivent encore être contrôlées **sur l'appareil**. La release officielle publique est donc **NO-GO**, malgré la préparation de la candidate privée.

**Ne pas désinstaller la version existante.** Exporter et vérifier la sauvegarde avant la première installation ; arrêter si la signature ou l'application effectivement installée diffère. Aucun crédit artificiel ni changement d'initialisation n'est autorisé.
