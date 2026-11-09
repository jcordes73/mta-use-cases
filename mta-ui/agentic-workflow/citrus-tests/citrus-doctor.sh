#!/usr/bin/env bash
# citrus-doctor: pre-flight checks for the citrus-tests author stage.
#
# Converts the known discover-fail cycles (each one a failed mvn run plus
# an LLM round-trip) into a single fast local check. Modes follow the
# author stage's timeline:
#
#   citrus-doctor.sh pre       <app-path>   # before any authoring
#   citrus-doctor.sh deps      <app-path>   # after pom edits, before 1st test run
#   citrus-doctor.sh container <app-path>   # before creating the container profile
#
# Output: PASS / WARN / FAIL lines. Exit 0 = all clear, 1 = at least one
# FAIL (fix before proceeding), 2 = warnings only (fix recommended).
# The failure signatures and their fixes are documented in the
# citrus-test-author skill - this script only detects, it never edits.

set -u
fail=0; warn=0

say()  { printf '%s\n' "$*"; }
FAIL() { say "FAIL  $*"; fail=$((fail+1)); }
WARN() { say "WARN  $*"; warn=$((warn+1)); }
PASS() { say "PASS  $*"; }

mode="${1:-}"; app="${2:-}"
if [ -z "$mode" ] || [ -z "$app" ] || [ ! -d "$app" ]; then
    say "usage: citrus-doctor.sh <pre|deps|container> <app-path>"
    exit 1
fi
app=$(cd "$app" && pwd)

# collect the pom chain from the application path up to the repo root
poms=$(d="$app"; while [ "$d" != "/" ]; do
    [ -f "$d/pom.xml" ] && printf '%s\n' "$d/pom.xml"; d=$(dirname "$d"); done)

# ---------------------------------------------------------------- pre mode
# resolve a ${property} reference against the pom chain (one hop)
resolve_prop() {
    local ref="$1"
    local name="${ref#\$\{}"; name="${name%\}}"
    grep -h "<${name}>" $poms 2>/dev/null | head -1 | \
        sed -n "s/.*<${name}>\(.*\)<\/${name}>.*/\1/p"
}

if [ "$mode" = "pre" ]; then

    # 1. Maven settings proxy: the sandbox image ships a settings.xml with a
    #    Squid proxy that is dead in connected environments.
    settings="${HOME}/.m2/settings.xml"
    if [ -f "$settings" ]; then
        while IFS=' ' read -r host port; do
            [ -z "$host" ] && continue
            if timeout 3 bash -c "exec 3<>/dev/tcp/${host}/${port}" 2>/dev/null; then
                PASS "maven proxy ${host}:${port} reachable"
            else
                FAIL "maven proxy ${host}:${port} in ${settings} is UNREACHABLE - strip the <proxies> block (connected env) or set MAVEN_SETTINGS_XML (disconnected env)"
            fi
        done <<EOF
$(tr -d ' \t\r' < "$settings" | \
    sed -n 's|.*<host>\([^<]*\)</host>.*<port>\([^<]*\)</port>.*|\1 \2|p')
