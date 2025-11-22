package app;

import heapfile.HeapFile;

import java.io.FileNotFoundException;
import java.time.LocalDate;

public class SystemHF {
    private HeapFile<Patient> heapFile;
    private int patientsCount;

    public SystemHF(boolean initialFill) {
        patientsCount = 0;
        HeapFile<Patient> heapFile = null;
        try {
            this.heapFile = new HeapFile<>("patients", 512, Patient.class);
        } catch (FileNotFoundException e) {
            java.lang.System.out.println("File not found");
        }
        if (initialFill) {generateRandomPatients(300);}
    }

    public void generateRandomPatients(int count) {
        for (int i = 0; i < count; i++) {
            heapFile.insert(new Patient("FN" + patientsCount, "LN" + patientsCount,
                    LocalDate.of(2000, 1, 1).plusDays(patientsCount), "" + patientsCount));
            patientsCount++;
        }
    }

    public void insertPatient(Patient patient) {
        heapFile.insert(patient);
    }

    public void removePatient(Patient patient, int blockIndex) {
        heapFile.delete(blockIndex, patient);
    }

    public String getBlocksForPrint() {
        return heapFile.toString();
    }

    public int getPatientsCount() {
        return patientsCount;
    }

    public void close() {
        heapFile.close();
    }

}
