# Quran Hifz 0.7.5 Claude NO-GO Fixes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Corriger les défauts bloquants et majeurs identifiés par l’audit Claude sur le candidat 0.7.5 sans modifier les invariants Hifz validés ni déclencher de GO_PHONE.

**Architecture:** Conserver le moteur physique existant et corriger uniquement ses raccordements : routage UI de Consolidation, migration 5→6 des unités fractionnées, sémantique de propriété des lignes, projection hebdomadaire et conservation des frontières de plages. Les sessions de Consolidation restent figées par `lineIds` exacts et `ConsolidationCycleEngine` n’est pas réécrit.

**Tech Stack:** Android Java, Gradle, JUnit, Android instrumented tests, GitHub Actions.

**Spec:** Audit Claude hors ligne du HEAD `5696e7e8b91d213deaaa40c7b4893965cf46d4e4` fourni dans la conversation du 2026-09-16.

## Global Constraints
- Base exacte : `5696e7e8b91d213deaaa40c7b4893965cf46d4e4`.
- PAGE/SOURATE jamais franchies ; split intra-ayah autorisé ; identité = `lineIds`.
- Découpage <=11→1, 12→6+6, 13→6+7, 14→7+7, 15→7+8.
- Ne pas réécrire `ConsolidationCycleEngine` ni les vecteurs.
- J10/Révision uniquement ACQUIRED.
- Audio, Tafsir, BOOX, release 0.7.4 hors scope.
- Aucun merge/release/publication/GO_PHONE automatique.
- TDD RED puis correction minimale puis GREEN puis vérification complète.

### Task 1 — Consolidation atteignable
- Test RED exigeant `MainActivity → RECENT_SABQI_REVIEW` quand session ouverte ou unité prête.
- Implémenter un routage progression-triggered, pas une cadence hebdomadaire.

### Task 2 — Migration 5→6 fractionnée
- Test instrumenté RED : migration puis Stabilisation d’une page 15 lignes avec ancien `itqanBlockIndex=2`.
- Migrer les lignes réellement validées par l’ancien découpage en STABILIZED, jamais directement ACQUIRED.
- Réinitialiser l’index incompatible avec le nouveau plan.

### Task 3 — Propriété physique des lignes
- Test RED frontière de verset en milieu de ligne.
- Ajouter un helper owner-based unique et l’utiliser pour planification/affichage/complétude.
- `readyUnits`: unité partiellement STABILIZED => skip; partiellement ACQUIRED reste invariant cassé.

### Task 4 — Projection = runtime
- Test RED page 15 lignes => deux unités.
- Utiliser `StabilizationHalfPagePolicy.planPage` dans projection hebdo et détail accueil.

### Task 5 — Plages et session figée
- Test RED : plages 53 puis 54 restent distinctes ; édition manuelle invalide session Consolidation figée.
- Fusionner chevauchements uniquement, pas adjacency.

### Task 6 — Closeout
- Canonicaliser le libellé legacy restant.
- Full JVM/source/core/compile/verifiers/emulator.
- Nouveau kit Claude autonome avec baseline/diff hors-scope.
