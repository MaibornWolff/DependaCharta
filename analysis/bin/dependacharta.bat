@echo off
setlocal enabledelayedexpansion

if "%JAVA_HOME%"=="" (
  set JAVA_CMD=java --enable-native-access=ALL-UNNAMED
) else (
  set JAVA_CMD="%JAVA_HOME%\bin\java" --enable-native-access=ALL-UNNAMED
)

set JAR_PATH=dependacharta.jar

set ARGS=%*

:whileLoop
%JAVA_CMD% -jar %JAR_PATH% %ARGS%
if ERRORLEVEL 1 (
    if exist hs_err* (
        cls
        REM remove --clean and -c arguments to not clean the analysis if the jar has to be restarted
        set ARGS=%ARGS:--clean=-c%
        set ARGS=%ARGS:-c=%
        del /Q ".\*.log"
        goto :whileLoop
    )
)
