package whoApp;

import file.hashfile.HashFile;
import file.heapfile.HeapFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDate;

public class HashSystem {
    private HashFile<Patient> hashFile;
    private int patientsCount;

    public HashSystem(boolean initialFill) {
                patientsCount = 0;
                HashFile<Patient> heapFile = null;
                try {
                    this.hashFile = new HashFile<>("patients", 512, 256, Patient.class); //512
                } catch (FileNotFoundException e) {
                    java.lang.System.out.println("File not found");
                } catch (IOException e) {
                    System.out.println("Error opening file");
                }
        if (initialFill) {generateRandomPatients(20);}
            }

            public void generateRandomPatients(int count) {
                for (int i = 0; i < count; i++) {
                    hashFile.insert(new Patient("FN" + patientsCount, "LN" + patientsCount,
                            LocalDate.of(2000, 1, 1).plusDays(patientsCount), "" + patientsCount));
                    patientsCount++;
                }
            }

            public String getBlocksForPrint() {
                return hashFile.toString();
            }

            public int getPatientsCount() {
                return hashFile.getRecordCount();
            }

            public void close() {
                hashFile.close();
            }


        }

