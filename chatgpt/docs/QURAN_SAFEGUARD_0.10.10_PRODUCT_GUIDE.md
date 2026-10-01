# Quran Safeguard 0.10.10 — Guide produit et périmètre d’utilisation

## 1. Objet de l’application

Quran Safeguard est une application Android privée de régulation volontaire du temps d’écran. Son objectif est de créer une pause consciente avant ou pendant l’usage de certaines applications choisies, en proposant une lecture réelle du Coran et, séparément, des outils de lecture, mémorisation et révision.

Le produit est conçu pour rester local autant que possible : les réglages, compteurs, signets, historique de lecture et progression Hifz sont enregistrés sur l’appareil. Le service d’accessibilité sert uniquement à détecter les applications explicitement protégées et à arrêter le compteur dès qu’elles ne sont plus au premier plan. Il ne doit pas lire ni enregistrer le contenu saisi dans les applications tierces.

## 2. Éditions Light et Plus

Deux éditions existent. Light fournit la protection, le lecteur du Muṣḥaf, la Mémorisation libre, le Parcours Hifz et les modules éditoriaux prévus pour Light. Plus ajoute le Tafsîr dans le mode Lecture/Étude uniquement.

Le Tafsîr est absent de Light. Dans Plus il reste interdit en Mémorisation libre et dans tout le Parcours Hifz. Cette séparation évite les collisions de gestes et garde l’objectif de mémorisation distinct de l’étude exégétique.

## 3. Protection des applications

La protection utilise une liste fermée de cibles. Seuls les réseaux sociaux et navigateurs suivants peuvent être sélectionnés : WhatsApp, X, Instagram, Facebook, YouTube, TikTok, Chrome, Firefox, Microsoft Edge, Brave, Opera, Samsung Internet, DuckDuckGo et Vivaldi.

Les applications bancaires, de sécurité, d’identité, de santé, de transport, de travail ou toute autre application non listée sont hors périmètre. Elles ne doivent jamais être classées, sélectionnées, journalisées ou interceptées comme cibles. Une application non ciblée n’a donc pas besoin d’une « liste d’exclusion » : elle se trouve hors du moteur de protection par construction.

Le temps est calculé uniquement lorsque l’une des applications protégées est réellement au premier plan. Quitter la cible suspend immédiatement le décompte. Passer de WhatsApp à Chrome, si les deux sont protégées, continue le même compteur global de présence cible ; passer à une application hors cible arrête ce compteur.

## 4. Règles de temps et lecture de déblocage

Le premier filtre de la journée demande 20 pages de lecture. Ensuite, le produit fonctionne sur des tranches cumulées de 15 minutes de présence réelle dans les applications cibles. Toutes les 15 minutes, une page de lecture est demandée. À 90 minutes, le sixième palier est remplacé par un bloc de 10 pages, puis un nouveau cycle commence.

Une page de déblocage doit rester active pendant au moins 60 secondes avant validation. Le temps passé en arrière-plan, téléphone verrouillé ou hors de la page de lecture ne doit pas être crédité. Après validation, l’application cible doit reprendre de façon fluide.

Les avertissements de temps restant sont sobres : à 10, 5 et 1 minute, une petite bannière et une vibration courte, sans son. Trois jokers maximum sont disponibles par jour selon la politique prévue par le produit.

## 5. Lecteur du Coran

Le lecteur affiche le Muṣḥaf de Médine canonique sur 604 pages. La page conserve ses proportions : pas de reflow du texte coranique, pas de reconstruction des lignes et pas de substitution par un texte généré. La page doit dominer l’écran, être centrée dans l’espace réellement disponible, sous la barre d’état Android et au-dessus de la navigation système.

L’interface de lecture est volontairement légère : crème et noir, commandes petites et principalement circulaires, bloc inférieur discret et transparent, curseur de page fin avec indicateur de page, disparition automatique des commandes après environ 2 à 3 secondes et réapparition par tap central. Les signets, l’historique, la navigation page/Juz/Hizb et le zoom restent disponibles sans encombrer la page.

Le lecteur normal n’affiche aucun player audio Al-Husary. L’audio est réservé à la Mémorisation libre et au Parcours Hifz.

## 6. Mémorisation libre

La Mémorisation libre reste indépendante du Parcours Hifz. L’utilisateur peut travailler un passage ponctuel sans modifier automatiquement son programme Sabqi–Itqān–Murājaʿah.

La progression utilise les vraies lignes du Muṣḥaf et un masquage déterministe. Une même étape ne doit pas changer arbitrairement de zones masquées après un rafraîchissement. Les aides et « Afficher brièvement » servent à consulter momentanément le texte mais ne valident jamais l’étape à elles seules.