EOF
        grep -q "<proxy>" "$settings" || PASS "no proxy configured in maven settings"
    else
        PASS "no maven settings.xml (no proxy to misroute downloads)"
    fi

    # 2. Java baseline: Citrus 5.x needs 17+.
    java_major=$(java -version 2>&1 | head -1 | sed -n 's/.*"\([0-9]*\)\..*/\1/p')
    case "${java_major:-0}" in
        [0-9]|[0-9][0-9]) ;;
        *) java_major=0 ;;
    esac
    if [ "$java_major" -ge 17 ] 2>/dev/null; then
        PASS "java ${java_major} (Citrus 5.x line viable)"
    elif [ "$java_major" -eq 0 ]; then
        WARN "could not determine java version from 'java -version'"
    else
        WARN "java ${java_major} - Citrus 5.x will not run; the plan must choose the 3.x line"
    fi

    # 3. Surefire: version floor 2.22 for JUnit 5, and restrictive includes.
    #    The version usually comes from the effective pom (Maven defaults /
    #    toolchain), not from any pom in the chain - query the effective pom.
    effpom=$(mktemp)
    if mvn -B -q -o help:effective-pom -f "$app/pom.xml" -Doutput="$effpom" >/dev/null 2>&1 && [ -s "$effpom" ]; then
        :
    elif mvn -B -q help:effective-pom -f "$app/pom.xml" -Doutput="$effpom" >/dev/null 2>&1 && [ -s "$effpom" ]; then
        :
    fi
    if [ -s "$effpom" ]; then
        sf_version=$(grep -A2 "<artifactId>maven-surefire-plugin</artifactId>" "$effpom" | \
            grep "<version>" | head -1 | sed -n 's/.*<version>\(.*\)<\/version>.*/\1/p')
        if [ -n "$sf_version" ]; then
            sf_major=${sf_version%%.*}; sf_minor=$(printf '%s' "$sf_version" | cut -d. -f2)
            if [ "$sf_major" -gt 2 ] || { [ "$sf_major" -eq 2 ] && [ "$sf_minor" -ge 22 ]; } 2>/dev/null; then
                PASS "surefire ${sf_version} (JUnit 5 capable)"
            else
                FAIL "surefire ${sf_version} < 2.22 - JUnit 5 tests would silently not run; add a test-scoped surefire upgrade"
            fi
        else
            WARN "surefire version not found in the effective pom - confirm it runs JUnit 5 (needs >= 2.22) from the first test run's report"
        fi
        if grep -A12 "<artifactId>maven-surefire-plugin</artifactId>" "$effpom" | grep -q "<include>"; then
            WARN "surefire declares explicit <includes> - confirm **/*Test.java is covered or the new suite silently runs zero tests"
        else
            PASS "surefire has no restrictive <includes>"
        fi
    else
        WARN "could not build the effective pom (offline?) - manually confirm surefire >= 2.22 and no restrictive <includes> before relying on green"
    fi
    rm -f "$effpom"

    # 4. Checkstyle on test sources: the quickstarts ruleset kills the build
    #    on redundant/unused imports - iterate with it off.
    if grep -q "checkstyle" $poms 2>/dev/null; then
        WARN "checkstyle is wired into the build - iterate with -Dcheckstyle.skip=true and run the suite once clean before committing (redundant/unused imports WILL fail the build otherwise)"
    else
        PASS "no checkstyle in the pom chain"
    fi

    # 5. BOM-managed jackson-annotations: Citrus 5 needs the 2.2x line.
    ja_version=$(grep -h "jackson-annotations" -A2 $poms 2>/dev/null | grep "<version>" | head -1 | sed -n 's/.*<version>\(.*\)<\/version>.*/\1/p')
    if [ -n "$ja_version" ]; then
        ja_major=${ja_version%%.*}
        if [ "$ja_major" -ge 2 ] 2>/dev/null && [ "$(printf '%s' "$ja_version" | cut -d. -f2)" -ge 20 ] 2>/dev/null; then
            PASS "jackson-annotations ${ja_version} managed (Jackson-3 line compatible)"
        else
            WARN "jackson-annotations ${ja_version} managed by the BOM - when adding Citrus 5.x, pin 2.22+ test-scoped (compiles fine, all tests fail at runtime otherwise)"
        fi
    else
        PASS "jackson-annotations not BOM-managed"
    fi
fi

