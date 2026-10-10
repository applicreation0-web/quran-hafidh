#!/usr/bin/env python3
"""Only the three explicitly authorized QCF fail-open HifzSessionActivity changes.

Compare byte-for-byte to the immutable official 1.17.1 source after applying
three exact substitutions. A future unrelated Hifz engine change fails CI.
"""
from pathlib import Path
import subprocess

BASE="7fd45bbe3d2e67a2e21c6f8ca1d5ae935d1f0d21"
PATH="hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java"
baseline=subprocess.check_output(["git","show",f"{BASE}:{PATH}"]).decode("utf-8")
replacements=[
(
"""        mushaf.clearPageLandmarkBoxes();
        if (itqanAnchorFailOpen) { mushaf.clearSemanticCues(); return; }
        mushaf.setSemanticCues(cues, true, false);""",
"""        mushaf.clearPageLandmarkBoxes();
        // A unit that has no Ibn Kathir beginning on the displayed page must
        // remain visible; a totally covered page is not a valid recall aid.
        if (cues.length() == 0) itqanAnchorFailOpen = true;
        if (itqanAnchorFailOpen) { mushaf.clearSemanticCues(); return; }
        mushaf.setSemanticCues(cues, true, false);"""
),
(
"""    /** The mask actually drawn: an anchored page without exact anchors stays readable. */
    private int displayedMask() {
        return ITQAN.equals(mode) && itqanAnchorFailOpen ? 0 : currentMask;
    }""",
"""    /** Fail open on active-recall pages with incomplete QCF landmark/cue geometry too. */
    private int displayedMask() {
        if (ITQAN.equals(mode) && itqanAnchorFailOpen) return 0;
        if (MURAJAAH_ACTIVE.equals(mode) && currentMask > 0
                && (semanticPassages == null || !semanticPassages.isAvailable()
                    || !semanticPassages.hasCompleteExactGeometryForPage(currentPage)))
            return 0;
        return currentMask;
    }"""
),
(
"""view.setPressed(false);mushaf.setMask(currentMask);if(action==android.view.MotionEvent.ACTION_UP)view.performClick();return true;""",
"""view.setPressed(false);mushaf.setMask(displayedMask());if(action==android.view.MotionEvent.ACTION_UP)view.performClick();return true;"""
),
]
for before,after in replacements:
    assert baseline.count(before)==1,"Official 1.17.1 source no longer matches expected Hifz code."
    baseline=baseline.replace(before,after)
actual=Path(PATH).read_text(encoding="utf-8")
assert baseline == actual, ("UNAUTHORIZED HifzSessionActivity delta: "
    "only the three verified Ibn Kathir fail-open substitutions are allowed")
print("PASS: HifzSessionActivity matches official v1.17.1 except three exact QCF fail-open deltas")
