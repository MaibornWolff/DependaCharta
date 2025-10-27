# Multi-stage Dockerfile for CodeGraph Analysis
# Stage 1: Build the application
FROM gradle:8.14-jdk17 AS builder

# Set working directory
WORKDIR /app

# Copy gradle files first for better caching
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY gradle/ ./gradle/

# Download dependencies (will be cached if gradle files don't change)
RUN gradle dependencies --no-daemon

# Copy source code
COPY src/ ./src/

# Build the fat JAR
RUN gradle fatJar --no-daemon

# Stage 2: Create minimal runtime image
FROM eclipse-temurin:17.0.16_8-jre

# Install build tools needed for Tree-sitter native libraries and create non-root user
RUN apt-get update && apt-get install -y \
    build-essential \
    git \
    && groupadd -g 1001 codegraph \
    && useradd -r -u 1001 -g codegraph codegraph \
    && mkdir -p /home/codegraph/.tree-sitter \
    && chown -R codegraph:codegraph /home/codegraph \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /app

# Copy the fat JAR from builder stage
COPY --from=builder /app/build/libs/codegraph.jar ./codegraph.jar

# Change ownership to non-root user
RUN chown -R codegraph:codegraph /app

# Switch to non-root user to initialize Tree-sitter libraries
USER codegraph

# Pre-initialize Tree-sitter libraries by running a quick help command
# This will trigger the native library compilation during build time
RUN timeout 30 java -jar /app/codegraph.jar --help || true

# Switch back to root to clean up build tools (optional, for smaller image)
USER root
RUN apt-get remove -y build-essential git && apt-get autoremove -y && apt-get clean

# Switch back to non-root user for runtime
USER codegraph

# Expose volume for input/output
VOLUME ["/workspace"]

# Set default working directory for analysis
WORKDIR /workspace

# Set entrypoint
ENTRYPOINT ["java", "-jar", "/app/codegraph.jar"]

# Default command (can be overridden)
CMD ["--help"]