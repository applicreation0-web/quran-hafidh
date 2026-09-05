# Audit contradictoire — Quran Safeguard 0.10.5 — trois Tafsīr

## Verdict actuel

**NON FUSIONNÉ / NON PUBLIABLE tant que le workflow 0.10.5 n'a pas été exécuté avec succès.**

La revue statique du diff est satisfaisante après correction d'un faux positif potentiel sur Jalalayn. L'exécution de reconstruction des PDF et l'audit APK restent des conditions obligatoires, non remplacées par cette revue.

## Périmètre contrôlé

La branche `fix/0.10.5-tafsir-formatting` part exactement de la base 0.10.4 et ne modifie aucun code métier de l'application. Le diff ajoute uniquement :

- `.github/workflows/android-0.10.5-tafsir-cleanup.yml` ;
- `scripts/audit_0105_tafsir_cleanup.py`.

Aucun changement du compteur, du pare-feu, des exclusions, de l'ergonomie, des Ḥikam ou du corpus Jalalayn n'est autorisé dans cette branche.

## Décisions gelées vérifiées

### Jalalayn

- 6 236 entrées et 427 notes ;
- archive et base SQLite doivent rester identiques aux checksums approuvés 0.10.4 ;
- aucun nouveau nettoyage typographique ne doit modifier son rendu validé.

### Qushayrī

- anglais uniquement dans l'asset Plus ;
- arabe source retiré ;
- traduction anglaise du verset conservée ;
- commentaire anglais conservé ;
- 806 segments ;
- couverture vérifiée des sourates 1 à 4.

### Qurtubī

- quatre volumes anglais approuvés seulement ;
- 432 entrées ;
- couverture : 1:1–7, 2:1–286, 3:1–200, 4:1–22 ;
- 4:23 doit rester absent car il commence dans le volume 5 ;
- absence d'arabe source et de contamination Sunniconnect/scan.

## Audit contradictoire effectué

### Défaut trouvé puis corrigé

La première version du nouveau gate appliquait une normalisation d'espaces supplémentaire à Jalalayn. C'était contradictoire avec la décision de conserver Jalalayn strictement inchangé : un corpus pourtant identique aux checksums approuvés aurait pu être rejeté à tort.

Correction : Jalalayn est désormais gouverné par l'identité binaire, les comptes canoniques et des contrôles non invasifs de corruption. Les nouvelles règles de nettoyage de whitespace s'appliquent à Qushayrī et Qurtubī reconstruits, pas au rendu Jalalayn gelé.

### Contrôles du nouveau gate

Pour Qushayrī et Qurtubī, chaque entrée est contrôlée pour :

- intégrité SQLite ;
- compte exact et unicité des segments ;
- couverture verset par verset du périmètre approuvé ;
- absence d'arabe source ;
- absence de glyphes Unicode Private Use Area ;
- absence de U+FFFD, U+00AD et caractères de contrôle ;
- absence de retours chariot et espaces d'extraction non normalisés ;
- absence d'en-têtes de page/sourate et de marqueurs de scan connus ;
- conservation de la traduction anglaise du verset chez Qushayrī ;
- restriction stricte de Qurtubī aux volumes `v1` à `v4` et exclusion de 4:23.

## Gate de publication

Le workflow doit encore, dans GitHub Actions :

1. télécharger les sources PDF épinglées ;
2. reconstruire Qushayrī et Qurtubī ;
3. exécuter `audit_0105_tafsir_cleanup.py` ;
4. exécuter les audits 0.10.4 existants ;
5. préserver le freeze des 264 Ḥikam ;
6. compiler et tester Light + Plus ;
7. prouver que Light ne contient aucun Tafsīr ;
8. prouver que l'APK Plus contient exactement les trois corpus approuvés.

**Décision contradictoire : ne pas merger la PR et ne pas publier 0.10.5 tant que ces huit étapes n'ont pas produit un run vert.**
