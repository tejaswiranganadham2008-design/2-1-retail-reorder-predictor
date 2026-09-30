#!/usr/bin/env bash
# =============================================================================
# Retail Reorder Point Predictor - Build & Run Script
# =============================================================================

set -e

echo "================================================================================"
echo "   RETAIL REORDER POINT PREDICTOR - SUPERMARKET AI & SUPPLY CHAIN SYSTEM"
echo "   Course: II B.Tech (AI | ADSA | OOPJ | Python)"
echo "   Team: R. Tejaswi (Lead), E. Gayathri, M. Purna Satya Sri,"
echo "         N. Vasantha Lakshmi, E. Ganga"
echo "================================================================================"

mkdir -p out

echo "[1/3] Compiling Java source files..."
javac -encoding UTF-8 -d out src/model/*.java src/ds/*.java src/service/*.java src/server/*.java src/Main.java tests/TestSuite.java
echo "[SUCCESS] Compilation successful."

echo "[2/3] Running automated unit & integration tests..."
java -cp out tests.TestSuite

echo "[3/3] Starting Built-in Java Web Server on http://localhost:8080 ..."
echo "Open in browser: http://localhost:8080"

# Open browser if on macOS or Linux with xdg-open
if command -v xdg-open > /dev/null; then
    xdg-open "http://localhost:8080" &
elif command -v open > /dev/null; then
    open "http://localhost:8080" &
fi

java -cp out Main "$@"
