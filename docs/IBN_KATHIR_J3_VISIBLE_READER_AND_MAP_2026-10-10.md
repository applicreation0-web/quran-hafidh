# J3 – Visible Ibn Kathir reading integration – 10 October 2026

This commit changes real Android screens, not just audit scripts.

* Lecture shows exact QCF word-location markers at the start of every verified digital Ibn Kathir source group. Markers replace the legacy Al-Munir overlay for Lecture only.
* The user can toggle these using the existing frozen icon and tap each marker to open the group reference, Quran.com Ibn Kathir English tafsir, or the semantic Map.
* Map marks the start of the currently selected group in its real Mushaf preview and opens its source/reference sheet on tap.
* The existing user reading-position and old amorce preference are left intact. A separate opt-in key (ibn_kathir_start_markers_visible) is used; Map read-only previews do not persist it.
* An exact QCF position is not the same as an approved mnemonic: only the first word is highlighted as a location indicator. No editorial choice or artificial quota for final recall amorces is claimed.
* Itqan and Murajaah Active continue to use the legacy repository until reliable mnemonic keys replace it safely. Their engine classes and Quiz source are byte-identical to official 1.17.1.
* JVM tests validate coverage of the 604 physical pages and all 1903 source groups against original QCF word boxes.
* No APK is built in this stage.
