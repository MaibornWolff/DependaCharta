#!/usr/bin/env bash
set -euo pipefail

SKILL_DIR="$(cd "$(dirname "$0")/.." && pwd)"
BIN_DIR="$SKILL_DIR/bin"
JAR_PATH="$BIN_DIR/dependacharta.jar"
DOCKER_IMAGE="maibornwolff/dependacharta-analysis:latest"

# --- Usage ---

usage() {
    echo "Usage: $(basename "$0") <directory> [options]"
    echo ""
    echo "Options:"
    echo "  -o <dir>      Output directory (default: <directory>/.dependacharta)"
    echo "  -f <name>     Output filename without extension (default: analysis)"
    echo "  -l <level>    Log level: debug|info|warn|error|fatal (default: info)"
    echo "  -g            Skip graph analysis (cycles, levels)"
    echo "  -h            Show this help"
    exit 1
}

# --- Parse arguments ---

if [ $# -lt 1 ] || [ "$1" = "-h" ] || [ "$1" = "--help" ]; then
    usage
fi

TARGET_DIR="$1"
shift

OUTPUT_DIR=""
FILENAME="analysis"
LOG_LEVEL="info"
SKIP_GRAPH=""

while [ $# -gt 0 ]; do
    case "$1" in
        -o) OUTPUT_DIR="$2"; shift 2 ;;
        -f) FILENAME="$2"; shift 2 ;;
        -l) LOG_LEVEL="$2"; shift 2 ;;
        -g) SKIP_GRAPH="true"; shift ;;
        *) echo "Unknown option: $1"; usage ;;
    esac
done

# Validate and resolve target directory to absolute path
if [ ! -d "$TARGET_DIR" ]; then
    echo "❌ Directory not found: $TARGET_DIR"
    exit 1
fi
TARGET_DIR="$(cd "$TARGET_DIR" && pwd)"

# Default output directory
if [ -z "$OUTPUT_DIR" ]; then
    OUTPUT_DIR="$TARGET_DIR/.dependacharta"
fi

# Resolve output directory to absolute path (create if needed)
mkdir -p "$OUTPUT_DIR"
OUTPUT_DIR="$(cd "$OUTPUT_DIR" && pwd)"

# --- Detect runtime ---

run_with_java() {
    local args=(-jar "$JAR_PATH" -d "$TARGET_DIR" -o "$OUTPUT_DIR" -f "$FILENAME" -l "$LOG_LEVEL")
    if [ -n "$SKIP_GRAPH" ]; then
        args+=(-g)
    fi
    echo "Running DependaCharta analysis (Java)..."
    echo "  Target:  $TARGET_DIR"
    echo "  Output:  $OUTPUT_DIR/$FILENAME.cg.json"
    echo ""
    java "${args[@]}"
}

run_with_docker() {
    local args=(-d /workspace -o /output -f "$FILENAME" -l "$LOG_LEVEL")
    if [ -n "$SKIP_GRAPH" ]; then
        args+=(-g)
    fi
    echo "Running DependaCharta analysis (Docker)..."
    echo "  Target:  $TARGET_DIR"
    echo "  Output:  $OUTPUT_DIR/$FILENAME.cg.json"
    echo ""
    docker run --rm --user root \
        -v "$TARGET_DIR:/workspace" \
        -v "$OUTPUT_DIR:/output" \
        "$DOCKER_IMAGE" \
        "${args[@]}"
}

# --- Run ---

if [ -f "$JAR_PATH" ] && command -v java &>/dev/null; then
    run_with_java
elif command -v docker &>/dev/null && docker info &>/dev/null; then
    if docker image inspect "$DOCKER_IMAGE" &>/dev/null; then
        run_with_docker
    else
        echo "❌ Docker image not found: $DOCKER_IMAGE"
        echo "   Run setup first: $SKILL_DIR/scripts/setup.sh"
        exit 1
    fi
else
    echo "❌ No runtime available. Run setup first:"
    echo "   $SKILL_DIR/scripts/setup.sh"
    exit 1
fi

echo ""
echo "✅ Analysis complete: $OUTPUT_DIR/$FILENAME.cg.json"
echo ""
echo "Visualize your results:"
echo "  🌐 Web Studio: https://maibornwolff.github.io/DependaCharta/"
echo "     Open your .cg.json file there to explore the dependency graph."
echo "     All processing happens locally in your browser — your data never leaves your machine."
echo ""
echo "  📖 More options: https://github.com/MaibornWolff/DependaCharta#visualize-your-results"
