# CP-08P — Prototype Android Al-Baqarah
Date 2026-10-15 — **EN COURS**. Nouvel APK : **NON CONSTRUIT**.

## Portée
- Android existant, branche unique `work/haafidh-1171-ibn-kathir-integration-independent`, aucune modification Hifz / QUL / édition 1.17.1.
- Treize thèmes anglais issus de « Surah Overview » https://quran.com/al-baqarah/info (adapté d'Ibn Ashur). Libellés courts affichés : adaptations éditoriales, non titres verbatim.
- Les références forment un index **non exhaustif et chevauchant** : 2:1 et 2:23 sont disjoints, 2:106 et 2:177 appartiennent à plusieurs rubriques. Jamais de découpage artificiel des 173 groupes Ibn Kathir.
- Vue graphique THEMES puis groupes correspondants, et ALL PASSAGES pour les 173 sans exclusion. Les 113 autres sourates conservent la carte CP-03.
- Titre anglais, préférence Israelites quand la fidélité des versets le permet et source conservée ; les 1 903 titres/commentaires privés restent chiffrés, jamais dans GitHub public.
- Le Tafsir intégral anglais reste subordonné à l'appariement OCR exact CP-08A/B et aux droits d'utilisation : aucun texte fictif.

## Contrat UI
- Panneau documentaire supérieur, Muṣḥaf QCF inférieur en **même instance** ; l'en-tête de bloc est le point d'entrée commentaire.
- Accès par thème, tout corpus, retour carte, scroll et surlignage inchangés. Sur BOOX côte à côte seulement si lisible.
- Tests ciblés 2:1/23, 2:106, 2:142–150, 2:148, 2:177, 2:255 et 2:284–286.

## Statuts
- Modèle thématique public, tests JUnit : **CODE ÉCRIT, CI À EXÉCUTER**.
- Interface Android : **EN COURS**.
- Appariement 173 commentaires intégraux et APK privé téléphone : **NON EXÉCUTÉS**.
- Validation utilisateur téléphone préalable à toute généralisation.
