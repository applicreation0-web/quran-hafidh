#!/usr/bin/env bash
# Disposable CI emulator only. No private corpus, official package, APK delivery or signing.
set -euo pipefail
surface="${1:?surface}";size="${2:?size}";density="${3:?density}";phase="${4:?phase}"
audit_dir="hifz-app/build/endurance-$surface-$phase"
mkdir -p "$audit_dir"
target=com.quransafeguard.hifz.ibnkathirtest
collector_pid=''
log_pid=''
event_pid=''
collect() {
  if [ -n "$collector_pid" ]; then kill "$collector_pid" 2>/dev/null || true; wait "$collector_pid" 2>/dev/null || true; fi
  for stream_pid in "$log_pid" "$event_pid"; do
    if [ -n "$stream_pid" ]; then kill "$stream_pid" 2>/dev/null || true; wait "$stream_pid" 2>/dev/null || true; fi
  done
  log_pid='';event_pid='' 
  adb shell dumpsys meminfo "$target" > "$audit_dir/meminfo-final.txt" || true
  adb shell dumpsys activity exit-info "$target" > "$audit_dir/exit-info.txt" || true
  adb pull /sdcard/Pictures/HaafidhAudit "$audit_dir/" || true
}
trap collect EXIT
adb shell wm size "$size"
adb shell wm density "$density"
adb logcat -c
adb logcat -b events -c
adb logcat -v threadtime > "$audit_dir/logcat.txt" 2>&1 &
log_pid=$!
adb logcat -b events -v threadtime > "$audit_dir/events.txt" 2>&1 &
event_pid=$!
(
  while true; do
    date -u +%FT%TZ
    adb shell dumpsys meminfo "$target" || true
    adb shell dumpsys meminfo | head -100 || true
    sleep 20
  done
) > "$audit_dir/memory-timeline.txt" 2>&1 &
collector_pid=$!
class=com.quransafeguard.hifz.preview
if [ "$phase" = navigation ]; then
  gradle --no-daemon :hifz-app:connectedDebugAndroidTest \
    -Pandroid.testInstrumentationRunnerArguments.class="$class.RevisionAtlasInstrumentedTest#passiveNestedComparisonPausesClockAndRestoresExactSessionAndBackup" \
    -Pandroid.testInstrumentationRunnerArguments.atlasEnduranceCycles=300
elif [ "$phase" = tafsir ]; then
  gradle --no-daemon :hifz-app:connectedDebugAndroidTest \
    -Pandroid.testInstrumentationRunnerArguments.class="$class.AtlasDiscriminantInstrumentedTest#enduranceTafsirRotationsAndQcfDisposal,$class.AtlasDiscriminantInstrumentedTest#changingCaseImmediatelyClearsOldQcfExcerpts,$class.AtlasDiscriminantInstrumentedTest#exactVerseInEitherAuthenticQcfPageOpensItsOwnTafsir" \
    -Pandroid.testInstrumentationRunnerArguments.atlasEndurance=true
else
  echo 'Unknown endurance phase' >&2;exit 1
fi
collect
collector_pid=''
python3 - "$audit_dir" "$phase" <<'PY'
from pathlib import Path
import re,sys,json
root=Path(sys.argv[1]);phase=sys.argv[2]
log=(root/'logcat.txt').read_text(errors='replace')
events=(root/'events.txt').read_text(errors='replace')
# Count each completed operation once, using only its distinct completion number.
pattern=r'phase='+('navigation' if phase=='navigation' else 'tafsir-close')+r' completed=(\d+)'
completed=set(map(int,re.findall(pattern,log)))
expected=300 if phase=='navigation' else 200
assert completed==set(range(1,expected+1)),f'Incomplete evidence: {len(completed)}/{expected}; logcat may have truncated'
assert 'am_proc_start' in events and 'com.quransafeguard.hifz.ibnkathirtest' in events,'Missing usable target lifecycle event evidence'
assert '--------- beginning of' in log,'Missing live logcat collector evidence'
failures=[line for line in events.splitlines() if re.search(r'am_(crash|anr)',line) and 'com.quransafeguard.hifz.ibnkathirtest' in line]
assert not failures,failures
measurements=[dict(zip(('pss_kib','native_alloc_bytes','java_used_bytes'),map(int,m)))
  for m in re.findall(r'HaafidhEndurance.*pss_kib=(\d+) native_alloc_bytes=(\d+) java_used_bytes=(\d+)',log)]
assert measurements,'Missing in-process memory measurements'
summary={'phase':phase,'completed':len(completed),'target_crash_or_anr_events':len(failures),
 'memory_samples':len(measurements),'first':measurements[0],'last':measurements[-1],
 'peak_pss_kib':max(m['pss_kib'] for m in measurements),
 'limitation':'PSS sampling and detached WebViews do not prove absence of all renderer/native leaks; no physical E-Ink test.'}
if phase=='tafsir':
 assert 'SUMMARY tafsir_open_close=200 rotations=100 qcf_side_selections=400' in log
 summary.update(tafsir_open_close=200,rotations=100,qcf_side_selections=400,close_method='Leave Atlas; no separate panel-close control exists')
(root/'summary.json').write_text(json.dumps(summary,indent=2)+'\n')
print(json.dumps(summary,indent=2))
PY
