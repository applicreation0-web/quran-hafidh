# Quran Hifz — Redesign rétention, Ancrage, acquis et J10

Date: 2026-09-15
Baseline: `75d35aaf3f46877b4f6b65fe1c276cc7b3fba1a6` (Quran Hifz 0.7.4 BOOX publiée)
Branche de conception: `work/hifz-retention-j10-redesign-spec`
Statut: conception approuvée en conversation, à relire avant plan d’implémentation

## 1. Objectif

Remplacer la logique actuelle de maturation longue et difficile à comprendre par un parcours simple et continu:

`Travail intensif hebdomadaire -> validation cumulative -> acquis -> rotation permanente avec garantie J10`.

Le nouveau modèle doit:

- mieux conserver les nouvelles Leçons neuves et les nouveaux Ancrages;
- supprimer le sas de 90 jours et la Consolidation conditionnée par 36 blocs;
- séparer réellement les plages acquises des plages à ancrer;
- rendre l’Ancrage uniforme en demi-page;
- utiliser une vitesse de base Entretien de 7 s/ligne;
- fixer Entretien à 1 h de base sans blocage horaire;
- garantir qu’aucune ligne acquise ne reste plus de 10 jours sans revue;
- adapter automatiquement la priorité de rotation quand le corpus acquis grandit;
- préserver la progression existante lors de la migration.

## 2. Principes fonctionnels

### 2.1 Leçon neuve

La Leçon neuve reste un bloc de 5 lignes, les lundi, mercredi et vendredi.

La reprise du soir devient cumulative sur la semaine:

- lundi soir: S1 en boucle;
- mercredi soir: S1 + S2 en boucle;
- vendredi soir: S1 + S2 + S3 en boucle.

La reprise du soir garde une enveloppe de 30 minutes. Le nombre de passages n’est pas fixe: le lot complet est parcouru dans l’ordre, puis recommence jusqu’à la fin du temps prévu.

Après validation de la reprise cumulative du vendredi soir, S1+S2+S3 deviennent acquis. Leur date `lastReviewed` initiale est le vendredi, car c’est le dernier passage réel du lot. Ils peuvent donc entrer dans la rotation générale sans être artificiellement prioritaires le samedi.

### 2.2 Ancrage

L’Ancrage est uniformisé en demi-pages Mushaf.

Règles de découpage:

- une unité cible environ 7 à 8 lignes physiques;
- une unité ne traverse jamais une limite de sourate;
- une unité ne coupe pas artificiellement un verset;
- si une sourate se termine avant la moitié de page, l’unité s’arrête à la fin de la sourate et la sourate suivante commence une nouvelle unité;
- une unité reste contenue dans sa page physique.

Le réglage « Sourates difficiles à ancrer » est supprimé. La logique spéciale de fractionnement liée à ce réglage est supprimée.

La semaine d’Ancrage suit le même principe cumulatif:

- mardi: A1;
- jeudi: A1 + A2;
- samedi: A1 + A2 + A3.

La séance d’Ancrage reste dans l’enveloppe existante de 60 minutes, mais n’impose plus un compteur ×35/×40 comme contrat fonctionnel. Le lot courant est parcouru en boucle dans l’ordre pendant le temps disponible. La réussite/validation de la séance clôt le lot correspondant.

Après validation de la séance cumulative du samedi, A1+A2+A3 deviennent acquis avec `lastReviewed = samedi`.

### 2.3 Corpus acquis et « À ancrer »

Le modèle doit avoir deux notions distinctes et explicites:

- **Corpus acquis**: une liste de plusieurs plages déjà maîtrisées et immédiatement éligibles à Entretien/J10;
- **À ancrer**: une liste de plusieurs plages destinées au travail d’Ancrage, pas encore éligibles à Entretien.

L’écran Paramètres doit permettre de gérer plusieurs plages acquises et plusieurs plages à ancrer.

Une même couverture ne doit jamais être simultanément « acquise » et « à ancrer ». Toute tentative de chevauchement doit être refusée explicitement; aucune suppression silencieuse n’est permise.

