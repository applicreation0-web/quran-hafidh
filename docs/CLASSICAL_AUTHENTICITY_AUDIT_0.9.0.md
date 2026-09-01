# Quran Safeguard 0.9.0 — Audit d’authenticité des textes classiques

Date : 2026-09-01

## Principe de release

**AUTHENTICITÉ AVANT QUANTITÉ.**

Quran Safeguard transmet des textes classiques. Il ne résume pas, n’interprète
pas et ne reconstruit pas la pensée d’Ibn ʿAṭāʾ Allāh, d’Ibn ʿAjība ou
d’al-Ghazālī.

Un texte classique n’est éligible à l’affichage que si, séparément :

- `sourceVerified == true`
- `attributionVerified == true`
- `translationVerified == true`
- `humanVerified == true`
- `rightsStatus != UNRESOLVED`
- `authenticityStatus == VERIFIED_SOURCE`

Les valeurs manquantes ne sont jamais compensées automatiquement.

## Droits

Les textes arabes historiques sous-jacents sont anciens. Une traduction moderne
est toutefois une œuvre distincte susceptible d’être protégée. Les traductions
françaises présentes dans les dépôts classiques de cette branche sont des
**traductions internes de travail**. Elles ne reprennent pas une traduction
commerciale française. Leur statut de droits interne est
`INTERNAL_TRANSLATION_ALLOWED`, mais elles restent **invisibles** tant que la
validation humaine de correspondance n’est pas enregistrée.

Aucune mise en page moderne, note éditoriale moderne ou traduction commerciale
n’est reproduite comme contenu utilisateur.

Référence juridique de prudence (Royaume-Uni) :
https://www.gov.uk/government/publications/copyright-notice-duration-of-copyright-term/copyright-notice-duration-of-copyright-term
La guidance de l’IPO rappelle notamment qu’une traduction peut attirer sa propre
protection, même lorsque l’œuvre sous-jacente est hors copyright, et qu’une
nouvelle disposition typographique dispose d’une protection distincte.

## 1 — Al-Ḥikam

| ID | N° | Arabe retrouvé | Traduction vérifiée | Source | Commentaire Ibn ʿAjība | Traduction commentaire | Droits traduction | Validation humaine | Authenticité | Affiché |
|---|---:|---|---|---|---|---|---|---|---|---|
| hikma_5 | 5 | Oui | **Non** | ibnalarabi.com, Hikma 5 | Arabe retrouvé, Īqāẓ al-Himam p.39 | **Non** | INTERNAL_TRANSLATION_ALLOWED | **Non** | PARTIALLY_VERIFIED | **Non** |
| hikma_10 | 10 | Oui | **Non** | ibnalarabi.com, Hikma 10 | Arabe retrouvé, Īqāẓ al-Himam p.50 | **Non** | INTERNAL_TRANSLATION_ALLOWED | **Non** | PARTIALLY_VERIFIED | **Non** |
| hikma_12 | 12 | Oui | **Non** | ibnalarabi.com, Hikma 12 | Arabe retrouvé, Īqāẓ al-Himam p.58 | **Non** | INTERNAL_TRANSLATION_ALLOWED | **Non** | PARTIALLY_VERIFIED | **Non** |

### Sources Al-Ḥikam retenues pour le contrôle documentaire

- Hikma 5 : https://www.ibnalarabi.com/books/hikam-ataiya.php?id=5
- Hikma 10 : https://www.ibnalarabi.com/books/hikam-ataiya.php?id=10
- Hikma 12 : https://www.ibnalarabi.com/books/hikam-ataiya.php?id=12
- Ibn ʿAjība, Hikma 5 : https://ablibrary.net/book_content/b/9684/39
- Ibn ʿAjība, Hikma 10 : https://ablibrary.net/book_content/b/9684/50
- Ibn ʿAjība, Hikma 12 : https://ablibrary.net/book_content/b/9684/58

La transcription Al-Ḥikam retenue est utilisée pour son identification et sa
numérotation, mais l’édition imprimée de contrôle humain reste à verrouiller.
En conséquence aucune entrée n’a `humanVerified=true`.

Le bouton **« Approfondir — commentaire classique »** est techniquement
impossible à afficher tant que le commentaire n’a pas lui aussi tous les
contrôles au vert.

## 2 — Al-Ghazālī

