# Atlas simplifié — checkpoint vérifié

Code compilé/testé : `600058942b51a5775a5dec774d321ef0a39a236d`.
Debug privé signé : `Quran-Haafidh-ATLAS-SIMPLIFIE-60005894-PRIVATE-DEBUG.apk`.
SHA-256 : `8132e5fceccf06ad1b5588f8dee2d1558993a060f5394c2c286cb07d6e0c48c1`.
Certificat inchangé : `85cd816f5213ae81f450146789c13ec476df54d7be8cc5f806decc9ed3f7061e`.

## Changement demandé

L'ancien Dialog de titre pouvait couvrir le Mushaf. Le titre complet est désormais dans une zone bornée au-dessus de la page, dimensionnée à son texte et au budget restant. Une page authentique entière remplace la paire de miniatures ; son ratio et sa taille suivent le viewport mesuré, sans recharge du WebView pour un état identique.

Les pastilles de pages remplacent les flèches redondantes ; la page active est agrandie et noire/crème, les autres pages du passage sont cernées. Cercles d'unité/passage agrandis, règle de pages arrondie, labels Pages X à Y et Passage X. Les gestes suivent le sens de Lecture et parcourent l'unité ; la bande de passages est également feuilletable. Les deux actions restantes sont commentaire et lecture. Pas de réécriture de titre ou de passage ; les chevauchements canoniques restent entiers.

## Preuves

- RED : 68f788fd, Android 38958255289, 101 tests dont un échec attendu (2 commandes attendues, 5 dans l'ancienne interface).
- Intermédiaire 136c2499 : Android 101 + 1 + 1 PASS ; inspection visuelle conduisant à réduire la hauteur des titres courts. Ce n'est pas le debug final.
- FINAL Android 38960138661 : 101/101 + préparation 1/1 + vérification après arrêt forcé 1/1, 29 captures. Contrôles de titre court/long, geste réel, retour Lecture, Al-Baqarah à deux rangées et page 7 entière.
- FINAL JVM/source 38960138983 : PASS ; XML de 3 scénarios datés 180/365/548 jours, pas 80 nouveaux scénarios.
- Compilation du drapeau privé SANS contenus 38960139016 PASS ; assemblage des 3 assets approuvés et signature locale seulement.
- Signature v2/v3, CRC, zipalign, conservation du payload pendant signature, compatibilité statique avec le debug 91890d74 : PASS. Même package/versionCode 39, minSdk 26/targetSdk 36. Les 615 assets gelés et 3 assets privés sont identiques.
- Local : 9 tests Python, contrat source, 2138 transitions Java et 16 cas du JavaScript de pagination généré. Aucun de ces tests locaux n'est présenté comme preuve physique BOOX.

## Périmètre et suite

Deux fichiers de production seulement : MawdudiMapScreen.java et MapQcfThumbnails.java. Moteurs Hifz, stockage, crédits, acquis, masques, texte arabe et Jalalayn inchangés. Aucune installation/désinstallation ou modification de données Hifz réelles. Aucun keystore, mot de passe, texte privé ou APK privé dans GitHub/CI publiques.

Runtime privé signé sur appareil, matrice exhaustive de densités/rotations, audit indépendant et BOOX physique : NON EXÉCUTÉS. Les captures CI utilisent des titres synthétiques. Le prompt d'audit distinct est prêt sur le SHA exact. RELEASE BLOQUÉE jusqu'à audit favorable, recette BOOX et sauvegarde/restauration validées. Ne pas utiliser les anciens prompts/binaires 91890d74 ou 136c2499 pour auditer ce nouveau debug.
