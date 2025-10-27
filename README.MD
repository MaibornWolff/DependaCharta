# TODO

We are currently moving everything from our intern Gitlab to GH. 

## Open Migration Steps

- [ ] Add pipeline
- [ ] Add sonar analysis
- [ ] Rename everything from CodeGraph to DependaCharta
- [ ] Fix Urls
- [ ] Add Pictures inside the issues


# Status 

| Visualization | Analysis |
|---------------|----------|
[![Quality Gate Status](https://codeanalysis.maibornwolff.de/api/project_badges/measure?project=Codegraph-Visualization&metric=alert_status)](https://codeanalysis.maibornwolff.de/dashboard?id=Codegraph-Visualization)| [![Quality Gate Status](https://codeanalysis.maibornwolff.de/api/project_badges/measure?project=Codegraph-Analysis&metric=alert_status)](https://codeanalysis.maibornwolff.de/dashboard?id=Codegraph-Analysis)|
|[![Coverage](https://codeanalysis.maibornwolff.de/api/project_badges/measure?project=Codegraph-Visualization&metric=coverage)](https://codeanalysis.maibornwolff.de/dashboard?id=Codegraph-Visualization)|[![Coverage](https://codeanalysis.maibornwolff.de/api/project_badges/measure?project=Codegraph-Analysis&metric=coverage)](https://codeanalysis.maibornwolff.de/dashboard?id=Codegraph-Analysis)|

# CodeGraph

CodeGraph is a tool to help quickly understand structure and architecture of codebases, by analyzing and identifying disadvantageous dependencies and quality hotspots.  
It runs locally, can handle non-compilable code and supports analyzing multiple programming languages.  
It consists of two components: an analysis component and a visualization component.
The analysis component is written in Kotlin and uses the library [Tree-sitter](https://tree-sitter.github.io/tree-sitter/).  
The visualization component is based on the [Cytoscape](https://js.cytoscape.org/) library and is written in Angular/TypeScript.

Releases can be downloaded under https://NEW-GITHUB-URL/-/releases.

## Overview of linked Documentation
All following linked documents are also linked in the respective sections.  
For a better overview, they are listed here as well.
### General
- [A log of Architecture Decision Records](doc/architecture/decisions)
- [Pipeline](Pipeline.md)

### Analysis
- [Analysis Readme](analysis/README.md)
- [How to add a new language](analysis/howto-add-new-language.md)
- [Cycle Detection Algorithm](analysis/src/main/kotlin/de/maibornwolff/codegraph/pipeline/processing/cycledetection/README.md)
- [Levelization Algorithm](analysis/src/main/kotlin/de/maibornwolff/codegraph/pipeline/processing/levelization/README.md)

### Visualization
- [Visualization Readme](visualization/README.md)


## Features/Components

### CLI Tool (Analysis)
Our command line tool (Analysis) enables you to analyze codebases and creates cg.json files containing structural information about your project. This among other things includes the hierarchy of files and packages, the dependencies of each file and package and information about whether the dependencies between files are cyclic.  
You can analyze multiple programming languages and in case your programming language is not supported, you can try to [extend the supported languages of CodeGraph](analysis/howto-add-new-language.md).  
Take a look at the [analysis](analysis/README.md) for instructions on how to set everything up to create a cg.json.

### Web App (Visualization)
Our Web App (Visualization) allows you to navigate and visualize dependencies and hierarchies inside your codebase. There are a variety of filters to display potentially critical dependencies inside your code. 
With the help of that, you can find quality hotspots and potential architecture violations to help you identify measures to resolve those problems.  
To get started with the Web App, you first need to create a cg.json file and upload it.
You can find more details about usage and installation instructions for our frontend in the [visualization](visualization/README.md).

## Quick Start with Just

CodeGraph includes a `justfile` for convenient development workflows:

```bash
# Run unit tests
just test

# Build the analysis tool
just build

# Run analysis on a directory (builds first if needed)
just run <directory-to-analyze>

# Analyze a directory and starts the frontend directly defaulting to this analysis
just analyze <directory-to-analyze>

# Start frontend development server (installs deps automatically)
just frontend

# Docker commands
just docker-build                    # Build Docker image for analyzer
just docker-build-multi              # Build for multiple platforms
just docker-run <directory-to-analyze>    # Run analysis with Docker
just docker-analyze <directory-to-analyze> # Docker analysis + frontend
```

**Note**: The `analyze` and `docker-analyze` commands output directly to the visualization directory, making it easy to analyze code and immediately view results in the frontend.

## Development
The team decided to not strangle the development workflow with to many rules.  
However, we follow the Clean Code principles but it isn't a dogma. We try to keep the codebase clean and maintainable with common sense.  
Tests are written before the actual implementation and we try to keep the test coverage as high as it makes sense.

### Versioning
In general we use the [Semantic Versioning](https://semver.org) scheme.  
For CodeGraph we increase the patch version for bug fixes, the minor version for new features and the major version for reached milestones or breaking changes.

### CI/CD
A more detailed description of the CI/CD pipeline can be found in the [Pipeline](Pipeline.md) documentation.
