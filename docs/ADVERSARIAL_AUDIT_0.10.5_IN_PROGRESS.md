# Quran Safeguard 0.10.5 — audit contradictoire général (EN COURS)

Date: 2026-09-05

## Verdict actuel

**ROUGE — aucune publication / aucune promotion en release tant que les bloqueurs ci-dessous ne sont pas levés sur une branche d’intégration commune.**

Cet audit est volontairement séparé des branches de correction et de corpus. Il ne modifie ni le texte coranique ni les corpus classiques.

## 1. Mushaf de Médine — complétude 604/604

### Signal utilisateur

Page 552 signalée absente (deuxième page de sourate aṣ-Ṣaff).

### Constat

- La cartographie interne inclut bien la page 552.
- Le lecteur demande bien le fichier `552.svg.br`.
- La source Hafs/KFQC épinglée est `quranpedia/quran-svg` au commit `1b427fab77aae1403fe7e1f0b8c794a5384d5605`.
- Le contrôle historique vérifiait 604 noms de fichiers mais seulement trois blobs sentinelles (001/302/604). Il ne démontrait donc pas la validité Brotli/SVG des 601 autres pages.

### Correction isolée

PR #48 — branche `fix/0.10.5-mushaf-completeness`:

- exige exactement 604 pages canoniques dans l’arbre amont ;
- compare **chaque page 001..604** byte-for-byte avec son Git blob au commit KFQC épinglé ;
- ajoute un test qui décompresse réellement les 604 Brotli SVG ;
- exige un SVG exploitable avec métadonnées de versets ;
- test ciblé : page 552 présente, décodable et contenant la sourate 61.

### Statut de preuve

Le workflow GitHub Actions de la PR #48 a échoué deux fois avant toute étape : aucun runner n’a été attribué et aucun log de compilation/test n’a été produit. Ce n’est pas une preuve d’échec du correctif, mais **ce n’est pas non plus une preuve de réussite**.

**Gate:** ne pas fusionner #48 tant qu’un vrai run n’a pas exécuté et validé les contrôles.

## 2. Tafsīr Plus — Jalalayn / Qushayrī / Qurtubī

PR #46 — branche `fix/0.10.5-tafsir-formatting`, head audité `6abcaec1eea45e442ee056eebfb98fae64dbe6eb`.

### Jalalayn

- corpus approuvé conservé byte-identique ;
- 6 236 commentaires ;
- 427 notes ;
- checksums gelés ;
- aucune opération de nettoyage de contenu appliquée au corpus approuvé.

### Qushayrī

- source disponible auditée sur sourates 1–4 ;
- anglais du verset conservé ;
- commentaire anglais conservé ;
- texte arabe source exclu de l’asset final selon décision éditoriale ;
- 806 ancres source reconstruites en 720 entrées logiques ;
- 86 ancres secondaires rattachées à 76 plages de commentaire partagé sans invention ;
- 584 soft hyphens source traités au raccord puis absents du payload final ;
- pas de NBSP, PUA, caractère de remplacement ou débris de scan autorisé.

### Qurtubī

- uniquement les quatre volumes anglais approuvés ;
- 432 entrées ;
- couverture exhaustive de ces volumes jusqu’à 4:22 ;
- 4:23 exclu car il commence au volume 5 hors corpus disponible/approuvé ;
- 3 NBSP résiduels découverts contradictoirement puis normalisés ;
- pas d’arabe source injecté dans l’asset final.

### Rendu

- commentaire principal des trois Tafsīr justifié par Compose ;
- notes Jalalayn justifiées ;
- justification visuelle uniquement, sans réécriture du texte classique ;
- A− / A+ conservés.

### Preuve CI du head final

Le workflow `Android 0.10.5 Tafsir contradictory audit` a terminé en succès et a exécuté :

1. contrat source/rendu ;
2. récupération du Mushaf ;
3. reconstruction Qushayrī/Qurtubī depuis sources épinglées ;
4. audit exhaustif de propreté ;
5. répétition du contrat après reconstruction ;
6. audit de régression général existant ;
7. tests Light + Plus ;
8. builds release Light + Plus ;
9. preuve Light sans Tafsīr ;
10. preuve APK Plus avec exactement les trois corpus approuvés.

