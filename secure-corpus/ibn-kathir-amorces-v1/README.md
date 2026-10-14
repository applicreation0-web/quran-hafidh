# Corpus Amorces M7-B — stockage public chiffré

Ce dossier ne contient aucun titre en clair et aucune clé.

`amorces-m7b-1903.qhaead` est un conteneur AES-256-GCM authentifié par
`tools/amorce-aead/AmorceAeadTool.java`. La clé brute de 32 octets est remise
séparément au propriétaire et ne doit jamais entrer dans Git, une CI publique
ou l’APK. Le déchiffrement sert uniquement à préparer une construction privée ;
le runtime Android ne reçoit pas la clé.

Le manifeste public fixe les comptes attendus et le SHA-256 du conteneur. La
preuve privée de reconstruction compare ensuite les huit chunks déchiffrés au
corpus M7-B approuvé : 1 903 groupes, 1 708 titres et 195 absences.

## Commandes locales

```bash
java tools/amorce-aead/AmorceAeadTool.java keygen /chemin/prive/amorces.key
java tools/amorce-aead/AmorceAeadTool.java encrypt /chemin/prive/amorces.key source.zip amorces.qhaead
java tools/amorce-aead/AmorceAeadTool.java decrypt /chemin/prive/amorces.key amorces.qhaead restored.zip
```

Les sorties existantes ne sont jamais écrasées. Une altération du conteneur ou
une mauvaise clé fait échouer l’authentification avant toute écriture du ZIP.
