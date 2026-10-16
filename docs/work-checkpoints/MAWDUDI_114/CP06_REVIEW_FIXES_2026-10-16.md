# CP06 — corrections après revue contradictoire, 16 octobre 2026

Entrée : 4992ccd1410076c42edf95bfe2bc8e69a808b2b0. Sortie : commit portant ce document.

Corrections vérifiées dans le code :
- flag privé défini dans debug uniquement ; release explicitement false, même avec variable privée ; assets/manifest et identifiant isolés debug.
- cible du premier mot transmet exactement le geste natif verseTap avant semanticCueTap, pour préserver choix de fin passive et marquage faible armé actif. Test Node rouge puis vert ; nouveau scénario Android sur les deux modes.
- pages du sous-passage 27 bornées au focus sélectionné au lieu du parent ; bornes physiques réelles, sans changer les passages.
- SHA-256 des trois assets privés approuvés épinglés au chargeur ; fichiers absents ou altérés rejetés, sans publier les libellés.
- contrôle d’icônes inchangé, nom de receveur rendu reconnaissable par son parseur.

Les 98 tests originaux sont conservés ; la suite est portée de 100 à 101 tests. Le nouveau test ne crée aucun crédit/séance validée et restaure la sauvegarde de test.
Les quatre seuls ajouts UI à HifzSessionActivity sont toujours reconstruits exactement sur le blob gelé 491aae573afba8bdd5da339a8e15dca8e59661ef. Aucun nouvel accès aux moteurs/masques.

Historique : audit source 38889877548 FAIL au contrôle de nom d’icône ; Android 38889877569 PASS 100. Ces résultats appartiennent à 4992ccd, pas au nouveau SHA. Nouvelle CI et build privé à terminer. Essais privés réels et BOOX : NOT_RUN.
Reprise : compiler/contrôler au SHA de ce commit, attendre les deux CI puis figer provenance et APK privés.

Suite au commit 709eccd4 : les 446 JVM + 244 contrats passent localement. Empaquetage du test APK réussi après suppression automatique d’un état temporaire périmé. CI Android 38891449248 arrêtée avant émulateur : compteur source resté à 100, corrigé à 101 sans supprimer de test. Sorties locales historiques privées/public mélangées dans les intermédiaires AGP : répertoire build-private114 séparé ajouté ; build public nettoyé puis recontrôlé avant toute diffusion. Aucun titre n’a été envoyé en CI. Nouvelle CI attendue au commit de ce complément.

Run 38891629474 au 27b846f1 : audit complet PASS, dont les 3 scénarios M8R 180/365/548 jours. Run Android 38891629718 : 101 exécutés, 100 PASS, 1 FAIL dans le nouveau test titleTargetPreservesPassiveEndpointAndArmedActiveWeakMark, ligne 138 (plan null avant le geste). Cause vérifiée : fixture acquis Fātiḥah seule, curseur bootstrap passif 2:1 hors corpus. Correction du test : fixer explicitement le curseur à 1:1 via l’API existante, garder toutes les assertions comportementales. Aucun code produit changé pour ce correctif. Nouvelle suite entière requise.
