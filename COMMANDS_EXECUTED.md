# Commands Executed (100% Raw Manual Commands Log)

This document provides a chronological, step-by-step log of every single raw terminal command and HBase Shell command required to build, load, query, and demonstrate the HBase Healthcare Patient Record Management pipeline.

---

## 📌 Phase 1: Host Workspace Setup & Git Synchronization

Executed on Host Machine (Mac Terminal):

```bash
# Navigate to workspace
cd /Users/aksharsakhi/Documents/Files/Code/Amrita/Big_Data

# Initialize Git repository
cd Healthcare-Patient-Record-Management-HBase
git init
git add .
git commit -m "Initial commit for Project Review 2 - Healthcare Patient Record Management using Apache HBase"
git branch -M main
git remote add origin https://github.com/aksharsakhi/Healthcare-Patient-Record-Management-HBase.git
git push -u origin main
```

---

## 📌 Phase 2: Virtual Machine Environment & Daemon Startup

Executed on UTM Ubuntu Virtual Machine (`hadoop@aksharsakhi-QEMU-Virtual-Machine`):

```bash
# Clone the repository
git clone https://github.com/aksharsakhi/Healthcare-Patient-Record-Management-HBase.git
cd Healthcare-Patient-Record-Management-HBase

# Start HDFS Distributed Storage Daemons
start-dfs.sh

# Start Apache HBase Distributed Master & RegionServer Daemons
start-hbase.sh

# Verify all daemons are active
jps
```
Expected output:
```
NameNode
DataNode
SecondaryNameNode
HMaster
HRegionServer
HQuorumPeer
```

---

## 📌 Phase 3: Raw Java Compilation & Batch Dataset Ingestion

Executed on Virtual Machine Terminal:

```bash
# Clean and create bytecode output directory
rm -rf classes && mkdir -p classes

# Compile HBase DataLoader with HBase classpath
javac -cp $(hbase classpath) -d classes src/bigdata/HBaseDataLoader.java

# Execute DataLoader to ingest 10,000 real-world records into HBase
HBASE_CLASSPATH=classes hbase bigdata.HBaseDataLoader dataset/healthcare_hospital_clean.csv 10000
```

---

## 📌 Phase 4: HBase Interactive Shell Execution

Executed inside `hbase shell`:

```bash
# Start HBase interactive shell
hbase shell
```

### 1. Verification of Table Schema
```ruby
status
list
describe 'patient_records'
```

### 2. Manual Sample Insertion (PUT)
```ruby
put 'patient_records', 'Cardiology#55629189#149190', 'admission:encounter_id', '149190'
put 'patient_records', 'Cardiology#55629189#149190', 'admission:admission_type', 'EMERGENCY'
put 'patient_records', 'Cardiology#55629189#149190', 'admission:time_in_hospital', '3'
put 'patient_records', 'Cardiology#55629189#149190', 'clinical:specialty', 'Cardiology'
put 'patient_records', 'Cardiology#55629189#149190', 'clinical:num_lab_procs', '59'
put 'patient_records', 'Cardiology#55629189#149190', 'clinical:num_meds', '18'
put 'patient_records', 'Cardiology#55629189#149190', 'clinical:diag_1', '414.01'
put 'patient_records', 'Cardiology#55629189#149190', 'patient:patient_nbr', '55629189'
put 'patient_records', 'Cardiology#55629189#149190', 'patient:gender', 'Female'
put 'patient_records', 'Cardiology#55629189#149190', 'patient:age', '[10-20)'
put 'patient_records', 'Cardiology#55629189#149190', 'patient:insulin', 'Up'
put 'patient_records', 'Cardiology#55629189#149190', 'patient:readmitted', '<30'
```

### 3. Point Retrieval (GET)
```ruby
# Retrieve entire row
get 'patient_records', 'Cardiology#55629189#149190'

# Retrieve specific column family (clinical)
get 'patient_records', 'Cardiology#55629189#149190', 'clinical'

# Retrieve specific qualifier (readmission status)
get 'patient_records', 'Cardiology#55629189#149190', 'patient:readmitted'
```

### 4. Basic Scans & Range Queries
```ruby
# Sample table preview
scan 'patient_records', {LIMIT => 5}

# Patient Longitudinal Medical History Range Scan
scan 'patient_records', {STARTROW => 'Cardiology#55629189#', STOPROW => 'Cardiology#55629189~', LIMIT => 5}
```

### 5. Advanced Analytical Filters (Rubric Requirement: 4 Marks)
```ruby
# Filter 1: SingleColumnValueFilter (30-Day Readmission Risk '<30')
scan 'patient_records', {FILTER => "SingleColumnValueFilter('patient', 'readmitted', =, 'binary:<30')", LIMIT => 5}

# Filter 2: SingleColumnValueFilter (Polypharmacy Risk: num_meds > 15)
scan 'patient_records', {FILTER => "SingleColumnValueFilter('clinical', 'num_meds', >, 'binary:15')", LIMIT => 5}

# Filter 3: PrefixFilter (Cardiology Department Triage)
scan 'patient_records', {FILTER => "PrefixFilter('Cardiology#')", LIMIT => 5}

# Filter 4: RowFilter with RegexStringComparator (Pediatric Encounters)
scan 'patient_records', {FILTER => "RowFilter(=, 'regexstring:^Pediatrics.*')", LIMIT => 5}

# Filter 5: ValueFilter with SubstringComparator (Keyword "EMERGENCY")
scan 'patient_records', {FILTER => "ValueFilter(=, 'substring:EMERGENCY')", LIMIT => 5}

# Filter 6: Compound FilterList (Cardiology AND Readmitted <30 Days)
scan 'patient_records', {FILTER => "(PrefixFilter('Cardiology#')) AND (SingleColumnValueFilter('patient', 'readmitted', =, 'binary:<30'))", LIMIT => 5}

# Filter 7: ColumnPaginationFilter
scan 'patient_records', {FILTER => "ColumnPaginationFilter(3, 0)", LIMIT => 5}
```

### 6. Count and Data Deletion Operations
```ruby
# Row Count
count 'patient_records', INTERVAL => 1000, CACHE => 1000

# Delete specific cell
delete 'patient_records', 'Cardiology#55629189#149190', 'clinical:diag_1'

# Delete entire row
deleteall 'patient_records', 'Cardiology#55629189#149190'

# Verify deletion
get 'patient_records', 'Cardiology#55629189#149190'
```

---

## 📌 Phase 5: Raw Java API Compilation & Execution

Executed on Virtual Machine Terminal:

```bash
# Compile HBase Java API client
javac -cp $(hbase classpath) -d classes src/bigdata/HBaseHealthcareOperations.java

# Run Java application
HBASE_CLASSPATH=classes hbase bigdata.HBaseHealthcareOperations
```
