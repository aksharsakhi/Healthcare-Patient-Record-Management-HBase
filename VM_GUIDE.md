# UTM Ubuntu Virtual Machine (VM) Execution & Screenshot Guide

This guide provides a comprehensive walkthrough for running the **Project Review 2 (Healthcare Patient Record Management using Apache HBase)** pipeline on your Ubuntu UTM Virtual Machine and capturing all the screenshots required for your presentation slides and final LaTeX report.

---

## 📋 Pre-Flight Checklist
Before beginning, ensure your VM terminal has access and you have cloned the repository.

---

## Step 1: Clone Repository on Your VM
Open your Ubuntu terminal on the UTM VM and clone the repository:
```bash
git clone https://github.com/aksharsakhi/Healthcare-Patient-Record-Management-HBase.git
cd Healthcare-Patient-Record-Management-HBase
```

---

## Step 2: Start Hadoop & HBase Daemons
Start HDFS (HBase distributed storage backbone) and Apache HBase:
```bash
# Start HDFS
start-dfs.sh

# Start Apache HBase
start-hbase.sh

# Verify all daemons are running
jps
```
> **What you should see in `jps`:**
> - `HMaster`
> - `HRegionServer`
> - `HQuorumPeer` (ZooKeeper)
> - `NameNode`
> - `DataNode`
> - `SecondaryNameNode`

📸 **SCREENSHOT 1: HBase Daemons Verification**
- **Action:** Take a screenshot of the `jps` output showing `HMaster`, `HRegionServer`, and `HQuorumPeer`.
- **Save as:** `presentation/new_img/screenshot1_daemons.png`

---

## Step 3: Ingest Clinical Dataset into HBase
Run the high-speed data ingestion utility:
```bash
chmod +x populate_hbase.sh run_java_api.sh run_hbase_shell.sh
./populate_hbase.sh
```
> **What happens:** The script compiles `HBaseDataLoader.java` and streams the 10,000 authentic clinical hospital records into the `patient_records` table in batches of 500 records.

📸 **SCREENSHOT 2: Batch Data Ingestion**
- **Action:** Take a screenshot showing compilation and the output `[SUCCESS] Completed loading 10000 clinical encounters in ... ms`.
- **Save as:** `presentation/new_img/screenshot2_ingestion.png`

---

## Step 4: HBase Shell - Table Verification & Point Retrieval (GET)
Launch the HBase interactive shell:
```bash
hbase shell
```
Inside the HBase shell, run:
```ruby
# Verify table schema
describe 'patient_records'

# Point Lookup (GET)
get 'patient_records', 'Cardiology#55629189#149190'
```

📸 **SCREENSHOT 3: Table Schema & GET Operation**
- **Action:** Take a screenshot showing `describe 'patient_records'` and the returned cells for the `get` command.
- **Save as:** `presentation/new_img/screenshot3_get.png`

---

## Step 5: HBase Shell - Longitudinal Patient History Range Scan
Inside the HBase shell, run the range scan to inspect the patient's complete encounter history:
```ruby
scan 'patient_records', {STARTROW => 'Cardiology#55629189#', STOPROW => 'Cardiology#55629189~', LIMIT => 5}
```
📸 **SCREENSHOT 4: Longitudinal Patient History Range Scan**
- **Action:** Take a screenshot showing all clinical admissions for Patient 55629189.
- **Save as:** `presentation/new_img/screenshot4_patient_scan.png`

---

## Step 6: HBase Shell - Analytical Filters (Rubric Requirement: 4 Marks)
Inside the HBase shell, execute the clinical filters:

### Filter 1: High-Risk 30-Day Hospital Readmissions
```ruby
scan 'patient_records', {FILTER => "SingleColumnValueFilter('patient', 'readmitted', =, 'binary:<30')", LIMIT => 5}
```
📸 **SCREENSHOT 5: SingleColumnValueFilter (Readmission Risk <30)**
- **Save as:** `presentation/new_img/screenshot5_filter_readmit.png`

### Filter 2: Polypharmacy Risk Monitoring (num_medications > 15)
```ruby
scan 'patient_records', {FILTER => "SingleColumnValueFilter('clinical', 'num_meds', >, 'binary:15')", LIMIT => 5}
```
📸 **SCREENSHOT 6: SingleColumnValueFilter (Polypharmacy Risk)**
- **Save as:** `presentation/new_img/screenshot6_filter_polypharmacy.png`

### Filter 3: Department-Level Clinical Triage (Cardiology)
```ruby
scan 'patient_records', {FILTER => "PrefixFilter('Cardiology#')", LIMIT => 5}
```
📸 **SCREENSHOT 7: PrefixFilter (Cardiology Department)**
- **Save as:** `presentation/new_img/screenshot7_prefix_cardiology.png`

### Filter 4 & 5: Regex RowFilter (Pediatrics) & ValueFilter (Emergency Admissions)
```ruby
# Track pediatric encounters across all hospitals
scan 'patient_records', {FILTER => "RowFilter(=, 'regexstring:^Pediatrics.*')", LIMIT => 5}

# Locate emergency admissions across all columns
scan 'patient_records', {FILTER => "ValueFilter(=, 'substring:EMERGENCY')", LIMIT => 5}
```
📸 **SCREENSHOT 8: Regex RowFilter & Value Substring Filters**
- **Save as:** `presentation/new_img/screenshot8_regex_filter.png`

---

## Step 7: Count & Deletion Operations
Inside the HBase shell, run the count and deletion commands:
```ruby
# Count total records in table
count 'patient_records', INTERVAL => 1000, CACHE => 1000

# Delete specific cell
delete 'patient_records', 'Cardiology#55629189#149190', 'clinical:diag_1'

# Delete entire row
deleteall 'patient_records', 'Cardiology#55629189#149190'

# Verify row is purged
get 'patient_records', 'Cardiology#55629189#149190'
```
Exit the shell when done:
```ruby
exit
```

📸 **SCREENSHOT 9: Count, Delete, and DeleteAll Operations**
- **Action:** Take a screenshot of the `count` output and the `deleteall` execution.
- **Save as:** `presentation/new_img/screenshot9_count_delete.png`

---

## Step 8: Java API Demonstration
Run the standalone Java API runner script:
```bash
./run_java_api.sh
```
> **What happens:** The script compiles and executes `HBaseHealthcareOperations.java`, demonstrating:
> 1. Table status verification
> 2. Real-time `Put` of clinical encounter `Cardiology#99887766#33445566`
> 3. Point lookup `Get`
> 4. `SingleColumnValueFilter` scan in Java (30-day readmissions)
> 5. `PrefixFilter` scan in Java (Cardiology department)
> 6. Record `Delete`
> 7. Verified empty retrieval

📸 **SCREENSHOT 10: HBase Java API Execution**
- **Action:** Take a screenshot showing the clean terminal banners and results from Step 1 through Step 7.
- **Save as:** `presentation/new_img/screenshot10_java_api.png`

---

## Step 9: Sync Screenshots to Presentation & Report
1. Copy the captured screenshots from your VM into your laptop repo folder under `presentation/new_img/`.
2. Commit and push back to git:
   ```bash
   git add presentation/new_img/
   git commit -m "Added live execution screenshots from Ubuntu VM"
   git push origin main
   ```
3. Recompile the LaTeX report and presentation using `tectonic`:
   ```bash
   tectonic presentation/report.tex
   tectonic presentation/presentation.tex
   ```
You are 100% prepared for your project review!
