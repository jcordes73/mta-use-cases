#!/usr/bin/env python3
"""Map application-to-application dependencies and record them in the MTA Hub.

Deterministic pipeline (the model orchestrates and reports, this script
computes and writes):

 1. Enumerate the applications in the Hub (GET /applications).
 2. Resolve each application to its source directory in the workspace via
    the Hub record's repository.path, and read its Maven coordinates
    (groupId:artifactId) from <path>/pom.xml.
 3. For every application, parse the <dependencies> of its pom.xml and match
    groupId:artifactId against the registry of OTHER applications - a match
    is an inter-application dependency (from = the depending application,
    to = the depended-on application). EAR aggregators fall out naturally:
    an ear module's pom declares sibling war/jar artifacts as dependencies.
 4. Reconcile with the Hub's application-dependency graph
    (POST /dependencies {"from": {"id": A}, "to": {"id": B}}; existing edges
    are listed with GET /dependencies?from.id=A and left alone). Existing
    edges that the map no longer justifies are only REPORTED - they are
    deleted only with --prune, and only when the hub records this same user
    as the creator (never touching edges from other tools or users).

The script never guesses: applications without a resolvable local pom (not
in this workspace, not Maven) are skipped and listed as such; a dependency
whose groupId:artifactId is not another Hub application is simply not an
inter-application edge.

Usage:
  app-dependency-map.py --workspace /workspace/repo [--apply] [--prune]
    [--hub-url URL] [--token-file FILE] [--output OUT.json]

Without --apply the script only reports what it would do (dry run). The
hub-url/token-file default to the entrypoint-provisioned files under
/tmp/konveyor/hub-report/.
"""

import argparse
import json
import os
import sys
import urllib.error
import urllib.request
import xml.etree.ElementTree as ET

HUB_REPORT_DIR = "/tmp/konveyor/hub-report"
M2_NS = "{http://maven.apache.org/POM/4.0.0}"


def hub_request(hub_url, token, path, method="GET", data=None):
    req = urllib.request.Request(
        f"{hub_url.rstrip('/')}{path}",
        data=data.encode() if data else None,
        headers={
            "Authorization": f"Bearer {token}",
            "Content-Type": "application/json",
            "Accept": "application/json",
        },
        method=method,
    )
    try:
        with urllib.request.urlopen(req, timeout=60) as resp:
            body = resp.read().decode()
            return resp.status, json.loads(body) if body else None
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8", "replace")[:300]


def pom_coordinates(pom_path):
    """Return (groupId, artifactId) of the pom itself, or None."""
    try:
        root = ET.parse(pom_path).getroot()
    except (ET.ParseError, OSError):
        return None
    coords = {}
    for tag in ("groupId", "artifactId"):
        el = root.find(f"{tag}") or root.find(f"{M2_NS}{tag}")
        coords[tag] = (el.text or "").strip() if el is not None else None
    if coords["artifactId"] is None:
        return None
    # groupId may come from the parent pom.
    gid = coords["groupId"]
    if not gid:
        parent = root.find("parent") or root.find(f"{M2_NS}parent")
        if parent is not None:
            pel = parent.find("groupId") or parent.find(f"{M2_NS}groupId")
            gid = (pel.text or "").strip() if pel is not None else None
    if not gid:
        return None
    return gid, coords["artifactId"]


