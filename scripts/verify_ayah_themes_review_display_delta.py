#!/usr/bin/env python3
"""Exact UI-only substitutions over the immutable 491aae Hifz session source.
No changes to masks, geometry, engines, counters or persistence are permitted.
"""
from pathlib import Path
import hashlib,subprocess
PATH="hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java"
FROZEN_BLOB="491aae573afba8bdd5da339a8e15dca8e59661ef"
CHANGES=[['    private SemanticPassageRepository semanticPassages;\n', '    private SemanticPassageRepository semanticPassages;\n    private AyahThemesCueIndex ayahThemesTitles;\n'], ['            semanticPassages = SemanticPassageRepository.shared(this);\n', '            semanticPassages = SemanticPassageRepository.shared(this);\n            ayahThemesTitles = AyahThemesCueIndex.load(this);\n'], ['        if (semanticPassages == null) return;\n        SemanticPassageRepository.Cue cue = semanticPassages.cue(passageId);\n        if (cue != null) semanticTitleDialog = SemanticTitlePopup.show(this, cue.title, semanticTitleDialog);\n', '        if(semanticTitleDialog!=null&&semanticTitleDialog.isShowing())semanticTitleDialog.dismiss();\n        semanticTitleDialog=null;\n        if(ayahThemesTitles==null||passageId==null||!passageId.startsWith("AT:"))return;\n        String title=ayahThemesTitles.titleForVisibleStart(currentPage,passageId.substring(3));\n        if(title==null)return;\n        semanticTitleDialog=SemanticTitlePopup.showEnglish(this,title,null);\n        SemanticTitlePopup.positionAboveIcons(this,semanticTitleDialog,actions);\n'], ['            restoreMurajaahEndpointSelectionOnCurrentPage();\n        }\n', '            restoreMurajaahEndpointSelectionOnCurrentPage();\n        }\n        if(semanticTitleDialog!=null&&semanticTitleDialog.isShowing())semanticTitleDialog.dismiss();\n        semanticTitleDialog=null;\n        boolean titleTargets=isMurajaahMode() || (ITQAN.equals(mode) && currentMask==100\n            && itqanUnit!=null && ItqanMaintenancePolicy.anchoredRecallRep(itqanRegime,prefs.itqanRep()));\n        if(titleTargets)AyahThemesReviewDisplay.apply(this,mushaf,ayahThemesTitles,page);\n']]

def expected_source(baseline):
    blob=baseline.encode()
    assert hashlib.sha1(b"blob "+str(len(blob)).encode()+b"\0"+blob).hexdigest()==FROZEN_BLOB
    for old,new in CHANGES:
        assert baseline.count(old)==1,"ambiguous approved UI insertion"
        baseline=baseline.replace(old,new,1)
    return baseline

def main():
    frozen=subprocess.check_output(["git","show",FROZEN_BLOB],text=True)
    assert Path(PATH).read_text()==expected_source(frozen),"Unapproved delta outside exact Ayah Themes display hooks"
    subprocess.run(["node","scripts/test_ayah_themes_review_overlay.js"],check=True)
    print("PASS: exact independent Ayah Themes title display only; pinned Hifz mask/engine source unchanged")

if __name__=="__main__":main()
