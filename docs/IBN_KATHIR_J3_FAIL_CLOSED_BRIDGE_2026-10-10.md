# J3 — amorces Ibn Kathīr : premier contrat Android fail-closed (2026-10-10)

**Statut : STAGING, PAS DE BASCULE RUNTIME.**

La nouvelle classe `IbnKathirAmorceBridge.java` verrouille l'interface documentaire d'entrée J2 → J3. Elle relit et vérifie en Java l'index documentaire réel de **1 903 intervalles** et ses **6 236 versets**, les **604 pages** du Mushaf via `GeometryRepository.pageForVerse`, la première clé de mot et l'intégrité de provenance. Elle refuse tout attribut qui essaierait de rendre active une clé non approuvée.

`IbnKathirAmorceBridgeTest.java` reçoit le véritable JSON J2 généré par la CI (`IK_J2_REVIEW_PATH`) et vérifie le NO GO, puis des tests négatifs (activation, approbation usurpée, fausse page QCF, droits affirmés sans preuve, groupe manquant).

La façade **n'est pas encore branchée à `SemanticPassageRepository`**, car il y a `0/1903` validations éditoriales et aucune autorisation de redistribution démontrée pour le texte du commentaire anglais Darussalam. Une bascule immédiate priverait Itqān post-An-Nās des repères existants et ferait disparaître les amorces de Lecture. Ce risque a été jugé incompatible avec la non-régression 1.17.1.

À terme, un **nouveau manifeste d'approbation authentifié** (distinct du JSON de revue) devra produire les `Cue` exactes, jamais le rapport de brouillons J2. Les façades Lecture et Itqān consomment déjà la même API `SemanticPassageRepository` ; il ne faudra modifier aucun moteur. Les clés supplémentaires dans un groupe et le cas sans clé doivent être explicitement supportés, sans quota.

Aucun actif éditorial tiers, aucune nouvelle amorce, aucune modification à `HifzSessionActivity`, `QuizCorpus`, `MushafView`, ni APK.
