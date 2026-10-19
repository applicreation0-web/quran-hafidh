"""The default audit path must stop before any APK build or install command."""
from pathlib import Path
import os,subprocess,tempfile,unittest
ROOT=Path(__file__).resolve().parent.parent
class NoApkDefault(unittest.TestCase):
 def test_default_denies_before_gradle_or_adb(self):
  with tempfile.TemporaryDirectory()as directory:
   d=Path(directory);marker=d/'called';tool=d/'gradle'
   tool.write_text('#!/bin/sh\ntouch "'+str(marker)+'"\nexit 99\n');tool.chmod(0o755)
   env=dict(os.environ);env.pop('ALLOW_EMULATOR_APK',None);env['PATH']=directory+':'+env['PATH']
   r=subprocess.run(['bash',str(ROOT/'scripts/ci_android_runtime_visual.sh')],cwd=ROOT,env=env,capture_output=True,text=True)
   self.assertEqual(64,r.returncode);self.assertIn('NON_EXECUTE',r.stderr);self.assertFalse(marker.exists())
 def test_push_cannot_opt_in_to_emulator_job(self):
  workflow=(ROOT/'.github/workflows/diagnose-quiz-launch-android.yml').read_text()
  self.assertIn("if: ${{ github.event_name == 'workflow_dispatch' && inputs.allow_emulator_apk }}",workflow)
  self.assertIn('default: false',workflow)
  self.assertIn("ALLOW_EMULATOR_APK: ${{ inputs.allow_emulator_apk && '1' || '0' }}",workflow)
 def test_private_flag_staging_only_compiles_without_apk_or_signing_export(self):
  workflow=(ROOT/'.github/workflows/haafidh-private114-offline-finalize.yml').read_text()
  self.assertIn(':hifz-app:compileDebugJavaWithJavac',workflow)
  self.assertNotIn(':hifz-app:assembleDebug',workflow)
  self.assertNotIn('actions/upload-artifact',workflow)
  self.assertNotIn('apksigner.jar',workflow)
  self.assertNotIn('CODE-ONLY-NOT-INSTALLABLE.apk',workflow)
if __name__=='__main__':unittest.main()
