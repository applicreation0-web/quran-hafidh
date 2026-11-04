"""Faithful conversion of the eight user-supplied relation SVGs; no UI wiring.

72x48 source geometry is centered in a 72x72 viewport without stretching.
Only the explicit source shapes used by this contract are accepted.
"""
import argparse
import re
from pathlib import Path
import xml.etree.ElementTree as ET

ANDROID='http://schemas.android.com/apk/res/android'
ET.register_namespace('android',ANDROID)
A='{'+ANDROID+'}'
ROOT=Path(__file__).resolve().parents[1]
SOURCE=ROOT/'docs/correspondences/icons_contractual_8'
NAMES=['repetition','ressemblance','opposition','progression','alternance','inversion','centre','debut_fin_inclusio']

def number(value):return format(float(value),'.10g')

def color(value,current):
    value=current if value=='currentColor' else value
    if value=='none':return '#00000000'
    if re.fullmatch(r'#[0-9a-fA-F]{3}',value):return '#'+''.join(c*2 for c in value[1:])
    if re.fullmatch(r'#[0-9a-fA-F]{6}',value):return value
    raise ValueError('Unsupported contract color: '+value)

def path_data(node):
    tag=node.tag.rsplit('}',1)[-1]
    if tag=='path':return node.attrib['d']
    if tag=='rect':
        x,y=float(node.get('x','0')),float(node.get('y','0'))
        w,h=float(node.attrib['width']),float(node.attrib['height'])
        if node.get('rx') or node.get('ry'):raise ValueError('Rounded rectangle not in contract')
        return f'M{number(x)} {number(y)}H{number(x+w)}V{number(y+h)}H{number(x)}Z'
    if tag=='circle':
        x,y,r=map(float,(node.attrib['cx'],node.attrib['cy'],node.attrib['r']))
        return f'M{number(x-r)} {number(y)}A{number(r)} {number(r)} 0 1 0 {number(x+r)} {number(y)}A{number(r)} {number(r)} 0 1 0 {number(x-r)} {number(y)}Z'
    raise ValueError('Unsupported contract shape: '+tag)

def convert_svg(raw):
    svg=ET.fromstring(raw)
    if svg.get('viewBox')!='0 0 72 48':raise ValueError('Unexpected source viewport')
    vector=ET.Element('vector',{A+'width':'24dp',A+'height':'24dp',A+'viewportWidth':'72',A+'viewportHeight':'72'})
    centered=ET.SubElement(vector,'group',{A+'translateY':'12'})
    clips={}
    for node in svg.iter():
        if node.tag.rsplit('}',1)[-1]=='clipPath':
            if len(node)!=1:raise ValueError('Unsupported compound clip')
            clips[node.attrib['id']]=path_data(node[0])

    def visit(node,parent,inherited):
        tag=node.tag.rsplit('}',1)[-1]
        if tag=='defs':return
        if node.get('transform'):raise ValueError('Unexpected transform in contract')
        style=dict(inherited)
        for key in ['fill','stroke','stroke-width','stroke-linecap','stroke-linejoin','color']:
            if key in node.attrib:style[key]=node.attrib[key]
        if tag=='g':
            group=ET.SubElement(parent,'group')
            if node.get('clip-path'):
                match=re.fullmatch(r'url\(#([^)]*)\)',node.get('clip-path'))
                if not match or match[1] not in clips:raise ValueError('Unknown contract clip')
                ET.SubElement(group,'clip-path',{A+'pathData':clips[match[1]]})
            for child in node:visit(child,group,style)
            return
        attrs={A+'pathData':path_data(node),A+'fillColor':color(style['fill'],style['color'])}
        if style['stroke']!='none':
            attrs.update({A+'strokeColor':color(style['stroke'],style['color']),A+'strokeWidth':style['stroke-width'],
                          A+'strokeLineCap':style['stroke-linecap'],A+'strokeLineJoin':style['stroke-linejoin']})
        ET.SubElement(parent,'path',attrs)

    style={'fill':'#000000','stroke':'none','stroke-width':'1','stroke-linecap':'butt','stroke-linejoin':'miter','color':svg.get('color','#000000')}
    for child in svg:visit(child,centered,style)
    ET.indent(vector,space='    ')
    return '<?xml version="1.0" encoding="utf-8"?>\n'+ET.tostring(vector,encoding='unicode')+'\n'

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--check',action='store_true');args=parser.parse_args()
    for i,name in enumerate(NAMES,1):
        source=SOURCE/f'{i:02}_{name}.svg'
        destination=ROOT/'hifz-app/src/main/res/drawable'/f'ic_corr_relation_{name}.xml'
        expected=convert_svg(source.read_text())
        if args.check:
            if not destination.exists() or destination.read_text()!=expected:raise SystemExit('Nonconforming vector: '+str(destination))
        elif destination.exists() and destination.read_text()!=expected:
            raise SystemExit('Preserve existing modified vector before regeneration: '+str(destination))
        else:destination.write_text(expected)
    print('PASS: 8 contract geometries '+('match generated vectors' if args.check else 'converted; native Android rendering NOT_RUN'))

if __name__=='__main__':main()
