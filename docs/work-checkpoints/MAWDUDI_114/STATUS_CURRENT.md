# Mawdudi 114 — état courant

CP-00 terminé et reprise isolée vérifiée. CP-01 en cours : les 114 index source sont capturés ; 1 044 notices originales et neuf adaptations de plages manquantes forment un parcours de travail de 1 053 passages, couvrant 6 236 versets sans doublon. Contrôle numérique uniquement : aucune approbation éditoriale globale.

Les douze archives privées du parcours de travail ont été sauvegardées, récupérées et vérifiées par SHA-256. La révision des titres, des introductions et des ouvertures est répartie par sourates ; les cas OCR demeurent explicitement partiels. CP-02 demeure ouvert (texte, notes, édition, droits). Préparation numérique QCF en cours ; miniatures et validation visuelle non exécutées. Aucun nouvel APK.

HEAD précédent confirmé : `7212a149d6d293c495e2857e3a1bdd1a382e4973`. Le commit de ce checkpoint suit sur la même branche. Reprendre la révision éditoriale et l’audit des commentaires, sans recommencer les captures source validées.


## Reprise auditée 2026-10-15 — source complète du statut : CP01_CONTINUATION_AUDIT_2026-10-15.md

SHA avant nouveau checkpoint documentaire : `d7e7022f88d4f2029a6ca158600334e1f0e26679` (vérifié distant). EPUB privé + 12 ZIPs NAV_DRAFT récupérés indépendamment depuis la bibliothèque persistante, SHA originaux contrôlés, 1053 IDs/6236 ayat/114 sourates sans trou ni doublon. L'original anglais reste privé. Rapport individuel privé persistant `MAWDUDI_CP01_CONTINUATION_AUDIT_PRIVATE_2026-10-15.json` (77 140 octets, SHA 785b28a0b580a760f8e56021244bfb431b16195447ae3e1d020398f13d563c83). 10/114 premières ouvertures seulement avec revue manuelle partielle, zéro PASS sémantique final, 59 dossiers de vocabulaire en attente. CP02 commentaires et droits ouverts. Intégration NUMÉRIQUE Android séparée réalisée, non branchée comme interface finale. Les CI de gel/audit et staging du commit d7e7022f sont vertes ; tests Android instrumentés encore en cours. Aucun nouvel APK complet ni essais BOOX.

NE PAS reproduire la collecte source ni recommencer CP00. Reprendre au premier lot NON APPROUVÉ de CP01, puis CP02. Ne pas confondre asset numérique de navigation et produit final. Vérifier la régression 38786649334 avant tout nouveau statut vert.
