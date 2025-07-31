@echo off
:: -----------------------------------------------------------------------------
:: Gradle start up script for Windows
:: -----------------------------------------------------------------------------

setlocal

set DIR=%~dp0
set APP_BASE_NAME=%~n0
set APP_HOME=%DIR%

:: Locate Java
if defined JAVA_HOME (
  set JAVA_EXE=%JAVA_HOME%\bin\java.exe
) else (
  set JAVA_EXE=java
)

:: Check Java exists
"%JAVA_EXE%" -version >NUL 2>&1
if %ERRORLEVEL% NEQ 0 (
  echo.
  echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
  echo Please set the JAVA_HOME variable in your environment to match the location of your Java installation.
  exit /b 1
)

set CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar

:: Launch Gradle
"%JAVA_EXE%" -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
endlocal
