package bigdata;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.hbase.filter.*;
import org.apache.hadoop.hbase.CompareOperator;

import java.io.IOException;

/**
 * 23CSE352: Big Data Analytics - Project Review 2
 * Topic: Healthcare Patient Record & Clinical Encounter Management using Apache HBase
 * Dataset: UCI 130-US Hospitals Authentic Clinical Records (1999-2008)
 * 
 * Demonstrates:
 * 1. Connecting to Apache HBase Cluster
 * 2. Creating Table 'patient_records' with Column Families ('admission', 'clinical', 'patient')
 * 3. Inserting Real-Time Clinical Encounter (PUT)
 * 4. Point Lookup by Composite Row Key (GET)
 * 5. Analytical Scans with Filters (SingleColumnValueFilter for High-Risk 30-Day Readmissions & PrefixFilter)
 * 6. Deleting Clinical Records (DELETE)
 */
public class HBaseHealthcareOperations {

    private static final String TABLE_NAME = "patient_records";
    private static final byte[] CF_ADMISSION = Bytes.toBytes("admission");
    private static final byte[] CF_CLINICAL = Bytes.toBytes("clinical");
    private static final byte[] CF_PATIENT = Bytes.toBytes("patient");

    public static void main(String[] args) {
        System.out.println("==================================================================");
        System.out.println(" 23CSE352: Big Data Analytics - Project Review 2");
        System.out.println(" Apache HBase Java API: Healthcare Patient Record Management");
        System.out.println("==================================================================\n");

        Configuration config = HBaseConfiguration.create();

        try (Connection connection = ConnectionFactory.createConnection(config);
             Admin admin = connection.getAdmin()) {

            TableName tableName = TableName.valueOf(TABLE_NAME);

            // Step 1: Check and Create Table if not present
            createTableIfNotExists(admin, tableName);

            try (Table table = connection.getTable(tableName)) {

                // Step 2: Insert a new clinical encounter (PUT)
                String demoRowKey = "Cardiology#99887766#33445566";
                insertPatientEncounter(table, demoRowKey);

                // Step 3: Point lookup by row key (GET)
                getPatientEncounter(table, demoRowKey);

                // Step 4: Analytical Scan - SingleColumnValueFilter (High-Risk Readmissions: '<30')
                scanHighRiskReadmissions(table);

                // Step 5: Analytical Scan - PrefixFilter (Cardiology Department)
                scanDepartmentPatients(table, "Cardiology#");

                // Step 6: Delete record (DELETE)
                deletePatientEncounter(table, demoRowKey);

                // Step 7: Verify deletion
                verifyDeletion(table, demoRowKey);

            }

            System.out.println("\n==================================================================");
            System.out.println(" [SUCCESS] All HBase Healthcare Java API Operations Completed!");
            System.out.println("==================================================================");

        } catch (IOException e) {
            System.err.println("[ERROR] Exception occurred during HBase operation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Step 1: DDL Table Creation with 3 Column Families (admission, clinical, patient).
     */
    private static void createTableIfNotExists(Admin admin, TableName tableName) throws IOException {
        System.out.println("[STEP 1] Checking HBase Table: " + tableName.getNameAsString());
        if (admin.tableExists(tableName)) {
            System.out.println("         -> Table '" + tableName.getNameAsString() + "' already exists. Reusing existing schema.\n");
            return;
        }

        System.out.println("         -> Table does not exist. Creating table with column families: admission, clinical, patient...");
        try {
            TableDescriptor desc = TableDescriptorBuilder.newBuilder(tableName)
                    .setColumnFamily(ColumnFamilyDescriptorBuilder.newBuilder(CF_ADMISSION).build())
                    .setColumnFamily(ColumnFamilyDescriptorBuilder.newBuilder(CF_CLINICAL).build())
                    .setColumnFamily(ColumnFamilyDescriptorBuilder.newBuilder(CF_PATIENT).build())
                    .build();
            admin.createTable(desc);
            System.out.println("         -> Table '" + tableName.getNameAsString() + "' created successfully.\n");
        } catch (Throwable t) {
            org.apache.hadoop.hbase.HTableDescriptor htd = new org.apache.hadoop.hbase.HTableDescriptor(tableName);
            htd.addFamily(new org.apache.hadoop.hbase.HColumnDescriptor(CF_ADMISSION));
            htd.addFamily(new org.apache.hadoop.hbase.HColumnDescriptor(CF_CLINICAL));
            htd.addFamily(new org.apache.hadoop.hbase.HColumnDescriptor(CF_PATIENT));
            admin.createTable(htd);
            System.out.println("         -> Table created using descriptor fallback.\n");
        }
    }

    /**
     * Step 2: DML Real-Time Patient Ingestion (PUT).
     * Row Key: MedicalSpecialty#PatientNBR#EncounterID
     */
    private static void insertPatientEncounter(Table table, String rowKey) throws IOException {
        System.out.println("[STEP 2] Inserting Real-Time Clinical Encounter (PUT)");
        System.out.println("         -> Row Key: " + rowKey);

        Put put = new Put(Bytes.toBytes(rowKey));

        // Column Family: admission
        put.addColumn(CF_ADMISSION, Bytes.toBytes("encounter_id"), Bytes.toBytes("33445566"));
        put.addColumn(CF_ADMISSION, Bytes.toBytes("admission_type"), Bytes.toBytes("EMERGENCY"));
        put.addColumn(CF_ADMISSION, Bytes.toBytes("time_in_hospital"), Bytes.toBytes("4"));
        put.addColumn(CF_ADMISSION, Bytes.toBytes("discharge_disp"), Bytes.toBytes("1"));

        // Column Family: clinical
        put.addColumn(CF_CLINICAL, Bytes.toBytes("specialty"), Bytes.toBytes("Cardiology"));
        put.addColumn(CF_CLINICAL, Bytes.toBytes("num_lab_procs"), Bytes.toBytes("62"));
        put.addColumn(CF_CLINICAL, Bytes.toBytes("num_meds"), Bytes.toBytes("19"));
        put.addColumn(CF_CLINICAL, Bytes.toBytes("diag_1"), Bytes.toBytes("414.01")); // Coronary Atherosclerosis
        put.addColumn(CF_CLINICAL, Bytes.toBytes("A1Cresult"), Bytes.toBytes(">8"));

        // Column Family: patient
        put.addColumn(CF_PATIENT, Bytes.toBytes("patient_nbr"), Bytes.toBytes("99887766"));
        put.addColumn(CF_PATIENT, Bytes.toBytes("gender"), Bytes.toBytes("Male"));
        put.addColumn(CF_PATIENT, Bytes.toBytes("age"), Bytes.toBytes("[60-70)"));
        put.addColumn(CF_PATIENT, Bytes.toBytes("insulin"), Bytes.toBytes("Up"));
        put.addColumn(CF_PATIENT, Bytes.toBytes("diabetesMed"), Bytes.toBytes("Yes"));
        put.addColumn(CF_PATIENT, Bytes.toBytes("readmitted"), Bytes.toBytes("<30"));

        table.put(put);
        System.out.println("         -> Encounter inserted into 'patient_records' across 3 column families.\n");
    }

    /**
     * Step 3: Point Lookup by Row Key (GET).
     */
    private static void getPatientEncounter(Table table, String rowKey) throws IOException {
        System.out.println("[STEP 3] Fetching Patient Encounter by Row Key (GET)");
        System.out.println("         -> Target Row Key: " + rowKey);

        Get get = new Get(Bytes.toBytes(rowKey));
        Result result = table.get(get);

        if (result.isEmpty()) {
            System.out.println("         -> [WARN] No clinical record found for row key: " + rowKey + "\n");
            return;
        }

        System.out.println("         ---------------- Patient Clinical Dossier ----------------");
        printCell(result, CF_ADMISSION, "encounter_id");
        printCell(result, CF_ADMISSION, "admission_type");
        printCell(result, CF_ADMISSION, "time_in_hospital");
        printCell(result, CF_CLINICAL, "specialty");
        printCell(result, CF_CLINICAL, "num_lab_procs");
        printCell(result, CF_CLINICAL, "num_meds");
        printCell(result, CF_CLINICAL, "diag_1");
        printCell(result, CF_PATIENT, "patient_nbr");
        printCell(result, CF_PATIENT, "gender");
        printCell(result, CF_PATIENT, "age");
        printCell(result, CF_PATIENT, "insulin");
        printCell(result, CF_PATIENT, "readmitted");
        System.out.println("         ----------------------------------------------------------\n");
    }

    /**
     * Step 4: Analytical Scan with SingleColumnValueFilter (readmitted = '<30').
     */
    private static void scanHighRiskReadmissions(Table table) throws IOException {
        System.out.println("[STEP 4] Scanning High-Risk Readmissions (<30 Days) with SingleColumnValueFilter");
        Scan scan = new Scan();
        scan.addFamily(CF_ADMISSION);
        scan.addFamily(CF_CLINICAL);
        scan.addFamily(CF_PATIENT);

        SingleColumnValueFilter filter = new SingleColumnValueFilter(
                CF_PATIENT,
                Bytes.toBytes("readmitted"),
                CompareOperator.EQUAL,
                new BinaryComparator(Bytes.toBytes("<30"))
        );
        filter.setFilterIfMissing(true);
        scan.setFilter(filter);
        scan.setLimit(5);

        int count = 0;
        try (ResultScanner scanner = table.getScanner(scan)) {
            for (Result res : scanner) {
                count++;
                String row = Bytes.toString(res.getRow());
                String spec = Bytes.toString(res.getValue(CF_CLINICAL, Bytes.toBytes("specialty")));
                String diag = Bytes.toString(res.getValue(CF_CLINICAL, Bytes.toBytes("diag_1")));
                String age = Bytes.toString(res.getValue(CF_PATIENT, Bytes.toBytes("age")));
                System.out.println("         [Risk Match " + count + "] Key: " + row +
                        " | Dept: " + spec + " | Diag: " + diag + " | Age: " + age);
            }
        }
        System.out.println("         -> Sampled high-risk readmission encounters displayed: " + count + "\n");
    }

    /**
     * Step 5: Analytical Scan with PrefixFilter (Department: 'Cardiology#').
     */
    private static void scanDepartmentPatients(Table table, String deptPrefix) throws IOException {
        System.out.println("[STEP 5] Scanning Department Patient Records with PrefixFilter ('" + deptPrefix + "')");
        Scan scan = new Scan();
        scan.addFamily(CF_CLINICAL);
        scan.addFamily(CF_PATIENT);

        PrefixFilter prefixFilter = new PrefixFilter(Bytes.toBytes(deptPrefix));
        scan.setFilter(prefixFilter);
        scan.setLimit(5);

        int count = 0;
        try (ResultScanner scanner = table.getScanner(scan)) {
            for (Result res : scanner) {
                count++;
                String row = Bytes.toString(res.getRow());
                String pat = Bytes.toString(res.getValue(CF_PATIENT, Bytes.toBytes("patient_nbr")));
                String meds = Bytes.toString(res.getValue(CF_CLINICAL, Bytes.toBytes("num_meds")));
                System.out.println("         [Dept Match " + count + "] Key: " + row +
                        " | Patient: " + pat + " | Medications: " + meds);
            }
        }
        System.out.println("         -> Department admissions previewed: " + count + "\n");
    }

    /**
     * Step 6: Delete Record (DELETE).
     */
    private static void deletePatientEncounter(Table table, String rowKey) throws IOException {
        System.out.println("[STEP 6] Deleting Encounter Record from HBase (DELETE)");
        System.out.println("         -> Target Row Key: " + rowKey);

        Delete delete = new Delete(Bytes.toBytes(rowKey));
        table.delete(delete);
        System.out.println("         -> Delete executed successfully.\n");
    }

    /**
     * Step 7: Verify Deletion with GET.
     */
    private static void verifyDeletion(Table table, String rowKey) throws IOException {
        System.out.println("[STEP 7] Verifying Deletion with GET");
        Get get = new Get(Bytes.toBytes(rowKey));
        Result result = table.get(get);
        if (result.isEmpty()) {
            System.out.println("         -> [VERIFIED] Encounter record purged from HBase (Empty Result).\n");
        } else {
            System.out.println("         -> [WARN] Record was still retrieved.\n");
        }
    }

    private static void printCell(Result result, byte[] family, String qualifier) {
        byte[] value = result.getValue(family, Bytes.toBytes(qualifier));
        String valStr = (value != null) ? Bytes.toString(value) : "NULL";
        System.out.printf("         %-22s: %s%n", Bytes.toString(family) + ":" + qualifier, valStr);
    }
}
