@echo off
setlocal

set "ROOT=%~dp0"
set "LOCAL_GRADLE=%ROOT%\.gradle-local\gradle-8.2\bin\gradle.bat"
set "CACHED_GRADLE=%USERPROFILE%\.gradle\wrapper\dists\gradle-8.2-bin\bbg7u40eoinfdyxsxr3z4i7ta\gradle-8.2\bin\gradle.bat"
set "JBR=C:\Program Files\Android\Android Studio\jbr"
set "GRADLE_USER_HOME=%ROOT%\.gradle-local\user-home"

if exist "%JBR%\bin\java.exe" (
  set "JAVA_HOME=%JBR%"
  set "PATH=%JBR%\bin;%PATH%"
)

if exist "%LOCAL_GRADLE%" (
  call "%LOCAL_GRADLE%" %*
  exit /b %ERRORLEVEL%
)

if not exist "%CACHED_GRADLE%" (
  echo Gradle 8.2 is not available in .gradle-local or at "%CACHED_GRADLE%".
  echo Install Gradle or run Android Studio once to populate the Gradle wrapper cache.
  exit /b 1
)

call "%CACHED_GRADLE%" %*
