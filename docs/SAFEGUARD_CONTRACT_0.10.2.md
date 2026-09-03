# Safeguard 0.10.2 — contrat de sortie Light et Plus

Ce contrat complète 0.10.1 sans modifier les règles de protection, de lecture
active, de cumul 15/90 minutes ou de désinstallation libre.

## Invariants reconduits

- Les 15 minutes sont exactement la somme des durées de présence au premier
  plan dans toutes les applications cibles sélectionnées.
- Les 90 minutes sont six cumuls successifs de 15 minutes de cette même
  présence cible.
- L’absence d’un signal de défilement ne bloque pas une page entièrement
  visible après 60 secondes de lecture active.

## Éditions

- Light conserve `com.applicreation0.quransafeguard`, la lignée de signature
  historique et constitue l’unique version distribuée par invitation.
- Plus utilise `com.applicreation0.quransafeguard.plus`, une clé dédiée et un
  canal privé. Elle peut coexister avec Light mais un seul service
  d’accessibilité doit être activé.
- Plus contient le tafsir anglais Al-Jalalayn hors ligne. Light ne contient ni
  corpus, ni PDF, ni pont JavaScript d’activation du tafsir.

## Tafsir Plus

- Les 6 236 commentaires numérotés et 427 notes sont rattachés exactement à
  leur verset; introductions et basmala non numérotées sont exclues.
- Le tap sélectionne le verset, le surligne en vert clair et ouvre un panneau
  natif défilable limité à la moitié de l’écran.
- Le chronomètre continue pendant la lecture du commentaire. Retour Android
  ferme le panneau et restaure la position de la page.
- L’intégrité de la base est vérifiée avant le build et à l’exécution.

## Barrières de publication

1. Audits et tests unitaires historiques réussis.
2. Compilation debug et release des deux éditions sur le même commit.
3. Inspection anti-fuite de l’APK Light.
4. Inspection du corpus reconstruit depuis l’APK Plus.
5. Signature Light avec le certificat historique et Plus avec son certificat
   privé distinct.
6. Contrôle APK v2/v3, identifiant, versionCode, versionName et SHA-256 des deux
   fichiers avant publication.