Deux compteurs distincts sont conservés et affichés : « lectures sans masquage » et « lectures avec masquage ». Les écoutes audio ne sont jamais comptées comme lectures.

L’audio Al-Husary Muʿallim peut être utilisé ici avec commandes séparées. La synchronisation visuelle se fait au verset entier, jamais mot à mot. Les fichiers audio peuvent être téléchargés puis lus hors ligne. Si l’audio local devient indisponible, la mémorisation doit pouvoir continuer sans inventer de répétitions audio.

## 7. Parcours Hifz structuré

Le Parcours Hifz organise trois types de travail : Sabqi, Itqān et Murājaʿah. Les jours par défaut sont lundi/mercredi/vendredi pour Sabqi, mardi/jeudi pour Itqān et samedi/dimanche pour Murājaʿah. Ce calendrier est configurable et persistant. Une absence ne transforme pas automatiquement la séance en échec et ne double pas le quota du lendemain : la séance reste à replanifier avec son curseur et son quota.

### Sabqi

Le Sabqi utilise un apprentissage progressif. Pour le protocole de lecture de base :

- 10 lectures sans masquage ;
- 5 lectures avec masquage à 25 % ;
- 5 lectures avec masquage à 50 % ;
- 5 lectures avec masquage à 75 % ;
- 7 lectures avec masquage à 100 %.

Cela représente 10 lectures visibles et 22 lectures masquées pour le protocole de base. Les phases d’écoute Al-Husary sont distinctes des lectures et ne gonflent pas ces compteurs.

Un verset très long n’est jamais coupé arbitrairement. S’il occupe plus de cinq lignes physiques, le travail pédagogique est découpé en segments d’au maximum cinq vraies lignes du Muṣḥaf. Tous les segments conservent le même verset canonique comme cible. Finir un segment ne crédite pas une fraction du verset : une phase finale d’assemblage du passage est requise avant la clôture de la tâche.

### Itqān

Une page d’Itqān comporte exactement 30 répétitions : 20 sans masquage puis 10 avec masquage. L’implémentation répartit les 10 répétitions masquées de manière progressive : 2 à 25 %, 2 à 50 %, 2 à 75 % et 4 à 100 %.

Le masquage est donc obligatoire dans la phase finale d’Itqān, mais les 20 premières répétitions restent visibles. Les compteurs « sans masquage » et « avec masquage » sont conservés séparément.

Cas limite important : une sourate, un Juz ou une plage Itqān peut commencer au milieu d’une page. La page entière du Muṣḥaf reste affichée pour préserver son intégrité visuelle, mais seuls les versets appartenant à la cible sont actifs, masqués et crédités. Les versets hors cible visibles sur la même ligne ou la même page ne font pas partie de l’exercice. La même règle vaut pour une fin de plage au milieu d’une page.

### Murājaʿah

La Murājaʿah privilégie la fluidité de révision. Aucun masquage n’est imposé par défaut. Une récitation normale est donc comptée comme lecture sans masquage. Si une option volontaire de test masqué est utilisée ultérieurement, ces répétitions doivent être comptées séparément comme lectures avec masquage ; elles ne doivent jamais être confondues avec le quota normal.

La cadence par défaut est calibrée sur environ un Juz en 45 minutes, avec possibilité d’ajustement. La Murājaʿah couvre l’Itqān consolidé ainsi que le Sabqi récent selon la politique du parcours.

## 8. Audio Al-Husary Muʿallim

Le player Al-Husary Muʿallim est limité à deux contextes : Mémorisation libre et Parcours Hifz. Il ne doit pas apparaître ni pouvoir être déclenché depuis le lecteur normal, le Tafsîr ou les autres écrans.

Dans les contextes autorisés, les commandes audio restent séparées des gestes de sélection, de masquage et du Tafsîr. Sont prévus : lecture, pause/reprise, répétition, téléchargement et lecture hors ligne. La synchronisation est au niveau du verset entier. Un événement audio ne doit pas compter comme une lecture visuelle et ne doit progresser le protocole Hifz que lorsqu’il correspond réellement à une phase audio prévue.

## 9. Tafsîr

Le Tafsîr est une fonction Plus réservée au mode Lecture/Étude. Les éditions disponibles sont Jalalayn, Qurtubi et Qushayri lorsque la source couvre réellement le verset sélectionné. Le rendu conserve les informations éditoriales pertinentes de la source, les notes et les références coraniques explicitement identifiées.

Aucune paraphrase ou « explication simple » générée n’est injectée. Le Tafsîr ne doit jamais s’ouvrir par tap en Mémorisation libre ou en Parcours Hifz.

## 10. Bibliothèque et rappels

