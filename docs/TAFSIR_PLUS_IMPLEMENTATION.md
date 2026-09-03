# Quran Safeguard Plus — contrat d’implémentation du tafsir

Base : `v0.10.1`, commit `9864ea17f75f2ef9eb706fa29d7d8e94f3baa080`.

## Séparation des éditions

- `light` conserve l’identifiant `com.applicreation0.quransafeguard`, le nom
  **Quran Safeguard**, la signature et le canal de distribution existants.
- `plus` utilise `com.applicreation0.quransafeguard.plus`, le nom
  **Quran Safeguard Plus**, une signature et un canal privés distincts.
- Le PDF source et la base SQLite n’appartiennent jamais à `src/main` ou
  `src/light`. La CI et les invitations construisent et désignent explicitement
  Light.
- Si les deux services d’accessibilité sont activés, les deux éditions suspendent
  l’interception et demandent à l’utilisateur d’en désactiver une.

## Interaction du lecteur Plus

- Un tap direct sur les mots ou la médaille d’un verset ouvre immédiatement le
  commentaire anglais et surligne tous ses polygones en vert clair translucide.
- Le pont WebView est limité à l’origine locale approuvée et ne transmet que la
  page, la sourate et le verset. Le commentaire reste natif, hors DOM et hors
  réseau.
- Le panneau part du bas, s’adapte au contenu et ne dépasse pas 50 % de l’écran.
  Le texte long défile à l’intérieur du panneau.
- Seul Retour ferme le panneau pendant l’usage normal. Il n’existe ni croix,
  ni fermeture extérieure, ni geste de rabattement.
- Un autre verset visible remplace directement le commentaire. Les notes sont
  repliées à chaque sélection et le commentaire repart du haut.
- `A−` et `A+` règlent uniquement le commentaire et les notes; la valeur est
  persistante et bornée.
- La page se déplace seulement du minimum nécessaire pour garder le verset au-
  dessus du panneau, puis retrouve sa position précédente à la fermeture.
- La navigation horizontale et les boutons de changement/sortie sont désactivés
  tant que le commentaire est ouvert.
- Le chronomètre de lecture continue pendant la réflexion. Il s’arrête uniquement
  selon les règles existantes de perte du premier plan, écran éteint ou page
  indisponible. Atteindre 60 secondes ne ferme ni ne valide automatiquement.
- Une interruption ferme le commentaire, retire la surbrillance et conserve la
  page ainsi que le temps valablement acquis.

## Corpus

Le script `scripts/extract_jalalayn_sqlite.py` extrait uniquement les 6 236
entrées numérotées `[sourate:verset]`. Les introductions, préambules,
bibliographies et basmala non numérotées sont exclus. Les 427 notes appelées par
un verset sont conservées, repliées par défaut. Les styles gras/italique et
l’Unicode sont stockés sous forme de runs structurés.

La césure InDesign U+00AD est retirée; le trait réel U+002D est conservé. La
génération échoue si les 6 236 clés canoniques ne sont pas présentes une seule
fois, si une entrée est vide, si une note n’est pas résolue, si un caractère de
remplacement/privé apparaît ou si l’intégrité SQLite échoue.

Le runtime copie la base vers `noBackupFilesDir` de façon atomique, vérifie son
SHA-256 et l’ouvre en lecture seule. Une entrée absente ou invalide affiche
exactement `Commentary unavailable for this verse.` sans utiliser un verset
voisin.

## Barrières de sortie

- La CI compile les deux variantes. Les invitations et les artefacts de partage
  public désignent exclusivement Light; Plus reste dans le dépôt privé.
- `assemblePlusDebug` et `assemblePlusRelease` échouent sans la base privée
  vérifiée.
- `scripts/verify_light_apk_no_tafsir.py` inspecte l’APK Light décompressée et
  refuse toute base, route, chaîne ou empreinte Plus.
- Avant usage réel : test WebView/téléphone obligatoire pour tap, pincement,
  swipe, Retour, TalkBack, repositionnement, reprise et chronomètre.
