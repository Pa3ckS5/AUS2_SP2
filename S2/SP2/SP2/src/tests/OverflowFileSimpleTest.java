package tests;

import file.hashfile.OverflowFile;
import whoApp.data.Patient;

import java.io.FileNotFoundException;
import java.time.LocalDate;

public class OverflowFileSimpleTest {
    public static void main(String[] args) {
        // Testovacie údaje
        Patient patient1 = new Patient("Janko", "Mrkvička", LocalDate.of(1985, 5, 15), "PAT001");
        Patient patient2 = new Patient("Mária", "Novakova", LocalDate.of(1990, 8, 22), "PAT002");
        Patient patient3 = new Patient("Peter", "Velky", LocalDate.of(1978, 12, 3), "PAT003");
        Patient patient4 = new Patient("Anna", "Mala", LocalDate.of(2000, 3, 30), "PAT004");
        Patient patient5 = new Patient("Janko", "Mrkvička", LocalDate.of(1985, 5, 15), "PAT005");
        Patient patient6 = new Patient("Mária", "Novakova", LocalDate.of(1990, 8, 22), "PAT006");
        Patient patient7 = new Patient("Peter", "Velky", LocalDate.of(1978, 12, 3), "PAT007");
        Patient patient8 = new Patient("Anna", "Mala", LocalDate.of(2000, 3, 30), "PAT008");
        Patient patient9 = new Patient("Anna", "Mala", LocalDate.of(2000, 3, 30), "PAT009");
        Patient patient10 = new Patient("Anna", "Malicka", LocalDate.of(2000, 3, 30), "PAT010");

        // Vytvorenie heap file
        OverflowFile<Patient> file = null;
        try {
            file = new OverflowFile<>("patients_overflow", 400, Patient.class);
        } catch (FileNotFoundException e) {
            System.out.println("File not found");
        }

        System.out.println("=== TEST VKLADANIA ===");
        // Vloženie záznamov

        int i0 = file.insertToStart(patient1);
        System.out.println("Patient1 vložený: " + i0);
        System.out.println("Patient2 vložený: " + file.insertToChain(i0, patient2));
        System.out.println("Patient3 vložený: " + file.insertToChain(i0, patient3));
        int i1 = file.insertToStart(patient4);
        System.out.println("Patient4 vložený: " + i1);
        System.out.println("Patient5 vložený: " + file.insertToChain(i1, patient5));
        // Výpis všetkých blokov
        file.printAllBlocks();


        System.out.println("\n=== TEST ČÍTANIA ===");
        // Čítanie záznamov
        Patient readPatient1 = file.get(i0, 0);
        System.out.println("Prečítaný patient1: " + readPatient1);
        Patient readPatient2 = file.get(i0, 1);
        System.out.println("Prečítaný patient2: " + readPatient2);


        System.out.println("\n=== TEST MAZANIA ===");
        // Mazanie záznamu
        boolean deleted = file.delete(i0, patient2);
        System.out.println("Patient2 vymazaný: " + deleted);

        // Výpis po mazaní
        file.printAllBlocks();


        System.out.println("\n=== TEST ĎALŠIEHO VKLADANIA ===");
        // Vloženie ďalšieho záznamu - mal by použiť uvoľnený blok
        System.out.println("Patient6 vložený: " + file.insertToChain(i0, patient6));
        System.out.println("Patient7 vložený: " + file.insertToChain(i0, patient7));
        System.out.println("Patient8 vložený: " + file.insertToChain(i1, patient8));
        System.out.println("Patient9 vložený: " + file.insertToChain(i1, patient9));

        System.out.println("Patient7 vymazany i1: " + file.delete(i1, patient7));
        System.out.println("Patient7 vymazany i0: " + file.delete(i0, patient7));

        int i2 = file.insertToStart(patient10);
        System.out.println("Patient10 vložený: " + i2);


        // Finálny výpis
        file.printAllBlocks();
        // Zatvorenie súboru
        file.close();


        System.out.println("\n=== TEST NAČÍTANIA ZO SÚBORU ===");
        // Test načítania z existujúceho súboru
        OverflowFile<Patient> reloadedOverflowFile = null;
        try {
            reloadedOverflowFile = new OverflowFile<>("patients_overflow", 400, Patient.class);
        } catch (FileNotFoundException e) {
            System.out.println("File not found");
        }
        reloadedOverflowFile.printAllBlocks();

        reloadedOverflowFile.close();

        System.out.println("Test dokončený!");
    }
}
