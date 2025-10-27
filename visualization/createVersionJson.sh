#!/bin/bash
set -e

if [ $# -eq 0 ]; then
  echo "No argument provided"
  exit 1
fi

targetDir=$1
version=$2
# shellcheck disable=SC2089
jsonContent="{ \"version\": \"$version\" }"

echo "$jsonContent" > $targetDir/version.json
echo "Version Json file created with content: $jsonContent"
