@echo off
title JavaFX Checkers
cd /d "%~dp0"
echo Starting JavaFX Checkers...
call .\gradlew.bat run
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo An error occurred while running the game.
    pause
)
