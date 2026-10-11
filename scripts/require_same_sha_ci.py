#!/usr/bin/env python3
"""HARD gate for the single Quran Haafidh debug after independent critical audit.

The GitHub Actions APK job is deliberately unable to publish merely because it
built successfully: it MUST wait for (1) exact official 1.17.1 Hifz/Quiz tests
and (2) the on-emulator Quiz/23 icons/eight Hifz modes/Ibn Kathir map/weekly UI,
plus PNG screenshot artifact, ALL on the SAME commit SHA.
"""
import json
import os
import subprocess
import sys
import time
import urllib.error
import urllib.parse
import urllib.request

repo = os.environ["GITHUB_REPOSITORY"]
sha = os.environ["GITHUB_SHA"]
token = os.environ["GITHUB_TOKEN"]
api = os.environ.get("GITHUB_API_URL", "https://api.github.com")
actual = subprocess.check_output(["git", "rev-parse", "HEAD"], text=True).strip()
assert len(sha) == 40 and actual == sha, "Not building exact audited commit"
names = (
    "Ibn Kathir Hifz + Quiz audit ONLY - no APK",
    "Quiz launch Android runtime regression ONLY - no APK",
)


def get(path):
    req = urllib.request.Request(
        api + path,
        headers={
            "Authorization": "Bearer " + token,
            "Accept": "application/vnd.github+json",
            "X-GitHub-Api-Version": "2022-11-28",
            "User-Agent": "HaafidhCriticalAuditExactShaGate",
        },
    )
    with urllib.request.urlopen(req, timeout=25) as response:
        return json.load(response)


deadline = time.monotonic() + 25 * 60
last_message = None
while time.monotonic() < deadline:
    try:
        data = get("/repos/" + repo + "/actions/runs?head_sha="
                   + urllib.parse.quote(sha) + "&per_page=100")
        runs = data.get("workflow_runs", [])
        selected = {}
        for name in names:
            matching = [r for r in runs
                        if r.get("name") == name and r.get("head_sha") == sha
                        and r.get("event") in ("push", "workflow_dispatch")]
            if matching:
                selected[name] = max(matching,
                    key=lambda r: (r.get("created_at") or "", r.get("id") or 0))
        description = ", ".join(
            n + ": " + (selected[n].get("conclusion") or
                         selected[n].get("status") or "pending")
            if n in selected else n + ": not started" for n in names
        )
        if description != last_message:
            print("Waiting for SAME-SHA critical gates", sha, description, flush=True)
            last_message = description
        for n in names:
            r = selected.get(n)
            if r and r.get("status") == "completed" and r.get("conclusion") != "success":
                raise SystemExit("FAIL-CLOSED: " + n + " => " + str(r.get("conclusion"))
                                 + ". No APK may be published.")
        if len(selected) == 2 and all(selected[n].get("conclusion") == "success" for n in names):
            runtime = selected[names[1]]
            art = get("/repos/" + repo + "/actions/runs/" + str(runtime["id"])
                      + "/artifacts?per_page=100")
            proofs = [a for a in art.get("artifacts", [])
                      if a.get("name", "").startswith("quiz-launch-android-runtime-")
                      and not a.get("expired")
                      and a.get("size_in_bytes", 0) >= 50000]
            if not proofs:
                raise SystemExit("FAIL-CLOSED: Android tests passed but actual PNG "
                                 "screenshots are absent/suspiciously small.")
            print("PASS: official 1.17.1 source freeze and all JVM+BOOX contracts (run "
                  + str(selected[names[0]]["id"]) + ")", flush=True)
            print("PASS: five+ true Android emulator UI tests + screenshot archive (run "
                  + str(runtime["id"]) + ", artifact "
                  + str(proofs[0]["id"]) + ", "
                  + str(proofs[0]["size_in_bytes"]) + " bytes)", flush=True)
            print("PASS: source SHA = Android runtime SHA = APK candidate SHA = " + sha,
                  flush=True)
            sys.exit(0)
    except (urllib.error.URLError, TimeoutError, json.JSONDecodeError) as error:
        print("Temporary GitHub status query failed; waiting: " + str(error),
              flush=True)
    time.sleep(20)
raise SystemExit("FAIL-CLOSED: exact same-commit source + Android critical audits did "
                 "not finish in 25 minutes. No APK published.")