def pom_dependencies(pom_path):
    """Yield (groupId, artifactId) of every <dependency> in the pom."""
    try:
        root = ET.parse(pom_path).getroot()
    except (ET.ParseError, OSError):
        return
    for dep in root.iter():
        if dep.tag not in ("dependency", f"{M2_NS}dependency"):
            continue
        d = {}
        for child in dep:
            tag = child.tag.replace(M2_NS, "")
            if tag in ("groupId", "artifactId") and child.text:
                d[tag] = child.text.strip()
        if d.get("groupId") and d.get("artifactId"):
            yield d["groupId"], d["artifactId"]


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--workspace", default="/workspace/repo")
    ap.add_argument("--hub-url", default=None)
    ap.add_argument("--token-file", default=None)
    ap.add_argument("--output", default=None, help="write the full map JSON here")
    ap.add_argument("--apply", action="store_true",
                    help="create missing dependency edges in the hub")
    ap.add_argument("--prune", action="store_true",
                    help="with --apply: also delete stale edges created by "
                         "this same user (never edges from other users)")
    args = ap.parse_args()

    token_file = args.token_file or os.path.join(HUB_REPORT_DIR, "token")
    if not os.path.isfile(token_file):
        print(f"app-dependency-map: no token file at {token_file} - "
              f"not provisioned for Hub access, cannot run (exit 1)")
        sys.exit(1)
    with open(token_file) as fh:
        token = fh.read().strip()
    hub_url = args.hub_url
    if not hub_url:
        with open(os.path.join(HUB_REPORT_DIR, "hub-url")) as fh:
            hub_url = fh.read().strip()

    # 1. Enumerate the Hub's applications.
    status, apps = hub_request(hub_url, token, "/applications")
    if status != 200:
        print(f"hub GET /applications failed: HTTP {status}: {apps}", file=sys.stderr)
        sys.exit(1)
    print(f"hub: {len(apps)} applications")

    # 2. Resolve each application to local Maven coordinates.
    registry = {}   # "gid:aid" -> app
    skipped = []    # apps without a resolvable local pom
    for app in apps:
        repo = app.get("repository") or {}
        path = (repo.get("path") or "").strip("/")
        pom = os.path.join(args.workspace, path, "pom.xml") if path else \
            os.path.join(args.workspace, "pom.xml")
        if not os.path.isfile(pom):
            skipped.append({"id": app["id"], "name": app["name"],
                            "path": path or ".", "reason": "no pom.xml in workspace"})
            continue
        coords = pom_coordinates(pom)
        if not coords:
            skipped.append({"id": app["id"], "name": app["name"],
                            "path": path or ".", "reason": "pom.xml unreadable"})
            continue
        key = f"{coords[0]}:{coords[1]}"
        entry = {"id": app["id"], "name": app["name"], "path": path,
                 "ga": key, "pom": pom}
        if key in registry:
            # Two hub applications with the same coordinates: ambiguous,
            # keep neither for matching.
            other = registry[key]
            print(f"warning: duplicate coordinates {key}: apps "
                  f"{other['id']} ({other['name']}) and {app['id']} ({app['name']}) "
                  f"- both excluded from matching")
            registry.pop(key)
            continue
        registry[key] = entry

    print(f"registry: {len(registry)} applications resolved to Maven coordinates "
          f"({len(skipped)} skipped)")

    # 3. Match every application's pom dependencies against the registry.
    edges = {}      # from_id -> set of to_ids
    for key, app in registry.items():
        deps = set(pom_dependencies(app["pom"]))
        for dep_ga in deps:
            dep_key = f"{dep_ga[0]}:{dep_ga[1]}"
            target = registry.get(dep_key)
            if target and target["id"] != app["id"]:
                edges.setdefault(app["id"], set()).add(target["id"])

    # 4. Reconcile with the hub's dependency graph.
    status, me = hub_request(hub_url, token, "/auth/self")
    me_name = (me.get("login") or me.get("userName")) \
        if status == 200 and isinstance(me, dict) else None
    created, existing, stale, failures = [], [], [], []
    for from_id in sorted(edges):
        status, current = hub_request(hub_url, token, f"/dependencies?from.id={from_id}")
        if status != 200:
            failures.append({"from": from_id, "error": f"HTTP {status}"})
            continue
        have = {d["to"]["id"] for d in current if d.get("to")}
        for to_id in sorted(edges[from_id]):
            if to_id in have:
                existing.append({"from": from_id, "to": to_id})
            elif args.apply:
                status, resp = hub_request(
                    hub_url, token, "/dependencies", "POST",
                    json.dumps({"from": {"id": from_id}, "to": {"id": to_id}}))
                if status in (200, 201):
                    created.append({"from": from_id, "to": to_id})
                else:
                    failures.append({"from": from_id, "to": to_id,
                                     "error": f"HTTP {status}: {str(resp)[:200]}"})
            else:
                created.append({"from": from_id, "to": to_id, "dry-run": True})
        if args.prune and args.apply:
            for d in current:
                to_id = (d.get("to") or {}).get("id")
                if to_id and to_id not in edges[from_id] \
                        and me_name and d.get("createUser") == me_name:
                    status, _ = hub_request(hub_url, token,
                                            f"/dependencies/{d['id']}", "DELETE")
                    if status == 204:
                        stale.append({"from": from_id, "to": to_id, "id": d["id"]})
                    else:
                        failures.append({"from": from_id, "to": to_id,
                                         "error": f"delete HTTP {status}"})
                elif to_id and to_id not in edges[from_id]:
                    stale.append({"from": from_id, "to": to_id,
                                  "id": d["id"], "kept-other-user": True})

    result = {
        "applications_resolved": len(registry),
        "applications_skipped": skipped,
        "edges": {str(k): sorted(v) for k, v in edges.items()},
        "created": created,
        "existing": existing,
        "stale": stale,
        "failures": failures,
        "applied": bool(args.apply),
    }
    if args.output:
        with open(args.output, "w") as fh:
            json.dump(result, fh, indent=2)
    verb = "applied" if args.apply else "dry run (pass --apply to create)"
    print(f"map: {len(registry)} apps, {sum(len(v) for v in edges.values())} "
          f"inter-application edges; {verb}: "
          f"{len([c for c in created if not c.get('dry-run')])} created, "
          f"{len(existing)} already present, {len(stale)} stale, "
          f"{len(failures)} failures")
    print(json.dumps(result, indent=2))


if __name__ == "__main__":
    main()
