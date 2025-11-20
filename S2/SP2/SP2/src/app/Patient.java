package app;

import heapfile.IRecord;

import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class Patient implements IRecord<Patient> {
    private String firstName;
    private String lastName;
    private LocalDate birthDate;
    private String patientId;

    private static final int FIRST_NAME_LENGTH = 15;
    private static final int LAST_NAME_LENGTH = 14;
    private static final int BIRTH_DATE_LENGTH = 10;
    private static final int PATIENT_ID_LENGTH = 10;

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

    private String padString(String str, int length) {
        if (str == null) return " ".repeat(length);
        if (str.length() > length) return str.substring(0, length);
        return String.format("%-" + length + "s", str);
    }

    public void setFirstName(String firstName) {
        this.firstName = padString(firstName, FIRST_NAME_LENGTH);
    }

    public void setLastName(String lastName) {
        this.lastName = padString(lastName, LAST_NAME_LENGTH);
    }

    public void setPatientId(String patientId) {
        this.patientId = padString(patientId, PATIENT_ID_LENGTH);
    }

    public String getFirstName() { return firstName.trim(); }
    public String getLastName() { return lastName.trim(); }
    public LocalDate getBirthDate() { return birthDate; }
    public String getPatientId() { return patientId.trim(); }

    @Override
    public int getSize() {
        return Character.BYTES * (FIRST_NAME_LENGTH + LAST_NAME_LENGTH + BIRTH_DATE_LENGTH + PATIENT_ID_LENGTH);
    }

    @Override
    public byte[] getBytes() {
        ByteArrayOutputStream hlpByteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream hlpOutStream = new DataOutputStream(hlpByteArrayOutputStream);

        try {
            hlpOutStream.writeChars(firstName);
            hlpOutStream.writeChars(lastName);
            hlpOutStream.writeChars(birthDate.format(DATE_FORMATTER));
            hlpOutStream.writeChars(patientId);
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
            this.firstName = "";
            for (int i = 0; i < FIRST_NAME_LENGTH; i++) {
                this.firstName += hlpInStream.readChar();
            }

            this.lastName = "";
            for (int i = 0; i < LAST_NAME_LENGTH; i++) {
                this.lastName += hlpInStream.readChar();
            }

            String dateStr = "";
            for (int i = 0; i < BIRTH_DATE_LENGTH; i++) {
                dateStr += hlpInStream.readChar();
            }
            dateStr = dateStr.trim();  // Odstráni medzery
            if (!dateStr.isEmpty()) {
                this.birthDate = LocalDate.parse(dateStr, DATE_FORMATTER);
            } else {
                this.birthDate = LocalDate.of(2000, 1, 1);  // Predvolená hodnota
            }

            this.patientId = "";
            for (int i = 0; i < PATIENT_ID_LENGTH; i++) {
                this.patientId += hlpInStream.readChar();
            }

        } catch (IOException e) {
            throw new IllegalStateException("Error during conversion from byte array.");
        }

        return this;
    }

    @Override
    public boolean isEqualTo(Patient other) {
        return this.patientId.trim().equals(other.patientId.trim());
    }

    @Override
    public Patient createClass() {
        return new Patient();
    }

    @Override
    public String toString() {
        return String.format("app.Patient{firstName='%s', lastName='%s', birthDate=%s, patientId='%s'}",
                getFirstName(), getLastName(), birthDate, getPatientId());
    }
}