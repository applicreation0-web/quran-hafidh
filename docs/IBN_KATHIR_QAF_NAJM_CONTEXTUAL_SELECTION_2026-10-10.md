# Première décision éditoriale contextuelle — 16 groupes Ibn Kathīr

Le passage de la méthode purement lexicale à la méthode sémantique est formalisé ici. **30 propositions de positions** sont préconisées à l'échelle de 8 groupes de Qāf + 8 groupes d'An-Najm.

La source coranique reste rigoureusement `quran.ws` Hafs, texte SHA-256 épinglé et mots alignés à la géométrie QCF. Les références exactes du Tafsir Ibn Kathīr sur Quran.com restent les seuls justificateurs du contexte.

**Décisions notables :**
- Qāf 50:23 / 50:27 : les deux formulations proches désignent des compagnons différents (ange témoin / compagnon diabolique). Conservation de deux amorces distinctes.
- 50:31 : trois mots sémantiquement cohérents, pas quatre mots finissant sur `غير` ; unicité lexicale non imposée.
- 50:36 : préférence pour l'image interne du parcours des terres en **50:36:10** plutôt qu'une amorce de sept mots au début du verset. Le second candidat **50:36:7** est écarté du choix préférentiel, mais reste une alternative documentée.
- 50:39 : choix interne **50:39:5**, 3 mots mentionnant la glorification, même si une séquence analogue existe ailleurs.
- 50:45 : quatre mots expriment une parole de réconfort complète ; ne pas ajouter le cinquième mot `وَمَا` dans le seul but d'unicité.
- An-Najm 53:1 et 53:3 : distinguer le serment d'ouverture et le cœur de l'argument sur la révélation ; éviter le faux titre « l'étoile atteste ».
- An-Najm 53:27 : prendre l'attribution aux anges à **53:27:6** plutôt que six premiers mots génériques.
- An-Najm 53:31 : prendre la rétribution des œuvres à **53:31:8** plutôt que huit premiers mots génériques.

Le fichier `docs/ibn_kathir_qaf_najm_editorial_preferences_round1.json` décrit les décisions et leurs raisons, **sans recopier le texte anglais du commentaire**. Le script vérifie ces sélections avec un vrai corpus de 77 432 mots, contre le registre de 30 candidats d'origine et contre les 9 variantes internes.

**Étape non atteinte : approbation de diffusion et activation dans l'application.** Le corpus doit subir la contre-lecture du sens, l'exactitude graphique Mushaf/QCF et un test de rappel BOOX avant `SemanticPassageRepository`. Statut `approved=0`, `runtime_ready=false`, pas d'APK.
