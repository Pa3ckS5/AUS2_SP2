package whoApp;

import file.hashfile.HashFile;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Random;

public class WhoSystem {
    private HashFile<Patient> patients;
    private HashFile<PcrTest> pcrTests;

    private final String systemName;
    private int blockSize;
    private int overflowBlockSize;
    private int nextPatientId;
    private int nextPcrTestId;

    private Random random = new Random();

    public WhoSystem(String systemName, int blockSize, int overflowBlockSize) {
        //delete old with same systemName
        deleteOldFiles(systemName);

        //initialize new
        this.systemName = systemName;
        this.blockSize = blockSize;
        this.overflowBlockSize = overflowBlockSize;
        this.nextPatientId = 0;
        this.nextPcrTestId = 0;

        try {
            this.patients = new HashFile<>(systemName + "_patients", blockSize, overflowBlockSize,  Patient.class);
            this.pcrTests = new HashFile<>(systemName + "_pcrTests", blockSize, overflowBlockSize,  PcrTest.class);
        } catch (IOException e) {
            throw new RuntimeException("Error creating file at " + systemName, e);
        }
    }

    public WhoSystem(String systemName) {
        this.systemName = systemName;

        //load
        if (!load(systemName)) {
            throw new RuntimeException("Error loading file at " + systemName);
        }
    }

    public void generate(int patientCount, int testCount) {
        generateRandomPatients(patientCount);
        generateRandomTests(testCount);
    }

    private void generateRandomTests(int count) {
        int maxAttempts = count * 2; // for safe
        int attempts = 0;
        int success = 0;

        while (success < count && attempts < maxAttempts) {
            attempts++;
            String randomPatientId = "" + random.nextInt(nextPatientId);

            PcrTest test = new PcrTest(
                    nextPcrTestId,
                    randomPatientId,
                    LocalDateTime.of(2020, 1, 1, 0, 0, 0)
                            .plusDays(nextPcrTestId)
                            .plusMinutes(nextPcrTestId),
                    random.nextBoolean(),
                    random.nextDouble() * 100.0,
                    "note: " + nextPcrTestId
            );

            if (insertPcrTest(test)) {
                success++;
            }
        }
    }

    private void generateRandomPatients(int count) {
        for (int i = 0; i < count; i++) {
            patients.insert(new Patient(
                    "FN" + nextPatientId,
                    "LN" + nextPatientId,
                    LocalDate.of(2000, 1, 1).plusDays(nextPatientId),
                    "" + nextPatientId
            ));
            nextPatientId++;
        }
    }

    public boolean insertPcrTest(PcrTest pcrTest) {
        // 1. Najprv nájdi pacienta
        String patientId = pcrTest.getPatientId();
        Patient patient = findPatient(patientId);

        // 2. Ak pacient neexistuje, test nemôžeme priradiť
        if (patient == null) {
            System.out.println("Patient with ID " + patientId + " not found. Test not inserted.");
            return false;
        }

        // 3. Priraď test pacientovi
        boolean assignedToPatient = patient.insertTest(pcrTest);
        if (!assignedToPatient) {
            System.out.println("Patient " + patientId + " has reached maximum test limit. Test not inserted.");
            return false;
        }

        // 4. Aktualizuj pacienta v databáze
        boolean patientUpdated = editPatient(patient);
        if (!patientUpdated) {
            System.out.println("Failed to update patient " + patientId + " with new test.");
            return false;
        }

        // 5. Vlož test do databázy PCR testov
        if (pcrTests.insert(pcrTest) >= 0) {
            nextPcrTestId++;
            return true;
        }

        return false;
    }

    public Patient findPatient(String id) {
        Patient p = new Patient(id);
        Patient foundPatient = patients.get(p);
        if (foundPatient == null) {
            return null;
        }
        return new Patient(foundPatient); //copy
    }

    public PcrTest findPcrTest(int id) {
        PcrTest t = new PcrTest(id);
        PcrTest foundTest = pcrTests.get(t);
        if (foundTest == null) {
            return null;
        }
        return new PcrTest(foundTest); //copy
    }

