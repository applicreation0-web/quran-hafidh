# Quran Haafidh 1.17.2 — CP11, vérifications reproductibles et état de release
_Date : 22 octobre 2026 ; branche `work/haafidh-1171-atlas-only-pre20-mask`._

## Synthèse objective (preuves GitHub)
1. Correctif du contrôle visuel Atlas : `b45267e821dde4cedc615053c0d1e7c2ef214349`. Vérification **stricte** de six sources Atlas/Jalalayn/Mushaf, avec seules dérogations reconstructibles (géométrie vide dans Lecture, callbacks JS par génération), et comparaison de hash de code.
2. CI Hifz et Atlas **PASS pour les deux jobs** sur `b45267e8` : https://github.com/applicreation0-web/quran-hafidh/actions/runs/39803462874 . Le log atteste (i) Hifz gelé, (ii) 23 icônes 13+10 byte-pinned, (iii) absence Correspondances, (iv) rendu Atlas et Jalalayn source pinned, (v) 604 pages QCF vérifiées, (vi) tests JVM et source-contract PASS. Ces contrôles de source ne constituent pas une recette visuelle BOOX.
3. Staging code public/privé **PASS, mais UNIQUEMENT NON FINALISÉ**, sur `b45267e8` : https://github.com/applicreation0-web/quran-hafidh/actions/runs/39803462931 . Artifact `HAAFIDH-BOOX-OFFLINE-UNFINALIZED-b45267e821dde4cedc615053c0d1e7c2ef214349` (GitHub artifact id 12437605162), volontairement sans corpus Atlas/Ayah Themes privé ni signature historique. **NE PAS INSTALLER**.
4. Tests Android instrumentés **3/3 PASS sur émulateur API 35** et JVM PASS au commit `c3b90ad4` : https://github.com/applicreation0-web/quran-hafidh/actions/runs/39795229345 . Les scénarios : rejet de schéma de sauvegarde malformé sans effacement, pré-ready FIFO des commandes WebView, masquage réel SVG/QCF et retour OFF. Le code de rendu/backup correspondant est inchangé entre `c3b90ad4` et `b45267e8` ; les scripts d'audit ont été révisés, non ces algorithmes. Cela n'est pas un test physique BOOX.
5. Candidat versionné : `027a0029418e6aa839b6e337ca1af4330c255de8` met `versionName=1.17.2`, `versionCode=40` sans changer l'application privée, les clés ni le code fonctionnel. Le test d'identité historique figé sur 1.17.1 a été adapté au candidat 1.17.2 dans `3c21fd7a7bdfe8a570bc16e7b853f473db236469`.
6. Exécutions suivantes relancées pour la version 1.17.2 : https://github.com/applicreation0-web/quran-hafidh/actions/runs/39804584621 (intégrité), https://github.com/applicreation0-web/quran-hafidh/actions/runs/39804584883 (émulateur), https://github.com/applicreation0-web/quran-hafidh/actions/runs/39804654963 (staging), avec contrôle de déclenchement correct dans `7bce999acc6cabbe8f5c349eec80172646ebde5c`. **Ne pas écrire PASS tant que GitHub ne confirme pas la conclusion de chaque run.**

## Périmètre des PASS — ne pas les exagérer
- Les contrôles source/pictogrammes attestent les 23 dessins via empreintes Git ; pas la taille, la fluidité ou l'ergonomie effective sur BOOX.
- La CI sous dépôt public n'inclut pas les paquets éditoriaux privés ; elle ne peut donc pas certifier les 1 419 titres réels dans le candidat final.
- Les tests WebView Android sont réels mais sur émulateur ; ils ne mesurent ni ghosting E-Ink ni lenteurs BOOX ni installation mise à jour sur données utilisateur.
- Le staging doit être assemblé HORS dépôt public avec ses ressources privées, signé avec le même certificat que l'application BOOX existante, et contrôlé par empreinte du **binaire final**.

## GO/NO-GO
**NO-GO RELEASE** : aucune publication, aucune APK prétendument officielle, tant que ne sont pas clos : (a) CI du dernier candidat (b) pack privé Atlas réel et 1 419 titres (c) signature historique + même identifiant applicatif (d) migration/restauration in-place et (e) recette BOOX physique.

**Prochaine action concrète :** vérifier le statut des runs du dernier HEAD, corriger tout FAIL, puis utiliser une zone privée et les vrais assets pour l'assemblage final — jamais déposer ces données dans ce dépôt public.
