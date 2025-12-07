package tests;

import whoApp.data.Patient;
import file.heapfile.HeapFile;

import java.io.FileNotFoundException;
import java.time.LocalDate;
import java.util.LinkedList;
import java.util.Random;

public class HeapFileTester {

    public void testMethods(boolean initialFilling) {
        String fileName = "test_patients";

        int repsNum = 100;
        int methodCallsNum = 1000;
        int initialElementsNum = 500;

        Random r = new Random(0);
        LinkedList<PatientBlockPair> linkedList = new LinkedList<>();
        deleteTestFiles(fileName);

        // my structure
        HeapFile<Patient> heapFile = null;

        try {
            heapFile = new HeapFile<>(fileName, 512, Patient.class);
        } catch (FileNotFoundException e) {
            System.out.println("File not found");
            return;
        }

        int numEquals = 0;
        int numNotEquals = 0;
        int patientsCount = 0;

        if (initialFilling) {
            generateRandomPatients(initialElementsNum, heapFile, linkedList);
            patientsCount = initialElementsNum;
        }

        for (int i = 0; i < repsNum; i++) {
            System.out.println("Repetition " + (i + 1) + "/" + repsNum);

            for (int j = 0; j < methodCallsNum; j++) {
                int methodProb = r.nextInt(100);

                if (methodProb < 35) {
                    // insert
                    if (patientsCount == 33) {
                        System.out.print("");
                    }
                    Patient p = new Patient("FN" + patientsCount, "LN" + patientsCount,
                            LocalDate.of(2000, 1, 1).plusDays(patientsCount), "" + patientsCount);
                    patientsCount++;

                    int blockIndex = heapFile.insert(p);
                    linkedList.add(new PatientBlockPair(p, blockIndex));

                } else if (methodProb >= 35 && methodProb < 66) {
                    // remove
                    if (!linkedList.isEmpty()) {
                        int removeIndex = r.nextInt(linkedList.size());
                        PatientBlockPair pairToRemove = linkedList.get(removeIndex);

                        boolean removedFromHeap = heapFile.delete(pairToRemove.getBlock(), pairToRemove.getPatient());
                        linkedList.remove(removeIndex);

                        if (!removedFromHeap) {
                            System.out.println("WARNING: Failed to remove patient from heap file: " + pairToRemove.getPatient());
                        }
                    }

                } else {
                    // find
                    if (!linkedList.isEmpty()) {
                        int findIndex = r.nextInt(linkedList.size());
                        PatientBlockPair pairToFind = linkedList.get(findIndex);

                        boolean found = false;
                        int recordsPerBlock = 512 / (new Patient().getSize()); // Calculate based on block size

                        Patient foundPatient = heapFile.get(pairToFind.getBlock(), pairToFind.getPatient());
                        if (foundPatient != null && foundPatient.isEqualTo(pairToFind.getPatient())) {
                            found = true;
                        }

                        if (!found) {
                            System.out.println("WARNING: Patient not found in block " + pairToFind.getBlock() + ": " + pairToFind.getPatient());
                        }
                    }
                }
            }

            boolean equals = verifyAllRecords(heapFile, linkedList);

            if (equals) {
                numEquals++;
                System.out.println("✓ Verification " + (i + 1) + " PASSED");
            } else {
                numNotEquals++;
                System.out.println("✗ Verification " + (i + 1) + " FAILED");
            }
        }

        // Final verification
        System.out.println("\n=== FINAL VERIFICATION ===");
        boolean finalEquals = verifyAllRecords(heapFile, linkedList);
        if (finalEquals) {
            System.out.println("✓ Final verification PASSED");
        } else {
            System.out.println("✗ Final verification FAILED");
        }

        heapFile.printAllBlocks();

        heapFile.close();

        System.out.println(String.format("\nSummary: %d/%d passed", numEquals, numEquals + numNotEquals));

        deleteTestFiles(fileName);
    }

    private boolean verifyAllRecords(HeapFile<Patient> heapFile, LinkedList<PatientBlockPair> linkedList) {
        System.out.println("Verifying " + linkedList.size() + " records...");

        for (PatientBlockPair pair : linkedList) {
            Patient foundPatient = heapFile.get(pair.getBlock(), pair.getPatient());
            if (foundPatient == null || !foundPatient.isEqualTo(pair.getPatient())) {
                System.out.println("ERROR: Patient not found in block " + pair.getBlock() + ": " + pair.getPatient());
                return false;
            }
        }
        return true;
    }

    private void generateRandomPatients(int count, HeapFile<Patient> heapFile, LinkedList<PatientBlockPair> linkedList) {
        for (int i = 0; i < count; i++) {
            Patient p = new Patient("FN" + i, "LN" + i,
                    LocalDate.of(2000, 1, 1).plusDays(i), "" + i);
            int blockIndex = heapFile.insert(p);
            linkedList.add(new PatientBlockPair(p, blockIndex));
        }
        System.out.println("Generated " + count + " initial patients");
    }

    private void deleteTestFiles(String baseName) {
        String[] extensions = {".dat", "_heap.dat"};
        for (String ext : extensions) {
            java.io.File file = new java.io.File(baseName + ext);
            if (file.exists()) {
                if (file.delete()) {
                    System.out.println("Deleted old test file: " + file.getName());
                } else {
                    System.out.println("Failed to delete old test file: " + file.getName());
                }
            }
        }
    }

    public class PatientBlockPair {
        private int block;
        private Patient patient;

        public PatientBlockPair(Patient patient, int block) {
            this.patient = patient;
            this.block = block;
        }

        public int getBlock() {
            return block;
        }

        public Patient getPatient() {
            return patient;
        }

        @Override
        public String toString() {
            return "Block " + block + ": " + patient;
        }
    }

    public static void main(String[] args) {
        HeapFileTester tester = new HeapFileTester();
        System.out.println("=== STARTING HEAP FILE TEST ===");
        tester.testMethods(true);
    }
}