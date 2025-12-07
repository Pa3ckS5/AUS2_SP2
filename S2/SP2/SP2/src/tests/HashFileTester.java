package tests;

import file.hashfile.HashFile;
import whoApp.data.PcrTest;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.Random;

public class HashFileTester {

    private int totalInsertTests = 0;
    private int failedInsertTests = 0;
    private int totalDeleteTests = 0;
    private int failedDeleteTests = 0;
    private int totalFindTests = 0;
    private int failedFindTests = 0;
    private int totalPlacementTests = 0;
    private int failedPlacementTests = 0;

    public void testMethods(boolean initialFilling) {
        String fileName = "test_pcrtests";
        int repsNum = 100;
        int methodCallsNum = 100;
        int initialElementsNum = 100;

        Random r = new Random(0);
        LinkedList<PcrTest> linkedList = new LinkedList<>();
        deleteTestFiles(fileName);

        // my structure
        HashFile<PcrTest> hashFile = null;

        try {
            hashFile = new HashFile<>(fileName, 1024, 512, PcrTest.class); //512 256
        } catch (IOException e) {
            System.out.println("Error creating hash file");
            return;
        }

        int numEquals = 0;
        int numNotEquals = 0;
        int testIdCounter = 0;

        if (initialFilling) {
            generateRandomTests(initialElementsNum, hashFile, linkedList);
            testIdCounter = initialElementsNum;
        }

        boolean inserted213 = false;

        for (int i = 0; i < repsNum; i++) {
            System.out.println("Repetition " + (i + 1) + "/" + repsNum);

            for (int j = 0; j < methodCallsNum; j++) {
                int methodProb = r.nextInt(100);

                if (methodProb < 35) {
                    // INSERT
                    PcrTest test = new PcrTest(
                            testIdCounter,
                            "PAT" + testIdCounter,
                            LocalDateTime.now().plusDays(testIdCounter),
                            r.nextBoolean(),
                            r.nextDouble() * 100,
                            "Note " + testIdCounter
                    );

                    testIdCounter++;

                    // Test before insertion
                    testRecordPlacementBeforeInsert(hashFile, test);

                    // Perform insertion
                    hashFile.insert(test);
                    linkedList.add(test);

                    // Test after insertion
                    testRecordPlacementAfterInsert(hashFile, test);
                    testFindOperation(hashFile, test, true);
                    testFillingConsistency(hashFile);

                } else if (methodProb >= 35 && methodProb < 66) {
                    // DELETE

                    if (!linkedList.isEmpty()) {
                        int removeIndex = r.nextInt(linkedList.size());
                        PcrTest testToRemove = linkedList.get(removeIndex);

                        // Test before deletion
                        testFindOperation(hashFile, testToRemove, true);

                        // Perform deletion
                        boolean removedFromHash = hashFile.delete(testToRemove);

                        // Test after deletion
                        if (removedFromHash) {
                            linkedList.remove(removeIndex);
                            testFindOperation(hashFile, testToRemove, false);
                            testFillingConsistency(hashFile);
                        } else {
                            System.out.println("WARNING: Failed to remove test from hash file: " + testToRemove);
                            failedDeleteTests++;
                        }
                        totalDeleteTests++;
                    }


                } else {
                    // FIND
                    if (!linkedList.isEmpty()) {
                        int findIndex = r.nextInt(linkedList.size());
                        PcrTest testToFind = linkedList.get(findIndex);

                        testFindOperation(hashFile, testToFind, true);
                    }
                }

                if (inserted213) {
                    PcrTest test = new PcrTest(213);
                    PcrTest found = hashFile.get(test);
                    boolean actuallyExists = (found != null && found.isEqualTo(test));
                }
            }

            boolean equals = verifyAllRecords(hashFile, linkedList);
            boolean structureValid = testHashFileStructure(hashFile);

            if (equals && structureValid) {
                numEquals++;
                System.out.println("✓ Verification " + (i + 1) + " PASSED");
            } else {
                numNotEquals++;
                System.out.println("✗ Verification " + (i + 1) + " FAILED");

                if (!equals) {
                    System.out.println("  - Record verification failed");
                }
                if (!structureValid) {
                    System.out.println("  - Hash file structure invalid");
                }
            }
        }

        // final verification
        System.out.println("\n=== FINAL VERIFICATION ===");
        boolean finalEquals = verifyAllRecords(hashFile, linkedList);
        boolean finalStructureValid = testHashFileStructure(hashFile);

        if (finalEquals && finalStructureValid) {
            System.out.println("✓ Final verification PASSED");
        } else {
            System.out.println("✗ Final verification FAILED");
        }

        // stats
        printTestStatistics();
        hashFile.printStatistics();

        hashFile.close();

        System.out.println("\nSummary: " + numEquals + "/" + (numEquals + numNotEquals) +  " passed");

        deleteTestFiles(fileName);
    }

    private void testRecordPlacementBeforeInsert(HashFile<PcrTest> hashFile, PcrTest test) {
        PcrTest found = hashFile.get(test);
        if (found != null) {
            System.out.println("ERROR: Test already exists before insertion: " + test);
            failedInsertTests++;
        }
    }

