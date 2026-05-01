$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $PSScriptRoot
Push-Location $Root
try {
  & (Join-Path $PSScriptRoot "build-gradle.ps1")
} finally {
  Pop-Location
}
