#!/usr/bin/env bash
# TimedItem test matrix: boots real servers (Paper / Folia / Purpur /
# Paper 1.20.x) headless, runs the in-game test harness
# (`titest run all`) from the console and reports a per-lane verdict.
#
# Usage:
#   ./testing/run-matrix.sh paper|folia|purpur|paper120   one lane
#   ./testing/run-matrix.sh all                           the 4 lanes
#   ./testing/run-matrix.sh hooks-lane                    paper + real hook jars
#   ./testing/run-matrix.sh paper --rebuild               force rebuild first
#
# Requirements: JDK 21 (JAVA_HOME), curl, Maven at the usual .tools path.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TESTING="$ROOT/testing"
SRV="$TESTING/servers"
RUNS="$SRV/runs"
MVN="C:/Users/Teerachot.a/Documents/.tools/maven/apache-maven-3.9.9/bin/mvn"
export JAVA_HOME="${JAVA_HOME:-/c/Users/teerachot.a.OBCJLL0067/AppData/Local/Programs/Eclipse Adoptium/jdk-21.0.12.101-hotspot}"

# always matches pom.xml so a version bump never leaves a stale jar in plugins/
PLUGIN_VERSION="$(sed -n 's,^[[:space:]]*<version>\(.*\)</version>,\1,p' "$ROOT/pom.xml" | head -1)"
FILL_API="https://fill.papermc.io/v3"
PURPUR_API="https://api.purpurmc.org/v2"
BOOT_WAIT="${BOOT_WAIT:-100}"   # seconds to let the server reach "Done"
RUN_WAIT="${RUN_WAIT:-260}"     # seconds for the suite to complete
KILL_AFTER="${KILL_AFTER:-420}" # hard cap per lane

REBUILD=0
LANES=()
for arg in "$@"; do
    case "$arg" in
        --rebuild) REBUILD=1 ;;
        paper|folia|purpur|paper120|all|hooks-lane) LANES+=("$arg") ;;
        *) echo "unknown argument: $arg" >&2; exit 2 ;;
    esac
