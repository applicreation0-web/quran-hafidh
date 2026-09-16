#!/usr/bin/env bash
set -euo pipefail

CANDIDATE="5d5b527ed6be54a5d73ac29545763454cba2d0f9"
ORIGINAL_CANDIDATE="5696e7e8b91d213deaaa40c7b4893965cf46d4e4"
ROUND1_CLEAN_BASE="7185a9ca92dfa46c10f63b0e5c499dce610dd720"
RELEASE_074="f344fc7d0313e4e327a53c0fe6da8f287d12bdac"
FINAL_RUN="35106385151"
PRECLEAN_GREEN_RUN="35104620315"
MIGRATION_RED_RUN="35103834954"
ROUND1_VALID_RED_RUN="35091592696"
ORIGINAL_FINAL_RUN="35078014986"
OFFICIAL_074_APK_SHA="c0007102e2e2f8ef69f303f141d148b416ae5f312a354f3f0a23fc2a70995ccc"
OFFICIAL_074_ASSET_ID="564818126"
ORIGINAL_KIT_ARTIFACT_ID="10442359501"
ORIGINAL_KIT_SHA="7491881591be1cd232206e07cd44f5e2c7ed75e1a0e4728568f9306e8c3b1616"
REPO="applicreation0-web/quran-unlock-android"
API="https://api.github.com/repos/${REPO}"
AUTH="Authorization: Bearer ${GH_TOKEN:?GH_TOKEN required}"

if [[ "$(git rev-parse HEAD)" != "$CANDIDATE" ]]; then
  echo "Wrong checkout: $(git rev-parse HEAD)" >&2
  exit 1
fi

status=""
conclusion=""
for _ in $(seq 1 90); do
  status="$(curl -fsSL -H "$AUTH" -H 'Accept: application/vnd.github+json' "$API/actions/runs/$FINAL_RUN" | python3 -c 'import json,sys; print(json.load(sys.stdin)["status"])')"
  if [[ "$status" == "completed" ]]; then
    conclusion="$(curl -fsSL -H "$AUTH" -H 'Accept: application/vnd.github+json' "$API/actions/runs/$FINAL_RUN" | python3 -c 'import json,sys; print(json.load(sys.stdin)["conclusion"])')"
    break
  fi
  sleep 10
done
[[ "$status" == "completed" && "$conclusion" == "success" ]] || { echo "Final CI is not SUCCESS: status=$status conclusion=$conclusion" >&2; exit 1; }

rm -rf claude_075_reaudit
mkdir -p claude_075_reaudit/{candidate_source,original_candidate_source,evidence,ci,data,checks}

# Complete tracked source snapshots. No GitHub/network access is required by the auditor.
git archive "$CANDIDATE" | tar -x -C claude_075_reaudit/candidate_source
git archive "$ORIGINAL_CANDIDATE" | tar -x -C claude_075_reaudit/original_candidate_source

# Exact identity and deltas.
{
  echo "repository=$REPO"
  echo "candidate_sha=$CANDIDATE"
  echo "candidate_tree=$(git rev-parse "$CANDIDATE^{tree}")"
  echo "original_audited_candidate=$ORIGINAL_CANDIDATE"
  echo "round1_clean_base=$ROUND1_CLEAN_BASE"
  echo "published_0.7.4_code_sha=$RELEASE_074"
  echo "final_ci_run=$FINAL_RUN"
  echo "preclean_green_run=$PRECLEAN_GREEN_RUN"
  echo "migration_red_run=$MIGRATION_RED_RUN"
  echo "round1_valid_red_run=$ROUND1_VALID_RED_RUN"
  echo "original_final_run=$ORIGINAL_FINAL_RUN"
} > claude_075_reaudit/evidence/EXACT_IDENTITY.txt

git show --no-patch --pretty=fuller "$CANDIDATE" > claude_075_reaudit/evidence/CANDIDATE_COMMIT.txt
git log --oneline --decorate "$ORIGINAL_CANDIDATE..$CANDIDATE" > claude_075_reaudit/evidence/COMMITS_ORIGINAL_TO_FINAL.txt
git diff --binary "$ORIGINAL_CANDIDATE..$CANDIDATE" > claude_075_reaudit/evidence/DIFF_5696_TO_FINAL.patch
git diff --name-status "$ORIGINAL_CANDIDATE..$CANDIDATE" > claude_075_reaudit/evidence/FILES_5696_TO_FINAL.txt
git diff --binary "$ROUND1_CLEAN_BASE..$CANDIDATE" > claude_075_reaudit/evidence/DIFF_7185_TO_FINAL.patch
git diff --name-status "$ROUND1_CLEAN_BASE..$CANDIDATE" > claude_075_reaudit/evidence/FILES_7185_TO_FINAL.txt
git diff --name-status "$RELEASE_074..$CANDIDATE" > claude_075_reaudit/evidence/FILES_074_TO_FINAL.txt

