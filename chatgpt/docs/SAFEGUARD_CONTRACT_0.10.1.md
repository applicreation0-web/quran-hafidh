# Safeguard 0.10.1 — contrat de pré-sortie

Ce contrat complète le contrat 0.10.0 sans changer l’architecture Android :
Quran Safeguard conserve son `AccessibilityService` et son périmètre de cibles
explicites.

## Invariants bloquants

- Les 15 minutes sont exactement la somme des durées de présence au premier plan
  dans toutes les applications cibles sélectionnées.
- Les 90 minutes sont six cumuls successifs de 15 minutes de cette même présence
  cible, jamais 90 minutes d’horloge.
- Passer de Chrome à YouTube, WhatsApp texte ou une autre cible ne réinitialise ni
  le cumul de 15 minutes ni le cumul de 90 minutes.
- Une application hors cible, un appel, l’écran éteint ou une activité en arrière-
  plan ne consomme aucune milliseconde du crédit.
- Le service revient à sa liste étroite immédiatement après le premier signal de
  sortie, au début d’un appel, à l’expiration, au retour vers Safeguard/System UI
  ou à une interruption du service.
- Chaque page requiert 60 secondes actives. L’absence d’un signal de défilement
  du WebView ne peut plus bloquer la validation après la minute.
- Les pages déjà validées sont accessibles vers la droite ; l’avance vers la
  gauche reste impossible tant que la page active n’a pas atteint 60 secondes.
- Après le quota, le déblocage est acquis et la lecture peut continuer librement.
- Les Hikam n’exposent aucun commentaire : seulement le texte arabe vocalisé, la
  traduction française et la source documentaire.
- Les retraits d’applications cibles restent effectifs le lendemain ; les ajouts
  et annulations de retrait restent immédiats.

## Barrières avant sortie

1. Audit statique complet.
2. Tests unitaires et stress tests déterministes.
3. Compilation debug et release sur le même commit.
4. Audit contradictoire, corrections, puis second audit.
5. Signature avec la lignée historique et contrôle cryptographique de l’APK.
6. Aucune fusion ou publication sans validation explicite.
