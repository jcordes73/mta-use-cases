#!/bin/bash
# Entrypoint wrapper for the rhda agent image.
#
# The migration harness fetches the full application record from the Hub
# (repository URL, branch AND path) but only keeps the clone credentials,
# then clears HUB_BASE_URL/HUB_TOKEN/APP_ID from the environment before
# the agent starts - so the agent can neither learn the application's
# repository path nor report findings back to the Hub.
#
# This wrapper runs BEFORE the harness, while the Hub env is still set,
# and provisions what the agent needs as files under a runtime directory:
#   hub-url    - the Hub API base URL
#   app-id     - the application ID from the run
#   token      - the Hub bearer token (0600; revoked by the harness at the
#                end of the run, so the file dies with the run's validity)
#   app-name   - the application's name
#   app-path   - the application record's repository path (the authoritative
#                scan scope for monorepo checkouts)
# The directory is /tmp/konveyor/hub-report - /run/konveyor is mounted
# READ-ONLY in the sandbox (it carries the harness's stage infrastructure),
# so a directory under it cannot be used.
# It then execs the real harness so signal handling and the PID stay intact.
set -eu

DIR=/tmp/konveyor/hub-report

if [ -n "${HUB_BASE_URL:-}" ] && [ -n "${APP_ID:-}" ] && [ -n "${HUB_TOKEN:-}" ]; then
  # Provisioning must never take the run down: if anything fails here the
  # scan still works (minus Hub reporting / record-based scoping).
  if mkdir -p "$DIR" && chmod 700 "$DIR" \
     && printf '%s' "$HUB_BASE_URL" > "$DIR/hub-url" \
     && printf '%s' "$APP_ID" > "$DIR/app-id" \
     && (umask 077 && printf '%s' "$HUB_TOKEN" > "$DIR/token"); then
  # App metadata: best-effort - the scan works without it (falls back to
  # the task instructions' path), only reporting and auto-scoping want it.
  # The repository path goes to its own app-path file: the skill reads it
  # as the authoritative scan scope.
  APP_JSON=$(curl -sf -H "Authorization: Bearer $HUB_TOKEN" \
    "${HUB_BASE_URL%/}/applications/${APP_ID}" 2>/dev/null || true)
  if [ -n "$APP_JSON" ]; then
    printf '%s' "$APP_JSON" | DIR="$DIR" python3 -c '
import json, os, sys
app = json.load(sys.stdin)
repo = app.get("repository") or {}
d = os.environ["DIR"]
with open(os.path.join(d, "app-name"), "w") as fh:
    fh.write(app.get("name", ""))
with open(os.path.join(d, "app-path"), "w") as fh:
    fh.write(repo.get("path") or "")
' 2>/dev/null || true
  fi
    echo "hub-report: provisioned $(ls "$DIR" | tr '\n' ' ')"
  else
    echo "hub-report: provisioning failed - continuing without Hub reporting"
  fi
else
  echo "hub-report: HUB env not set - skipping (standalone mode)"
fi

exec "$@"