done
if [ ${#LANES[@]} -eq 0 ]; then
    echo "usage: $0 paper|folia|purpur|paper120|all|hooks-lane [--rebuild]" >&2
    exit 2
fi

log() { echo "[matrix] $*" >&2; }

# ---------------------------------------------------------------- builds
ensure_artifacts() {
    if [ "$REBUILD" = "1" ] || [ ! -f "$ROOT/target/TimedItem-$PLUGIN_VERSION.jar" ]; then
        log "building main plugin..."
        (cd "$ROOT" && "$MVN" -q install -DskipTests)
    fi
    if [ "$REBUILD" = "1" ] || [ ! -f "$TESTING/harness/target/TimedItemTestHarness-1.0.0.jar" ]; then
        log "building test harness..."
        (cd "$ROOT" && "$MVN" -q -f testing/harness/pom.xml package "-Dtimeditem.version=$PLUGIN_VERSION")
    fi
    if [ "$REBUILD" = "1" ] || [ ! -f "$TESTING/fakehooks/out/FakeShopkeepers.jar" ]; then
        log "building FakeHooks stubs..."
        (cd "$TESTING/fakehooks" && bash build.sh)
    fi
}

# ------------------------------------------------------------ downloads
family_version() { # project family_regex -> newest stable version of that family
    curl -fsS --max-time 60 "$FILL_API/projects/$1" \
        | grep -o "\"$2\":\[[^]]*\]" | head -1 \
        | sed 's/.*\[//; s/\]$//' | tr ',' '\n' | tr -d '" ' | grep -v '^$' | head -1
}

latest_build_url() { # project version -> direct download url of the latest stable build
    curl -fsS --max-time 60 "$FILL_API/projects/$1/versions/$2/builds/latest" \
        | grep -o '"url":"[^"]*"' | head -1 | sed 's/^"url":"//; s/"$//'
}

download_server() { # lane url  -> cached jar path
    local lane="$1" url="$2" jar="$SRV/jars/$lane.jar"
    if [ -f "$jar" ]; then
        log "$lane: using cached server jar ($(du -m "$jar" | cut -f1) MB)"
        echo "$jar"; return 0
    fi
    mkdir -p "$SRV/jars"
    log "$lane: downloading server jar..."
    curl -fL --max-time 600 --retry 2 -o "$jar.part" "$url"
    mv "$jar.part" "$jar"
    echo "$jar"
}

paper_jar() { # project family_regex lane
    local project="$1" pattern="$2" lane="$3"
    local version url
    version=$(family_version "$project" "$pattern")
    [ -n "$version" ] || { echo "cannot resolve $project version" >&2; return 1; }
    url=$(latest_build_url "$project" "$version")
    [ -n "$url" ] || { echo "cannot resolve $project $version build url" >&2; return 1; }
    log "$lane: $project $version (latest stable build)"
    download_server "$lane" "$url"
}

purpur_jar() { # lane
    local lane="$1" version build
    version=$(family_version_purpur '1\.21')
    build=$(curl -fsS --max-time 60 "$PURPUR_API/purpur/$version" \
        | grep -o '"latest":"[0-9]*"' | head -1 | tr -dc '0-9')
    [ -n "$version" ] && [ -n "$build" ] || { echo "cannot resolve purpur version/build" >&2; return 1; }
    log "$lane: purpur $version build $build"
    download_server "$lane" "$PURPUR_API/purpur/$version/$build/download"
}

family_version_purpur() { # family_regex - purpur lists versions as one flat ascending array
    curl -fsS --max-time 60 "$PURPUR_API/purpur" \
        | grep -o '"versions":\[[^]]*\]' | head -1 \
        | sed 's/.*\[//; s/\]$//' | tr ',' '\n' | tr -d '" ' \
        | grep -E "^$1" | tail -1
}

# ------------------------------------------------------------- configs
lane_config() { # dest config.yml path
    cat > "$1" <<'EOF'
config-version: 12
Settings:
  language: en
  metrics: false
  debug: true
  expire_check:
    enabled: true
    interval_seconds: 5
    scan_world_items: true
    ignore_creative: true
  blacklist:
    materials: [BEDROCK]
  expiry_commands: []
  actionbar:
    enabled: false
  sound:
    enabled: false
  particles:
    enabled: false
  update_check:
    enabled: false
  recovery:
    enabled: true
  storage:
    type: flat
  warning:
    enabled: false
  expiry_transform:
    enabled: true
    default: "ash"
  transforms:
    ash:
      material: "GUNPOWDER"
      name: "&8Ash"
      lore: ["&7Remains of an expired item"]
      amount: 1
      commands: []
  hologram:
    enabled: true
    format: "&e⌛ {time}"
    height: 0.5
  live_lore:
    enabled: false
  container_scan:
    enabled: true
    mode: "periodic"
    interval_seconds: 5
  anti_hide:
    enabled: false
Hooks:
  mmoitems: true
  itemsadder: true
  oraxen: true
  nexo: true
  executableitems: true
  slimefun: true
  ecoitems: true
  shopkeepers:
    enabled: true
    auto_timer: "30s"
  crazycrates:
    enabled: true
    auto_timer: "45s"
EOF
}

lane_properties() { # dest port
    local dest="$1" port="$2"
    cat > "$dest" <<EOF
online-mode=false
level-type=minecraft\:flat
view-distance=2
simulation-distance=2
spawn-monsters=false
spawn-animals=false
spawn-npcs=false
spawn-protection=0
max-players=0
server-port=$port
motd=TimedItem test lane
sync-chunk-writes=false
spawn-chunk-radius=16
EOF
}

# --------------------------------------------------------------- runner
copy_plugins() { # dir lane
    local dir="$1" lane="$2"
    mkdir -p "$dir/plugins/TimedItem"
    cp "$ROOT/target/TimedItem-$PLUGIN_VERSION.jar" "$dir/plugins/"
    cp "$TESTING/harness/target/TimedItemTestHarness-1.0.0.jar" "$dir/plugins/"
    local fake jar real
    for fake in "$TESTING/fakehooks/out/"*.jar; do
        jar="$(basename "$fake")"
        if [ "$lane" = "hooks-lane" ]; then
            local skip=0
            for real in "$TESTING/real-hooks/"*.jar; do
                [ -e "$real" ] || continue
                real="$(basename "$real" .jar)"
                case "${jar#Fake}" in
                    *"$real"*|"$real"*) skip=1 ;;
                esac
            done
            [ "$skip" = "1" ] && continue
        fi
        cp "$fake" "$dir/plugins/"
    done
    if [ "$lane" = "hooks-lane" ]; then
        for real in "$TESTING/real-hooks/"*.jar; do
            [ -e "$real" ] && cp "$real" "$dir/plugins/"
        done
    fi
    lane_config "$dir/plugins/TimedItem/config.yml"
}

run_lane() { # lane project version_pattern port
    local lane="$1" project="$2" pattern="$3" port="$4"
    local dir="$RUNS/$lane"
    # skip a port if something is still bound to it (stale server)
    while (exec 3<>"/dev/tcp/127.0.0.1/$port") 2>/dev/null; do
        exec 3>&- 3<&- || true
        log "$lane: port $port is busy - trying $((port + 1))"
        port=$((port + 1))
    done
    local jar
    if [ "$lane" = "purpur" ]; then
        jar=$(purpur_jar "$lane")
    else
        jar=$(paper_jar "$project" "$pattern" "$lane")
    fi

    log "=== lane $lane ==="
    rm -rf "$dir"
    mkdir -p "$dir"
    echo "eula=true" > "$dir/eula.txt"
    lane_properties "$dir/server.properties" "$port"
    copy_plugins "$dir" "$lane"

    local started
    started=$(date +%s)
    ( sleep "$BOOT_WAIT"; echo "timeditem reload"; sleep 5; echo "titest run all"; sleep "$RUN_WAIT"; echo "stop" ) \
        | ( cd "$dir" && timeout --kill-after=10 "$KILL_AFTER" \
              java -Xmx1G -Dcom.mojang.eula.agree=true -jar "$jar" nogui ) \
        > "$dir/console.log" 2>&1 || true

    summarize "$lane" "$dir" "$started"
}

summarize() { # lane dir startseconds
    local lane="$1" dir="$2" started="$3"
    local log="$dir/console.log"
    local verdict="FAILED"
    local suite
    suite=$(grep -o 'SUITE COMPLETE passed=[0-9]* failed=[0-9]* skipped=[0-9]* total=[0-9]*' "$log" | tail -1 || true)
    local boot_ok=0
    grep -q "Done (" "$log" && boot_ok=1

    echo ""
    echo "--- lane $lane ($(docker_time "$started")) ---"
    if [ "$boot_ok" != "1" ]; then
        echo "server did not reach 'Done' - boot failure. Last lines:"
        tail -20 "$log"
        echo "$verdict $lane boot" >> "$RUNS/summary.txt"
        return 1
    fi
    if [ -z "$suite" ]; then
        echo "harness suite never completed (no SUITE COMPLETE line). TITEST lines found:"
        grep "\[TITEST\]" "$log" | tail -30 || echo "  (none)"
        echo "$verdict $lane suite-incomplete" >> "$RUNS/summary.txt"
        return 1
    fi
    local passed failed skipped
    passed=$(echo "$suite" | sed 's/.*passed=\([0-9]*\).*/\1/')
    failed=$(echo "$suite" | sed 's/.*failed=\([0-9]*\).*/\1/')
    skipped=$(echo "$suite" | sed 's/.*skipped=\([0-9]*\).*/\1/')
    grep -E "\[TITEST\] (PASS|FAIL|SKIP)" "$log" | sed 's/.*\[TITEST\]/  [TITEST]/'
    local errors
    errors=$(grep -c "\[SEVERE\]\|\[ERROR\]\|ERROR:" "$log" || true)
    if [ "$failed" -gt 0 ]; then
        verdict="FAILED"
    else
        verdict="PASSED"
    fi
    echo "result: $verdict (passed=$passed failed=$failed skipped=$skipped, error lines in log: $errors)"
    echo "$verdict $lane passed=$passed failed=$failed skipped=$skipped" >> "$RUNS/summary.txt"
    [ "$failed" -eq 0 ]
}

docker_time() { echo "$(( $(date +%s) - $1 ))s"; }

# ----------------------------------------------------------------- main
mkdir -p "$RUNS"
rm -f "$RUNS/summary.txt"
ensure_artifacts

overall=0
for lane in "${LANES[@]}"; do
    case "$lane" in
        paper)      run_lane paper paper '1\.21' 25581 || overall=1 ;;
        folia)      run_lane folia folia '1\.21' 25582 || overall=1 ;;
        purpur)     run_lane purpur purpur '1\.21' 25583 || overall=1 ;;
        paper120)   run_lane paper120 paper '1\.20' 25584 || overall=1 ;;
        hooks-lane) run_lane hooks-lane paper '1\.21' 25585 || overall=1 ;;
        all)
            run_lane paper paper '1\.21' 25581 || overall=1
            run_lane folia folia '1\.21' 25582 || overall=1
            run_lane purpur purpur '1\.21' 25583 || overall=1
            run_lane paper120 paper '1\.20' 25584 || overall=1
            ;;
    esac
done

echo ""
echo "=== matrix summary ==="
cat "$RUNS/summary.txt" || true
exit "$overall"
