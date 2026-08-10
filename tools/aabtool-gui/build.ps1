$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$srcDir = Join-Path $root "src\main\java"
$resourceSrcDir = Join-Path $root "src\main\resources"
$buildDir = Join-Path $root "build"
$classesDir = Join-Path $buildDir "classes"
$resourcesDir = Join-Path $buildDir "resources"
$distDir = Join-Path $root "dist"
$vendorDir = Join-Path $root "vendor"
$bundletoolJar = Join-Path $vendorDir "bundletool-all-1.18.3.jar"
$appJar = Join-Path $distDir "aabtool-gui-3.0.jar"
$launcher = Join-Path $distDir "launch-aabtool-gui.bat"
$cliLauncher = Join-Path $distDir "launch-aabtool-cli.bat"
$manifest = Join-Path $buildDir "manifest.mf"

if (-not (Test-Path $bundletoolJar)) {
    throw "Missing bundletool runtime: $bundletoolJar"
}

Remove-Item $buildDir -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item $distDir -Recurse -Force -ErrorAction SilentlyContinue

New-Item -ItemType Directory -Force $classesDir | Out-Null
New-Item -ItemType Directory -Force $resourcesDir | Out-Null
New-Item -ItemType Directory -Force $distDir | Out-Null

$javaFiles = Get-ChildItem -Path $srcDir -Recurse -Filter *.java | Select-Object -ExpandProperty FullName
if (-not $javaFiles) {
    throw "No Java sources found under $srcDir"
}

javac --release 17 -encoding UTF-8 -d $classesDir $javaFiles

if (Test-Path $resourceSrcDir) {
    Copy-Item (Join-Path $resourceSrcDir "*") $resourcesDir -Recurse -Force
}

$embeddedDir = Join-Path $resourcesDir "embedded"
New-Item -ItemType Directory -Force $embeddedDir | Out-Null
Copy-Item $bundletoolJar (Join-Path $embeddedDir "bundletool-all-1.18.3.jar") -Force

@"
Main-Class: com.codex.aabtoolgui.AabToolGuiApp
"@ | Set-Content -Path $manifest -Encoding ascii

jar cfm $appJar $manifest -C $classesDir . -C $resourcesDir .

@"
@echo off
setlocal
set SCRIPT_DIR=%~dp0
if defined JAVA_HOME (
  set JAVA_CMD=%JAVA_HOME%\bin\java.exe
) else (
  set JAVA_CMD=java
)
"%JAVA_CMD%" -jar "%SCRIPT_DIR%aabtool-gui-3.0.jar"
"@ | Set-Content -Path $launcher -Encoding ascii

@"
@echo off
setlocal
set SCRIPT_DIR=%~dp0
if defined JAVA_HOME (
  set JAVA_CMD=%JAVA_HOME%\bin\java.exe
) else (
  set JAVA_CMD=java
)
"%JAVA_CMD%" -jar "%SCRIPT_DIR%aabtool-gui-3.0.jar" %*
"@ | Set-Content -Path $cliLauncher -Encoding ascii

Write-Host "Built:"
Write-Host "  $appJar"
Write-Host "  $launcher"
Write-Host "  $cliLauncher"
