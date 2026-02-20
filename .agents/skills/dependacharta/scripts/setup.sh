#!/usr/bin/env bash
set -euo pipefail

SKILL_DIR="$(cd "$(dirname "$0")/.." && pwd)"
BIN_DIR="$SKILL_DIR/bin"
JAR_PATH="$BIN_DIR/dependacharta.jar"
VERSION_FILE="$BIN_DIR/.version"
GITHUB_REPO="MaibornWolff/DependaCharta"
DOCKER_IMAGE="maibornwolff/dependacharta-analysis:latest"

# --- Helper functions ---

check_java() {
    if ! command -v java &>/dev/null; then
        return 1
    fi
    local version
    version=$(java -version 2>&1 | grep -i 'version' | head -1 | sed -E 's/.*"([0-9]+).*/\1/')
    [ "$version" -ge 17 ] 2>/dev/null
}

check_docker() {
    command -v docker &>/dev/null && docker info &>/dev/null
}

check_jar_cached() {
    [ -f "$JAR_PATH" ] && [ -f "$VERSION_FILE" ]
}

detect_runtime() {
    if check_jar_cached && check_java; then
        echo "java-cached"
    elif check_java; then
        echo "java"
    elif check_docker; then
        echo "docker"
    else
        echo "none"
    fi
}

# --- Status check ---

if [ "${1:-}" = "--check" ]; then
    runtime=$(detect_runtime)
    case "$runtime" in
        java-cached)
            version=$(cat "$VERSION_FILE")
            echo "✅ Ready: Java runtime with cached JAR (version: $version)"
            echo "   JAR: $JAR_PATH"
            ;;
        java)
            echo "⚠️  Java 17+ found but JAR not downloaded yet. Run setup without --check."
            ;;
        docker)
            if docker image inspect "$DOCKER_IMAGE" &>/dev/null; then
                echo "Ready: Docker image already pulled ($DOCKER_IMAGE)"
            else
                echo "Docker available but image not pulled yet. Run setup without --check."
            fi
            ;;
        none)
            echo "❌ Neither Java 17+ nor Docker found."
            ;;
    esac
    exit 0
fi

# --- Setup ---

echo "DependaCharta Analysis - Setup"
echo "=============================="
echo ""

runtime=$(detect_runtime)

case "$runtime" in
    java-cached)
        version=$(cat "$VERSION_FILE")
        echo "✅ Already set up: JAR cached at $JAR_PATH (version: $version)"
        echo "   To force re-download, delete $BIN_DIR and run setup again."
        exit 0
        ;;

    java)
        echo "Found Java 17+. Downloading DependaCharta JAR from GitHub..."
        echo ""

        # Get latest release info
        release_json=$(curl -sL "https://api.github.com/repos/$GITHUB_REPO/releases/latest")

        # Parse JSON (jq preferred, sed fallback)
        if command -v jq &>/dev/null; then
            tag=$(echo "$release_json" | jq -r '.tag_name // empty')
            asset_url=$(echo "$release_json" | jq -r '[.assets[].browser_download_url | select(test("dependacharta-analysis-"))] | first // empty')
        else
            tag=$(echo "$release_json" | grep '"tag_name"' | head -1 | sed -E 's/.*"tag_name":\s*"([^"]+)".*/\1/')
            asset_url=$(echo "$release_json" | grep '"browser_download_url"' | grep 'dependacharta-analysis-' | head -1 | sed -E 's/.*"browser_download_url":\s*"([^"]+)".*/\1/')
        fi

        if [ -z "$tag" ]; then
            echo "❌ Failed to fetch latest release from GitHub."
            echo "   Check your network connection or visit:"
            echo "   https://github.com/$GITHUB_REPO/releases"
            exit 1
        fi

        echo "Latest release: $tag"

        if [ -z "$asset_url" ]; then
            echo "❌ Could not find analysis zip in release $tag."
            echo "   Visit: https://github.com/$GITHUB_REPO/releases/tag/$tag"
            exit 1
        fi

        echo "Downloading: $asset_url"

        # Download and extract
        mkdir -p "$BIN_DIR"
        tmp_dir=$(mktemp -d)
        trap 'rm -rf "$tmp_dir"' EXIT

        curl -sL -o "$tmp_dir/analysis.zip" "$asset_url"

        # Extract zip (try unzip, fall back to python3)
        if command -v unzip &>/dev/null; then
            unzip -q -o "$tmp_dir/analysis.zip" -d "$tmp_dir/extracted"
        elif command -v python3 &>/dev/null; then
            python3 -c "import zipfile; zipfile.ZipFile('$tmp_dir/analysis.zip').extractall('$tmp_dir/extracted')"
        else
            echo "❌ Neither unzip nor python3 available to extract the archive."
            exit 1
        fi

        # Find the JAR in the extracted contents
        jar_file=$(find "$tmp_dir/extracted" -name "dependacharta.jar" -type f | head -1)

        if [ -z "$jar_file" ]; then
            echo "❌ Could not find dependacharta.jar in the downloaded archive."
            exit 1
        fi

        cp "$jar_file" "$JAR_PATH"
        echo "$tag" > "$VERSION_FILE"

        echo ""
        echo "✅ Setup complete. JAR cached at: $JAR_PATH (version: $tag)"
        ;;

    docker)
        echo "No Java 17+ found, but Docker is available."
        echo "Pulling Docker image: $DOCKER_IMAGE"
        echo ""

        docker pull "$DOCKER_IMAGE"

        echo ""
        echo "✅ Setup complete. Using Docker image: $DOCKER_IMAGE"
        ;;

    none)
        echo "❌ Neither Java 17+ nor Docker is available."
        echo ""
        echo "Options:"
        echo "  1. Install Java 17+ (recommended)"
        echo "     - macOS: brew install openjdk@17"
        echo "     - Ubuntu/Debian: sudo apt install openjdk-17-jre"
        echo "     - Windows: winget install EclipseAdoptium.Temurin.17.JRE"
        echo ""
        echo "  2. Install Docker"
        echo "     - https://docs.docker.com/get-docker/"
        echo ""
        exit 1
        ;;
esac
