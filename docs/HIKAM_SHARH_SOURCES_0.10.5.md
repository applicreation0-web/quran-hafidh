# Quran Safeguard Plus 0.10.5 — témoins documentaires du Sharḥ

Ce document fixe les sources autorisées pour le chantier des commentaires des 264 Ḥikam. Il ne transforme aucune extraction automatique en texte publiable.

## 1. ʿAbd al-Majīd al-Sharnūbī

- Œuvre : **شرح الحكم العطائية** (`Sharḥ al-Ḥikam al-ʿAṭāʾiyya`).
- Auteur : ʿAbd al-Majīd al-Sharnūbī al-Azharī (m. 1348 H / 1929).
- Témoin paginé de contrôle : édition établie/commentée par ʿAbd al-Fattāḥ al-Bazm, 2e édition, 1410 H / 1989, 240 pages, signalée par al-Maktaba al-Waqfiyya.
- Page documentaire : https://waqfeya.net/books/شرح-الحكم-العطائية/7634e36af5324f5792b3f6ec4f735f45
- Scan lié par la notice : https://archive.org/download/FP40507/40507.pdf
- Une transcription web éventuelle ne sert que de **candidat d’alignement**. Le texte final doit être recoupé avec le témoin paginé et recevoir un locator précis.

## 2. Ibn ʿAbbād al-Rundī

- Œuvre : **غيث المواهب العلية في شرح الحكم العطائية** (`Ghayth al-mawāhib al-ʿaliyya fī sharḥ al-Ḥikam al-ʿAṭāʾiyya`).
- Auteur : Muḥammad ibn Ibrāhīm Ibn ʿAbbād al-Nafzī al-Rundī (733–792 H / 1333–1390).
- Témoin de contrôle : manuscrit de la National Library of Israel, Project “Warraq”, identifiant **NNL_ALEPH990035287340205171**.
- Notice : https://www.nli.org.il/en/manuscripts/NNL_ALEPH990035287340205171/NLI
- La notice NLI indique que l’objet est dans le domaine public et que tout usage est permis, avec attribution à la collection NLI / Project “Warraq”.
- Un texte numérique secondaire peut aider à repérer les limites, mais **ne remplace pas** le contrôle du manuscrit pour la promotion en production.

## Règles de promotion en production

Pour chacune des 528 entrées attendues (264 × 2), il faut :

1. correspondance exacte avec le numéro canonique de la Ḥikma ;
2. limite début/fin du commentaire contrôlée indépendamment ;
3. arabe relu sur le témoin documentaire ;
4. traduction française indépendante du même passage, sans synthèse ni fusion ;
5. auteur, ouvrage, édition/témoin, URL et locator ;
6. crédit de traduction explicite ;
7. hashes SHA-256 du matn canonique, du commentaire arabe et de la traduction ;
8. trois contrôles séparés consignés : alignement, arabe, traduction ;
9. aucun extrait tronqué présenté comme commentaire complet ;
10. aucun contenu « research », OCR brut ou candidat ne peut avoir le statut `verified`.

Le script `scripts/verify_0105_hikam_sharh_264.py` matérialise ces règles et bloque la release tant que les 528 entrées ne sont pas présentes et conformes.
