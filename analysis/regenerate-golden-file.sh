#!/bin/bash
set -e

echo "Regenerating golden file for ProcessingPipelineTest..."

# Temporarily disable the tearDown method to prevent cleanup
TEST_FILE="src/test/kotlin/de/maibornwolff/dependacharta/pipeline/processing/ProcessingPipelineTest.kt"
BACKUP_FILE="${TEST_FILE}.backup"

# Backup the original test file
cp "$TEST_FILE" "$BACKUP_FILE"

# Comment out the entire tearDown method
sed -i '/^    @AfterEach$/,/^    }$/ s/^/\/\//' "$TEST_FILE"

# Run the test to generate the output (it will fail on assertion, but that's OK)
echo "Running test to generate output..."
./gradlew test --tests "ProcessingPipelineTest.processes Java Project correctly" > /dev/null 2>&1 || true

# Restore the original test file
mv "$BACKUP_FILE" "$TEST_FILE"

# Check if the output file was generated
if [ ! -f "testresult/test.cg.json" ]; then
    echo "Error: testresult/test.cg.json was not generated"
    echo "The test may have failed before generating output."
    exit 1
fi

# Copy the generated file to the golden file location
cp testresult/test.cg.json src/test/resources/pipeline/projectreport/java-example.cg.json

# Clean up
rm -rf testresult

echo "Golden file regenerated successfully!"
echo "Location: src/test/resources/pipeline/projectreport/java-example.cg.json"