#!/usr/bin/env python3
"""Exact navigation + V1 physical-continuity exception authorized 26 Oct.
Reconstruct every pre-existing production byte against the audited 71077a2.
No credit, repetition, mask, initialization or backup writer is exempted.
"""
from pathlib import Path
import subprocess
BASE="71077a2f921a74a1f756fa3a8cbb49295e8ef309"
ROOT="hifz-app/src/main/java/com/quransafeguard/hifz/preview/"
HASHES={'HifzPrefs.java': '3c8a9b63c3f8835b57ea80a2059539d50469d1fe', 'HifzSessionActivity.java': '9812dd7c9135f8dbb1ff77ca88e06bed14929585', 'MainActivity.java': '131e4f58140df16559a4fddbd0f098a685431028', 'WeeklyDashboardPlanner.java': '68cd39bc69208b7c3d9f8d46d86a456b2c2170c9'}
def canonical(name, source):
    assert subprocess.check_output(["git","hash-object",ROOT+name],text=True).strip()==HASHES[name], "Unreviewed continuity adapter: "+name
    if name=="HifzPrefs.java":
        source=source.replace("        AnchoringQueue.Entry remainder=legacyFourDateRemainder(geometry);\n        if(remainder!=null)return remainder;\n", "",1)
        for begin,end in [("    /** Compatibility at the exact V1 -> ordinary boundary only.","    /** Read-only position of the current Stabilisation unit"),
                          ("    /** Navigation date only: retain the earliest ungraduated week's Sunday,","    public String lastLearningSnowballEveningDate()")]:
            a,b=source.index(begin),source.index(end)
            source=source[:a]+source[b:]
    elif name=="HifzSessionActivity.java":
        source=source.replace("prefs.ordinaryItqanLineIds(savedStart,savedEnd,geometry)","CorpusLinePolicy.ownedLineIdsForRangeOnPage(savedStart,savedEnd,geometry)",1)
        source=source.replace("prefs.ordinaryItqanLineIds(entryStart, entryEnd, geometry)","CorpusLinePolicy.ownedLineIdsForRangeOnPage(entryStart, entryEnd, geometry)",1)
    elif name=="MainActivity.java":
        source=source.replace("LocalDate sunday=prefs.pendingLearningFinalDate(today);","LocalDate sunday=today.minusDays(today.getDayOfWeek().getValue()%7);",1)
        source=source.replace("if(sunday!=null && sunday.isBefore(today)","if(sunday.isBefore(today)",1)
        source=source.replace("prefs.ordinaryItqanLineIds(start,end,geometry)","CorpusLinePolicy.ownedLineIdsForRangeOnPage(start,end,geometry)",1)
    elif name=="WeeklyDashboardPlanner.java":
        source=source.replace("List<String> owned=itqanForecast.isInitialUnit()\n                            ?prefs.ordinaryItqanLineIds(start,end,geometry)\n                            :CorpusLinePolicy.ownedLineIdsForRangeOnPage(start,end,geometry);", "List<String> owned=CorpusLinePolicy.ownedLineIdsForRangeOnPage(start,end,geometry);",1)
    baseline=subprocess.check_output(["git","show",BASE+":"+ROOT+name],text=True)
    assert source==baseline,"Unexpected change outside physical compatibility / original-date navigation: "+name
    return source
if __name__=="__main__":
    for name in HASHES: canonical(name,Path(ROOT+name).read_text())
    print("PASS: exact V1 physical adapter and Sabqi original-date navigation only; all existing writers byte-identical")
