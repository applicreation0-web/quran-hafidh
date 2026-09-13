from pathlib import Path

p = Path('.github/workflows/hifz-test-app.yml')
text = p.read_text(encoding='utf-8')

old = '''      - work/hifz-0.7.3-spec-update\n  workflow_dispatch:\n'''
new = '''      - work/hifz-0.7.3-spec-update\n      - work/hifz-0.7.3-finalize-all\n      - work/hifz-0.7.3-claude-all-fixes\n  workflow_dispatch:\n'''
if text.count(old) != 1:
    raise SystemExit('final work branch insertion point mismatch')
text = text.replace(old, new, 1)

old = '''          adb wait-for-device\n          booted=0\n'''
new = '''          if ! timeout 90s adb wait-for-device; then\n            echo 'Android emulator did not register with adb within 90 seconds' >&2\n            adb devices -l >&2 || true\n            cat emulator.log >&2 || true\n            exit 1\n          fi\n          booted=0\n'''
if text.count(old) != 1:
    raise SystemExit('adb wait insertion point mismatch')
text = text.replace(old, new, 1)

p.write_text(text, encoding='utf-8')
print('OFFICIAL_HIFZ_WORKFLOW_PATCH_OK')
