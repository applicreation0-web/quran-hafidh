# Audit contradictoire — Quran Safeguard Plus 0.10.5 — Sharḥ des 264 Ḥikam

## Verdict actuel

**BLOQUÉ POUR INTÉGRATION ET PUBLICATION.**

L'infrastructure documentaire et les garde-fous ont été renforcés, mais le dépôt ne contient pas encore l'asset de production `app/src/plus/assets/hikam/hikam_sharh_dual.json`. Il serait donc faux d'annoncer les commentaires comme intégrés.

La PR doit rester en draft jusqu'à présence et validation de **528 entrées réelles** : 264 commentaires d'al-Sharnūbī et 264 commentaires d'Ibn ʿAbbād.

## Périmètre et décisions conservés

- le corpus canonique des 264 Ḥikam reste gelé ;
- matn arabe, traduction française de la Ḥikma, aide terminologique et sharḥ restent séparés ;
- chaque commentaire appartient à un auteur et une œuvre identifiés ;
- al-Sharnūbī et Ibn ʿAbbād restent séparés, sans synthèse ni fusion ;
- le sharḥ est Plus uniquement ; Light n'embarque aucun asset de commentaire ;
- aucun OCR, candidat d'alignement ou texte de recherche n'est promu automatiquement.

## Défauts découverts par la revue contradictoire

### 1. Provenance de traduction insuffisamment formalisée

Le modèle 0.10.4 connaissait le commentateur, l'œuvre, le texte et le locator mais ne rendait pas obligatoire une édition/témoin source et un crédit explicite de traduction française.

**Correction 0.10.5 :** `sourceEdition` et `translationCredit` sont obligatoires pour qu'une entrée soit affichable.

### 2. Gate plus strict que le runtime

La première version du gate 0.10.5 exigeait une frontière documentaire vérifiée, mais le parseur runtime aurait encore pu marquer une entrée comme vérifiée sans `source_boundary_status=verified`.

**Correction :** le runtime exige désormais les quatre statuts : commentaire, traduction, alignement Hikma↔sharḥ et frontière source.

### 3. Locator de page insuffisant

Deux commentaires peuvent commencer ou finir sur la même page. Un simple `p. N` ne suffit pas à prouver la frontière du commentaire.

**Correction :** chaque entrée doit maintenant porter un `commentary_boundary_locator` non vide en plus du locator de page/témoin.

### 4. Absence de test Plus dédié au contrat Sharḥ

Le projet ne contenait pas de test unitaire spécifique au modèle Sharḥ.

**Correction :** ajout de `HikamSharhContractTest.kt`, couvrant notamment l'obligation de frontière documentaire, de crédit de traduction et du statut vérifié.

## Témoins documentaires retenus

### al-Sharnūbī

- `شرح الحكم العطائية` ;
- édition paginée contrôlable, 2e édition, 1410 H / 1989, établie/commentée par ʿAbd al-Fattāḥ al-Bazm ;
- notice Waqfeya et scan Archive.org documentés dans `HIKAM_SHARH_SOURCES_0.10.5.md`.

### Ibn ʿAbbād al-Rundī

- `غيث المواهب العلية في شرح الحكم العطائية` ;
- témoin de contrôle NLI / Project Warraq, identifiant `NNL_ALEPH990035287340205171` ;
- la notice NLI le signale dans le domaine public avec usage permis ;
- les transcriptions numériques secondaires servent uniquement au repérage/alignement, jamais d'autorité finale.

## Contrat documentaire exigé pour chacune des 528 entrées

Chaque entrée doit posséder :

1. numéro canonique de la Ḥikma ;
2. commentateur ;
3. œuvre ;
4. édition ou témoin ;
5. texte arabe du commentaire ;
6. traduction française du même passage ;
7. crédit de traduction ;
8. URL source approuvée ;
9. locator de page/témoin ;
10. locator de frontière ;
11. statut d'alignement `verified` ;
12. statut du commentaire `verified` ;
13. statut de traduction `verified` ;
14. statut de frontière `verified` ;
15. SHA-256 du matn canonique ;
16. SHA-256 du commentaire arabe ;
17. SHA-256 de la traduction française ;
18. contrôle indépendant de frontière ;
19. contrôle du texte arabe ;
20. contrôle de la traduction française ;
21. identité/rôle du reviewer et date du contrôle.

Le gate refuse en outre les placeholders, OCR brut, candidats, extraits tronqués, doublons de texte et sources hors des hôtes approuvés.

## Décision de release

- **PR : draft** ;
- **fusion : interdite à ce stade** ;
- **publication Plus : interdite à ce stade** ;
- le gate doit échouer tant que les 528 entrées vérifiées ne sont pas réellement présentes ;
- aucune donnée artificielle ne doit être ajoutée pour satisfaire le compteur.

Cette situation est volontairement fail-closed : mieux vaut une 0.10.5 non publiée qu'un sharḥ attribué, découpé ou traduit sans preuve suffisante.
