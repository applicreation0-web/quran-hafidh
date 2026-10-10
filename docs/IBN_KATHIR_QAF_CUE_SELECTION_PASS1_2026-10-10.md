# J2 : premier tri des amorces de Qāf — 10 octobre 2026

Ce jalon change l'unité de travail : au lieu d'un simple inventaire de 8 groupes, **14 positions contextuelles** (50:2, 6, 12, 15, 16, 19, 23, 27, 30, 31, 36, 39, 41, 45) ont une proposition de longueur de 2 ou 3 mots coraniques, une justification, une réserve contradictoire et des références Quran.com. Il n'y a pas de règle imposant une amorce par groupe.

Le fichier source est `docs/ibn_kathir_qaf_cue_selection_pass1.json`. Le contrôle `scripts/verify_ibn_kathir_qaf_cue_selection_pass1.py` prouve que les mots proposés sont contigus, ont des clés QCF vérifiées dans les pages réelles 518–520 et ne débordent pas sur un autre verset. Il bloque les écarts et toute activation anticipée.

Ce n'est PAS une validation éditoriale finale : **0 approuvées**. Les graphies courtes visibles sur Quran.com sont une aide de lecture et ne sont pas encore un contrôle indépendant caractère par caractère des polices QCF. Restent surtout la rareté du fragment dans tout le Coran, la minimalité face aux autres possibilités, et la prévention de confusion 50:23 / 50:27. La justification française est un commentaire de sélection, PAS un titre original d'Ibn Kathīr à afficher.

**Aucun changement** dans le moteur Hifz, Lecture, Itqān, Sabqi, Quiz, `MushafView`, ni dans les ressources runtime ; aucune nouvelle amorce n'est exposée sur l'écran ; aucun APK généré.

Prochain GO : revue contradictoire et comparaison des fragments dans le corpus complet, puis seulement approbation des meilleures sélections. Les fragments insuffisamment discriminants devront être rallongés ou écartés, sans quota.
