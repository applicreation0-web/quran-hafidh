# Quran Safeguard — cahier des charges fonctionnel de handoff Claude

Date: 2026-09-11
Produit: Quran Safeguard
Cible: smartphone Android.
Principe global: OFFLINE-FIRST pour Mushaf, Tafsir, règles de protection et persistance.

## 1. Frontière produit
Quran Safeguard est séparé de Quran Hifz. Il contient lecteur Mushaf, Lecture/Étude + Tafsir, protection d'applications, lecture débloquante et suivi de temps. Il ne contient aucune Mémorisation libre, Sabqi, Itqān, Murājaʿah ni audio Hifz.

## 2. Apps protégées
- Architecture positive allowlist uniquement.
- Cibles historiques: WhatsApp, X, Instagram, Facebook, YouTube, TikTok; Chrome, Firefox, Edge, Brave, Opera, Samsung Internet, DuckDuckGo, Vivaldi.
- Interdiction de `QUERY_ALL_PACKAGES` et de toute énumération large.
- Apps bancaires, identité, sécurité, santé et travail sensibles: hors périmètre par construction, sans classification, sélection, log, interception ni persistance.
- NO-GO si une banque/identité/sécurité subit un écran, une interception ou un lag provoqué par Safeguard.

## 3. Lecture débloquante
- Une page du Mushaf de Médine doit être affichée au moins 60 secondes de temps réellement actif avant validation.
- Temps monotone; arrière-plan et écran éteint ne comptent pas.
- Page maximisée tout en conservant le format canonique; scroll si nécessaire.
- Après validation, retour direct et fluide vers l'application cible.

## 4. Mushaf et Lecture/Étude
- 604 pages locales `.svg.br` dans l'APK, aucune dépendance réseau nécessaire.
- Géométrie locale.
- Lecture volontaire séparée de la lecture débloquante.
- Lecture/Étude propose le Tafsir; aucune fonctionnalité Hifz dans ce lecteur.
- Test bloquant: page réelle visible, p1->p2->p1, jamais écran blanc/page erreur.

## 5. Tafsir
- Base SQLite locale compressée.
- Corpus test actuellement embarqué: Al-Jalalayn anglais, hash validé `26d8715a9bcecda6cb6397f0d8a530cb9404bb69ba66ed5264ed3f5b16d11a56`.
- Corpus français final à valider; ne jamais inventer de traduction.
- Fonctionnement attendu hors ligne.

## 6. Compteurs de temps
Principe produit attendu: seul le temps réel passé dans l'application protégée est débité. Quitter l'app doit mettre son compteur en pause immédiatement; revenir reprend le reste exact. Le temps dans une autre app ne doit pas être débité.

Exemple cible à préserver lors de la finalisation: WhatsApp 20 min -> utilisation 5 min -> reste 15; une heure hors WhatsApp -> reste toujours 15; retour 4 min -> reste 11. Chrome doit avoir son propre compteur indépendant.

ATTENTION: la base historique Claude contient encore des règles 15 min / 90 min globales. Ne pas les considérer comme règle finale tant que la politique exacte n'est pas figée avec le propriétaire du produit. Cette ambiguïté doit être explicitement résolue, jamais silencieusement.

## 7. Avertissements
- À 10 min, 5 min, 1 min restantes.
- Petite bannière, une vibration, aucun son.
- Une seule notification par seuil.

## 8. Jokers
- Historique: 3 par jour.
- L'effet métier exact d'un joker doit encore être figé avant release finale.
- Le compteur 3/jour peut rester dans le build de test, mais ne pas inventer une sémantique définitive.

## 9. Réinitialisation quotidienne
- Ne pas inventer l'heure définitive de reset. Historique et versions précédentes divergent.
- Toute politique de reset doit être centralisée et testée explicitement avant release.

## 10. Protection / Accessibility
- Le service Accessibility doit rester strictement limité aux packages de l'allowlist.
- `canRetrieveWindowContent=false`.
- Aucun contenu de fenêtre sensible ne doit être lu.
- Pas de collecte de package hors scope.
- Les réglages doivent être guidés depuis l'app afin d'éviter une ergonomie laboratoire, tout en respectant les contraintes Android qui exigent l'activation utilisateur d'Accessibility.

## 11. Transition après déblocage
Parcours attendu: app cible -> Safeguard -> lecture 60 s active -> validation -> app cible au premier plan. Pas d'écran intermédiaire inutile, pas de perte du contexte cible.

## 12. Désinstallation
- Désinstallation libre; aucune rétention coercitive.
- L'utilisateur ne doit jamais être piégé dans l'application.

## 13. Rappels éditoriaux
- Rappel quotidien 20h: arabe puis français, source précise.
- Adhkar matin et soir, translittération optionnelle uniquement pour adhkar.
- Hadiths: sources reconnues et authenticité vérifiable.
- Al-Hikam: corpus authentifié, arabe + français, pas de synthèse IA; dans les versions les plus récentes la préférence est de garder les Hikam elles-mêmes et de retirer les commentaires si leur qualité/source n'est pas garantie.
- Ne pas réintroduire du contenu non vérifié pour compléter artificiellement un corpus.

## 14. Persistance
- État de protection, compteurs, avertissements déjà émis, jokers et lecture débloquante doivent survivre aux changements d'activité/recréation selon la règle métier.
- Aucune re-créditation silencieuse.

## 15. Tests bloquants
NO-GO si: écran blanc lecteur, APK non installable, banque/identité/sécurité observée ou ralentie, temps débité hors app cible, compteurs qui se mélangent, validation lecture avant 60 s actives, écran éteint qui fait avancer le chrono, cible non rouverte après validation, crash/ANR, Hifz/audio Hifz présent dans le produit smartphone.

## 16. Principe de preuve
Pour chaque fonction critique: cause/comportement attendu -> un test unique et lisible -> preuve runtime. Ne pas multiplier les tests avant d'avoir isolé la cause d'un défaut. Une CI verte ne remplace pas un test utilisateur réel sur téléphone pour les banques, transitions et ergonomie.