# --------------------------------------------------------------- deps mode
if [ "$mode" = "deps" ]; then
    dep_list=$(mktemp)
    if ! mvn -B -q dependency:list -DoutputFile="$dep_list" -f "$app/pom.xml" >/dev/null 2>&1 \
        && [ ! -s "$dep_list" ]; then
        mvn -B dependency:list -f "$app/pom.xml" 2>/dev/null | \
            grep -oE '[a-zA-Z0-9._-]+:[a-zA-Z0-9._-]+:jar:[^: ]+:[a-z]+' > "$dep_list" || true
    fi
    deps=$(cat "$dep_list"); rm -f "$dep_list"

    # 6. jandex clash: RESTEasy's org.jboss:jandex 2.x shadows the
    #    io.smallrye:jandex 3.x Weld 5 needs (runtime NoSuchMethodError).
    if printf '%s\n' "$deps" | grep -q "org\.jboss:jandex:"; then
        if printf '%s\n' "$deps" | grep -q "io\.smallrye:jandex:" || \
           printf '%s\n' "$deps" | grep -q "org\.jboss\.weld:weld-core-impl:jar:5\."; then
            FAIL "org.jboss:jandex (pre-3.x) co-resident with Weld 5/io.smallrye:jandex - exclude org.jboss:jandex (it shadows org.jboss.jandex.* classes) and add io.smallrye:jandex test-scoped, or the first test run dies with NoSuchMethodError: Indexer.indexWithSummary"
        else
            PASS "org.jboss:jandex present without Weld 5 (no clash)"
        fi
    else
        PASS "no org.jboss:jandex on the classpath"
    fi

    # 7. resolved jackson-annotations must be the 2.2x line when Citrus 5 is used
    if printf '%s\n' "$deps" | grep -q "org\.citrusframework:citrus-junit-jupiter"; then
        ja_resolved=$(printf '%s\n' "$deps" | grep "com\.fasterxml\.jackson\.core:jackson-annotations:" | head -1 | cut -d: -f4)
        if [ -n "$ja_resolved" ] && [ "$(printf '%s' "$ja_resolved" | cut -d. -f2)" -lt 20 ] 2>/dev/null; then
            FAIL "jackson-annotations ${ja_resolved} resolved with Citrus 5.x - pin 2.22+ test-scoped (compilation succeeds, every test fails at runtime with tools.jackson errors)"
        else
            PASS "jackson-annotations ${ja_resolved:-<none>} compatible with Citrus 5"
        fi
    # 7b. hibernate-core declared test-scoped while hibernate-jpamodelgen is
    #     provided: nearest-wins removes hibernate-core from the MAIN-compile
    #     classpath and crashes the annotation processor on COLD compiles
    #     (NoClassDefFoundError ArrayHelper) - warm target/classes masks it.
    if printf '%s\n' "$deps" | grep -q "org\.hibernate\.orm:hibernate-jpamodelgen" && \
       grep -A4 "<artifactId>hibernate-core</artifactId>" $poms 2>/dev/null | grep -q "<scope>test</scope>"; then
        FAIL "hibernate-core declared test-scoped while hibernate-jpamodelgen is provided - nearest-wins mediation removes hibernate-core from the main-compile classpath and crashes HibernateProcessor on cold compiles (NoClassDefFoundError ArrayHelper); drop the test-scoped declaration and rely on the provided transitive (visible on the test classpath)"
    else
        PASS "hibernate-core/jpamodelgen scope shape OK"
    fi

    # 8. JUnit 5 engine for the Citrus Jupiter runtime
        if printf '%s\n' "$deps" | grep -q "junit-jupiter-engine"; then
            PASS "junit-jupiter-engine on the test classpath"
        else
            FAIL "citrus-junit-jupiter present but junit-jupiter-engine missing - tests would be skipped silently"
        fi
    else
        PASS "no citrus-junit-jupiter (deps-mode jackson/engine checks n/a)"
    fi
fi

# ---------------------------------------------------------- container mode
if [ "$mode" = "container" ]; then
    # 9. wildfly-maven-plugin version floor (resolve ${property} versions)
    wf_version=$(grep -h "wildfly-maven-plugin" -A3 $poms 2>/dev/null | grep "<version>" | head -1 | sed -n 's/.*<version>\(.*\)<\/version>.*/\1/p')
    case "$wf_version" in
        '${'*) wf_version=$(resolve_prop "$wf_version") ;;
    esac
    if [ -z "$wf_version" ]; then
        PASS "wildfly-maven-plugin not yet declared (recipe pins >= 5.x)"
    else
        wf_major=${wf_version%%.*}
        if [ "$wf_major" -ge 5 ] 2>/dev/null; then
            PASS "wildfly-maven-plugin ${wf_version} (>= 5, can parse EAP feature-pack schema 3.0)"
        else
            FAIL "wildfly-maven-plugin ${wf_version} < 5 - cannot parse the EAP feature-pack schema; use >= 5.x"
        fi
    fi
    # 10. channel manifest: the real EAP artifact resolver
    if grep -q "<channels>" $poms 2>/dev/null; then
        PASS "EAP channel manifest present (the real resolver for EAP artifacts)"
    else
        WARN "no <channels> manifest in the pom chain - several EAP artifacts are not on redhat-ga; the profile must resolve via org.jboss.eap.channels:eap-<ver>"
    fi
    # 11. profile-level pluginRepositories for the Galleon resolution
    if grep -q "repository.jboss.org\|maven.repository.redhat.com" $poms 2>/dev/null; then
        PASS "Red Hat repositories referenced"
    else
        WARN "no Red Hat Maven repositories in the pom chain - the container profile needs its own <pluginRepositories> (Galleon resolves feature packs as plugin dependencies)"
    fi
fi

if [ "$fail" -gt 0 ]; then exit 1; fi
if [ "$warn" -gt 0 ]; then exit 2; fi
exit 0
