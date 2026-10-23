# Haafidh 1.17.5 — debug privé accueil BOOX (23 octobre 2026)

**GO pour essai privé ; NO-GO release officielle.** Build de test isolé, sans modification des moteurs, sans audio nouveau ni Correspondances.

- Commit de code source / build : `d0103cd15aa2423a39814eff543d7a3dd3a80dba`.
- Test JVM + contrats source : PASS https://github.com/applicreation0-web/quran-hafidh/actions/runs/40020374798
- Compilation et export de staging **sans actifs privés** : PASS https://github.com/applicreation0-web/quran-hafidh/actions/runs/40020374881
- Android instrumenté : **26/26 PASS**, commit `3805ee2ea67e60bd939f3c6889503dc319cf800d`, https://github.com/applicreation0-web/quran-hafidh/actions/runs/40019706269
- APK local livré dans ChatGPT seulement : `QURAN_HAAFIDH_BOOX_1.17.5_ACCUEIL_ATLAS_DEBUG_PRIVE.apk`.
- SHA-256 APK : `b1231a89f1b20c2fac93af7b2de113071fb51d38cf9bf3d66a22be666c36fc52`.
- Taille : 91 891 791 octets.
- Android package : `com.quransafeguard.hifz.boox1175debug` ; version : `1.17.5-BOOX-HOME-PRIVATE-DEBUG` ; versionCode 42.
- Signature locale v2 PASS, zipalign PASS. Signer **de test**, différent du signataire officiel. **Aucune mise à jour in-place** : application installable séparément, sans accès aux acquis officiels.
- Exactement sept ressources privées Atlas de la bibliothèque personnelle : 7/7 SHA-256 identiques aux valeurs attendues ; 604 pages QCF (SVG-Brotli) incluses, **aucune** Correspondance.
- Aucun fichier privé ni APK complet publié sur GitHub.
- Validation physique BOOX, ghosting, exactitude des tailles et interaction écran E-Ink : **non testées**.
- Chantiers CP2–CP6 non terminés ; cette édition sert à tester le nouvel accueil et les accès, sans revendiquer l'achèvement de la 1.17.5.

Détail et preuves additionnelles dans le rapport livré directement avec l'APK dans la conversation.
