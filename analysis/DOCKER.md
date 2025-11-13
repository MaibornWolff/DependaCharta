# Docker Usage Guide

DependaCharta Analysis is available as a Docker image for easy deployment and CI/CD integration.

## Quick Start

### Using Pre-built Images

Pre-built images are automatically published to GitHub Container Registry on every release and main branch push.

```bash
# Pull the latest image
docker pull ghcr.io/maibornwolff/dependacharta-analysis:latest

# Run analysis on your project
docker run --rm --user root \
  -v "$(pwd)/your-project:/workspace" \
  ghcr.io/maibornwolff/dependacharta-analysis:latest \
  -d /workspace

# Output will be in your-project/output/analysis.cg.json
```

### Available Tags

- `latest` - Latest release version
- `v*.*.*` - Specific release versions (e.g., `1.2.3`)
- `1.2` - Major.minor version (auto-updated with patch releases)
- `1` - Major version (auto-updated with minor/patch releases)

```bash
# Use the latest release
docker pull ghcr.io/maibornwolff/dependacharta-analysis:latest

# Use a specific version
docker pull ghcr.io/maibornwolff/dependacharta-analysis:1.2.3

# Use a major.minor version (gets patch updates)
docker pull ghcr.io/maibornwolff/dependacharta-analysis:1.2

# Use a major version (gets all updates within v1.x.x)
docker pull ghcr.io/maibornwolff/dependacharta-analysis:1
```

## Usage Examples

### Basic Analysis

```bash
docker run --rm --user root \
  -v "$(pwd)/my-project:/workspace" \
  ghcr.io/maibornwolff/dependacharta-analysis:latest \
  -d /workspace
```

### Custom Output Directory

```bash
docker run --rm --user root \
  -v "$(pwd)/my-project:/workspace" \
  -v "$(pwd)/analysis-output:/output" \
  ghcr.io/maibornwolff/dependacharta-analysis:latest \
  -d /workspace -o /output
```

### With Custom Options

```bash
docker run --rm --user root \
  -v "$(pwd)/my-project:/workspace" \
  ghcr.io/maibornwolff/dependacharta-analysis:latest \
  -d /workspace \
  -f my-analysis.cg.json \
  -l debug
```

## CI/CD Integration

### GitHub Actions

```yaml
- name: Analyze codebase
  run: |
    docker pull ghcr.io/maibornwolff/dependacharta-analysis:latest
    docker run --rm --user root \
      -v "${{ github.workspace }}:/workspace" \
      ghcr.io/maibornwolff/dependacharta-analysis:latest \
      -d /workspace

- name: Upload analysis results
  uses: actions/upload-artifact@v4
  with:
    name: dependency-analysis
    path: output/analysis.cg.json
```

### GitLab CI

```yaml
analyze:
  image: ghcr.io/maibornwolff/dependacharta-analysis:latest
  script:
    - java -jar /app/dependacharta.jar -d $CI_PROJECT_DIR
  artifacts:
    paths:
      - output/analysis.cg.json
```

### Jenkins

```groovy
pipeline {
    agent any
    stages {
        stage('Analyze') {
            steps {
                sh '''
                    docker pull ghcr.io/maibornwolff/dependacharta-analysis:latest
                    docker run --rm --user root \
                      -v "${WORKSPACE}:/workspace" \
                      ghcr.io/maibornwolff/dependacharta-analysis:latest \
                      -d /workspace
                '''
            }
        }
        stage('Archive') {
            steps {
                archiveArtifacts artifacts: 'output/analysis.cg.json'
            }
        }
    }
}
```

## Building the Image Locally

If you need to build the image yourself:

```bash
cd analysis

# Build the image
docker build -t dependacharta-analysis:local .

# Run with local image
docker run --rm --user root \
  -v "$(pwd)/your-project:/workspace" \
  dependacharta-analysis:local \
  -d /workspace
```

## Troubleshooting

### Permission Issues

On macOS and Windows, Docker may have permission issues writing to mounted volumes. Use `--user root` to avoid these issues:

```bash
docker run --rm --user root \
  -v "$(pwd)/project:/workspace" \
  ghcr.io/maibornwolff/dependacharta-analysis:latest \
  -d /workspace
```

### Output Directory Not Found

The analysis tool doesn't create the output directory automatically. Make sure it exists:

```bash
mkdir -p output
docker run --rm --user root \
  -v "$(pwd)/project:/workspace" \
  ghcr.io/maibornwolff/dependacharta-analysis:latest \
  -d /workspace -o /workspace/output
```

### View All Available Options

```bash
docker run --rm ghcr.io/maibornwolff/dependacharta-analysis:latest --help
```

## Image Details

- **Base Image**: Eclipse Temurin JRE 17
- **Size**: ~400MB (compressed)
- **Architecture**: linux/amd64
- **User**: Runs as non-root user `dependacharta` (UID 1001) by default
- **Working Directory**: `/workspace`
- **Entrypoint**: `java -jar /app/dependacharta.jar`

## Security

The Docker image:
- Uses official Eclipse Temurin JRE base image
- Runs as non-root user by default for security
- Removes build tools after compilation to minimize attack surface
- Includes build provenance attestation
- Is automatically scanned for vulnerabilities in CI/CD

## Support

For issues or questions:
- [Open an issue](https://github.com/MaibornWolff/DependaCharta/issues)
- [View documentation](https://github.com/MaibornWolff/DependaCharta)
