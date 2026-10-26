#!/usr/bin/env python3
"""Exact display-only 1.17.5 Itqan vocabulary; persisted choice keys unchanged."""
from pathlib import Path
import subprocess
BASE='f0f0c89285a89b20a73fc32b5594434f23bfce33'
ROOT='hifz-app/src/main/java/com/quransafeguard/hifz/preview/'
CHANGES={'SettingsActivity.java': {'sha': '3f0d47355e58adfa5dc6a90bcf690340573c099c', 'replacements': [(180, 181, ['            "Précision et fluidité de la récitation. Dans Haafidh : Consolidation, puis Maintien de la maîtrise après l’arrivée à An-Nās.");\n']), (380, 380, ['        if(prefs.itqanPostNasMaintenance())return "Itqān · Maintien de la maîtrise · 1 hizb ×3";\n']), (385, 386, ['            return "Itqān · Maintien de la maîtrise · en attente de fin de parcours · dès le "+from;\n']), (387, 388, ['            return ("ENTRETIEN".equals(mode)?"Itqān · Maintien de la maîtrise · hizb ×3":"Itqān · Consolidation · approfondie")\n']), (389, 390, ['        return ("ENTRETIEN".equals(mode)?"Itqān · Maintien de la maîtrise · hizb ×3":"Itqān · Consolidation · approfondie")\n']), (395, 395, ['        if(prefs.itqanPostNasMaintenance()) {\n', '            new AlertDialog.Builder(this).setTitle("Itqān · Maintien de la maîtrise")\n', '                .setMessage("1 hizb réel par séance : deux lectures visibles et une récitation masquée avec amorces. Les séances déjà ouvertes conservent leur protocole.")\n', '                .setPositiveButton("Fermer",null).show();\n', '            return;\n', '        }\n']), (396, 398, ['            "Itqān · Consolidation · protocole approfondi",\n', '            "Itqān · Maintien de la maîtrise · 1 hizb ×3"\n']), (412, 413, ['                            ? "\\nLe maintien commencera au plus tôt après l\'arrivée effective à An-Nās."\n'])]}, 'HifzDisplayVocabulary.java': {'sha': 'd30c6cfc475a95497660f1f2167c0b0635e91c10', 'replacements': [(11, 12, ['            .replace("Entretien · Itqān","__ITQAN_MAINTENANCE_REVERSED__")\n', '            .replace("Itqān · Maintien de la maîtrise","__ITQAN_MAINTENANCE__")\n', '            .replace("Itqān · Consolidation","__ITQAN_CONSOLIDATION__");\n']), (28, 30, ['  .replace("__ITQAN_MAINTENANCE__", "Itqān · Maintien de la maîtrise")\n', '  .replace("__ITQAN_MAINTENANCE_REVERSED__", "Itqān · Maintien de la maîtrise")\n', '  .replace("__ITQAN_CONSOLIDATION__", "Itqān · Consolidation");\n'])]}}
for name,change in CHANGES.items():
    source=subprocess.check_output(["git","show",BASE+":"+ROOT+name],text=True)
    lines=source.splitlines(True)
    for start,end,replacement in reversed(change['replacements']):
        lines[start:end]=replacement
    source=''.join(lines)
    assert source==Path(ROOT+name).read_text(),"Unexpected functional or initialization change: "+name
    assert subprocess.check_output(["git","hash-object",ROOT+name],text=True).strip()==change['sha']
print("PASS: exact display labels only; programme keys, dates and initialization untouched")
