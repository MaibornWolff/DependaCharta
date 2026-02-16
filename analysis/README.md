# DependaCharta Analysis

This is the analysis component of DependaCharta that analyzes source code and generates dependency graphs.

# Installation

## Prerequisites

**With mise (recommended):** Run `mise install` from the repository root to get the correct Java version automatically.

**Manual:** Java 17 or higher (check with `java -version`)
## Build it yourself
- Clone the repository
- Download the gradle dependencies of the `analysis` project
- Run `./gradlew fatJar` in the `analysis` project
- Run `java -jar analysis/build/libs/dependacharta.jar`

## Use the prebuilt scripts and jar file (Recommended)
- Navigate to `analysis/bin`
- On Windows, run the `dependacharta.bat` script
- On Mac/Linux, run the `dependacharta.sh` script

## Docker Installation (Alternative)
For users who prefer containerized execution:

### Build the Docker image
```bash
# From the analysis directory
docker build -t dependacharta-analysis .

# For multi-platform support (ARM64/AMD64)
docker buildx build --platform linux/amd64,linux/arm64 -t dependacharta-analysis .
```

### Run with Docker
```bash
# Basic usage - analyze a project
docker run --rm -v /path/to/your/project:/workspace dependacharta-analysis -d /workspace

# With custom output directory
docker run --rm -v /path/to/your/project:/workspace -v /path/to/output:/output dependacharta-analysis -d /workspace -o /output

# View help
docker run --rm dependacharta-analysis --help
```

The Docker image includes all necessary dependencies and Tree-sitter parsers, making it ideal for CI/CD pipelines or environments where Java installation is not desired.

# Usage
## CLI Parameters
### Required parameters
- `-d` or `--directory`: The path to the project you want to analyze
### Optional/Default parameters
- `-o` or `--outputDirectory`: The path to the output directory, relative to the current directory (default: `output`)
- `-f` or `--filename`: The output filename without the file extension `.cg.json` (default: `analysis`)
- `-c` or `--clean`: Deletes the temporary directory before starting the analysis, forcing a new analysis (default: `false`)
- `-h` or `--help`: Shows the help message
- `-v` or `--version`: Shows the version of the tool
- `-l` or `--logLevel`: Define the log level for STDOUT. Possible values are debug, info, warn, error, fatal. Independent of this setting the log file will always contain all levels. 
## Attention
- The analysis can take a long time for large projects. If an analysis is stopped midway, you can continue it by running the same command again.
- During the analysis, a directory named `dependacharta_temp` is created in the current directory. This directory is used to store temporary files and will be deleted after the analysis is finished. **Do not delete it during a running analysis!**
## Result
- The result is a `.cg.json` file that can be used in the [visualization](../visualization/README.md) tool
- **Important**: The output file is always named `[filename].cg.json` (note the `.cg.json` extension, not just `.json`)
- It is located in `[outputDirectory]/[filename].cg.json`
- Example: `-f my-analysis -o output` creates `output/my-analysis.cg.json`

# Development

## Building from Source
```bash
cd analysis
./gradlew build              # Build and test
./gradlew build -x test      # Build without tests
./gradlew ktlintFormat       # Auto-format code
```

The build creates two JAR files in `build/libs/`:
- `dependacharta.jar` - Fat JAR (includes all dependencies, ~15MB)
- `dependacharta-analysis.jar` - Thin JAR (no dependencies, ~600KB)

**Always use the fat JAR** (`dependacharta.jar`) for distribution and running the tool. The thin JAR won't work standalone because it's missing the required libraries.

## Updating the Distribution JAR
After making code changes, copy the fat JAR to `bin/`:
```bash
cd analysis
./gradlew clean build -x test
cp build/libs/dependacharta.jar bin/dependacharta.jar
```

## Regenerating Example Files
After modifying the output format, regenerate the example `.cg.json` files:

```bash
# Java example (used by visualization tests)
java -jar analysis/bin/dependacharta.jar \
  -d analysis/src/test/resources/analysis/contract/examples/java \
  -f java-example \
  -o visualization/public/resources \
  -c

# Go example
java -jar analysis/bin/dependacharta.jar \
  -d exampleProjects/GoExample \
  -f go-example \
  -o visualization/public/resources \
  -c

# Test expectations file
java -jar analysis/bin/dependacharta.jar \
  -d analysis/src/test/resources/analysis/contract/examples/java \
  -f java-example \
  -o analysis/src/test/resources/pipeline/projectreport \
  -c
```

**Note**: The `-c` flag clears the temporary analysis cache, forcing a fresh analysis.

## Adding New Fields to Output

When adding a new field to the `.cg.json` output:

1. **Update data model** in `ProjectReportDto.kt`
2. **Configure JSON serialization** in `ExportService.kt`:
   ```kotlin
   private val json = Json {
       prettyPrint = true
       encodeDefaults = true  // Required for fields with default values
   }
   ```
3. **Update calculation logic** where edges/nodes are created
4. **Update tests** and regenerate test expectations
5. **Rebuild JAR** and regenerate example files (see above)

## Common Issues

### Missing Fields in JSON Output
- Ensure `encodeDefaults = true` in `ExportService.kt`
- Check that fields don't have default values matching the type's default

### Build Fails with ktlint Errors
```bash
./gradlew ktlintFormat  # Auto-fix formatting
```

### Tool Fails with ClassNotFoundException
- You're using the thin JAR (`dependacharta-analysis.jar`) which doesn't include dependencies
- Use the fat JAR (`dependacharta.jar`) instead

# Extend the supported programming languages
The programming language of your project is not yet supported but you would still like to analyze it?  
You can try adding your own language parser, we documented how to do that [here](howto-add-new-language.md)

# Package Strucure and Dataflow
See [ADR Log](../doc/architecture/decisions)

# Processing steps
1) Resolve dependencies between the single files of the project
2) Find cycles (see [Cycle Detection Algorithm](src/main/kotlin/de/maibornwolff/dependacharta/pipeline/processing/cycledetection/README.md))
3) Levelize the dependency graph according to the [Levelized Structure Map](https://structure101.com/help/cpa/studio/Content/restructure101/lsm.html) (see [Levelization Algorithm](src/main/kotlin/de/maibornwolff/dependacharta/pipeline/processing/levelization/README.md))
4) Generate the final `cg.json` output file
