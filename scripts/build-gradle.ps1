param(
  [switch]$Clean,
  [switch]$Debug,
  [switch]$Daemon
)

$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $PSScriptRoot
$LogDir = Join-Path $Root "dist"
$LogFile = Join-Path $LogDir "gradle-build.log"
$Gradle = Join-Path $Root "gradlew.bat"
$LocalProperties = Join-Path $Root "local.properties"

New-Item -ItemType Directory -Force $LogDir | Out-Null

if (!(Test-Path $LocalProperties) -and $env:LOCALAPPDATA) {
  $SdkDir = Join-Path $env:LOCALAPPDATA "Android\Sdk"
  if (Test-Path $SdkDir) {
    $PortableSdkDir = $SdkDir -replace "\\", "/"
    "sdk.dir=$PortableSdkDir" | Set-Content -Path $LocalProperties -Encoding ASCII
  }
}

Push-Location $Root
try {
  Write-Host "Stopping existing Gradle daemons..."
  & $Gradle --stop

  $tasks = @()
  if ($Clean) {
    $tasks += "clean"
  }
  $tasks += @(":phone:assembleDebug", ":wear:assembleDebug", "copyDebugApks")

  $logLevel = if ($Debug) { "--debug" } else { "--info" }
  $daemonMode = if ($Daemon) { "--daemon" } else { "--no-daemon" }
  $arguments = @()
  $arguments += $tasks
  $arguments += @("--build-cache", "--parallel", $daemonMode, $logLevel, "--console=plain")

  "Corner Call Gradle build started: $(Get-Date -Format o)" | Set-Content -Path $LogFile -Encoding UTF8
  "Command: $Gradle $($arguments -join ' ')" | Add-Content -Path $LogFile -Encoding UTF8
  "" | Add-Content -Path $LogFile -Encoding UTF8

  Write-Host "Writing live build log to $LogFile"
  Write-Host "Tail it with: powershell -ExecutionPolicy Bypass -File scripts/tail-gradle-log.ps1"

  $previousErrorActionPreference = $ErrorActionPreference
  $ErrorActionPreference = "Continue"
  $sawBuildFailure = $false
  try {
    & $Gradle @arguments 2>&1 |
      ForEach-Object {
        $line = $_.ToString()
        if ($line -match "BUILD FAILED|FAILURE: Build failed") {
          $script:sawBuildFailure = $true
        }
        $line | Add-Content -Path $LogFile -Encoding UTF8
        Write-Host $line
      }
    $gradleExitCode = $LASTEXITCODE
  } finally {
    $ErrorActionPreference = $previousErrorActionPreference
  }

  if ($gradleExitCode -ne 0 -or $sawBuildFailure) {
    $exitSummary = if ($gradleExitCode -ne 0) { "exit code $gradleExitCode" } else { "a BUILD FAILED marker" }
    throw "Gradle build failed with $exitSummary. See $LogFile"
  }
} finally {
  if (!$Daemon) {
    Write-Host "Stopping Gradle daemons so automation exits cleanly..."
    & $Gradle --stop
  }
  Pop-Location
}
