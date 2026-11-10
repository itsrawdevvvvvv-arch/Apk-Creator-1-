@rem Gradle startup script for Windows
@if "%DEBUG%"=="" @echo off
set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
set APP_HOME=%DIRNAME%
set WRAPPER_JAR=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar
if exist "%WRAPPER_JAR%" (
    java -Xmx64m -Xms64m -classpath "%WRAPPER_JAR%" org.gradle.wrapper.GradleWrapperMain %*
) else (
    where gradle >nul 2>nul
    if %ERRORLEVEL% equ 0 (
        gradle %*
    ) else (
        echo ERROR: gradle-wrapper.jar not found and gradle is not on PATH.
        echo Open in Android Studio or run "gradle wrapper --gradle-version 8.13".
        exit /b 1
    )
)
