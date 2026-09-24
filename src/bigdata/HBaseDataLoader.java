package bigdata;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 23CSE352: Big Data Analytics - Project Review 2
 * Fast Batch CSV Data Loader for Hospital Clinical Encounters into Apache HBase
 */
public class HBaseDataLoader {

    private static final String TABLE_NAME = "patient_records";
    private static final byte[] CF_ADMISSION = Bytes.toBytes("admission");
    private static final byte[] CF_CLINICAL = Bytes.toBytes("clinical");
    private static final byte[] CF_PATIENT = Bytes.toBytes("patient");
    private static final int BATCH_SIZE = 500;

    public static void main(String[] args) {
        String csvFilePath = (args.length > 0) ? args[0] : "dataset/healthcare_hospital_clean.csv";
        int maxRecords = (args.length > 1) ? Integer.parseInt(args[1]) : 10000;

        System.out.println("==================================================================");
        System.out.println(" HBase Healthcare Real-World Batch Ingestion Utility");
        System.out.println(" Source File: " + csvFilePath);
        System.out.println(" Target Table: " + TABLE_NAME);
        System.out.println(" Max Ingest Limit: " + maxRecords);
        System.out.println("==================================================================\n");

        Configuration config = HBaseConfiguration.create();

        try (Connection connection = ConnectionFactory.createConnection(config);
             Admin admin = connection.getAdmin()) {

            TableName tableName = TableName.valueOf(TABLE_NAME);

            // Create table if missing
            if (!admin.tableExists(tableName)) {
                System.out.println("[-] Table '" + TABLE_NAME + "' does not exist. Creating schema...");
                try {
                    TableDescriptor desc = TableDescriptorBuilder.newBuilder(tableName)
                            .setColumnFamily(ColumnFamilyDescriptorBuilder.newBuilder(CF_ADMISSION).build())
                            .setColumnFamily(ColumnFamilyDescriptorBuilder.newBuilder(CF_CLINICAL).build())
                            .setColumnFamily(ColumnFamilyDescriptorBuilder.newBuilder(CF_PATIENT).build())
                            .build();
                    admin.createTable(desc);
                } catch (Throwable t) {
                    org.apache.hadoop.hbase.HTableDescriptor htd = new org.apache.hadoop.hbase.HTableDescriptor(tableName);
                    htd.addFamily(new org.apache.hadoop.hbase.HColumnDescriptor(CF_ADMISSION));
                    htd.addFamily(new org.apache.hadoop.hbase.HColumnDescriptor(CF_CLINICAL));
                    htd.addFamily(new org.apache.hadoop.hbase.HColumnDescriptor(CF_PATIENT));
                    admin.createTable(htd);
                }
                System.out.println("[+] Schema created with families: admission, clinical, patient.\n");
            }

            try (Table table = connection.getTable(tableName);
                 BufferedReader br = new BufferedReader(new FileReader(csvFilePath))) {

                String headerLine = br.readLine();
                if (headerLine == null) {
                    System.err.println("[ERROR] Empty CSV file: " + csvFilePath);
                    return;
                }

                String line;
                int totalLoaded = 0;
                List<Put> batchList = new ArrayList<>(BATCH_SIZE);
                long startTime = System.currentTimeMillis();

                System.out.println("[*] Streaming and batching clinical admissions into HBase...");

                while ((line = br.readLine()) != null && totalLoaded < maxRecords) {
                    String[] tokens = parseCsvLine(line);
                    if (tokens.length < 13) {
                        continue;
                    }

                    // Field Mapping:
                    // 0: encounter_id, 1: patient_nbr, 2: medical_specialty, 3: admission_type,
                    // 4: time_in_hospital, 5: num_lab_procedures, 6: num_medications, 7: diag_1,
                    // 8: age, 9: gender, 10: insulin, 11: diabetesMed, 12: readmitted
                    String encounterId = tokens[0].trim();
                    String patientNbr = tokens[1].trim();
                    String specialty = tokens[2].trim();
                    String admissionType = tokens[3].trim();
                    String timeInHospital = tokens[4].trim();
                    String numLabProcs = tokens[5].trim();
                    String numMeds = tokens[6].trim();
                    String diag1 = tokens[7].trim();
                    String age = tokens[8].trim();
                    String gender = tokens[9].trim();
                    String insulin = tokens[10].trim();
                    String diabetesMed = tokens[11].trim();
                    String readmitted = tokens[12].trim();

                    // Composite Row Key: MedicalSpecialty#PatientNBR#EncounterID
                    String rowKey = specialty + "#" + patientNbr + "#" + encounterId;
                    Put put = new Put(Bytes.toBytes(rowKey));

                    // CF: admission
                    addCell(put, CF_ADMISSION, "encounter_id", encounterId);
                    addCell(put, CF_ADMISSION, "admission_type", admissionType);
                    addCell(put, CF_ADMISSION, "time_in_hospital", timeInHospital);

                    // CF: clinical
                    addCell(put, CF_CLINICAL, "specialty", specialty);
                    addCell(put, CF_CLINICAL, "num_lab_procs", numLabProcs);
                    addCell(put, CF_CLINICAL, "num_meds", numMeds);
                    addCell(put, CF_CLINICAL, "diag_1", diag1);

                    // CF: patient
                    addCell(put, CF_PATIENT, "patient_nbr", patientNbr);
                    addCell(put, CF_PATIENT, "age", age);
                    addCell(put, CF_PATIENT, "gender", gender);
                    addCell(put, CF_PATIENT, "insulin", insulin);
                    addCell(put, CF_PATIENT, "diabetesMed", diabetesMed);
                    addCell(put, CF_PATIENT, "readmitted", readmitted);

                    batchList.add(put);
                    totalLoaded++;

                    if (batchList.size() >= BATCH_SIZE) {
                        table.put(batchList);
                        batchList.clear();
                        System.out.printf("    Loaded %6d encounters...%n", totalLoaded);
                    }
                }

                if (!batchList.isEmpty()) {
                    table.put(batchList);
                    batchList.clear();
                }

                long duration = System.currentTimeMillis() - startTime;
                System.out.println("\n[SUCCESS] Completed loading " + totalLoaded + " clinical encounters in " + duration + " ms (" + (duration / 1000.0) + " seconds).");
            }

        } catch (Exception e) {
            System.err.println("[ERROR] Failed to load data into HBase: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void addCell(Put put, byte[] family, String qualifier, String value) {
        if (value != null && !value.isEmpty()) {
            put.addColumn(family, Bytes.toBytes(qualifier), Bytes.toBytes(value));
        }
    }

    private static String[] parseCsvLine(String line) {
        List<String> list = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                list.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        list.add(sb.toString());
        return list.toArray(new String[0]);
    }
}
