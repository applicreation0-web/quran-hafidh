#!/usr/bin/env python3
"""Additive Itqan two-page FOUR-SESSION MODEL only. Production Hifz stays frozen.

Only the future migration may activate this planner, after separately proved
snapshot, date completion and snowball atomicity. Keep it side-effect free.
"""
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
APP=ROOT/'hifz-app/src/main/java/com/quransafeguard/hifz/preview'
source=(APP/'ItqanFourSessionPolicy.java').read_text(encoding='utf-8')
test=(ROOT/'hifz-app/src/test/java/com/quransafeguard/hifz/preview/ItqanFourSessionPolicyTest.java').read_text(encoding='utf-8')
assert source.count('static final int SESSIONS=4;')==1
assert source.count('static final int MAX_BLOCK_LINES=11;')==1
assert 'surahOf(lines.get(i))==surah' in source
assert 'double target=(double)n/slots;' in source
assert 'if(!flattened.equals(seen)||plan.lines()!=n)' in source
for forbidden in ('SharedPreferences','p.edit()', 'setItqanRep(', 'completeStabilizationBlockV6(',
                  'lastItqanDate', 'setCursor(', 'commit()', 'startActivity(', 'Intent(', 'onClick('):
    assert forbidden not in source,'Itqan planner attempted live mutation: '+forbidden
for contract in ('all604PagesAndEveryPairHaveExactFourWayCoverage',
 'wholeMufassalFromPages562To604NeverDoubleCredits',
 'qafDhariyat519To522NeverMixesSurahs',
 'partialCorpusMustNeverSkipMissingLinesToFillQuota',
 'oneLineNearNasYieldsThreeEmptySlotsWithoutJump',
 'sameLineTwiceRejected'):
    assert contract in test,'Missing adversarial planner regression: '+contract
snapshot=(APP/'ItqanFourSessionSnapshot.java').read_text(encoding='utf-8')
snapshot_test=(ROOT/'hifz-app/src/test/java/com/quransafeguard/hifz/preview/ItqanFourSessionSnapshotTest.java').read_text(encoding='utf-8')
for tag in ('static final int VERSION=1;', 'static ItqanFourSessionSnapshot decode(', 
 'String encode()', 'originalTailCompleted', 'plan.sessions.size()!=4',
 'Corrupt frozen Itqan snapshot; do not replan', 'SHA-256'):
    assert tag in snapshot,"Missing frozen 4-session snapshot contract: "+tag
for tag in ('partiallyCompletedSnapshotPreservesExactSlotAndRep',
 'corruptLineIdentityIsNeverSilentlyRecomputed',
 'alteredProgressOutside40RepsMustFail'):
    assert tag in snapshot_test,"Missing interrupted four-session regression: "+tag
for forbidden in ('SharedPreferences', '.commit()', 'v6AcquiredCreditLineIds',
                  'advanceItqanRotationPast(', 'lastItqanDate'):
    assert forbidden not in snapshot,'Planning snapshot must have no live progression effects'
store=(APP/'ItqanFourSessionStore.java').read_text(encoding='utf-8')
store_test=(ROOT/'hifz-app/src/test/java/com/quransafeguard/hifz/preview/ItqanFourSessionStoreTest.java').read_text(encoding='utf-8')
for tag in ('static final String KEY=', 'ItqanFourSessionSnapshot.decode(raw,geo)',
            'Finish existing legacy Itqan before new window',
            'if(stored==null||!stored.encode().equals(expected.encode()))return false',
            'p.edit().putString(KEY,next.encode()).putInt("itqanRep",next.reps).commit()'):
    assert tag in store,"Missing crash-safe snapshot store contract: "+tag
for tag in ('frozenPlanSurvivesRestartAndRejectsStaleTap',
            'aFailedCommitLeavesTheEntireSnapshotAndRepUntouched',
            'existingOpenLegacySessionCannotBeOverwritten',
            'aBlockCannotAdvanceBeforeFortyRepetitions',
            'aCorruptSnapshotNeverResetsToCurrentGeometry'):
    assert tag in store_test,"Missing atomicity / backward-compatibility regression: "+tag
print('PASS: pure 2-page plan, frozen QCF snapshot, and crash-safe staging store')
