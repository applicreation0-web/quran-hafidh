# Audit contradictoire — Quran Safeguard 0.10.0

Date : 2026-09-02  
Base auditée : implémentation des points 1 et 2, commit `ec38c931a3698e887b0c53e6a8794fce472ae377`.

## Méthode

L’audit cherche volontairement à invalider le produit sur cinq axes : périmètre, exactitude du temps effectif, transitions 15/90 minutes, reprise après interruption et friction Android. Le premier build réel a validé l’audit Gradle, les tests unitaires et les APK debug/release avant l’application des corrections ci-dessous.

## Résultats et corrections

| Risque attaqué | Constat contradictoire | Correction |
| --- | --- | --- |
| Application non cible observée | L’écoute est étroite au repos, s’élargit seulement pendant une cible et oublie le premier paquet de sortie sans journalisation. | Conforme. Les anciens classifieurs, l’écran d’exclusions et la découverte globale des applications sont supprimés. |
| Cible désinstallée conservée | Une ancienne sélection pouvait rester dans les préférences et gonfler le compteur visuel. | Le catalogue ne vérifie que les 14 identifiants autorisés, met en cache le sous-ensemble installé et purge les sélections absentes. Une nouvelle installation reste volontairement opt-in. |
| Changement de cible | Deux crédits séparés permettraient de contourner le quota en alternant les applications. | Un seul budget global suit toutes les cibles. Les transitions et PiP ne peuvent avoir qu’un propriétaire actif. |
| Expiration interrompue | Un arrêt du processus entre le budget à zéro et le niveau en attente pourrait affaiblir le sixième palier. | Le niveau est persisté avant l’expiration ; l’accès exige explicitement l’absence de palier en attente. La reconstruction conserve MICRO contre HIZB. |
| Validation interrompue | Un arrêt entre la fin d’un palier et l’attribution du crédit pourrait exiger une lecture supplémentaire ou laisser passer un crédit. | La fin du palier et le nouveau crédit de 15 minutes partagent une transaction `SharedPreferences`. |
| Joker interrompu | Un arrêt pouvait théoriquement consommer un joker sans attribuer son crédit. | Consommation, niveau franchi, crédit et entrée d’historique partagent une transaction. |
| Passage de minuit | Une page ouverte avant minuit pouvait ne plus appartenir au plan matinal du nouveau jour. | La page périmée est refusée sans plantage et l’interface ouvre le nouveau plan quotidien. |
| Appel entrant/sortant | WhatsApp texte et appel partagent le même paquet Android. | Les modes audio 1 à 6 suspendent le temps pour sonnerie, téléphonie et VoIP ; les activités d’appel WhatsApp connues couvrent la transition avant le mode audio. Aucune permission téléphonique ou notification n’est ajoutée. |
| Ancienne protection de désinstallation | Un identifiant interne inutilisé maintenait un lien conceptuel avec une application non cible. | Identifiant et méthodes mortes supprimés ; Android Settings reste totalement hors périmètre. |
| Retour depuis les réglages | Les nombres affichés pouvaient rester anciens jusqu’à une recomposition ultérieure. | Les écrans d’accueil rechargent cibles, pool et progression à chaque retour au premier plan. |
| Suivi des jokers | Le nombre restant était neutre mais ne consignait pas explicitement l’usage. | Le tableau de bord indique « utilisés aujourd’hui » et « disponibles », sans jugement. |
| Test manuel | Le bouton de test pouvait ouvrir Chrome même s’il n’était pas sélectionné. | Il ne choisit plus qu’une cible installée et activée. |

## Vérifications de règles

- 6 cibles sociales exactes et 8 navigateurs exacts.
- 20 pages matinales, 60 secondes actives par page.
- Un pool d’un Hizb répète les mêmes 10 pages ; plusieurs Hizb avancent dans l’ordre croissant.
- Cinq paliers d’une page, puis un palier de 10 pages au sixième intervalle.
- Le palier de 90 minutes absorbe la pause simple correspondante et remet le cycle courant à zéro.
- Trois jokers quotidiens utilisables aux trois niveaux.
- Aucune durée configurable, aucun crédit par application et aucun accès réseau.
- Appels, arrière-plan, écran éteint et lecteur non principal ne consomment pas le temps.

## Limites honnêtes à tester sur téléphone

1. Les fabricants Android peuvent retarder ou regrouper certains événements d’accessibilité ; le compteur utilise des pauses immédiates et un checkpoint d’une seconde, mais un essai réel reste obligatoire.
2. WhatsApp peut renommer ses activités d’appel. `AudioManager` reste le mécanisme principal ; le nom d’activité n’est qu’une couverture anticipée.
3. Le minimum de 60 secondes prouve une présence active sur chaque page, pas la compréhension spirituelle.
4. Un nouvel identifiant régional ou « Lite » d’une application n’est pas ajouté automatiquement : le périmètre reste strict par conception.

## Verdict avant second build

Aucun contournement logiciel certain n’est resté ouvert dans le modèle testé. Les corrections ci-dessus doivent encore passer le second audit Gradle, l’ensemble des tests, les deux assemblages APK et le contrôle de signature. Aucune publication n’est autorisée.
