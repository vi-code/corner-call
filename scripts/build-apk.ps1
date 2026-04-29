$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $PSScriptRoot
$AndroidHome = Join-Path $env:LOCALAPPDATA "Android\Sdk"
$BuildTools = Join-Path $AndroidHome "build-tools\34.0.0"
$PlatformJar = Join-Path $AndroidHome "platforms\android-34\android.jar"
$JbrHome = "C:\Program Files\Android\Android Studio\jbr"
$JbrBin = Join-Path $JbrHome "bin"
$Java = Join-Path $JbrBin "java.exe"
$Javac = Join-Path $JbrBin "javac.exe"
$Jar = Join-Path $JbrBin "jar.exe"
$Keytool = Join-Path $JbrBin "keytool.exe"
$Aapt2 = Join-Path $BuildTools "aapt2.exe"
$D8 = Join-Path $BuildTools "d8.bat"
$Zipalign = Join-Path $BuildTools "zipalign.exe"
$Apksigner = Join-Path $BuildTools "apksigner.bat"

foreach ($Tool in @($Java, $Javac, $Jar, $Keytool, $Aapt2, $D8, $Zipalign, $Apksigner, $PlatformJar)) {
  if (!(Test-Path $Tool)) {
    throw "Missing Android build dependency: $Tool"
  }
}

$env:JAVA_HOME = $JbrHome
$env:Path = "$JbrBin;$env:Path"

function Invoke-Checked {
  param(
    [Parameter(Mandatory = $true)][string]$FilePath,
    [string[]]$Arguments = @()
  )

  & $FilePath @Arguments
  if ($LASTEXITCODE -ne 0) {
    throw "$FilePath failed with exit code $LASTEXITCODE"
  }
}

$BuildDir = Join-Path $Root "dist\android"
$ResDir = Join-Path $Root "android\res"
$CompiledRes = Join-Path $BuildDir "compiled-res.zip"
$GenDir = Join-Path $BuildDir "gen"
$ClassesDir = Join-Path $BuildDir "classes"
$DexDir = Join-Path $BuildDir "dex"
$UnsignedApk = Join-Path $BuildDir "corner-call-unsigned.apk"
$AlignedApk = Join-Path $BuildDir "corner-call-aligned.apk"
$FinalApk = Join-Path $Root "dist\cornercall0.1.apk"
$Keystore = Join-Path $BuildDir "corner-call-debug.keystore"

New-Item -ItemType Directory -Force $BuildDir | Out-Null
$PreservedKeystore = $null
if (Test-Path $Keystore) {
  $PreservedKeystore = Join-Path $env:TEMP "corner-call-debug.keystore"
  Copy-Item $Keystore $PreservedKeystore -Force
}
Remove-Item -Recurse -Force $BuildDir
New-Item -ItemType Directory -Force $GenDir, $ClassesDir, $DexDir | Out-Null
if ($PreservedKeystore -and (Test-Path $PreservedKeystore)) {
  New-Item -ItemType Directory -Force (Split-Path -Parent $Keystore) | Out-Null
  Copy-Item $PreservedKeystore $Keystore -Force
}

$LinkArgs = @(
  "link",
  "-o", $UnsignedApk,
  "-I", $PlatformJar,
  "--manifest", (Join-Path $Root "android\AndroidManifest.xml"),
  "--java", $GenDir,
  "--min-sdk-version", "23",
  "--target-sdk-version", "34"
)

if (Test-Path $ResDir) {
  Invoke-Checked -FilePath $Aapt2 -Arguments @(
    "compile",
    "--dir", $ResDir,
    "-o", $CompiledRes
  )
  $LinkArgs += $CompiledRes
}

Invoke-Checked -FilePath $Aapt2 -Arguments $LinkArgs

$SourceFiles = @(
  (Join-Path $Root "android\src\com\cornercall\app\MainActivity.java")
) + (Get-ChildItem $GenDir -Recurse -Filter "*.java" | ForEach-Object { $_.FullName })

$JavacArgs = @(
  "-encoding", "UTF-8",
  "-source", "8",
  "-target", "8",
  "-bootclasspath", $PlatformJar,
  "-d", $ClassesDir
) + $SourceFiles
Invoke-Checked -FilePath $Javac -Arguments $JavacArgs

$ClassFiles = Get-ChildItem $ClassesDir -Recurse -Filter "*.class" | ForEach-Object { $_.FullName }
$D8Args = @(
  "--lib", $PlatformJar,
  "--output", $DexDir
) + $ClassFiles
Invoke-Checked -FilePath $D8 -Arguments $D8Args

Push-Location $DexDir
try {
  Invoke-Checked -FilePath $Jar -Arguments @("uf", $UnsignedApk, "classes.dex")
} finally {
  Pop-Location
}

Invoke-Checked -FilePath $Zipalign -Arguments @("-f", "-p", "4", $UnsignedApk, $AlignedApk)

if (!(Test-Path $Keystore)) {
  Invoke-Checked -FilePath $Keytool -Arguments @(
    "-genkeypair",
    "-v",
    "-keystore", $Keystore,
    "-storepass", "android",
    "-keypass", "android",
    "-alias", "corner-call-debug",
    "-keyalg", "RSA",
    "-keysize", "2048",
    "-validity", "10000",
    "-dname", "CN=Corner Call Debug,O=Corner Call,C=US"
  )
}

Invoke-Checked -FilePath $Apksigner -Arguments @(
  "sign",
  "--ks", $Keystore,
  "--ks-pass", "pass:android",
  "--key-pass", "pass:android",
  "--out", $FinalApk,
  $AlignedApk
)

Invoke-Checked -FilePath $Apksigner -Arguments @("verify", "--verbose", $FinalApk)
Write-Host "APK built: $FinalApk"