Le produit contient une bibliothèque spirituelle avec contenus sourcés. Les Al-Ḥikam sont conservées comme textes vérifiés avec arabe, traduction française et source ; les commentaires générés ou synthèses IA sont exclus. Les hadiths doivent être accompagnés d’une source identifiable et d’un niveau d’authenticité adapté. Les adhkār du matin et du soir comportent l’arabe vocalisé et la traduction prévue.

La pensée du jour est prévue à 20 h. Les notifications sont discrètes : petite bannière et une vibration courte, sans son ni ouverture forcée. Les plages des adhkār sont liées aux horaires locaux calculés sur l’appareil.

## 11. Appareils visés

La cible principale est Android. Deux familles d’appareils doivent être considérées comme premières classes :

1. téléphones Android classiques pour la protection, les déblocages, la lecture, la mémorisation et le Hifz ;
2. tablettes/liseuses Android E-Ink, particulièrement dans la plage approximative 7,8 à 10,3 pouces, pour une lecture et une mémorisation confortables.

Il n’existe pas de version iOS dans ce périmètre 0.10.10.

## 12. Mode E-Ink

Le profil d’affichage propose Automatique, Standard et E-Ink avec surcharge manuelle. Standard doit rester un no-op pour le comportement de rafraîchissement E-Ink. Le mode E-Ink réduit les animations inutiles, conserve les masques déterministes et déclenche un nettoyage complet uniquement lorsque le risque de ghosting le justifie, par exemple après certaines révélations ou un changement de page.

L’implémentation ne dépend pas d’une API propriétaire BOOX/Onyx : elle repose sur des mécanismes Android/WebView portables. Le nettoyage visuel reste monochrome, noir puis crème, sans flash vert ou couleur parasite. Les boutons matériels Page Up/Page Down sont pris en charge lorsque le profil E-Ink est actif.

La qualité réelle du ghosting, la vitesse de rafraîchissement, la lisibilité des masques, l’autonomie et le comportement prolongé ne peuvent pas être certifiés par un émulateur : ils nécessitent un essai sur appareil E-Ink physique.

## 13. Migration 0.10.8 → 0.10.10

La mise à jour doit être installable directement par-dessus la 0.10.8 avec la même identité de package et la même lignée de signature. Elle doit préserver les données durables prévues : applications protégées encore installées, choix Juz/Hizb, compteurs et historique de lecture, signets, rappels et autres réglages compatibles.

Une application protégée qui n’est réellement plus installée peut être retirée de la sélection lors de la réconciliation. Un test de migration doit donc installer de vraies cibles factices portant les identifiants de package attendus avant de vérifier leur conservation. Injecter dans les préférences une cible inexistante puis exiger qu’elle survive est un test invalide du comportement produit.

## 14. Cas limites à vérifier systématiquement

Les principaux cas limites produit sont : début ou fin Itqān au milieu d’une page ; ligne physique partagée par un verset cible et un verset hors cible ; verset Sabqi de plus de cinq ou quinze lignes ; changement de page pendant un masque ; révélation temporaire puis retour au même masque ; extinction/verrouillage/mise en arrière-plan pendant une séance ; reprise après interruption ; audio absent ou partiellement téléchargé ; passage d’une application protégée à une autre ; passage vers une application bancaire ou autre application hors cible ; installation simultanée Light/Plus avec protection active ; migration 0.10.8 avec données existantes ; E-Ink avec ghosting et changements rapides ; limites page 1/page 604 ; plage de sourate/Juz/Hizb non alignée sur une page entière.

## 15. Méthode de validation

Chaque preuve doit suivre la chaîne : **exigence → risque → test → preuve**. Un test statique ne prouve pas un comportement runtime. Un émulateur Android ne prouve pas le ghosting d’une liseuse E-Ink ni l’absence de latence dans une vraie application bancaire. Un build vert ne signifie donc pas automatiquement « prêt à publier ».

Les preuves automatisables sont : compilation Light/Plus, tests de logique Hifz, géométrie du Muṣḥaf, isolation Tafsîr, séparation des compteurs, scope audio, politique E-Ink, allowlist des applications, migration avec cibles réellement installées, lancement Android 15 et absence de crash/ANR au démarrage.

Les preuves qui restent physiques sont : absence de gêne/latence dans de vraies applications bancaires ou d’identité sur téléphone, transition réelle vers les applications cibles, ergonomie tactile prolongée, ghosting E-Ink, rafraîchissements, boutons matériels selon le modèle, autonomie et stabilité de longue durée.

Le candidat ne doit être gelé pour audit contradictoire externe qu’après passage des preuves automatisables et réalisation des validations physiques requises. Toute modification applicative postérieure crée un nouveau candidat et invalide au minimum les preuves directement touchées par cette modification.
