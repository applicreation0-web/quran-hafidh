# CP04 — Stable Quran QCF navigation controller (partial)

Source commit: `8cc1f02a4680af3a7509a82134b23802a6427716`

The MawdudiMapModel.Navigation class provides **model-only** navigation over the 1,053 canonical source-index passage ranges in all 114 surahs. It shares true QCF geometry and the exact final-ayah physical page fix with the Al-Baqarah pilot. `Snapshot` saves stable passage ID, ordinal, page, map scroll, commentary scroll, and commentary visibility. Restoration refuses mismatched stable IDs (fail closed), clamps pages, and never mutates Sabqi, Itqān, consolidation, reinforcement, planning or Hifz persistence.

New JVM regression methods traverse the entire canonical index, check reader jump verses against the QCF selected passage, restore saved positions, traverse 1→2, and reject stale ID. Test CI still pending on this commit.

**NOT COMPLETED:** authenticated editorial themes/titles for 114, real general graphic map on Android (old Ibn Kathir screen remains runtime for surahs other than Al-Baqarah), authentic QCF thumbnail presentation inside that new map, two-way connected UI, physical BOOX and phone visual review, 556 corrected/approved commentaries and notes, rights clearance, signed private final APK and launch proof. Do not assert any of these from the model-only controller.
