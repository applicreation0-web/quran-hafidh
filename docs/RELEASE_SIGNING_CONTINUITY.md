# Quran Safeguard — continuité de signature

Date : 2026-09-02

## Lignée retenue

La clé historique à conserver jalousement est `Quran-Safeguard-release.p12`, dont le certificat SHA-256 est :

`6C:70:6F:4E:A4:4E:F6:67:D0:B9:69:8C:07:A3:9E:92:9B:1E:28:6D:23:97:66:55:04:41:1D:ED:B3:74:57:AC`

Les APK 0.3.2, 0.8.2 et la version release retenue portent ce même certificat. C’est la lignée utilisée pour signer le candidat 0.10.0.

## Anomalie conservée comme preuve

L’artefact nommé `Quran-Safeguard-0.9.0-first-install-release.apk` observé dans les fichiers porte un autre certificat, `9C:66:DE:17:…:E7`. Cette anomalie ne doit ni remplacer ni faire supprimer la clé historique. Un téléphone installé depuis cet artefact précis demanderait une réinstallation propre ou la clé correspondante.

## Règles

1. Never commit `Quran-Safeguard-release.p12`, son alias ou ses mots de passe.
2. Vérifier le certificat du nouvel APK après signature.
3. Conserver l’identifiant `com.applicreation0.quransafeguard`.
4. Tester l’installation par mise à jour sur l’appareil avant toute diffusion.
5. La production d’un APK local de test n’autorise aucune publication.
