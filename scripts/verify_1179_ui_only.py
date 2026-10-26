#!/usr/bin/env python3
"""Fail closed if 1.17.9 changes any established runtime file outside its two UI surfaces."""
import subprocess,pathlib,hashlib,json
root=pathlib.Path(__file__).resolve().parents[1]
base='ae68ee1a46e195e9da1068aed981f3363869a4e0'
allowed={'hifz-app/src/main/java/com/quransafeguard/hifz/preview/'+n for n in ['MainActivity.java','WeeklyDashboardPlanner.java']}
protected=[]
for name in subprocess.check_output(['git','ls-tree','-r','--name-only',base],cwd=root,text=True).splitlines():
    if name.startswith(('hifz-app/src/main/','hifz-core/src/main/','safeguard-core/src/main/')) and name not in allowed:
        old=subprocess.check_output(['git','show',base+':'+name],cwd=root)
        new=(root/name).read_bytes()
        assert old==new, 'FROZEN RUNTIME MODIFIED: '+name
        protected.append(name)
print('PASS: '+str(len(protected))+' established runtime files byte-identical to 1.17.8 source, including motors, stores, assets, mask, Atlas, icons and manifest')
