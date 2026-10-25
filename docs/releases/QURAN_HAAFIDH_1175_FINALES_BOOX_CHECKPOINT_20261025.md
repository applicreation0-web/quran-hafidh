# Quran Haafidh 1.17.5 — checkpoint BOOX et finales (25 octobre 2026)

## Périmètre et statut

Branche unique : `work/haafidh-1175-boox-ux-hifz-atlas`. Version cible : `1.17.5`, versionCode 43.
La version BOOX installée par l'utilisateur reste `1.17.4 (42)`; aucun APK officiel ni debug installable nouveau n'a été livré dans ce chantier.
**Release officielle : interdite jusqu'à accord explicite.**

### État du correctif métier déjà commité

- `27928f76` : les séances du dimanche possèdent des slots persistants distincts des séances ordinaires, avec migration atomique des anciennes finales. Aucun effacement de sauvegarde, de progression, ni des répétitions.
- `981554d1`, `af345894` : contrôles de sécurité pinés aux empreintes exactes des seuls fichiers modifiés. La géométrie QCF, les moteurs tiers, les icônes figées et le protocole post-An-Nās restent sous leurs contrôles indépendants.
- `11b3f7dd` : workflows déclenchés pour toutes modifications pertinentes du moteur.
- `36c5fb87` : test de contrat adapté à la nouvelle séparation des sessions, sans retirer son invariant de reprise.
- Simulation de migration sur copie de la sauvegarde du 25/10 : séance ordinaire `learning-2026-10-24` intacte, finale `stabilization-final-2026-10-25` déplacée byte à byte, reste de l'état préservé. Ce contrôle de copie ne remplace pas un test Android d'upgrade sur l'appareil.

### Protocole dominical demandé : spécification, NON encore appliquée

Il est impératif de distinguer les corrections de plantage déjà codées du nouveau protocole qui nécessite encore développement, tests et arbitrage technique de ses mécanismes persistants.

| Parcours | Unité quotidienne | Finales et horaires | Passages à la finale | Crédits |
|---|---|---|---|---|
| Sabqi | 5 lignes, protocole ordinaire conservé | Dimanche **matin** ; page/portion exacte constituée des lignes gagnées | ×3 par page : 1 visible, puis 2 masquées 100 % avec ancrages | Seulement Appris → Acquis |
| Itqān **avant** An-Nās | Volume hebdomadaire configurable, actuellement 3 ou 4 pages (séances 1,5 ou 2 pages) ; protocole ordinaire ×40, LIGHT selon état gelé | Dimanche **soir** ; pages/portions effectivement travaillées | ×3 par page : 1 visible, puis 2 masquées 100 % avec ancrages | Seulement Stabilisé → Acquis |
| Itqān **après** An-Nās | 1 hizb (limites réelles), ×3 : 2 visibles et 1 masquée avec ancrages | Aucune seconde finale de stabilisation pour les pages déjà Acquises | Uniquement les 3 passages du hizb ordinaire ; sessions OPEN héritées gardent leur protocole figé | Aucune nouvelle acquisition pour les lignes déjà Acquises |

- **Temps total de validation dominicale** : cible 30 minutes, plafond combiné de 40 minutes, avec sauvegarde de l'avancement et reports non crédités.
- Les rappels espacés à S−1 et S−3 sont des révisions passives ×1 masquées, pas des ajouts à la finale du dimanche ; sortir les Acquis de la fenêtre cumulative de huit semaines sans perdre le rappel ordinaire.
- Supprimer les ×3 supplémentaires sur le passage continu réunissant toutes les pages. Conserver le parcours intégral d'une page et une courte liaison masquée entre deux pages successives.
- Le bouton Valider la finale s'active uniquement après les répétitions requises. Ne pas fausser les acquis si un passage a échoué, si le temps a expiré ou si une séance ordinaire demeure ouverte.
- Un quota de 4, 3, 2, 1, ou 0 page/semaine doit rester indépendant du nombre de séances, de leurs jours, de la longueur d'une unité QCF et de l'historique déjà crédité.
- Tout choix paramétrique est daté ; conserver le volume d'une séance ouverte et ne jamais arrondir 1,5 page à 2 pages.
- **Ces points ne sont pas déclarés réalisés** par le commit de séparation des slots ; ne pas produire de debug en prétendant qu'ils le sont.

## Vérification des six photographies BOOX

Archive originale `HAAFIDH_BOOX_REPRISE_6_PHOTOS_ET_PROMPT_20261025.zip` : 6/6 empreintes JPEG SHA-256 conformes au manifeste. Les photos représentent un debug antérieur et ne prouvent pas le rendu actuel.

| Photo | Écran | Défaut de l'ancien debug | Situation du HEAD / reste à prouver |
|---|---|---|---|
| 01 | Paramètres racine | Textes et sous-lignes peu lisibles | Agrandissement 21 sp / 18 sp déjà dans HEAD ; validation réelle BOOX à faire |
| 02 | Plages & corpus | Références trop petites | Agrandissement et protection contre troncature déjà dans HEAD ; contrôler RTL/portrait physique |
| 03 | Atlas Lecture | Frises et titres minuscules | Agrandissement annulé pour préserver la géométrie QCF ; test physique prioritaire sans déplacer/étirer Mushaf |
| 04 | Progression | Légende non visible sur photo | Légende quatre états avant quadrillage 604 pages déjà dans HEAD ; valider physiquement |
| 05 | Parcours | Six grandes icônes | Layout et icônes conservés ; vérifier défilement et libellés sur BOOX |
| 06 | Accueil | Ancienne disposition 3+1 et séparateur | Grille 2×2 définitive Parcours/Progression, Lecture/Atlas en HEAD c6f3f20d + 4135df38 ; aucun séparateur ; valider rendu physique |

Le bouton rond en bord de photographie peut provenir du système BOOX, pas de l'application.

## Verrous avant debug privé installable

1. Finaliser et tester la **nouvelle grammaire de finales** ainsi que les changements de quota (4/3/2/1/0) ; retrouver si possible le patch local Work `87274656` qui n'avait pas été poussé. Sa seule description dans REPORT.md ne permet pas de reconstruire fidèlement les changements.
2. Tests du vrai JSON de sauvegarde sur copie, reprises interruption/date/semaines, migrations, masquage et ancrages, aucun saut ni double crédit ; Android émulateur distinct du BOOX physique.
3. Compiler et vérifier les artéfacts de CI. Le workflow « OFFLINE private assembly » produit **UNFINALIZED** sans assets privés ni signature historique : ne jamais le distribuer comme APK installable.
4. Pour mise à jour in-place de 1.17.4 (42) : même `applicationId=com.quransafeguard.hifz`, versionCode 43, ressources Atlas privées intégrales, signature de la même identité historique et restauration sans désinstallation.
5. Aucune release officielle sans accord explicite ; éviter l'intégration Correspondances ou la refonte Atlas.

**Décision de livraison : NO-GO tant que ces conditions ne sont pas démontrées.**
