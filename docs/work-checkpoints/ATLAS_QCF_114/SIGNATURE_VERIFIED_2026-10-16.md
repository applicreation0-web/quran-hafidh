# Debug Atlas — signature vérifiée

Code : `91890d7412821103113c5faf98f3aafd750643bb`.
APK privé signé : `Quran-Haafidh-ATLAS-91890d74-PRIVATE-DEBUG.apk`.
SHA-256 : `5e06fb6d343b9173bf203991437fef94d6ed511262e8ddfc4aaca64f9c08a3f6`.
Certificat SHA-256 identique au Soleil sauvegardé : `85cd816f5213ae81f450146789c13ec476df54d7be8cc5f806decc9ed3f7061e`.

- PASS : clé fournie déverrouillée, certificat comparé avant signature ; signatures v2/v3 valides.
- PASS : CRC, zipalign 4, contenu applicatif inchangé pendant la signature ; 615 assets gelés identiques à Soleil, 604 SVG, trois assets privés épinglés inchangés.
- PASS statique : package `com.quransafeguard.hifz.ayahthemes114private`, versionCode 39 et versionName identiques, minSdk 26/targetSdk 36, permissions/sharedUserId inchangés.
- NON EXÉCUTÉ : installation/mise à jour sur BOOX réelle et lecture du certificat actuellement installé ; runtime du nouvel APK privé signé, audit contradictoire indépendant, recette physique et restauration.
- PREPARÉ : prompt d'audit distinct READ-ONLY sur l'APK et le SHA exacts ; aucun audit PASS revendiqué.

Les CI Android 38952593226 (101 + 1 + 1), JVM/source 38952593472 et compilation sans données 38952593215 sont les preuves du code 91890d74 avant signature ; elles ne sont pas présentées comme un test du runtime privé signé. Aucun build ou scénario longitudinal relancé.

Aucune installation, désinstallation ni modification de progression. Aucun secret ou corpus privé transmis à GitHub/CI. Rapport et APK livrés hors du dépôt. Ne jamais joindre la clé ou le mot de passe à l'audit. Release privée BLOQUÉE jusqu'aux validations indépendantes et physiques.