# Strong scope assertion: only the six expected Hifz product Java files may differ from the originally audited candidate.
cat > claude_075_reaudit/checks/EXPECTED_PRODUCT_FILES.txt <<'EOF'
hifz-app/src/main/java/com/quransafeguard/hifz/preview/ConsolidationPhysicalUnitPolicy.java
hifz-app/src/main/java/com/quransafeguard/hifz/preview/CorpusLinePolicy.java
hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java
hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java
hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java
hifz-app/src/main/java/com/quransafeguard/hifz/preview/WeeklyDashboardPlanner.java
EOF
git diff --name-only "$ORIGINAL_CANDIDATE..$CANDIDATE" -- hifz-app/src/main app/src/main | sort > claude_075_reaudit/checks/ACTUAL_PRODUCT_FILES.txt
sort claude_075_reaudit/checks/EXPECTED_PRODUCT_FILES.txt -o claude_075_reaudit/checks/EXPECTED_PRODUCT_FILES.txt
diff -u claude_075_reaudit/checks/EXPECTED_PRODUCT_FILES.txt claude_075_reaudit/checks/ACTUAL_PRODUCT_FILES.txt > claude_075_reaudit/checks/PRODUCT_SCOPE_DIFF.txt || {
  cat claude_075_reaudit/checks/PRODUCT_SCOPE_DIFF.txt >&2
  exit 1
}
printf 'PASS: only the six reviewed Hifz product files changed from 5696 to final.\n' > claude_075_reaudit/checks/PRODUCT_SCOPE_ASSERTION.txt

# Explicit forbidden-scope scan across product trees.
git diff --name-only "$ORIGINAL_CANDIDATE..$CANDIDATE" -- hifz-app/src/main app/src/main \
  | grep -Ei '(audio|tafsir|boox|MushafView|reader\.js$|index\.html$)' \
  > claude_075_reaudit/checks/FORBIDDEN_SCOPE_HITS.txt || true
[[ ! -s claude_075_reaudit/checks/FORBIDDEN_SCOPE_HITS.txt ]] || { cat claude_075_reaudit/checks/FORBIDDEN_SCOPE_HITS.txt >&2; exit 1; }
printf 'PASS: no Audio/Tafsir/BOOX/MushafView/reader.js/index.html product delta from 5696 to final.\n' > claude_075_reaudit/checks/FORBIDDEN_SCOPE_ASSERTION.txt

# Immutable published 0.7.4 APK and real Mushaf geometry.
curl -fL -H "$AUTH" -H 'Accept: application/octet-stream' "$API/releases/assets/$OFFICIAL_074_ASSET_ID" -o claude_075_reaudit/data/Quran-Hifz-0.7.4-official.apk
echo "$OFFICIAL_074_APK_SHA  claude_075_reaudit/data/Quran-Hifz-0.7.4-official.apk" | sha256sum -c -
unzip -p claude_075_reaudit/data/Quran-Hifz-0.7.4-official.apk assets/reader109/geometry.json > claude_075_reaudit/data/geometry.json

# Preserve the prior self-contained independent-audit kit as immutable evidence when still available.
mkdir -p /tmp/original_kit_artifact claude_075_reaudit/evidence/original_kit
if curl -fL -H "$AUTH" -H 'Accept: application/octet-stream' "$API/actions/artifacts/$ORIGINAL_KIT_ARTIFACT_ID/zip" -o /tmp/original_kit_artifact.zip; then
  unzip -q /tmp/original_kit_artifact.zip -d /tmp/original_kit_artifact
  original_zip="$(find /tmp/original_kit_artifact -type f -name 'CLAUDE_QURAN_HIFZ_0.7.5_SELF_CONTAINED.zip' -print -quit || true)"
  if [[ -n "$original_zip" ]]; then
    cp "$original_zip" claude_075_reaudit/evidence/original_kit/
    echo "$ORIGINAL_KIT_SHA  claude_075_reaudit/evidence/original_kit/CLAUDE_QURAN_HIFZ_0.7.5_SELF_CONTAINED.zip" | sha256sum -c -
  else
    echo "Original artifact wrapper did not contain the expected nested ZIP." > claude_075_reaudit/evidence/original_kit/UNAVAILABLE.txt
  fi
