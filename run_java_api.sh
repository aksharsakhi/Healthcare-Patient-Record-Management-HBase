#!/bin/bash
# ==============================================================================
# Script: run_java_api.sh
# Purpose: Compiles and executes HBase Healthcare Java API Operations
# ==============================================================================

set -e

echo "=================================================================="
echo " [1/3] Checking Environment Prerequisites"
echo "=================================================================="

if ! command -v hbase &> /dev/null; then
    echo "[ERROR] 'hbase' command not found. Please ensure HBase is installed and added to PATH."
    exit 1
fi

echo "[✓] HBase binary found: $(which hbase)"

echo ""
echo "=================================================================="
echo " [2/3] Compiling HBaseHealthcareOperations.java"
echo "=================================================================="
mkdir -p classes
javac -cp "$(hbase classpath)" -d classes src/bigdata/HBaseHealthcareOperations.java
echo "[✓] Compilation successful."

echo ""
echo "=================================================================="
echo " [3/3] Executing HBase Java API Program"
echo "=================================================================="
HBASE_CLASSPATH=classes hbase bigdata.HBaseHealthcareOperations

echo ""
echo "=================================================================="
echo " [SUCCESS] HBase Healthcare Java API Demonstration Finished!"
echo "=================================================================="
