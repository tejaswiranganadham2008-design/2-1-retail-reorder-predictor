@echo off
setlocal
title Retail Reorder Point Predictor - II B.Tech Mini Project

echo ================================================================================
echo    RETAIL REORDER POINT PREDICTOR - SUPERMARKET AI ^& SUPPLY CHAIN SYSTEM
echo    Course: II B.Tech (AI ^| ADSA ^| OOPJ ^| Python)
echo    Team: R. Tejaswi (Lead), E. Gayathri, M. Purna Satya Sri,
echo          N. Vasantha Lakshmi, E. Ganga
echo ================================================================================
echo.

:: 1. Verify Java compiler
where javac >nul 2>nul
if %errorlevel% neq 0 (
    echo [ERROR] 'javac' (Java Compiler) was not found in PATH.
    echo Please install JDK 17+ and add it to your environment PATH.
    pause
    exit /b 1
)

:: 2. Verify Python
where python >nul 2>nul
if %errorlevel% neq 0 (
    where py >nul 2>nul
    if %errorlevel% neq 0 (
        echo [WARN] Python 3 not found in standard PATH. Forecasting may require python installed.
    )
)

:: 3. Create output directory if not exists
if not exist "out" mkdir out

echo [1/3] Compiling Java source files...
javac -encoding UTF-8 -d out src/model/*.java src/ds/*.java src/service/*.java src/server/*.java src/Main.java tests/TestSuite.java
if %errorlevel% neq 0 (
    echo.
    echo [ERROR] Compilation failed!
    pause
    exit /b 1
)
echo [SUCCESS] Compilation completed without errors.
echo.

echo [2/3] Running automated unit ^& integration tests...
java -cp out tests.TestSuite
if %errorlevel% neq 0 (
    echo.
    echo [WARN] Some tests failed. Proceeding with caution...
)
echo.

echo [3/3] Starting Built-in Java Web Server on http://localhost:8080 ...
echo [INFO] Opening default browser in 2 seconds...
start "" "http://localhost:8080"

echo.
echo ================================================================================
echo   SERVER ACTIVE: http://localhost:8080
echo   Press Ctrl+C anytime to terminate the server.
echo ================================================================================
java -cp out Main %*

pause
