# Reprise exécutable — Mawdudi 114

1. Lire STATUS_CURRENT.md, CP00_REPORT.md, CP00_INITIAL_INVENTORY.json, PROGRESS_MATRIX.csv et PRIVATE_ASSETS_MANIFEST.md.
2. Vérifier branche unique, HEAD distant, git status et changements locaux. Le dernier checkpoint initial vérifié est `9354af4397f248a64add0484a0ed898cef54187b` ; récupérer ensuite le commit du présent rapport. Ne rien écraser.
3. Lever le blocage de stockage privé ; aucune suppression de fichiers utilisateur n’est autorisée par cette procédure.
4. Retrouver l’EPUB et le prompt joints via les références PRIVÉES conservées dans le reçu de remise. SHA EPUB : `8d0a4c5a34bdc8404900b8b61b8a93d633c7984f3497fe74a9d846adfa454bba`. Le ZIP temporaire n’a pas été sauvegardé.
5. Depuis la racine du dépôt, la vérification reproductible est :

```bash
python -B scripts/test_audit_mawdudi_epub.py
python -B scripts/audit_mawdudi_epub.py --epub "$MAWDUDI_SOURCE_EPUB" --output "$MAWDUDI_AUDIT_OUTPUT"
```

Définir ces deux variables avec les chemins réels de la source privée et d’une sortie locale. Comparer la sortie à EPUB_STRUCTURAL_AUDIT.json. Les chiffres 114 / 6127 / 109 sont des résultats structurels historiques, pas des nouveaux PASS.

6. Sauvegarder le paquet privé sur un stockage durable et vérifier les octets récupérés ; simulation de reprise complète NON EXÉCUTÉE à cause du quota. Les extraits privés intermédiaires sont régénérables depuis la source brute et le script.
7. Clôturer CP-00 après persistance vérifiée. Reprendre CP-01 lot sourates 001–010, jamais 114 d’un bloc ; sauvegarde publique expurgée et privée avant le lot suivant.
8. Pour 109 labels non reconnus, conserver la preuve/source et ne jamais compléter par IA ou une substitution globale. Les crochets de la sourate 9 sont déjà gérés ; ne pas les réauditer comme des omissions.
9. SOURCE_PRECHECK_FINDINGS.json contient les anomalies d’index 2/19/54/55. Aucune correction de bornes ou de titres n’est approuvée.
10. Préserver les moteurs Hifz, QCF et l’officielle. Aucun texte protégé, identifiant privé, APK ou secret dans GitHub/CI publics. CP-02 à CP-07 restent NON EXÉCUTÉS.
