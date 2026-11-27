# Glossaire de travail et design

Les propositions wolof sont destinées à une revue humaine. Le sens d’un terme traditionnel et son adéquation à un protocole de l’application doivent être examinés ensemble.

| Français | English | Wolof à examiner | Base |
|---|---|---|---|
| Révision | Review | Nafar | MEN 2020, p. 29 |
| Révision active | Recall practice | Nafar gumba | Proposition prioritaire de l’utilisateur. Usage en daara et graphie de l’expression à confirmer |
| Mémoriser | Memorize | Mokkal | UCAD 2021, forme *mokal*, et terminologie des daaras |
| Réciter | Recite | Tari | MEN 2020, p. 28 |
| Renforcement | Reinforcement | Ndëgëral | MEN 2020, p. 29. Vérifier l’emploi pour le protocole Sabqi |
| Répéter | Repeat | Baamtu | MEN 2020, p. 29 |
| Continuer ou reprendre | Continue or resume | Wéyal | MEN 2020, p. 10 |
| Relecture | Read-through review | Njàngaat | MEN 2020, p. 29 |
| Travailler une fragilité | Work on a weak spot | Njoyyanti | MEN 2020, p. 29, remédiation |
| Perfectionnement | Refinement | Gëndel | Diakhaté et Samb, extrait indexé sur le perfectionnement d’un élève |
| Progression traditionnelle dans l’ordre canonique | Canonical-order stage | Ñajji | Diakhaté et Samb, extrait indexé, usage historique |
| Méthode de travail ou parcours | Study method or plan | À valider | Les termes méthodologiques et le programme de séances sont deux libellés distincts |
| Consolidation approfondie, consolidation cumulative, maintien | Intensive consolidation, cumulative consolidation, retention maintenance | À valider séparément | Trois disciplines distinctes |
| Amorces et ancrages | Recall cues and anchors | À valider séparément | Distinguer l’indice de récitation et le repère mémoriel |
| Masquer et dévoiler | Hide and reveal | Nëbb / Wone | Propositions lexicales à valider en situation |
| Validation, acquis, progression et report | Confirmation, confirmed memorization, progress and defer | À valider | Préserver le sens de chaque action et de chaque état |
| Paramètres, sauvegarde et restauration | Settings, backup and restore | À valider | Confirmer des commandes naturelles sur smartphone |

- [Terminologie bilingue du ministère de l’Éducation nationale du Sénégal, 2020](https://senprof.education.sn/PROGRAMME%20LECTURE%20POUR%20TOUS/documentation/Terminologies/Terminologie%20wolof%20fusion.pdf)
- [Techniques d’enseignement dans les daaras traditionnels, UCAD, 2021](https://fastef.ucad.sn/liens/LIEN32/liens32_article14.pdf)
- [Khassim Diakhaté et Moussa Samb, enseignement arabo-islamique au Sénégal](https://dspace.univ-guelma.dz/jspui/bitstream/123456789/14083/1/ARTICLE%2030.pdf), extrait indexé lors de l’étude.
- [Journal officiel du Sénégal, n° 6280, décret d’orthographe wolof, p. 539](https://www.vie-publique.sn/documents/14178/JO-6280-du-27-mai-2006)

## Repères visuels à qualifier

- Conserver le Mushaf QCF V2 à 604 pages, la géométrie, le centrage et la barre d’état.
- Garder les sept commandes de Fragilités accessibles avec des cibles tactiles de 48 dp. Tester l’écran de 320 dp, le dévoilement ciblé et la distinction entre validation et report.
- Tester la palette crème, noir et gris, les grands caractères et les glyphes wolof. Réduire les rafraîchissements sur BOOX.
- La capture synthétique de référence mesure 1 080 × 2 400 px. Le folio couvre 883 × 1 407 px, soit 81,8 % de la largeur et 47,9 % de la surface totale. Le point de contrôle n’a pas de mesure équivalente sur la nouvelle branche.
- [Accessibilité Android](https://developer.android.com/design/ui/mobile/guides/foundations/accessibility) · [Langues par application](https://developer.android.com/guide/topics/resources/app-languages) · [Écran e-ink BOOX](https://help.boox.com/41d97/838da/cd623)

## Contrôle des sources

- Le décret fixe la graphie et la séparation des mots. Il ne fixe pas les noms des disciplines de mémorisation.
- La terminologie du MEN atteste le sens pédagogique général de *nafar*, *tari*, *baamtu* et *ndëgëral*. Le passage à une discipline coranique spécialisée reste une décision éditoriale.
- Les entrées de *Ay Baati Wolof* et du dictionnaire de Fal, Santos et Doneux n’ont pas été consultées intégralement. Les catalogues en documentent les références bibliographiques. [Institut français du Sénégal](https://mediatheque.ifs.sn/index.php?id=33889&lvl=notice_display) · [Université de Columbia](https://guides.library.columbia.edu/wolof-language)
- La recherche de l’expression exacte *nafar gumba* n’a pas fourni d’attestation de daara vérifiable. Pour les écrans d’essai, son statut reste « proposition ». Faire distinguer par les relecteurs le titre d’une séance, la récitation sans support et la commande qui masque le folio.
- Les ressources `values-wo` sont des libellés de développement. Revoir en contexte la longueur des commandes et la lisibilité des formes accentuées sur écran étroit.
