package app;

import heapfile.HeapFile;

import java.io.FileNotFoundException;
import java.time.LocalDate;

public class System {
    private HeapFile<Patient> heapFile;
    private int patientsCount;

    public System(boolean initialFill) {
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


}
