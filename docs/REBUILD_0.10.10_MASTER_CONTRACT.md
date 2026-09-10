# Quran Safeguard 0.10.10 — contrat maître de reconstruction

## Provenance

- Base applicative obligatoire : `f21002d6c491a1a1556c5c7e1c55bf5635e741c5` (0.10.8 validée)
- Branche de reconstruction : `rebuild/0.10.10-hifz-on-0.10.8`
- Ancien candidat 0.10.10 utilisé uniquement comme donneur de blocs contrôlés : `24640a22a828409bb98fcab76dc49eadf967339b`
- La migration officielle doit être directe 0.10.8 -> 0.10.10 ; 0.10.9 n'est jamais un prérequis.

## Fonctions obligatoires 0.10.10

1. Protection limitée aux réseaux sociaux et navigateurs explicitement sélectionnés.
2. Banque, sécurité et identité strictement hors scope : aucune interception, blocage ou ralentissement indu.
3. Compteur de déblocage basé uniquement sur la présence active réelle dans l'application cible, avec pause/reprise exacte et persistance.
4. Budget/cycle Safeguard conforme au contrat courant : 15 min de présence active pour le budget concerné et cycles 90 min là où prévus.
5. Challenge Qur'an : Mushaf de Médine canonique 604 pages, minimum 60 s de lecture active par page, navigation et validations obligatoires, filtre matinal 20 pages.
6. Retour fluide vers l'application cible après déblocage.
7. Lecture libre indépendante du challenge et du Hifz.
8. Mémorisation libre indépendante : sélection/focus exclusif, masquage progressif, persistance, aucun Tafsir en Light ni Plus.
9. Audio Al-Husary Mu'allim pour mémorisation et Hifz : synchronisation au verset entier uniquement, répétitions, pause/reprise, téléchargement local et usage hors ligne après téléchargement.
10. Parcours Hifz structuré indépendant : Sabqi, Itqan, Muraja'ah ; bornes configurables ; jours par défaut ; quotas ; reports souples ; suivi temps, erreurs, aides et répétitions.
11. Sabqi : cible 5 lignes et protocole de mémorisation prévu.
12. Itqan : page x30, masquage obligatoire, statistiques de durée et révélations comme signal de difficulté.
13. Muraja'ah : Itqan + Sabqi récent ; rythme par défaut 1 juz = 45 min ajustable.
14. E-Ink : profil dédié, navigation fiable, rafraîchissement/ghosting contrôlé, masquage/révélation propre, sessions longues stables.
15. Tafsir : Plus uniquement en Lecture/Étude ; jamais dans Mémorisation ou Hifz ; Light sans corpus Tafsir.
16. Adhkar matin et soir, rappels planifiés, bibliothèque Hadith/Al-Hikam, arabe + français + sources vérifiées.
17. Al-Hikam : conserver les Hikam ; ne pas réintroduire de commentaire classique si le contrat produit courant le retire.
18. Suivi local : historique lecture, temps, moyennes et détection des valeurs irréalistes.
19. Migration directe 0.10.8 -> 0.10.10 sans perte silencieuse des réglages, signets, états utiles, protection ou compteurs compatibles.
20. Robustesse : veille, retour app, verrouillage, redémarrage, faible mémoire, stress Android et stress E-Ink.
21. Éditions Light/Plus strictement isolées.
22. Aucun composant historique, expérimental ou 0.10.9 n'est réimporté sans correspondre explicitement à une fonction ci-dessus.

## Discipline de validation

- Une fonction obligatoire non testée = NO-GO.
- Un défaut critique téléphone ou E-Ink = NO-GO.
- Un échec de harnais CI ne doit jamais être présenté comme un échec produit sans preuve.
- Aucun changement applicatif n'est justifié uniquement pour faire passer un test faible ou erroné.
- Chaque bloc importé doit avoir des tests unitaires/runtime correspondants avant gel de release.
