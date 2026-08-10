@echo off
setlocal
set SCRIPT_DIR=%~dp0
if defined JAVA_HOME (
  set JAVA_CMD=%JAVA_HOME%\bin\java.exe
) else (
  set JAVA_CMD=java
)
"%JAVA_CMD%" -jar "%SCRIPT_DIR%aabtool-gui-3.0.jar"
