package tests;

import whoApp.PcrTest;
import file.hashfile.HashFile;

import java.io.IOException;
import java.time.LocalDateTime;

public class HashFileSimpleTest {

    public static void main(String[] args) {

        // Testovacie údaje
        PcrTest[] tests = new PcrTest[100000];
        for (int i = 0; i < 100000; i++) {
            tests[i] = new PcrTest(
                    i, // testId
                    "PAT" + String.format("%03d", i), // patientId
                    LocalDateTime.now().minusDays(i), // testDateTime
                    i % 3 == 0, // result (každý tretí pozitívny)
                    25.5 + i * 0.1, // testValue
                    "Test note " + i // note
            );
        }

        // Vytvorenie hash file
        HashFile<PcrTest> hashFile = null;
        try {
            hashFile = new HashFile<>("pcr_tests", 512, 256, PcrTest.class);
        } catch (IOException e) {
            System.out.println("Error creating hash file: " + e.getMessage());
            return;
        }

        System.out.println("=== TEST VKLADANIA PCR TESTOV ===");
        // Vloženie záznamov
        for (int i = 0; i < 10; i++) {
            int blockIndex = hashFile.insert(tests[i]);
            System.out.println("Test " + i + " vložený do bloku: " + blockIndex);
        }

        // Výpis celej hash tabuľky
        System.out.println("\n=== VÝPIS HASH TABUĽKY PO VKLADANÍ ===");
        hashFile.printAllBlocks();


        boolean error = false;
        System.out.println("\n=== TEST VYHĽADÁVANIA ===");
        // Vyhľadávanie niektorých záznamov
        for (int i = 0; i < 10; i += 1) { // každý párny test
            PcrTest searchTest = new PcrTest(i, "", LocalDateTime.now(), false, 0.0, "");
            PcrTest found = hashFile.get(searchTest);
            if (found != null) {
                System.out.println("Nájdený test " + i + ": " + found);
            } else {
                error = true;
                System.out.println("--------------------Test " + i + " NEBOL nájdený!");
            }
        }
        System.out.println("Error " +  error + "\n");

        PcrTest editedTest1 = new PcrTest(1, "1", LocalDateTime.now(), true, 11.11, "11");
        System.out.println("Edit patient 1: " + hashFile.edit(editedTest1));
        PcrTest editedTest9 = new PcrTest(9, "1", LocalDateTime.now(), true, 11.11, "11");
        System.out.println("Edit patient 9: " + hashFile.edit(editedTest9));
        PcrTest editedTest10 = new PcrTest(10, "1", LocalDateTime.now(), true, 11.11, "11");
        System.out.println("Edit patient 10: " + hashFile.edit(editedTest10));

        // Výpis celej hash tabuľky
        System.out.println("\n=== VÝPIS HASH TABUĽKY PO VKLADANÍ ===");
        hashFile.printAllBlocks();
/*
        System.out.println("\n=== TEST MAZANIA ===");
        // Zmazanie niektorých záznamov
        boolean deleted3 = hashFile.delete(tests[3]);
        System.out.println("Test 3 vymazaný: " + deleted3);

        boolean deleted7 = hashFile.delete(tests[7]);
        System.out.println("Test 7 vymazaný: " + deleted7);

        // Pokus o zmazanie neexistujúceho záznamu
        PcrTest nonExistent = new PcrTest(99, "", LocalDateTime.now(), false, 0.0, "");
        boolean deleted99 = hashFile.delete(nonExistent);
        System.out.println("Test 99 vymazaný: " + deleted99);

        System.out.println("\n=== VÝPIS HASH TABUĽKY PO MAZANÍ ===");
        hashFile.printAllBlocks();

        System.out.println("\n=== TEST ĎALŠIEHO VKLADANIA ===");
        // Vloženie nových záznamov - mali by sa dostať na uvoľnené miesta
        PcrTest newTest1 = new PcrTest(
                100, // nové ID
                "PAT100",
                LocalDateTime.now(),
                true,
                30.5,
                "Nový test 1"
        );

        PcrTest newTest2 = new PcrTest(
                101, // nové ID
                "PAT101",
                LocalDateTime.now(),
                false,
                22.1,
                "Nový test 2"
        );

        int block100 = hashFile.insert(newTest1);
        System.out.println("Nový test 100 vložený do bloku: " + block100);

        int block101 = hashFile.insert(newTest2);
        System.out.println("Nový test 101 vložený do bloku: " + block101);

        System.out.println("\n=== FINÁLNY VÝPIS HASH TABUĽKY ===");
        hashFile.printAllBlocks();

        // Výpis štatistík
        System.out.println("\n=== ŠTATISTIKY ===");

        // Zatvorenie súboru
        hashFile.close();

        System.out.println("\n=== TEST NAČÍTANIA ZO SÚBORU ===");
        // Test načítania z existujúceho súboru
        try {
            HashFile<PcrTest> reloadedHashFile = new HashFile<>("pcr_tests", 512, 256, PcrTest.class);
            reloadedHashFile.printAllBlocks();

            // Overenie, že dáta sú stále prístupné
            PcrTest searchTest = new PcrTest(5, "", LocalDateTime.now(), false, 0.0, "");
            PcrTest foundAfterReload = reloadedHashFile.get(searchTest);
            System.out.println("Test 5 po načítaní: " + foundAfterReload);

            reloadedHashFile.close();
        } catch (IOException e) {
            System.out.println("Error reloading hash file: " + e.getMessage());
        }
         */
        System.out.println("\nTest dokončený!");
    }
}