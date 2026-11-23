#!/usr/bin/env python3
"""Report jboss-eap-7-to-8 planning findings to the MTA Hub as an Analysis.

Converts a findings JSON (transcribed from the plan stage's migration-issue
list) into the hub's analysis manifest format - GS-delimited (\\x1D)
sections carrying the Analysis resource and one Insight per migration rule
(with one incident per affected file) - and POSTs it to
POST /applications/:id/analyses. The hub ingests it exactly like a
static-analyzer report, so the planning issues appear in the application's
Issues and Reports views.

Insights are GROUPED by rule (one insight per migration rule, one incident
per finding) - the same shape the static analyzer emits, so the Issues view
shows one issue per rule with the affected files underneath it.

Deterministic by design: the YAML encoding, the GS markers and the
request format live here, not in the agent's model-generated text.

Usage (the skill always runs it with just --findings; the hub-url/app-id/
token defaults come from the entrypoint-provisioned files, and the script
exits 0 without POSTing when the run is not provisioned or has no findings):
  eap-hub-report.py --findings /tmp/eap-findings.json
"""

import argparse
import json
import os
import sys
import urllib.request

# GS-delimited section markers, verbatim from the hub's shared/api.
BEGIN_MAIN, END_MAIN = "\x1DBEGIN-MAIN\x1D", "\x1DEND-MAIN\x1D"
BEGIN_INSIGHTS, END_INSIGHTS = "\x1DBEGIN-INSIGHTS\x1D", "\x1DEND-INSIGHTS\x1D"

RULESET = "jboss-eap-7-to-8"

REQUIRED = ("rule", "file")

# Findings whose `category` is missing default to mandatory: the skill's
# migration items are breaking changes by definition (removals, namespace
# migrations, descriptor updates). `potential` is for advisory findings
# (e.g. recommended but not required improvements).
DEFAULT_CATEGORY = "mandatory"

VALID_CATEGORIES = ("mandatory", "potential", "optional", "information")


def yaml_str(s):
    """Quote a string for single-line YAML embedding."""
    return '"' + str(s).replace("\\", "\\\\").replace('"', '\\"') + '"'


def group_by_rule(findings):
    """Group valid findings by rule, keeping first-seen order."""
    skipped = 0
    groups = {}
    for f in findings:
        if not isinstance(f, dict):
            skipped += 1
            continue
        missing = [k for k in REQUIRED if not f.get(k)]
        if missing:
            print(f"skipping malformed finding (missing {missing}): {f}", file=sys.stderr)
            skipped += 1
            continue
        groups.setdefault(str(f["rule"]), []).append(f)
    return groups, skipped


def build_manifest(findings):
    lines = []

    # MAIN: the Analysis resource.
    lines += [BEGIN_MAIN, "mode: hub-report", f"provider: {RULESET}", END_MAIN]

    # INSIGHTS: one Insight per migration rule, one incident per finding
    # (file). The hub decodes each section as a stream of YAML documents
    # (one resource per `---`-separated document, hub reads with Decode
    # until EOF) - NOT a YAML list.
    groups, _skipped = group_by_rule(findings)
    lines.append(BEGIN_INSIGHTS)
    first = True
    for rule, fs in groups.items():
        if not first:
            lines.append("---")
        first = False
        head = next((f for f in fs if f.get("name")), fs[0])
        name = head.get("name") or rule
        lines.append(f"ruleset: {RULESET}")
        lines.append(f"rule: {yaml_str(rule)}")
        lines.append(f"name: {yaml_str(name)}")
        desc = head.get("description") or (
            f"JBoss EAP 7 to 8 migration planning identified rule {rule} "
            f"in {len(fs)} file(s)."
        )
        lines.append(f"description: {yaml_str(desc)}")
        category = str(head.get("category", DEFAULT_CATEGORY)).lower()
        if category not in VALID_CATEGORIES:
            category = DEFAULT_CATEGORY
        lines.append(f"category: {category}")
        # Effort: story points so the UI's effort totals stay sane. Taken
        # from the finding if given, else 3 per rule (grouped insight).
        effort = head.get("effort", 3)
        try:
            effort = max(0, int(effort))
        except (TypeError, ValueError):
            effort = 3
        lines.append(f"effort: {effort}")
        phase = str(head.get("phase", "")).lower()
        lines.append("labels:")
        for label in ["migration", RULESET] + ([f"phase/{phase}"] if phase else []):
            lines.append(f"  - {label}")
        lines.append("incidents:")
        for f in fs:
            lines.append(f"  - file: {yaml_str(f['file'])}")
            lines.append(f"    message: {yaml_str(f.get('message') or name)}")
    lines.append(END_INSIGHTS)

    return "\n".join(lines) + "\n"


def post_analysis(hub_url, app_id, token, manifest):
    url = f"{hub_url.rstrip('/')}/applications/{app_id}/analyses"
    req = urllib.request.Request(
        url,
        data=manifest.encode("utf-8"),
        headers={
            "Authorization": f"Bearer {token}",
            "Content-Type": "application/x-yaml",
            "Accept": "application/json",
        },
        method="POST",
    )
    with urllib.request.urlopen(req, timeout=60) as resp:
        body = resp.read().decode("utf-8", "replace")
        return resp.status, body


HUB_REPORT_DIR = "/tmp/konveyor/hub-report"


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--findings", required=True, help="findings JSON path")
    ap.add_argument("--hub-url", default=None,
                    help=f"Hub base URL (default: {HUB_REPORT_DIR}/hub-url)")
    ap.add_argument("--app-id", default=None,
                    help=f"application ID (default: {HUB_REPORT_DIR}/app-id)")
    ap.add_argument("--token-file", default=None,
                    help=f"file holding the hub bearer token (default: {HUB_REPORT_DIR}/token)")
    args = ap.parse_args()

    # The existence decision lives HERE, not in the agent: the skill always
    # runs this script and the script no-ops when the run was not provisioned
    # for Hub reporting (standalone mode / execute-verify stages on the stock
    # image) or when there is nothing to report.
    token_file = args.token_file or os.path.join(HUB_REPORT_DIR, "token")
    if not os.path.isfile(token_file):
        print("hub-report: no token file - not provisioned for Hub reporting, "
              "skipping (exit 0)")
        return
    with open(args.findings) as fh:
        findings = json.load(fh)
    if isinstance(findings, dict):
        findings = findings.get("findings", [])
    if not findings:
        print("hub-report: no planning findings - nothing to report (exit 0)")
        return
    with open(token_file) as fh:
        token = fh.read().strip()
    hub_url = args.hub_url
    app_id = args.app_id
    if not hub_url:
        with open(os.path.join(HUB_REPORT_DIR, "hub-url")) as fh:
            hub_url = fh.read().strip()
    if not app_id:
        with open(os.path.join(HUB_REPORT_DIR, "app-id")) as fh:
            app_id = fh.read().strip()

    manifest = build_manifest(findings)
    n_insights = manifest.count("ruleset: " + RULESET)
    n_incidents = manifest.count("  - file:")
    print(f"reporting {n_insights} insights ({n_incidents} incidents) "
          f"to {hub_url}/applications/{app_id}/analyses")

    status, body = post_analysis(hub_url, app_id, token, manifest)
    if status not in (200, 201):
        print(f"hub rejected the analysis: HTTP {status}: {body[:300]}", file=sys.stderr)
        sys.exit(1)
    try:
        analysis_id = json.loads(body).get("id")
    except Exception:
        analysis_id = "?"
    print(json.dumps({"status": status, "analysis_id": analysis_id,
                      "insights": n_insights, "incidents": n_incidents}))


if __name__ == "__main__":
    main()
