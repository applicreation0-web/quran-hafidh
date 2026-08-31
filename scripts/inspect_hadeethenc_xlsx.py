#!/usr/bin/env python3
import sys
import zipfile
import xml.etree.ElementTree as ET
from pathlib import Path

NS = {"m": "http://schemas.openxmlformats.org/spreadsheetml/2006/main",
      "r": "http://schemas.openxmlformats.org/officeDocument/2006/relationships"}

def col_index(ref):
    letters = "".join(ch for ch in ref if ch.isalpha())
    value = 0
    for ch in letters:
        value = value * 26 + (ord(ch.upper()) - 64)
    return value - 1

def read_rows(path, limit=8):
    with zipfile.ZipFile(path) as z:
        shared = []
        if "xl/sharedStrings.xml" in z.namelist():
            root = ET.fromstring(z.read("xl/sharedStrings.xml"))
            for si in root.findall("m:si", NS):
                shared.append("".join(t.text or "" for t in si.findall(".//m:t", NS)))

        workbook = ET.fromstring(z.read("xl/workbook.xml"))
        rels = ET.fromstring(z.read("xl/_rels/workbook.xml.rels"))
        relmap = {rel.attrib["Id"]: rel.attrib["Target"] for rel in rels}
        sheets = workbook.findall("m:sheets/m:sheet", NS)
        print("SHEETS", [(s.attrib.get("name"), s.attrib.get("{%s}id" % NS["r"])) for s in sheets])

        first = sheets[0]
        rid = first.attrib["{%s}id" % NS["r"]]
        target = relmap[rid]
        sheet_path = target if target.startswith("xl/") else "xl/" + target.lstrip("/")
        root = ET.fromstring(z.read(sheet_path))

        out = []
        for row in root.findall(".//m:sheetData/m:row", NS)[:limit]:
            vals = {}
            for c in row.findall("m:c", NS):
                idx = col_index(c.attrib.get("r","A1"))
                typ = c.attrib.get("t")
                v = c.find("m:v", NS)
                inline = c.find("m:is/m:t", NS)
                raw = ""
                if inline is not None:
                    raw = inline.text or ""
                elif v is not None:
                    raw = v.text or ""
                    if typ == "s" and raw.isdigit():
                        raw = shared[int(raw)]
                vals[idx] = raw
            if vals:
                maxidx = max(vals)
                out.append([vals.get(i,"") for i in range(maxidx+1)])
        return out

for arg in sys.argv[1:]:
    print("\nFILE", arg)
    rows = read_rows(Path(arg))
    for i,row in enumerate(rows):
        print("ROW", i, [str(x)[:240] for x in row])
