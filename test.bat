@echo off
setlocal
if not exist "out" mkdir out
echo Compiling source and test files...
javac -encoding UTF-8 -d out src/model/*.java src/ds/*.java src/service/*.java src/server/*.java src/Main.java tests/TestSuite.java
if %errorlevel% neq 0 (
    echo Compilation failed!
    exit /b 1
)
echo Running TestSuite...
java -cp out tests.TestSuite
pause
