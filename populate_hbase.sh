#!/bin/bash
# ==============================================================================
# Script: populate_hbase.sh
# Purpose: Compiles and executes HBaseDataLoader to batch insert Healthcare CSV
# ==============================================================================

set -e

echo "=================================================================="
echo " [1/3] Checking Environment Prerequisites"
echo "=================================================================="

if ! command -v hbase &> /dev/null; then
    echo "[ERROR] 'hbase' command not found. Please ensure HBase is installed and added to PATH."
    exit 1
fi

DATASET_FILE="dataset/healthcare_hospital_clean.csv"
if [ ! -f "$DATASET_FILE" ]; then
    echo "[ERROR] Clean dataset file not found at $DATASET_FILE"
    exit 1
fi

echo "[✓] HBase binary found: $(which hbase)"
echo "[✓] Dataset located: $DATASET_FILE ($(wc -l < "$DATASET_FILE") rows)"

echo ""
echo "=================================================================="
echo " [2/3] Compiling HBaseDataLoader.java"
echo "=================================================================="
mkdir -p classes
javac -cp "$(hbase classpath)" -d classes src/bigdata/HBaseDataLoader.java
echo "[✓] Compilation successful. Bytecode generated in classes/bigdata/"

echo ""
echo "=================================================================="
echo " [3/3] Ingesting CSV Records into HBase Table 'patient_records'"
echo "=================================================================="
HBASE_CLASSPATH=classes hbase bigdata.HBaseDataLoader "$DATASET_FILE" 10000

echo ""
echo "=================================================================="
echo " [SUCCESS] Healthcare Batch Data Ingestion Complete!"
echo " Next step: Launch 'hbase shell' and run queries in hbase_commands.txt"
echo "=================================================================="
