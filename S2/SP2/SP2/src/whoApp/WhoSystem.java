package whoApp;

import hashfile.HashFile;
import heapfile.HeapFile;

import java.io.FileNotFoundException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Random;

public class WhoSystem {
    private HashFile<Patient> patients;
    private HashFile<PcrTest> pcrTests;

    private int nextPatientId;
    private int patientCount;
    private int nextPcrTestId;
    private int pcrTestCount;

    public WhoSystem(boolean initialFill) {
        nextPatientId = 0;
        patientCount = 0;

        try {
            this.patients = new HashFile<>("patients", 560, Patient.class);
            this.pcrTests = new HashFile<>("pcrTests", 560, PcrTest.class);
        } catch (FileNotFoundException e) {
            java.lang.System.out.println("File not found");
        }

        if (initialFill) {
            generateRandomPatients(300);
            generateRandomTests(500);
        }
    }

    private void generateRandomTests(int count) {
        Random r = new Random();
        for (int i = 0; i < count; i++) {
            pcrTests.insert(new PcrTest(nextPcrTestId, "" + r.nextInt(nextPcrTestId),
                    LocalDateTime.of(2020, 1, 1, 0, 0, 0).plusDays(nextPcrTestId).plusMinutes(nextPcrTestId),
                    false, r.nextDouble(), "Note" + nextPcrTestId));
            nextPcrTestId++;
        }
        pcrTestCount += count;
    }

    public void generateRandomPatients(int count) {
        for (int i = 0; i < count; i++) {
            patients.insert(new Patient("FN" + nextPatientId, "LN" + nextPatientId,
                    LocalDate.of(2000, 1, 1).plusDays(nextPatientId), "" + nextPatientId));
            nextPatientId++;
        }
        patientCount += count;
    }

    // 1. Vloženie výsledku PCR testu do systému (systém zabezpečí unikátnosť kódu PCR test ale neoveruje ju).
    public boolean insertPcrTest(Patient patient) {
        if (patients.insert(patient) >= 0) {
            patientCount++;
            return true;
        }
        return false;
    }

    // 2. Vyhľadanie osoby (definovaná unikátnym číslom pacienta) pričom sa zobrazia všetky údaje o osobe vrátane výpisu všetkých uskutočnených PCR testov.
    public Patient findPatient(String id) {
        return null;
    }

    // 3. Vyhľadanie PCR testu podľa jeho kódu, pričom sa zobrazia aj údaje o pacientovi.
    public PcrTest findPcrTest(int id) {
        return null;
    }

    // 4. Vloženie osoby do systému (systém zabezpečí unikátnosť číslo pacienta ale neoveruje ju).
    public boolean insertPatient(Patient patient) {
        if (patients.insert(patient) >= 0) {
            patientCount++;
            return true;
        }
        return false;
    }

    // 5. Trvalé a nevratné vymazanie výsledku PCR testu (napr. po chybnom vložení), test je definovaný svojim kódom.
    public boolean removePcrTest(int id) {
        return false;
    }

    // 6. Vymazanie osoby zo systému (definovaná unikátnym číslom pacienta) aj s jej výsledkami PCR testov.
    public boolean removePatient(String id) {
//        if (patients.delete(blockIndex, patient)) {
//            patientCount--;
//            return true;
//        }
        return false;
    }

    // 7. Vyhľadanie osoby (definovaná unikátnym číslom pacienta), pričom bude umožnené editovať jej údaje okrem testov.
    public Patient editPatient(String id) {
        return null;
    }

    // 8. Vyhľadanie PCR testu podľa jeho kódu, pričom bude umožnené editovať jeho údaje.
    public PcrTest editPcrTest(int id) {
        return null;
    }

    public String getBlocksForPrint() {
        return patients.toString();
    }

    public int getPatientCount() {return this.patientCount;}

    public int getPcrTestCount() {return this.pcrTestCount;}

    public void close() {
        patients.close();
    }

}
