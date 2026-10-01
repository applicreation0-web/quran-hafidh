# Audit contradictoire — Quran Safeguard 0.10.5 — trois Tafsīr

## Périmètre

Priorité exclusive de ce chantier : **Jalalayn, Qushayrī et Qurtubī dans Plus**.

Le chantier des commentaires des Ḥikam a été abandonné pour 0.10.5 et n'est pas fusionné. La branche Tafsīr ne doit modifier ni compteur de déblocage, ni pare-feu/exclusions, ni logique Mushaf, ni corpus canonique des Ḥikam.

## Décisions conservées

### Jalalayn

- corpus approuvé inchangé ;
- 6 236 commentaires et 427 notes ;
- base SQLite SHA-256 : `26d8715a9bcecda6cb6397f0d8a530cb9404bb69ba66ed5264ed3f5b16d11a56` ;
- archive packagée SHA-256 : `824fa202ad2b47aabdc6910f4792e0c8951a5cc8a641f47a2bab70de73b90680` ;
- aucun nettoyage éditorial 0.10.5 ne réécrit ce corpus.

### Qushayrī

- anglais uniquement dans l'asset Plus ;
- arabe source exclu ;
- traduction anglaise du verset conservée ;
- commentaire anglais conservé sans synthèse ;
- couverture exhaustive des sourates 1 à 4 ;
- source PDF épinglée par SHA-256.

### Qurtubī

- quatre volumes anglais approuvés seulement ;
- 432 entrées ;
- couverture : 1:1–7, 2:1–286, 3:1–200, 4:1–22 ;
- 4:23 reste absent car il commence dans le volume 5 ;
- arabe source et contamination Sunniconnect/scan exclus ;
- chaque PDF des volumes 1 à 4 est épinglé par SHA-256.

## Défauts réellement trouvés par l'audit contradictoire

### 1. Qushayrī : 86 lignes avec traduction mais commentaire vide

Le premier audit exhaustif a arrêté la release sur une entrée Qushayrī vide. L'inspection du corpus a ensuite montré qu'il ne s'agissait pas de commentaires manquants : dans le livre, plusieurs traductions de versets consécutifs sont parfois suivies d'un **commentaire commun**.

L'ancien extracteur transformait cette structure en plusieurs lignes indépendantes : certaines lignes ne contenaient alors que la traduction anglaise et le commentaire commun était attribué uniquement à la dernière ancre.

Correction 0.10.5 :

- 806 ancres source sont toujours conservées ;
- 86 ancres uniquement traduction sont rattachées au commentaire commun réellement présent dans la source ;
- elles forment 76 plages documentaires partagées ;
- le corpus final contient 720 entrées logiques ;
- les traductions individuelles des versets regroupés sont toutes conservées et étiquetées explicitement ;
- l'audit recompte directement les 806 ancres depuis le payload final : il ne se fie pas seulement aux métadonnées ;
- aucune ligne finale avec traduction ou commentaire vide n'est autorisée.

Cette correction préserve la structure éditoriale de la source au lieu d'inventer ou de dupliquer un commentaire.

### 2. Qushayrī : mots coupés par les soft hyphens du PDF

Après un premier run techniquement vert, l'inspection contradictoire de la base reconstruite a encore trouvé des mots coupés aux changements de bloc PDF, par exemple des fragments du type `struc` / `tures` ou `aspira` / `tion`.

Cause : l'ancien extracteur supprimait `U+00AD` trop tôt. Une fois le soft hyphen effacé, il devenait impossible de distinguer une coupure de mot d'une véritable séparation entre blocs.

Correction 0.10.5 :

- chaque soft hyphen est d'abord conservé sous forme de marqueur interne ;
- les fragments alphabétiques séparés par ce marqueur sont réunis avant la normalisation finale ;
- le marqueur est ensuite supprimé ;
- le PDF épinglé contient exactement **584** occurrences observées par ce parcours ; ce compte est désormais figé dans les gates de préparation, de corpus et d'APK ;
- `U+00AD`, le marqueur interne et `U+00A0` sont interdits dans le corpus final ;
- une inspection indépendante de la base reconstruite après correction n'a retrouvé aucun des mots cassés détectés avant correction.

### 3. Qurtubī : trois espaces insécables issus du PDF