Le mécanisme actuel où `itqanRanges` sert à la fois de plage configurée d’Ancrage et de corpus immédiatement visible par Entretien doit être remplacé par des données sémantiquement distinctes.

### 2.4 Suppression de la gare des 90 jours

Le nouveau modèle supprime du parcours fonctionnel:

- l’attente minimale de 90 jours;
- le seuil de 36 blocs avant Consolidation;
- l’assiduité dominicale 20/26 comme condition de promotion;
- la promotion forcée par dépassement de 60 blocs;
- la Consolidation dominicale historique comme sas avant Ancrage.

Ces mécanismes deviennent inutiles parce que la conservation rapprochée est assurée par la boule de neige hebdomadaire, puis la conservation longue par J10.

La migration ne doit pas perdre les blocs déjà récents, promus ou en cours. Ils doivent être reclassés selon leur état réel sans les marquer comme « revus » s’ils ne l’ont pas été.

## 3. Entretien

### 3.1 Durée et vitesse

La vitesse de base passe de 9 s/ligne à **7 s/ligne**.

Une séance d’Entretien a une durée de référence de **60 minutes**.

À 7 s/ligne, la capacité théorique d’une séance est:

`floor(3600 / 7) = 514 lignes`.

La durée de 60 minutes n’est **pas bloquante**:

- l’utilisateur peut arrêter avant;
- seules les lignes réellement parcourues sont créditées;
- à 60 minutes l’application affiche que l’objectif de base est atteint;
- l’utilisateur peut continuer au-delà;
- aucun contenu non parcouru n’est marqué comme revu;
- le curseur et le crédit exact doivent survivre à une sortie/reprise de l’application.

Pour une installation existante, une ancienne estimation non calibrée doit migrer vers 7 s/ligne. Une vitesse réellement calibrée à partir de mesures acceptées doit être préservée.

### 3.2 Rotation hybride

L’Entretien utilise une rotation hybride:

1. priorité aux lignes qui approchent leur échéance J10;
2. tout le temps restant continue la rotation normale depuis le curseur persistant.

Une préemption J10 ne déplace pas définitivement le curseur de rotation normale.

Ordre de priorité J10:

- J9/J10: prioritaire de manière systématique;
- J8: prioritaire si la capacité est en tension;
- J7: prioritaire uniquement si la prévision indique que le système deviendrait non tenable sans anticipation.

Ce comportement reprend le principe déjà présent dans `J10ReviewPolicy`.

### 3.3 Comptage réel du parcours

Le suivi d’Entretien ne peut plus se limiter à un couple `début -> fin`.

Le moteur doit comptabiliser explicitement:

- les IDs de lignes réellement parcourues;
- le nombre total de lignes créditées;
- les passages de boucle lorsque le corpus est plus petit que l’objectif de séance;
- le curseur de reprise exact;
- la date de dernière revue par ligne.

Cela évite qu’un parcours de « 1 tour + 150 lignes » soit confondu avec un simple parcours jusqu’au même verset.

## 4. Garantie J10 et capacité

Le corpus acquis entier doit rester soumis à la règle:

`date_limite(ligne) = lastReviewed(ligne) + 10 jours`.

La planification doit être deadline-aware: une capacité située après l’échéance ne peut pas masquer une insuffisance avant l’échéance.

Les séances normales comptées comme capacité J10 sont uniquement:

- mardi soir: Entretien 60 min;
- jeudi soir: Entretien 60 min;
- samedi soir: Entretien 60 min;
- dimanche soir: Entretien 60 min.

La Reprise du soir et l’Ancrage ne doivent plus être comptés comme capacité J10 réutilisable normale, car ils sont déjà occupés par leur travail propre.

Le dimanche matin devient une **réserve J10**:

- aucun travail obligatoire si la prévision est normale;
- activable si le moteur détecte tension ou non-tenabilité;
- cette réserve ajoute 60 minutes de capacité lorsqu’elle est utilisée.

### 4.1 Capacité structurelle

Avec 4 séances d’Entretien de 60 min et une vitesse de 7 s/ligne:

- capacité par séance: 514 lignes;
- capacité hebdomadaire brute: 2 056 passages de lignes;
- dans la pire fenêtre glissante de 10 jours, on dispose de 5 séances normales;
- capacité garantie dans cette pire fenêtre: environ **2 570 lignes acquises**.

Avec la réserve du dimanche matin, la pire fenêtre peut disposer de 6 séances, soit environ **3 084 lignes**.

Le système doit donc afficher une prévision de soutenabilité, par exemple:

- NORMAL: charge largement sous la capacité;
- TENSION: charge >= 80 % de la capacité pertinente;
- NON TENABLE: certaines échéances ne rentrent pas dans la capacité disponible.

La durée de 60 min reste une base, pas un plafond. Si 60 min ne suffisent plus, l’application doit le dire explicitement au lieu de prétendre que J10 est respecté.

## 5. Croissance du corpus acquis

À cadence normale:

- Leçon neuve: 3 × 5 lignes = 15 nouvelles lignes/semaine;
- Ancrage: 3 demi-pages ≈ 21 à 24 lignes/semaine;
- croissance typique: environ 36 à 39 lignes acquises/semaine.

À 7 s/ligne et avec une obligation de revue sous 10 jours, cette croissance ajoute environ 3 minutes de charge Entretien hebdomadaire récurrente par semaine de progression.

Conséquence: les 4 h hebdomadaires d’Entretien sont suffisantes au départ mais pas indéfiniment. Le moteur doit surveiller la croissance du corpus et prévenir avant saturation.

Ordres de grandeur pour respecter J10 avec cinq séances disponibles dans la pire fenêtre de 10 jours:

- 1 500 lignes: ~35 min par séance;
- 2 000 lignes: ~47 min;
- 2 500 lignes: ~58 min;
- 2 570 lignes: ~60 min;
- 3 000 lignes: ~70 min;
- 4 000 lignes: ~93 min;
- 5 000 lignes: ~117 min.

Ces valeurs servent au diagnostic et à la planification, pas à imposer un blocage horaire.

## 6. Planning hebdomadaire cible

| Jour | Matin | Soir |
| --- | --- | --- |
| Lundi | Leçon neuve S1 · 5 lignes | Reprise 30 min · S1 en boucle |
| Mardi | Ancrage A1 · demi-page | Entretien · base 60 min |
| Mercredi | Leçon neuve S2 · 5 lignes | Reprise 30 min · S1+S2 en boucle |
| Jeudi | Ancrage cumulatif A1+A2 | Entretien · base 60 min |
| Vendredi | Leçon neuve S3 · 5 lignes | Reprise 30 min · S1+S2+S3 en boucle, puis lot acquis |
| Samedi | Ancrage cumulatif A1+A2+A3, puis lot acquis | Entretien · base 60 min |
| Dimanche | Réserve J10 seulement si nécessaire | Entretien · base 60 min |

## 7. Paramètres et libellés

Les sections doivent devenir explicites:

- **Corpus acquis · plages**
- **À ancrer · plages**
- **Entretien**
- **J10**

Les libellés « Ancrage · plages » et « Corpus d’ancrage » ne doivent plus désigner des plages déjà visibles par Entretien.

Le réglage « Sourates difficiles à ancrer » est retiré de l’interface et du comportement futur.

L’écran doit pouvoir afficher:

- nombre de lignes acquises;
- vitesse active Entretien, avec distinction estimation/mesure;
- charge J10 requise;
- capacité disponible;
- état NORMAL / TENSION / NON TENABLE;
- prochain bloc menacé par J10;
- curseur de rotation Entretien;
- plages en attente d’Ancrage.

## 8. Migration 0.7.4 -> nouveau schéma

La migration doit être atomique, idempotente et testée sur des états réels 0.7.4.

Règles:

- préserver tout le `Corpus acquis` existant;
- préserver toutes les plages réellement en attente d’Ancrage;
- préserver l’Ancrage en cours sans perdre répétitions/progression déjà effectuées;
- reclasser les anciens `itqanRanges` selon leur comportement actuel: puisqu’ils sont déjà visibles dans `murajaahCorpus()`, ils migrent comme acquis, pas comme « à ancrer »;
- migrer les anciens `unconsolidatedPromotedRanges` comme « à ancrer »;
- convertir les unités d’Ancrage non commencées en demi-pages selon la nouvelle géométrie;
- conserver une unité déjà commencée jusqu’à sa validation ou son abandon explicite, puis basculer sur les demi-pages;
- préserver les dates J10 existantes lorsqu’elles existent;
- ne jamais marquer comme revue une ligne dont aucune revue réelle n’est prouvée;
- migrer une vitesse Entretien non calibrée vers 7 s/ligne;
- préserver une vitesse calibrée réelle.

## 9. Invariants de sécurité fonctionnelle

Après migration et pendant l’usage normal:

- une ligne acquise possède une date de dernière revue;
- une ligne « à ancrer » n’apparaît pas dans Entretien;
- aucune ligne n’est simultanément « acquise » et « à ancrer »;
- une validation de Leçon neuve ou d’Ancrage ne crédite que les lignes réellement couvertes;
- une sortie de séance ne crédite rien de non lu;
- le curseur Entretien reste valide après ajout/suppression de plages;
- une modification du Corpus acquis réconcilie le J10 sans créer de lignes fantômes;
- une suppression de plage acquise retire sa couverture active du planificateur J10 après confirmation explicite;
- la rotation J10 ne peut pas déclarer NORMAL si une échéance précoce dépasse la capacité disponible avant cette échéance.

## 10. Tests indispensables

### Migration

- 0.7.4 avec uniquement corpus par défaut;
- plusieurs anciennes `itqanRanges`;
- `unconsolidatedPromotedRanges` non vides;
- Ancrage en cours;
- vitesse 9 s/ligne non calibrée;
- vitesse calibrée personnalisée;
- J10 avec historique partiel et sans historique.

### Boule de neige Leçon neuve

- lundi S1;
- mercredi S1+S2;
- vendredi S1+S2+S3;
- absence un soir sans double crédit;
- reprise après process death;
- acquisition du lot uniquement après validation finale.

### Ancrage demi-page

- page standard;
- fin de sourate dans la première moitié;
- début de sourate en milieu de page;
- verset occupant plusieurs lignes;
- aucune unité à cheval sur deux sourates;
- migration d’une unité ancienne déjà commencée.

### Entretien/J10

- 7 s/ligne => 514 lignes théoriques pour 60 min;
- arrêt à 20, 45 ou 59 min crédite uniquement le réel;
- poursuite au-delà de 60 min;
- corpus plus petit que 514 lignes avec plusieurs tours;
- corpus plus grand que 514 lignes avec reprise exacte;
- préemption J9;
- anticipation J8/J7 selon tension;
- 2 570 lignes tenables avec le planning normal;
- dépassement correctement signalé;
- réserve dimanche matin intégrée uniquement lorsqu’elle est utilisée;
- Reprise du soir et Ancrage non comptés comme capacité J10 normale.

## 11. Hors périmètre

Ce redesign ne modifie pas:

- l’intégrité du Mushaf 604 pages;
- les contenus Tafsir;
- le mode Mémorisation libre;
- les fonctions audio;
- la signature/release existante 0.7.4;
- les paramètres BOOX/E-Ink hors adaptation nécessaire aux nouveaux écrans;
- les règles éditoriales de l’application.

## 12. Critère de réussite

Le redesign est acceptable uniquement si un utilisateur peut expliquer le parcours sans connaître les structures internes:

> « J’apprends mes nouvelles lignes pendant la semaine. Je les reprends cumulativement. En fin de semaine elles deviennent acquises. Je travaille mes demi-pages d’Ancrage de la même manière. Une fois acquises, toutes ces lignes entrent dans l’Entretien. L’application me fait tourner dans l’acquis et garantit qu’aucune ligne ne dépasse 10 jours sans revue. L’heure d’Entretien est un objectif, pas un blocage. Si mon corpus devient trop grand, l’application me le dit avant que J10 devienne impossible. »

Aucune implémentation ne doit commencer avant validation de cette spécification.