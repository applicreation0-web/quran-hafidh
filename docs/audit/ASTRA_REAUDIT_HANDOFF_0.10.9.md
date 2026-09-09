# Quran Safeguard 0.10.9 — handoff de re-audit final

## Statut actuel

- Branche : `release/0.10.9-work`
- Ancien candidat Astra interrompu : `df0be3ce9521e0bf0ecb6a62bbb4e393f1261a4b`
- Ancien parent NO-GO : `9846de48794e34eefc66d947e7f18991ad1c3342`
- Ancien artefact validé avant poursuite contradictoire : `3d63bb1045d8e7489b4b43fd8c65c22fa91b6bde`
- Pull request : #78

L’audit Astra Light a été interrompu par la limite Work avant verdict final. Avant l’interruption, Astra avait recalculé avec succès les SHA-256 des deux APK alors candidats, confirmé 604 pages dans les deux éditions, confirmé l’absence d’assets Tafsir dans Light et exécuté la suite JavaScript disponible. Astra avait également identifié que le test de restauration Activity ne constituait pas à lui seul une preuve de vrai process death/no-flash.

La poursuite contradictoire a ensuite révélé plusieurs faux PASS et écarts de contrat dans le candidat précédent. Ces écarts ont été corrigés directement sur `release/0.10.9-work`. Les anciennes références CI/APK de ce document sont donc SUPERSEDEES et ne doivent plus être utilisées pour une publication. Le HEAD final exact est celui de la branche après ce commit documentaire ; les APK officiels devront provenir du nouveau run 0.10.9 sur ce HEAD final.

## Corrections post-Astra intégrées

1. Découpage Mémorisation : 1–3 lignes restent un bloc court valide, 4–7 lignes restent un seul bloc ; 7 lignes ne produit plus `[4,3]`. Cas de contrôle : 4→[4], 5→[5], 6→[6], 7→[7], 8→[4,4], 10→[5,5], 11→[6,5], 15→[5,5,5].
2. Préparation de bloc ajoutée à la machine pédagogique : lecture attentive ×2 puis, uniquement si audio réellement disponible, écoute passive ×2.
3. `Afficher brièvement` : génération/cancellation des callbacks, deuxième révélation successive remplaçant proprement la première, callbacks obsolètes neutralisés lors des changements de page/session/mode/étape.
4. Transition multi-page Mémorisation : attente de `jumpToStep()` avant le rendu du nouvel état afin d’éviter la frame ancienne page/mauvais masque.
5. Aides : réécoute marquée comme aide ; validation impossible tant qu’une aide de l’étape reste active. Les pseudo-indices `Premier mot/Premiers mots` basés sur des groupes d’encre non linguistiques ont été retirés plutôt que présentés comme des mots authentifiés.
6. Signets Mémorisation : rattachement uniquement à une session correspondant réellement au passage, sans priorité aveugle à la session courante.
7. Reprise ciblée : masque 100 %, trois réussites locales exigées pour terminer, jusqu’à deux écoutes de renforcement par ligne si audio est disponible, jalons précédents conservés.
8. E-Ink : cleanup différé conservé ; `dispose()` ajouté ; callbacks/overlays de fallback suivis et supprimés au lifecycle ; STANDARD reste no-op.
9. Lecteur E-Ink : PAGE_UP/PAGE_DOWN uniquement pour la pagination physique ; les touches volume ne sont plus capturées. Une sélection de verset n’empêche plus les touches page.
10. Navigation Tafsir contextuelle : un Back depuis un contexte de renvoi ferme directement l’Activity contextuelle et revient d’un niveau, sans pression supplémentaire.
11. Safeguard challenge : validation centrale exige maintenant >=60 secondes actives ET `bottomReached`; une page entièrement visible marque automatiquement le bas au chargement, tandis qu’une page scrollable doit réellement atteindre son bas. Le backend `GuardPrefs` applique aussi cette règle fail-closed.
12. Frontière Tafsir 10.9 : le challenge chronométré ne peut plus ouvrir le Tafsir ; le Tafsir reste réservé à Lecture/Étude Plus. JavaScript du WebView challenge est désactivé.
13. Cleanup E-Ink du challenge : `refreshController.dispose()` au `onDestroy`.

## Exigences de validation finale

Le nouveau HEAD doit passer intégralement :

- `Quran Safeguard 0.10.9 build and audit` ;
- tests Node du protocole Mémorisation ;
- `:app:verifyReleaseAudit` ;
- unitaires Light et Plus ;
- builds release/debug Light et Plus ;
- inspection des frontières Light/Plus ;
- instrumentation Android Light et Plus ;
- audits sensibles / Tafsir / Juz-Hizb applicables ;
- 604 pages exactes ;
- audio gate `redistributionApproved=false` ;
- aucune permission de surveillance élargie.

Aucun APK antérieur à ces corrections ne doit être signé ou publié.

## Privacy / périmètre

Les corrections ne doivent introduire ni UsageStats, ni `PACKAGE_USAGE_STATS`, ni `QUERY_ALL_PACKAGES`, ni NotificationListener, ni `packageNames=null`, ni surveillance d’applications banque/sécurité/identité. La confidentialité des applications hors cible reste prioritaire sur une précision de compteur impossible sans élargissement de visibilité.

## Limitation matérielle

La logique E-Ink peut être validée en tests/emulation. L’efficacité physique du ghosting/full refresh et les API propriétaires BOOX restent une validation matérielle séparée ; aucune preuve logicielle ne doit être présentée comme PASS physique.

## Publication

Ne signer et ne publier 0.10.9 qu’après un nouveau CI entièrement vert sur le HEAD final, téléchargement/recalcul des SHA-256 des APK Light/Plus issus de CE run et audit de cohérence final. Ne jamais réutiliser les anciens hashes de ce document.