**Statut Tafsīr isolé: VERT.** La PR reste draft car la release intégrée n’est pas encore sûre.

## 3. Frontière banque / sécurité / identité

### Constat critique

La configuration Accessibility statique est étroite et n’inclut que Safeguard, System UI, six réseaux sociaux et huit navigateurs approuvés. `canRetrieveWindowContent=false` est conservé.

Cependant, lorsque le budget d’une cible est actif, `QuranAccessibilityService.applyEventPackageScope(broad = true)` peut temporairement définir `serviceInfo.packageNames = null`. Cela permet de recevoir le premier événement d’une application hors périmètre comme signal anonyme de sortie avant de rétablir la liste étroite.

Même sans journalisation ni persistance du package hors périmètre, ce mécanisme contredit la règle produit stricte : **banque / sécurité / identité = aucun événement Accessibility reçu par Quran Safeguard.**

Issue #49 créée comme bloqueur 0.10.5.

### Contraintes de résolution

- pas de liste nominative de banques ;
- pas de `QUERY_ALL_PACKAGES` ;
- pas de `canRetrieveWindowContent=true` ;
- pas de journalisation/persistance hors périmètre ;
- ne pas remplacer le problème par une API Android non garantie pour la logique produit ;
- si Android ne permet pas simultanément une mesure de présence parfaitement exacte et zéro événement hors périmètre, la frontière de confidentialité/sécurité prime et le compromis compteur doit être explicite et testé.

**Statut: BLOQUANT.**

## 4. Compteurs, appels, transitions et périmètre cible

Le gate `verifyReleaseAudit` couvre actuellement notamment :

- confidentialité ;
- frontière éditoriale ;
- intégrité du budget de déblocage ;
- migration de mise à jour ;
- protection limitée aux cibles ;
- pensée du jour ;
- expérience/ergonomie ;
- isolation Light/Plus ;
- 264 Ḥikam canoniques ;
- snapshot de rappels ;
- présence des 604 fichiers Mushaf.

Le head Tafsīr final a passé ce gate et les tests Light+Plus. Cela ne lève pas le bloqueur #49 car l’ancien gate accepte explicitement le sentinel anonyme temporairement large.

## 5. Version et migration

La branche de travail reste volontairement sur :

- `versionCode = 22`
- `versionName = "0.10.3"`

La 0.10.4 publiée était préparée au build par un script qui promeut 22/0.10.3 vers 23/0.10.4.

Pour 0.10.5, le marquage final doit être réalisé **uniquement après intégration des branches validées**, avec continuité de versionCode/migration (attendu séquentiel: versionCode 24 si aucune autre release intermédiaire n’a consommé ce code).

**Gate:** aucune APK appelée 0.10.5 ne doit sortir avec les métadonnées 0.10.3.

## 6. CI général — lacunes à fermer avant publication

Le workflow général `android.yml` actuel :

- exécute encore `verify_0103_update.py` ;
- lance les tests unitaires Light dans son étape générique ;
- compile Plus, mais le test Plus n’est pas explicitement inclus dans cette étape ;
- utilise encore des actions dont GitHub signale la migration de runtime/action comme maintenance à effectuer.

Le workflow contradictoire Tafsīr compense actuellement les deux premières limites pour cette branche en exécutant Light+Plus et les vérifications multi-Tafsīr complètes. **La branche d’intégration 0.10.5 devra rendre ces garanties permanentes dans le CI général/release, pas seulement dans un workflow de chantier.**

## 7. Interdictions de fusion/publication actuelles

Ne pas publier 0.10.5 tant que les quatre conditions suivantes ne sont pas simultanément vraies :

1. PR #48 Mushaf exécutée réellement par CI et verte ;
2. issue #49 résolue et test contradictoire d’isolation hors périmètre vert ;
3. branches Mushaf + Tafsīr intégrées sur une branche finale, puis audit complet répété sur **le même commit** ;
4. version/migration 0.10.5 et workflow de release final vérifiés sur ce commit.

## 8. Principe de preuve final

Les succès obtenus sur des branches séparées ne s’additionnent pas automatiquement. Le seul candidat publiable sera un **commit d’intégration unique** dont les deux APK Light/Plus sont construits, inspectés et audités ensemble après résolution de tous les bloqueurs.
