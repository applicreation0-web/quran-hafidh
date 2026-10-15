# Registre CI et tests

Les API de statuts et de runs PR renvoient des listes vides : cela ne signifie pas absence de CI.
Lecture REST des cinq derniers runs de branche effectuée le 15 octobre 2026.

| Run | Commit | Conclusion | Nature |
|---|---|---|---|
| 38738763440 | 00fb6986 | success | historique : régression Android |
| 38738763412 | 00fb6986 | success | historique : staging public sans corpus privé |
| 38738763463 | 00fb6986 | success | historique : audit Hifz/Amorces |

Aucun de ces runs ne prouve la présence d’un corpus Mawdudi final ni une recette BOOX physique.

## Nouveaux contrôles CP-00

- Python 3 / stdlib : `python -B scripts/test_audit_mawdudi_epub.py` : 8 tests PASS.
- `audit_mawdudi_epub.py` : source CRC correct, 114 en-têtes, 6127 labels reconnus, 109 non résolus. Ce résultat n’est PAS un PASS éditorial.
- Lors du premier essai sur la source réelle : FAIL technique sur la spine (référence `cover` non définie). Anomalie de source reproduite par test synthétique, puis signalée explicitement sans modifier le source ; seconde exécution réussie.
- Une première version de la commande exploratoire demandait bs4 : NON EXÉCUTÉE (module absent). Extraction définitive avec la bibliothèque standard, sans installation.
- Diff app/hifz-app/chatgpt depuis le HEAD de départ : vide. Aucun test Android/JVM/runtime nouveau, aucun APK.
- Écriture initiale GitHub par git push : refus automatique avant publication, puis échec d’authentification shell après expurgation. Sauvegarde expurgée réussie par connecteur GitHub au commit 9354af43 et relue à distance.
- Sauvegarde privée ZIP : FAIL (quota de stockage), aucune persistance nouvelle revendiquée.
