package tests;

import whoApp.Patient;
import heapfile.HeapFile;

import java.io.FileNotFoundException;
import java.time.LocalDate;

public class HeapFileSimpleTest {

    public static void main(String[] args) {
        // Testovacie údaje
        Patient patient1 = new Patient("Janko", "Mrkvička", LocalDate.of(1985, 5, 15), "PAT001");
        Patient patient2 = new Patient("Mária", "Novakova", LocalDate.of(1990, 8, 22), "PAT002");
        Patient patient3 = new Patient("Peter", "Velky", LocalDate.of(1978, 12, 3), "PAT003");
        Patient patient4 = new Patient("Anna", "Mala", LocalDate.of(2000, 3, 30), "PAT004");

        // Vytvorenie heap file
        HeapFile<Patient> heapFile = null;
        try {
            heapFile = new HeapFile<>("patients", 512, Patient.class);
        } catch (FileNotFoundException e) {
            System.out.println("File not found");
        }

        System.out.println("=== TEST VKLADANIA ===");
        // Vloženie záznamov
        int block1 = heapFile.insert(patient1);
        System.out.println("Patient1 vložený do bloku: " + block1);

        int block2 = heapFile.insert(patient2);
        System.out.println("Patient2 vložený do bloku: " + block2);

        int block3 = heapFile.insert(patient3);
        System.out.println("Patient3 vložený do bloku: " + block3);

        // Výpis všetkých blokov
        heapFile.printAllBlocks();

        System.out.println("\n=== TEST ČÍTANIA ===");
        // Čítanie záznamov
        Patient readPatient1 = heapFile.get(block1, 0);
        System.out.println("Prečítaný patient1: " + readPatient1);

        Patient readPatient2 = heapFile.get(block2, 1);
        System.out.println("Prečítaný patient2: " + readPatient2);

        System.out.println("\n=== TEST MAZANIA ===");
        // Mazanie záznamu
        boolean deleted = heapFile.delete(block2, patient2);
        System.out.println("Patient2 vymazaný: " + deleted);

        // Výpis po mazaní
        heapFile.printAllBlocks();

        System.out.println("\n=== TEST ĎALŠIEHO VKLADANIA ===");
        // Vloženie ďalšieho záznamu - mal by použiť uvoľnený blok
        int block4 = heapFile.insert(patient4);
        System.out.println("Patient4 vložený do bloku: " + block4);

        // Finálny výpis
        heapFile.printAllBlocks();

        // Zatvorenie súboru
        heapFile.close();

        System.out.println("\n=== TEST NAČÍTANIA ZO SÚBORU ===");
        // Test načítania z existujúceho súboru
        HeapFile<Patient> reloadedHeapFile = null;
        try {
            reloadedHeapFile = new HeapFile<>("patients", 512, Patient.class);
        } catch (FileNotFoundException e) {
            System.out.println("File not found");
        }
        reloadedHeapFile.printAllBlocks();
        reloadedHeapFile.close();

        System.out.println("Test dokončený!");
    }
}