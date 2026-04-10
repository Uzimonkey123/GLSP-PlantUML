@echo off
setlocal

set TARGET_JAR=target\GLSPPlantUML-1.0-SNAPSHOT.jar
set SERVER_DIR=plantuml-client\server
set CLIENT_DIR=plantuml-client

if "%1"=="-h" goto :help
if "%1"=="--help" goto :help
if "%1"=="-s" goto :skip
if "%1"=="-c" goto :client
if "%1"=="-f" goto :full
if "%1"=="-p" goto :package

echo [1/2] Building server (with tests)
call mvn clean package
if %ERRORLEVEL% neq 0 ( echo Maven build failed! & exit /b 1 )
goto :copy

:skip
echo [1/2] Building server (skip tests)
call mvn clean package -DskipTests
if %ERRORLEVEL% neq 0 ( echo Maven build failed! & exit /b 1 )
goto :copy

:client
echo [1/2] Building server (with tests)
call mvn clean package
if %ERRORLEVEL% neq 0 ( echo Maven build failed! & exit /b 1 )
goto :copyclient

:full
echo [1/2] Building server (skip tests)
call mvn clean package -DskipTests
if %ERRORLEVEL% neq 0 ( echo Maven build failed! & exit /b 1 )
goto :copyclient

:package
echo [1/4] Building server (skip tests)
call mvn clean package -DskipTests
if %ERRORLEVEL% neq 0 ( echo Maven build failed! & exit /b 1 )
echo [2/4] Copying JAR to %SERVER_DIR%
copy /Y "%TARGET_JAR%" "%SERVER_DIR%\" >nul
echo JAR copied.
echo [3/4] Building client (npm run build:all)
pushd %CLIENT_DIR%
call npm run build:all
if %ERRORLEVEL% neq 0 ( popd & echo Client build failed! & exit /b 1 )
echo [4/4] Packaging VSIX
call npx vsce package --no-dependencies
if %ERRORLEVEL% neq 0 ( popd & echo VSIX packaging failed! & exit /b 1 )
popd
echo VSIX packaged.
echo Done.
exit /b 0

:copy
echo [2/2] Copying JAR to %SERVER_DIR%
copy /Y "%TARGET_JAR%" "%SERVER_DIR%\" >nul
echo JAR copied.
echo Done.
exit /b 0

:copyclient
echo [2/3] Copying JAR to %SERVER_DIR%
copy /Y "%TARGET_JAR%" "%SERVER_DIR%\" >nul
echo JAR copied.
echo [3/3] Building client (npm run build:all)
pushd %CLIENT_DIR%
call npm run build:all
if %ERRORLEVEL% neq 0 ( popd & echo Client build failed! & exit /b 1 )
popd
echo Client built.
echo Done.
exit /b 0

:help
echo.
echo Usage: build.bat [option]
echo.
echo Options:
echo   (no flag)   Maven build with tests, copy JAR
echo   -s          Maven build without tests, copy JAR
echo   -c          Maven build + npm run build:all
echo   -f          Maven build + npm run build:all (skip tests)
echo   -p          Full build (skip tests) + npm build + vsce package
echo   -h          Show this help
echo.
exit /b 0