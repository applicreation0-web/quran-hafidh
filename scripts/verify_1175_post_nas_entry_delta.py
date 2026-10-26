#!/usr/bin/env python3
"""Exact post-Nas NEW-session routing guard. Frozen OPEN and every writer unchanged."""
from pathlib import Path
import subprocess
BASE='f0f0c89285a89b20a73fc32b5594434f23bfce33'
ROOT='hifz-app/src/main/java/com/quransafeguard/hifz/preview/'
CHANGES={'MainActivity.java': {'sha': '8e329bb967da7d69bf92d99f24aad98d88376e69', 'replacements': [(680, 681, ['        if(prefs.itqanPostNasMaintenance())return HifzSessionActivity.ITQAN;\n']), (682, 683, ['        return today.getDayOfWeek()==DayOfWeek.SUNDAY && !prefs.itqanPostNasMaintenance()\n']), (704, 705, ['                    && (prefs.itqanPostNasMaintenance() || consolidationFinalResolved(date)));\n']), (713, 714, ['                    && (prefs.itqanPostNasMaintenance() || consolidationFinalResolved(date));\n']), (788, 789, ['            if(!prefs.itqanPostNasMaintenance() && !consolidationFinalResolved(date))\n']), (802, 803, ['                if(!prefs.itqanPostNasMaintenance() && !consolidationFinalResolved(today))\n']), (948, 949, ['            if(!prefs.itqanPostNasMaintenance()\n']), (969, 970, ['                if(!prefs.itqanPostNasMaintenance()\n']), (1109, 1110, ['        boolean stabilization=(!prefs.itqanPostNasMaintenance() || historicalItqanFinal)\n'])]}, 'HifzSessionActivity.java': {'sha': '09b2913b65d077ed7c9fcd3ef6b26a18de4e06d5', 'replacements': [(699, 699, ['            return;\n', '        }\n', '        // The physical An-Nas latch forbids every NEW final, including a\n', '        // stale/direct intent or an explicit programme choice. A real OPEN\n', '        // first-regime final still restores its original protocol and date.\n', '        if(prefs.itqanPostNasMaintenance()\n', '                && prefs.unfinishedConsolidationSessionDate(\n', '                    ConsolidationCycleEngine.Family.STABILIZATION,"stabilization-final-")==null) {\n', '            sessionCompleted=true;\n', '            program.setText("Itqān · Maintien de la maîtrise");\n', '            progress.setText("Aucune nouvelle finale Itqān. Reprendre la séance ordinaire d’un hizb ×3.");\n'])]}, 'WeeklyDashboardPlanner.java': {'sha': '8f004d4d08de7aa8f0307e826d91f3e4217dde10', 'replacements': [(199, 200, ['                    boolean itqanFinalDue=!prefs.itqanPostNasMaintenance()||oldItqanFinal;\n'])]}, 'ItqanRegimeStore.java': {'sha': '75c8aaacdceec9c4b8a591b84b7d1493aa15d515', 'replacements': [(100, 101, ['        if("CONSOLIDATION".equals(programme))return reached;\n'])]}}
def canonical(name,source):
    change=CHANGES[name]
    assert subprocess.check_output(["git","hash-object",ROOT+name],text=True).strip()==change['sha'],"Unreviewed post-Nas entry guard: "+name
    original=subprocess.check_output(["git","show",BASE+":"+ROOT+name],text=True)
    lines=original.splitlines(True)
    for start,end,replacement in reversed(change['replacements']):
        lines[start:end]=replacement
    expected=''.join(lines)
    assert expected==source,"Unreviewed change outside entry guard: "+name
    return original
if __name__=="__main__":
    for name in CHANGES:canonical(name,Path(ROOT+name).read_text())
    print("PASS: physical An-Nas latch governs new entry only; old OPEN sessions, writers and protocol vectors untouched")
