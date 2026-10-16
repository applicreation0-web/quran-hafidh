# CP05 — une passe mesurée, 16 octobre 2026

Entrée : 4992ccd1410076c42edf95bfe2bc8e69a808b2b0. Sortie : commit portant ce document.
Captures publiques Android inspectées : audit-map114-synthetic-graph-qcf.png, audit-map114-synthetic-reader-focus.png, audit-map114-synthetic-return.png ; run 38889877569 PASS 100/100. Axe graphique horizontal, titres au-dessus des clés Lucide existantes, vrai QCF et focus visible. Pas de liste/recycler/cartes de texte. Barres compactes 44 dp ; axe 78 dp masqué en mode focus ; positionnement instantané, sans animation de défilement.

La revue des icônes a trouvé un échec du parseur statique (receveur nommé host non reconnu). Renommage en activity, sans changer une icône ni assouplir le contrôle : verify_j6_icon_roles.py PASS 23 vecteurs, 66 actions.

État : PARTIAL. Les marges intrinsèques du Mushaf restent gelées. Les cibles 82 % / ~91 % BOOX ne sont pas mesurées sur matériel ; aucune certification de lisibilité/flicker e-ink. BOOX PHYSIQUE NON TESTÉ. Pas de refonte spéculative supplémentaire.
Reprise : recette propriétaire portrait BOOX sur le livrable privé, puis seulement correction justifiée par mesure.