| ID | Ouvrage | Arabe retrouvé | Traduction vérifiée | Source primaire/texte de l’œuvre | Contexte supplémentaire | Droits traduction | Validation humaine | Authenticité | Affiché |
|---|---|---|---|---|---|---|---|---|---|
| ghazali_bidaya_religion_two_halves | Bidāyat al-Hidāya | Oui, **corrigé** | **Non** | Section II, transcription Wikisource l.207 | Oui, texte continu d’al-Ghazālī | INTERNAL_TRANSLATION_ALLOWED | **Non** | PARTIALLY_VERIFIED | **Non** |
| ghazali_bidaya_limb_guardianship | Bidāyat al-Hidāya | Oui | **Non** | Section II, transcription Wikisource l.208 | Oui, texte continu d’al-Ghazālī | INTERNAL_TRANSLATION_ALLOWED | **Non** | PARTIALLY_VERIFIED | **Non** |
| ghazali_ihya_outer_inner_adab | Iḥyāʾ ʿUlūm al-Dīn | Oui | **Non** | Kitāb Ādāb al-Maʿīsha…, transcription Wikisource l.97 | Oui, texte continu d’al-Ghazālī | INTERNAL_TRANSLATION_ALLOWED | **Non** | PARTIALLY_VERIFIED | **Non** |

Sources :

- Bidāyat al-Hidāya : https://ar.wikisource.org/wiki/بداية_الهداية
- Iḥyāʾ ʿUlūm al-Dīn, Kitāb Ādāb al-Maʿīsha wa-Akhlāq al-Nubuwwa :
  https://ar.wikisource.org/wiki/إحياء_علوم_الدين/كتاب_آداب_المعيشة_وأخلاق_النبوة

Le futur bouton **« Approfondir — contexte dans l’œuvre »** ne montre que du
texte d’al-Ghazālī lui-même. Il n’utilise aucun commentaire généré par
l’application.

## Anomalies contradictoires trouvées et corrigées

1. **Variante al-Ghazālī enregistrée comme citation exacte**  
   Ancien texte : `اعلم أن الدين شطران...`  
   Texte retrouvé dans la source retenue :
   `اعلم أن للدين شطرين، أحدهما: ترك المناهي، والآخر: فعل الطاعات.`  
   La valeur canonique interne a été corrigée et verrouillée par test.

2. **Traductions françaises sans validation humaine attestée**  
   Elles étaient auparavant affichables. Elles sont maintenant des brouillons
   internes non éligibles à l’affichage.

3. **Commentaires Ibn ʿAjība précédemment affichables sans validation humaine de
   la traduction**  
   Les extraits arabes sont retrouvés aux pages documentées, mais le bouton est
   maintenant bloqué tant que la traduction et la validation humaine ne sont
   pas complètes.

4. **Absence de séparation technique assez forte des statuts**  
   Corrigée : source, attribution, traduction, validation humaine et droits ont
   désormais des champs distincts.

5. **Risque de duplication al-Ghazālī dans la bibliothèque générique**  
   Corrigé : les textes al-Ghazālī ont leur dépôt canonique dédié et ne sont
   plus stockés comme simples `DailyReminder`.

6. **Numérotation Al-Ḥikam potentiellement divergente selon les éditions**  
   Corrigé dans le modèle : le numéro est explicitement celui de la source
   retenue. Le build interdit les affirmations « Toutes les Ḥikam » et un total
   figé de 264 présenté comme corpus complet.

## Non-interprétation

Le build recherche et interdit dans le code utilisateur des formulations telles
que :

- « Pour comprendre cette Hikma »
- « Explication simple »
- « Ibn ʿAṭāʾ Allāh veut dire… »
- « Al-Ghazâlî nous enseigne ici… »
- « ce que l’auteur veut dire »
- « en d’autres termes »
- « Explication de la pensée »
- « Résumé IA »

Ces contrôles complètent les tests unitaires ; ils ne remplacent pas la revue
humaine des textes.

## Décision actuelle

**NO-GO pour l’affichage des textes classiques.**

Cela ne signifie pas que les sources arabes sont nécessairement fausses.
Cela signifie que, conformément à la mission, le pipeline refuse de transformer
une vérification documentaire partielle en validation éditoriale complète.

La release 0.9 peut seulement devenir GO si :

- soit une revue humaine documentée valide les traductions et les entrées,
  faisant passer leurs statuts au vert ;
- soit les textes classiques non validés restent invisibles dans la version
  distribuée.

Aucune donnée `NOT_VERIFIED` ou `PARTIALLY_VERIFIED` n’est affichée.
