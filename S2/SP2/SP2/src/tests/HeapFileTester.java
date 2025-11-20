package tests;

import app.Patient;
import heapfile.HeapFile;

import java.io.FileNotFoundException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Random;

public class HeapFileTester {

    public void testMethods(boolean initialFilling) {
        Random r = new Random();
        LinkedList<Integer> linkedList = new LinkedList<>();

        //my structure
        HeapFile<Patient> heapFile = null;

        try {
            heapFile = new HeapFile<>("patients", 512, Patient.class);
        } catch (FileNotFoundException e) {
            java.lang.System.out.println("File not found");
        }
        if (initialFilling) {generateRandomPatients(300, heapFile);}

        int repsNum = 100;
        int methodCallsNum = 1000;
        int maxValue = 100_000;
        int initialElementsNum = 10_000;


        //random methods calls
        int numEquals = 0;
        int numNotEquals = 0;

        for (int i = 0; i < repsNum; i++) {

            for (int j = 0; j < methodCallsNum; j++) {
                int methodProb = r.nextInt(100);

                if (methodProb < 35) {
                    //insert
                    int a = r.nextInt(maxValue);
                    heapFile.insert(a);
                    if (!linkedList.contains(a)) {
                        linkedList.add(a);
                    }


                } else if (methodProb >= 35 && methodProb < 66) {
                    //remove
                    int linkedListSize = linkedList.size();
                    if (linkedListSize > 0) {
                        int index = r.nextInt(linkedListSize);
                        int a = linkedList.get(index);
                        heapFile.delete(a);
                        linkedList.remove(index);
                    }


                } else {
                    // find
                    int linkedListSize = linkedList.size();
                    if (linkedListSize > 0) {
                        int index = r.nextInt(linkedListSize);
                        Integer valueToFind = linkedList.get(index);
                        Integer found = tree.find(valueToFind);
                        if (found == null) {
                            System.out.println("Error: find - value " + valueToFind + " not found in tree");
                            return;
                        }
                    }
                }
            }
        }
    }

    private void generateRandomPatients(int count, HeapFile<Patient> heapFile) {
        for (int i = 0; i < count; i++) {
            heapFile.insert(new Patient("FN" + i, "LN" + i,
                    LocalDate.of(2000, 1, 1).plusDays(i), "" + i));
            i++;
        }
    }

    public static void main(String[] args) {
        HeapFileTester tester = new HeapFileTester();
        tester.testMethods(false);
    }
}