    public boolean insertPatient(Patient patient) {
        if (patients.insert(patient) >= 0) {
            nextPatientId++;
            return true;
        }
        return false;
    }

    public boolean removePcrTest(int id) {
        PcrTest t = new PcrTest(id);
        return pcrTests.delete(t);
    }

    public boolean removePatient(String id) {
        Patient p = new Patient(id);
        return patients.delete(p);
    }

    public boolean editPatient(Patient patient) {
        return patients.edit(patient);
    }

    public boolean editPcrTest(PcrTest pcrTest) {
        return pcrTests.edit(pcrTest);
    }

    public String getBlocksForPrint() {
        return patients.toString();
    }

    public boolean saveMetadata(String systemName) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(systemName + "_meta.txt"))) {
            writer.println(nextPatientId);
            writer.println(nextPcrTestId);
            writer.println(blockSize);
            writer.println(overflowBlockSize);
            return true;
        } catch (IOException e) {
            System.err.println("Error saving metadata: " + e.getMessage());
            return false;
        }
    }

    public boolean load(String systemName) {
        File metaFile = new File(systemName + "_meta.txt");

        if (!metaFile.exists()) {
            return false;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(metaFile))) {
            nextPatientId = Integer.parseInt(reader.readLine());
            nextPcrTestId = Integer.parseInt(reader.readLine());
            blockSize = Integer.parseInt(reader.readLine());
            overflowBlockSize = Integer.parseInt(reader.readLine());

            // Initialize HashFile objects with loaded parameters
            try {
                this.patients = new HashFile<>(systemName + "_patients", blockSize, overflowBlockSize, Patient.class);
                this.pcrTests = new HashFile<>(systemName + "_pcrTests", blockSize, overflowBlockSize, PcrTest.class);
                return true;
            } catch (IOException e) {
                System.err.println("Error loading hash files: " + e.getMessage());
                return false;
            }

        } catch (IOException | NumberFormatException e) {
            System.err.println("Error reading metadata: " + e.getMessage());
            return false;
        }
    }

    public void close() {
        if (patients != null) {
            patients.close();
        }
        if (pcrTests != null) {
            pcrTests.close();
        }
        saveMetadata(this.systemName);
    }

    public int getPatientCount() {
        return nextPatientId;
    }

    public int getTestCount() {
        return nextPcrTestId;
    }

    public String getHashFilesAsString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Next patient id: " + nextPatientId + ", Next test id: " + nextPcrTestId);

        sb.append("\n\n#################### PATIENTS HASH FILE ####################\n\n");
        sb.append("Total Patients: ").append(nextPatientId).append("\n");
        if (patients != null) {
            sb.append(patients.toString());
        } else {
            sb.append("Patients file not initialized\n");
        }

        sb.append("\n\n#################### PCR TESTS HASH FILE ####################\n\n");
        sb.append("Total Tests: ").append(nextPcrTestId).append("\n");
        if (pcrTests != null) {
            sb.append(pcrTests.toString());
        } else {
            sb.append("PCR Tests file not initialized\n");
        }

        return sb.toString();
    }

    private void deleteOldFiles(String baseName) {
        java.io.File file = new java.io.File(baseName + "_meta.txt");
        if (file.delete()) {
            System.out.println("Deleted old file: " + file.getName());
        } else {
            System.out.println("Failed to delete old file: " + file.getName());
        }

        String[] extensions1 = {"_patients","_pcrTests"};
        String[] extensions2 = {".dat","_heap.dat", "_hash.dat", "_overflow.dat", "_overflow_heap.dat"};
        for (String ext1 : extensions1) {
            for (String ext2 : extensions2) {
                file = new java.io.File(baseName + ext1 + ext2);
                if (file.exists()) {
                    if (file.delete()) {
                        System.out.println("Deleted old file: " + file.getName());
                    } else {
                        System.out.println("Failed to delete old file: " + file.getName());
                    }
                }
            }
        }
    }
}