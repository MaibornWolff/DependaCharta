# Installation
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
- It is located in `[outputDirectory]/[filename].cg.json`

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
