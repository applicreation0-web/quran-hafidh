# Quran Haafidh 1.17.5 — périmètre approuvé et plan de livraison

Date : 2026-10-23. Statut : **PLAN INITIAL — PAS UNE IMPLÉMENTATION NI UN APK**.

## Référence et protection

- Dépôt : `applicreation0-web/quran-hafidh`.
- Branche de travail : `work/haafidh-1175-boox-ux-hifz-atlas`.
- Référence figée au départ : `103598d4317e474b321ceab98b27afe38c21d38c` (documentation / sources 1.17.4).
- Source du build privé officiel documenté : `8e7b132d7e075bc3e6f8f1eb6216b48e7178c95e`.
- Conserver sans modification les corpus privés, les 604 pages QCF, les décisions éditoriales Atlas (366 Unités, 1 053 Passages), les acquis Hifz, les migrations V6, les règles de stabilisation, les icônes figées, les ancrages exacts et les fonctionnalités déjà validées.
- Ne pas modifier l'audio (code, fichiers audio ou flux de lecture) ni réintroduire Correspondances. Les tests de non-régression peuvent les inspecter en lecture seule.
- Ne pas copier de ressources privées, de keystores, de signatures ou de contenus sous droits dans le dépôt public.
- Ne pas publier ni installer d'APK sans accord explicite du propriétaire. Sauvegarder les acquis avant tout test de migration.

## Ordre d'exécution et conditions de sortie

### CP0 — Baseline et tests de sécurité de changement
- Photographier l'état des écrans actuels (ou données instrumentées si captures BOOX indisponibles), mesurer tailles sp/dp, scroll, densité et zones tactiles.
- Repérer `MainActivity.java`, `Ui.java`, `WeeklyDashboardPlanner.java`, le rendu QCF, les méthodes de masquage et la navigation Atlas.
- Exécuter les tests disponibles sur les moteurs préexistants, enregistrer leurs numéros d'exécution et le SHA testé.
- Construire un tableau explicite des invariants, des anomalies reproductibles et des contrôles physiques impossibles en CI.
- Sortie : zéro modification du comportement applicatif et base de comparaison référencée.

### CP1 — Accueil et parcours BOOX (refonte UX ciblée)
- Accueil : séance du jour dominante, trois destinations principales (Parcours / Lecture / Atlas), accès secondaires Libre / Progression, Paramètres discrets.
- Parcours : trois modes principaux visibles immédiatement (Apprentissage, Stabilisation, Révision), Renforcement/Consolidation conservés mais secondaires, pas de parcours supprimé.
- Planning : rail de sept jours ; détail du jour sélectionné seulement, informations réelles issues du moteur existant, sans états ou crédits déduits de l'UI.
- Supprimer les pavés et longues listes de l'écran d'accueil, pas de sous-écran supplémentaire avant ouverture de Lecture/Atlas.
- Sortie : tous les accès et les routes existants atteignables ; aucun affichage réduit/tronqué sur BOOX portrait ; test automatique des destinations.

### CP2 — Lisibilité, densité et E-Ink
- Cibles de conception à mesurer physiquement : actions 20–22sp, corps 17–18sp, titre principal 24–26sp, zones tactiles >=56dp lorsque possible.
- Remplacer les corps de 11–12.5sp du planning principal ; normaliser le contraste, le wrapping et les états indisponibles (ne pas s'en remettre à alpha=0.42).
- Contrôler polices/scaling Android, facteur d'accessibilité, portrait E-Ink, hauteur disponible, écrans système, rafraîchissements.
- Sortie : captures au format natif et revue BOOX ; aucune action coupée, hors écran ou indiscernable.

### CP3 — Masques QCF et ghosting
- Vérifier le premier rendu déjà masqué, le calcul géométrique avant affichage, la couverture des mots/signes et les trous des ancrages autorisés.
- Tester page suivante/précédente, reprise d'activité, changement de sous-mode, 5 activations successives, révision active, apprentissage et lecture libre.
- Ne pas improviser de trous sans coordonnées exactes ; signaler les données insuffisantes, préserver le Mushaf non modifié.
- Sortie : tests automatisés pertinents PASS et validation visuelle BOOX sans flash de texte ni traces résiduelles.

### CP4 — Fiabilité des moteurs Hifz
- Vérifier 1/3–2/3–3/3 en parallélisme, crédits différés, Sabqi, Itqān, révision active/passive et consolidations.
- Post-An-Nās : Itqān un hizb et x3 (visible/visible/masqué, ancrages conservés) ; préserver les séances x4 déjà ouvertes et le premier passage x40.
- Contrôler frontières de sourates/hizb, jonction Qāf→adh-Dhāriyāt, plan sur 7 jours, restauration, absence de doubles crédits.
- Simulations ciblées 180/365/548 jours si outillage disponible ; conserver précisément les preuves des tests exécutés.
- Sortie : invariants vérifiés, aucune nouvelle promotion anticipée ni perte d'acquis.

### CP5 — Atlas navigation et cohérence
- Carte : Unités uniquement ; titres approuvés non réécrits. Passage et navigation frise uniquement dans Lecture ; panneau Jalalayn adjacent au Mushaf.
- Aligner le titre, la sélection Ayah Themes, le surlignage QCF, l'ancrage non encadré et le retour au même emplacement.
- Contrôler typographie des noms translittérés avec arabe entre parenthèses, absence de numéros de versets dans les sous-titres, artefacts textuels, multi-pages et points QCF.
- Sortie : tests de limites sur 114 sourates, tests de restauration/navigation, puis contrôle BOOX.

### CP6 — Améliorations de mémorisation complémentaires
- Rappel actif des titres d'Unités : cacher/révéler au toucher, jamais une modification du titre enregistré ni un crédit Hifz.
- Bilan hebdomadaire : données effectives du ledger et moteurs, distinguer prévu/accompli/reporté, aucune inférence de maîtrise à partir du temps.
- Interface légère, facultative et réversible ; ne pas introduire un nouveau flux de cartes/lists imposé sur l'accueil.
- Sortie : tests de séparation entre lecture du bilan/rappel et écritures Hifz.

### CP7 — Audit contradictoire et candidat privé
- Refaire tests JVM et Android instrumentés, migration 1.17.1→candidat, résilience au redémarrage, intégrité des sept ressources privées QCF/Atlas, contraste et audit d'icônes.
- Comparer APK et source ; afficher versionCode, appId, certificat, SHA-256 et provenance exacts. Ne pas confondre packaging statique et mise à jour physique.
- Tester BOOX physique avec preuve des défauts résolus (captures avant/après) et liste des cas restés non exécutés.
- Livrer rapport GO/NO-GO et candidat privé uniquement sur autorisation. Pas de release finale publique.

## Politique de commits

- Un checkpoint/commit autonome par chantier terminé, tests associés au même SHA.
- Si un test critique échoue, corriger avant la prochaine étape ; rollback limité au chantier.
- Ne pas déclarer « corrigé » sans preuve de code ou test attachée.