else
  echo "Original artifact expired/unavailable; candidate source, geometry, diffs, and CI evidence remain self-contained." > claude_075_reaudit/evidence/original_kit/UNAVAILABLE.txt
fi

# Download exact CI metadata and logs, including the RED that proved the migration loss.
for run in "$FINAL_RUN" "$PRECLEAN_GREEN_RUN" "$MIGRATION_RED_RUN" "$ROUND1_VALID_RED_RUN" "$ORIGINAL_FINAL_RUN"; do
  curl -fsSL -H "$AUTH" -H 'Accept: application/vnd.github+json' "$API/actions/runs/$run" > "claude_075_reaudit/ci/run_${run}.json"
  curl -fsSL -H "$AUTH" -H 'Accept: application/vnd.github+json' "$API/actions/runs/$run/jobs?per_page=100" > "claude_075_reaudit/ci/run_${run}_jobs.json"
  curl -fL -H "$AUTH" -H 'Accept: application/vnd.github+json' "$API/actions/runs/$run/logs" -o "claude_075_reaudit/ci/run_${run}_logs.zip" || true
done
mkdir -p claude_075_reaudit/ci/final_logs
unzip -q claude_075_reaudit/ci/run_${FINAL_RUN}_logs.zip -d claude_075_reaudit/ci/final_logs

grep -R -F 'BEHAVIOR_JVM tests=166 failures=0 errors=0 skipped=0' claude_075_reaudit/ci/final_logs >/dev/null
grep -R -F 'SOURCE_CONTRACT tests=71 failures=0 errors=0 skipped=0' claude_075_reaudit/ci/final_logs >/dev/null
grep -R -F 'CORE_JVM tests=20 failures=0 errors=0 skipped=0' claude_075_reaudit/ci/final_logs >/dev/null
grep -R -F 'Starting 45 tests' claude_075_reaudit/ci/final_logs >/dev/null
grep -R -F 'Finished 45 tests' claude_075_reaudit/ci/final_logs >/dev/null

cat > claude_075_reaudit/evidence/CI_SUMMARY.txt <<EOF
EXACT FINAL CI
run=$FINAL_RUN
sha=$CANDIDATE
BEHAVIOR_JVM=166 failures=0 errors=0 skipped=0
SOURCE_CONTRACT=71 failures=0 errors=0 skipped=0
CORE_JVM=20 failures=0 errors=0 skipped=0
ANDROID_INSTRUMENTED=45 failures=0 skipped=0
Emulator=API35 x86_64 pixel_6

RED evidence
migration_zero_loss_run=$MIGRATION_RED_RUN
expected itqanRep=9, observed=0 before fix
EOF

cat > claude_075_reaudit/README_FIRST.md <<'EOF'
# Quran Hifz 0.7.5 — independent Claude re-audit kit

This package is deliberately self-contained. Do not use GitHub or the network.
Audit the exact candidate identified in `evidence/EXACT_IDENTITY.txt`; do not silently substitute another SHA.

The candidate source is in `candidate_source/`. The originally audited 5696 candidate is in `original_candidate_source/`.
The exact deltas, CI metadata/logs, immutable 0.7.4 APK, real `geometry.json`, scope assertions, and prior kit (when artifact retention allows) are included.

Treat supplied CI results and assertions as evidence to verify, not as conclusions to trust.
Start with `RE_AUDIT_INSTRUCTIONS.md`.
EOF

cat > claude_075_reaudit/RE_AUDIT_INSTRUCTIONS.md <<EOF
# MISSION — RE-AUDIT INDÉPENDANT FINAL QURAN HIFZ 0.7.5

Audit EXACTEMENT le candidat:
$CANDIDATE

Candidat initial audité précédemment:
$ORIGINAL_CANDIDATE

Dernière release de référence:
$RELEASE_074

Tu n'as besoin d'aucun accès GitHub/réseau: tout le nécessaire est dans ce ZIP.
Ne fais confiance ni au résumé ni aux affirmations de correction: vérifie le code, les tests, les diffs, les logs et la géométrie fournis.

