#!/usr/bin/env python3
"""Permit only CP08 pre-write schema rejection; original backup pin remains authoritative."""
from pathlib import Path
import hashlib
root=Path(__file__).resolve().parents[1]
source=(root/'hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzBackup.java').read_text()
insertions=['            // A missing schema would initialize defaults over the imported acquisitions.\n            if(!values.has("schema"))\n                throw new JSONException("Schéma de progression absent de la sauvegarde.");\n', '        if(values.has("schema")) {\n            JSONObject schema=values.getJSONObject("schema");\n            Object number=schema.get("v");\n            // JSONObject.getInt coerces strings and fractions; SharedPreferences does not.\n            if(!"i".equals(schema.getString("t")) || !(number instanceof Integer)\n                    || (Integer)number<1 || (Integer)number>PreviewConfig.SCHEMA_VERSION)\n                throw new JSONException("Schéma de progression invalide ou non pris en charge.");\n        }\n']
for insertion in insertions:
    if source.count(insertion)!=1: raise SystemExit('FAIL: changed schema rejection block')
    source=source.replace(insertion,'',1)
raw=source.encode()
blob=hashlib.sha1(b'blob '+str(len(raw)).encode()+b'\0'+raw).hexdigest()
if blob!='4a8c2ab0b34d4de6b33e8b14809f297373ffdbf1': raise SystemExit('FAIL: unexpected backup delta')
print('PASS: only pre-write schema rejection; original adapter otherwise byte-identical')
