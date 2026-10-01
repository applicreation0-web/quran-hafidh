# Quran Safeguard 0.10.5 — audit banque / sécurité / identité

## Décision

La frontière de confidentialité prime sur l'exactitude absolue du compteur lors d'une transition Android silencieuse vers une application totalement hors périmètre.

À partir de 0.10.5, `TargetPresenceScopePolicy.requiresAnonymousExitSentinel()` retourne littéralement `false`. Quran Safeguard ne demande donc plus, même temporairement, un flux Accessibility non filtré pour détecter la première fenêtre extérieure.

## Périmètre autorisé

Le service Accessibility reste limité à :

- Quran Safeguard ;
- les applications sociales / navigateurs explicitement sélectionnés ;
- System UI ;
- le launcher courant, uniquement comme signal de transition.

Aucune application bancaire, de sécurité, d'identité, de santé, de transport ou de travail n'est classifiée ou ajoutée au périmètre.

## Interdictions maintenues

- pas de `QUERY_ALL_PACKAGES` ;
- pas de `PACKAGE_USAGE_STATS` / `UsageStatsManager` ;
- pas de `READ_PHONE_STATE` ;
- `canRetrieveWindowContent=false` ;
- pas de `FLAG_RETRIEVE_INTERACTIVE_WINDOWS` / énumération des fenêtres ;
- pas d'Internet ;
- aucun package hors périmètre journalisé ou persisté.

## Conséquence connue

Android ne fournit pas de callback fiable « cette application filtrée vient de quitter le premier plan » sans observer la fenêtre suivante. Une transition directe et silencieuse d'une cible vers une application exclue peut donc ne pas fournir immédiatement un signal de sortie à Safeguard.

Ce compromis est accepté pour 0.10.5 : il est préférable de sous/retarder ponctuellement la comptabilisation plutôt que de recevoir un événement Accessibility d'une banque ou d'une application sensible.

## Gates

`scripts/verify_0105_sensitive_scope.py` bloque la release si le sentinel n'est plus un `false` littéral ou si une voie alternative de visibilité large est introduite.
