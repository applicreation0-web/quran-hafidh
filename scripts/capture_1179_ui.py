#!/usr/bin/env python3
"""Native exact-build screenshots, logical sizes recorded by adb, no rendered mockups."""
import subprocess,time,pathlib,xml.etree.ElementTree as ET,re,json
out=pathlib.Path('hifz-app/build/boox-audit');out.mkdir(parents=True,exist_ok=True)
pkg='com.quransafeguard.hifz.ibnkathirtest'
def adb(*args):return subprocess.check_output(['adb',*args])
def shot(name):
    (out/(name+'.png')).write_bytes(adb('exec-out','screencap','-p'))
    adb('shell','uiautomator','dump','/sdcard/ui1179.xml')
    (out/(name+'.xml')).write_bytes(adb('exec-out','cat','/sdcard/ui1179.xml'))
def tap(text,desc=False):
    adb('shell','uiautomator','dump','/sdcard/ui1179.xml')
    root=ET.fromstring(adb('exec-out','cat','/sdcard/ui1179.xml'))
    key='content-desc' if desc else 'text'
    for node in root.iter('node'):
        if node.get(key)==text:
            x1,y1,x2,y2=map(int,re.findall(r'\d+',node.get('bounds')))
            if y2<=y1:continue
            adb('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2));time.sleep(1);return
    raise RuntimeError('Visible target missing: '+text)
for name,size,density in [('compact','1080x1920','480'),('tablet','1404x1872','240')]:
    adb('shell','wm','size',size);adb('shell','wm','density',density)
    adb('shell','am','force-stop',pkg)
    adb('shell','am','start','-n',pkg+'/com.quransafeguard.hifz.preview.MainActivity');time.sleep(5)
    (out/(name+'-metrics.txt')).write_bytes(adb('shell','wm','size')+adb('shell','wm','density'))
    shot(name+'-accueil');tap('Parcours Hifz',True);shot(name+'-parcours')
    # Scroll to week controls on compact screens; portrait tablet should show them directly.
    if name=='compact':adb('shell','input','swipe','500','1500','500','600','250');time.sleep(1)
    shot(name+'-calendrier');tap('›');shot(name+'-semaine-suivante');tap('Aujourd’hui')
    tap('Mois');shot(name+'-mois');adb('shell','input','keyevent','4')
    tap('Programme hebdomadaire');shot(name+'-configuration');adb('shell','input','keyevent','4')
    adb('shell','input','keyevent','4');shot(name+'-retour-accueil')
adb('shell','wm','size','reset');adb('shell','wm','density','reset')
print('PASS: native compact/tablet navigation screenshots captured; physical e-ink remains untested')
