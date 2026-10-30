@echo off
setlocal
set "APP_HOME=%~dp0"
set "JAVA_EXE=java"
if exist "%APP_HOME%runtime\bin\java.exe" set "JAVA_EXE=%APP_HOME%runtime\bin\java.exe"

"%JAVA_EXE%" --module-path "%APP_HOME%javafx" --add-modules javafx.controls,javafx.graphics,javafx.base -jar "%APP_HOME%network-speed-meter-1.0.0.jar"
if errorlevel 1 (
    echo.
    echo NetSpeed Meter failed to start. Install JDK 21 and keep the javafx folder next to this script.
    pause
)
endlocal