Le même contrôle textuel appliqué à Qurtubī n'a détecté aucun mot cassé, mais a trouvé trois `U+00A0` résiduels dans les quatre volumes reconstruits.

Correction 0.10.5 :

- `U+00A0` est normalisé en espace ordinaire dès l'extraction ;
- le builder échoue si un espace insécable subsiste ;
- le corpus audit et l'audit de l'APK Plus refusent également tout `U+00A0` résiduel.

### 4. Faux positif potentiel sur Jalalayn

Une première version du nouveau gate appliquait une politique d'espacement plus stricte à Jalalayn. Cela contredisait la décision de conserver le corpus approuvé strictement inchangé.

Correction : Jalalayn est gouverné par ses checksums binaires approuvés, ses comptes canoniques et des contrôles non invasifs de corruption. Les nouvelles règles de nettoyage PDF concernent Qushayrī et Qurtubī.

## Rendu Plus — justification

Le panneau Tafsīr est un renderer commun aux trois éditions. En 0.10.5 :

- le commentaire principal utilise explicitement `TextAlign.Justify` ;
- les notes Jalalayn utilisent aussi `TextAlign.Justify` ;
- la taille de texte ajustable A− / A+ et le sélecteur d'édition restent conservés ;
- la justification est vérifiée par `scripts/verify_0105_tafsir_update.py` afin qu'une future modification ne puisse pas la supprimer silencieusement.

Le contenu des corpus n'est pas modifié pour obtenir la justification : celle-ci est exclusivement un réglage de rendu Compose.

## Contrôles 0.10.5

### Source / structure

`scripts/verify_0105_tafsir_update.py` vérifie notamment :

- Jalalayn inchangé ;
- présence des trois éditions dans Plus ;
- Qushayrī 806 ancres → 720 entrées logiques ;
- 86 ancres secondaires / 76 plages communes ;
- politique des 584 soft hyphens ;
- Qurtubī limité aux quatre volumes et exclusion de 4:23 ;
- normalisation NBSP ;
- commentaire et notes rendus en `TextAlign.Justify` ;
- Light toujours sans Tafsīr.

### Base reconstruite

`scripts/audit_0105_tafsir_cleanup.py` contrôle chaque entrée pour :

- intégrité SQLite ;
- compte et unicité ;
- couverture verset par verset ;
- absence d'arabe source pour Qushayrī/Qurtubī ;
- absence de PUA, `U+FFFD`, `U+00AD`, `U+00A0`, caractères de contrôle et marqueurs internes ;
- absence d'en-têtes de page/sourate et de contamination de scan connue ;
- traductions anglaises Qushayrī conservées ;
- commentaires non vides ;
- recomptage direct des 806 ancres Qushayrī ;
- exactement 76 plages regroupées et 86 ancres secondaires ;
- volumes Qurtubī exactement `v1`, `v2`, `v3`, `v4` ;
- 4:23 absent.

### APK

`scripts/verify_plus_apk_multitafsir.py` répète les invariants critiques sur **ce qui est réellement embarqué dans l'APK Plus** :

- Jalalayn checksum/comptes ;
- Qushayrī 720 lignes avec reconstruction réelle de 806 ancres ;
- Qurtubī 432 lignes / quatre volumes ;
- couvertures ;
- longs commentaires non tronqués ;
- aucun PDF source dans l'APK ;
- aucune fuite arabe/PUA/NBSP/soft-hyphen ;
- 4:23 Qurtubī absent.

L'APK Light est contrôlé séparément pour prouver l'absence de tout Tafsīr privé.

## Gate de fusion / publication

La PR #46 doit rester non fusionnée tant que le **head final** n'a pas produit un parcours entièrement vert comprenant :

1. audit source/rendu justifié ;
2. reconstruction depuis les PDF épinglés ;
3. audit exhaustif des bases ;
4. audit source/rendu répété après reconstruction ;
5. audit de régression général ;
6. tests Light et Plus ;
7. builds release Light et Plus ;
8. preuve Light sans Tafsīr ;
9. preuve APK Plus des trois corpus et de tous les invariants ci-dessus.

Un run vert antérieur à une correction ultérieure ne suffit pas : seule la tête finale de la branche fait foi.
