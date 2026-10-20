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
$flatlafJar = Join-Path $vendorDir "flatlaf-3.6.1.jar"
$appJar = Join-Path $distDir "aabtool-gui-5.1.jar"
$launcher = Join-Path $distDir "launch-aabtool-gui.bat"
$cliLauncher = Join-Path $distDir "launch-aabtool-cli.bat"
$manifest = Join-Path $buildDir "manifest.mf"

if (-not (Test-Path $bundletoolJar)) {
    throw "Missing bundletool runtime: $bundletoolJar"
}
if (-not (Test-Path $flatlafJar)) {
    throw "Missing theme runtime: $flatlafJar"
}

Remove-Item $buildDir -Recurse -Force -ErrorAction SilentlyContinue

New-Item -ItemType Directory -Force $classesDir | Out-Null
New-Item -ItemType Directory -Force $resourcesDir | Out-Null
New-Item -ItemType Directory -Force $distDir | Out-Null

$javaFiles = Get-ChildItem -Path $srcDir -Recurse -Filter *.java | Select-Object -ExpandProperty FullName
if (-not $javaFiles) {
    throw "No Java sources found under $srcDir"
}

javac --release 17 -encoding UTF-8 -cp $flatlafJar -d $classesDir $javaFiles
if ($LASTEXITCODE -ne 0) { throw "Java compilation failed." }

# Bundle FlatLaf classes, native libraries and license metadata into the standalone JAR.
Push-Location $resourcesDir
try {
    jar xf $flatlafJar
    if ($LASTEXITCODE -ne 0) { throw "Unable to unpack FlatLaf." }
} finally {
    Pop-Location
}

if (Test-Path $resourceSrcDir) {
    Copy-Item (Join-Path $resourceSrcDir "*") $resourcesDir -Recurse -Force
}

$embeddedDir = Join-Path $resourcesDir "embedded"
New-Item -ItemType Directory -Force $embeddedDir | Out-Null
Copy-Item $bundletoolJar (Join-Path $embeddedDir "bundletool-all-1.18.3.jar") -Force

@"
Main-Class: com.codex.aabtoolgui.AabToolGuiApp
Multi-Release: true
"@ | Set-Content -Path $manifest -Encoding ascii

$stagedJar = Join-Path $buildDir "aabtool-gui-5.1.jar"
jar cfm $stagedJar $manifest -C $classesDir . -C $resourcesDir .
if ($LASTEXITCODE -ne 0) { throw "JAR packaging failed." }
# Preserve existing artifacts if packaging fails or a running app locks the release JAR.
Copy-Item -LiteralPath $stagedJar -Destination $appJar -Force

@"
@echo off
setlocal
set SCRIPT_DIR=%~dp0
if defined JAVA_HOME (
  set JAVA_CMD=%JAVA_HOME%\bin\java.exe
) else (
  set JAVA_CMD=java
)
"%JAVA_CMD%" -jar "%SCRIPT_DIR%aabtool-gui-5.1.jar"
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
"%JAVA_CMD%" -jar "%SCRIPT_DIR%aabtool-gui-5.1.jar" %*
"@ | Set-Content -Path $cliLauncher -Encoding ascii

Write-Host "Built:"
Write-Host "  $appJar"
Write-Host "  $launcher"
Write-Host "  $cliLauncher"
