# Quran Safeguard — continuité de signature

Date : 2026-09-03

## Lignée retenue

La clé historique à conserver jalousement est `Quran-Safeguard-release.p12`, dont le certificat SHA-256 est :

`6C:70:6F:4E:A4:4E:F6:67:D0:B9:69:8C:07:A3:9E:92:9B:1E:28:6D:23:97:66:55:04:41:1D:ED:B3:74:57:AC`

Les APK 0.3.2, 0.8.2 et la version release retenue portent ce même certificat. C’est la lignée utilisée pour signer le candidat 0.10.1.

Le candidat 0.10.1 vérifié mesure 71 981 397 octets et porte le SHA-256 `43c3d61e96ee23eb2c289266d99ff6dcb2da7898a92c88aa30dc0283b1c3a1f2`. Sa signature APK v2/v3 contient un seul signataire et retrouve exactement le certificat historique ci-dessus.

## Anomalie conservée comme preuve

L’artefact nommé `Quran-Safeguard-0.9.0-first-install-release.apk` observé dans les fichiers porte un autre certificat, `9C:66:DE:17:…:E7`. Cette anomalie ne doit ni remplacer ni faire supprimer la clé historique. Un téléphone installé depuis cet artefact précis demanderait une réinstallation propre ou la clé correspondante.

## Règles

1. Never commit `Quran-Safeguard-release.p12`, son alias ou ses mots de passe.
2. Vérifier le certificat du nouvel APK après signature.
3. Conserver l’identifiant `com.applicreation0.quransafeguard`.
4. Tester l’installation par mise à jour sur l’appareil avant toute diffusion.
5. La production d’un APK local de test n’autorise aucune publication.

## Lignée Plus

Quran Safeguard Plus utilise une clé PKCS12 distincte nommée
`Quran-Safeguard-Plus-release.p12`. Son certificat SHA-256 est :

`18:43:D2:45:83:25:49:AF:C6:33:3A:B7:7A:58:76:49:A1:4F:ED:B1:F4:AC:C9:C0:8F:36:7C:3F:03:01:D9:89`

Cette clé ne doit jamais signer Light et ne doit jamais être commitée.
