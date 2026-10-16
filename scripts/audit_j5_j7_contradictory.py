#!/usr/bin/env python3
"""Contradictory J5-J7 static audit, complementing (never replacing) JVM tests.

Each rule deliberately checks a counterexample/risk: a repeated credit, a
post-midnight inherited session, unauthorized corpus growth, a
Sunday passive cursor advance, a missing first-two-word QCF fallback, or a
silent weakening of BOOX vector contracts. This is NOT an on-device certificate.
"""
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/"hifz-app/src/main/java/com/quransafeguard/hifz/preview"
def source(name): return (JAVA/name).read_text(encoding="utf-8")
p=source("HifzPrefs.java")
h=source("HifzSessionActivity.java")
m=source("MainActivity.java")
w=source("WeeklyDashboardPlanner.java")
s=source("SettingsActivity.java")
core=source("ConsolidationCycleEngine.java")

k=source("IbnKathirFirstWordsCorpus.java")
r=source("SemanticPassageRepository.java")
checks=[
 ("J1 exact deterministic 1903 QCF two-word starts", 'index.count()!=1903' in k and 'EXPECTED_QCF_WORDS = 77432' in k and 'group.startAyah+":1"' in k and "inside?2:1" in k),
 ("J1 old recall masks frozen, Lecture Ayah Themes source separate", "IbnKathirFirstWordsCorpus.load(wordGeometry)" in r and "semanticPassages.readerCuesForPage(page)" in h and "ayahThemesCues.forPage(page)" in source("StudyReaderActivity.java") and "IbnKathirQuranComPrivateTitles.approvedTitle(this,group)" not in source("StudyReaderActivity.java")),
 ("J1 fail-open masked recall", "if (cues.length() == 0) itqanAnchorFailOpen = true;" in h and "hasCompleteExactGeometryForPage(currentPage)" in h),
 ("J2 new x40 and frozen old plans", "BALANCED_20_5_5_5_5" in source("ItqanRegimeStore.java") and "next<=20" in source("ItqanBalancedMaskSchedule.java")),
 ("J2 completion lines physical same surah", "if(endOfPhysicalPage || endOfSurah)" in source("ItqanCompletionBoundaryPolicy.java") and "!onlySurah(candidate,surah)" in source("ItqanCompletionBoundaryPolicy.java")),
 ("J3 date-scoped programme", "weeklyProgramFor(date)" in m and "prefs.weeklyProgramFor(day)" in source("WeeklyProgramPolicy.java")),
 ("J4 x7 and previous continuous x1", "case SNOWBALL_STABILIZATION_SEVEN:" in core and "copy(7, 0, 0, 0, 0)" in core and "case SNOWBALL_CONTINUOUS:" in core and "copy(1, 0, 0, 0, 0)" in core),
 ("J4 unchanged learning x10", "case SNOWBALL:" in core and "copy(10, 0, 0, 0, 0)" in core),
 ("J5 atomic snapshot/date consumption", "snowballCompletedSnapshotKey(family),snapshot" in p and ".remove(consolidationStateKey(family)).commit();" in p and "if(!snowballEveningPending(family,day))return false;" in p),
 ("J5 separate same cumulative and new accumulation", "snowballSnapshot(family,date)" in p and "return !snapshot.isEmpty()" in p and "mondayOf(date).toString()" in p),
 ("J5 no previously Acquis weekly final reappearance", "if (weekFullyAcquired(week, acquired)) continue;" in p),
 ("J5 no double final promotion", 'date.equals(p.getString("lastRecentSabqiReviewDate",""))' in p and 'date.equals(p.getString("lastLearningConsolidationDate",""))' in p),
 ("J5 refuse cross-date frozen session mixing", "consolidationSession.cycle().cycleId().equals(cycleIdPrefix+today)" in h),
 ("J6 Appris only adds to passive, no acquired promotion", "public EligibleCorpus murajaahCorpus(GeometryRepository geometry)" in p and 'v6LineIdSet("v6LearnedLineIds")' in p and "geometry.allVersesFullyCredited(eligible)" in p and "newlyEligible.add(verse)" in p),
 ("J6 passive enhanced active independent", "prefs.murajaahCorpus(geometry)" in h and "prefs.activeMurajaahCorpus()" in h and "activeMurajaahCorpus()" in p),
 ("J6 no Sunday revision in both programmes", "if(today.getDayOfWeek()!=DayOfWeek.SUNDAY)" in m and "No active/passive review on Sunday in EITHER programme" in m and "Sunday cannot consume a passive cursor" in w),
 ("J6 05:30 and 20:00 editable and nonlocking", 'hifzSuggestedMorningHHmmV1","05:30"' in p and 'hifzSuggestedEveningHHmmV1","20:00"' in p and "TimePickerDialog" in s),
 ("J7 Quiz code fully removed, independent Hifz progression intact",
   all(not (JAVA/name).exists() for name in ("QuizActivity.java","QuizCorpus.java","QuizHistory.java","QuizQuestion.java"))
   and "transitionV6Lines" in p and "completeConsolidationSessionV6" in p),
 ("J7 new manual schedule is seven full days",
   "MANUAL_SEVEN" in p and "7-learningDaysPerWeek()" in p
   and "day==DayOfWeek.SUNDAY ? CadenceAction.STABILIZATION" in source("WeeklyProgramPolicy.java")),
 ("J7 BOOX 23-vector immutable audit exists", (ROOT/"docs/FROZEN_UI_ICONS_13_PLUS_10.tsv").exists() and (ROOT/"scripts/verify_contractual_ui_icons.py").exists()),
 ("J7 exact Hifz/Quiz baseline parity gate exists", (ROOT/"scripts/audit_hifz_quiz_against_1171.sh").exists()),
]
for name,ok in checks:
    print(("PASS" if ok else "FAIL")+": "+name)
assert all(ok for _,ok in checks), "Contradictory audit uncovered an unprotected behaviour"
print(f"PASS: {len(checks)} contradictory source checks; JVM and real BOOX testing remain separate.")
