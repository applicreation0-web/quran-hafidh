# Quran Safeguard 0.10.10 — paquet d’audit contradictoire externe

## Mission

Auditer indépendamment le candidat exact Quran Safeguard 0.10.10. Ne pas valider un comportement uniquement parce qu’un test affiche PASS. Pour chaque conclusion utiliser la chaîne **exigence → risque → test/preuve → verdict**. Chercher activement les faux positifs, tests trop faibles, régressions, collisions de gestes, erreurs de persistance, violations Light/Plus, écarts au contrat produit et défauts runtime invisibles aux tests statiques.

Le SHA exact du candidat doit être celui indiqué par le run final et par `candidate-sha.txt` dans l’artefact du run. Refuser l’audit si les APK, le code source ou les preuves ne correspondent pas à ce même SHA.

## Périmètre bloquant

### Protection

Seuls 14 packages de réseaux sociaux/navigateurs sont des cibles. Les banques, applications de sécurité, d’identité et toute application hors allowlist ne doivent jamais être protégées, classées ou persistées. Vérifier que la présence d’une application bancaire installée ne la fait pas entrer dans le scope. La preuve CI n’est pas suffisante pour conclure à l’absence de latence sur une vraie application bancaire : ce point exige un appareil physique.

Le budget est un cumul de présence réelle au premier plan dans les cibles : 15 minutes par palier, six paliers pour 90 minutes. Le compteur doit s’arrêter hors cible, ne pas être recrédité dans la journée et ne pas repartir à zéro lors d’un changement entre deux cibles protégées.

### Lecture de déblocage

Muṣḥaf de Médine canonique, page entière, au moins 60 secondes actives avant validation. Le temps en arrière-plan ou écran verrouillé ne compte pas. Le premier filtre quotidien est 20 pages ; le palier 90 minutes est 10 pages. Après validation, retour fluide vers la cible.

### Lecteur et visuel

Identité finale : crème + noir uniquement, avec gris neutres/transparence. Aucun vert, or ou brun hérité ne doit produire de pixel actif. L’icône est un Coran/rehal très abstrait, moderne et minimal, adaptive icon crème/noir.

Le Muṣḥaf doit dominer l’écran : page centrée, barre d’état Android intacte, contrôles bas légers, principalement circulaires, curseur fin intégré, commandes qui disparaissent après environ 2–3 secondes et reviennent par tap. Rechercher les gros panneaux/cartes, densité excessive et collisions avec les insets.

### Audio Al-Husary Muʿallim

Le player ne doit être accessible que dans **Mémorisation libre** et **Parcours Hifz**. Il ne doit ni apparaître ni pouvoir être déclenché depuis le lecteur normal ou le Tafsîr, y compris via un appel JS forgé si le bridge natif peut le bloquer.

Dans les modes autorisés : lecture, pause/reprise, répétition, téléchargement et lecture hors ligne ; synchronisation au verset entier uniquement, jamais mot à mot. Les écoutes ne doivent pas être comptées comme lectures visuelles. Une phase audio Hifz ne doit progresser que lorsqu’une lecture audio correspondant réellement au passage et à l’étape est terminée.

### Mémorisation libre

Indépendante du Parcours Hifz. Aucun Tafsîr. Masquage déterministe, aide/révélation temporaire sans validation automatique. Les compteurs persistants « sans masquage » et « avec masquage » doivent rester séparés. Vérifier la compatibilité des anciennes sessions qui ne possèdent pas encore ces champs.

### Parcours Hifz

Sabqi : 10 lectures visibles, puis 5×25 %, 5×50 %, 5×75 %, 7×100 %. Les phases audio sont séparées des compteurs de lecture. Un long verset est segmenté uniquement selon les vraies lignes du Muṣḥaf, maximum cinq lignes par segment ; terminer un segment ne crédite pas une fraction du verset et une phase d’assemblage est obligatoire pour une cible multi-segments.

Itqān : exactement 30 répétitions par page = 20 sans masquage + 10 avec masquage. Répartition actuelle des 10 masquées : 2×25 %, 2×50 %, 2×75 %, 4×100 %. Si une sourate/plage commence ou finit au milieu d’une page, la page entière reste visible mais seuls les versets dans la cible sont actifs/masqués/crédités. Vérifier aussi une ligne physique partagée entre cible et hors cible.

Murājaʿah : aucun masquage imposé. La séance normale est visible. Le quota/temps reste distinct du protocole Itqān. Les lectures visibles et masquées restent des métriques distinctes si une fonction volontaire de masquage existe plus tard.

Jours par défaut : lundi/mercredi/vendredi Sabqi ; mardi/jeudi Itqān ; samedi/dimanche Murājaʿah. Les jours/plages sont configurables et persistants. Une absence doit permettre une replanification souple sans échec ni double quota automatique.

### Tafsîr

Fonction Plus uniquement en Lecture/Étude. Light : aucun Tafsîr. Aucun Tafsîr en Mémorisation libre ou Hifz dans Light ou Plus. Jalalayn/Qurtubi/Qushayri uniquement lorsque la source couvre réellement le verset. Pas de paraphrase IA ni « explication simple » générée.

### E-Ink

Profils Automatique/Standard/E-Ink avec surcharge manuelle. Standard doit rester un no-op pour la logique de refresh. E-Ink : animations inutiles supprimées, masque stable, nettoyage différé/coalescé sans perte du retour de révélation, rendu noir/crème, aucun flash vert, aucune dépendance API propriétaire BOOX/Onyx. Vérifier boutons matériels Page Up/Page Down quand le profil E-Ink est actif.

La CI ne peut pas prouver le ghosting, l’autonomie ou le confort réel. Exiger une preuve sur liseuse/tablette Android E-Ink physique, idéalement 7,8–10,3 pouces.

### Migration

La preuve utile est une migration directe 0.10.8 → 0.10.10 avec la même lignée de signature et avec de vraies cibles factices **installées** sur l’émulateur avant injection des préférences. Vérifier préservation des cibles installées, Juz/Hizb, compteurs/historique, signets, rappel, schéma et version. Une cible absente peut légitimement être réconciliée et supprimée : ne pas transformer ce comportement en faux bug.

## Preuves attendues du run final

Le run final doit produire : SHA exact du candidat ; hash SHA-256 des APK Light/Plus ; build Light/Plus ; tests unitaires/logiques ; test JS Mémorisation ; contrat produit crème/noir/audio/Hifz/E-Ink ; Android 15 launch/smoke ; migration 0.10.8→0.10.10 avec fixtures WhatsApp et Chrome installées ; preuve qu’un package fixture bancaire installé reste hors sélection ; archive du code source exact ; guide produit.

## Limites de la preuve automatisée

Ne pas conclure « release-ready » à partir de la CI seule. Restent physiques : latence/interférence sur vraies applications bancaires/sécurité/identité, transitions réelles vers les applications cibles, ergonomie tactile, comportement long, ghosting/rafraîchissement/autonomie E-Ink et particularités de boutons matériels selon modèle.

## Format de verdict demandé

Pour chaque bloc : PASS / FAIL / NON PROUVÉ, avec la preuve exacte et le risque résiduel. Un FAIL doit être rattaché à une exigence produit. Un test qui ne mesure pas le comportement attendu n’est pas une preuve. Verdict global final : GO uniquement si aucun blocker réel n’est ouvert et si les validations physiques obligatoires sont présentes ; sinon NO-GO avec liste minimale des blockers réellement démontrés.
