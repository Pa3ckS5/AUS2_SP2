package whoApp;

import heapfile.IRecord;

import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class Patient implements IRecord<Patient> {
    private String firstName;
    private String lastName;
    private LocalDate birthDate;
    private String patientId;

    private static final int MAX_FIRST_NAME_LENGTH = 15;
    private static final int MAX_LAST_NAME_LENGTH = 14;
    private static final int MAX_BIRTH_DATE_LENGTH = 10;
    private static final int MAX_PATIENT_ID_LENGTH = 10;

    private int firstNameLength;
    private int lastNameLength;
    private int patientIdLength;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    public Patient() {
        this("", "", LocalDate.of(2000, 1, 1), "");
    }

    public Patient(String firstName, String lastName, LocalDate birthDate, String patientId) {
        setFirstName(firstName);
        setLastName(lastName);
        this.birthDate = birthDate;
        setPatientId(patientId);
    }

    public void setFirstName(String firstName) {
        this.firstNameLength = firstName.length();
        this.firstName = cutString(firstName, MAX_FIRST_NAME_LENGTH);
    }

    public void setLastName(String lastName) {
        this.lastNameLength = lastName.length();
        this.lastName = cutString(lastName, MAX_LAST_NAME_LENGTH);
    }

    public void setPatientId(String patientId) {
        this.patientIdLength = patientId.length();
        this.patientId = cutString(patientId, MAX_PATIENT_ID_LENGTH);
    }

    public String getFirstName() { return firstName.trim(); }
    public String getLastName() { return lastName.trim(); }
    public LocalDate getBirthDate() { return birthDate; }
    public String getPatientId() { return patientId.trim(); }

    @Override
    public int getSize() {
        // lengths + strings
        return Integer.BYTES * 3 + Character.BYTES * (MAX_FIRST_NAME_LENGTH + MAX_LAST_NAME_LENGTH + MAX_BIRTH_DATE_LENGTH + MAX_PATIENT_ID_LENGTH);
    }

    @Override
    public byte[] getBytes() {
        ByteArrayOutputStream hlpByteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream hlpOutStream = new DataOutputStream(hlpByteArrayOutputStream);

        try {
            hlpOutStream.writeInt(firstNameLength);
            hlpOutStream.writeChars(padString(firstName, MAX_FIRST_NAME_LENGTH));

            hlpOutStream.writeInt(lastNameLength);
            hlpOutStream.writeChars(padString(lastName, MAX_LAST_NAME_LENGTH));

            hlpOutStream.writeChars(birthDate.format(DATE_FORMATTER));

            hlpOutStream.writeInt(patientIdLength);
            hlpOutStream.writeChars(padString(patientId, MAX_PATIENT_ID_LENGTH));
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
                if (i < firstNameLength) {
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
                if (i < firstNameLength) {
                    this.patientId += hlpInStream.readChar();
                } else {
                    hlpInStream.readChar();
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
        return String.format("%s %s (%s) %s}",
                getFirstName(), getLastName(), getPatientId(), birthDate);
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