    private void testRecordPlacementAfterInsert(HashFile<PcrTest> hashFile, PcrTest test) {
        // Test that test exists after insertion
        PcrTest found = hashFile.get(test);
        int hashCode = test.hashCode();
        if (found == null || !found.isEqualTo(test)) {
            System.out.println("ERROR: Test not found after insertion: " + test + " (hash: " + hashCode + ")");
            failedInsertTests++;
        }

        totalInsertTests++;
        totalPlacementTests++;
    }

    private void testFindOperation(HashFile<PcrTest> hashFile, PcrTest test, boolean shouldExist) {
        PcrTest found = hashFile.get(test);
        boolean actuallyExists = (found != null && found.isEqualTo(test));

        if (shouldExist && !actuallyExists) {
            System.out.println("ERROR: Test should exist but wasn't found: " + test);
            failedFindTests++;
        } else if (!shouldExist && actuallyExists) {
            System.out.println("ERROR: Test should not exist but was found: " + test);
            failedFindTests++;
        }
        totalFindTests++;
    }

    private void testFillingConsistency(HashFile<PcrTest> hashFile) {
        try {
            //hashFile.printStatistics();
            testHashFileStructure(hashFile);

        } catch (Exception e) {
            System.out.println("ERROR: Filling consistency check failed: " + e.getMessage());
        }
    }

    private boolean testHashFileStructure(HashFile<PcrTest> hashFile) {
        try {
            // Test basic invariants
            if (hashFile.getBlockCount() < 2) {
                System.out.println("ERROR: Block count should be at least 2");
                return false;
            }

            // Test that split pointer is within valid range
            int hashEdge = hashFile.getHashEdge();
            int splitPointer = hashFile.getSplitPointer();

            if (splitPointer < 0 || splitPointer >= hashEdge) {
                System.out.println("ERROR: Split pointer out of range: " + splitPointer);
                return false;
            }

            // Test that record count is non-negative
            if (getRecordCount(hashFile) < 0) {
                System.out.println("ERROR: Negative record count");
                return false;
            }

            return true;

        } catch (Exception e) {
            System.out.println("ERROR: Hash file structure test failed: " + e.getMessage());
            return false;
        }
    }

    private int getRecordCount(HashFile<PcrTest> hashFile) {
        try {
            java.lang.reflect.Field field = hashFile.getClass().getDeclaredField("recordCount");
            field.setAccessible(true);
            return (int) field.get(hashFile);
        } catch (Exception e) {
            return -1;
        }
    }

    private boolean verifyAllRecords(HashFile<PcrTest> hashFile, LinkedList<PcrTest> linkedList) {
        System.out.println("Verifying " + linkedList.size() + " records...");

        int verified = 0;
        int failed = 0;

        for (PcrTest test : linkedList) {
            PcrTest foundTest = hashFile.get(test);
            if (foundTest == null || !foundTest.isEqualTo(test)) {
                System.out.println("ERROR: Test not found: " + test);
                failed++;
            } else {
                verified++;
            }
        }

        System.out.println("Verification result: " + verified + " verified, " + failed + " failed");
        return failed == 0;
    }

    private void generateRandomTests(int count, HashFile<PcrTest> hashFile, LinkedList<PcrTest> linkedList) {
        Random r = new Random(0);
        for (int i = 0; i < count; i++) {
            PcrTest test = new PcrTest(
                    i,
                    "PAT" + i,
                    LocalDateTime.now().plusDays(i),
                    r.nextBoolean(),
                    r.nextDouble() * 100,
                    "Note " + i
            );

            // Test insertion
            testRecordPlacementBeforeInsert(hashFile, test);
            hashFile.insert(test);
            linkedList.add(test);
            testRecordPlacementAfterInsert(hashFile, test);
        }
        System.out.println("Generated " + count + " initial PCR tests");
    }

    private void deleteTestFiles(String baseName) {
        String[] extensions = {".dat","_heap.dat", "_hash.dat", "_overflow.dat", "_overflow_heap.dat"};
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

    private void printTestStatistics() {
        System.out.println("\n=== DETAILED TEST STATISTICS ===");
        System.out.println("Insert Operations:");
        System.out.println("  Total: " + totalInsertTests + ", Failed: " + failedInsertTests +
                ", Success Rate: " + getSuccessRate(totalInsertTests, failedInsertTests));

        System.out.println("Delete Operations:");
        System.out.println("  Total: " + totalDeleteTests + ", Failed: " + failedDeleteTests +
                ", Success Rate: " + getSuccessRate(totalDeleteTests, failedDeleteTests));

        System.out.println("Find Operations:");
        System.out.println("  Total: " + totalFindTests + ", Failed: " + failedFindTests +
                ", Success Rate: " + getSuccessRate(totalFindTests, failedFindTests));

        System.out.println("Placement Tests:");
        System.out.println("  Total: " + totalPlacementTests + ", Failed: " + failedPlacementTests +
                ", Success Rate: " + getSuccessRate(totalPlacementTests, failedPlacementTests));

        System.out.println("=== END TEST STATISTICS ===");
    }

    private String getSuccessRate(int total, int failed) {
        if (total == 0) return "N/A";
        double successRate = ((double) (total - failed) / total) * 100;
        return String.format("%.2f%%", successRate);
    }

    public static void main(String[] args) {
        HashFileTester tester = new HashFileTester();
        System.out.println("=== STARTING ENHANCED HASH FILE TEST WITH PCR TESTS ===");
        tester.testMethods(true);
    }
}