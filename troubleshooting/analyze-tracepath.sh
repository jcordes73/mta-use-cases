#!/usr/bin/env bash
# analyze-tracepath.sh - run tracepath inside the mtu-test pod and analyze
# the output: which hop lowers the path MTU (the culprit for jumbo-frame
# failures), where probes go unanswered (firewalls), and whether the
# destination was reached. If a lower MTU is identified, it re-runs
# tracepath at that size to confirm the path is clean there.
#
# Usage:
#   ./analyze-tracepath.sh [destination|-]
#     destination   DNS name or IP (default: $OPENAI_HOST)
#     -             analyze tracepath output read from stdin instead of
#                   running tracepath (POD_MTU env sets the start MTU;
#                   no second pass is done in this mode)
# Env overrides: POD (default mtu-test), MAX_HOPS (15), TIMEOUT (60)
#
# Prerequisite: the mtu-test pod is running (see README.md step 1).
set -euo pipefail

POD=${POD:-mtu-test}
MAX_HOPS=${MAX_HOPS:-15}
TIMEOUT=${TIMEOUT:-60}

analyze() {
    # $1 = tracepath output, $2 = start MTU, $3 = destination IP
    echo "=== tracepath analysis (start MTU $2) ==="
    echo "$1" | awk -v start_mtu="$2" -v max_hops="$MAX_HOPS" -v dest="$3" '
    BEGIN { cur = start_mtu; ncap = 0 }
    function hopnum(s) { gsub(/[?:]/, "", s); return s + 0 }

    # hop lines: " 1?: [LOCALHOST] pmtu 8901", " 3: 10.0.32.1 1.9ms pmtu 8500",
    #            " 7: no reply"
    /^[ ]*[0-9]+\??:/ {
        hop = hopnum($1)
        rest = $0; sub(/^[ ]*[0-9]+\??:[ ]*/, "", rest)
        ip = rest; sub(/[ \t].*/, "", ip)
        seen[hop] = ip
        if (rest ~ /^no reply/) { dead[hop] = 1; next }
        if (match(rest, /pmtu [0-9]+/)) {
            p = substr(rest, RSTART + 5, RLENGTH - 5) + 0
            if (p < cur) {
                label = (ip == "[LOCALHOST]") ? "(first hop, via PTB)" : ip
                printf "  hop %-3d %-28s PMTU lowered: %d -> %d\n", hop, label, cur, p
                cur = p; capper = hop; capip = label; ncap++
            }
        }
        lasthop = hop; lastip = ip
        next
    }
    # trailer lines: "     Too many hops: pmtu 1500" / "     Resume: pmtu 1500"
    /pmtu [0-9]+/ {
        if (match($0, /pmtu [0-9]+/)) {
            p = substr($0, RSTART + 5, RLENGTH - 5) + 0
            if (p < cur) { cur = p }
        }
    }
    /Too many hops/ { toomany = 1 }
    END {
        printf "  unanswered hops (likely firewalled): "
        first = 1
        for (h in dead) { printf "%s%d", (first ? "" : ", "), h; first = 0 }
        if (first) printf "none"
        printf "\n\n"
        if (ncap == 0) {
            printf "  No hop lowered the MTU below %d - the path is clean at this size,\n", start_mtu
            printf "  no MTU problem seen on this path.\n"
        } else {
            printf "  Effective path MTU: %d (payload <= %d bytes)\n", cur, cur - 28
            printf "  Cap introduced at hop %d (%s)\n", capper, capip
            printf "  -> this hop is the bottleneck for jumbo frames; nothing beyond it can help.\n"
        }
        printf "\n"
        if (lastip == dest) {
            printf "  Destination reached at hop %d.\n", lasthop
        } else if (toomany) {
            printf "  Destination NOT reached within %d hops (last answered: hop %d, %s).\n", max_hops, lasthop, lastip
            printf "  -> rerun with MAX_HOPS=30; if still blocked, probes are firewalled past that edge\n     (use TCP checks, e.g. curl, to confirm reachability).\n"
        }
    }'
    echo "=== end analysis ==="
}

run_tracepath() {
    # $1 = starting packet length
    oc exec "$POD" -- timeout "$TIMEOUT" \
        tracepath -n -m "$MAX_HOPS" -l "$1" "$HOST" 2>&1 || true
}

if [[ ${1:-} == "-" ]]; then
    # analyze pre-captured output (single pass only)
    OUT=$(cat)
    POD_MTU=${POD_MTU:?set POD_MTU when analyzing stdin}
    DEST_IP=${DEST_IP:-unknown}
    echo "--- raw tracepath output ---"
    echo "$OUT"
    echo "----------------------------"
    analyze "$OUT" "$POD_MTU" "$DEST_IP"
    exit 0
fi

HOST=${1:-${OPENAI_HOST:?set OPENAI_HOST or pass a destination as argument}}
POD_MTU=$(oc exec "$POD" -- cat /sys/class/net/eth0/mtu)
DEST_IP=$(oc exec "$POD" -- getent hosts "$HOST" | awk 'NR==1{print $1}')
echo "tracepath from pod '$POD' to $HOST ($DEST_IP), start MTU $POD_MTU"

# pass 1: probe at the interface MTU to identify the effective path MTU
echo
echo "### pass 1: probe at the interface MTU ($POD_MTU)"
OUT1=$(run_tracepath "$POD_MTU")
echo "--- raw tracepath output ---"
echo "$OUT1"
echo "----------------------------"
analyze "$OUT1" "$POD_MTU" "$DEST_IP"

# lowest MTU identified anywhere on the path
LOWEST=$(echo "$OUT1" | grep -oE 'pmtu [0-9]+' | awk '{print $2}' | sort -n | head -1)
if [[ -z $LOWEST || $LOWEST -gt $POD_MTU ]]; then LOWEST=$POD_MTU; fi

# pass 2: confirm the path is clean when probed at the lowest MTU found
if (( LOWEST < POD_MTU )); then
    echo
    echo "### pass 2: re-test at the lowest MTU identified ($LOWEST)"
    OUT2=$(run_tracepath "$LOWEST")
    echo "--- raw tracepath output ---"
    echo "$OUT2"
    echo "----------------------------"
    analyze "$OUT2" "$LOWEST" "$DEST_IP"
fi
