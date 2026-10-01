# Safeguard 0.10.0 — contrat de pré-sortie

Les décisions des points 1 et 2 ci-dessous remplacent les anciens mécanismes d’exclusions, de crédits par application et de durées réglables.

## Invariants bloquants

- Le périmètre se limite aux six réseaux/messageries approuvés et aux huit navigateurs approuvés.
- Toute autre application est hors périmètre sans configuration, classification, conservation ni journalisation.
- Un unique compteur d’usage effectif suit l’ensemble des cibles afin qu’un changement d’application ne crée ni crédit ni dette séparée.
- Les appels classiques et WhatsApp audio/vidéo mettent immédiatement le compteur en pause.
- Le premier accès quotidien exige 20 pages, avec 60 secondes actives au minimum par page.
- Si le pool ne contient qu’un Hizb, ses 10 pages sont répétées ; sinon la sélection avance séquentiellement du plus petit au plus grand.
- Chaque tranche de 15 minutes exige une page.
- La sixième tranche exige 10 pages et absorbe la page simple qui aurait coïncidé à 90 minutes.
- La validation des 10 pages remet le cycle 90 minutes à zéro.
- Trois jokers quotidiens peuvent franchir chacun des trois niveaux et ouvrent le prochain intervalle normal.
- Le tableau de bord consigne l’usage et les jokers de façon factuelle, sans culpabilisation.
- Toute page requiert 60 secondes actives et la progression réelle jusqu’en bas si nécessaire.
- L’usage et la lecture sont suspendus en arrière-plan, écran éteint et pendant les appels.
- L’identité visuelle, le Mushaf canonique et les règles éditoriales des rappels demeurent inchangés.

## Choix de moindre friction

- Pas de `NotificationListenerService`, pas de permission téléphonique et pas d’écran d’exclusions.
- Détection des appels par les modes audio Android, complétée par les activités d’appel WhatsApp connues.
- L’écoute d’accessibilité est étroite par défaut. Elle s’élargit uniquement durant une cible jusqu’au premier événement de sortie, sans conserver l’application de destination.
- Aucun détour vers les réglages Android hormis la page d’activation explicite du service Safeguard.

## Séquence obligatoire

Implémentation -> tests -> audit contradictoire -> corrections directes -> second audit -> APK exact -> contrôle de signature -> essai sur appareil.

L’APK de test peut être produit, mais aucune publication ni fusion de la branche n’est autorisée sans validation explicite de l’utilisateur.