## Invariants obligatoires
- Stabilisation/Consolidation utilisent les lineIds physiques du Mushaf, jamais de simples bornes de versets.
- Jamais de franchissement PAGE ou SOURATE; ordre physique conservé; une unité peut couper au milieu d'un même verset.
- Identité d'unité = liste exacte de lineIds.
- Taille par segment de sourate/page: <=11 => 1; 12=>6+6; 13=>6+7; 14=>7+7; 15=>7+8.
- Page 48 / 2:282 = 15 lignes physiques et doit être 7+8.
- Consolidation: groupes 1→2→3; une seule session OPEN; snapshot/ordre/protocole/stage/nextUnitIndex/donePerStage gelés; restauration process-death exacte; persistance après chaque répétition; répétitions groupées ne modifient pas les compteurs individuels; vecteurs LEARNING37/LIGHT/FULL inchangés; LIGHT→FULL après 3 échecs inchangé.
- Chaîne: Apprentissage → Appris → Stabilisation → Stabilisé → Consolidation → Acquis → Révision.
- Cadence: lun/mer/ven Apprentissage; mar/jeu Stabilisation; sam/dim Révision. Consolidation est déclenchée par progression, sans jour fixe. Révision/J10 uniquement ACQUIRED.
- Schéma 6: préserver frontières physiques/ranges; pas de coalescence destructrice aux frontières de sourate.
- Hors scope: Audio, Tafsir, BOOX, contenu publié 0.7.4, vecteurs Consolidation, J10 sauf régression prouvée, renommage global de clés.

## Constats historiques à revalider
1. Consolidation réellement atteignable depuis l'UI, mais ne doit pas affamer la séance planifiée après une Consolidation déjà validée le même jour.
2. Ownership physique correct aux frontières; une entrée sans ligne propriétaire ne doit ni crasher Home ni empoisonner la queue.
3. Projection Home/hebdo = même planificateur physique que le runtime.
4. Migration 5→6: remap 5/5/5→7+8 sécurisé; aucune fausse acquisition.
5. Migration zéro-perte: si l'unité ouverte historique reste exactement compatible avec UNE unité physique nouvelle et aucun ancien sous-bloc n'est déjà crédité, conserver rep/assistances/révélations/chrono. Si remap ou crédit partiel, reset sécurisé permis.
6. Ranges: adjacence dans une même sourate peut fusionner; une range persistée multi-sourates ne doit pas être éclatée arbitrairement; aucune frontière physique utile ne doit être perdue.
7. Session Consolidation gelée invalidée si édition manuelle pertinente.
8. Exceptions de validation contenues sans crash applicatif.
9. Page 48 / 2:282 sur géométrie réelle: 15 lignes, unités 7+8 distinctes.
10. Aucun delta produit Audio/Tafsir/BOOX/MushafView/reader.js/index.html depuis le candidat initial audité.

## Preuves CI à vérifier
Final exact run: $FINAL_RUN, SHA $CANDIDATE.
Attendu dans les logs fournis: 166 behavior JVM, 71 source contracts, 20 core JVM, 45 tests Android, tous sans échec ni skip.
Le run $MIGRATION_RED_RUN doit prouver le RED migration: progression compatible attendue à 9 répétitions mais observée à 0 avant correction.

## Verdict obligatoire
Après audit indépendant, termine par EXACTEMENT l'une des deux lignes:
AUDIT CLAUDE GREEN
ou
AUDIT CLAUDE NO-GO

En cas de NO-GO, liste chaque blocker/major avec fichier, méthode/zone, scénario reproductible et test manquant ou échouant. Ne donne aucun GO_PHONE.
EOF

# Internal hashes first, then immutable outer archive hash.
(
  cd claude_075_reaudit
  find . -type f ! -name SHA256SUMS -print0 | sort -z | xargs -0 sha256sum > SHA256SUMS
)
zip -qr "CLAUDE_QURAN_HIFZ_0.7.5_REAUDIT_${CANDIDATE:0:8}.zip" claude_075_reaudit
sha256sum "CLAUDE_QURAN_HIFZ_0.7.5_REAUDIT_${CANDIDATE:0:8}.zip" > "CLAUDE_QURAN_HIFZ_0.7.5_REAUDIT_${CANDIDATE:0:8}.zip.sha256"
