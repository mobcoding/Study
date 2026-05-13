@echo off
setlocal EnableDelayedExpansion

set "REAL_AAPT2=D:\Android\Sdk\build-tools\36.1.0\aapt2.exe"

set "CAPTURE_DIR=%~dp0capture"
if not exist "%CAPTURE_DIR%" mkdir "%CAPTURE_DIR%"

set "STAMP=%RANDOM%-%RANDOM%-%RANDOM%"
set "LOG_FILE=%CAPTURE_DIR%\aapt2-wrapper.log"

echo [%STAMP%] %*>>"%LOG_FILE%"

set "LAST_ARG="
for %%A in (%*) do set "LAST_ARG=%%~A"

if /I "%1"=="convert" (
  if defined LAST_ARG (
    if exist "!LAST_ARG!" (
      copy /Y "!LAST_ARG!" "%CAPTURE_DIR%\proto-input-%STAMP%.apk" >nul
      echo [%STAMP%] captured !LAST_ARG!>>"%LOG_FILE%"
    ) else (
      echo [%STAMP%] missing input !LAST_ARG!>>"%LOG_FILE%"
    )
  )
)

"%REAL_AAPT2%" %*
set "EXIT_CODE=%ERRORLEVEL%"

if /I "%1"=="convert" (
  if not "%EXIT_CODE%"=="0" (
    echo [%STAMP%] convert failed with exit code %EXIT_CODE%>>"%LOG_FILE%"
  )
)

exit /b %EXIT_CODE%
