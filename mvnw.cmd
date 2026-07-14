@REM Maven Wrapper startup script (Windows)
@ECHO OFF
SETLOCAL
SET "MVNW_DIR=%~dp0"
SET "MAVEN_VERSION=3.9.11"
SET "MAVEN_HOME=%USERPROFILE%\.m2\wrapper\dists\apache-maven-%MAVEN_VERSION%"
SET "MAVEN_CMD=%MAVEN_HOME%\bin\mvn.cmd"
IF EXIST "%MAVEN_CMD%" GOTO run
ECHO Downloading Apache Maven %MAVEN_VERSION%...
IF NOT EXIST "%MAVEN_HOME%" MKDIR "%MAVEN_HOME%"
powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; $zip=Join-Path $env:TEMP 'apache-maven-%MAVEN_VERSION%-bin.zip'; Invoke-WebRequest 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MAVEN_VERSION%/apache-maven-%MAVEN_VERSION%-bin.zip' -OutFile $zip; Expand-Archive $zip -DestinationPath (Split-Path $env:MAVEN_HOME) -Force; Move-Item (Join-Path (Split-Path $env:MAVEN_HOME) 'apache-maven-%MAVEN_VERSION%') $env:MAVEN_HOME -Force; Remove-Item $zip"
IF ERRORLEVEL 1 EXIT /B 1
:run
CALL "%MAVEN_CMD%" %*
EXIT /B %ERRORLEVEL%
