"""Reject packaging/install tasks in a Gradle dry-run before any task executes."""
import re,sys

def forbidden_tasks(text):
 tasks=re.findall(r'(?:^|\s)(?::[A-Za-z0-9_.-]+)*:([A-Za-z0-9_.-]+)(?=\s|$)',text)
 rejected=[]
 for task in tasks:
  lower=task.lower()
  forbidden=(lower.startswith(('assemble','install','connected','sign','zipalign'))
    or (lower.startswith('package') and not task.endswith('Resources'))
    or (lower.startswith('bundle') and not task.endswith('Jar')))
  if forbidden and task not in rejected:rejected.append(task)
 return rejected

if __name__=='__main__':
 blocked=forbidden_tasks(open(sys.argv[1],encoding='utf-8').read())
 if blocked:
  print('NON_EXECUTE: task graph would create/install a package: '+', '.join(blocked));sys.exit(64)
 print('PASS: task graph contains no APK assembly, packaging, signing or install task')
