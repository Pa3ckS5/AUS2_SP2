package whoApp.data;

import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class Patient implements IRecord<Patient> {
    private String firstName;
    private String lastName;
    private LocalDate birthDate;
    private String patientId;
    private ArrayList<Integer> testIds; // Zmenené na ID testov
    private static final int MAX_TESTS = 6;

    private static final int MAX_FIRST_NAME_LENGTH = 15;
    private static final int MAX_LAST_NAME_LENGTH = 14;
    private static final int MAX_BIRTH_DATE_LENGTH = 10;
    private static final int MAX_PATIENT_ID_LENGTH = 10;

    private int firstNameLength;
    private int lastNameLength;
    private int patientIdLength;
    private int testCount;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    public Patient(String firstName, String lastName, LocalDate birthDate, String patientId) {
        setFirstName(firstName);
        setLastName(lastName);
        this.birthDate = birthDate;
        setPatientId(patientId);
        testIds = new ArrayList<>();
        testCount = 0;
    }

    public Patient() {
        this("", "", LocalDate.of(2000, 1, 1), "");
    }

    public Patient(String patientId) {
        this("", "", LocalDate.of(2000, 1, 1), patientId);
    }

    public Patient(Patient patient) {
        setFirstName(patient.getFirstName());
        setLastName(patient.getLastName());
        this.birthDate = patient.getBirthDate();
        setPatientId(patient.getPatientId());
        this.testIds = new ArrayList<>(patient.getTestIds());
        this.testCount = patient.testCount;
    }

    public boolean insertTest(PcrTest test) {
        if (testIds.size() < MAX_TESTS) {
            testIds.add(test.getTestId());
            testCount = testIds.size();
            return true;
        }
        return false;
    }

    public boolean insertTestId(int testId) {
        if (testIds.size() < MAX_TESTS) {
            testIds.add(testId);
            testCount = testIds.size();
            return true;
        }
        return false;
    }

    public boolean removeTest(PcrTest test) {
        return removeTestId(test.getTestId());
    }

    public boolean removeTestId(int testId) {
        for (int i = 0; i < testIds.size(); i++) {
            if (testIds.get(i) == testId) {
                testIds.remove(i);
                testCount = testIds.size();
                return true;
            }
        }
        return false;
    }

    public void setFirstName(String firstName) {
        this.firstName = cutString(firstName, MAX_FIRST_NAME_LENGTH);
        this.firstNameLength = firstName.length();
    }

    public void setLastName(String lastName) {
        this.lastName = cutString(lastName, MAX_LAST_NAME_LENGTH);
        this.lastNameLength = lastName.length();
    }

    public void setPatientId(String patientId) {
        this.patientId = cutString(patientId, MAX_PATIENT_ID_LENGTH);
        this.patientIdLength = patientId.length();
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public LocalDate getBirthDate() { return birthDate; }
    public String getPatientId() { return patientId; }

    public ArrayList<Integer> getTestIds() {
        return new ArrayList<>(testIds);
    }

    public int getTestCount() {
        return testCount;
    }

    // Pre spätnú kompatibilitu - vráti prázdny zoznam
    public ArrayList<Integer> getTests() {
        return testIds;
    }

    @Override
    public int getSize() {
        // lengths + strings + testCount + MAX_TESTS * Integer (pre test IDs)
        return Integer.BYTES * 4 + // firstNameLength, lastNameLength, patientIdLength, testCount
                Character.BYTES * (MAX_FIRST_NAME_LENGTH + MAX_LAST_NAME_LENGTH +
                        MAX_BIRTH_DATE_LENGTH + MAX_PATIENT_ID_LENGTH) +
                Integer.BYTES * MAX_TESTS; // Pre test IDs
    }

    @Override
    public byte[] getBytes() {
        ByteArrayOutputStream hlpByteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream hlpOutStream = new DataOutputStream(hlpByteArrayOutputStream);

        try {
            // Personal information
            hlpOutStream.writeInt(firstNameLength);
            hlpOutStream.writeChars(padString(firstName, MAX_FIRST_NAME_LENGTH));

            hlpOutStream.writeInt(lastNameLength);
            hlpOutStream.writeChars(padString(lastName, MAX_LAST_NAME_LENGTH));

            hlpOutStream.writeChars(padString(birthDate.format(DATE_FORMATTER), MAX_BIRTH_DATE_LENGTH));

            hlpOutStream.writeInt(patientIdLength);
            hlpOutStream.writeChars(padString(patientId, MAX_PATIENT_ID_LENGTH));

            // Test information
            hlpOutStream.writeInt(testCount);
            for (int i = 0; i < MAX_TESTS; i++) {
                if (i < testIds.size()) {
                    hlpOutStream.writeInt(testIds.get(i));
                } else {
                    hlpOutStream.writeInt(-1); // Prázdne miesto
                }
            }

        } catch (IOException e) {
            throw new IllegalStateException("Error during conversion to byte array.");
        }

        return hlpByteArrayOutputStream.toByteArray();
    }

    @Override
    public Patient fromBytes(byte[] bytes) {
        ByteArrayInputStream hlpByteArrayInputStream = new ByteArrayInputStream(bytes);
        DataInputStream hlpInStream = new DataInputStream(hlpByteArrayInputStream);

        try {
            // Personal information
            this.firstNameLength = hlpInStream.readInt();
            this.firstName = "";
            for (int i = 0; i < MAX_FIRST_NAME_LENGTH; i++) {
                if (i < firstNameLength) {
                    this.firstName += hlpInStream.readChar();
                } else {
                    hlpInStream.readChar();
                }
            }

            this.lastNameLength = hlpInStream.readInt();
            this.lastName = "";
            for (int i = 0; i < MAX_LAST_NAME_LENGTH; i++) {
                if (i < lastNameLength) {
                    this.lastName += hlpInStream.readChar();
                } else {
                    hlpInStream.readChar();
                }
            }

            String dateStr = "";
            for (int i = 0; i < MAX_BIRTH_DATE_LENGTH; i++) {
                dateStr += hlpInStream.readChar();
            }
            dateStr = dateStr.trim();

            try {
                if (!dateStr.isEmpty() && dateStr.length() >= 10) {
                    this.birthDate = LocalDate.parse(dateStr, DATE_FORMATTER);
                } else {
                    this.birthDate = LocalDate.of(1, 1, 1);
                }
            } catch (Exception e) {
                this.birthDate = LocalDate.of(1, 1, 1);
            }

            this.patientIdLength = hlpInStream.readInt();
            this.patientId = "";
            for (int i = 0; i < MAX_PATIENT_ID_LENGTH; i++) {
                if (i < patientIdLength) {
                    this.patientId += hlpInStream.readChar();
                } else {
                    hlpInStream.readChar();
                }
            }

            // Test information
            this.testCount = hlpInStream.readInt();
            this.testIds = new ArrayList<>();
            for (int i = 0; i < MAX_TESTS; i++) {
                int testId = hlpInStream.readInt();
                if (testId != -1 && i < testCount) {
                    testIds.add(testId);
                }
            }

        } catch (IOException e) {
            throw new IllegalStateException("Error during conversion from byte array.");
        }

        return this;
    }

    @Override
    public boolean isEqualTo(Patient other) {
        return this.patientId.equals(other.patientId);
    }

    @Override
    public Patient createClass() {
        return new Patient();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%s %s (%s) %s", getFirstName(), getLastName(), getPatientId(), birthDate));

        sb.append(" [");
        for (int i = 0; i < testIds.size(); i++) {
            sb.append(testIds.get(i));
            if (i < testIds.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append("]");


        return sb.toString();
    }

    @Override
    public int hashCode() {
        return this.patientId.hashCode();
    }

    private String padString(String str, int length) {
        if (str == null) return " ".repeat(length);
        if (str.length() > length) return str.substring(0, length);
        return String.format("%-" + length + "s", str);
    }

    private String cutString(String str, int length) {
        if (str == null) return "";
        if (str.length() > length) return str.substring(0, length);
        return str;
    }
}