import unittest
from check_no_apk_task_graph import forbidden_tasks
class NoApkGraphTest(unittest.TestCase):
 def test_real_compile_intermediates_are_allowed(self):
  self.assertEqual([],forbidden_tasks(':hifz-app:packageDebugResources SKIPPED\n:hifz-app:bundleDebugClassesToCompileJar SKIPPED\n:hifz-app:compileDebugAndroidTestJavaWithJavac SKIPPED'))
 def test_apk_package_and_assembly_are_rejected_before_execution(self):
  for task in ['assembleDebug','assembleDebugAndroidTest','packageDebug','packagePrivate114Debug','packageDebugAndroidTest','installDebug','connectedDebugAndroidTest','signReleaseBundle','bundleRelease']:
   self.assertEqual([task],forbidden_tasks(':hifz-app:'+task+' SKIPPED'),task)
 def test_recorded_logs_use_same_detection(self):
  self.assertEqual(['packageDebug'],forbidden_tasks('> Task :hifz-app:packageDebug\n> Task :hifz-app:packageDebugResources'))
if __name__=='__main__':unittest.main()
