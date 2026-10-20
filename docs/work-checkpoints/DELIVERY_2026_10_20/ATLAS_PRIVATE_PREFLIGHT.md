# Atlas Private114 — vérification du catalogue hors dépôt (20 octobre 2026)

Code vérifié : `346b5523c813e340da335d107228ed3ad8d0538e`.

La campagne d'instrumentation Android du même HEAD a fini SUCCESS : **108/108 tests**, reprise après arrêt forcé et **32 captures**. Ces essais utilisent un corpus synthétique/public; ils ne prouvent pas la présence des véritables intitulés Atlas dans un APK privé.

La comparaison hors ligne des deux fichiers privés, conservés **hors GitHub**, a établi : 1 419 entrées exactes = 366 Unités + 1 053 Passages, 114 sourates, 89 identifiants de plage communs aux deux niveaux, 0 divergence de titre ou statut entre le registre éditorial final et le catalogue Android préparé. Les 46 arbitrages sont inchangés. Empreinte de contrôle des octets source vérifiée. Les huit contrôles locaux de cohérence/anti-altération ont réussi.

**Blocage réel :** les 1 419 entrées ont toutes le consentement éditorial enregistré, mais **0 entrée** possède les quatre gates `user_approved && source_verified && independent_verified && redistributable`. Le code `MawdudiMapData.load` exige 366 titres d'Unités entièrement activés en mode `AYAH_THEMES_FINAL_PRIVATE`; l'APK final privé **ne doit pas être déclaré installable avec le catalogue actuel**, sans preuves complémentaires et activation juridiquement justifiée. Ne pas forcer les gates pour contourner le blocage.

Le matériel privé contient le vérificateur hors ligne, les 1 419 données exactes, huit tests anti-altération et un rapport **sans titres**. Aucun intitulé, manuscrit PDF, APK privé, clé ou mot de passe n'est committé. La présente note est un checkpoint public sans contenu privé.

Suite : examiner preuves de sources/droits sans rouvrir les décisions éditoriales; préparer le catalogue admissible, puis assembler/signer hors GitHub et vérifier la compatibilité BOOX. Debug/tests autorisés; release publique/finale non autorisée sans accord utilisateur.
