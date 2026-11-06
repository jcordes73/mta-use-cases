#!/usr/bin/env python3
"""Report RHDA CVE scan findings to the MTA Hub as an Analysis.

Converts a findings JSON (transcribed from the rhda MCP tool output)
into the hub's analysis manifest format - GS-delimited (\\x1D) sections
carrying the Analysis resource, one Insight per CVE and TechDependency
records - and POSTs it to POST /applications/:id/analyses. The hub
ingests it exactly like a static-analyzer report, so the CVEs appear in
the application's Issues and Reports views.

Deterministic by design: the YAML encoding, the GS markers and the
request format live here, not in the agent's model-generated text.

Usage (the skill always runs it with just --findings; the hub-url/app-id/
token defaults come from the entrypoint-provisioned files, and the script
exits 0 without POSTing when the run is not provisioned or has no findings):
  rhda-hub-report.py --findings findings.json
"""

import argparse
import json
import os
import sys
import urllib.request

# GS-delimited section markers, verbatim from the hub's shared/api.
BEGIN_MAIN, END_MAIN = "\x1DBEGIN-MAIN\x1D", "\x1DEND-MAIN\x1D"
BEGIN_INSIGHTS, END_INSIGHTS = "\x1DBEGIN-INSIGHTS\x1D", "\x1DEND-INSIGHTS\x1D"
BEGIN_DEPS, END_DEPS = "\x1DBEGIN-DEPS\x1D", "\x1DEND-DEPS\x1D"

# Severity -> story-point effort, so the UI's effort totals stay sane.
EFFORT = {"critical": 8, "high": 5, "medium": 3, "low": 1}

REQUIRED = ("cve", "manifest")


def yaml_str(s):
    """Quote a string for single-line YAML embedding."""
    return '"' + str(s).replace("\\", "\\\\").replace('"', '\\"') + '"'


def build_manifest(findings, app_path):
    lines = []

    # MAIN: the Analysis resource.
    lines += [BEGIN_MAIN, "mode: hub-report", "provider: rhda", END_MAIN]

    # INSIGHTS: one Insight per CVE finding, one incident per manifest.
    # The hub decodes each section as a stream of YAML documents (one
    # resource per `---`-separated document, hub reads with Decode until
    # EOF) - NOT a YAML list.
    lines.append(BEGIN_INSIGHTS)
    first = True
    for f in findings:
        missing = [k for k in REQUIRED if not f.get(k)]
        if missing:
            print(f"skipping malformed finding (missing {missing}): {f}", file=sys.stderr)
            continue
        if not first:
            lines.append("---")
        first = False
        cve = f["cve"]
        severity = str(f.get("severity", "medium")).lower()
        effort = EFFORT.get(severity, 3)
        gav = f.get("gav", "")
        name = f.get("name") or f"CVE {cve} in {gav}" if gav else f"CVE {cve}"
        lines.append("ruleset: rhda")
        lines.append(f"rule: {yaml_str('cve-' + cve)}")
        lines.append(f"name: {yaml_str(name)}")
        desc = f.get("description") or (
            f"Red Hat Dependency Analytics identified {cve} ({severity}) "
            f"in dependency {gav}."
        )
        lines.append(f"description: {yaml_str(desc)}")
        lines.append("category: potential")
        lines.append(f"effort: {effort}")
        lines.append("labels:")
        for label in ["rhda", "cve", f"severity/{severity}"]:
            lines.append(f"  - {label}")
        if f.get("link"):
            lines.append("links:")
            lines.append(f"  - url: {yaml_str(f['link'])}")
            lines.append(f"    title: {yaml_str(cve + ' advisory')}")
        lines.append("incidents:")
        lines.append(f"  - file: {yaml_str(f['manifest'])}")
        msg = f"{gav + ': ' if gav else ''}{cve} ({severity})"
        if f.get("fixed_in"):
            msg += f" - fixed in {f['fixed_in']}"
        lines.append(f"    message: {yaml_str(msg)}")
    lines.append(END_INSIGHTS)

    # DEPS: one TechDependency per unique GAV across findings.
    deps = {}
    for f in findings:
        gav = f.get("gav", "")
        if gav and ":" in gav and gav not in deps:
            deps[gav] = f.get("version", "")
    if deps:
        lines.append(BEGIN_DEPS)
        for i, (gav, version) in enumerate(sorted(deps.items())):
            if i:
                lines.append("---")
            lines.append("provider: maven")
            lines.append(f"name: {yaml_str(gav)}")
            if version:
                lines.append(f"version: {yaml_str(version)}")
            lines.append("indirect: true")
            lines.append("labels:")
            lines.append("  - rhda")
        lines.append(END_DEPS)

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
    # for Hub reporting (standalone mode) or when there is nothing to report.
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
        print("hub-report: no CVE findings - nothing to report (exit 0)")
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

    manifest = build_manifest(findings, None)
    n_insights = manifest.count("ruleset: rhda")
    n_deps = manifest.count("provider: maven")
    print(f"reporting {n_insights} insights, {n_deps} dependencies "
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
                      "insights": n_insights, "dependencies": n_deps}))


if __name__ == "__main__":
    main()
