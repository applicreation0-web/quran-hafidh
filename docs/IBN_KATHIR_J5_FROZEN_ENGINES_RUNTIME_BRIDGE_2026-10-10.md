# J5 – Brancher un futur corpus de rappel Ibn Kathīr, sans régression

**Modification de production effective :** SemanticPassageRepository reçoit un chargeur Ibn Kathīr, avant le corpus Al-Munīr. Quand (et seulement quand) un corpus *intégralement revu* est emballé dans l'APK et que son SHA-256 spécifique est épinglé dans le code, Lecture/Révision active/Itqān peuvent consommer ce corpus par l'API actuelle, **sans changer les moteurs**.

**La bascule est encore STRICTEMENT désactivée** : EXPECTED_APPROVED_MANIFEST_SHA256 est volontairement vide, aucun fichier approved-recall-v1.json n'est livré. La 1.17.1 Hifz/Quiz utilise toujours les amorces Al-Munīr à ce stade. Ce n'est ni une approbation éditoriale ni une bascule livrée.

Contrat vérifié par le parseur :
- SHA source Ibn Kathīr + SHA mots Hafs + schéma canonique ;
- exactement 1 903 groupes éditorialement décidés, autorisant **0, 1 ou plusieurs** départs de récitation par groupe ;
- pour chaque départ, approbation de la fonction mnémotechnique, contrôle minimalité et géométrie QCF, vraie suite de mots sans franchir groupe ou page ;
- mots réels, coordonnées exactes 345×550, aucun commentaire anglais reproduit ni titre inventé ;
- chaque page possédant des repères pour éviter un masque noir aveugle en Révision active et Itqān ; les sous-blocs Itqān devront recevoir des tests spécifiques ;
- pas de modification des historiques/cursors, de HifzPrefs, HifzSessionActivity, du moteur Quiz ni du renderer MushafView.

**Tests à prévoir / poursuivre** : rejeter manifest incomplet, omission d'un groupe, clé forgée, groupe incohérent, défaut de repère sur une page, faux SHA ; tester le chargement d'un corpus artificiel *en mémoire de test uniquement* sans lui conférer d'approbation documentaire.

**GO/NO GO** : l'activation reste NO GO jusqu'à validation éditoriale sur les 604 pages, audit régression sur debug réel (BOOX) et accord explicite avant APK.
