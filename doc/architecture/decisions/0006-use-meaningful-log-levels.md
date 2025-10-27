# 6. Use meaningful log levels

Date: 2025-09-11

## Status

Accepted

Involved: Adrian Endrich

## Context

The logging system in this tool serves multiple distinct purposes, each requiring different approaches and levels of detail.

**User Experience**: End users need visibility into the running process to ensure it hasn't stalled and to understand current 
operations. Progress indicators and estimated completion times are essential. Visual cues like color coding help users 
quickly identify issues that require attention.

**Development and Debugging**: Given the complexity and variety of code analyses performed, comprehensive logging is crucial 
for development workflows. Detailed logs provide essential insights into performance bottlenecks, failure points, and 
internal system behavior during feature development and troubleshooting.

**Long-term Diagnostics**: Due to potentially long-running analysis processes, console output alone is insufficient. 
Persistent file-based logging is necessary for post-execution analysis and debugging of complex scenarios.

## Decision

We implement a hierarchical logging system using standard log levels to address the varying information needs of 
different stakeholders.

**Console Logging**: Users can configure the console output verbosity by selecting an appropriate log level. 
The default configuration INFO provides progress updates and essential information without overwhelming technical details.

**File Logging**: Regardless of console log level settings, the system maintains comprehensive file logs at 
maximum detail level. This ensures complete diagnostic information is always available for troubleshooting and analysis.

**Thread Safety**: The logging implementation is designed to be thread-safe from the ground up, supporting future 
parallelization efforts without requiring additional synchronization considerations.

### Log Level Definitions

- DEBUG: Detailed diagnostic information. Internal state changes, algorithm steps, performance metrics. Only relevant during development and deep troubleshooting.
- INFO: General informational messages. Process milestones, configuration details, normal operation status. Default level for end users.
- WARN: Potentially problematic situations. Recoverable errors, deprecated usage, performance concerns, warnings about potential problems in a lower log level. Situations that don't stop execution but merit attention.
- ERROR: Error conditions. Failed operations, invalid inputs, recoverable failures. Issues that prevent specific operations but don't crash the application.
- FATAL: Critical failures. System-level failures, unrecoverable errors that terminate execution. Reserved for situations requiring immediate attention.

## Consequences

### Positive Outcomes

**Clear Separation of Concerns**: Different user types (end users vs. developers) can access appropriate levels of detail without configuration complexity.

**Robust Diagnostics**: Complete file logging ensures no diagnostic information is lost, regardless of console settings.

**Future-Proof Architecture**: Thread-safe design eliminates concerns about logging in parallel processing scenarios.

**Improved User Experience**: Default settings provide meaningful feedback without information overload, while power users can access detailed information when needed.

### Considerations

**File Storage**: Comprehensive logging may result in large log files for extensive analysis runs.

**Performance**: Debug-level logging may introduce minor performance overhead, though this is mitigated by conditional logging based on active levels.

**Learning Curve**: Developers must understand appropriate log level selection to maintain consistent logging standards across the codebase.
