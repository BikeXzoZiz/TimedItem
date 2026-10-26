#!/usr/bin/env bash
# Builds the 9 FakeHooks stub plugins into out/*.jar.
# Requirements: JDK (JAVA_HOME or on PATH) + spigot-api 1.13.2 in the
# local Maven repo (a normal main-project build puts it there).
set -euo pipefail
cd "$(dirname "$0")"

JAVAC="${JAVA_HOME:-}/bin/javac"
JAR="${JAVA_HOME:-}/bin/jar"
if [ ! -x "$JAVAC" ]; then JAVAC="javac"; JAR="jar"; fi

SPIGOT="$HOME/.m2/repository/org/spigotmc/spigot-api/1.13.2-R0.1-SNAPSHOT/spigot-api-1.13.2-R0.1-SNAPSHOT.jar"
if [ ! -f "$SPIGOT" ]; then
    echo "spigot-api 1.13.2 jar not found at $SPIGOT" >&2
    echo "Run the main project build once (mvn install) first." >&2
    exit 1
fi

rm -rf build out
mkdir -p build/classes out

# Tell Paper the jars are already mojang-mapped so its remapper leaves
# the fake API classes (and the static fields) exactly as compiled.
cat > build/manifest.mf <<'EOF'
Manifest-Version: 1.0
paperweight-mappings-namespace: mojang
EOF

echo "Compiling FakeHooks sources..."
find src -name '*.java' > build/sources.txt
"$JAVAC" --release 8 -encoding UTF-8 -nowarn -cp "$SPIGOT" -d build/classes @build/sources.txt

write_plugin() { # name main extra...
    local name="$1" main="$2"; shift 2
    mkdir -p "build/$name"
    {
        echo "name: $name"
        echo "main: $main"
        echo "version: '2.0-fake'"
        echo "api-version: '1.13'"
        echo "folia-supported: true"
        echo "description: TimedItem test stub - NOT the real $name plugin."
        if [ $# -gt 0 ]; then
            echo "commands:"
            for cmd in "$@"; do
                echo "  $cmd:"
                echo "    description: fire a fake $name event"
            done
        fi
    } > "build/$name/plugin.yml"
}

pack() { # name path1 [path2...]
    local name="$1"; shift
    local args=()
    local path
    for path in "$@"; do
        args+=(-C "build/classes" "$path")
    done
    args+=(-C "build/$name" "plugin.yml")
    "$JAR" cfm "out/Fake$name.jar" build/manifest.mf "${args[@]}"
    echo "built out/Fake$name.jar"
}

write_plugin "MythicLib" "io.lumine.mythiclib.MythicLib"
pack "MythicLib" "io/lumine"

write_plugin "MMOItems" "net.Indyuce.mmoitems.MMOItems"
pack "MMOItems" "net/Indyuce"

write_plugin "ItemsAdder" "dev.lone.itemsadder.ItemsAdder"
pack "ItemsAdder" "dev/lone"

write_plugin "Oraxen" "io.th0rgal.oraxen.OraxenPlugin"
pack "Oraxen" "io/th0rgal"

write_plugin "Nexo" "com.nexomc.nexo.NexoPlugin"
pack "Nexo" "com/nexomc"

write_plugin "ExecutableItems" "com.ssomar.executableitems.ExecutableItemsPlugin"
pack "ExecutableItems" "com/ssomar"

write_plugin "Slimefun" "fake.slimefun.FakeSlimefunPlugin"
pack "Slimefun" "io/github/thebusybiscuit" "fake/slimefun"

write_plugin "EcoItems" "com.willfp.ecoitems.EcoItemsPlugin"
pack "EcoItems" "com/willfp"

write_plugin "Shopkeepers" "com.nisovin.shopkeepers.ShopkeepersPlugin" "fakesh"
pack "Shopkeepers" "com/nisovin" "fake/shopkeepers"

write_plugin "CrazyCrates" "com.badbones69.crazycrates.CrazyCrates" "fakecc"
pack "CrazyCrates" "com/badbones69" "fake/crazycrates"

echo "FakeHooks build complete: $(ls out | wc -l) jars in testing/fakehooks/out/"
