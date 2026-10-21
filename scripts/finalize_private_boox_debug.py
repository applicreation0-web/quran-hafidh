#!/usr/bin/env python3
"""Finalize a CI-produced private BOOX *debug* completely offline.

No Android code or editorial flags are modified. The script fails closed on an
unexpected CI commit, private pack SHA, source files, APK identity or signer.
Never upload the private pack, password file or PKCS#12 to public CI.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import struct
import subprocess
import sys
import tempfile
import zipfile

PINNED_SIGNER_SHA256 = '85cd816f5213ae81f450146789c13ec476df54d7be8cc5f806decc9ed3f7061e'
CURRENT_PRIVATE_PACK_SHA256 = 'ec21168411f95d66ef336a5091050c86e482ccb62a51cb44436a9f6f6d5bf870'
EXPECTED_PACKAGE = 'com.quransafeguard.hifz.ayahthemes114private'
ASSET_FILES = {
    'assets/atlas/atlas_display_catalog_v1.json',
    'assets/atlas_private/atlas_366_comments_offline_reference_v1.json',
    'assets/atlas_private/atlas_approved_unit_titles_366_v1.json',
    'assets/atlas_private/maududi_original.pdf',
    'assets/ayah_themes_private/macros_114.json',
    'assets/ayah_themes_private/special_27.json',
    'assets/ayah_themes_private/titles_114.json',
    'assets/correspondences/approved_v1.json',
    'assets/correspondences/historical_110_v1.json',
}

def sha(b):
    return hashlib.sha256(b).hexdigest()

def require(condition, reason):
    if not condition:
        raise ValueError(reason)

def run(args, env=None):
    process = subprocess.run(args, env=env, capture_output=True, text=True)
    if process.returncode:
        raise RuntimeError(f'External validation failed: {Path(args[0]).name}; exit {process.returncode}; details: {process.stderr[:500]}')
    return process.stdout

def manifest_identity(data):
    """Read only the binary AndroidManifest.xml package/version. No aapt needed."""
    def u16(o):return struct.unpack_from('<H', data, o)[0]
    def u32(o):return struct.unpack_from('<I', data, o)[0]
    def len8(p):
        n=data[p];p+=1
        if n & 128:n=((n&127)<<8)|data[p];p+=1
        return n,p
    def len16(p):
        n=u16(p);p+=2
        if n & 32768:n=((n&32767)<<16)|u16(p);p+=2
        return n,p
    off=8; strings=[]
    while off+8<=len(data):
        typ,hsize,size=struct.unpack_from('<HHI',data,off)
        require(size>=hsize and size>=8,'Malformed Android binary XML chunk')
        if typ==1:
            count=u32(off+8);flags=u32(off+16);start=u32(off+20)
            require(count<200000,'Oversized XML string pool')
            for i in range(count):
                p=off+start+u32(off+hsize+i*4)
                if flags & 256:
                    _,p=len8(p);n,p=len8(p);strings.append(data[p:p+n].decode('utf8'))
                else:
                    n,p=len16(p);strings.append(data[p:p+n*2].decode('utf-16-le'))
        elif typ==0x102:
            _,name=struct.unpack_from('<II',data,off+16)
            if name<len(strings) and strings[name]=='manifest':
                start,delta,count=struct.unpack_from('<HHH',data,off+24)
                attrs={}
                for i in range(count):
                    p=off+16+start+delta*i
                    _,key,raw=struct.unpack_from('<III',data,p)
                    kind=data[p+15];value=u32(p+16)
                    if key>=len(strings):continue
                    attrs[strings[key]]=strings[raw] if raw<len(strings) else strings[value] if kind==3 and value<len(strings) else value
                return attrs
        off+=size
    raise ValueError('Android manifest root not found')

def compare_trusted_code(staged, signed):
    with zipfile.ZipFile(staged) as a,zipfile.ZipFile(signed) as b:
        for obj in a.infolist():
            name=obj.filename
            if name.startswith('META-INF/') and (name.endswith(('.RSA','.DSA','.EC','.SF')) or name=='META-INF/MANIFEST.MF'):
                continue
            require(name in b.namelist(), f'Compiled component disappeared: {name}')
            require(sha(a.read(name))==sha(b.read(name)), f'Compiled component changed after private overlay: {name}')

def assemble(staging, private_pack, out, expected_sha, old_apk, tmp):
    require(re.fullmatch(r'[a-f0-9]{40}',expected_sha) is not None,'Expected full 40-digit Git commit hash')
    require(sha(private_pack.read_bytes())==CURRENT_PRIVATE_PACK_SHA256,'Unexpected private pack fingerprint: manual audit/version bump required')
    with zipfile.ZipFile(staging) as z:
        require(z.testzip() is None,'Corrupted CI staging ZIP')
        provenance=z.read('README.txt').decode('utf8')
        require(f'Source: {expected_sha}' in provenance,'CI build does not match expected code SHA')
        raw_apk=z.read('UNFINALIZED-PRIVATE-ASSETS-MISSING-DO-NOT-INSTALL.apk')
        jar=z.read('apksigner.jar')
        align=z.read('zipalign')
        lib=z.read('libc++.so')
    code_apk=tmp/'ci-staging.apk';code_apk.write_bytes(raw_apk)
    signerjar=tmp/'apksigner.jar';signerjar.write_bytes(jar)
    zipalign=tmp/'zipalign';zipalign.write_bytes(align);zipalign.chmod(0o700)
    (tmp/'libc++.so').write_bytes(lib)
    with zipfile.ZipFile(private_pack) as z:
        require(z.testzip() is None,'Corrupted private pack')
        manifest=json.loads(z.read('PRIVATE_ASSET_MANIFEST.json'))
        require(manifest.get('schema')=='HAAFIDH_PRIVATE_BOOX_ASSETS_V1','Unsupported private pack')
        fingerprints=manifest['assets']
        require(set(fingerprints)==ASSET_FILES,'Nine private asset names must match exact list')
        private={name:z.read(name) for name in sorted(ASSET_FILES)}
        for name, data in private.items():
            require(sha(data)==fingerprints[name],f'Private asset integrity failure: {name}')
    require(sha(private['assets/atlas/atlas_display_catalog_v1.json'])=='065ec0dd68c488efacbd9e77635bf9d531b4cbb2b1bc82db7853ad4da0c02c9c','Atlas editorial asset pin mismatch')
    require(sha(private['assets/correspondences/approved_v1.json'])=='6fcbe644df3b990760d6b8e886ac60bc66d187dcff09dc0e411cf5a4a47c7ce0','Approved relations changed')
    require(sha(private['assets/correspondences/historical_110_v1.json'])=='b46c0e7d7800cbb1cfe34c72a39328339608d9b4e0ede5397dcd780de32aa8a2','Historical dossier changed')
    atlas=json.loads(private['assets/atlas/atlas_display_catalog_v1.json'])
    historical=json.loads(private['assets/correspondences/historical_110_v1.json'])
    approved=json.loads(private['assets/correspondences/approved_v1.json'])
    require(len(atlas['entries'])==1419 and sum(x['level']=='UNIT' for x in atlas['entries'])==366 and sum(x['level']=='PASSAGE' for x in atlas['entries'])==1053,'Atlas completeness')
    require(all(x['user_approved'] is True for x in atlas['entries']),'Editorial approval is missing')
    require(len(historical['entries'])==110 and len(set(x['id'] for x in historical['entries']))==110,'Historical relation catalogue incomplete')
    require(len(approved['entries'])==4,'Approved relation catalogue must remain the 4 authorized subset')
    pre=tmp/'with-assets-unsigned.apk'
    with zipfile.ZipFile(code_apk) as inp,zipfile.ZipFile(pre,'w',allowZip64=True) as dest:
        require(inp.testzip() is None,'Corrupted CI APK')
        require(not ASSET_FILES.intersection(inp.namelist()),'CI APK unexpectedly includes private data')
        for info in inp.infolist():
            if info.filename.startswith('META-INF/') and (info.filename.endswith(('.RSA','.DSA','.EC','.SF')) or info.filename=='META-INF/MANIFEST.MF'):continue
            dest.writestr(info,inp.read(info.filename))
        for name, data in private.items():dest.writestr(name,data,compress_type=zipfile.ZIP_DEFLATED,compresslevel=7)
    with zipfile.ZipFile(code_apk) as z:
        identity=manifest_identity(z.read('AndroidManifest.xml'))
        require(identity.get('package')==EXPECTED_PACKAGE,'Wrong Android package in CI staging')
        require('PRIVATE' in identity.get('versionName',''),'Wrong preview variant')
        require(identity.get('versionCode',0)>0,'Wrong debug version code')
    if old_apk:
        with zipfile.ZipFile(old_apk) as z:
            old=manifest_identity(z.read('AndroidManifest.xml'))
        require(old.get('package')==identity['package'],'Different package from previous user debug')
        require(old.get('versionCode',100000)<=identity['versionCode'],'Downgrade would invalidate safe in-place update')
    env=os.environ.copy();env['LD_LIBRARY_PATH']=str(tmp)
    aligned=tmp/'aligned-unsigned.apk'
    run([str(zipalign),'-P','16','-f','4',str(pre),str(aligned)],env)
    run([str(zipalign),'-c','-P','16','4',str(aligned)],env)
    return code_apk,signerjar,zipalign,aligned,env,identity,private

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--staging-zip',type=Path,required=True)
    parser.add_argument('--private-pack',type=Path,required=True)
    parser.add_argument('--keystore',type=Path,required=True)
    parser.add_argument('--password-file',type=Path,required=True)
    parser.add_argument('--expected-code-sha',required=True)
    parser.add_argument('--previous-apk',type=Path,help='Signed private BOOX APK known to be installed or compatible')
    parser.add_argument('--output',type=Path,required=True)
    args=parser.parse_args()
    require(args.output.resolve() not in {args.staging_zip.resolve(),args.private_pack.resolve(),args.keystore.resolve()},'Refusing to overwrite inputs')
    require(not args.output.exists(),'Refusing to overwrite existing output')
    with tempfile.TemporaryDirectory(prefix='haafidh-private-') as dirname:
        tmp=Path(dirname)
        code,jar,align,aligned,env,identity,private=assemble(args.staging_zip,args.private_pack,args.output,args.expected_code_sha,args.previous_apk,tmp)
        password=args.password_file.read_text(encoding='utf8').strip()
        require(password and '\n' not in password,'Password file must contain exactly one line')
        env['HAAFIDH_SIGN_PW']=password
        keytool=run(['keytool','-list','-v','-storetype','PKCS12','-keystore',str(args.keystore),'-storepass:env','HAAFIDH_SIGN_PW'],env)
        pairs=re.findall(r'SHA256:\s*((?:[A-F0-9]{2}:){31}[A-F0-9]{2})',keytool,re.I)
        require(PINNED_SIGNER_SHA256 in [p.replace(':','').lower() for p in pairs],'Incorrect signing key: would cause update incompatibility')
        args.output.parent.mkdir(parents=True,exist_ok=True)
        run(['java','-jar',str(jar),'sign','--ks',str(args.keystore),'--ks-type','PKCS12','--ks-key-alias','haafidh-114-private','--ks-pass','env:HAAFIDH_SIGN_PW','--key-pass','env:HAAFIDH_SIGN_PW','--v2-signing-enabled','true','--v3-signing-enabled','true','--out',str(args.output),str(aligned)],env)
        signer=run(['java','-jar',str(jar),'verify','--verbose','--print-certs',str(args.output)],env)
        require(PINNED_SIGNER_SHA256 in signer.lower(),'Signed APK certificate does not match user debug')
        require('Verified using v2 scheme (APK Signature Scheme v2): true' in signer and 'Verified using v3 scheme (APK Signature Scheme v3): true' in signer,'Invalid Android APK signature')
        run([str(align),'-c','-P','16','4',str(args.output)],env)
        compare_trusted_code(code,args.output)
        with zipfile.ZipFile(args.output) as z:
            require(z.testzip() is None,'Signed APK CRC error')
            require(all(sha(z.read(n))==sha(b) for n,b in private.items()),'Post-signing private asset damage')
            pages=[n for n in z.namelist() if re.fullmatch(r'assets/mushaf/hafs/kfqc/svg-br/\d{3}\.svg\.br',n)]
            require(len(pages)==604,'Missing genuine 604-page Quran geometry')
    summary={'result':'PASS (binary and offline integrity only; no physical BOOX testing)',
      'apk_sha256':sha(args.output.read_bytes()),'apk_bytes':args.output.stat().st_size,
      'code_sha':args.expected_code_sha,'assets':9,'atlas_titles':1419,'historical_relations':110,
      'approved_relations':4,'qcf_pages':604,'android_package':identity['package'],
      'version_code':identity['versionCode'],'signer_cert_sha256':PINNED_SIGNER_SHA256,
      'physical_installation':'NOT TESTED: back up existing app before install; never uninstall current app'}
    report=args.output.with_suffix('.verification.json')
    report.write_text(json.dumps(summary,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
    print('PASS: private BOOX debug is assembled, signed, 16KiB-aligned, source-preserving and integrity verified.')
    print('APK:',args.output,'SHA256',summary['apk_sha256'])
    print('Verification report:',report)

if __name__=='__main__':
    try:main()
    except Exception as error:
        print('NO-GO:',type(error).__name__,str(error),file=sys.stderr)
        raise SystemExit(2)