#!/usr/bin/env bash
set -e
mkdir -p out
echo "Compiling source and test files..."
javac -encoding UTF-8 -d out src/model/*.java src/ds/*.java src/service/*.java src/server/*.java src/Main.java tests/TestSuite.java
echo "Running TestSuite..."
java -cp out tests.TestSuite
