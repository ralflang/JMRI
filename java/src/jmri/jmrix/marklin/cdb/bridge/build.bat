@echo off
REM Build script for Märklin CC-Schnitte Serial-to-TCP Bridge (Windows)

echo Building Märklin CC-Schnitte Serial-to-TCP Bridge...
echo.

REM Check if JMRI_HOME is set
if "%JMRI_HOME%"=="" (
    set "JMRI_HOME=%~dp0..\..\..\..\..\.."
    echo JMRI_HOME not set, using: %JMRI_HOME%
)

cd /d "%JMRI_HOME%"

REM Create output directory
if not exist target\bridge mkdir target\bridge

REM Find required JARs
for %%f in (lib\jSerialComm-*.jar) do set "JSERIALCOMM=%%f"
for %%f in (lib\slf4j-api-*.jar) do set "SLF4J_API=%%f"
for %%f in (lib\slf4j-simple-*.jar) do set "SLF4J_SIMPLE=%%f"

if "%JSERIALCOMM%"=="" (
    echo Error: jSerialComm library not found in lib/
    exit /b 1
)
if "%SLF4J_API%"=="" (
    echo Error: slf4j-api library not found in lib/
    exit /b 1
)
if "%SLF4J_SIMPLE%"=="" (
    echo Error: slf4j-simple library not found in lib/
    exit /b 1
)

echo Using libraries:
echo   %JSERIALCOMM%
echo   %SLF4J_API%
echo   %SLF4J_SIMPLE%
echo.

REM Compile
echo Compiling...
javac -d target\bridge ^
      -cp "%JSERIALCOMM%;%SLF4J_API%;%SLF4J_SIMPLE%" ^
      -source 11 -target 11 ^
      java\src\jmri\jmrix\marklin\cdb\bridge\CdbSerialToTcpBridge.java

if errorlevel 1 (
    echo Compilation failed
    exit /b 1
)

REM Create JAR
echo Creating JAR...
cd target\bridge
jar cfe ..\..\marklin-cdb-bridge.jar ^
    jmri.jmrix.marklin.cdb.bridge.CdbSerialToTcpBridge ^
    jmri\jmrix\marklin\cdb\bridge\*.class

cd ..\..

REM Copy dependencies
echo Copying dependencies...
copy "%JSERIALCOMM%" . >nul
copy "%SLF4J_API%" . >nul
copy "%SLF4J_SIMPLE%" . >nul

echo.
echo Build complete!
echo.
echo Output:
echo   marklin-cdb-bridge.jar
dir /b marklin-cdb-bridge.jar 2>nul
echo.
echo Dependencies:
dir /b jSerialComm-*.jar slf4j-*.jar 2>nul
echo.
echo Usage:
echo   java -jar marklin-cdb-bridge.jar --port COM3
echo   java -jar marklin-cdb-bridge.jar --help
