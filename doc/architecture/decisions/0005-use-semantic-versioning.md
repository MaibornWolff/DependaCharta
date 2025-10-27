# 5. use semantic versioning

Date: 2025-09-02

## Status

Accepted

## Context

A strategy for versioning the tools and managing the version numbers is necessary.

## Decision

The analysis tool uses the [Semantic Versioning](https://semver.org) scheme.
The version is stored in the `version.json` file and is written by the Gradle build script generateVersionJson.  
The strategy for setting the version is to let it get set by the CI/CD pipeline, by giving the version as a parameter to the Gradle build script. With this, the structure of the analysis component is hidden from the pipeline but the pipeline can still set the version.  

The visualization component uses the [Semantic Versioning](https://semver.org) scheme.
The version is stored in the `version.json` file and is written by the shell script, which can be accessed during the build via the node alias `ci-createVersion`, defined in the `package.json` file.  
The strategy for setting the version is to let it get set by the CI/CD pipeline, by giving the version as a parameter to node build script. With this, the structure of the visualization component is hidden from the pipeline but the pipeline can still set the version.  

For more information how the version is set, see the [Pipeline](../../../Pipeline.md) documentation.


## Consequences

Semantic Versioning gives us meaningful and distinct version numbers