$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $PSScriptRoot
$LogFile = Join-Path $Root "dist\gradle-build.log"

if (!(Test-Path $LogFile)) {
  New-Item -ItemType Directory -Force (Split-Path -Parent $LogFile) | Out-Null
  "No Gradle build log yet. Start scripts/build-gradle.ps1 in another terminal." |
    Set-Content -Path $LogFile
}

Get-Content $LogFile -Wait -Tail 80
