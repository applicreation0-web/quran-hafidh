# J2 — Quran.com : vérification de Qāf (10 octobre 2026)

**Travail effectué :** confrontation des huit groupes documentaires de Qāf (50:1–45) avec les huit pages de Tafsir *Ibn Kathīr (abridged)* affichées sur Quran.com. Source technique de l'index : `spa5k/tafsir_api`, SHA `eb82bb6294efe30ad5c135c03b1864afaa70e855`.

Données machine : `docs/ibn_kathir_qaf_qurancom_editorial_evidence.json`.

Contrôle reproductible : `python3 scripts/verify_ibn_kathir_qaf_qurancom.py --negative-tests`. Il recalcule les bornes de Qāf depuis l'index zlib épinglé, vérifie les 3 pages 518–520 et l'existence réelle de chaque mot de départ envisagé dans le fichier source de 77 432 coordonnées QCF. Il rejette toute fausse approbation, mauvais verset, fausse page ou groupe supprimé.

### Observations vérifiées sur Quran.com

| Groupe | Page | Titres/discussions repérés dans le Tafsir | Point de vigilance |
|---|---:|---|---|
| 50:1–5 | 518 | Introduction / section Mufassal ; étonnement devant le message et la Résurrection | Les informations introductives ne sont pas automatiquement des amorces |
| 50:6–11 | 518 | Puissance créatrice et argument en faveur de la Résurrection | Pas de division artificielle |
| 50:12–15 | 518 | Châtiment des anciens négateurs **puis** capacité à renouveler la création | Transition au verset **15**, sans réviser la frontière de groupe |
| 50:16–22 | 519 | Connaissance divine / anges scribes **puis** mort et rassemblement | Transition dans le même groupe au verset **19** |
| 50:23–29 | 519 | Ange témoin / rétribution **puis** dispute avec le compagnon | La source distingue les sens de *qarīn* aux versets **23 et 27** |
| 50:30–35 | 519 | Enfer et Jardin | Deux scènes, titre commun |
| 50:36–40 | 520 | Avertissement par les peuples passés, puis ordre de patience et de louange | Verset **39** à examiner |
| 50:41–45 | 520 | Résurrection **puis** réconfort du Prophète | Transition au verset **45** |

Les extraits originaux anglais, volontairement courts, sont dans le JSON à côté des URL exactes des huit sources. Ce n'est pas un corpus exhaustif de tous les sous-titres. Ni le commentaire entier ni des titres religieux inventés n'ont été copiés.

**Important :** les références de mots QCF sont des *positions à examiner*, pas des amorces approuvées : leur longueur, pertinence, rareté et effet de rappel restent à décider. Aucun quota une amorce/groupe. Aucun actif Android ou moteur modifié ; Al-Munīr reste actif jusqu'à un remplacement sûr.

**Jalon suivant :** appliquer la même méthode aux 109 cas contradictoires, puis aux 1 903 groupes, en vérifiant le texte et les contextes réellement disponibles sur Quran.com. Les différences documentaire/édition doivent rester visibles.
