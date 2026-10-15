# CP-00 — reprise et contrôle de la source Mawdudi

**État : BLOQUÉ sur la nouvelle sauvegarde privée durable. Le produit Mawdudi 114 n’est pas réalisé.**

## Acquis vérifiés

- Dépôt unique et branche imposée retrouvés. HEAD applicatif de départ `00fb698603b83ffda49bed75be6f55d3f08400ac`, référence officielle `7fd45bbe3d2e67a2e21c6f8ca1d5ae935d1f0d21` résolue.
- Copie locale initialement propre à `6a17d3e6`, avancée par fast-forward. Aucune modification utilisateur à récupérer ou écraser.
- Trois CI historiques réussies au HEAD de départ, sans réexécution ; IDs dans CI_TEST_LEDGER.md.
- Sauvegarde initiale expurgée confirmée sur la branche distante : `9354af4397f248a64add0484a0ed898cef54187b`. Lecture distante du manifeste effectuée après écriture.
- EPUB joint : 5 175 278 octets ; SHA-256 `8d0a4c5a34bdc8404900b8b61b8a93d633c7984f3497fe74a9d846adfa454bba`. CRC ZIP correct ; 3 124 membres, 3 117 pages numérotées. Source brute non modifiée.
- 114 en-têtes de sourates localisés dans l’ordre réel de la spine EPUB. Les variantes Sura/Surah, une virgule et l’absence de Surah au début de la sourate 16 sont reconnues pour l’inventaire uniquement.
- 6 127 références ayah distinctes reconnues dans leurs régions de sourate, sur 6 236 attendues. **109 références restent à contrôler, dans 37 sourates. Ce ne sont pas 109 omissions textuelles prouvées.** Les 77 autres sourates ne sont pas validées éditorialement.
- Exemple contrôlé : entre les références 2:96 et 2:101, le membre `EPUB/page_50.html` contient une continuation de note puis du bruit OCR ; les textes anglais de 2:97–100 ne sont pas disponibles dans ce passage exploitable. Une récupération complémentaire ciblée est justifiée ; aucun texte n’a été reconstruit.
- Le format `[9:1]` est valide dans l’EPUB : les crochets ont été reconnus sans les confondre avec une lacune ou modifier le fichier.

## Limites et anomalies

- Les métadonnées OPF n’identifient ni titre bibliographique, auteur, éditeur, langue, droits, source ni date d’édition. La date technique de génération ne prouve pas une édition.
- La spine référence un élément `cover` absent du manifeste. L’audit le signale sans réparer le fichier ni perdre les pages réellement présentes.
- L’avertissement Internet Archive décrit explicitement l’OCR et ses risques. L’arabe OCR n’est jamais une source de rendu QCF.
- Des labels corrects ne prouvent pas les notes complètes, l’ordre interne, la traduction, le sens des titres ni les 556 sections web. Ces contrôles sont NON EXÉCUTÉS.
- L’édition web annonce plusieurs traducteurs selon les groupes de sourates ; ne pas présumer l’homogénéité de l’EPUB. Droits d’intégration/offline : NON ÉTABLIS à ce stade ; aucune conclusion juridique tirée de l’absence de métadonnées.
- Précontrôles des index externes : 2:134 absent des plages affichées ; trous 54:45–55 et 55:56–78 confirmés dans les index consultés. Plages ALIM de Maryam cohérentes numériquement ; autre source inaccessible. Détails, URLs et décisions dans SOURCE_PRECHECK_FINDINGS.json. Aucun titre source copié dans ce rapport public.

## Blocage de persistance — preuve et portée

La création du checkpoint privé ZIP a échoué avec `library_storage_limit_exceeded` : espace de stockage plein. Aucune création durable n’a été confirmée. Ne pas déduire une sauvegarde du simple chemin local ou d’un lien de téléchargement.

Le ZIP préparé est local uniquement : 7 942 544 octets ; SHA-256 `120acb9664b3176c3f3548cb8cbf3fad95cc2107bfa2ba527184ea6ddd74efe8`. Il contient le source brut fourni, le prompt, l’extraction OCR non corrigée et le premier inventaire. Les identifiants privés de récupération restent hors GitHub. Les fichiers joints d’origine existent déjà comme fichiers persistants fournis par l’utilisateur ; cela ne rend pas persistante la nouvelle extraction.

Conformément à H0.2, CP-00 n’est pas déclaré sauvegardé intégralement et CP-01 n’est pas lancé. Une reprise réelle depuis la seule nouvelle sauvegarde privée est NON EXÉCUTÉE. Aucune suppression d’anciens fichiers utilisateur n’a été tentée. Solution : libérer de l’espace ou fournir un autre stockage privé durable, puis enregistrer le paquet, retrouver les octets et vérifier leur SHA avant le lot suivant.

## Vérification du travail de cette session

- Huit tests synthétiques de l’outil d’audit : PASS. Ils vérifient crochets, références étrangères, labels malformés, bornes, doublons, absence de faux PASS éditorial et spine incomplète.
- Audit de la source complète : exécuté et archivé sous EPUB_STRUCTURAL_AUDIT.json ; tableaux de 114 sourates et 109 références à contrôler.
- Aucun fichier app, hifz-app ou chatgpt modifié par cette session depuis le HEAD de départ : contrôle de diff effectué. Moteurs Hifz, sauvegardes, QCF et version officielle non modifiés. Ceci ne remplace pas les tests runtime futurs.
- Aucun APK construit ou livré ; JVM/Android/BOOX de la nouvelle variante : NON EXÉCUTÉS.
- Aucun commentaire ou titre anglais tiers ni secret publié. Un premier push contenant des identifiants privés a été refusé par le contrôle automatique ; le commit non publié a été expurgé avant la sauvegarde distante via le connecteur GitHub autorisé.

## Reprise exacte

1. Lever le blocage de stockage privé sans supprimer de fichiers sans instruction utilisateur.
2. Récupérer et vérifier les sources jointes par leurs références conservées dans le manifeste privé.
3. Sauvegarder et vérifier le checkpoint privé ; ne pas confondre le ZIP temporaire avec une sauvegarde durable.
4. Exécuter la procédure reproductible de HANDOVER_REPRISE.md ; comparer le JSON aux résultats déjà acquis, ne pas recommencer l’exploration.
5. Clôturer CP-00 seulement après preuve de persistance, puis CP-01 lot 001–010 et sauvegarde avant 011. Traiter les 109 labels comme dossiers éditoriaux ; collecte complémentaire ciblée, jamais 114 téléchargements redondants.
6. CP-02 à CP-07 restent NON EXÉCUTÉS ; ne pas reprendre les anciens CP comme des validations du nouveau produit.

## Mise à jour postérieure — blocage de stockage levé

La sauvegarde privée a désormais réussi, avec récupération, empreintes et simulation isolée vérifiées. Voir CP00_CLOSURE.md ; les constats éditoriaux ci-dessus restent ouverts. L’échec initial est conservé pour l’historique.
