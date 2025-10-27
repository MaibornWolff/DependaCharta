#!/bin/bash

if [ -z "$JAVA_HOME" ]; then
  JAVA_CMD=java
else
  JAVA_CMD="$JAVA_HOME/bin/java"
fi

JAR_PATH="codegraph.jar"

args="$@"

while true; do
  $JAVA_CMD -jar $JAR_PATH $args
  RETURN_CODE=$?
  if [ $RETURN_CODE -eq 134 ]; then
    clear
  # remove --clean or -c from args to not clean the analysis if the jar has to be restarted
    args=$(echo $args | sed 's/--clean//g' | sed 's/-c//g' )
    rm -f ./*.log
  else
    break
  fi
done